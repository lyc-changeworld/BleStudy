package com.example.achuan.blestudy.app;

/**
 * Created by achuan on 17-1-25.
 * 功能：存放一些静态不变的量
 */

public class Constants {

    //=================MODULE TYPE CODE====================
    public static final int TYPE_NEWS=100;

    /*MAIN TYPE CODE*/
    public static final int TYPE_SETTINGS=101;



    //=================SHARED_PREFERENCE VALUE_NAME====================
    //创建的SharedPreferences文件的文件名
    public static final String PREFERENCES_NAME = "my_sp";
    //当前处于的模块
    public static final String CURRENT_ITEM = "current_item";




    //=================REQUEST CODE请求码====================


    //=================OTHER STRING====================
    //目标蓝牙service的uuid号
    public static final String TARGET_SERVICE_UUID="0000fff0-0000-1000-8000-00805f9b34fb";
    //目标特征值的uuid号
    public static final String TARGET_CHARACTERISTIC_UUID="0000fff6-0000-1000-8000-00805f9b34fb";

}
