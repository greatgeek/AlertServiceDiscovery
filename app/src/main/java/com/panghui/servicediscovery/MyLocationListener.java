package com.panghui.servicediscovery;

import android.os.Handler;
import android.util.Log;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;

public class MyLocationListener extends BDAbstractLocationListener {

    public BaiduMap mBaiduMap;
    public Handler handler;
    public MyLocationListener(BaiduMap mBaiduMap, Handler handler){
        this.mBaiduMap=mBaiduMap;
        this.handler=handler;
    }

    @Override
    public void onReceiveLocation(BDLocation location) {
        if(location.getLocType() == BDLocation.TypeOffLineLocation){
            MyLocationData locData = new MyLocationData.Builder().accuracy(location.getRadius())
                    .latitude(location.getLatitude()).longitude(location.getLongitude())
                    .direction(0).accuracy(0).build();
//            mBaiduMap.setMyLocationData(locData);
//            BitmapDescriptor bitmap_self = BitmapDescriptorFactory.fromResource(R.drawable.location_image_self);
//            BitmapDescriptor bitmap_others = BitmapDescriptorFactory.fromResource(R.drawable.location_image_others);
//
            LatLng own_position = new LatLng(location.getLatitude(),location.getLongitude());
//            //设置缩放中心点；缩放比例；
//            MapStatus.Builder builder = new MapStatus.Builder();
//            builder.target(own_position).zoom(18.0f);
//
//            //给地图设置状态
//            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
//
//            LatLng others_position = new LatLng(location.getLatitude()+0.0005,location.getLongitude()+0.0005);
//
//            OverlayOptions option = new MarkerOptions().position(others_position).icon(bitmap_others);
//            mBaiduMap.addOverlay(option);// 在地图上增加图层
//
//
//            OverlayOptions option2 = new MarkerOptions().position(own_position).icon(bitmap_self);
//            mBaiduMap.addOverlay(option2);// 在地图上增加图层

            Log.d(MainActivity.TAG,"离线定位成功");
            double lat = location.getLatitude();
            double lon = location.getLongitude();
            Log.d(MainActivity.TAG,"lat = "+lat+";"+"lon = "+lon);

//            handler.obtainMessage(MainActivity.SET_OWN_LOCATION,own_position).sendToTarget();
        }else if(location.getLocType() == BDLocation.TypeOffLineLocationFail){
            Log.d(MainActivity.TAG,"离线定位失败");
        }else {
            LatLng own_position = new LatLng(location.getLatitude(),location.getLongitude());
//            handler.obtainMessage(MainActivity.SET_OWN_LOCATION,own_position).sendToTarget();
            Log.d(MainActivity.TAG,"location type = "+location.getLocType());
        }
    }
}
