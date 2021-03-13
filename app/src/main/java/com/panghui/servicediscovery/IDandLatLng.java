package com.panghui.servicediscovery;

import com.baidu.mapapi.model.LatLng;

public class IDandLatLng {
    private String id;
    private LatLng latLng;

    public IDandLatLng(String id, LatLng latLng) {
        this.id = id;
        this.latLng = latLng;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }
}
