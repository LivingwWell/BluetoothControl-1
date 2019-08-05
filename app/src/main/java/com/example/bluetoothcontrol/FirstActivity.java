package com.example.bluetoothcontrol;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.hjq.permissions.OnPermission;
import com.hjq.permissions.XXPermissions;

import java.util.ArrayList;
import java.util.List;


public class FirstActivity extends AppCompatActivity implements View.OnClickListener {
    private Button b_seek1, b_seek2, b_set, b_more;
    private RecyclerView recyclerView;
    private List<HomeItem> list;
    private BaseQuickAdapter adapter;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice device;
    public String mDeviceAddress = "00:15:A6:00:4D:C5";
    private static String TAG = "FirstActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ex);
        b_seek1 = findViewById(R.id.b1);
        b_seek2 = findViewById(R.id.b2);
        //b_set= findViewById(R.id.b3);
        //b_more= findViewById(R.id.b4);
        recyclerView = findViewById(R.id.recycle);
        //点击事件
        b_seek1.setOnClickListener(this);
        b_seek2.setOnClickListener(this);
        //b_set.setOnClickListener(this);
        //b_more.setOnClickListener(this);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(receiver, intentFilter);
        requestPermission();
        list = new ArrayList<>();
        recyclerView.addItemDecoration(new DividerItemDecoration(FirstActivity.this, DividerItemDecoration.VERTICAL));//分割线
        //管理器
        recyclerView.setLayoutManager(new LinearLayoutManager(FirstActivity.this,
                RecyclerView.VERTICAL, false));
    }

        //数据



    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.b1:
                if (bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    // startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    Toast.makeText(FirstActivity.this, "蓝牙已打开", Toast.LENGTH_SHORT).show();
                    //如果我们已经发现了，请停止它
                    if (bluetoothAdapter.isDiscovering()) {
                        bluetoothAdapter.cancelDiscovery();
                    }
                    //搜索蓝牙
                    bluetoothAdapter.startDiscovery();
                    // write(etInput.getText().toString().getBytes());
                } else {
                    //Log.e(TAG, "蓝牙没打开");
                    Toast.makeText(FirstActivity.this, "蓝牙没打开", Toast.LENGTH_SHORT).show();
                }
                //搜索
                break;

        }
    }


       /* switch (v.getId()){
            case R.id.b2:
                Intent intent  = new Intent(view.getContext(),BluetoothActivity.class);
        view.getContext().startActivity(intent);}
                break;
        }
        switch (v.getId()){
            case R.id.b3:
                //设置 页面切换
                break;
        }
        switch (v.getId()){
            case R.id.b4:
                //更多 页面切换
                break;
        }*/


    //权限
    public void requestPermission() {
        String[] PermissionGroup = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH};


        XXPermissions.with(this)
                // .constantRequest() //可设置被拒绝后继续申请，直到用户授权或者永久拒绝...闪退
                // .permission(Permission.SYSTEM_ALERT_WINDOW, Permission.REQUEST_INSTALL_PACKAGES) //支持请求6.0悬浮窗权限8.0请求安装权限
                .permission(PermissionGroup) //不指定权限则自动获取清单中的危险权限
                .request(new OnPermission() {
                    @Override
                    public void hasPermission(java.util.List<String> granted, boolean isAll) {
                    }

                    @Override
                    public void noPermission(List<String> denied, boolean quick) {
                    }
                });
    }

    //接收
    private final BroadcastReceiver receiver;

    {

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intent) {
                String actino = intent.getAction();
                //当发现找到设备时
                if (BluetoothDevice.ACTION_FOUND.equals(actino)) {
                    //从Intent获取BluetoothDevice对象
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //如果它已经配对，请跳过它，因为它已经被列出了
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        //未配对设备
                        Toast.makeText(FirstActivity.this, "未配对设备1", Toast.LENGTH_SHORT).show();
                    } else {
                        // 显示已经配对过的设备
                        mDeviceAddress = device.getAddress();
                        //将数据填充到实体类HomeItem,再将实体类添加到list





                       // adapter = new recyclerviewAdapter(R.layout.item_recyclerview, list);    //初始化适配器
                       // recyclerView.setAdapter(adapter);     //设置适配器
                        //recyclerView.addItemDecoration(new DividerItemDecoration(FirstActivity.this, DividerItemDecoration.VERTICAL));//分割线
                        //设置点击事件,跳转到BluetoothActivity
                        adapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
                            @Override
                            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                                Intent intent1 = new Intent(FirstActivity.this, BluetoothActivity.class);
                                startActivity(intent1);
                                //adapter.notifyDataSetChanged();

                            }
                        });
                        //管理器
                        recyclerView.setLayoutManager(new LinearLayoutManager(FirstActivity.this,
                                RecyclerView.VERTICAL, false));
                        Toast.makeText(FirstActivity.this, device.getName() + "\n" + device.getAddress(), Toast.LENGTH_SHORT).show();
                    }
                    //list = new ArrayList<>();
                    HomeItem homeItem = new HomeItem();
                    homeItem.setName(device.getName() + "\n" + device.getAddress());
                    list.add(homeItem);

                   adapter = new recyclerviewAdapter(R.layout.item_recyclerview, list);    //初始化适配器
                    recyclerView.setAdapter(adapter);     //设置适配器
                    recyclerView.addItemDecoration(new DividerItemDecoration(FirstActivity.this, DividerItemDecoration.VERTICAL));//分割线
                    //设置点击事件,跳转到BluetoothActivity

                    adapter.notifyDataSetChanged();
                    Log.i(TAG, "NAME:" + device.getName() + "ADDRESS:" + device.getAddress());
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(actino)) {
                    Log.i(TAG, "search finish!");
                    Toast.makeText(FirstActivity.this, "search finish11!", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }


}




