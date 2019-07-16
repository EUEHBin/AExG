/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.lenovo.myaexg;

import android.Manifest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;

import android.view.MenuItem;
import android.view.View;

import android.view.ViewGroup.LayoutParams;

import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is the main Activity that displays the current chat session.
 * Main Activity
 */
public class BluetoothChat extends AppCompatActivity {
    // Debugging
    private static final String TAG = "BluetoothChat";
    //是否打印log
    private static final boolean D = true;
    private boolean SendingData = false;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Bluetooth variables
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    // 从BluetoothChatService处理程序发送的消息类型
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;


    // 从BluetoothChatService处理程序收到的Key
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Layout Views
    private TextView mTitle;
    private Button mSendButton;

    private String mConnectedDeviceName = null;

    private BluetoothAdapter mBluetoothAdapter = null;
    // BluetoothChatService 成员对象
    private BluetoothChatService mChatService = null;

    /* Packet construction 包构造 */
    private static final int PACKET_ID_LEN = 1;	/* packet number length 包号长度  */
    private static final int STATUS_BYTES_LEN = 3;
    private static final int CHANNELS = 2;
    private static final int BYTES_PER_CHANNEL = 3;
    private static final int MAX_NUM_PACKET_BYTES = PACKET_ID_LEN + STATUS_BYTES_LEN + (CHANNELS * BYTES_PER_CHANNEL);

    /* Data conversion states  数据转换状态 */
    private static int data_state;
    private static final int INVALID_DATA = 0;
    private static final int FIRST_START_BYTE = 1;
    private static final int VALID_DATA = 3;

    /* Data conversion variables  数据转换变量 */
    private static int packet_count = 0;
    private static int byte_count = 0;
    private static int previous_packet_number = 0;
    private static int current_x_value = 0;
    private static int update_count = 0;
    int ch = 0;

    /* Sampling Constants  采样常数 */
    private static final int SPS = 250;
    private static final double Fs = (double) SPS;
    private static final double Ts = 1 / Fs;

    /* LPF Cut-off Variables  LPF截止变量 */
    private static final double[] LPF1_options = {0, 8.0, 10.0, 15.0, 10000.0};
    private static int LPF1_options_ptr = 2;
    private static double LPF1_Fc = LPF1_options[LPF1_options_ptr];
    private static double LPF1_Tc = 1 / LPF1_Fc;
    private static double LPF1_RC = LPF1_Tc / (2 * Math.PI);
    private static double LPF1_ALPHA = Ts / (LPF1_RC + Ts);

    private static final double[] LPF2_options = {0, 8.0, 10.0, 15.0, 10000.0};
    private static int LPF2_options_ptr = 2;
    private static double LPF2_Fc = LPF2_options[LPF2_options_ptr];
    private static double LPF2_Tc = 1 / LPF2_Fc;
    private static double LPF2_RC = LPF2_Tc / (2 * Math.PI);
    private static double LPF2_ALPHA = Ts / (LPF2_RC + Ts);

    /* HPF Cut-off Variables  HPF截止变量 */
    private static final double[] HPF1_options = {0, 0.1, 0.5, 1.5, 5.0};
    private static int HPF1_options_ptr = 3;
    private static double HPF1_Fc = HPF1_options[HPF1_options_ptr]; //TODO: LPF_options ??
    private static double HPF1_Tc = 1 / HPF1_Fc;
    private static double HPF1_RC = HPF1_Tc / (2 * Math.PI);
    private static double HPF1_ALPHA = HPF1_RC / (HPF1_RC + Ts);

    private static final double[] HPF2_options = {0, 0.1, 0.5, 1.5, 5.0};
    private static int HPF2_options_ptr = 3;
    private static double HPF2_Fc = HPF2_options[HPF2_options_ptr]; //TODO: LPF_options ??
    private static double HPF2_Tc = 1 / HPF2_Fc;
    private static double HPF2_RC = HPF2_Tc / (2 * Math.PI);
    private static double HPF2_ALPHA = HPF2_RC / (HPF2_RC + Ts);

    /* Filter Variables   过滤变量 */
    private static double LPF1_filter_value = 0;
    private static double HPF1_filter_value = 0;
    private static double[] filter_input1 = {0, 0};
    private static double LPF2_filter_value = 0;
    private static double HPF2_filter_value = 0;
    private static double[] filter_input2 = {0, 0};
    private static boolean LPF1_on = true;
    private static boolean HPF1_on = true;
    private static boolean LPF2_on = true;
    private static boolean HPF2_on = true;


    /* Channel Parameters  通道参数*/
    private static final int CHANNEL_GAIN = 6;
    private static final double VOLTAGE_RANGE = 2.4;
    private static final double VOLTAGE_DIVISOR = (VOLTAGE_RANGE / (0x7FFFFF * CHANNEL_GAIN)) * 1000;

    /* Debug variables  调试变量 */
    private static int previous_ch = 0;

    private boolean autofit_on = false;
    private boolean labels_on = false;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Chart Variables  图表变量
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
    private XYSeries mCurrentSeries;
    private XYSeriesRenderer mCurrentRenderer;
    private String mDateFormat;
    private Button mStart;
    private Button mFCLow_Button1;
    private Button mFCHigh_Button1;
    private Button mFCLow_Button2;
    private Button mFCHigh_Button2;
    private GraphicalView mChartView;
    //    private PacketData mPacketData;
    private int[] mColour = {
            Color.BLUE, Color.RED,
            Color.CYAN, Color.GREEN,
            Color.MAGENTA, Color.YELLOW,
            Color.GRAY, Color.BLACK};


    // Plot point buffers  绘图点缓冲区

    private ArrayList<ArrayList<Double>> yBuffer = new ArrayList<ArrayList<Double>>();

    private int mWindowSize = 1000;
    private PopupWindow mWindow;
    private TextView mTvChangePar;


    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);
        mDataset = (XYMultipleSeriesDataset) savedState.getSerializable("dataset");
        mRenderer = (XYMultipleSeriesRenderer) savedState.getSerializable("renderer");
        mCurrentSeries = (XYSeries) savedState.getSerializable("current_series");
        mCurrentRenderer = (XYSeriesRenderer) savedState.getSerializable("current_renderer");
        mDateFormat = savedState.getString("date_format");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("dataset", mDataset);
        outState.putSerializable("renderer", mRenderer);
        outState.putSerializable("current_series", mCurrentSeries);
        outState.putSerializable("current_renderer", mCurrentRenderer);
        outState.putString("date_format", mDateFormat);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);


        for (int i = 0; i < 1; i++) {        //Could be this
            yBuffer.add(new ArrayList<Double>());
        }

        if (D) Log.e(TAG, "+++ ON CREATE +++");

        setContentView(R.layout.main);

        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);

        mTvChangePar = (TextView) findViewById(R.id.tv_change_par);

        View popView = LayoutInflater.from(this).inflate(R.layout.my_pop, null, false);
        mWindow = new PopupWindow(popView, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        mWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.ha)));
        mWindow.setOutsideTouchable(true);
        mWindow.setTouchable(true);

        mTvChangePar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWindow.showAsDropDown(mTvChangePar);
            }
        });


        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        /***********************************
         * Chart setup
         **********************************/

        mRenderer.setApplyBackgroundColor(true);
        mRenderer.setBackgroundColor(Color.WHITE);
        mRenderer.setMarginsColor(Color.WHITE);
        mRenderer.setLabelsColor(Color.BLUE);
        mRenderer.setAxisTitleTextSize(16);
        mRenderer.setChartTitleTextSize(20);
        mRenderer.setLabelsTextSize(15);
        mRenderer.setLegendTextSize(15);
        mRenderer.setMargins(new int[]{20, 30, 15, 0});
        mRenderer.setZoomButtonsVisible(false);
        mRenderer.setPointSize(1);
        mRenderer.setYAxisMax(3);
        mRenderer.setYAxisMin(-1);
        mRenderer.setShowGrid(true);
        mRenderer.setXLabels((mWindowSize * 5) / SPS);
        mRenderer.setShowLegend(false);
        mRenderer.setShowLabels(false);
        mRenderer.setXLabelsAngle(90);
        mRenderer.setYTitle("mV");

        mStart = (Button) popView.findViewById(R.id.start);
        mFCLow_Button1 = (Button) popView.findViewById(R.id.fclow1);
        mFCHigh_Button1 = (Button) popView.findViewById(R.id.fchigh1);
        mFCLow_Button2 = (Button) popView.findViewById(R.id.fclow2);
        mFCHigh_Button2 = (Button) popView.findViewById(R.id.fchigh2);

        // Create plot with axis  用轴创建绘图
        for (int i = 0; i < 2; i++) {        // TODO: Change to channels
            String seriesTitle = "Channel " + (mDataset.getSeriesCount() + 1);// + (mDataset.getSeriesCount() + 1);
            XYSeries series = new XYSeries(seriesTitle);
            mDataset.addSeries(series);
            XYSeriesRenderer renderer = new XYSeriesRenderer();
            mRenderer.addSeriesRenderer(renderer);
            mCurrentRenderer = renderer;
            mCurrentRenderer.setColor(mColour[i]);

        }
        mCurrentSeries = mDataset.getSeriesAt(0);

        mStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mChatService.getState() == BluetoothChatService.STATE_CONNECTED) {
                    if (!SendingData) {
//	            	sendMessage("AT102=2\r\n");
                        sendMessage("AT100=1\r\n");
                        SendingData = true;
                        mStart.setText(R.string.stop);
                    } else {
                        sendMessage("AT100=0\r\n");
                        SendingData = false;
                        mStart.setText(R.string.start);
                    }
                } else {
                    Toast.makeText(BluetoothChat.this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                    SendingData = false;
                    mStart.setText(R.string.start);
                }
            }
        });
        mFCLow_Button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HPF1_options_ptr = (++HPF1_options_ptr) % HPF1_options.length;
                switch (HPF1_options_ptr) {
                    case 0:
                        mFCLow_Button1.setText(R.string.fclow_none);
                        break;
                    case 1:
                        mFCLow_Button1.setText(R.string.fclow_0_1);
                        break;
                    case 2:
                        mFCLow_Button1.setText(R.string.fclow_0_5);
                        break;
                    case 3:
                        mFCLow_Button1.setText(R.string.fclow_1_5);
                        break;
                    case 4:
                        mFCLow_Button1.setText(R.string.fclow_5);
                        break;
                }
                HPF1_Filter_Config(HPF1_options[HPF1_options_ptr]);
            }
        });
        mFCHigh_Button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LPF1_options_ptr = (++LPF1_options_ptr) % LPF1_options.length;
                switch (LPF1_options_ptr) {
                    case 0:
                        mFCHigh_Button1.setText(R.string.fchigh_none);
                        break;
                    case 1:
                        mFCHigh_Button1.setText(R.string.fchigh_8);
                        break;
                    case 2:
                        mFCHigh_Button1.setText(R.string.fchigh_10);
                        break;
                    case 3:
                        mFCHigh_Button1.setText(R.string.fchigh_15);
                        break;
                    case 4:
                        mFCHigh_Button1.setText(R.string.fchigh_10k);
                        break;
                }
                LPF1_Filter_Config(LPF1_options[LPF1_options_ptr]);
            }
        });

        mFCLow_Button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HPF2_options_ptr = (++HPF2_options_ptr) % HPF2_options.length;
                switch (HPF2_options_ptr) {
                    case 0:
                        mFCLow_Button2.setText(R.string.fclow_none);
                        break;
                    case 1:
                        mFCLow_Button2.setText(R.string.fclow_0_1);
                        break;
                    case 2:
                        mFCLow_Button2.setText(R.string.fclow_0_5);
                        break;
                    case 3:
                        mFCLow_Button2.setText(R.string.fclow_1_5);
                        break;
                    case 4:
                        mFCLow_Button2.setText(R.string.fclow_5);
                        break;
                }
                HPF2_Filter_Config(HPF2_options[HPF2_options_ptr]);
            }
        });
        mFCHigh_Button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LPF2_options_ptr = (++LPF2_options_ptr) % LPF2_options.length;
                switch (LPF2_options_ptr) {
                    case 0:
                        mFCHigh_Button2.setText(R.string.fchigh_none);
                        break;
                    case 1:
                        mFCHigh_Button2.setText(R.string.fchigh_8);
                        break;
                    case 2:
                        mFCHigh_Button2.setText(R.string.fchigh_10);
                        break;
                    case 3:
                        mFCHigh_Button2.setText(R.string.fchigh_15);
                        break;
                    case 4:
                        mFCHigh_Button2.setText(R.string.fchigh_10k);
                        break;
                }
                LPF2_Filter_Config(LPF2_options[LPF2_options_ptr]);
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    private void LPF1_Filter_Config(double fc) {
        Log.d(TAG, "HFc: " + fc);
        if (fc == 0) {
            LPF1_on = false;
        } else {
            LPF1_on = true;
            LPF1_Fc = fc;
            LPF1_Tc = 1 / LPF1_Fc;
            LPF1_RC = LPF1_Tc / (2 * Math.PI);
            LPF1_ALPHA = Ts / (LPF1_RC + Ts);
        }
    }

    private void HPF1_Filter_Config(double fc) {
        if (fc == 0) {
            HPF1_on = false;
        } else {
            HPF1_on = true;
            HPF1_Fc = fc;
            HPF1_Tc = 1 / HPF1_Fc;
            HPF1_RC = HPF1_Tc / (2 * Math.PI);
            HPF1_ALPHA = HPF1_RC / (HPF1_RC + Ts);
        }
    }

    private void LPF2_Filter_Config(double fc) {
        Log.d(TAG, "HFc: " + fc);
        if (fc == 0) {
            LPF2_on = false;
        } else {
            LPF2_on = true;
            LPF2_Fc = fc;
            LPF2_Tc = 1 / LPF2_Fc;
            LPF2_RC = LPF2_Tc / (2 * Math.PI);
            LPF2_ALPHA = Ts / (LPF2_RC + Ts);
        }
    }

    private void HPF2_Filter_Config(double fc) {
        if (fc == 0) {
            HPF2_on = false;
        } else {
            HPF2_on = true;
            HPF2_Fc = fc;
            HPF2_Tc = 1 / HPF2_Fc;
            HPF2_RC = HPF2_Tc / (2 * Math.PI);
            HPF2_ALPHA = HPF2_RC / (HPF2_RC + Ts);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        //如果BT未开启，请求启用它
        // setupChat() will then be called during onActivityResult
        //然后在onActivityResult期间调用setupChat（）
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise（否则）, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (D) Log.e(TAG, "+ ON RESUME +");
        if (mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "++ Enabled? ++");
            // Performing this check in onResume() covers the case in which BT was
            // not enabled during onStart(), so we were paused to enable it...
            // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
            //在onResume（）中执行此检查涵盖了BT的情况
            //在onStart（）期间未启用，因此我们暂停启用它...
            //当ACTION_REQUEST_ENABLE活动返回时，将调用// onResume（）。
            if (mChatService != null) {
                // Only if the state is STATE_NONE, do we know that we haven't started already
                //只有当状态为STATE_NONE时，我们才知道我们还没有开始
                if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                    // Start the Bluetooth chat services
                    //启动蓝牙聊天服务
                    mChatService.start();
                }
            }
        }

        if (mChartView == null) {
            LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
            mChartView = ChartFactory.getLineChartView(this, mDataset, mRenderer);


            layout.addView(mChartView, new LayoutParams(LayoutParams.FILL_PARENT,
                    LayoutParams.FILL_PARENT));

        } else {

        }


    }

    private void setupChat() {
        if (D) Log.d(TAG, "setupChat()");
        // Initialize the BluetoothChatService to perform bluetooth connections
        //初始化BluetoothChatService以执行蓝牙连接
        mChatService = new BluetoothChatService(this, mHandler);
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        Log.d(TAG, "PAUSE");
        if (mChatService != null) {
            sendMessage("AT100=0\r\n");
        }

        SendingData = false;
        mStart.setText(R.string.start);

        if (D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mChatService != null) {
            sendMessage("AT100=0\r\n");
        }
        // Stop the Bluetooth chat services
        //停止蓝牙聊天服务
        if (mChatService != null) mChatService.stop();

        if (D) Log.e(TAG, "--- ON DESTROY ---");
    }


    private void toggleAutofit() {
        autofit_on = !autofit_on;
        Log.i(TAG, "follow data = " + autofit_on);
        if (!autofit_on) {
            mRenderer.setYAxisMax(5);
            mRenderer.setYAxisMin(-1);
        }
    }

    private void togglelabels() {
        labels_on = !labels_on;
        if (labels_on) {
            mRenderer.setShowLabels(true);
        } else {
            mRenderer.setShowLabels(false);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        //在尝试任何事情之前检查我们是否真正连接过
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            //检查是否确实要发送内容
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }

    // The Handler that gets information back from the BluetoothChatService
    //处理程序从BluetoothChatService获取信息
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            mTitle.setText(R.string.title_connected_to);
                            mTitle.append(mConnectedDeviceName);

                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            mTitle.setText(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            mTitle.setText(R.string.title_not_connected);
                            if (mSendButton != null) {
                                mSendButton.setText(R.string.start);
                                SendingData = false;
                            }

                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    //从缓冲区构造一个字符串
                    String writeMessage = new String(writeBuf);

                    break;
                case MESSAGE_READ:
                    // *******************************************
                    // Data from BT comes in here, then needs to be
                    // placed in a buffer ready for the graphings
                    //来自BT的数据来自这里，然后需要放置在准备好图形的缓冲区中
                    //********************************************
                    int data;
                    ArrayList<Integer> list = new ArrayList<Integer>();

                    int x, y;

                    byte[] readBuf = (byte[]) msg.obj;
                    for (int i = 0; i < msg.arg1; i++) {
                        data = (int) readBuf[i] & 0xFF;
                        list.add(data);
                        switch (data_state) {
                            case INVALID_DATA:
                                if (data == 170) {
                                    data_state = FIRST_START_BYTE;
                                }
                                break;
                            case FIRST_START_BYTE:
                                if (data == 85) {
                                    data_state = VALID_DATA;
                                    packet_count = 0;
                                    byte_count = 0;
                                    ch = 0;
                                } else {
                                    data_state = INVALID_DATA;
                                }
                                break;
                            case VALID_DATA:
                                if (packet_count == 0) {
                                    getx(data);
                                } else if ((packet_count > 3) && (packet_count < MAX_NUM_PACKET_BYTES)) {
                                    ch <<= 8;
                                    ch += data;
                                    byte_count++;
                                    if ((byte_count % 3) == 0) {
                                        ch = ch & 0x00FFFFFF;
                                        ch = twos(ch);

                                        plot(current_x_value, ch, byte_count / 3);
                                        ch = 0;
                                    }
                                }
                                packet_count++;
                                if (packet_count >= MAX_NUM_PACKET_BYTES) {
                                    updateChart();
                                    data_state = INVALID_DATA;
                                    packet_count = 0;
                                    byte_count = 0;
                                }
                                break;
                        }
                    }
                    previous_ch = ch;
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    //保存连接设备的名称
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;

            }
        }

        private void getx(int packet_num) {
            if (packet_num < previous_packet_number) {
                current_x_value += (256 - previous_packet_number) + packet_num;
            } else {
                current_x_value += (packet_num - previous_packet_number);
            }
            previous_packet_number = packet_num;
        }

        private int twos(int num) {
            if (num >= 0x800000) {
                num = 0xFF000000 + num;
            }

            return num;
        }

    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                //当DeviceListActivity返回一个要连接的设备时
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    if (D) Log.e(TAG, "onActivityResult " + address);
                    // Get the BLuetoothDevice object
                    //获取BLuetoothDevice对象
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    //尝试连接到设备
                    mChatService.connect(device);

                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                //当启用蓝牙的请求返回时
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "RESULT OK");
                    // Bluetooth is now enabled, so set up a chat session
                    //蓝牙现已启用，因此请设置聊天会话
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occured
                    //用户未启用蓝牙或发生错误
                    if (D) Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 这条表示加载菜单文件，第一个参数表示通过那个资源文件来创建菜单
        // 第二个表示将菜单传入那个对象中。这里我们用Menu传入menu
        // 这条语句一般系统帮我们创建好
        getMenuInflater().inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;

            case R.id.autofit:
                toggleAutofit();
                return true;
            case R.id.togglelabels:
                togglelabels();
        }
        return false;
    }

    public void plot(int x, int y, int channel) {
        double localx, localy;
        mCurrentSeries = mDataset.getSeriesAt(channel - 1);
        localx = to_time((double) x);
        localy = to_voltage((double) y);

        if (channel == 1) {
            localy = HPF1(LPF1(localy));
        } else if (channel == 2) {
            localy = HPF2(LPF2(localy)) + 2;
        }
        mCurrentSeries.add(localx, localy);
        if ((mCurrentSeries.getMaxX() - mCurrentSeries.getMinX()) > (to_time((double) mWindowSize - 1))) {
            mCurrentSeries.remove(0);
        }
    }

    public double LPF1(double raw) {
        if (LPF1_on) {
            LPF1_filter_value = LPF1_filter_value + (LPF1_ALPHA * (raw - LPF1_filter_value));
            return LPF1_filter_value;
        } else {
            return raw;
        }
    }

    public double HPF1(double raw) {
        if (HPF1_on) {
            HPF1_filter_value = HPF1_ALPHA * (HPF1_filter_value + raw - filter_input1[0]);
            filter_input1[0] = raw; // TODO: raw?
            return HPF1_filter_value;
        } else {
            return raw;
        }
    }

    public double LPF2(double raw) {
        if (LPF2_on) {
            LPF2_filter_value = LPF2_filter_value + (LPF2_ALPHA * (raw - LPF2_filter_value));
            return LPF2_filter_value;
        } else {
            return raw;
        }
    }

    public double HPF2(double raw) {
        if (HPF2_on) {
            HPF2_filter_value = HPF2_ALPHA * (HPF2_filter_value + raw - filter_input2[0]);
            filter_input2[0] = raw; // TODO: raw?
            return HPF2_filter_value;
        } else {
            return raw;
        }
    }

    public double to_voltage(double value) {
        return value * VOLTAGE_DIVISOR;
    }

    public double to_time(double value) {
        return value * Ts;
    }

    public void updateChart() {
        double margin = 0;
        if (mChartView != null) {
            if (autofit_on) {
                margin = (mCurrentSeries.getMaxY() - mCurrentSeries.getMinY()) * 0.2;
                mRenderer.setYAxisMax(mCurrentSeries.getMaxY() + margin);
                mRenderer.setYAxisMin(mCurrentSeries.getMinY() - margin);
            }
            mChartView.invalidate();
            update_count = 0;
        }
    }

}


