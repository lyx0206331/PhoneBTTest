package com.adrian.phonebttest;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.adrian.phonebttest.ble.BLEUtil;
import com.adrian.phonebttest.classic.ClassicBTUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements BLEUtil.IScanDevCallback, BLEUtil.ICharacteristicUpdateCallback {

    public static final String UUID_SERVICE = "0000ff00-0000-1000-8000-00805f9b34fb";
    public static final String UUID_CHARACT = "0000ff01-0000-1000-8000-00805f9b34fb";

    @BindView(R.id.btn_search)
    Button mBtnSearch;
    @BindView(R.id.tv_content)
    TextView mTvContent;
    @BindView(R.id.btn_connect)
    Button mBtnConnect;
    @BindView(R.id.btn_read_notify)
    Button mBtnReadNotify;

    private BLEUtil bleUtil;
    private boolean isSearching;
    private boolean isConnected;
    private BluetoothDevice curDev;
    private BluetoothGatt curGatt;

    private static final int MY_PERMISSIONS_REQUEST_CODE = 1;
    private String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION};
    private List<String> permissionList = new ArrayList<>();

    private ClassicBTUtil classicBTUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        bleUtil = new BLEUtil(this);
        bleUtil.setScanDevCallback(this);
        bleUtil.setCharUpdateCallback(this);
        bleUtil.setNotificationUUID(UUID_SERVICE, UUID_CHARACT);

        classicBTUtil = new ClassicBTUtil(this);
    }

    private void requestPermission() {
        Log.e("TAG", "request permission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionList.add(permission);
                }
            }
            if (permissionList.isEmpty()) {
                startScan();
            } else {
                String[] per = permissionList.toArray(new String[permissionList.size()]);
                ActivityCompat.requestPermissions(this, per, MY_PERMISSIONS_REQUEST_CODE);
            }
        } else {
            startScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    boolean isAskAgain = ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i]);
                    if (isAskAgain) {
                        bleUtil.showToast("权限未申请");
                    }
                } else {
                    startScan();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @OnClick({R.id.btn_search, R.id.btn_connect, R.id.btn_read_notify})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_search:
                if (!isSearching) {
                    requestPermission();
                } else {
                    stopScan();
                }
                break;
            case R.id.btn_connect:
                if (curDev == null) {
                    bleUtil.showToast("请先搜索设备");
                    return;
                }
                if (!isConnected && curDev != null) {
                    bleUtil.openGatt(curDev);
                    return;
                }
                if (isConnected) {
                    bleUtil.closeGatt();
                    return;
                }
                break;
            case R.id.btn_read_notify:
                if (curGatt != null) {
                    bleUtil.discoverServices(curGatt);
                }
                break;
            default:
                break;
        }
    }

    private void stopScan() {
        bleUtil.stopLeScan();
    }

    private void startScan() {
        bleUtil.startLeScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isSearching) {
            stopScan();
        }
    }

    @Override
    protected void onDestroy() {
        bleUtil.closeGatt();
        classicBTUtil.destroy();
        super.onDestroy();
    }

    @Override
    public void onStartScan() {
        mBtnSearch.setText("正在搜索...");
        isSearching = true;
    }

    @Override
    public void onFoundDev(BluetoothDevice dev) {
        Log.e("DEV", "name/mac:" + dev.getName() + "/" + dev.getAddress());
        if (dev.getAddress().equals("00:13:8A:20:00:43")) {
            curDev = dev;
            stopScan();
        }
    }

    @Override
    public void onStopScan() {
        Log.e("TAG", "++++++++++++++++++++");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBtnSearch.setText("扫描设备");
                isSearching = false;
                if (curDev != null) {
                    mBtnSearch.setText(curDev.getName());
                }
            }
        });
    }

    @Override
    public void onConnectedDev(final BluetoothGatt gatt) {
        Log.e("TAG", "--------------------");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                isConnected = true;
                mBtnConnect.setText("已连接");
                curGatt = gatt;
            }
        });

        classicBTUtil.autoConnect(curDev);
    }

    @Override
    public void onDisconnectedDev() {
        Log.e("TAG", "********************");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                isConnected = false;
                mBtnConnect.setText("未连接");
                mBtnSearch.setText("扫描设备");
                curDev = null;
                curGatt = null;
            }
        });
        classicBTUtil.disconnect();
    }

    @Override
    public void onUpdateCharacteristic(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        Log.e("TAG", "value:" + new String(characteristic.getValue()));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBtnReadNotify.setText(characteristic.getUuid().toString());
                mTvContent.append(new String(characteristic.getValue()) + "\n");
            }
        });

    }
}
