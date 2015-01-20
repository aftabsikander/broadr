package com.abbiya.broadr.events;

import com.abbiya.broadr.dao.Message;

/**
 * Created by seshachalam on 2/9/14.
 */
public class ReceivedGCMMessageEvent {
    private Message message;

    public ReceivedGCMMessageEvent() {

    }

    public ReceivedGCMMessageEvent(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
