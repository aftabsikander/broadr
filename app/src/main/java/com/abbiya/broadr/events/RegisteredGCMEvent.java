package com.abbiya.broadr.events;

/**
 * Created by seshachalam on 30/8/14.
 */
public class RegisteredGCMEvent {
    String regId;

    public RegisteredGCMEvent(String regId) {
        this.regId = regId;
    }

    public String getRegId() {
        return regId;
    }
}
