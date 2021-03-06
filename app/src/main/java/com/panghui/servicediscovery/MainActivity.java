package com.panghui.servicediscovery;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ZoomControls;

import com.baidu.location.LocationClient;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "ServiceDiscovery";
    public static final int SET_TEXTVIEW = 1000;
    public static final int DISCOVERY_SERVICE_DONE = 1001;
    public static final int HANDLE_REMOTE_LOCATION_ITEMS_DONE = 1002;
    public static final int SET_OWN_LOCATION = 1003;
    public static final int SET_OTHERS_LOCATION = 1004;
    public static final int HANDLE_REMOTE_MESSAGE_ITEMS_DONE = 1005;

    private WifiP2pManager wifiP2pManager;
    private Channel channel;

    private String localAndroidID;

    public TextView log;
    public ScrollView scrollView;
    public EditText text;

    // 地图部分
    private MapView mMapView = null;
    public BaiduMap mBaiduMap = null;
    public LocationClient mLocationClient = null;
    private MyLocationListener myLocationListener;

    // 服务发现坐标部分
    public HashMap<String,String> localLocationAlertTable = new HashMap<>(); // [sourceDeviceId,location]
    public HashMap<String,String> remoteLocationAlertTable = new HashMap<>(); // [sourceDeviceId,location]
    public LinkedList<AlertMessage> localLocationItems = new LinkedList<>();
    public HashMap<String,Integer> locationSeqNoRecord = new HashMap<>();// [deviceID,locationSeqNo]
    public ConcurrentHashMap<String,Integer> locationTTL = new ConcurrentHashMap<>();// [location,TTL]
    public Set<String> otherLocationSet = new HashSet<>(); // 收到其他的坐标位置集合
    WifiP2pDnsSdServiceInfo locationServiceInfo;

    // 服务发现消息部分
    public HashMap<String,String> localMessageAlertTable = new HashMap<>(); // [sourceDeviceId,message]
    public HashMap<String,String> remoteMessageAlertTable = new HashMap<>(); // [sourceDeviceId,message]
    public LinkedList<AlertMessage> localMessageItems = new LinkedList<>();
    public HashMap<String,Integer> messageSeqNoRecord = new HashMap<>();// [deviceID,messageSeqNo]
    public ConcurrentHashMap<String,Integer> messageTTL = new ConcurrentHashMap<>();// [message,TTL]
    public Set<String> otherMessageSet = new HashSet<>(); // 收到其他的信息位置集合
    WifiP2pDnsSdServiceInfo messageServiceInfo;

    // 坐标标记图
    BitmapDescriptor bitmap_self = BitmapDescriptorFactory.fromResource(R.drawable.location_image_self);
    BitmapDescriptor bitmap_others = BitmapDescriptorFactory.fromResource(R.drawable.location_image_others);

    public int messageSeqNo = 0;
    public int TTL = 10;

    Timer Atimer = null;
    TimerTask handlerAlerMessageTask = null;

    /**
     * 周期性发现任务
     */
    Timer Btimer = null;
    TimerTask discoverServiceTask = null;

    Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = findViewById(R.id.text);
        Button addLocalService = findViewById(R.id.addLocalService);
        Button discoverService = findViewById(R.id.discoverService);
        Button removeLocalService = findViewById(R.id.removeLocalService);
        Button showAllMessage = findViewById(R.id.showAllMessage);
        log = findViewById(R.id.log);
        scrollView = findViewById(R.id.scrollView);

        // 获取地图控件引用
        mMapView = findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap(); // 获取地图实例
        mBaiduMap.setMyLocationEnabled(true); // 开启定位图层

        myLocationListener=new MyLocationListener(mBaiduMap,handler);

        // 地图部分
        mLocationClient = new LocationClient(getApplicationContext()); // 声明 LocationClient 类
        mLocationClient.registerLocationListener(myLocationListener);

        /**
         * 隐藏百度地图logo
         */
        View child = mMapView.getChildAt(1);
        if(child != null && (child instanceof ImageView || child instanceof ZoomControls)){
            child.setVisibility(View.INVISIBLE);
        }

        addLocalService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = text.getText().toString();
                handler.obtainMessage(SET_TEXTVIEW,"输入了："+data+"deviceID:"+localAndroidID).sendToTarget();

                // 生成一条本地警报记录
                Random random = new Random();
                double lat=34.233894+random.nextDouble()*0.001,lng=108.920091+random.nextDouble()*0.001;

                String location_str = lat+"_"+lng;
                AlertMessage item = new AlertMessage(localAndroidID,0,location_str,true);
                localLocationItems.add(item);// 保存本地条目实例
                locationTTL.put(Utils.AlerMessageToString(item),TTL); // 为该条目设置 TTL
                localLocationAlertTable.put(localAndroidID,Utils.AlerMessageToString(item));

                LatLng own_position = new LatLng(lat,lng);
                otherLocationSet.add(location_str);
                handler.obtainMessage(MainActivity.SET_OWN_LOCATION,own_position).sendToTarget();

                // 注册本地服务
                locationServiceInfo = WifiP2pDnsSdServiceInfo.newInstance("alert","location",localLocationAlertTable);
                AlertMessage messageItem = new AlertMessage(localAndroidID,messageSeqNo++,data,true);
                localMessageItems.add(messageItem); // 保存本地消息条目
                messageTTL.put(Utils.AlerMessageToString(messageItem),TTL); // 为消息条目设置 TTL
                localMessageAlertTable.put(localAndroidID,Utils.AlerMessageToString(messageItem));
                messageServiceInfo = WifiP2pDnsSdServiceInfo.newInstance("alert","message",localMessageAlertTable);
                startRegistration(locationServiceInfo);
                startRegistration(messageServiceInfo);
                text.setText("");
            }
        });

        removeLocalService.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                wifiP2pManager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
//                        Log.d(MainActivity.TAG,"clearLocalServices 成功");// Success!
//                        handler.obtainMessage(SET_TEXTVIEW,"clearLocalServices 成功").sendToTarget();
                    }

                    @Override
                    public void onFailure(int reason) {
//                        Log.d(MainActivity.TAG,"clearLocalServices 成功");// Success!
//                        handler.obtainMessage(SET_TEXTVIEW,"clearLocalServices 成功").sendToTarget();
                    }
                });
            }
        });

        discoverService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Btimer == null){
                    // 启动服务发现定时器
                    startBTimer();
                }

//                serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
//                discoverService();
//
//                wifiP2pManager.addServiceRequest(channel,
//                        serviceRequest,
//                        new WifiP2pManager.ActionListener() {
//                            @Override
//                            public void onSuccess() {
////                                Log.d(MainActivity.TAG,"addServiceRequest 成功");// Success!
////                                handler.obtainMessage(SET_TEXTVIEW,"addServiceRequest 成功").sendToTarget();
//                            }
//
//                            @Override
//                            public void onFailure(int code) {
////                                Log.d(MainActivity.TAG,"addServiceRequest 失败");// Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
////                                handler.obtainMessage(SET_TEXTVIEW,"addServiceRequest 失败").sendToTarget();
//                            }
//                        });
//                wifiP2pManager.discoverServices(channel, new WifiP2pManager.ActionListener() {
//                    @Override
//                    public void onSuccess() {
////                        Log.d(MainActivity.TAG,"discoverServices 成功");
////                        handler.obtainMessage(SET_TEXTVIEW,"discoverServices 成功").sendToTarget();
//                    }
//
//                    @Override
//                    public void onFailure(int reason) {
////                        Log.d(MainActivity.TAG,"discoverServices 失败");
////                        handler.obtainMessage(SET_TEXTVIEW,"discoverServices 失败").sendToTarget();
//                    }
//                });
            }
        });

        showAllMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(MainActivity.TAG,"showAllMessage:"+ localLocationAlertTable.toString());// Success!
                handler.obtainMessage(SET_TEXTVIEW,"showAllMessage:"+ localLocationAlertTable.toString()).sendToTarget();
            }
        });

        List<String> permissionList = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }

        if(!permissionList.isEmpty()){
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this,permissions,1);
        }else {
            requestLocation();
        }

        localAndroidID = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID).substring(0,4);
        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);

        // 1. 通过WLAN 框架注册应用。必须调用此方法，然后再调用任何其他 WLAN P2P 方法
        channel = wifiP2pManager.initialize(this,getMainLooper(),null);
        // 注册服务发现的回调接口
        discoverService();
    }

    private void requestLocation(){
        mLocationClient.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
    }

    /**
     * 打开定时器，以固定的速率执行服务发现任务
     * 每5秒执行一次
     */
    private void startBTimer(){
        if(Btimer ==null){
            Btimer = new Timer();
        }

        if(discoverServiceTask == null){
            discoverServiceTask = new DiscoverServiceTask();
        }

        if(Btimer !=null && discoverServiceTask != null){
            Btimer.scheduleAtFixedRate(discoverServiceTask,random.nextInt(5)*100,10*1000);
        }
    }

    /**
     * 管理本地条目的征集周期(属于电量优化部分)
     */
    class DecreaseTTLForLocalItems implements Runnable{

        @Override
        public void run() {
            for(String str: locationTTL.keySet()){
                if(locationTTL.get(str)<=0){
                    locationTTL.remove(str);
                }else {
                    locationTTL.put(str, locationTTL.get(str)-1);
                }
            }
        }
    }

    /**
     * 服务发现任务
     */
    class DiscoverServiceTask extends TimerTask{

        @Override
        public void run() {
            WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
            wifiP2pManager.addServiceRequest(channel,
                    serviceRequest,
                    new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
//                            Log.d(MainActivity.TAG,"addServiceRequest 成功");// Success!
//                            handler.obtainMessage(SET_TEXTVIEW,"addServiceRequest 成功").sendToTarget();
                        }

                        @Override
                        public void onFailure(int code) {
//                            Log.d(MainActivity.TAG,"addServiceRequest 失败");// Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
//                            handler.obtainMessage(SET_TEXTVIEW,"addServiceRequest 失败").sendToTarget();
                        }
                    });

            wifiP2pManager.discoverServices(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
//                    Log.d(MainActivity.TAG,"discoverServices 成功");
//                    handler.obtainMessage(SET_TEXTVIEW,"discoverServices 成功").sendToTarget();
                }

                @Override
                public void onFailure(int reason) {
//                    Log.d(MainActivity.TAG,"discoverServices 失败");
//                    handler.obtainMessage(SET_TEXTVIEW,"discoverServices 失败").sendToTarget();
                }
            });
        }
    }


    // 注册本地坐标服务
    private void startRegistration(WifiP2pDnsSdServiceInfo serviceInfo){

        wifiP2pManager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(MainActivity.TAG,"注册本地服务成功");
                handler.obtainMessage(SET_TEXTVIEW,"注册本地服务成功").sendToTarget();
            }

            @Override
            public void onFailure(int reason) {
                Log.d(MainActivity.TAG,"注册本地服务失败");
                handler.obtainMessage(SET_TEXTVIEW,"注册本地服务失败").sendToTarget();
            }
        });
    }


    /**
     * 注册服务发现的回调接口
     */
    private void discoverService(){
        WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            // 创建 WifiP2pManager.DnsSdTxtRecordListener 以监听传入的记录。
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomain, Map<String, String> record, WifiP2pDevice device) {
                Log.d(MainActivity.TAG,"DnsSdTxtRecord available fullDomain - "+fullDomain);
                handler.obtainMessage(SET_TEXTVIEW,"DnsSdTxtRecord available - "+ record.toString()).sendToTarget();

                if(fullDomain.equals("alert.location.local.")){
                    new Thread(new HandlerRemoteLocationItemTask((HashMap<String, String>) record)).start();// 交给子线程进行处理
                }else if(fullDomain.equals("alert.message.local.")){
                    new Thread(new HandlerRemoteMessageItemTask((HashMap<String, String>) record)).start(); // 交给子线程处理
                }
            }
        };

        WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice resourceType) {
//                resourceType.deviceName = recordContent
//                        .containsKey(resourceType.deviceAddress) ? recordContent
//                        .get(resourceType.deviceAddress) : resourceType.deviceName;

                Log.d(MainActivity.TAG,"onBonjourServiceAvailable"+instanceName);
                handler.obtainMessage(SET_TEXTVIEW,"onBonjourServiceAvailable"+instanceName).sendToTarget();
                handler.obtainMessage(DISCOVERY_SERVICE_DONE).sendToTarget();
            }
        };

        wifiP2pManager.setDnsSdResponseListeners(channel,servListener,txtListener);
    }

    /**
     * 处理远程坐标条目
     */
    class HandlerRemoteLocationItemTask implements Runnable{

        HashMap<String,String> AlertTable;

        public HandlerRemoteLocationItemTask(HashMap<String,String> AlertTable){
               this.AlertTable =AlertTable;
        }

        @Override
        public void run() {
            boolean isUpdate = false; // 指示所收到的内容是否有更新
            Set<String> deviceIDSet = new HashSet<>(AlertTable.keySet());
            for(String srcDeviceID:deviceIDSet){
                if(AlertTable.get(srcDeviceID)==null) return;

                AlertMessage item = Utils.StringToAlerMessage(AlertTable.get(srcDeviceID));
                if(item==null) return;
                boolean isAdd = otherLocationSet.add(item.data);
                if(isAdd && item.data!=null){
                    String[] doubleStr = item.data.split("_");
                    if(doubleStr.length!=2) return;
                    LatLng otherLocation = new LatLng(Double.parseDouble(doubleStr[0]),Double.parseDouble(doubleStr[1]));
                    handler.obtainMessage(SET_OTHERS_LOCATION,otherLocation).sendToTarget();
                }

                if(!locationSeqNoRecord.containsKey(srcDeviceID) ||
                        item.seqNo > locationSeqNoRecord.get(srcDeviceID)){ // seqNo > lastSeenSeqNo ?

                    if(item.isValid){
                        item.lastHopAddress= localAndroidID;// 将条目中的上一跳地址改成本地地址
                        localLocationItems.add(item);// 加入本地条目
                        locationTTL.put(Utils.AlerMessageToString(item),TTL); // 为该消息添加 TTL
                        locationSeqNoRecord.put(srcDeviceID,item.seqNo); // 为该消息添加 seqNo
                        remoteLocationAlertTable.put(srcDeviceID, Utils.AlerMessageToString(item)); // if isValid is true, create an alert record
                        isUpdate=true;
                    }
                } // seqNo <= lastSeenSeqNo, discard
            }
            if(isUpdate){
                handler.obtainMessage(SET_TEXTVIEW,"处理远程坐标条目完毕").sendToTarget();
                handler.obtainMessage(HANDLE_REMOTE_LOCATION_ITEMS_DONE).sendToTarget(); // 处理完毕
            }else {
                handler.obtainMessage(SET_TEXTVIEW,"坐标条目无更新").sendToTarget();
            }
        }
    }

    /**
     * 处理远程信息条目
     */
    class HandlerRemoteMessageItemTask implements Runnable{

        HashMap<String,String> AlertTable;

        public HandlerRemoteMessageItemTask(HashMap<String,String> AlertTable){
            this.AlertTable =AlertTable;
        }

        @Override
        public void run() {
            boolean isUpdate = false; // 指示所收到的内容是否有更新
            Set<String> deviceIDSet = new HashSet<>(AlertTable.keySet());
            for(String srcDeviceID:deviceIDSet){
                if(AlertTable.get(srcDeviceID)==null) return;

                AlertMessage item = Utils.StringToAlerMessage(AlertTable.get(srcDeviceID));
                if(item==null) return;

                if(!messageSeqNoRecord.containsKey(srcDeviceID) ||
                        item.seqNo > messageSeqNoRecord.get(srcDeviceID)){ // seqNo > lastSeenSeqNo ?

                    if(item.isValid){
                        item.lastHopAddress= localAndroidID;// 将条目中的上一跳地址改成本地地址
                        localMessageItems.add(item);// 加入本地条目
                        messageTTL.put(Utils.AlerMessageToString(item),TTL); // 为该消息添加 TTL
                        messageSeqNoRecord.put(srcDeviceID,item.seqNo); // 为该消息添加 seqNo
                        remoteMessageAlertTable.put(srcDeviceID, Utils.AlerMessageToString(item)); // if isValid is true, create an alert record
                        isUpdate=true;
                    }
                } // seqNo <= lastSeenSeqNo, discard
            }
            if(isUpdate){
                handler.obtainMessage(SET_TEXTVIEW,"处理远程消息条目完毕").sendToTarget();
                handler.obtainMessage(HANDLE_REMOTE_MESSAGE_ITEMS_DONE).sendToTarget(); // 处理完毕
            }else {
                handler.obtainMessage(SET_TEXTVIEW,"消息条目无更新").sendToTarget();
            }
        }
    }



    public Handler handler = new Handler(){
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case SET_TEXTVIEW:{
                    String str = (String) log.getText();
                    String res = (String) msg.obj;
                    log.setText(str+"\n"+res);
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    break;
                }

                case DISCOVERY_SERVICE_DONE:{
                    wifiP2pManager.clearServiceRequests(channel,new WifiP2pManager.ActionListener(){

                        @Override
                        public void onSuccess() {
//                            Log.d(TAG,"clearServiceRequests 成功");
//                            handler.obtainMessage(SET_TEXTVIEW,"clearServiceRequests 成功").sendToTarget();
                        }

                        @Override
                        public void onFailure(int reason) {
//                            Log.d(TAG,"clearServiceRequests 失败:"+reason);
//                            handler.obtainMessage(SET_TEXTVIEW,"clearServiceRequests 失败:"+reason).sendToTarget();
                        }
                    });
                    break;
                }

                case HANDLE_REMOTE_LOCATION_ITEMS_DONE:{
                    // 清除本地服务
                    wifiP2pManager.removeLocalService(channel, locationServiceInfo, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG,"removeLocalService 成功");
                            handler.obtainMessage(SET_TEXTVIEW,"removeLocalService 成功").sendToTarget();
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.d(TAG,"removeLocalService 失败");
                            handler.obtainMessage(SET_TEXTVIEW,"removeLocalService 失败").sendToTarget();
                        }
                    });

                    Set<String> keySet = new HashSet<>(remoteLocationAlertTable.keySet());
                    for (String key : keySet){
                        localLocationAlertTable.put(key, remoteLocationAlertTable.get(key));
                    }

                    WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("alert","location", localLocationAlertTable);
                    startRegistration(serviceInfo);
                    break;
                }

                case HANDLE_REMOTE_MESSAGE_ITEMS_DONE:{
                    // 清除本地服务
                    wifiP2pManager.removeLocalService(channel, messageServiceInfo, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG,"removeLocalService 成功");
                            handler.obtainMessage(SET_TEXTVIEW,"removeLocalService 成功").sendToTarget();
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.d(TAG,"removeLocalService 失败");
                            handler.obtainMessage(SET_TEXTVIEW,"removeLocalService 失败").sendToTarget();
                        }
                    });

                    Set<String> keySet = new HashSet<>(remoteMessageAlertTable.keySet());
                    for (String key : keySet){
                        localMessageAlertTable.put(key, remoteMessageAlertTable.get(key));
                    }

                    WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("alert","location", localMessageAlertTable);
                    startRegistration(serviceInfo);
                    break;
                }

                case SET_OWN_LOCATION:{
                    LatLng own_position = (LatLng) msg.obj;
                    //设置缩放中心点；缩放比例；
                    MapStatus.Builder builder = new MapStatus.Builder();
                    builder.target(own_position).zoom(18.0f);
                    //给地图设置状态
                    mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
                    OverlayOptions own_position_overlay = new MarkerOptions().position(own_position).icon(bitmap_self);
                    mBaiduMap.addOverlay(own_position_overlay); // 在地图上增加图层
                    break;
                }

                case SET_OTHERS_LOCATION:{
                    LatLng others_position = (LatLng) msg.obj;
                    OverlayOptions options = new MarkerOptions().position(others_position).icon(bitmap_others);
                    mBaiduMap.addOverlay(options);// 在地图上增加图层
                    break;
                }

                default:
                    break;
            }
        }
    };
}