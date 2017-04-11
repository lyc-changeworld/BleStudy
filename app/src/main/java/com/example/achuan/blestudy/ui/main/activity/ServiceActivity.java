package com.example.achuan.blestudy.ui.main.activity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;

import com.example.achuan.blestudy.R;
import com.example.achuan.blestudy.SampleGattAttributes;
import com.example.achuan.blestudy.Tools;
import com.example.achuan.blestudy.app.Constants;
import com.example.achuan.blestudy.base.SimpleActivity;
import com.example.achuan.blestudy.service.BleService;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by achuan on 17-4-10.
 * 功能：
 */

public class ServiceActivity extends SimpleActivity {

    public static final String TAG="ServiceActivity";

    @BindView(R.id.expand_lv)
    ExpandableListView mExpandLv;

    //设置广播监听
    private BluetoothReceiver bluetoothReceiver = null;

    //初始化控件
    private List<List<BluetoothGattCharacteristic>> mBluetoothGattCharacteristic; // 记录所有特征
    private SimpleExpandableListAdapter service_list_adapter;//列表适配器
    private List<Map<String, String>> grounps;// 一级条目
    private List<List<Map<String, String>>> childs;// 二级条目

    /*一些连接时的状态标志*/
    private boolean read_name_flag = false;//读取名称标志
    //	private boolean servicesdiscovered_flag = false;
    private boolean connect_flag = false;//连接标志
    private boolean bind_flag = false;//设备标志

    private BluetoothDevice mBluetoothDevice;//当前需要连接的设备实例

    private BluetoothGattCharacteristic mGattCharacteristic;

    @Override
    protected int getLayout() {
        return R.layout.activity_service;
    }

    @Override
    protected void initEventAndData() {
        setBroadcastReceiver();
        initView();
        getDefaultName();
    }

    /*自定义的蓝牙广播监听器类*/
    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BleService.ACTION_READ_Descriptor_OVER:
                    //设备描述读取完毕
                    if (BluetoothGatt.GATT_SUCCESS == intent.getIntExtra("value", -1)) {
                        read_name_flag = true;
                    }
                    break;
                case BleService.ACTION_ServicesDiscovered_OVER:
                    //connect_flag = true;
                    break;
                case BleService.ACTION_STATE_CONNECTED:
                    //设备已连接
                    connect_flag = true;
                    Toast.makeText(context, "已连接", Toast.LENGTH_SHORT).show();
                    break;
                case BleService.ACTION_STATE_DISCONNECTED:
                    //设备断开连接
                    connect_flag= false;
                    Toast.makeText(context, "已断开连接", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    //BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //绑定状态发生改变
                    if (BluetoothDevice.BOND_BONDED == mBluetoothDevice.getBondState()) {
                        //该设备已经被绑定了
                        Tools.mBleService.disConectBle();
                        readNameFail.sendEmptyMessageDelayed(0, 200);
                    } else if (BluetoothDevice.BOND_BONDING == mBluetoothDevice.getBondState()) {
                        //正在配对
                        bind_flag = true;
                    }
                    break;
                case BleService.ACTION_DATA_CHANGE:
                    //数据改变通知


                    break;
                case BleService.ACTION_READ_OVER:
                    //读取数据


                    break;
                case BleService.ACTION_WRITE_OVER:
                    //写入数据

                    break;
                default:break;
            }
        }
    }

    


    /**1-动态注册广播接收器*/
    private void setBroadcastReceiver() {
        // 创建一个IntentFilter对象，将其action指定为BluetoothDevice.ACTION_FOUND
        IntentFilter intentFilter = new IntentFilter(
                BleService.ACTION_READ_Descriptor_OVER);//设备描述相关读取完毕
        intentFilter.addAction(BleService.ACTION_ServicesDiscovered_OVER);//设备发现完毕
        intentFilter.addAction(BleService.ACTION_STATE_CONNECTED);//已连接上设备
        intentFilter.addAction(BleService.ACTION_STATE_DISCONNECTED);//连接断开
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);//设备绑定状态改变
        //数据相关
        intentFilter.addAction(BleService.ACTION_READ_OVER);//读取数据
        //intentFilter.addAction(BleService.ACTION_RSSI_READ);
        intentFilter.addAction(BleService.ACTION_WRITE_OVER);//写入数据
        intentFilter.addAction(BleService.ACTION_DATA_CHANGE);//数据发生改变

        bluetoothReceiver = new BluetoothReceiver();
        // 注册广播接收器
        registerReceiver(bluetoothReceiver, intentFilter);
    }

    /**2-初始化布局*/
    private void initView() {

        grounps = new ArrayList<Map<String, String>>();
        childs = new ArrayList<List<Map<String, String>>>();

        //存储"特征值"集合对象
        mBluetoothGattCharacteristic = new ArrayList<List<BluetoothGattCharacteristic>>();

        //创建列多级条目的列表适配器
        service_list_adapter = new SimpleExpandableListAdapter(this,
                grounps, R.layout.service_grounp_fmt,//数据＋布局
                new String[] { "name", "Uuid" },//名称
                new int[] { R.id.tv_grounp_name, R.id.tv_grounp_uuid },//对应控件ID
                childs, R.layout.service_child_fmt,//子项
                new String[] { "name", "prov", "uuid" },
                new int[] { R.id.tv_child_name, R.id.tv_prov, R.id.tv_child_uuid });
        //为控件添加适配器
        mExpandLv.setAdapter(service_list_adapter);

        /*mExpandLv.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {

                Intent intent = new Intent(getApplicationContext(),
                        TalkActivity.class);
                intent.putExtra("one", groupPosition);
                intent.putExtra("two", childPosition);
                startActivityForResult(intent, 0);
                //startActivity(intent);
                return false;
            }
        });*/


    }



    //发现服务后的处理方法
    private Handler dis_services_handl = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            service_list_adapter.notifyDataSetChanged();
            pd.dismiss();
        }
    };

    //读取名称失败后的处理方法
    private Handler readNameFail = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //关闭连接
            Tools.mBleService.disConectBle();
            //重新读取名称信息
            new readNameThread().start();
        }
    };

    //连接失败后的处理方法
    private Handler connect_fail_handl = new Handler() {
        public void handleMessage(Message msg) {
            Tools.mBleService.disConectBle();
            Toast.makeText(getApplicationContext(), "连接失败",
                    Toast.LENGTH_LONG).show();
            finish();
        };
    };



    private ProgressDialog pd;

    private Handler reflashDialogMessage = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();
            pd.setMessage(b.getString("msg"));
        }
    };


    private void getDefaultName() {
        // 开启一个缓冲对话框
        pd = new ProgressDialog(this);
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.setTitle("正在加载...");
        pd.setMessage("正在连接");
        pd.show();

        //拿到上个活动传递过来的"设备对象"
        mBluetoothDevice = (BluetoothDevice) getIntent().getParcelableExtra("device");
        new readNameThread().start();
    }

    // 读取线程
    private class readNameThread extends Thread{
        @Override
        public void run() {
            super.run();

            Message msg = reflashDialogMessage.obtainMessage();
            Bundle b = new Bundle();
            msg.setData(b);

            try {
                while(true){
                    connect_flag = false;
                    if(exit_activity)
                        return;  // 如果已经退出程序，则结束线程
                    //开始连接设备
                    Tools.mBleService.conectBle(mBluetoothDevice);

                    for (int j = 0; j < 50; j++) {
                        if (connect_flag) {
                            //如果在5秒内连接上了,就跳出循环
                            break;
                        }
                        sleep(100);
                    }
                    if (connect_flag) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            read_name_flag = false; // 读取设备名

            //获取到该Gatt中存在的service服务集合
            List<BluetoothGattService> services = Tools.mBleService.mBluetoothGatt
                    .getServices();


            if(services.size() == 0){
                if(mBluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    //未发现任何的服务service,发出消息
                    readNameFail.sendEmptyMessage(0);
                }
                return;
            }

            String uuid;
            //走到这一步,开始读取通道信息
            b.putString("msg", "读取通道信息");
            reflashDialogMessage.sendMessage(msg);


            //准备好UI显示
            grounps.clear();
            childs.clear();

            //遍历服务集合,服务名作为一级条目,该服务中存在的特征值作为二级条目
            for (BluetoothGattService service : services) {

                uuid = service.getUuid().toString().trim();//获取服务的uuid表示

                //仅显示我们需要的service
                if(!uuid.equalsIgnoreCase(Constants.TARGET_SERVICE_UUID)){
                    continue;
                }

                //获取到该服务中存在的特征值集合
                List<BluetoothGattCharacteristic> gattCharacteristics = service
                        .getCharacteristics();

                if(gattCharacteristics.size() == 0){
                    //服务中没有特征值也跳过下面的操作
                    continue;
                }

                // 添加一个一级目录
                Map<String, String> grounp = new HashMap<String, String>();
                //对一级目录的名称进行设置
                grounp.put("name", SampleGattAttributes.lookup(uuid, "unknow"));
                grounp.put("Uuid", uuid);
                grounps.add(grounp);//存储到集合体中
                //打印显示你期望的Service的uuid号
                //Log.d(TAG,"我期望的service的uuid号为："+uuid);

                //创建一个集合来存储单个"服务"中对应的所有的"特征值"对象
                List<BluetoothGattCharacteristic> grounpCharacteristic = new ArrayList<BluetoothGattCharacteristic>();

                //存储一个二级条目的信息的集合体
                List<Map<String, String>> child = new ArrayList<Map<String, String>>();

                //遍历该服务中的特征值对象
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    // 添加一个二级条目
                    Map<String, String> child_data = new HashMap<String, String>();

                    //获取"特征值"对应的uuid号
                    uuid = gattCharacteristic.getUuid().toString();

                    //只获取我期望使用的"特征值"的对象
                    if(!uuid.equalsIgnoreCase(Constants.TARGET_CHARACTERISTIC_UUID)){
                        continue;
                    }

                    //Log.d(TAG,"我期望的Characteristic的uuid号为："+uuid);

                    //这样就获取到了我想要的"特征值"对象
                    mGattCharacteristic=gattCharacteristic;


                    BluetoothGattDescriptor descriptor = gattCharacteristic
                            .getDescriptor(UUID
                                    .fromString("00002901-0000-1000-8000-00805f9b34fb"));

                    //根据descriptor对象来设置"特征名"
                    if (null != descriptor) {
                        read_name_flag = false;
                        Tools.mBleService.mBluetoothGatt.readDescriptor(descriptor);
                        while (!read_name_flag) {// 等待读取完成
                            if (exit_activity || bind_flag){
                                bind_flag = false;
                                return; // 读取超时，结束线程
                            }
                        }
                        try {
                            child_data.put("name",
                                    new String(descriptor.getValue(), "GB2312"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    } else {
                        child_data.put("name", SampleGattAttributes.lookup(uuid, "unknow"));
                    }

                    //获取并设置操作权限名
                    String pro = "";
                    if (0 != (gattCharacteristic.getProperties() &
                            BluetoothGattCharacteristic.PROPERTY_READ)) { // 可读
                        pro += "可读,";
                    }
                    if ((0 != (gattCharacteristic.getProperties() &
                            BluetoothGattCharacteristic.PROPERTY_WRITE)) ||
                            (0 != (gattCharacteristic.getProperties()
                                    & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE))) { // 可写
                        pro += "可写,";
                    }
                    if ((0 != (gattCharacteristic.getProperties() &
                            BluetoothGattCharacteristic.PROPERTY_NOTIFY)) ||
                            (0 != (gattCharacteristic.getProperties() &
                                    BluetoothGattCharacteristic.PROPERTY_INDICATE))	) { // 通知
                        pro += "可通知";
                    }

                    //单个二级条目的设置
                    child_data.put("prov", pro);
                    child_data.put("uuid", uuid);
                    //某一级对应的所有的条目信息的集合：child
                    child.add(child_data);
                    //某一级条目中对应的所有特征值对象的集合:grounpCharacteristic
                    grounpCharacteristic.add(gattCharacteristic);
                }
                //一个一级条目添加完成
                childs.add(child);
                //存储一个"特征值"集合对象
                mBluetoothGattCharacteristic.add(grounpCharacteristic);
            }
            //服务查询完毕后的操作
            dis_services_handl.sendEmptyMessage(0);
        }
    }


    //从后面的活动返回时,
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //如果未和当前设备
        if(!Tools.mBleService.isConnected()){
            finish();
        }
    }

    private boolean exit_activity = false;

    /*活动销毁后,记得关闭连接和取消蓝牙监听*/
    @Override
    protected void onDestroy() {
        Tools.mBleService.disConectBle();
        exit_activity = true;
        unregisterReceiver(bluetoothReceiver);
        super.onDestroy();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: add setContentView(...) invocation
        ButterKnife.bind(this);
    }
}
