package com.abbiya.broadr.events;

import com.abbiya.broadr.api.MessageResponseMap;

/**
 * Created by seshachalam on 2/10/14.
 */
public class GotMessagesEvent {

    private MessageResponseMap messageResponseMap;

    public GotMessagesEvent(MessageResponseMap messageResponseMap) {
        this.messageResponseMap = messageResponseMap;
    }

    public MessageResponseMap getMessageResponseMap() {
        return messageResponseMap;
    }
}
