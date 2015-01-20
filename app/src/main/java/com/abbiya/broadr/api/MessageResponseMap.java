package com.abbiya.broadr.api;

/**
 * Created by seshachalam on 1/10/14.
 */

import com.abbiya.broadr.dao.Message;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MessageResponseMap {

    @SerializedName("error")
    @Expose
    private Boolean error;

    @SerializedName("message")
    @Expose
    private String message;

    @SerializedName("messages")
    @Expose
    private List<Message> messages;

    public Boolean getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public List<Message> getMessages() {
        return messages;
    }
}
