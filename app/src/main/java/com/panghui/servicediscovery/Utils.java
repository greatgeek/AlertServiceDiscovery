package com.panghui.servicediscovery;

import android.util.Log;

import com.baidu.mapapi.model.LatLng;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    /**
     * Turn AlerMessaage instance to a string.
     * @param message
     * @return
     */
    public static String AlerMessageToString(AlertMessage message){
            return "lastHopAddress-" + message.deviceID +
                    ",seqNo-" + message.seqNo +
                    ",data-" + message.data  +
                    ",isValid-" + message.isValid;
    }

    /**
     * Turn a string to AlerMessage instance
     * @param str
     * @return
     */
    public static AlertMessage StringToAlerMessage(String str){
        Log.d("MyString",str);
        String[] strArr = str.split(",");
        String[] items = new String[strArr.length];
        for(int i=0;i<strArr.length;i++){
            String[] split = strArr[i].split("-");
            if(split.length>=2){
                items[i]=split[1];
            }
        }

        if(items.length!=4) return null;
        AlertMessage item = new AlertMessage(items[0],Integer.parseInt(items[1]),items[2],
                Boolean.parseBoolean(items[3]));
        return item;
    }

    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4   true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }

    // 从字符串中提取ID
    public static String extractIDFromMessage(String msg){
        Pattern p = Pattern.compile("\\[(.*?)]");
        Matcher m = p.matcher(msg);
        String res=null;
        while(m.find()){
            res=m.group(1);
            break;
        }
        return res;
    }

    // 截断字符串
    public static String truncateString(String str){
        if(str==null) return null;
        char[] strArr = str.toCharArray();
        for(int i=0;i<strArr.length;i++){
            if(strArr[i]==':'){
                return str.substring(i+1);
            }
        }
        return str;
    }

    // 从字符串中返回坐标
    public static LatLng transStringToLatLng(String str){
        if(str==null) return null;
        String[] strArr = str.split("_");
        if(strArr.length<2) return null;

        return new LatLng(Double.parseDouble(strArr[0]),Double.parseDouble(strArr[1]));
    }
}
