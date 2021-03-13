package com.panghui.servicediscovery.networkManager;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Handler;
import android.util.Log;

import com.panghui.servicediscovery.DeviceAttributes;
import com.panghui.servicediscovery.MainActivity;
import com.panghui.servicediscovery.routing.RoutingTableItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class InformationCollection extends Thread {
    WifiP2pManager wifiP2pManager;
    Channel channel;
    Handler handler;

    public InformationCollection(WifiP2pManager wifiP2pManager, Channel channel, Handler handler){
        this.wifiP2pManager = wifiP2pManager;
        this.channel = channel;
        this.handler = handler;
    }

    // 解析设备名获得设备的ID列表、GO设备和RE设备的组网凭证和GO设备的 Jaccard 指数
    /**
     * 返回按照 jaccard 指数排序的列表，列表中包含了[deviceID, credential]
     * 同时将0跳路由设备记录进路由表
     * @param peersList
     * @return
     */
    public static ArrayList<JaccardIndexItem> parseDeivceName(List<WifiP2pDevice> peersList){
        // oldDeviceId 和 newDeviceId 用于求 jaccard 指数
        ArrayList<JaccardIndexItem> jaccardIndexList = new ArrayList<>(); //[deviceID,credential,jaccardIndex]

        for(int i=0;i<peersList.size();i++){
            String[] deviceNamePart = peersList.get(i).deviceName.split("_");

            if(deviceNamePart.length>0){
                // 获取设备的 Jaccard 指数
                if(deviceNamePart[0].equals("GO") && deviceNamePart.length>=4){
                    RoutingTableItem item = new RoutingTableItem(DeviceAttributes.androidID,deviceNamePart[1],0);
                    DeviceAttributes.routingTable.add(item);
                    DeviceAttributes.neighborList.add(deviceNamePart[1]);
                    jaccardIndexList.add(new JaccardIndexItem(deviceNamePart[1],deviceNamePart[2],Integer.parseInt(deviceNamePart[3])));
                }
            }
        }

        Collections.sort(jaccardIndexList, new Comparator<JaccardIndexItem>() {
            @Override
            public int compare(JaccardIndexItem a, JaccardIndexItem b) {
                return b.value-a.value;
            }
        });
        return jaccardIndexList;
    }

    @Override
    public void run() {
        // 1. 启动对等发现
        DeviceAttributes.foundP2pDevicesDone = false;
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(MainActivity.TAG,"成功开始扫描P2P设备");
                handler.obtainMessage(MainActivity.SET_TEXTVIEW,"成功开始扫描P2P设备").sendToTarget();
            }

            @Override
            public void onFailure(int reason) {
                Log.d(MainActivity.TAG,"失败开始扫描P2P设备");
                handler.obtainMessage(MainActivity.SET_TEXTVIEW,"失败开始扫描P2P设备").sendToTarget();
            }
        });
    }

    static public class JaccardIndexItem{
        String deviceID;
        String credential;
        int value;

        public JaccardIndexItem(String deviceID, String credential, int value) {
            this.deviceID = deviceID;
            this.credential = credential;
            this.value = value;
        }

        public String getDeviceID() {
            return deviceID;
        }

        public String getCredential() {
            return credential;
        }

        public int getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "JaccardIndexItem{" +
                    "deviceID='" + deviceID + '\'' +
                    ", credential='" + credential + '\'' +
                    ", value=" + value +
                    '}';
        }
    }
}
