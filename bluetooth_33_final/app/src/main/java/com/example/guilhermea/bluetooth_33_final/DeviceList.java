package com.example.guilhermea.bluetooth_33_final;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

public class DeviceList extends Activity{
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private BluetoothAdapter mBTAdapter;

    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        setResult(Activity.RESULT_CANCELED);

        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });
        // Inicializa o array adapters. Um para os dispositivo pareados
        // outro para os descobertos
        ArrayAdapter<String> pairedDevicesArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);


        // Lista os dispositivos pareados
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Lista os dispositivos descobertos
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Registra quando um dispositivo for descoberto
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Registra quando a busca se encerra
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Obtém o adaptador Bluetooth local
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = mBTAdapter.getBondedDevices();

        // Se houver dispositivos pareados, todos serão adicionados ao array adapter
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = "Nenhum dispositivo pareado".toString();
            pairedDevicesArrayAdapter.add(noDevices);
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        // Cancela a descoberta de dispositivos
        if(mBTAdapter != null){
            mBTAdapter.cancelDiscovery();
        }
        // Cancela o registro
        this.unregisterReceiver(mReceiver);
    }

    // Inicia a descoberta de dispositivos
    private void doDiscovery(){
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        checkBTPermissions();
        // Cancela se já houver alguma outra descoberta sendo executada
        if (mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
        }
        mBTAdapter.startDiscovery();
    }

    // Verifica se o usuário concedeu permissão de acesso ao Bluetooth
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
        }
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener(){
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3){
            // Cancela outras descobertas
            mBTAdapter.cancelDiscovery();

            // Obtém o MAC do dispositivo
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Associa o MAC do dispositivo a chave "EXTRA_DEVICE_ADDRESS"
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Envia o resultado de volta a UI e encerra a atividade
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothDevice.ACTION_FOUND.equals(action)){

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            // Se o dispositivo já estiver listado como pareado, não o lista novamente
            if (device.getBondState() != BluetoothDevice.BOND_BONDED){
                mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }

        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){

            if (mNewDevicesArrayAdapter.getCount() == 0){
                String noDevices = "Nenhum dispositivo encontrado".toString();
                mNewDevicesArrayAdapter.add(noDevices);
            }
        }
        }
    };
}
