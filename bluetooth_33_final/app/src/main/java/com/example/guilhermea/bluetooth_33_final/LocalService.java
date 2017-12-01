package com.example.guilhermea.bluetooth_33_final;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

public class LocalService extends Service {
    private final IBinder mBinder = new LocalBinder();

    private BluetoothAdapter myBluetoothAdapter = null;
    private BluetoothChatService mChatService = null;

    private String mConnectedDeviceName;

    private boolean connection = false;

    private String allData;
    StringBuilder bluetoothData = new StringBuilder();
    StringBuilder serviceData = new StringBuilder();

    public static final String
            ACTION_BLUETOOTH_BROADCAST = "Service",
            DATA = "data";

    /**
      * Class used for the client Binder.  Because we know this service always
      * runs in the same process as its clients, we don't need to deal with IPC.
      */

    // Retorna a instância do LocalService para poder acessar os métodos públicos
    public class LocalBinder extends Binder {
        LocalService getService() {
            return LocalService.this;
        }
    }

    // Método para vincular o serviço
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // Método que inicia o serviço
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            setupChat();
            myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        return START_NOT_STICKY;
    }

    public void connectDevice(Intent data, boolean secure){
        // Obtém o MAC do dispositivo
        String address = data.getExtras().getString(DeviceList.EXTRA_DEVICE_ADDRESS);

        // Obtém o objeto BluetoothDevice
        BluetoothDevice device = myBluetoothAdapter.getRemoteDevice(address);

        // Tenta a conexão com o dispositivo
        mChatService.connect(device, secure);
    }

    public void disconnectDevice(){
        // Envia a mensagem "Sair" para o dispositivo
        mChatService.write("Sair".getBytes());

        // Para a conexão
        mChatService.stop();

        // Altera o estado de conexão para não conectado
        connection = false;

        // Para o serviço
        stopSelf();
    }

    // Retorna o estado da conexão
    public boolean isConnected(){
        return connection;
    }

    // Envia dados para o dispositivo conectado
    public void sendMessage(String message){
        serviceData.delete(0,serviceData.length());

        // Verifica se há conexão
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED){
            Toast.makeText(getApplicationContext(), "Não está conectado a um dispositivo", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verifica se há alguma mensagem para ser enviada
        if (message.trim().length() > 0){
            mChatService.write(message.getBytes());
        }
    }

    // Se houver alguma conexão, envia os dados recebidos do dispositivo conectado
    public void sendData(){
        if(connection){
            sendBroadcastMessage(allData);
        }
    }

    // Inicia a classe de conexão
    private void setupChat() {
        if (mChatService == null){
            mChatService = new BluetoothChatService(this, mHandler);
        }
    }

    /** Faz a comunicação entre o serviço e a classe principal
     *  associando a chave "DATA" o valor da string,
     *  que é o valor passado como parâmetro quando o método for chamado
     */
    private void sendBroadcastMessage(String string){
        if (string != null) {
            Intent intent = new Intent(ACTION_BLUETOOTH_BROADCAST);
            intent.putExtra(DATA, string);
            sendBroadcast(intent);
        }
    }

    /** O Handler é definido para receber as informações do BluetoothChatService
     *  para atualização do status da conexão
     *  e quando algum dado for recebido na classe BluetoothChatService,
     *  para ser enviado ao serviço
     */

    private final Handler mHandler = new Handler(){
        @Override
        public void handleMessage (Message msg){
            switch (msg.what){
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1){
                        case BluetoothChatService.STATE_CONNECTED:
                            Toast.makeText(LocalService.this, "Conectado!", Toast.LENGTH_LONG).show();
                            connection = true;
                            serviceData.delete(0,serviceData.length());
                            sendBroadcastMessage("conectado");
                            sendBroadcastMessage("");
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            Toast.makeText(LocalService.this, "Conectando...", Toast.LENGTH_LONG).show();
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            Toast.makeText(LocalService.this, "Não Conectado!", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;

                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;

                    String writeMessage = new String(writeBuf);
                    break;

                case Constants.MESSAGE_READ:
                    String received = (String) msg.obj;

                    bluetoothData.append(received);

                    int endInformation = bluetoothData.indexOf("}");

                    if (endInformation > 0){
                        String completeData = bluetoothData.substring(0, endInformation);
                        int informationLenght = completeData.length();

                        if(completeData.length() > 2 && (completeData.charAt(0) == '{' || completeData.charAt(1) == '{'  || completeData.charAt(2) == '{')){
                            String finalData = bluetoothData.substring(1, informationLenght);

                            serviceData.append(finalData);
                            allData = serviceData.substring(0,serviceData.length());
                            sendBroadcastMessage(allData);

                            if (finalData.contains("alerta")){
                                Intent intent = new Intent(LocalService.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                intent.putExtra("mensagem", "alerta");
                                int id = 1;
                                PendingIntent pi = PendingIntent.getActivity(LocalService.this, 0, intent, 0);
                                Notification notification = new Notification.Builder(getBaseContext())
                                        .setContentTitle("De: Muleta")
                                        .setContentText("alerta").setSmallIcon(R.mipmap.ic_launcher)
                                        .setVibrate(new long[]{0, 500})
                                        .setContentIntent(pi).build();

                                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                notification.flags |= Notification.FLAG_AUTO_CANCEL;

                                notificationManager.notify(id, notification);
                                try{
                                    Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                    Ringtone tone = RingtoneManager.getRingtone(LocalService.this, sound);
                                    tone.play();
                                } catch (Exception e){

                                }
                            }
                        }
                        bluetoothData.delete(0, bluetoothData.length());
                    }
                    break;

                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    //Toast.makeText(LocalService.this, mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
}