package com.example.lenovo.myaexg;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * wifi连接
 */

public class WifiChatService {
    // Debugging
    private static final String TAG = "WifiChatService";
    private static final boolean D = true;

    // SocketIp
    private String IP = "192.168.21.100";
    private int Port = 8899;

    // Member fields
    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    //什么也没做
    private static final int STATE_NONE = 0;
    //正在听取传入
    public static final int STATE_LISTEN = 1;
    //现在开始传出
    private static final int STATE_CONNECTING = 2;
    //远程连接完成
    private static final int STATE_CONNECTED = 3;

    /**
     * 构造函数。 准备新的WifiChat会话。
     */
    public WifiChatService(Handler handler) {
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * 设置聊天连接的当前状态
     */
    private synchronized void setState(int state) {
        if (D) {
            Log.d(TAG, "setState() " + mState + " -> " + state);
        }
        mState = state;
        //将新状态提供给处理程序，以便UI活动可以更新
        mHandler.obtainMessage(BluetoothChat.MESSAGE_STATE_CHANGE, state, -1)
                .sendToTarget();
    }

    /**
     * 返回当前连接状态。
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * 启动聊天服务。 具体来说，启动AcceptThread开始
     * 侦听（服务器）模式下的会话。 由Activity onResume（）调用
     */
    public synchronized void start() {
        if (D) {
            Log.d(TAG, "start");
        }

        //取消尝试建立连接的任何线程
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        //取消当前正在运行连接的任何线程
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

    }

    /**
     * 启动ConnectThread以启动与远程设备的连接。
     */
    public synchronized void connect(String IP, int port) {
        if (D) {
            Log.d(TAG, "connect to:\n IP:" + IP + "\n+port:" + port);
        }

        //取消尝试建立连接的任何线程
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        //取消当前正在运行连接的任何线程
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        //启动线程以连接给定设备
        mConnectThread = new ConnectThread(IP, port);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * 启动ConnectedThread以开始管理蓝牙连接
     */
    public synchronized void connected(Socket socket, String IP, int port) {

        if (D) {
            Log.d(TAG, "connected");
        }

        //取消完成连接的线程
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        //取消当前正在运行连接的任何线程
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        //启动线程以管理连接并执行传输
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        //将已连接设备的名称发送回UI活动
        Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.DEVICE_NAME, "IP:" + IP + "\nPort:" + port);
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) {
            Log.d(TAG, "stop");
        }
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * 以不同步的方式写入ConnectedThread
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object  创建临时对象
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread  同步ConnectedThread的副本
        synchronized (this) {
            if (mState != STATE_CONNECTED)
                return;
            r = mConnectedThread;
        }
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     * 指示连接尝试失败并通知UI活动。
     */
    private void connectionFailed() {
        setState(STATE_NONE);

        //将失败消息发送回活动
        Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     * 指示连接已丢失并通知UI活动。
     */
    private void connectionLost() {
        setState(STATE_NONE);

        //将失败消息发送回活动
        Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * 尝试与设备建立传出连接时，此线程会运行。 它直接通过; 连接成功或失败。
     * This thread runs while attempting to make an outgoing connection with a
     * device. It runs straight through; the connection either succeeds or
     * fails.
     */
    private class ConnectThread extends Thread {
        private Socket mmSocket = null;
        private String IP;
        private int port;

        private ConnectThread(String IP, int port) {
            this.IP = IP;
            this.port = port;
        }

        public void run() {
            if (D) {
                Log.i(TAG, "BEGIN mConnectThread");
            }
            setName("ConnectThread");

            try {
                //Socket连接
                mmSocket = new Socket(IP, port);
            } catch (IOException e) {
                //连接失败、关闭socket
                connectionFailed();
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                WifiChatService.this.start();
                return;
            }

            //重置ConnectThread因为我们已经完成了
            synchronized (WifiChatService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            //启动连接的线程
            connected(mmSocket, IP, port);
        }

        private void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                if (D) Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * 此线程在与远程设备连接期间运行。 它处理所有
     * 传入和传出传输。
     */
    private class ConnectedThread extends Thread {

        private final Socket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private boolean kill = false;

        public ConnectedThread(Socket socket) {
            if (D) {
                Log.d(TAG, "create ConnectedThread");
            }
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //获取BluetoothSocket输入和输出流
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                if (D) {
                    Log.e(TAG, "temp sockets not created", e);
                }
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            if (D) Log.i(TAG, "BEGIN mConnectedThread");
            setName("ConnectedThread");

            // Keep listening to the InputStream while connected
            //连接时继续收听InputStream
            while (!kill) {
                byte[] buffer = new byte[1024];
                int bytes;
                try {
                    // Read from the InputStream 从InputStream中读取
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI Activity
                    //将获取的字节发送到UI活动
                    mHandler.obtainMessage(BluetoothChat.MESSAGE_READ, bytes,
                            -1, buffer).sendToTarget();
                } catch (IOException e) {
                    if (D) Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * 写入已连接的OutStream。
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                // Share the sent message back to the UI Activity
                //将发送的消息共享回UI活动
                mHandler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1,
                        buffer).sendToTarget();
            } catch (IOException e) {
                if (D) Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                kill = true;
                mmSocket.close();
            } catch (IOException e) {
                if (D) Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
