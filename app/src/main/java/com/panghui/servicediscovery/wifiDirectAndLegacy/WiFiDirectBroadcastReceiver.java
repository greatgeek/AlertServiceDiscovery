package com.panghui.servicediscovery.wifiDirectAndLegacy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Handler;
import android.util.Log;

import com.panghui.servicediscovery.DeviceAttributes;
import com.panghui.servicediscovery.MainActivity;

import java.util.List;

import static com.panghui.servicediscovery.MainActivity.SET_TEXTVIEW;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager wifiP2pManager;
    private Channel channel;
    private MainActivity activity;
    private MyPeerListListener myPeerListListener;
    private Handler handler;

    public WiFiDirectBroadcastReceiver(WifiP2pManager wifiP2pManager, Channel channel,
                                       MainActivity activity, List<WifiP2pDevice> peers, Handler handler){
        super();
        this.wifiP2pManager =wifiP2pManager;
        this.channel=channel;
        this.activity=activity;
        myPeerListListener=new MyPeerListListener(peers,handler);
        this.handler=handler;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

//        handler.obtainMessage(SET_TEXTVIEW,"DeviceAttributes.isGO： "+DeviceAttributes.isGO).sendToTarget();
        if(DeviceAttributes.isConnectedToGO){ // 若已连接到GO，则暂时不需要进行P2P设备扫描
            return;
        }

        String action = intent.getAction();
        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){

            // UI update to indicate wifi p2p status
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
            if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                // Wifi Direct mode is enabled
                activity.setIsWifiP2pEnabled(true);
            }else {
                activity.setIsWifiP2pEnabled(false);
            }
            Log.d(MainActivity.TAG,"P2P state changed - " + state);
            handler.obtainMessage(SET_TEXTVIEW,"P2P state changed : " + state).sendToTarget();
        }else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
            if(wifiP2pManager !=null){
                // 请求发现对等设备的当前列表
                wifiP2pManager.requestPeers(channel, myPeerListListener);
            }
            Log.d(MainActivity.TAG,"P2P peers changed");
            handler.obtainMessage(SET_TEXTVIEW,"P2P peers changed").sendToTarget();
        }else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
            if(wifiP2pManager == null){
                return;
            }

            NetworkInfo networkInfo = intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if(networkInfo.isConnected()){// 成为 GO 或连接到GO会引起连接改变
                Log.d(MainActivity.TAG,"p2p连接成功！");
                handler.obtainMessage(SET_TEXTVIEW,"p2p连接成功！").sendToTarget(); // 成功成为GO后会提示连接成功！1. 实际上是自己连接到了自己的组 2. 或是有其他设备连入了该GO
                wifiP2pManager.requestConnectionInfo(channel,activity); // 获取连接信息,可在此处判断是哪种情况
            }else { // 成功移除GO后会提示连接失败！实际上是自己脱离了自己的组
                Log.d(MainActivity.TAG,"p2p连接失败！");
                handler.obtainMessage(SET_TEXTVIEW,"p2p连接失败！").sendToTarget();
            }

        }else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
           WifiP2pDevice device = (WifiP2pDevice)intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE); // 获取设备名称
           Log.d(MainActivity.TAG,"显示设备名："+device.deviceName);
           handler.obtainMessage(SET_TEXTVIEW,"显示设备名："+device.deviceName).sendToTarget();
        }
    }
}
