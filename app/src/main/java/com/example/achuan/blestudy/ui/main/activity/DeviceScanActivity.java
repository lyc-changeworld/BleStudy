package com.example.achuan.blestudy.ui.main.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.achuan.blestudy.R;
import com.example.achuan.blestudy.base.SimpleActivity;
import com.example.achuan.blestudy.mode.bean.MTBeacon;
import com.example.achuan.blestudy.ui.main.adapter.DeviceAdapter;
import com.example.achuan.blestudy.widget.RyItemDivider;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DeviceScanActivity extends SimpleActivity {

    private final static int REQUEST_ENABLE_BT = 2001;//打开蓝牙的请求码
    public static final int PERMISSIONS_REQUEST_FINE_LOCATION = 1;//申请权限的请求码
    @BindView(R.id.rv)
    RecyclerView mRv;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    // Stops scanning after 5 seconds.
    private static final long SCAN_PERIOD = 5000;


    Context mContext;
    List<BluetoothDevice> mDevices;//用来存储扫描到的设备集合
    DeviceAdapter mDeviceAdapter;
    LinearLayoutManager mLinearlayoutManager;//列表布局管理者

    //预处理和最后使用的设备数据集合
    private List<MTBeacon> mScanDevices_before;
    private List<MTBeacon> mScanDevices_last;


    @Override
    protected int getLayout() {
        return R.layout.activity_main;
    }

    @Override
    protected void initEventAndData() {
        mContext = this;
        setToolBar(mToolbar,getString(R.string.app_name),false);

        /*运行时申请权限*/
        requestPermission();


        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        /**如果蓝牙可以用,进行初始化操作*/
        mHandler = new Handler();
        mScanDevices_before=new ArrayList<>();
        mScanDevices_last=new ArrayList<>();
        //创建集合实例对象
        mDevices = new ArrayList<>();
        mDeviceAdapter = new DeviceAdapter(mContext, mScanDevices_before);
        mLinearlayoutManager = new LinearLayoutManager(mContext);
        //设置方向(默认是垂直,下面的是水平设置)
        //linearlayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRv.setLayoutManager(mLinearlayoutManager);//为列表添加布局
        mRv.setAdapter(mDeviceAdapter);//为列表添加适配器
        //添加自定义的分割线
        mRv.addItemDecoration(new RyItemDivider(this, R.drawable.di_item));

    }

    /**恢复交互时,重新进行设备扫描操作*/
    @Override
    protected void onResume() {
        super.onResume();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } else {
            //否则,直接查询设备
            scanLeDevice(true);
        }
    }

    /**退出交互时,停止设备扫描操作,清空数据*/
    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mDevices.clear();
    }

    /**
     * 2-从打开蓝牙的窗口处理完后的回调方法
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                //进行设备查询
                scanLeDevice(true);
            } else {
                finish();
            }
        }
    }

    /*查询设备的方法*/
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mDeviceAdapter.notifyDataSetChanged();
                    invalidateOptionsMenu();

                    /*for (int i = 0; i < mScanDevices_before.size();) { // 防抖
                        if (mScanDevices_before.get(i).CheckSearchcount() > 2) {
                            mScanDevices_before.remove(i);
                        } else {
                            i++;
                        }
                    }*/

                    /*mScanDevices_last.clear(); // 显示出来
                    for (MTBeacon device : mScanDevices_before) {
                        mScanDevices_last.add(device);
                    }
                    mDeviceAdapter.notifyDataSetChanged();*/

                }
            }, SCAN_PERIOD);
            mScanning = true;//标记正在扫描设备
            mScanDevices_before.clear();
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    /*蓝牙设备扫描时的回调方法,在此处进行列表刷新显示*/
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    //回到主线程进行UI更新
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 检查是否是搜索过的设备，并且更新
                            for (int i = 0; i < mScanDevices_before.size(); i++) {
                                //compareTo()方法的返回结果为0时:代表相等
                                if (0 == device.getAddress().compareTo(
                                        mScanDevices_before.get(i).GetDevice().getAddress())) {
                                    mScanDevices_before.get(i).ReflashInf(device, rssi, scanRecord); // 更新信息
                                    //mDeviceAdapter.notifyDataSetChanged();
                                    return;//当前设备已经搜索过了,更新一下信息就行,下面的操作不再执行
                                }
                            }
                            MTBeacon mtBeacon=new MTBeacon(device, rssi, scanRecord);
                            mtBeacon.CalculateDistance(19);//开始计算距离
                            // 增加新设备
                            mScanDevices_before.add(mtBeacon);
                            mDeviceAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };


    /**
     * 1-运行时申请权限
     */
    private void requestPermission() {
        if (ActivityCompat.checkSelfPermission(this, //Context
                Manifest.permission.ACCESS_FINE_LOCATION)//具体的权限名
                != PackageManager.PERMISSION_GRANTED) {//用来比较权限
            // No explanation needed　申请权限.
            ActivityCompat.requestPermissions(this,//Activity实例
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},//数组,存放权限名
                    PERMISSIONS_REQUEST_FINE_LOCATION);//请求码
        }
    }

    /*用户对申请权限进行操作后的回调方法*/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_FINE_LOCATION:
                //授权结果通过
                if (grantResults.length > 0 && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this, "你拒绝了授权", Toast.LENGTH_SHORT).show();
                }
            default:
                break;
        }
    }


    /**Toolbar上菜单的设置*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().//获得MenuInflater对象
                inflate(R.menu.menu_toolbar_main,//指定通过哪一个资源文件来创建菜单
                menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null).
                    setVisible(false);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.toolbar_progress);
        }
        return true;//返回true,表示允许创建的菜单显示出来
    }

    /**Toolbar上菜单的点击监听事件*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mDevices.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
            default:break;
        }
        return true;//返回true,表示允许item点击响应
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: add setContentView(...) invocation
        ButterKnife.bind(this);
    }
}
