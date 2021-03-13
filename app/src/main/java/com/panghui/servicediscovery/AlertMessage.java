package com.panghui.servicediscovery;

public class AlertMessage {
    String deviceID;
    Integer seqNo;
    String data;
    Boolean isValid;

    public AlertMessage(String deviceID, Integer seqNo, String data, Boolean isValid) {
        this.deviceID = deviceID;
        this.seqNo = seqNo;
        this.data = data;
        this.isValid = isValid;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public Integer getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(Integer seqNo) {
        this.seqNo = seqNo;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Boolean getValid() {
        return isValid;
    }

    public void setValid(Boolean valid) {
        isValid = valid;
    }


    @Override
    public String toString() {
        return "AlerMessage{" +
                "SourceAdddress-'" + deviceID + '\'' +
                ", seqNo-" + seqNo +
                ", data-'" + data + '\'' +
                ", isValid-" + isValid +
                '}';
    }
}
