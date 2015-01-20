package com.abbiya.broadr.events;


/**
 * Created by seshachalam on 30/8/14.
 */
public class RegisteringGCMEvent {
    String message;

    public RegisteringGCMEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

