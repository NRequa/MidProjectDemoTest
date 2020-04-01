package com.example.bluetoothhardwareemulator;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final String NAME = "SensorSend";
    private final String TAG = "BLE";
    private static BluetoothAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get adapter
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mAdapter = mBluetoothAdapter;
        BluetoothDevice mDevice;

        // Check if support bluetooth
        if(mBluetoothAdapter == null){
            Log.e(TAG, "Bluetooth not supported");
        }

        // Check if BT enabled, request enable if not
        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        AcceptThread findConnection = new AcceptThread();
        findConnection.run();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private OutputStream outData;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            while(true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e){
                    Log.e(TAG, "Socket accept() failed", e);
                }

                if(socket != null){
                    while(true) {
                        sendData(socket);
                    }
                }
            }
        }

        public void sendData(BluetoothSocket socket){
            try{
                outData = socket.getOutputStream();
                String sensorData = "{timestamp, heartrate, temp, spo2}";
                outData.write(sensorData.getBytes());
                Log.d(TAG, "Sent sensor data: " + sensorData);
            } catch(IOException e){
                Log.e(TAG, "Socket connection failed", e);
            }
        }
    }


}
