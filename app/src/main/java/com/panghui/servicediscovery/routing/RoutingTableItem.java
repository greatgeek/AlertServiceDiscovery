package com.panghui.servicediscovery.routing;

import com.alibaba.fastjson.annotation.JSONField;

/**
 *路由表条目格式：
 * / gateway / destination / hops /
 * 在传输时，路由表条目会转化成 json 格式字符串，放入 MessageItem 的payload中，并设置该 MessageItem 的类型（type）为 ROUTING_TYPE
 * 接收设备收到 ROUTING_TYPE类型的数据后，将其 payload 的 json 字符串解析成 RoutingTableItem ，将其作为补充路由表。
 */
public class RoutingTableItem {

    @JSONField(name="SOURCE")
    private String source; // gateway

    @JSONField(name="DESTINATION")
    private String destination;

    @JSONField(name="HOPS")
    private int hops;

    public RoutingTableItem(){

    }

    public RoutingTableItem(String source, String destination, int hops) {
        this.source = source;
        this.destination = destination;
        this.hops = hops;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public int getHops() {
        return hops;
    }

    public void setHops(int hops) {
        this.hops = hops;
    }

    @Override
    public String toString() {
        return "RoutingTableItem{" +
                "source='" + source + '\'' +
                ", destination='" + destination + '\'' +
                ", hops=" + hops +
                '}';
    }
}
