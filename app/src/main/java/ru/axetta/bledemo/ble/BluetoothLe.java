package ru.axetta.bledemo.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BluetoothLe extends BluetoothGattCallback {


    //    private static final String FILTER_NAME = "MI Band 2";
//    private static final int SERIAL_NUMBER_VALUE = 0;
//    private static final int HW_REV_VALUE = 1;
//    private static final int SW_REV_VALUE = 2;
//    private static final UUID GENERIC_ACCESS_SRVS = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
//    private static final UUID DEVICE_NAME_CHAR = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
    //    private static final UUID DEVICE_INFO_SRVS = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
//    private static final UUID DEVICE_SERIAL_NUM_CHAR = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
//    private static final UUID HW_REV_CHAR = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb");
//    private static final UUID SW_REV_CHAR = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb");
//    private static final UUID ALERT_SRV_UUID = UUID.fromString("00001811-0000-1000-8000-00805f9b34fb");
//    private static final UUID ALERT_CHAR_UUID = UUID.fromString("00002a46-0000-1000-8000-00805f9b34fb");
    private static final UUID CCC_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final UUID DATA_SERVICE = UUID.fromString("F000C0E0-0451-4000-B000-000000000000");
    private static final UUID DATA_CHAR = UUID.fromString("F000C0E1-0451-4000-B000-000000000000");


    private static final boolean aboveLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    private static final int SCAN_MAX_TIME = 30_000;

    private BluetoothGattCharacteristic dataChar;
    private DeviceLe lastDevice;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BleCallback callback;
    private BluetoothGatt mGatt;
    private BluetoothLeScanner btScanner;
    private BluetoothGattDescriptor cccdDesc;
    private Queue<byte[]> byteQueue = new ConcurrentLinkedQueue<>();
    // TODO Значения могут быть не строковыми
    private List<String> infoValues = new ArrayList<>();

    private Map<String, BluetoothGatt> connectedDeviceMap = new HashMap<>();

    public interface BleCallback {
        void onScanStart();

        void onScanStop();

        void onDeviceFound(DeviceLe deviceLe);

        void onConnectFailed();

        void onDisconnected();

        void onDeviceInfoAvailable();

        void onConnected(DeviceLe device);

        void onCharChanged(byte[] value);

        void onDataSend(String data, int length);

        void onRssiReceived(BluetoothDevice device, int rssi);
    }


    public BluetoothLe(Context context, BluetoothAdapter bluetoothAdapter, BleCallback callback) {
        this.context = context;
        this.bluetoothAdapter = bluetoothAdapter;
        this.callback = callback;
    }


    public void startReadingRssi() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (Map.Entry<String, BluetoothGatt> entry : connectedDeviceMap.entrySet())
                {
                   entry.getValue().readRemoteRssi();
                }
            }
        }, 0, 500);
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        callback.onRssiReceived(gatt.getDevice(), rssi);
    }

    private ScanCallback scanCallbackEx = new ScanCallback() {
//        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            callback.onDeviceFound(new DeviceLe(result.getDevice(), result.getRssi()));
            callback.onDeviceFound(new DeviceLe(result.getDevice(), result.getRssi()));
            result.getDevice().connectGatt(context, false, BluetoothLe.this);
            Log.i("SCAN CALLBACK", "New device found " + result.getDevice().getName());
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i("SCAN CALLBACK", "Some error occurred " + errorCode);
        }
    };


    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] bytes) {
            callback.onDeviceFound(new DeviceLe(bluetoothDevice, rssi));
            bluetoothDevice.connectGatt(context, false, BluetoothLe.this);


        }
    };


    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        BluetoothDevice device = gatt.getDevice();
        String mac = device.getAddress();
//
        if (newState == BluetoothGatt.STATE_CONNECTED) {
            if (status == BluetoothGatt.GATT_SUCCESS) {


                if(!connectedDeviceMap.containsKey(mac)) {
                    connectedDeviceMap.put(mac, gatt);
                }
//                if (!gatt.discoverServices()) {
//                    connectionFailure();
//                }
            } else {
                connectionFailure();
            }
        } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            callback.onDisconnected();
            if (connectedDeviceMap.containsKey(mac)){
                BluetoothGatt bluetoothGatt = connectedDeviceMap.get(mac);
                if( bluetoothGatt != null ){
                    bluetoothGatt.close();
                    bluetoothGatt = null;
                }
                connectedDeviceMap.remove(mac);
            }
        }
    }


    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_FAILURE) {
            connectionFailure();
            return;
        }

        BluetoothGattService dataService = gatt.getService(DATA_SERVICE);
        if (dataService != null) {
            Log.i("GATT_LOG", "Service is ready");
            dataChar = dataService.getCharacteristic(DATA_CHAR);
            if (dataChar != null) {
                Log.i("GATT_LOG", "Char is ready");
                cccdDesc = dataChar.getDescriptor(CCC_DESCRIPTOR);
                if (cccdDesc != null) {
                    Log.i("GATT_LOG", "CCCD is ready");
                    callback.onConnected(lastDevice);
                    cccdDesc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(cccdDesc);
                    gatt.setCharacteristicNotification(dataChar, true);


                }
            }
        }


    }


    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//        Log.i("CHARWRITE","Status " + status);
        byte[] byteValue = byteQueue.poll();
        if (byteValue != null) {
            Log.i("CHARWRITE", "Value " + Arrays.toString(byteValue));
            dataChar.setValue(byteValue);
            mGatt.writeCharacteristic(dataChar);
            callback.onDataSend(Arrays.toString(byteValue), byteValue.length);
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Log.i("NOTIFY", Arrays.toString(characteristic.getValue()));
        callback.onCharChanged(characteristic.getValue());
        cccdDesc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(cccdDesc);
        gatt.setCharacteristicNotification(dataChar, true);

    }

    public void writeChar() {

        try {
            InputStream in = context.getAssets().open("ble_data");
            byte[] packet = new byte[20];
            byteQueue.clear();
            while (in.read(packet) != -1) {
//            in.read(packet);
                byteQueue.add(packet);
                packet = new byte[20];
            }

            Log.i("GATT_LOG", String.valueOf(byteQueue.size()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (dataChar != null) {
//            byte[] byteValue = {0x11, 0x65, 0x6c, 0x6c, 0x6f, 0x7e, 0x47, 0x44};
//            dataChar.setValue(byteValue);
//            mGatt.writeCharacteristic(dataChar);
            Log.i("CHARWRITE", "GONNA WRITE CHAR");
            byte[] byteValue = byteQueue.poll();
            if (byteValue != null) {
                callback.onDataSend(Arrays.toString(byteValue), byteValue.length);
                dataChar.setValue(byteValue);
                mGatt.writeCharacteristic(dataChar);
            }

        }

    }


    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//        infoValues.add(characteristic.getStringValue(0));
//        BluetoothGattCharacteristic nextChar = byteQueue.poll();
//        if (nextChar != null) {
//            gatt.readCharacteristic(nextChar);
//        } else {
//            callback.onDeviceInfoAvailable();
//        }
    }


    public void scan() {
//        if (aboveLollipop) {
//            scanLeDeviceExtended();
//        } else {
//            scanLeDevice();
//        }

        scanLeDeviceExtended();
        startReadingRssi();
    }


    private void connectionFailure() {
        callback.onConnectFailed();
        Log.i("GATT ", "Connection failure");
        Toast.makeText(context, "Failed to connect", Toast.LENGTH_SHORT).show();
    }

    public void disconnect() {
        if (mGatt != null) {
            mGatt.disconnect();

        }
        mGatt = null;
    }

    public boolean isConnected() {
        return mGatt != null;
    }


    private void scanLeDeviceExtended() {
        btScanner = bluetoothAdapter.getBluetoothLeScanner();
        List<ScanFilter> filters = new ArrayList<>();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(scanCallbackEx);
                callback.onScanStop();
            }
        }, SCAN_MAX_TIME);
//        ScanFilter scanServiceFilter = new ScanFilter.Builder().setDeviceName("Lenovo P70-A").build();
//        ScanFilter scanServiceFilter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("F000C0E0-0451-4000-B000-000000000000")).build();
//        filters.add(scanServiceFilter);
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        btScanner.startScan(null, scanSettings, scanCallbackEx);
        callback.onScanStart();
    }

//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void stopScan() {
        btScanner.stopScan(scanCallbackEx);
    }

    private void scanLeDevice() {
        //TODO Make some 4.4 code
    }


    public void connect(DeviceLe deviceLe) {
        lastDevice = deviceLe;
//        if(mGatt!=null) {
//            mGatt.disconnect();
//            mGatt=null;
//        }
        mGatt = deviceLe.getDevice().connectGatt(context, false, this);
    }


    public String getInfoValue(int index) {
        return infoValues.get(index);
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
