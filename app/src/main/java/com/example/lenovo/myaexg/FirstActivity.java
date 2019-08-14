package com.example.lenovo.myaexg;

import android.Manifest;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import me.weyye.hipermission.HiPermission;
import me.weyye.hipermission.PermissionCallback;
import me.weyye.hipermission.PermissionItem;

public class FirstActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btnWifi, btnBluetooth;
    private List<PermissionItem> mPermissionItems;

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

        mPermissionItems = new ArrayList<>();
        mPermissionItems.add(new PermissionItem(Manifest.permission.ACCESS_FINE_LOCATION,
                "定位", R.drawable.permission_ic_location));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //蓝牙
            case R.id.btn_bluetooth:
                Intent intent = new Intent(FirstActivity.this, BluetoothChat.class);
                startActivity(intent);
                break;
            //wifi
            case R.id.btn_wifi:
                HiPermission.create(this)
                        .permissions(mPermissionItems)
                        .checkMutiPermission(new PermissionCallback() {
                            @Override
                            public void onClose() {

                            }

                            @Override
                            public void onFinish() {

                            }

                            @Override
                            public void onDeny(String permission, int position) {

                            }

                            @Override
                            public void onGuarantee(String permission, int position) {

                            }
                        });
                break;
        }
    }
}
