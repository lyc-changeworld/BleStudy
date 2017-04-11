package com.example.achuan.blestudy.ui.main.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.achuan.blestudy.R;
import com.example.achuan.blestudy.Tools;
import com.example.achuan.blestudy.base.SimpleActivity;
import com.example.achuan.blestudy.mode.bean.MTBeacon;
import com.example.achuan.blestudy.service.BleService;
import com.example.achuan.blestudy.ui.main.adapter.DeviceAdapter;
import com.example.achuan.blestudy.widget.RyItemDivider;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DeviceScanActivity extends SimpleActivity {

    private final static String TAG="DeviceScanActivity";

    private final static int REQUEST_ENABLE_BT = 2001;//打开蓝牙的请求码

    public static final int PERMISSIONS_REQUEST_FINE_LOCATION = 1;//申请权限的请求码
    @BindView(R.id.rv)
    RecyclerView mRv;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    private boolean mScanning;
    private Handler mHandler;

    // Stops scanning after 5 seconds.
    private static final long SCAN_PERIOD = 5000;

    Context mContext;
    DeviceAdapter mDeviceAdapter;
    LinearLayoutManager mLinearlayoutManager;//列表布局管理者

    //预处理和最后使用的设备数据集合
    private List<MTBeacon> mScanDevices_before;
    //private List<MTBeacon> mScanDevices_last;

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
        //初始化
        initViewAndData();

        Intent intent=new Intent(this, BleService.class);
        //startService(intent);
        //将活动和服务绑定在一起
        bindService(intent,//创建一个意图,指向服务
                mConnection,//绑定的实例化对象
                Context.BIND_AUTO_CREATE);//活动和服务绑定后自动创建服务
    }

    /*初始化view和数据*/
    private void initViewAndData(){
        mHandler = new Handler();
        mScanDevices_before=new ArrayList<>();
        //mScanDevices_last=new ArrayList<>();
        //创建集合实例对象
        mDeviceAdapter = new DeviceAdapter(mContext, mScanDevices_before);
        mLinearlayoutManager = new LinearLayoutManager(mContext);
        //设置方向(默认是垂直,下面的是水平设置)
        //linearlayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRv.setLayoutManager(mLinearlayoutManager);//为列表添加布局
        mRv.setAdapter(mDeviceAdapter);//为列表添加适配器
        //添加自定义的分割线
        mRv.addItemDecoration(new RyItemDivider(this, R.drawable.di_item));

        mDeviceAdapter.setOnClickListener(new DeviceAdapter.OnClickListener() {
            @Override
            public void onClick(View view, int postion) {

                Intent intent = new Intent(getApplicationContext(),
                        ServiceActivity.class);
                intent.putExtra("device", mScanDevices_before.get(postion).GetDevice());
                startActivity(intent);

            }
        });
    }


    /*活动和服务绑定的实例化方法*/
    private ServiceConnection mConnection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BleService.LocalBinder binder= (BleService.LocalBinder) service;
            Tools.mBleService=binder.getService();//获取到对应的的服务对象

            //对硬件设备进行初始化判断
            if(Tools.mBleService.initBle()){
                if (!Tools.mBleService.mBluetoothAdapter.isEnabled()) {
                    final Intent enableBtIntent = new Intent(
                            BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    //蓝牙已经打开
                    scanLeDevice(true); // 开始扫描设备
                }
            }else {
                //不支持蓝牙,直接退出
                finish();
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };



    /**恢复交互时,重新进行设备扫描操作*/
    @Override
    protected void onResume() {
        super.onResume();
        if(mScanning==false&&Tools.mBleService!=null){
            scanLeDevice(true);
        }
    }

    /**退出交互时,停止设备扫描操作,清空数据*/
    @Override
    protected void onPause() {
        super.onPause();
        if(Tools.mBleService!=null){
            scanLeDevice(false);
        }
    }

    /*活动销毁时记得解绑服务连接*/
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
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

    /*扫描设备的方法*/
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    Tools.mBleService.stopscanBle(mLeScanCallback);
                    //最后更新距离
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
            //启动扫描后,清空数据
            mScanDevices_before.clear();
            Tools.mBleService.scanBle(mLeScanCallback);
        } else {
            mScanning = false;
            Tools.mBleService.stopscanBle(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    /*蓝牙设备扫描时的回调方法,在此处进行列表刷新显示*/
    /**
     * @param device 被手机蓝牙扫描到的BLE外设实体对象
     * @param rssi 大概就是表示BLE外设的信号强度，如果为0，则表示BLE外设不可连接。
     * @param scanRecord 被扫描到的BLE外围设备提供的扫描记录，一般没什么用
     */
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
                            mtBeacon.CalculateDistance(19);//开始计算距离(19代表信号存储的次数)
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
            //扫描停止
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);//显示扫描菜单
            menu.findItem(R.id.menu_refresh).setActionView(null).setVisible(false);//进度条不显示
        } else {
            //正在扫描
            menu.findItem(R.id.menu_stop).setVisible(true);//显示停止菜单
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.toolbar_progress);//加载显示进度条
        }
        return true;//返回true,表示允许创建的菜单显示出来
    }

    /**Toolbar上菜单的点击监听事件*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
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
