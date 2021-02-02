package com.panghui.servicediscovery;

public class AlertMessage {
    String lastHopAddress;
    Integer seqNo;
    String data;
    Boolean isValid;

    public AlertMessage(String lastHopAddress, Integer seqNo, String data, Boolean isValid) {
        this.lastHopAddress = lastHopAddress;
        this.seqNo = seqNo;
        this.data = data;
        this.isValid = isValid;
    }

    public String getLastHopAddress() {
        return lastHopAddress;
    }

    public void setLastHopAddress(String lastHopAddress) {
        this.lastHopAddress = lastHopAddress;
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
                "SourceAdddress-'" + lastHopAddress + '\'' +
                ", seqNo-" + seqNo +
                ", data-'" + data + '\'' +
                ", isValid-" + isValid +
                '}';
    }
}
