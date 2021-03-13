package com.panghui.servicediscovery.wifiDirectAndLegacy;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Handler;
import android.util.Log;

import com.panghui.servicediscovery.DeviceAttributes;
import com.panghui.servicediscovery.MainActivity;

import java.util.List;

public class MyPeerListListener implements PeerListListener {
    private List<WifiP2pDevice> peers;
    private Handler handler;
        // 将 Device 列表传递到 MainActivity
    public MyPeerListListener(List<WifiP2pDevice> peers,
                              Handler handler){
        this.handler=handler;
        this.peers=peers;
    }
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peersList) {

        if(DeviceAttributes.foundP2pDevicesDone){
            return;
        }

        // 获取 peers
        peers.clear();
        peers.addAll(peersList.getDeviceList());

        Log.d(MainActivity.TAG,"找到了peer设备:"+peers.size());
        handler.obtainMessage(MainActivity.SET_TEXTVIEW,"找到了peer设备:"+peers.size()).sendToTarget();
        for (int i=0;i<peers.size();i++){
            handler.obtainMessage(MainActivity.SET_TEXTVIEW,peers.get(i).deviceName).sendToTarget();
        }

        handler.obtainMessage(MainActivity.FOUND_P2P_DEVICES_DONE).sendToTarget();// 表示已找到设备
    }
}
