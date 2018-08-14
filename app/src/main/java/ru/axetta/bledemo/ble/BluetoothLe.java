package ru.axetta.bledemo.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothLe extends BluetoothGattCallback {

    //TODO Replace with UART cntrl name
    private static final String FILTER_NAME = "MI Band 2";

    private static final UUID GENERIC_ACCESS_SRVS = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_NAME_CHAR = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");

    private static final UUID DEVICE_INFO_SRVS = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_SERIAL_NUM_CHAR = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
    private static final UUID HW_REV_CHAR = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb");
    private static final UUID SW_REV_CHAR = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb");


    private final static UUID ALERT_SRV_UUID = UUID.fromString("00001811-0000-1000-8000-00805f9b34fb");
    private final static UUID ALERT_CHAR_UUID = UUID.fromString("00002a46-0000-1000-8000-00805f9b34fb");

    private final static UUID CHARACTERISTIC_NOTIFICATION_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final static boolean aboveLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    private static final int SCAN_MAX_TIME = 30_000;

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BleCallback callback;
    private BluetoothGatt gatt;

    public interface BleCallback {
        void onScanStart();

        void onScanStop();

        void onDeviceFound(DeviceLe deviceLe);
    }


    public BluetoothLe(Context context, BluetoothAdapter bluetoothAdapter, BleCallback callback) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        this.callback = callback;
    }

    //----------------------------------------------------------------------------------------------

    private ScanCallback scanCallbackEx = new ScanCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            callback.onDeviceFound(new DeviceLe(result.getDevice(), result.getRssi()));
           // Toast.makeText(context, "New device found " + result.getDevice().getName(), Toast.LENGTH_SHORT).show();
            Log.i("SCAN CALLBACK", "New device found " + result.getDevice().getName());
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i("SCAN CALLBACK", "Some error occurred " + errorCode);
        }
    };

    //----------------------------------------------------------------------------------------------

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] bytes) {

        }
    };

    //----------------------------------------------------------------------------------------------

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
    }

    //----------------------------------------------------------------------------------------------

    public void scan() {
        if (aboveLollipop) {
            scanLeDeviceExtended();
        } else {
            scanLeDevice();
        }
    }

    //----------------------------------------------------------------------------------------------

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void scanLeDeviceExtended() {
        final BluetoothLeScanner btScanner = bluetoothAdapter.getBluetoothLeScanner();
        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(scanCallbackEx);
                callback.onScanStop();
            }
        }, SCAN_MAX_TIME);
        ScanFilter scanServiceFilter = new ScanFilter.Builder().setDeviceName(FILTER_NAME).build();
        filters.add(scanServiceFilter);
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        btScanner.startScan(filters, scanSettings, scanCallbackEx);
        callback.onScanStart();
    }

    //----------------------------------------------------------------------------------------------

    private void scanLeDevice() {
        //TODO Make some 4.4 code
    }

    //----------------------------------------------------------------------------------------------

    public class DeviceLe {
        private BluetoothDevice device;
        private int rssi;

        DeviceLe(BluetoothDevice device, int rssi) {
            this.device = device;
            this.rssi = rssi;
        }

        public BluetoothDevice getDevice() {
            return device;
        }

        public void setDevice(BluetoothDevice device) {
            this.device = device;
        }

        public int getRssi() {
            return rssi;
        }

        public void setRssi(int rssi) {
            this.rssi = rssi;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (obj instanceof DeviceLe) {
                DeviceLe device = (DeviceLe) obj;
                return (device.getDevice().getAddress() == null && this.device.getAddress() == null) ||
                        (device.getDevice().getAddress().equals(this.device.getAddress()));
            }
            return false;
        }
    }
}
