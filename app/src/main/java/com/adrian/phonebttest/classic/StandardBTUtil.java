package com.adrian.phonebttest.classic;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by qing on 2018/3/12 0012.
 */

public class StandardBTUtil {
    private static final String TEST_DEV_NAME = "I480";
    private static final String SPP_UUID = "00000000-0000-1000-8000-00805f9b34fb";
    private Context context;
    private BluetoothAdapter btAdapt;
    private static BluetoothSocket btSocket;
    private List<BluetoothDevice> lstDevices = new ArrayList<BluetoothDevice>();
    private BluetoothA2dp a2dp;

    private BroadcastReceiver searchDevices = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle b = intent.getExtras();
            Object[] lstName = b.keySet().toArray();

            // 显示所有收到的消息及其细节
            for (int i = 0; i < lstName.length; i++) {
                String keyName = lstName[i].toString();
                Log.e(keyName, String.valueOf(b.get(keyName)));
            }
            BluetoothDevice device = null;
            // 搜索设备时，取得设备的MAC地址
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.e("BOND_NONE", "bt:" + device.getName() + "--" + device.getAddress() + "--" + device.getBondState());
                    if (lstDevices.indexOf(device) == -1)// 防止重复添加
                        lstDevices.add(device); // 获取设备名称和mac地址

                }
                autoConnect(device);
            }else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_BONDING:
                        Log.d("BOND_BONDING", "正在配对......");
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        Log.d("BOND_BONDED", "完成配对");
                        autoConnect(device);//连接设备
                        break;
                    case BluetoothDevice.BOND_NONE:
                        Log.d("BOND_NONE", "取消配对");
                    default:
                        break;
                }
            }

        }
    };

    public StandardBTUtil(Context context) {
        this.context = context;

        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);// 用BroadcastReceiver来取得搜索结果
        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(searchDevices, intent);
    }

    public void destroy() {
        context.unregisterReceiver(searchDevices);
        if (btSocket != null && btSocket.isConnected()) {
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void enable() {
        if (btAdapt == null) {
            btAdapt = BluetoothAdapter.getDefaultAdapter();
        }

        if (!btAdapt.isEnabled()) {
            btAdapt.enable();
        }
    }

    public boolean startSearch() {
        enable();
        return btAdapt.startDiscovery();
    }

    public void stopSearch() {
        if (btAdapt.isDiscovering()) {
            btAdapt.cancelDiscovery();
        }
    }

//    public void bondDevice(BluetoothDevice btDev) {
//        stopSearch();
//        if (btDev.getBondState() == BluetoothDevice.BOND_NONE) {
//            btDev.createBond();
//        }else if(btDev.getBondState() == BluetoothDevice.BOND_BONDED){
//            connect(btDev);
//        }
//    }
//
//    public void bondDevice(String address) {
//        BluetoothDevice dev = btAdapt.getRemoteDevice(address);
//        bondDevice(dev);
//    }

    /**
     * 设置蓝牙可见
     * @param seconds
     */
    public void setDiscoverable(int seconds) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, seconds);//可见时间 单位s
        context.startActivity(intent);
    }

    /**
     * 取消配对
     * @param device
     */
    public void removeBond(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.d("BlueUtils", e.getMessage());
        }
    }

    private void connect(final BluetoothDevice btDev) {
        UUID uuid = UUID.fromString(SPP_UUID);
        try {
//             btSocket = btDev.createRfcommSocketToServiceRecord(uuid);
            btSocket =(BluetoothSocket) btDev.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(btDev,1);

            btSocket.connect();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        //连接设备
        btAdapt.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                try {
                    if (profile == BluetoothProfile.A2DP) {
                        /**使用A2DP的协议连接蓝牙设备（使用了反射技术调用连接的方法）*/
                        a2dp = (BluetoothA2dp) proxy;
                        if (a2dp.getConnectionState(btDev) != BluetoothProfile.STATE_CONNECTED) {
                            a2dp.getClass()
                                    .getMethod("connect", BluetoothDevice.class)
                                    .invoke(a2dp, btDev);
                            Toast.makeText(context, "请播放音乐", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {

            }
        }, BluetoothProfile.A2DP);
    }

    public void autoConnect(BluetoothDevice btDev) {
        Log.e("BT_NAME", btDev.getName());
        if (TextUtils.isEmpty(btDev.getName()) || !btDev.getName().contains(TEST_DEV_NAME)) {
            Toast.makeText(context, "只能连接I480系列蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }
        stopSearch();
        try {
            if (btDev.getBondState() == BluetoothDevice.BOND_NONE) {
                btDev.createBond();
            }else if(btDev.getBondState() == BluetoothDevice.BOND_BONDED){
                connect(btDev);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void autoConnect(String address){
        if (TextUtils.isEmpty(address)) {
            Toast.makeText(context, "MAC地址为空", Toast.LENGTH_SHORT).show();
            return;
        }
        BluetoothDevice btDev = btAdapt.getRemoteDevice(address);
        autoConnect(btDev);
    }
}
