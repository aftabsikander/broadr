package com.abbiya.broadr.events;

import com.abbiya.broadr.dao.Message;

/**
 * Created by seshachalam on 30/8/14.
 */
public class SentRavenEvent {

    private Message message;

    public SentRavenEvent(Message message) {
        this.message = message;

    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

}
