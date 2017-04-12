package com.example.achuan.blestudy.mode.bean;

/**
 * Created by achuan on 17-4-12.
 */

public class ChatMsgFmt {

    private String name; // 名字
    private String msg; // 信息
    private MESSAGE_FROM from; // 接受还是发送

    public String getName() {
        return name;
    }

    public String getMsg() {
        return msg;
    }

    public MESSAGE_FROM getFrom() {
        return from;
    }

    public ChatMsgFmt(String name, String msg, MESSAGE_FROM from) {
        this.name = name;
        this.msg = msg;
        this.from = from;
    }


    // 消息是自己发送还是接收
    public enum MESSAGE_FROM {
        ME, OTHERS
    }

}
