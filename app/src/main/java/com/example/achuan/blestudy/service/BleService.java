package com.example.achuan.blestudy.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.example.achuan.blestudy.R;
import com.example.achuan.blestudy.app.App;
import com.example.achuan.blestudy.app.Constants;

/**
 * Created by achuan on 17-4-10.
 *
 */

public class BleService extends Service {

    //下面的常量是用来设置广播的
    public final static String ACTION_DATA_CHANGE = "com.example.bluetooth.le.ACTION_DATA_CHANGE";
    public final static String ACTION_RSSI_READ = "com.example.bluetooth.le.ACTION_RSSI_READ";
    public final static String ACTION_STATE_CONNECTED = "com.example.bluetooth.le.ACTION_STATE_CONNECTED";
    public final static String ACTION_STATE_DISCONNECTED = "com.example.bluetooth.le.ACTION_STATE_DISCONNECTED";
    public final static String ACTION_WRITE_OVER = "com.example.bluetooth.le.ACTION_WRITE_OVER";
    public final static String ACTION_READ_OVER = "com.example.bluetooth.le.ACTION_READ_OVER";
    public final static String ACTION_READ_Descriptor_OVER = "com.example.bluetooth.le.ACTION_READ_Descriptor_OVER";
    public final static String ACTION_WRITE_Descriptor_OVER = "com.example.bluetooth.le.ACTION_WRITE_Descriptor_OVER";
    public final static String ACTION_ServicesDiscovered_OVER = "com.example.bluetooth.le.ACTION_ServicesDiscovered_OVER";


    public BluetoothManager mBluetoothManager;
    public BluetoothAdapter mBluetoothAdapter;
    public BluetoothGatt mBluetoothGatt;
    private boolean connect_flag = false;

    //创建一个专门的Binder对象
    public class LocalBinder extends Binder {
        public BleService getService(){
            return BleService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /*****************************Ble蓝牙的一些基本方法****************************/

    /*－－－－－－－－－－－－－－－－－初始化＋扫描设备相关的－－－－－－－－－－－－－－－－*/
    //初始化BLE的方法,返回true才说明可以使用蓝牙功能
    public boolean initBle() {

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            return false;
        }

        mBluetoothManager = (BluetoothManager) getSystemService(
                Context.BLUETOOTH_SERVICE);

        if (null == mBluetoothManager) {
            return false;
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (null == mBluetoothAdapter) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    //扫描
    public void scanBle(BluetoothAdapter.LeScanCallback callback) {
        mBluetoothAdapter.startLeScan(callback);
    }

    //停止扫描
    public void stopscanBle(BluetoothAdapter.LeScanCallback callback) {
        mBluetoothAdapter.stopLeScan(callback);
    }


    /*－－－－－－－－－－－－－－－－－和远程设备连接－－－－－－－－－－－－－－－－*/
    // 检查是否连接
    public boolean isConnected()
    {
        return connect_flag;
    }

    //关闭连接
    public void disConectBle(){
        if(mBluetoothGatt != null){
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            connect_flag = false;
        }
    }

    //发起连接
    public boolean conectBle(BluetoothDevice mBluetoothDevice) {
        disConectBle();
        //通过传入的设备的Mac地址，获取远程设备
        BluetoothDevice device_tmp = mBluetoothAdapter.getRemoteDevice(
                mBluetoothDevice.getAddress());
        if(device_tmp == null){
            //如果为空,说明当前范围内找不到该设备,返回false
            return false;
        }
        //否则，开始执行连接，并添加回调方法
        mBluetoothGatt = device_tmp.connectGatt(App.getInstance().getContext(),
                false, mGattCallback);
        return true;
    }

    /*连接BEL外设时，需要一个实现回调接口以得到连接状态*/
    private BluetoothGattCallback mGattCallback=new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            //监听连接的状态,同时发送广播出去
            if (newState == BluetoothProfile.STATE_CONNECTED) {//连接成功
                connect_flag = true;
                mBluetoothGatt.discoverServices();
                broadcastUpdate(ACTION_STATE_CONNECTED);
                //当前外设相当于前面章节提到的Server角色：发送数据的一方
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {//断开链接
                connect_flag = false;
                broadcastUpdate(ACTION_STATE_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            //已经发现到了设备
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_ServicesDiscovered_OVER, status);
            }
        }

        @Override // 读取操作的回调结果
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            //读取了特征值中携带来的信息
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //如果是从目标“特征值”搬运工那获取到数据,就广播发送数据出去
                if(characteristic.getUuid().toString().equalsIgnoreCase(
                        Constants.TARGET_CHARACTERISTIC_UUID))
                broadcastUpdate(ACTION_READ_OVER, characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            //往特征值中写数据完了
            broadcastUpdate(ACTION_WRITE_OVER, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            //特征值中数据发生改变
            broadcastUpdate(ACTION_DATA_CHANGE, characteristic.getValue());

        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            //读取设备描述完毕
            broadcastUpdate(ACTION_READ_Descriptor_OVER, status);
        }
    };







    /************下面是发送广播消息的3种方法,根据不同的情况来**************/
    // 发送广播消息
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    // 发送广播消息
    private void broadcastUpdate(final String action, int value) {
        final Intent intent = new Intent(action);
        intent.putExtra("value", value);
        sendBroadcast(intent);
    }

    // 发送广播消息
    private void broadcastUpdate(final String action, byte value[]) {
        final Intent intent = new Intent(action);
        intent.putExtra("value", value);
        sendBroadcast(intent);
    }

}
