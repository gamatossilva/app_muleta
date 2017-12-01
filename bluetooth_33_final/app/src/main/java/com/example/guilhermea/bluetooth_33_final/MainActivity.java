package com.example.guilhermea.bluetooth_33_final;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ACTIVATION = 1;
    private static final int REQUEST_CONECTION = 2;

    private String fileName;

    private final SimpleDateFormat dataFormater = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    private BluetoothAdapter myBluetoothAdapter = null;

    private Button btnConection, btnSent;
    private EditText etText;
    private TextView tvReceivedText;
    private TextView tvSentText;

    private boolean connection = false;

    private boolean isReceiverResgistered;
    LocalService mService;

    @Override
    public void onResume(){
        super.onResume();
        // Registra o serviço se este não estiver registrado, para receber os valores enviados pelo serviço
        if(!isReceiverResgistered){
            registerReceiver(mHandleMessageReceiver, new IntentFilter(LocalService.ACTION_BLUETOOTH_BROADCAST));
            isReceiverResgistered = true;
        }
        Intent intent = new Intent(this, LocalService.class);
        // Vincula o serviço a atividade
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mConnection != null){
            // Desvincula o serviço
            unbindService(mConnection);
        }
    }

    /** Método para ler o último arquivo editado
     *  Associando a chave "Constants.RECORDED_FILE" o valor
     *  do diretório e nome do arquivo em questão
     */
    public void fReadText(View view){
        File lastFile = getLatestFilefromDir(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
        if(lastFile != null) {
            Intent it = new Intent(this, ReadActivity.class);
            it.putExtra(Constants.STORAGE_TYPE, Constants.Type.INTERNAL);
            it.putExtra(Constants.RECORDED_FILE, lastFile.toString());
            startActivity(it);
        } else{
            Toast.makeText(getApplicationContext(), "Nenhum arquivo para ser lido", Toast.LENGTH_SHORT).show();
        }
    }

    /** Salva todos os dados presentes em "tvReceivedText"
     *  em um arquivo
     */
    public void fSaveText(View view){
        String text = tvReceivedText.getText().toString();
        String path;
        try{
            path = saveData(text);
            Toast.makeText(getApplicationContext(), "Arquivo salvo em: " + path, Toast.LENGTH_LONG).show();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConection = (Button) findViewById(R.id.btnConection);
        btnSent = (Button) findViewById(R.id.btnSent);
        etText = (EditText) findViewById(R.id.etText);
        tvReceivedText = (TextView) findViewById(R.id.tvReceivedText);
        tvSentText = (TextView) findViewById(R.id.tvSentText);

        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (myBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Seu dispositivo não possui bluetooth", Toast.LENGTH_LONG).show();
        } else if (!myBluetoothAdapter.isEnabled()) {
            // Solicita a ativação do Bluetooth
            Intent activateBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(activateBluetooth, REQUEST_ACTIVATION);
        }

        btnConection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connection = mService.isConnected();
                if (connection) {
                    // Desconectar do dispositivo

                    tvSentText.setText("Sair");
                    fSaveText(v);
                    mService.disconnectDevice();
                    btnConection.setText("Conectar");
                } else {
                    // Conectar ao dispositivo
                    Intent openList = new Intent(MainActivity.this, DeviceList.class);
                    startActivityForResult(openList, REQUEST_CONECTION);
                    tvReceivedText.setText("");
                }
            }
        });

        // Envia os dados contidos em "etText"
        btnSent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvSentText.setText(etText.getText().toString());
                mService.sendMessage(etText.getText().toString());
                etText.setText("");
                tvReceivedText.setText("");
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        checkWritePermissions();
        // Inicia o serviço para poder ser executado em segundo plano
        Intent intent = new Intent(this, LocalService.class);
        startService(intent);
        // Vincula o serviço a esta atividade
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(isReceiverResgistered) {
            try {
                // Cancela o registro
                unregisterReceiver(mHandleMessageReceiver);
            } catch (IllegalArgumentException e){
            }
            isReceiverResgistered = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ACTIVATION:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "Bluetooth foi ativado", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Bluetooth não foi ativado, o aplicativo será encerrado", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;

            case REQUEST_CONECTION:
                if (resultCode == Activity.RESULT_OK) {
                    // Tenta se conectar ao dispositivo
                    mService.connectDevice(data, false);

                    // Registra o serviço
                    registerReceiver(mHandleMessageReceiver, new IntentFilter(LocalService.ACTION_BLUETOOTH_BROADCAST));
                    isReceiverResgistered = true;
                    connection = mService.isConnected();
                    if(connection){
                        btnConection.setText("Desconectar");
                    }
                }
                break;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Obtém a instância do serviço
            LocalService.LocalBinder binder = (LocalService.LocalBinder) service;

            mService = binder.getService();
            connection = mService.isConnected();
            mService.sendData();
            if (connection){
                btnConection.setText("Desconectar");
            } else {
                btnConection.setText("Conectar");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    // Método que cria o arqvuio para que os dados possam ser escritos nele
    private String saveData(String text) throws IOException{
        String status = Environment.getExternalStorageState();

        if (!status.equals(Environment.MEDIA_MOUNTED)){
            throw new IOException("SD Card não montado ou não disponpivel!!!");
        }
        fileName = String.format(Constants.FILE_NAME, dataFormater.format(new Date()));
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
        PrintWriter pw = new PrintWriter(file);

        try{
            pw.print(text);
        } finally {
            pw.close();
        }
        MediaScannerConnection.scanFile(this, new String[] {file.getPath().toString()}, null, null);
        return file.getPath();
    }

    // Verifica se foi dada permissão de escrita pelo usuário
    private void checkWritePermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.WRITE_EXTERNAL_STORAGE");
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
            }
        }else{
        }
    }

    // Procura na pasta Downloads pelo último arquivo editado
    private File getLatestFilefromDir(String dirPath){
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return null;
        }

        File lastModifiedFile = files[0];
        for (int i = 1; i < files.length; i++) {
            if (lastModifiedFile.lastModified() < files[i].lastModified()) {
                lastModifiedFile = files[i];
            }
        }
        return lastModifiedFile;
    }

    /** Método para receber os dados do serviço
     *  e os enviar ao usuário
     */
    private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getExtras().getString(LocalService.DATA);
            if(message.contains("conectado")){
                btnConection.setText("Desconectar");
                tvSentText.setText("Entre com a sua massa");
                tvReceivedText.setText("");
            }else {
                tvReceivedText.setMovementMethod(new ScrollingMovementMethod());
                tvReceivedText.setText(message.toString());
            }
        }
    };
}
