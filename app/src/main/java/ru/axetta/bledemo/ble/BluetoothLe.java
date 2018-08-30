package ru.axetta.bledemo.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import ru.axetta.bledemo.R;

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
    private BluetoothLeScanner btScanner;
    private BleCallback callback;
    private BluetoothGatt gatt;
    private DeviceLe nearestDevice;
    private Queue<DeviceLe> rssiRequests = new ConcurrentLinkedQueue<>();
    private Queue<BluetoothGattCharacteristic> charQueue = new ConcurrentLinkedQueue<>();
    private BluetoothGattCharacteristic serialNum;
    private BluetoothGattCharacteristic hwRev;
    private BluetoothGattCharacteristic swRev;
    private boolean findingNearest;

    public interface BleCallback {
        void onBluetoothEnable();

        void onScanStart();

        void onScanStop();

        void onDeviceFound(DeviceLe deviceLe);

        void onConnectFailed();

        void onNearestFound(DeviceLe device);

        void onDisconnected();

        void onDeviceInfoAvailable(DeviceLe device);
    }


    public BluetoothLe(Context context, BluetoothAdapter bluetoothAdapter, BleCallback callback) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        this.callback = callback;
    }


    public void initBluetooth() {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(context.getApplicationContext(),
                    R.string.ble_not_supported, Toast.LENGTH_LONG).show();
        } else {
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null)
                bluetoothAdapter = bluetoothManager.getAdapter();
            else {
                Toast.makeText(context.getApplicationContext(),
                        R.string.bt_not_supported, Toast.LENGTH_LONG).show();
                return;
            }
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                callback.onBluetoothEnable();
            } else {
                startScan();
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    private ScanCallback scanCallbackEx = new ScanCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            callback.onDeviceFound(new DeviceLe(result.getDevice(), result.getRssi()));
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
            callback.onDeviceFound(new DeviceLe(bluetoothDevice, rssi));
        }
    };

    //----------------------------------------------------------------------------------------------

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                if (findingNearest) {
                    gatt.readRemoteRssi();
                    return;
                }

                if (!gatt.discoverServices()) {
                    connectionFailure();
                }
            } else {
                connectionFailure();
            }
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            callback.onDisconnected();
        }
    }

    //----------------------------------------------------------------------------------------------

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_FAILURE) {
            connectionFailure();
            return;
        }
        serialNum = gatt.getService(DEVICE_INFO_SRVS).getCharacteristic(DEVICE_SERIAL_NUM_CHAR);
        hwRev = gatt.getService(DEVICE_INFO_SRVS).getCharacteristic(HW_REV_CHAR);
        swRev = gatt.getService(DEVICE_INFO_SRVS).getCharacteristic(SW_REV_CHAR);
        charQueue.offer(serialNum);
        charQueue.offer(hwRev);
        charQueue.offer(swRev);
        gatt.readCharacteristic(serialNum);
    }

    //----------------------------------------------------------------------------------------------

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            BluetoothGattCharacteristic nextChar = charQueue.poll();
            if (nextChar != null) {
                gatt.readCharacteristic(nextChar);
            } else {
                callback.onDeviceInfoAvailable(new DeviceLe(gatt.getDevice(), 0, serialNum.getStringValue(0), hwRev.getStringValue(0), swRev.getStringValue(0)));
            }
        }
    }


    //----------------------------------------------------------------------------------------------

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        if (nearestDevice == null || nearestDevice.getRssi() < rssi) {
            nearestDevice = new DeviceLe(gatt.getDevice(), rssi);
        }
        Log.i("RSSI ", String.valueOf(rssi));
        gatt.disconnect();
        getRssiQueueDevice();
    }


    //----------------------------------------------------------------------------------------------

    public void startScan() {
        if (aboveLollipop) {
            scanLeDeviceExtended();
        } else {
            scanLeDevice();
        }
    }

    //----------------------------------------------------------------------------------------------

    private void connectionFailure() {
        callback.onConnectFailed();
        Log.i("GATT ", "Connection failure");
    }

    //----------------------------------------------------------------------------------------------

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void scanLeDeviceExtended() {
        btScanner = bluetoothAdapter.getBluetoothLeScanner();
        List<ScanFilter> filters = new ArrayList<>();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScan();
            }
        }, SCAN_MAX_TIME);
        ScanFilter scanServiceFilter = new ScanFilter.Builder().setDeviceName(FILTER_NAME).build();
        filters.add(scanServiceFilter);
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        btScanner.startScan(null, scanSettings, scanCallbackEx);
        callback.onScanStart();
    }

    //----------------------------------------------------------------------------------------------

    private void scanLeDevice() {
        //TODO Make some 4.4 code
    }

    //----------------------------------------------------------------------------------------------

    public void stopScan() {
        if (aboveLollipop) {
            if (btScanner != null) {
                btScanner.stopScan(scanCallbackEx);
                callback.onScanStop();
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    public void connect(DeviceLe deviceLe) {
        gatt = deviceLe.getDevice().connectGatt(context, true, this);
    }

    //----------------------------------------------------------------------------------------------

    public void connectNearestDevice(List<DeviceLe> deviceList) {
        findingNearest = true;
        for (DeviceLe deviceLe : deviceList) {
            rssiRequests.offer(deviceLe);
        }
        getRssiQueueDevice();
    }

    //----------------------------------------------------------------------------------------------

    private void getRssiQueueDevice() {
        if (!rssiRequests.isEmpty()) {
            DeviceLe device = rssiRequests.poll();
            device.getDevice().connectGatt(context, true, this);
        } else {
            if (nearestDevice != null) {
                findingNearest = false;
                callback.onNearestFound(nearestDevice);
                nearestDevice.getDevice().connectGatt(context, true, this);
            } else {
                Log.i("GATT ", "No devices in area");
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    public class DeviceLe {
        private BluetoothDevice device;
        private int rssi;
        private String serialNumber;
        private String hwRev;
        private String swRev;

        DeviceLe(BluetoothDevice device, int rssi) {
            this.device = device;
            this.rssi = rssi;
        }

        public DeviceLe(BluetoothDevice device, int rssi, String serialNumber, String hwRev, String swRev) {
            this.device = device;
            this.rssi = rssi;
            this.serialNumber = serialNumber;
            this.hwRev = hwRev;
            this.swRev = swRev;
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

        public String getSerialNumber() {
            return serialNumber;
        }

        public void setSerialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
        }

        public String getHwRev() {
            return hwRev;
        }

        public void setHwRev(String hwRev) {
            this.hwRev = hwRev;
        }

        public String getSwRev() {
            return swRev;
        }

        public void setSwRev(String swRev) {
            this.swRev = swRev;
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
