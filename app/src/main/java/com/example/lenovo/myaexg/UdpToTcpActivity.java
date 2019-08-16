package com.example.lenovo.myaexg;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UdpToTcpActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btnUdp, btnTcp;
    private TextView tvUdp, tvTcp;
    private String TAG = "MyTag";

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

        tvUdp = (TextView) findViewById(R.id.tv_udp);
        tvTcp = (TextView) findViewById(R.id.tv_tcp);
    }

    private void initData() {
        btnUdp.setOnClickListener(this);
        btnTcp.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //TCP
            case R.id.btn_tcp:

                break;
            //UDP
            case R.id.btn_udp:

                break;
        }
    }

    private void udpReceiver() {
        try {
            DatagramSocket datagramSocket = new DatagramSocket(48899);
            byte[] buff = new byte[4 * 1024];
            DatagramPacket packet = new DatagramPacket(buff, buff.length);
            try {
                datagramSocket.receive(packet);
                String result = new String(packet.getData(), packet.getOffset(), packet.getLength());
                Log.d("MyTag", "UdpReceiverResult:" + result);
                datagramSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}
