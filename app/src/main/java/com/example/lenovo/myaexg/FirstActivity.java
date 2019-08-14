package com.example.lenovo.myaexg;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class FirstActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btnWifi,btnBluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
        initView();
        initData();
    }

    private void initView() {
        btnWifi = (Button) findViewById(R.id.btn_wifi);
        btnBluetooth = (Button) findViewById(R.id.btn_bluetooth);
    }

    private void initData() {
        btnWifi.setOnClickListener(this);
        btnBluetooth.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_wifi:
                Intent intent = new Intent(FirstActivity.this,BluetoothChat.class);
                startActivity(intent);
                break;

            case R.id.btn_bluetooth:

                break;
        }
    }
}
