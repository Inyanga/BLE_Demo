package ru.axetta.bledemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ru.axetta.bledemo.ble.BluetoothLe;
import ru.axetta.bledemo.recycler_logic.DeviceListAdapter;

public class MainActivity extends AppCompatActivity implements BluetoothLe.BleCallback {

    private final static int BT_REQUEST_CODE = 1;
    private BluetoothAdapter bluetoothAdapter;
    private List<BluetoothLe.DeviceLe> deviceList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView statusText;
    private RecyclerView deviceRecycler;
    private DeviceListAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        deviceRecycler = findViewById(R.id.deviceRecycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        adapter = new DeviceListAdapter(deviceList);
        deviceRecycler.setLayoutManager(layoutManager);
        deviceRecycler.setAdapter(adapter);
        initBluetooth();
    }

    //----------------------------------------------------------------------------------------------

    public void initBluetooth() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getApplicationContext(),
                    R.string.ble_not_supported, Toast.LENGTH_LONG).show();
        } else {
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null)
                bluetoothAdapter = bluetoothManager.getAdapter();
            else {
                Toast.makeText(getApplicationContext(),
                        R.string.bt_not_supported, Toast.LENGTH_LONG).show();
                return;
            }
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                Intent btEnableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(btEnableIntent, BT_REQUEST_CODE);
            } else {
                scanForDevices();
            }

        }
    }

    //----------------------------------------------------------------------------------------------

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == BT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                scanForDevices();
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    private void scanForDevices() {
        BluetoothLe bluetoothLe = new BluetoothLe(getApplicationContext(), bluetoothAdapter, this);
        bluetoothLe.scan();
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

    @Override
    public void onScanStart() {
        statusText.setVisibility(View.VISIBLE);
        statusText.setText(R.string.scaning);
        progressBar.setVisibility(View.VISIBLE);
    }

    //----------------------------------------------------------------------------------------------

    @Override
    public void onScanStop() {
        statusText.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
    }

    //----------------------------------------------------------------------------------------------

    @Override
    public void onDeviceFound(BluetoothLe.DeviceLe device) {
        if(!deviceList.contains(device)) {
            deviceList.add(device);
            adapter.notifyDataSetChanged();
        }
    }
}
