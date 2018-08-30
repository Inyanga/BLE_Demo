package ru.axetta.bledemo;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ru.axetta.bledemo.ble.BluetoothLe;
import ru.axetta.bledemo.recycler_logic.DeviceListAdapter;
import ru.axetta.bledemo.recycler_logic.OnDeviceClickListener;

public class MainActivity extends AppCompatActivity implements BluetoothLe.BleCallback {


    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADDRESS = "device_address";
    public static final String DEVICE_RSSI = "device_rssi";
    public static final String DEVICE_SN = "device_sn";
    public static final String DEVICE_HW = "device_hw";
    public static final String DEVICE_SW = "device_sw";


    private final static int BT_REQUEST_CODE = 1;
    private BluetoothAdapter bluetoothAdapter;
    private List<BluetoothLe.DeviceLe> deviceList;
    private ProgressBar progressBar;
    private TextView statusText;
    private Button scanBtn;
    private DeviceListAdapter adapter;
    private BluetoothLe bluetoothLe;
    private boolean isScaning;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        scanBtn = findViewById(R.id.scanBtn);
        deviceList = new ArrayList<>();
        RecyclerView deviceRecycler = findViewById(R.id.deviceRecycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        adapter = new DeviceListAdapter(deviceList, new OnDeviceClickListener() {
            @Override
            public void onDeviceClick(BluetoothLe.DeviceLe deviceLe) {
                bluetoothLe.connect(deviceLe);
                Log.i("RECYCLER ", "Click..");
            }
        });
        deviceRecycler.setLayoutManager(layoutManager);
        deviceRecycler.setAdapter(adapter);
        bluetoothLe = new BluetoothLe(getApplicationContext(), bluetoothAdapter, this);
        bluetoothLe.initBluetooth();

    }

    //----------------------------------------------------------------------------------------------

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == BT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                bluetoothLe.startScan();
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //----------------------------------------------------------------------------------------------

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //----------------------------------------------------------------------------------------------

    public void scanTrigger(View view) {
        if (isScaning) {
            bluetoothLe.stopScan();
        } else {
            bluetoothLe.startScan();
        }
    }

    //----------------------------------------------------------------------------------------------

    public void connectToNearest(View view) {
        bluetoothLe.connectNearestDevice(deviceList);
    }

    //----------------------------------------------------------------------------------------------

    @Override
    public void onScanStart() {
        statusText.setVisibility(View.VISIBLE);
        statusText.setText(R.string.scaning);
        progressBar.setVisibility(View.VISIBLE);
        scanBtn.setText("STOP");
        isScaning = true;
        deviceList.clear();
    }

    //----------------------------------------------------------------------------------------------

    @Override
    public void onScanStop() {
        //  statusText.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        scanBtn.setText("START");
        isScaning = false;
    }

    //----------------------------------------------------------------------------------------------

    @Override
    public void onDeviceFound(BluetoothLe.DeviceLe device) {
        if (!deviceList.contains(device)) {
            deviceList.add(device);
            adapter.notifyDataSetChanged();
        }
    }


    //----------------------------------------------------------------------------------------------

    @Override
    public void onConnectFailed() {
        Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
    }

    //----------------------------------------------------------------------------------------------

    @Override
    public void onDisconnected() {
        // Toast.makeText(getApplicationContext(), "Device was disconnected", Toast.LENGTH_SHORT).show();
    }

    //----------------------------------------------------------------------------------------------


    @Override
    public void onBluetoothEnable() {
        Intent btEnableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(btEnableIntent, BT_REQUEST_CODE);
    }

    @Override
    public void onNearestFound(final BluetoothLe.DeviceLe device) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), device.getDevice().getAddress(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onDeviceInfoAvailable(BluetoothLe.DeviceLe device) {
        Intent infoIntent = new Intent(getApplicationContext(), DeviceInfoActivity.class);
        infoIntent.putExtra(DEVICE_NAME, device.getDevice().getName());
        infoIntent.putExtra(DEVICE_ADDRESS, device.getDevice().getAddress());
        infoIntent.putExtra(DEVICE_RSSI, device.getRssi());
        infoIntent.putExtra(DEVICE_SN, device.getSerialNumber());
        infoIntent.putExtra(DEVICE_HW, device.getHwRev());
        infoIntent.putExtra(DEVICE_SW, device.getSwRev());
        startActivity(infoIntent);
    }
}
