package com.panghui.servicediscovery.routing;

public class SourcePayloadAndLocation {
    private String source;
    private String payload;
    private String location;

    public SourcePayloadAndLocation(String source, String payload, String location) {
        this.source = source;
        this.payload = payload;
        this.location = location;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
