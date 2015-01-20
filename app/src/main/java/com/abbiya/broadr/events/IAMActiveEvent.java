package com.abbiya.broadr.events;

/**
 * Created by seshachalam on 28/11/14.
 */
public class IAMActiveEvent {
    private String activity;

    public IAMActiveEvent(String activity) {
        this.activity = activity;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }
}
