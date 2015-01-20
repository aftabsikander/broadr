package com.abbiya.broadr.events;

import com.abbiya.broadr.dao.Message;

/**
 * Created by seshachalam on 30/8/14.
 */
public class SendingRavenEvent {
    private Message message;

    public SendingRavenEvent(Message message) {
        this.message = message;
    }

    ;

    public Message getMessage() {
        return message;
    }
}
