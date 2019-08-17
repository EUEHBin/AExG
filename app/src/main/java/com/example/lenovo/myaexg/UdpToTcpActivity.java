package com.example.lenovo.myaexg;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

public class UdpToTcpActivity extends AppCompatActivity implements View.OnClickListener, UDPBuild.OnUDPReceiveCallbackBlock {
    private Button btnUdp, btnTcp,btnTcpDisconnect;
    private TextView tvUdp, tvTcp;
    private String TAG = "MyTag";
    private UDPBuild mUDPBuild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udp_to_tcp);
        initView();
        initData();
    }

    private void initView() {
        btnUdp = (Button) findViewById(R.id.btn_udp);
        btnTcp = (Button) findViewById(R.id.btn_tcp);
        btnTcpDisconnect = (Button) findViewById(R.id.btn_tcp_disconnect);

        tvUdp = (TextView) findViewById(R.id.tv_udp);
        tvTcp = (TextView) findViewById(R.id.tv_tcp);

    }

    private void initData() {
        btnUdp.setOnClickListener(this);
        btnTcp.setOnClickListener(this);
        btnTcpDisconnect.setOnClickListener(this);

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
                      tvTcp.setText(strToASCII(receicedMessage));
                  }
              });

            }
        });

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //TCP
            case R.id.btn_tcp:

                TaskCenter.sharedCenter().connect(tvUdp.getText().toString(),8899);
                break;
            case R.id.btn_tcp_disconnect:
                TaskCenter.sharedCenter().disconnect();
                break;
            //UDP
            case R.id.btn_udp:
                sendMessage();
                break;
        }
    }

    @Override
    public void OnParserComplete(DatagramPacket data) {
        String strReceive = new String(data.getData(), 0, data.getLength());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());
        String str = formatter.format(curDate);
        Log.d(TAG, "收到："+str + "：" + strReceive);
        if (strReceive.contains("USR-C322")){
            final List<String> list = Arrays.asList(strReceive.split(","));
            Log.d(TAG, "IP："+ list.get(0));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvUdp.setText(list.get(0));
                }
            });

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
