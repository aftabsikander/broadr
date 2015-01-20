package com.abbiya.broadr.api;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by seshachalam on 17/11/14.
 */
public class SaveMessagesIfNotExists {

    @SerializedName("messages")
    @Expose
    private List<MessageContentToBeSent> messages;

    public List<MessageContentToBeSent> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageContentToBeSent> messages) {
        this.messages = messages;
    }
}
