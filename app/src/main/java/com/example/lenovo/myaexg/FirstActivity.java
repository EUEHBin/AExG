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

        mPermissionItems.add(new PermissionItem(Manifest.permission.READ_EXTERNAL_STORAGE,
                "读存储卡",R.drawable.permission_ic_storage));
        mPermissionItems.add(new PermissionItem(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                "写存储卡",R.drawable.permission_ic_storage));
        mPermissionItems.add(new PermissionItem(Manifest.permission.ACCESS_COARSE_LOCATION,
                "GPS",R.drawable.permission_ic_sensors));
        mPermissionItems.add(new PermissionItem(Manifest.permission.ACCESS_FINE_LOCATION,
                "定位",R.drawable.permission_ic_location));
    }

    @Override
    public void onClick(final View v) {

        HiPermission.create(this)
                .permissions(mPermissionItems)
                .checkMutiPermission(new PermissionCallback() {
                    @Override
                    public void onClose() {

                    }

                    @Override
                    public void onFinish() {

                        switch (v.getId()) {
                            //蓝牙
                            case R.id.btn_bluetooth:
                                Intent intent = new Intent(FirstActivity.this, BluetoothChat.class);
                                intent.putExtra("Type","bluetooth");
                                startActivity(intent);
                                break;
                            //wifi
                            case R.id.btn_wifi:
                                Intent intent1 = new Intent(FirstActivity.this,UdpToTcpActivity.class);
                                intent1.putExtra("Type","wifi");
                                startActivity(intent1);
                                break;
                        }

                    }

                    @Override
                    public void onDeny(String permission, int position) {

                    }

                    @Override
                    public void onGuarantee(String permission, int position) {

                    }
                });

    }
}
