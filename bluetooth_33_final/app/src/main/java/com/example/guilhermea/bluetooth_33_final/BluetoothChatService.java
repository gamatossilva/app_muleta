package com.example.guilhermea.bluetooth_33_final;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothChatService {
    // UUID usada para se conectar ao Arduino
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private int mNewState;

    // Constantes que indicam o estado da conexão
    public static final int STATE_NONE = 0;         // nada está sendo feito
    public static final int STATE_LISTEN = 1;       // procurando por uma conexão
    public static final int STATE_CONNECTING = 2;   // conectando
    public static final int STATE_CONNECTED = 3;    // conectado a um dispositivo

     //Prepara uma nova sessão do BluetoothChat

    public BluetoothChatService(Context context, Handler handler){
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mNewState = mState;
        mHandler = handler;
    }

    //Retorna o estado atual da conexão
    public synchronized int getState(){ return mState; }

    public synchronized void start(){
        // Cancela qualquer outra tentatica de conexão
        if (mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancela qualquer conexão estabelecida anteriormente
        if (mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Atualiza a UI
        updateUserInterfaceTitle();
    }

    public synchronized void connect (BluetoothDevice device, boolean secure){
        // Cancela qualquer outra tentatica de conexão
        if (mState == STATE_CONNECTING){
            if (mConnectThread != null){
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancela qualquer conexão estabelecida anteriormente
        if (mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Inicia a thread para conectar com o dispositivo
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();

        // Atualiza a UI
        updateUserInterfaceTitle();
    }

    public synchronized void connected (BluetoothSocket socket, BluetoothDevice device, final String socketType){
        // Cancela qualquer outra tentatica de conexão
        if (mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancela qualquer conexão estabelecida anteriormente
        if (mConnectedThread != null){
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Inicia a thread para gerencia a conexão
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Envia o nome do dispositivo conectado para o serviço
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Atualiza a UI
        updateUserInterfaceTitle();
    }

    // Cancela todas as threads
    public synchronized void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mState = STATE_NONE;
        // Atualiza a UI
        updateUserInterfaceTitle();
    }

    public void write(byte[] out){
        // Cria um objeto temporário
        ConnectedThread r;

        synchronized (this){
            if(mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        // Utiliza o método de escrever assíncrono
        r.write(out);
    }

    // Atualiza a UI de acordo com o estado de conexão atual
    private synchronized void updateUserInterfaceTitle(){
        mState = getState();
        mNewState = mState;

        // Passa para o Handler o estado atual da conexão para que ele atualize a UI
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
    }

    // Indica que a conexão falhou e notifica a UI
    private void connectionFailed(){
        // Envia uma mensagem de falha de volta a UI
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;

        // Atualiza a UI
        updateUserInterfaceTitle();

        // Reinicia o BluetoothChatService
        BluetoothChatService.this.start();
    }

    //Indica que a conexão foi perdida e notifica a UI
    private void connectionLost(){
        // Envia uma mensagem de falha de volta a UI
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;

        // Atualiza a UI
        updateUserInterfaceTitle();

        // Reinicia o BluetoothChatService
        BluetoothChatService.this.start();
    }

    /** Esta thread é executada enquanto uma tentativa de conexão estiver sendo feita
     *  Ela independe da conexão ter ou não sucesso
     */

    private class ConnectThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure){
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
            } catch (IOException e) {
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run(){
            setName("ConnectThread" + mSocketType);

            // Sempre cancela a descoberta
            mAdapter.cancelDiscovery();

            try{
                mmSocket.connect();
            } catch (IOException e){
                try{
                    mmSocket.close();
                } catch (IOException e2){
                }

                connectionFailed();
                return;
            }

            synchronized (BluetoothChatService.this){
                mConnectThread = null;
            }

            connected(mmSocket, mmDevice, mSocketType);
        }
        public void cancel(){
            try{
                mmSocket.close();
            } catch (IOException e){
            }
        }

    }

    //Esta é a thread que gerencia a conexão
    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType){
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try{
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e){
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;

            while (mState == STATE_CONNECTED){
                try{
                    bytes = mmInStream.read(buffer);
                    String dataBt = new String(buffer, 0, bytes);
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, dataBt).sendToTarget();
                } catch (IOException e){
                    connectionLost();
                    break;
                }
            }

        }

        // Método de escrita assíncrona
        public void write(byte[] buffer){
            try{
                mmOutStream.write(buffer);
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e){
            }
        }

        public void cancel(){
            try {
                mmSocket.close();
            } catch (IOException e){
            }
        }
    }
}
