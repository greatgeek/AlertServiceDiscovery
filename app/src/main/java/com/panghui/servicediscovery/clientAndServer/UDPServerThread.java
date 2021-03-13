package com.panghui.servicediscovery.clientAndServer;

import android.os.Handler;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.panghui.servicediscovery.DeviceAttributes;
import com.panghui.servicediscovery.MainActivity;
import com.panghui.servicediscovery.Utils;
import com.panghui.servicediscovery.routing.MessageItem;
import com.panghui.servicediscovery.routing.RoutingTableItem;
import com.panghui.servicediscovery.routing.SourcePayloadAndLocation;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;

public class UDPServerThread extends Thread{
    Handler handler;
    String Andriod_ID;
    int receivePort;
    String ip;

    public UDPServerThread(String Android_ID,Handler handler,int receivePort){
        this.Andriod_ID = Android_ID;
        this.handler = handler;
        this.receivePort = receivePort;
        this.ip = Utils.getIPAddress(true);
        handler.obtainMessage(MainActivity.SET_TEXTVIEW,"设备IP为:"+ip).sendToTarget();
    }
    @Override
    public void run() {
        listen();
    }

    private void listen(){
        String TAG = "Listen";
        while (true){
            Log.d(TAG,"UDP Listen begin");
            handler.obtainMessage(MainActivity.SET_TEXTVIEW,"UDP Listen begin").sendToTarget();
            DatagramSocket rds = null;
            try{
//                int receivePort = 23000;
                byte[] inBuf = new byte[1024];
                rds = new DatagramSocket(receivePort);
                DatagramPacket inPacket = new DatagramPacket(inBuf,inBuf.length);
                rds.receive(inPacket);
                rds.setSoTimeout(1000);

                InetAddress address = inPacket.getAddress(); //获取发送方的IP地址
                String clientIP = address.toString();
                clientIP = clientIP.substring(1);
                if(clientIP.equals(ip)){ // 若收到来自本设备的数据包，则重新开始循环
                    continue;
                }

                String destination = "fa58"; // fa58 just a example
                MessageItem item = new MessageItem(Andriod_ID,destination, MessageItem.TEXT_TYPE,"ack",DeviceAttributes.locationStr);

                String str = JSON.toJSONString(item);
                sendMessage(str,clientIP); // 接收到消息后，回复一个 ack

//                handler.obtainMessage(MainActivity.FORWARD,inPacket).sendToTarget(); // 转发
                handler.obtainMessage(MainActivity.SET_TEXTVIEW,"clientIP:"+clientIP).sendToTarget();
                handler.obtainMessage(MainActivity.SET_TEXTVIEW,"设备IP为:"+ip).sendToTarget();

                String rdata = new String(inPacket.getData()).trim();
                parseUDPPayload(rdata);
                Log.d(TAG,rdata);

                handler.obtainMessage(MainActivity.SET_TEXTVIEW,rdata+" "+"from "+ clientIP).sendToTarget();
                // 方案一：每发送完一条消息就要断开连接进入GO； 方案二：每发送完成一条消息可以继续发送，等待用户已经确定发送完成消息后，再手动进入GO状态
//                if(clientIP.equals("192.168.49.1")){
//                    // 收到来自GO 的数据包后，即可以断开连接
//                    handler.obtainMessage(MainActivity.DISCONNECT_FROM_GO_DONE).sendToTarget();
//                }

            }catch (SocketTimeoutException e){
                Log.e(TAG,"listen timeout");
                handler.obtainMessage(MainActivity.SET_TEXTVIEW,"listen timeout").sendToTarget();
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                if(rds != null && !rds.isClosed()){
                    rds.close();
                }
            }
        }
    }

    /**
     * 发送消息
     * @param str 消息内容
     * @param ip 指定 IP 地址
     * @throws UnknownHostException
     */
    private void sendMessage(String str,String ip) throws UnknownHostException {
        String TAG = "sendMessage";
        Log.d(TAG,"发送数据");
        handler.obtainMessage(MainActivity.SET_TEXTVIEW,"发送数据").sendToTarget();
        InetAddress broadcastAddress = InetAddress.getByName(ip);
        DatagramSocket ds = null;

        try {
            int sendPort = 23000;
            ds = new DatagramSocket();
            ds.setBroadcast(true);
            DatagramPacket dp = new DatagramPacket(str.getBytes(), str.getBytes().length, broadcastAddress, sendPort);
            ds.send(dp);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ds != null) ds.close();
        }
    }

    /**
     * 解析 UDP payload
     * @param UDPpayload
     */
    private void parseUDPPayload(String UDPpayload){
        MessageItem item = JSON.parseObject(UDPpayload, MessageItem.class);
        if(item.getType()== MessageItem.TEXT_TYPE){
            String source = item.getSource();
            if(source.equals(DeviceAttributes.androidID)) return;
            String payload = item.getPayload();
            String location = item.getLocation();
            handler.obtainMessage(MainActivity.SET_TEXTVIEW,"收到 MESSAGE_TYPE: "+payload).sendToTarget();
            handler.obtainMessage(MainActivity.MESSAGE_RECEIVE,new SourcePayloadAndLocation(source,payload,location)).sendToTarget();
        }else if(item.getType()== MessageItem.ROUTING_TYPE){
            handler.obtainMessage(MainActivity.SET_TEXTVIEW,"收到 ROUTING_TYPE").sendToTarget();
            RoutingTableItem routingTableItem = JSON.parseObject(item.getPayload(),RoutingTableItem.class);
            addRoutingTable(item.getPayload());
            handler.obtainMessage(MainActivity.SET_TEXTVIEW,
                    routingTableItem.getSource()+"-"+routingTableItem.getDestination()+"-"+routingTableItem.getHops()).sendToTarget();
        }
    }

    /**
     * 更新路由表
     * @param payloadStr
     */
    private void addRoutingTable(String payloadStr){
        RoutingTableItem routingTableItem = JSON.parseObject(payloadStr,RoutingTableItem.class);
        String gateway = routingTableItem.getSource();
        String destination = routingTableItem.getDestination();
        int hops = routingTableItem.getHops();

        // destination 设备为自身则无需更新路由表
        if(destination.equals(DeviceAttributes.androidID)) return;

        LinkedList<RoutingTableItem> routingTable = DeviceAttributes.routingTable;
        for(int i=0;i < routingTable.size();i++){
            // 发现更短的路径（对已存在的路径进行更改）
            if(routingTable.get(i).getDestination().equals(destination) &&
                    (!routingTable.get(i).getSource().equals(gateway) && routingTable.get(i).getHops()>hops+1)){
                routingTable.get(i).setSource(gateway);
                routingTable.get(i).setHops(hops+1);
                return;
            }
        }

        // 添加原本不存在的路径
        // 若gateway 是邻居设备，destination 不是邻居设备，则添加进路由表并跳数+1
        if(DeviceAttributes.neighborList.contains(gateway) &&
                !DeviceAttributes.neighborList.contains(destination)){

            RoutingTableItem item = new RoutingTableItem( gateway,
                    destination, hops + 1);

            DeviceAttributes.routingTable.add(item);
        }
    }
}
