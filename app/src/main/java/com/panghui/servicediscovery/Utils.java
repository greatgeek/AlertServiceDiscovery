package com.panghui.servicediscovery;

public class Utils {
    /**
     * Turn AlerMessaage instance to a string.
     * @param message
     * @return
     */
    public static String AlerMessageToString(AlerMessage message){
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
    public static AlerMessage StringToAlerMessage(String str){
        String[] strArr = str.split(",");
        String[] items = new String[strArr.length];
        for(int i=0;i<strArr.length;i++){
            items[i]=strArr[i].split("-")[1];
        }

        AlerMessage item = new AlerMessage(items[0],Integer.parseInt(items[1]),items[2],
                Boolean.parseBoolean(items[3]));
        return item;
    }
}
