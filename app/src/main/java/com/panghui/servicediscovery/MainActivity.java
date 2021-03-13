package com.panghui.servicediscovery;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.baidu.location.LocationClient;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.TextOptions;
import com.baidu.mapapi.model.LatLng;
import com.panghui.servicediscovery.clientAndServer.UDPClientThread;
import com.panghui.servicediscovery.clientAndServer.UDPServerThread;
import com.panghui.servicediscovery.networkManager.InformationCollection;
import com.panghui.servicediscovery.routing.MessageItem;
import com.panghui.servicediscovery.routing.SourcePayloadAndLocation;
import com.panghui.servicediscovery.showMessage.Msg;
import com.panghui.servicediscovery.showMessage.MsgAdapter;
import com.panghui.servicediscovery.wifiDirectAndLegacy.WiFiDirectBroadcastReceiver;
import com.panghui.servicediscovery.wifiDirectAndLegacy.WifiAdmin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

public class MainActivity extends AppCompatActivity implements WifiP2pManager.ConnectionInfoListener, WifiP2pManager.GroupInfoListener {

    public static final String TAG = "ServiceDiscovery";
    public static final int SET_TEXTVIEW = 1000;
    public static final int DISCOVERY_SERVICE_DONE = 1001;
    public static final int HANDLE_REMOTE_LOCATION_ITEMS_DONE = 1002;
    public static final int SET_OWN_LOCATION = 1003;
    public static final int SET_OTHERS_LOCATION = 1004;
    public static final int HANDLE_REMOTE_MESSAGE_ITEMS_DONE = 1005;

    /**
     * wifi direct and legacy
     */
    public static final int REMOVE_GROUP = 10010;
    public static final int CONNECT_TO_GO_DONE = 10011;
    public static final int FOUND_LEGACY_DEVICES_DONE = 10012;
    public static final int FOUND_P2P_DEVICES_DONE = 1013;
    public static final int BECOME_GO = 10014;
    public static final int MESSAGE_RECEIVE = 1016;
    public static final int MESSAGE_SENT = 1017;



    // wifi direct and wifi legacy manager
    private WifiP2pManager wifiP2pManager;
    private WifiManager wifiManager;
    private BatteryManager batteryManager;
    private boolean isWifiP2pEnabled = false;

    // wifiP2p and wifi legacy IntentFilter
    private final IntentFilter wifiP2pIntentFilter = new IntentFilter();
    private final IntentFilter wifiIntentFilter = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver wifiP2pReceiver = null;

    // wifiP2p discoverPeerss and wifi legacy scan result
    private List<WifiP2pDevice> peers = new ArrayList<>(); // p2p 接口扫描结果
    public List<ScanResult> devicesResult = new ArrayList<>(); // 传统 wifi 接口扫描结果
    private ArrayList<InformationCollection.JaccardIndexItem> jaccardIndexArray = new ArrayList<>(); // 从 p2p 接口扫描结果
    private int networkId; // 自动连接上的网络ID。断开时需要忘记密码，防止自动连接
    public WifiConfiguration wifiConfiguration;

    // local android device ID
    private String Android_ID;

    private String localAndroidID;

    // network credential
    private HashMap<String, String> networkCredential = new HashMap<>(); // [SSID,passphrase]

    // wifi legacy administrator
    public WifiAdmin wifiAdmin;

    // Server and Client Thread
    private UDPServerThread serverThread = null;


    // wifi ip of wifi direct and ip of wifi legacy
    static String ip = "";

    // UI 显示部分
    public TextView log;
    public ScrollView scrollView;

    // 显示消息
    private RecyclerView msgRecyclerView;
    private MsgAdapter adapter;
    private List<Msg> msgList = new ArrayList<Msg>();

    // 定向发送消息
    private Button GO;
    private EditText inputText;
    private Button send;

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
     * wifi Direct and Legacy
     */

    // global sending and receiving port
    int globalReceivePort = 23000;
    int globalSendPort = 23000;

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
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        log = findViewById(R.id.log);
        scrollView = findViewById(R.id.scrollView);

        // 定向发向消息部分控件引用
        GO = findViewById(R.id.GO);
        inputText = findViewById(R.id.inputText);
        send = findViewById(R.id.send);

        // 获取地图控件引用
        mMapView = findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap(); // 获取地图实例
        mBaiduMap.setMyLocationEnabled(true); // 开启定位图层

        myLocationListener=new MyLocationListener(mBaiduMap,handler);

        // 地图部分
        mLocationClient = new LocationClient(getApplicationContext()); // 声明 LocationClient 类
        mLocationClient.registerLocationListener(myLocationListener);

        localAndroidID = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID).substring(0,4);

        DeviceAttributes.androidID = localAndroidID;
        /**
         * 隐藏百度地图logo
         */
        View child = mMapView.getChildAt(1);
        if(child != null && (child instanceof ImageView || child instanceof ZoomControls)){
            child.setVisibility(View.INVISIBLE);
        }

//        initMsgs();
        // 显示消息
        msgRecyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        msgRecyclerView.setLayoutManager(layoutManager);
        adapter = new MsgAdapter(msgList);
        msgRecyclerView.setAdapter(adapter);

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

        // android id
        Android_ID = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID).substring(0, 4);
        DeviceAttributes.androidID = Android_ID;

        // wifiP2p manager and wifi manager
        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiAdmin = new WifiAdmin(getApplicationContext());
        batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);

        // wifi p2p intent filter
        wifiP2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        wifiP2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        wifiP2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        wifiP2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // wifi legacy intent filter
        wifiIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        wifiIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

        // wifiP2p broadcast receiver
        wifiP2pReceiver = new WiFiDirectBroadcastReceiver(wifiP2pManager, channel, this, peers, handler);

        // 1. 通过WLAN 框架注册应用。必须调用此方法，然后再调用任何其他 WLAN P2P 方法
        channel = wifiP2pManager.initialize(this,getMainLooper(),null);
        // 注册服务发现的回调接口
        discoverService();

        // 搜索与连接设备发送消息
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputStr = inputText.getText().toString().trim();
                DeviceAttributes.messageTargetID = Utils.extractIDFromMessage(inputStr);
                DeviceAttributes.messaageContent = Utils.truncateString(inputStr);

                if(DeviceAttributes.isConnectedToGO){ // 若已连接，则可以直接发送消息
                    String str = inputText.getText().toString();
                    new UDPClientThread(Android_ID,handler,globalSendPort, MessageItem.TEXT_TYPE,str).start();
                }else{
                    // 先进行组移除再启动扫描P2P设备过程
                    handler.obtainMessage(REMOVE_GROUP).sendToTarget();
                    // 启动扫描P2P设备过程
                    new InformationCollection(wifiP2pManager,channel,handler).start();
                }
            }
        });

        // 断开 Wi-Fi 连接后成为GO
        GO.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectFromGO();
                handler.obtainMessage(BECOME_GO).sendToTarget();
            }
        });

        // 应用启动即开启监听线程
        new UDPServerThread(Android_ID, handler, globalReceivePort).start();
    }


    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }



    /**
     * 添加本地服务
     */
    private void addLocalServiceToolbar(){
        // 生成一条本地警报记录
        Random random = new Random();
        double lat=34.233894+random.nextDouble()*0.001,lng=108.920091+random.nextDouble()*0.001;

        String location_str = lat+"_"+lng;
        DeviceAttributes.locationStr=location_str;
        AlertMessage item = new AlertMessage(localAndroidID,0,location_str,true);
        localLocationItems.add(item);// 保存本地条目实例
        locationTTL.put(Utils.AlerMessageToString(item),TTL); // 为该条目设置 TTL
        localLocationAlertTable.put(localAndroidID,Utils.AlerMessageToString(item));

        LatLng own_position = new LatLng(lat,lng);
        otherLocationSet.add(location_str);
        handler.obtainMessage(MainActivity.SET_OWN_LOCATION,own_position).sendToTarget();

        // 注册本地服务
        locationServiceInfo = WifiP2pDnsSdServiceInfo.newInstance("alert","location",localLocationAlertTable);
        AlertMessage messageItem = new AlertMessage(localAndroidID,messageSeqNo++,"data",true);
        localMessageItems.add(messageItem); // 保存本地消息条目
        messageTTL.put(Utils.AlerMessageToString(messageItem),TTL); // 为消息条目设置 TTL
        localMessageAlertTable.put(localAndroidID,Utils.AlerMessageToString(messageItem));
        messageServiceInfo = WifiP2pDnsSdServiceInfo.newInstance("alert","message",localMessageAlertTable);
        startRegistration(locationServiceInfo);
        startRegistration(messageServiceInfo);
    }

    /**
     * 移除本地服务
     */
    private void removeLocalServiceToolbar(){
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

    /**
     * 启动服务发现
     */
    private void discoverServiceToolbar(){
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

    /**
     * 显示所有信息
     */
    private void showAllMessage(){
        Log.d(MainActivity.TAG,"showAllMessage:"+ localLocationAlertTable.toString());// Success!
        handler.obtainMessage(SET_TEXTVIEW,"showAllMessage:"+ localLocationAlertTable.toString()).sendToTarget();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.addLocalService:
                Toast.makeText(this, "添加本地服务",Toast.LENGTH_SHORT).show();
                addLocalServiceToolbar();
                break;
            case R.id.performServiceDiscovery:
                Toast.makeText(this, "启动服务发现",Toast.LENGTH_SHORT).show();
                discoverServiceToolbar();
                break;
            default:
        }
        return true;
    }

    /**
     * 初始化消息
     */
    private void initMsgs() {
        Msg msg1 = new Msg("Hello guy.", Msg.TYPE_RECEIVED);
        msgList.add(msg1);
        Msg msg2 = new Msg("Hello. Who is that?", Msg.TYPE_SENT);
        msgList.add(msg2);
        Msg msg3 = new Msg("This is Tom. Nice talking to you. ", Msg.TYPE_RECEIVED);
        msgList.add(msg3);
    }

    private void requestLocation(){
        mLocationClient.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
        wifiP2pReceiver = new WiFiDirectBroadcastReceiver(wifiP2pManager, channel, this, peers, handler);
        registerReceiver(wifiP2pReceiver, wifiP2pIntentFilter);
        registerReceiver(wifiReceiver, wifiIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
        unregisterReceiver(wifiP2pReceiver);
        unregisterReceiver(wifiReceiver);
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
     * 获取 wifi p2p 的连接信息
     * @param info
     */
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        // InetAddress from WifiP2pInfo struct.
        String groupOwnerAddress = info.groupOwnerAddress.getHostAddress();

        // After the group negotiation, we can determine the group owner
        // (server).
        if (info.groupFormed && info.isGroupOwner) {
            // Do whatever tasks are specific to the group owner.
            // One common case is creating a group owner thread and accepting
            // incoming connections.
            DeviceAttributes.isGO = true;
            DeviceAttributes.deviceFlag = "GO"; // 设置 deviceFlag 为 GO

            // 获取组信息并修改设备名
            new GetGroupInformationAndModifyDeviceName().start();
        } else if (info.groupFormed) {
            // The other device acts as the peer (client). In this case,
            // you'll want to create a peer thread that connects
            // to the group owner.
            DeviceAttributes.isGO = false;
        }
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup group) {
        if (group == null) return;
        String groupName = group.getOwner().deviceName;
        Log.d(MainActivity.TAG, "加入组：" + groupName);
        handler.obtainMessage(SET_TEXTVIEW, "加入组：" + groupName).sendToTarget();
        Log.d(MainActivity.TAG, "网络ID：" + group.getNetworkId());
        handler.obtainMessage(SET_TEXTVIEW, "网络ID：" + group.getNetworkId()).sendToTarget();
        Log.d(MainActivity.TAG, "网络名称：" + group.getNetworkName());
        handler.obtainMessage(SET_TEXTVIEW, "网络名称：" + group.getNetworkName()).sendToTarget();
        Log.d(MainActivity.TAG, "群组密码：" + group.getPassphrase());
        handler.obtainMessage(SET_TEXTVIEW, "群组密码：" + group.getPassphrase()).sendToTarget();
        DeviceAttributes.networkCredential = group.getPassphrase();

    }

    /**
     * 管理本地条目的征集周期(属于电量优化部分)
     */
    class DecreaseTTLForLocalItems implements Runnable {

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
                    IDandLatLng iDandLatLng = new IDandLatLng(srcDeviceID,otherLocation);
                    handler.obtainMessage(SET_OTHERS_LOCATION,iDandLatLng).sendToTarget();
                }

                if(!locationSeqNoRecord.containsKey(srcDeviceID) ||
                        item.seqNo > locationSeqNoRecord.get(srcDeviceID)){ // seqNo > lastSeenSeqNo ?

                    if(item.isValid){
                        item.deviceID = localAndroidID;// 将条目中的上一跳地址改成本地地址
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
                        item.deviceID = localAndroidID;// 将条目中的上一跳地址改成本地地址
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



    /**
     * 从GO处断开
     */
    public void disconnectFromGO() {
        DeviceAttributes.isConnectedToGO = false; // 将标记位设置为 false，方便 wifiReceiver 接收广播信号
        wifiManager.disconnect();
        // 忘记网络的密码
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            wifiManager.removeNetwork(i.networkId);
        }
    }

    // wifi legacy 扫描失败
    private void scanFailure() {
        Log.d(MainActivity.TAG, "扫描失败");
        handler.obtainMessage(SET_TEXTVIEW, "扫描失败").sendToTarget();
    }

    // wifi legacy 扫描成功
    private void scanSuccess() {
        devicesResult = wifiManager.getScanResults();
        if (devicesResult.size() > 0) {
            handler.obtainMessage(FOUND_LEGACY_DEVICES_DONE).sendToTarget();
        }

        for (int i = 0; i < devicesResult.size(); i++) {
            if(devicesResult.get(i).SSID.contains("GO")){
                Log.d(MainActivity.TAG, "找到" + devicesResult.get(i).SSID);
                handler.obtainMessage(SET_TEXTVIEW, "找到" + devicesResult.get(i).SSID).sendToTarget();
            }
        }
    }


    class BecomeGroupOwner extends Thread {
        @Override
        public void run() {

            wifiP2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    DeviceAttributes.isGO = true;
                    Log.d(TAG, "成功成为GO");
                    handler.obtainMessage(SET_TEXTVIEW, "成功成为GO").sendToTarget();
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "失败成为GO");
                    handler.obtainMessage(SET_TEXTVIEW, "失败成为GO").sendToTarget();
                }
            });
        }
    }

    class GetGroupInformationAndModifyDeviceName extends Thread{
        @Override
        public void run() {
            wifiP2pManager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() { //获取组网密码
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group == null) return;
                    String groupName = group.getOwner().deviceName;
                    Log.d(MainActivity.TAG, "加入组：" + groupName);
                    handler.obtainMessage(SET_TEXTVIEW, "加入组：" + groupName).sendToTarget();
                    Log.d(MainActivity.TAG, "网络ID：" + group.getNetworkId());
                    handler.obtainMessage(SET_TEXTVIEW, "网络ID：" + group.getNetworkId()).sendToTarget();
                    Log.d(MainActivity.TAG, "网络名称：" + group.getNetworkName());
                    handler.obtainMessage(SET_TEXTVIEW, "网络名称：" + group.getNetworkName()).sendToTarget();
                    Log.d(MainActivity.TAG, "群组密码：" + group.getPassphrase());
                    handler.obtainMessage(SET_TEXTVIEW, "群组密码：" + group.getPassphrase()).sendToTarget();
                    DeviceAttributes.networkCredential = group.getPassphrase();

                    // 修改设备名
                    try {
                        Method m = wifiP2pManager.getClass().getMethod("setDeviceName", new Class[]{
                                channel.getClass(), String.class, WifiP2pManager.ActionListener.class});

                        String wifiP2pDeviceName = DeviceAttributes.deviceFlag;
                        wifiP2pDeviceName += "_" + DeviceAttributes.androidID;
                        wifiP2pDeviceName += "_" + DeviceAttributes.networkCredential;
                        int battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                        int jaccardIndex = battery;
                        DeviceAttributes.jaccardIndex = jaccardIndex;
                        wifiP2pDeviceName += "_" + DeviceAttributes.jaccardIndex;

                        m.invoke(wifiP2pManager, channel, wifiP2pDeviceName, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Log.d(MainActivity.TAG, "修改名称成功");
                                handler.obtainMessage(SET_TEXTVIEW, "修改名称成功").sendToTarget();
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d(MainActivity.TAG, "修改名称失败");
                                handler.obtainMessage(SET_TEXTVIEW, "修改名称失败").sendToTarget();
                            }
                        });
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        Log.d(MainActivity.TAG, "修改名称失败");
                        handler.obtainMessage(SET_TEXTVIEW, "修改名称失败").sendToTarget();
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    class RemoveGroupOwnerWithoutScanP2pDevices extends Thread{
        @Override
        public void run() {
            wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    DeviceAttributes.isGO = false;
                    Log.d(TAG,"成功移除组");
                    handler.obtainMessage(SET_TEXTVIEW,"成功移除组").sendToTarget();
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG,"失败移除组");
                    handler.obtainMessage(SET_TEXTVIEW,"失败移除组"+reason).sendToTarget();
                }
            });
        }
    }

    class RemoveGroupOwner extends Thread{
        @Override
        public void run() {
            wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    DeviceAttributes.isGO = false;
                    Log.d(TAG,"成功移除组");
                    handler.obtainMessage(SET_TEXTVIEW,"成功移除组").sendToTarget();
                    // 组移除成功后，启动P2P设备扫描过程
                    new InformationCollection(wifiP2pManager,channel,handler).start();
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG,"失败移除组");
                    handler.obtainMessage(SET_TEXTVIEW,"失败移除组"+reason).sendToTarget();
                }
            });
        }
    }

    class StopPeerDiscovery extends Thread{
        @Override
        public void run() {
            // 找到peers 设备后，停止发现过程
            DeviceAttributes.foundP2pDevicesDone = true;
            wifiP2pManager.stopPeerDiscovery(channel,new WifiP2pManager.ActionListener(){
                @Override
                public void onSuccess() {
                    Log.d(MainActivity.TAG,"停止发现过程成功");
                    handler.obtainMessage(SET_TEXTVIEW,"停止发现过程成功").sendToTarget();
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(MainActivity.TAG,"停止发现过程失败："+reason);
                    handler.obtainMessage(SET_TEXTVIEW,"停止发现过程失败").sendToTarget();
                }
            });
        }
    }

    // wifi legacy scan receiver
    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

//            handler.obtainMessage(MainActivity.SET_TEXTVIEW,"DeviceAttributes.isConnectedToGO: "+DeviceAttributes.isConnectedToGO).sendToTarget();
//            handler.obtainMessage(MainActivity.SET_TEXTVIEW,"DeviceAttributes.isGO: "+DeviceAttributes.isGO).sendToTarget();

            if (DeviceAttributes.isConnectedToGO || DeviceAttributes.isGO) {
                return;
            }

            String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    scanSuccess();
                } else {
                    scanFailure();
                }
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info.getState().equals(NetworkInfo.State.CONNECTED)) { // 以 wifi Legacy 连接到GO时，会收到该广播
                    DeviceAttributes.isConnectedToGO = true;

                    ip = Utils.getIPAddress(true);

                    Log.d(MainActivity.TAG, "wifi legacy 成功连接");
                    handler.obtainMessage(SET_TEXTVIEW, "wifi legacy 成功连接").sendToTarget();
                    handler.obtainMessage(CONNECT_TO_GO_DONE).sendToTarget();

                    Log.d(MainActivity.TAG, "wifi legacy ip address: " + Utils.getIPAddress(true));
                    handler.obtainMessage(SET_TEXTVIEW, "wifi legacy ip address: " + Utils.getIPAddress(true)).sendToTarget();
                } else if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                    DeviceAttributes.isConnectedToGO = false;

                    Log.d(MainActivity.TAG, "wifi legacy 断开连接");
                    handler.obtainMessage(SET_TEXTVIEW, "wifi legacy 断开连接").sendToTarget();

//                    handler.obtainMessage(BECOME_GO).sendToTarget();
                }
            }
        }
    };

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
                    MarkerOptions own_position_markerOptions = new MarkerOptions()
                            .position(own_position)
                            .clickable(true)
                            .icon(bitmap_self);
                    Marker ownPositionMarker = (Marker) mBaiduMap.addOverlay(own_position_markerOptions); // 在地图上增加图层

                    Bundle mBundle = new Bundle();
                    mBundle.putString("id",DeviceAttributes.androidID);
                    ownPositionMarker.setExtraInfo(mBundle);

                    mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(Marker marker) {
                            Bundle bundle = marker.getExtraInfo();
                            String id = bundle.getString("id");
                            String content = "to ["+id+"]:";
                            inputText.setText(content);
                            inputText.setSelection(content.length());
                            handler.obtainMessage(SET_TEXTVIEW,"点击了 marker : "+id).sendToTarget();
                            return true;
                        }
                    });

                    OverlayOptions mTextOptions = new TextOptions()
                            .text(DeviceAttributes.androidID)
                            .bgColor(0xAAFFFF00)
                            .fontSize(24)
                            .rotate(-30)
                            .fontColor(R.color.colorText)
                            .position(own_position);
                    mBaiduMap.addOverlay(mTextOptions);
                    break;
                }

                case SET_OTHERS_LOCATION:{
                    IDandLatLng others_position = (IDandLatLng) msg.obj;
                    if(others_position==null) return;

                    OverlayOptions options = new MarkerOptions()
                            .position(others_position.getLatLng())
                            .clickable(true)
                            .icon(bitmap_others);
                    Marker otherPositionMarker = (Marker) mBaiduMap.addOverlay(options);// 在地图上增加图层

                    Bundle mBundle = new Bundle();
                    mBundle.putString("id",others_position.getId());
                    otherPositionMarker.setExtraInfo(mBundle);
                    mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(Marker marker) {
                            Bundle bundle = marker.getExtraInfo();
                            String id = bundle.getString("id");
                            String content = "to ["+id+"]:";
                            inputText.setText(content);
                            inputText.setSelection(content.length());
                            handler.obtainMessage(SET_TEXTVIEW,"点击了 marker : "+id).sendToTarget();
                            return true;
                        }
                    });

                    OverlayOptions mTextOptions = new TextOptions()
                            .text(others_position.getId())
                            .bgColor(0xAAFFFF00)
                            .fontSize(24)
                            .rotate(-30)
                            .fontColor(R.color.colorText)
                            .position(others_position.getLatLng());
                    mBaiduMap.addOverlay(mTextOptions);
                    break;
                }

                case REMOVE_GROUP:{
                    new RemoveGroupOwner().start();
                    break;
                }

                case BECOME_GO:{
                    new BecomeGroupOwner().start();
                    break;
                }

                case CONNECT_TO_GO_DONE:{
                    handler.obtainMessage(SET_TEXTVIEW,"成功连接上GO").sendToTarget();
                    String str = inputText.getText().toString();
                    new UDPClientThread(Android_ID,handler,globalSendPort, MessageItem.TEXT_TYPE,str).start();
                    handler.obtainMessage(MESSAGE_SENT,DeviceAttributes.messaageContent + " to ["+DeviceAttributes.messageTargetID+"]").sendToTarget();
                    break;
                }

                case FOUND_LEGACY_DEVICES_DONE:{
                    handler.obtainMessage(SET_TEXTVIEW,"FOUND_LEGACY_DEVICES_DONE").sendToTarget();
                    boolean foundTargetID = false;
                    String networkSSID = null;
                    String credential = null;
                    if(DeviceAttributes.messageTargetID!=null
                            && !DeviceAttributes.messageTargetID.equals("")){
                        for(int i=0;i<jaccardIndexArray.size();i++){
                            if(DeviceAttributes.messageTargetID .equals(jaccardIndexArray.get(i).getDeviceID()) ){
                                networkSSID = jaccardIndexArray.get(i).getDeviceID() ;
                                credential = jaccardIndexArray.get(i).getCredential();
                                handler.obtainMessage(SET_TEXTVIEW,networkSSID+":"+credential).sendToTarget();
                                foundTargetID = true;

                                for(int j=0;j<devicesResult.size();j++){
                                    // The detected signal level in dBm, also known as the RSSI.
                                    // TODO: RSSI强度用来辅助选择连接哪个GO
                                    int RSSI = devicesResult.get(j).level;
                                    if(devicesResult.get(j).SSID.contains(networkSSID)){
                                        wifiConfiguration = wifiAdmin.CreateWifiInfo(devicesResult.get(j).SSID,credential,3);
                                        networkId = wifiConfiguration.networkId;
                                        wifiAdmin.addNetwork(wifiConfiguration);
                                        DeviceAttributes.currentlyConnectedDevice=networkSSID; // 设置目前连接着的设备名称
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // 进行自动连接
                    if( foundTargetID==false &&jaccardIndexArray.size()>0){
                        networkSSID = jaccardIndexArray.get(0).getDeviceID();
                        credential = jaccardIndexArray.get(0).getCredential();
                        handler.obtainMessage(SET_TEXTVIEW,networkSSID+":"+credential).sendToTarget();
                        for(int i=0;i<devicesResult.size();i++){
                            // The detected signal level in dBm, also known as the RSSI.
                            // TODO: RSSI强度用来辅助选择连接哪个GO
                            int RSSI = devicesResult.get(i).level;
                            if(devicesResult.get(i).SSID.contains(networkSSID)){
                                wifiConfiguration = wifiAdmin.CreateWifiInfo(devicesResult.get(i).SSID,credential,3);
                                networkId = wifiConfiguration.networkId;
                                wifiAdmin.addNetwork(wifiConfiguration);
                                DeviceAttributes.currentlyConnectedDevice=networkSSID; // 设置目前连接着的设备名称
                                break;
                            }
                        }
                    }
                    // 发现LEGACY 设备完成后，暂时不需要接收器工作
//                    DeviceAttributes.isConnectedToGO = true;
                    break;
                }

                case FOUND_P2P_DEVICES_DONE:{
                    handler.obtainMessage(SET_TEXTVIEW,"FOUND_P2P_DEVICES_DONE").sendToTarget();

                    new StopPeerDiscovery().start();
                    // 获取到并进行解析了 peers 设备列表
                    if(peers!=null){
                        jaccardIndexArray = InformationCollection.parseDeivceName(peers);
                        for (int i=0;i<jaccardIndexArray.size();i++){
                            handler.obtainMessage(SET_TEXTVIEW,jaccardIndexArray.get(i).getDeviceID()).sendToTarget();
                        }
                    }

                    handler.obtainMessage(SET_TEXTVIEW,"WifiManager start scan").sendToTarget();
                    // 进行 wifi legacy 扫描
                    wifiManager.startScan();
                    break;
                }

                case MESSAGE_RECEIVE:{
                    SourcePayloadAndLocation sourcePayloadAndLocation = (SourcePayloadAndLocation) msg.obj;
                    String source = sourcePayloadAndLocation.getSource();
                    String payload = sourcePayloadAndLocation.getPayload();
                    String location = sourcePayloadAndLocation.getLocation();

                    if(location!=null && !otherLocationSet.add(location)){// 若集合中已经添加过该坐标，则不再进行绘制
                        LatLng latLng = Utils.transStringToLatLng(location);
                        handler.obtainMessage(SET_OTHERS_LOCATION,new IDandLatLng(source,latLng)).sendToTarget();
                    }

                    Msg msg1 = new Msg(Utils.truncateString(payload)
                            +" from ["+ source +"]", Msg.TYPE_RECEIVED);
                    msgList.add(msg1);
                    adapter.notifyItemInserted(msgList.size()-1);
                    msgRecyclerView.scrollToPosition(msgList.size()-1);
                    break;
                }

                case MESSAGE_SENT:{
                    String str = (String) msg.obj;
                    Msg msg1 = new Msg(str, Msg.TYPE_SENT);
                    msgList.add(msg1);
                    adapter.notifyItemInserted(msgList.size()-1);
                    msgRecyclerView.scrollToPosition(msgList.size()-1);
                    break;
                }

                default:
                    break;
            }
        }
    };
}