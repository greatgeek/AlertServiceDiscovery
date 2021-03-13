package com.panghui.servicediscovery.routing;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 应用传输的消息格式：
 * / source / destination / type / payload /
 * 若是传递文字消息，则消息类型为 MESSAGE_TYPE，payload 为文字字符串
 * 若是传递路由表，则消息类型为 ROUTING_TYPE，payload 为路由表的 json格式
 */
public class MessageItem {
    public static final int TEXT_TYPE =3000;
    public static final int ROUTING_TYPE=2000;

    @JSONField(name = "SOURCE")
    private String source;

    @JSONField(name = "DESTINATION")
    private String destination; // 目的地址

    @JSONField(name = "TYPE")
    private int type; // 类型（消息/路由）

    @JSONField(name = "PAYLOAD")
    private String payload; // 数据

    @JSONField(name = "LOCATION")
    private String location; // 坐标

    public MessageItem(){

    }

    public MessageItem(String source, String destination, int type, String payload, String location) {
        this.source = source;
        this.destination = destination;
        this.type = type;
        this.payload = payload;
        this.location = location;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
