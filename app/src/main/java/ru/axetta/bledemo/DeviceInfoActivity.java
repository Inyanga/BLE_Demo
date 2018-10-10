package ru.axetta.bledemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class DeviceInfoActivity extends AppCompatActivity {

    private TextView name, address, rssi, sn, hw, sw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
//        name = findViewById(R.id.textView);
//        address = findViewById(R.id.textView2);
//        rssi = findViewById(R.id.textView3);
//        sn = findViewById(R.id.textView4);
//        hw = findViewById(R.id.textView5);
//        sw = findViewById(R.id.textView6);
//        name.setText(getIntent().getStringExtra(MainActivity.DEVICE_NAME));
//        address.setText(getIntent().getStringExtra(MainActivity.DEVICE_ADDRESS));
//        rssi.setText(getIntent().getIntExtra(MainActivity.DEVICE_RSSI, -666));
//        sn.setText(getIntent().getStringExtra(MainActivity.DEVICE_SN));
//        hw.setText(getIntent().getStringExtra(MainActivity.DEVICE_HW));
//        sw.setText(getIntent().getStringExtra(MainActivity.DEVICE_SW));
    }
}
