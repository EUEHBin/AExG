package com.example.lenovo.myaexg.wifi;

import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lenovo.myaexg.R;
import com.example.lenovo.myaexg.tcp.TaskCenter;
import com.example.lenovo.myaexg.udp.UDPBuild;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * https://www.jianshu.com/p/d8fe6e3fc00b
 * https://www.jianshu.com/p/965c14febf11
 */
public class UdpToTcpActivity extends AppCompatActivity implements View.OnClickListener, UDPBuild.OnUDPReceiveCallbackBlock {

    private String TAG = "MyTag";
    private UDPBuild mUDPBuild;
    private PopupWindow mWindow;
    private TextView mTvChangePar, mTitle;
    private Button mStart, mFCLow_Button1, mFCHigh_Button1, mFCLow_Button2, mFCHigh_Button2;
    private String TcpAdress = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_udp_to_tcp);
        //绑定视图、数据
        initView();
        initData();
    }

    private void initView() {
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTvChangePar = (TextView) findViewById(R.id.tv_change_par);
        View popView = LayoutInflater.from(this).inflate(R.layout.my_pop, null, false);
        mWindow = new PopupWindow(popView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.ha)));
        mWindow.setOutsideTouchable(true);
        mWindow.setTouchable(true);
        mTvChangePar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWindow.showAsDropDown(mTvChangePar);
            }
        });

        mStart = (Button) popView.findViewById(R.id.start);
        mFCLow_Button1 = (Button) popView.findViewById(R.id.fclow1);
        mFCHigh_Button1 = (Button) popView.findViewById(R.id.fchigh1);
        mFCLow_Button2 = (Button) popView.findViewById(R.id.fclow2);
        mFCHigh_Button2 = (Button) popView.findViewById(R.id.fchigh2);

        mStart.setOnClickListener(this);
        mFCLow_Button1.setOnClickListener(this);
        mFCHigh_Button1.setOnClickListener(this);
        mFCLow_Button2.setOnClickListener(this);
        mFCHigh_Button2.setOnClickListener(this);

    }

    private void initData() {

        mUDPBuild = UDPBuild.getUdpBuild();
        mUDPBuild.setUdpReceiveCallback(this);

        //断开连接
        TaskCenter.sharedCenter().setDisconnectedCallback(new TaskCenter.OnServerDisconnectedCallbackBlock() {
            @Override
            public void callback(IOException e) {

            }
        });
        //连接
        TaskCenter.sharedCenter().setConnectedCallback(new TaskCenter.OnServerConnectedCallbackBlock() {
            @Override
            public void callback() {

            }
        });
        //数据
        TaskCenter.sharedCenter().setReceivedCallback(new TaskCenter.OnReceiveCallbackBlock() {
            @Override
            public void callback(final String receicedMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        tvTcp.setText(strToASCII(receicedMessage));
                    }
                });

            }
        });

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

//            case R.id.btn_tcp_disconnect:
//                TaskCenter.sharedCenter().disconnect();
//                break;

            //开始按钮
            case R.id.start:
                sendMessage();
                break;
            case R.id.fclow1:
                WifiDataCalculation.mFCLowButton1(mFCLow_Button1);
                break;
            case R.id.fchigh1:
                WifiDataCalculation.mFCHighButton1(mFCHigh_Button1);
                break;
            case R.id.fclow2:
                WifiDataCalculation.mFCLowButton2(mFCLow_Button2);
                break;
            case R.id.fchigh2:
                WifiDataCalculation.mFCHighButton2(mFCHigh_Button2);
                break;
        }
    }

    //UDP收到的数值
    @Override
    public void OnParserComplete(DatagramPacket data) {
        String strReceive = new String(data.getData(), 0, data.getLength());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());
        String str = formatter.format(curDate);
        Log.d(TAG, "收到：" + str + "：" + strReceive);
        if (strReceive.contains("USR-C322")) {
            final List<String> list = Arrays.asList(strReceive.split(","));
            Log.d(TAG, "IP：" + list.get(0));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //获取到TCP的IP地址并设置
                    TcpAdress = list.get(0);
                    Toast.makeText(UdpToTcpActivity.this, "由UDP拿到的TCP地址为：" + TcpAdress, Toast.LENGTH_SHORT).show();
                    //进行TCP连接
                    TaskCenter.sharedCenter().connect(TcpAdress, 8899);
                }
            });
        } else {
            Toast.makeText(UdpToTcpActivity.this, "获取地址失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage() {
        mUDPBuild.sendMessage("www.usr.cn");
    }

    public static String strToASCII(String data) {
        String requestStr = "";
        for (int i = 0; i < data.length(); i++) {
            char a = data.charAt(i);
            int aInt = (int) a;
            requestStr = requestStr + integerToHexString(aInt);
        }
        return requestStr;
    }

    public static String integerToHexString(int s) {
        String ss = Integer.toHexString(s);
        if (ss.length() % 2 != 0) {
            ss = "0" + ss;//0F格式
        }
        return ss.toUpperCase();
    }
}
