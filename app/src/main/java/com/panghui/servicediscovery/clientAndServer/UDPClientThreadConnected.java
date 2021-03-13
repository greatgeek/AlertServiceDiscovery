package com.panghui.servicediscovery.clientAndServer;

import android.os.Handler;

import com.alibaba.fastjson.JSON;
import com.panghui.servicediscovery.DeviceAttributes;
import com.panghui.servicediscovery.routing.MessageItem;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPClientThreadConnected extends Thread {

    public Handler handler;
    public String Android_ID;
    public DatagramSocket client;
    public UDPClientThreadConnected(String Android_ID, Handler handler) throws SocketException, UnknownHostException {
        this.Android_ID = Android_ID;
        this.handler=handler;
    }

    @Override
    public void run() {
        try {
            client = new DatagramSocket();
            InetAddress ipAddress = InetAddress.getByName("192.168.49.1");
            client.connect(ipAddress,23000);
            MessageItem item = new MessageItem(Android_ID, DeviceAttributes.currentlyConnectedDevice, MessageItem.TEXT_TYPE,"hello",DeviceAttributes.locationStr);
            sendMessageConnected(JSON.toJSONString(item));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    private void sendMessage(String str) throws UnknownHostException {
//        String TAG = "sendMessage";
//        Log.d(TAG,"发送数据");
//        handler.obtainMessage(MainActivity.SET_TEXTVIEW,"发送数据").sendToTarget();
//        InetAddress broadcastAddress = InetAddress.getByName("192.168.49.255");
//        DatagramSocket ds = null;
//
//        try {
//            int sendPort = 23000;
//            ds = new DatagramSocket();
//            ds.setBroadcast(true);
//            DatagramPacket dp = new DatagramPacket(str.getBytes(), str.getBytes().length, broadcastAddress, sendPort);
//            ds.send(dp);
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if (ds != null) ds.close();
//        }
//    }

    private void sendMessageConnected(String str) throws IOException {
        DatagramPacket dp = new DatagramPacket(str.getBytes(), str.getBytes().length);
        client.send(dp);
    }
}
