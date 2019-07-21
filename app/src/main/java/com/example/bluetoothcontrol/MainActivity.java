package com.example.bluetoothcontrol;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static app.akexorcist.bluetotohspp.library.BluetoothState.REQUEST_ENABLE_BT;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private String[] perms = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN};
    private Button button;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice device;
    private InputStream inputStream;
    private OutputStream OutputStream;
    private static String TAG = "MainActivity";
    private String mDeviceAddress = "00:15:A6:00:4D:E6";
    public static UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private EditText etInput;
    private static final boolean D = true;
    private int mState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RxPermissions rxPermissions = new RxPermissions(this);
        button = findViewById(R.id.button);
        etInput = findViewById(R.id.editText);
        button.setOnClickListener(this);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
        }
        return;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String actino = intent.getAction();
            //当发现找到设备时
            if (BluetoothDevice.ACTION_FOUND.equals(actino)) {
                //从Intent获取BluetoothDevice对象
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //如果它已经配对，请跳过它，因为它已经被列出了
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    //未配对设备
                    Toast.makeText(MainActivity.this, "未配对设备", Toast.LENGTH_SHORT).show();
                } else {
                    //显示已经配对过的设备
                    //     TextView tvPaired = findViewById(R.id.tvPaired);
                    //     tvPaired.setVisibility(View.VISIBLE);
                    //     tvPaired.setText(device.getName() + "\n" + device.getAddress());
                    //     mDeviceAddress = device.getAddress();
                    Toast.makeText(MainActivity.this, device.getName() + "\n" + device.getAddress(), Toast.LENGTH_SHORT).show();
                }
                Log.i(TAG, "NAME:" + device.getName() + "ADDRESS:" + device.getAddress());
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(actino)) {
                Log.i(TAG, "search finish!");
                Toast.makeText(MainActivity.this, "search finish!", Toast.LENGTH_SHORT).show();
            }
        }
    };


    @Override
    public void onClick(View view) {
        if (bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            // Toast.makeText(MainActivity.this, "蓝牙已打开", Toast.LENGTH_SHORT).show();
            //如果我们已经发现了，请停止它
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            //搜索蓝牙
            bluetoothAdapter.startDiscovery();
            // write(etInput.getText().toString().getBytes());
        } else {
            //Log.e(TAG, "蓝牙没打开");
            Toast.makeText(MainActivity.this, "蓝牙没打开", Toast.LENGTH_SHORT).show();
        }
        ConnectThread();
        //   sendData(etInput.getText().toString().getBytes());
    }

    public void ConnectThread() {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(mDeviceAddress);
        try {
            BluetoothSocket bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            Log.d(TAG, "开始连接...");
            if (bluetoothSocket != null) {
                bluetoothSocket.connect();
                try {
                    OutputStream outputStream = bluetoothSocket.getOutputStream();
                    outputStream.write(etInput.getText().toString().getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
