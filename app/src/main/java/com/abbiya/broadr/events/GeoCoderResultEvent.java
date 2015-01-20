package com.abbiya.broadr.events;

/**
 * Created by seshachalam on 30/9/14.
 */
public class GeoCoderResultEvent {
    private String address;

    public GeoCoderResultEvent(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }
}
