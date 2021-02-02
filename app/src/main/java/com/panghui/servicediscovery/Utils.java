package com.panghui.servicediscovery;

import android.util.Log;

public class Utils {
    /**
     * Turn AlerMessaage instance to a string.
     * @param message
     * @return
     */
    public static String AlerMessageToString(AlertMessage message){
            return "lastHopAddress-" + message.lastHopAddress +
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
}
