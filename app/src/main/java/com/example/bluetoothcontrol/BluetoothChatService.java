package com.example.bluetoothcontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothChatService {
    // Debugging
    private static final String TAG = "BluetoothChatService";
    private static final boolean D = true;

    //创建服务器套接字时SDP记录的名称
    private static final String NAME = "BluetoothCom";

    //此应用程序的唯一UUID //蓝牙串行板
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //提示：如果要连接蓝牙串行板，请尝试使用众所周知的SPP UUID 00001101-0000-1000-8000-00805F9B34FB。
    //但是，如果您要连接到Android对等端，请生成您自己的唯一UUID。
    //会员字段
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // 指示当前连接状态的常量
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     *构造函数。 准备新的BluetoothChat会话。
     * @param context UI活动上下文
     * @param handler 一个Handler，用于将消息发送回UI Activity
     */
    public BluetoothChatService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }



    /**
     *设置聊天连接的当前状态
     * @param state 定义当前连接状态的整数
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        //将新状态提供给处理程序，以便UI活动可以更新

    }



    /**
     *启动聊天服务。 具体来说，启动AcceptThread开始
     *侦听（服务器）模式下的会话。 由Activity onResume（）调用*/

    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        //取消任何尝试建立连接的线程
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        //取消任何尝试建立连接的线程
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        //启动线程以侦听BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        setState(STATE_LISTEN);
    }

    //启动线程以侦听BluetoothServerSocket
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        //取消任何尝试建立连接的线程
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        //取消当前正在运行连接的任何线程
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        //启动线程以连接给定设备
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     *启动ConnectedThread以开始管理蓝牙连接
     * @param socket 连接的BluetoothSocket
     * @param device 已连接的BluetoothDevice
     */

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        //取消完成连接的线程
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        //取消当前正在运行连接的任何线程
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        //取消接受线程，因为我们只想连接到一个设备
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}

        //启动线程来管理连接并执行传输
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        //将连接设备的名称发送回UI活动
        setState(STATE_CONNECTED);
    }

    /**
     * 停止所有线程
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        setState(STATE_NONE);
    }

    /**
     *以不同步的方式写入ConnectedThread
     * @param out 要写入的字节
     * @see ConnectedThread #write（byte []）
     */

    public void write(byte[] out) {
        //创建临时对象
        ConnectedThread r;
        //同步ConnectedThread的副本
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        //执行写入不同步
        r.write(out);
    }

    /**
     *指示连接尝试失败并通知UI活动。
     */
    private void connectionFailed() {
        setState(STATE_LISTEN);

        //将失败消息发送回活动
    }

    /**
     *表明连接已丢失并通知UI活动。
     */


    private void connectionLost() {
        setState(STATE_LISTEN);

        //将失败消息发送回活动

    }

    /**
     *此线程在侦听传入连接时运行。 它表现得很好
     *像服务器端客户端。 它会一直运行，直到接受连接
     *（或直到取消）。
     */

    private class AcceptThread extends Thread {
        //本地服务器套接字
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            //创建一个新的侦听服务器套接字
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D) Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            BluetoothSocket socket = null;

            //如果我们没有连接，请收听服务器套接字
            while (mState != STATE_CONNECTED) {
                try {
                    //这是一个阻止调用，只会返回一个
                    //成功连接或异常
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                   // Log.e(TAG, "accept() failed", e);
                    break;
                }

                //如果接受了连接
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            //情况正常 启动连接的线程。
                            connected(socket, socket.getRemoteDevice());
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            //未准备好或已连接。 终止新套接字。
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }
            if (D) Log.i(TAG, "END mAcceptThread");
        }

        public void cancel() {
            if (D) Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }


    /**
     *此线程在尝试进行传出连接时运行
     *带有设备。 它直接通过; 连接也是
     *成功或失败。
     */

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            //获取与之连接的BluetoothSocket
            //给出了BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            //始终取消发现，因为它会降低连接速度
            mAdapter.cancelDiscovery();

            //建立与BluetoothSocket的连接
            try {
                //这是一个阻止调用，只会返回一个
                //成功连接或异常
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                //关闭套接字
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                //启动服务以重新启动侦听模式
                BluetoothChatService.this.start();
                return;
            }

            //重置ConnectThread，因为我们已经完成了
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }

            //启动连接的线程
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     *此线程在与远程设备连接期间运行。
     *它处理所有传入和传出传输。
     */

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //获取BluetoothSocket输入和输出流
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            byte[] buffer2 = new byte[1024];
            int bytes;
            int i;
            //连接时继续收听InputStream
            while (true) {
                try {
                    //从InputStream中读取
                    bytes = mmInStream.read(buffer);
                    if(bytes>0){
                    buffer2=buffer.clone();
                    for(i=0;i<buffer.length;i++)buffer[i]=0;
                        //将获取的字节发送到UI活动

                    }
                } catch (IOException e) {
                    //Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         *写入连接的OutStream。
         * @param buffer 要写入的字节
         */

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);


            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
