package com.panghui.servicediscovery;

import com.panghui.servicediscovery.routing.RoutingTableItem;

import java.util.LinkedList;

/**
 * 该类用于存储设备所需要的属性
 */
public class DeviceAttributes {
    static public String deviceFlag;
    static public String androidID;
    static public String networkCredential;
    static public int jaccardIndex;
    static public boolean isConnectedToGO = false;
    static public boolean isGO = false;
    static public boolean foundP2pDevicesDone = false;

    static public String messageTargetID=null;
    static public String messaageContent=null;
    static public String currentlyConnectedDevice="";

    static public String locationStr;

    // routing table
    static public LinkedList<RoutingTableItem> routingTable = new LinkedList<>();
    static public LinkedList<String> neighborList = new LinkedList<>(); // 邻居列表
}
