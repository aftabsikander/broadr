package com.abbiya.broadr.events;

import com.abbiya.broadr.dao.Message;

/**
 * Created by seshachalam on 4/9/14.
 */
public class DeliveredRavenEvent {
    private Message message;

    public DeliveredRavenEvent(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
