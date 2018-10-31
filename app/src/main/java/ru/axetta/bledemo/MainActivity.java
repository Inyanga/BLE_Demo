package ru.axetta.bledemo;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.constraint.Group;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.axetta.bledemo.ble.BluetoothLe;
import ru.axetta.bledemo.recycler_logic.DeviceListAdapter;
import ru.axetta.bledemo.recycler_logic.OnDeviceClickListener;

public class MainActivity extends AppCompatActivity implements BluetoothLe.BleCallback {

    private final static int BT_REQUEST_CODE = 1;
    private BluetoothAdapter bluetoothAdapter;
    private List<BluetoothLe.DeviceLe> deviceList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView statusText, dName, notifyValue, log, count;
    private Button connectBtn,mainScan;
    private Group recyclerGroup, deviceGroup;
    private DeviceListAdapter adapter;
    private BluetoothLe bluetoothLe;
    private BluetoothLe.DeviceLe lastDevice;
    private boolean connected;
    private int counter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        dName = findViewById(R.id.d_name);
        connectBtn = findViewById(R.id.connect_btn);
        notifyValue = findViewById(R.id.notify_value);
        count = findViewById(R.id.count);
        mainScan = findViewById(R.id.main_scan);
        log = findViewById(R.id.log);
        log.setMovementMethod(new ScrollingMovementMethod());
        recyclerGroup = findViewById(R.id.recyclerGroup);
        deviceGroup = findViewById(R.id.deviceGroup);
        RecyclerView deviceRecycler = findViewById(R.id.deviceRecycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        adapter = new DeviceListAdapter(deviceList, this);

        deviceRecycler.setLayoutManager(layoutManager);
        deviceRecycler.setAdapter(adapter);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(!areLocationServicesEnabled(this)) {
                Intent gpsOptionsIntent = new Intent(
                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(gpsOptionsIntent, 12);
            } else {
                checkPermissions();
            }

        } else {
            initBluetooth();
        }


    }

    //----------------------------------------------------------------------------------------------
//    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    13);
        } else {
            initBluetooth();
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 13) {
            if(checkGrantResults(permissions, grantResults)) initBluetooth();

        }
    }




    public boolean checkGrantResults(String[] permissions, int[] grantResults) {
        int granted = 0;

        if (grantResults.length > 0) {
            for(int i = 0; i < permissions.length ; i++) {
                String permission = permissions[i];
                if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        granted++;
                    }
                }
            }
        } else { // if cancelled
            return false;
        }

        return granted == 2;
    }


    public boolean areLocationServicesEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


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
        if(requestCode == 12) {
            if (resultCode == 0) {
                checkPermissions();
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    private void scanForDevices() {
        bluetoothLe = new BluetoothLe(getApplicationContext(), bluetoothAdapter, this);
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
        mainScan.setVisibility(View.INVISIBLE);
    }

    //----------------------------------------------------------------------------------------------

    @Override
    public void onScanStop() {
        statusText.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        if(!(deviceGroup.getVisibility() == View.VISIBLE)) {
            mainScan.setVisibility(View.VISIBLE);
        }

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

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectBtn.setEnabled(true);
                connected = false;
            }
        });
    }

    //----------------------------------------------------------------------------------------------


    @Override
    public void onDeviceInfoAvailable() {

    }

    @Override
    public void onCharChanged(final byte[] value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyValue.setText(String.format("%s %s", notifyValue.getText(), Arrays.toString(value)));
            }
        });
    }

    @Override
    public void onDataSend(final String data, final int length) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                counter += length;
                count.setText("Bytes: " + counter);
                log.append("\n" + data);
//                log.setText(log.getText() + "\n" + data);
            }
        });
    }

    //    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onConnected(final BluetoothLe.DeviceLe device) {
        bluetoothLe.stopScan();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectBtn.setEnabled(false);
                recyclerGroup.setVisibility(View.GONE);
                deviceGroup.setVisibility(View.VISIBLE);
                mainScan.setVisibility(View.INVISIBLE);
                log.setText("");
                notifyValue.setText("");
                counter = 0;
                dName.setText(device.getDevice().getName());
                connected = true;
            }
        });

    }


    @Override
    public void onRssiReceived(int rssi, int i) {
        
    }

    //    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void changeUi(BluetoothLe.DeviceLe device) {
        lastDevice = device;
        Log.i("ASDASDASD", String.valueOf(!bluetoothLe.isConnected()));
//        if (!connected)
            bluetoothLe.connect(device);
    }

    //    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void reConnect(View view) {
        changeUi(lastDevice);
    }

    public void reScan(View view) {
        bluetoothLe.disconnect();
        connected = false;
        recyclerGroup.setVisibility(View.VISIBLE);
        deviceGroup.setVisibility(View.GONE);
        mainScan.setVisibility(View.INVISIBLE);
        bluetoothLe.scan();
    }

    public void send(View view) {
        bluetoothLe.writeChar();
        counter = 0;
    }

    public void scanAgain(View view) {
        deviceList.clear();
        adapter.notifyDataSetChanged();
        bluetoothLe.scan();
    }
}
