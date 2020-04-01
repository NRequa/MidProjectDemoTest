package com.example.bluetoothcode;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final String hardwareModule = "Galaxy Tab A";
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final String tag = "BLE";
    private BluetoothDevice hardwareBT = null;
    private static BluetoothAdapter mAdapter;

    Handler bluetoothRead;
    final int DATA_IN = 0;
    private StringBuilder dataString = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find UI elements we want to mess with
        final TextView deviceName = (TextView)  findViewById(R.id.DeviceName);
        final TextView deviceAddress = (TextView) findViewById(R.id.devciceAddress);
        final TextView message = (TextView) findViewById(R.id.BLmsg);

        // Create handler to update UI and store data
        bluetoothRead = new Handler() {
            public void handleMessage(Message msg){
                if(msg.what == DATA_IN){
                    // Get all the
                    String sensorData = (String) msg.obj;
                    dataString.append(sensorData);

                    // Parse dataString for our sensor data
                    message.setText(dataString);
                    //dataString.delete(0, dataString.length());
                }
            }
        };

        // Get adapter
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mAdapter = mBluetoothAdapter;
        BluetoothDevice mDevice;

        // Check if support bluetooth
        if(mBluetoothAdapter == null){
            Log.e(tag, "Bluetooth not supported");
        }

        // Check if BT enabled, request enable if not
        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        // Get List of paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        List<String> deviceList = new ArrayList<String>();

        // Find our hardware device
        for(BluetoothDevice device : pairedDevices){
            Log.d(tag, device.getName());
            deviceList.add(device.getName());

            if(device.getName().equals(hardwareModule)){
                hardwareBT = device;
                deviceName.setText(device.getName());
                deviceAddress.setText(device.getAddress());
            }
        }

        // Thread that opens socket to device and starts reading messages.
        ConnectThread makeConnectionThread = new ConnectThread(hardwareBT);
        makeConnectionThread.start();


    }



    // Connection thread for bluetooth device
    private class ConnectThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device){
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(tag, "Socket creation failed", e);
            }

            mmSocket = tmp;
        }

        public void run() {
            mAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
                Log.d(tag, "Connected to socket");
            } catch (IOException connectException){
                // Can't connect, try to close socket
                try {
                    mmSocket.close();
                } catch (IOException closeException){
                    Log.e(tag, "Coudn't close socket.", closeException);
                }
                // Should do something here, say restart app or loop a search?
                Log.d(tag, "Couldn't connect to socket");
                return;
            }

            manageMyConnectedSocket(mmSocket);
        }

        public void cancel(){
            try {
                mmSocket.close();
            } catch(IOException e){
                Log.e(tag, "Couldn't close socket", e);
            }
        }

        private void manageMyConnectedSocket(BluetoothSocket socket){
            ConnectedThread receiveThread = new ConnectedThread(socket);
            receiveThread.start();
        }
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer;

        public ConnectedThread(){
            mmSocket = null;
            mmInStream = null;
            mmOutStream = null;
        };

        public ConnectedThread(BluetoothSocket socket){
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e){
                Log.e(tag, "Error getting input stream.", e);
            }

            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(tag, "Error getting output stream.", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

    /*
        public void run() {
            while(true) {
                String data = "{timestamp, heartbeat, temp, sp02}";
                Message msg = bluetoothRead.obtainMessage(DATA_IN, data);
                bluetoothRead.sendMessage(msg);
            }
        }
*/

        public void run(){
            mmBuffer = new byte[1024];
            int numBytes;

            while(true) {
                try {
                    numBytes = mmInStream.read(mmBuffer);
                    String stringMessage = new String(mmBuffer, 0, numBytes);
                    Message readMsg = bluetoothRead.obtainMessage(DATA_IN, numBytes, -1, stringMessage);
                    bluetoothRead.sendMessage(readMsg);
                } catch(IOException e){
                    Log.d(tag, "Input stream disconnected", e);
                    break;
                }
            }
        }



    }
}
