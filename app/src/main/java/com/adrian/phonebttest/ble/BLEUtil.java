package com.adrian.phonebttest.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.adrian.phonebttest.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BLEUtil {
    private static final String TAG = "BLEUtil";

    private static final int REQUEST_ENABLE_BT = 0;

    private Context context;

    private BluetoothAdapter mBTAdapter;
    private BluetoothManager mBTMngr;
    private List<BluetoothDevice> devices;
    private IScanDevCallback scanDevCallback;
    private ICharacteristicUpdateCallback charUpdateCallback;
    private boolean isScanning;
    private static final long SCAN_PERIOD = 10000;
    private Handler mHandler;
    private String uuid_service;
    private String uuid_characteristic;

    private BluetoothGatt bluetoothGatt;

    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTING:
                    Log.e(TAG, "connecting...");
                    break;
                case BluetoothProfile.STATE_CONNECTED:
                    Log.e(TAG, "connected. callback is null:" + (scanDevCallback==null));
                    if (scanDevCallback != null) {
                        scanDevCallback.onConnectedDev(gatt);
                    }
//                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    Log.e(TAG, "disconnecting...");
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e(TAG, "disconnected. callback is null:" + (scanDevCallback==null));
                    gatt.close();
                    bluetoothGatt = null;
                    if (scanDevCallback != null) {
                        scanDevCallback.onDisconnectedDev();
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
//                    List<BluetoothGattService> services = gatt.getServices();
//                    for (BluetoothGattService gattService : services) {
//                        List<BluetoothGattCharacteristic> characteristics = gattService.getCharacteristics();
//                        for (BluetoothGattCharacteristic gattChar : characteristics) {
//                            Log.e(TAG, "discovered service uuid:" + gattService.getUuid().toString() + "/char uuid:"
//                                    + gattChar.getUuid().toString() + "||char value:" + gattChar.getStringValue(0));
//                        }
//                    }
                    if (!TextUtils.isEmpty(uuid_service) && !TextUtils.isEmpty(uuid_characteristic)) {
                        BluetoothGattService service = gatt.getService(UUID.fromString(uuid_service));
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(uuid_characteristic));
                        gatt.setCharacteristicNotification(characteristic, true);
                    }
                    break;
                case BluetoothGatt.GATT_FAILURE:
                    break;
                    default:
                        break;
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
                    byte[] charBytes = characteristic.getValue();
                    String valueString = new String(charBytes);
                    Log.e(TAG, "read char value:" + valueString);
                    break;
                case BluetoothGatt.GATT_FAILURE:
                    break;
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.e(TAG, "changed uuid:" + characteristic.getUuid().toString());
            if (charUpdateCallback != null) {
                charUpdateCallback.onUpdateCharacteristic(gatt, characteristic);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }
    };

    @Deprecated
    BluetoothAdapter.LeScanCallback callback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            devices.add(bluetoothDevice);
            if (scanDevCallback != null) {
                scanDevCallback.onFoundDev(bluetoothDevice);
            }
        }
    };

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            devices.add(result.getDevice());
            if (scanDevCallback != null) {
                scanDevCallback.onFoundDev(result.getDevice());
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            showToast("扫描设备失败");
            if (scanDevCallback != null) {
                scanDevCallback.onStopScan();
            }
        }
    };

    /**
     * 扫描设备回调接口
     */
    public interface IScanDevCallback {
        /**
         * 开始扫描设备
         */
        void onStartScan();

        /**
         * 发现设备
         * @param dev
         */
        void onFoundDev(BluetoothDevice dev);

        /**
         * 停止扫描设备
         */
        void onStopScan();

        /**
         * 已连接设备
         */
        void onConnectedDev(BluetoothGatt gatt);

        /**
         * 已断开设备
         */
        void onDisconnectedDev();
    }

    public interface ICharacteristicUpdateCallback {
        void onUpdateCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
    }

    class BTReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                devices.add(device);
                if (scanDevCallback != null) {
                    scanDevCallback.onFoundDev(device);
                }
            }
        }
    }

    public BLEUtil(Context context) {
        this.context = context;
        mBTAdapter = getAdapter();
        mHandler = new Handler();
        devices = new ArrayList<>();
    }

    public void showToast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public IScanDevCallback getScanDevCallback() {
        return scanDevCallback;
    }

    public void setScanDevCallback(IScanDevCallback scanDevCallback) {
        this.scanDevCallback = scanDevCallback;
    }

    public ICharacteristicUpdateCallback getCharUpdateCallback() {
        return charUpdateCallback;
    }

    public void setCharUpdateCallback(ICharacteristicUpdateCallback charUpdateCallback) {
        this.charUpdateCallback = charUpdateCallback;
    }

    public List<BluetoothDevice> getDevices() {
        return devices;
    }

    public void setDevices(List<BluetoothDevice> devices) {
        this.devices = devices;
    }

    /**
     * 设置通知提示UUID
     * @param service_uuid
     * @param characteristic_uuid
     */
    public void setNotificationUUID(String service_uuid, String characteristic_uuid) {
        this.uuid_service = service_uuid;
        this.uuid_characteristic = characteristic_uuid;
    }

    /**
     * 检测是否支持ble
     * @return
     */
    public boolean checkIfSupportBle() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    private BluetoothAdapter getAdapter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBTMngr = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            mBTAdapter = mBTMngr.getAdapter();
        } else {
            mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        return mBTAdapter;
    }

    /**
     * 蓝牙是否打开
     */
    private void enableBluetooth() {
        if (mBTAdapter == null || !mBTAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((Activity)context).startActivityForResult(intent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * 获取已绑定设备
     */
    private void getBoundDevices() {
        Set<BluetoothDevice> devices = mBTAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {

        }
    }

    /**
     * 扫描蓝牙设备方法一
     * 在有的手机上可扫描所有蓝牙设备，包括传统蓝牙及ble，但有的手机上不行
     */
    public void startDiscovery() {
        enableBluetooth();
        if (mBTAdapter.isEnabled()) {
            mBTAdapter.startDiscovery();
        } else {
            showToast("蓝牙未打开，请先打开蓝牙");
        }
    }

    /**
     * 停止扫描蓝牙设备方法一
     */
    public void stopDiscovery() {
        if (mBTAdapter.isEnabled()) {
            mBTAdapter.cancelDiscovery();
        }
    }

    /**
     * 扫描蓝牙设备方法二
     * 官方API声明已过时，但可用
     */
    @Deprecated
    public void startLeScan() {
        if (checkIfSupportBle()) {
            enableBluetooth();
            if (isScanning) {
                showToast("正在搜索设备...");
                return;
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopLeScan();
                }
            }, SCAN_PERIOD);
            devices.clear();
            isScanning = true;
            mBTAdapter.startLeScan(callback);
            Log.e(TAG, "start scan");
            if (scanDevCallback != null) {
                scanDevCallback.onStartScan();
            }
        } else {
            showToast("此设备不支持ble");
        }
    }

    /**
     * 停止扫描BLE设备方法二
     * 官方声明已过时，但可用
     */
    @Deprecated
    public void stopLeScan() {
        if (isScanning) {
            isScanning = false;
            mBTAdapter.stopLeScan(callback);
            Log.e(TAG, "stop scan");
            if (scanDevCallback != null) {
                scanDevCallback.onStopScan();
            }
        }
    }

    /**
     * 扫描BLE设备方法三（Android5.0及以上可用）
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public void startLeScan21() {
        if (checkIfSupportBle()) {
            enableBluetooth();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBTAdapter.getBluetoothLeScanner().stopScan(scanCallback);
                }
            }, SCAN_PERIOD);
            devices.clear();
            isScanning = true;
            mBTAdapter.getBluetoothLeScanner().startScan(scanCallback);
            Log.e(TAG, "start scan");
            if (scanDevCallback != null) {
                scanDevCallback.onStartScan();
            }
        } else {
            showToast("此设备不支持ble");
        }
    }

    /**
     * 停止扫描BLE设备（Android5.0及以上可用）
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public void stopLeScan21() {
        isScanning = false;
        mBTAdapter.getBluetoothLeScanner().stopScan(scanCallback);
        Log.e(TAG, "stop scan");
        if (scanDevCallback != null) {
            scanDevCallback.onStopScan();
        }
    }

    public boolean discoverServices(BluetoothGatt gatt) {
        return gatt.discoverServices();
    }

    /**
     * 连接ble设备
     * @param device
     */
    public void openGatt(BluetoothDevice device) {
        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }

    /**
     * 断开ble设备
     */
    public void closeGatt() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
    }

    /**
     * 是否可读
     * @param characteristic
     * @return
     */
    private boolean ifCharacteristicReadable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0;
    }

    /**
     * 是否可写
     * @param characteristic
     * @return
     */
    private boolean ifCharacteristicWriteable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0
                || (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0;
    }

    /**
     * 是否可接收通知
     * @param characteristic
     * @return
     */
    private boolean ifCharacteristicNotifiable(BluetoothGattCharacteristic characteristic) {
        return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0
                || (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0;
    }

}
