package com.abbiya.broadr.api;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Created by seshachalam on 17/11/14.
 */
public class MessageContentToBeSent {

    @SerializedName("content")
    @Expose
    private String content;
    @SerializedName("uuid")
    @Expose
    private String uuid;
    @SerializedName("createdAt")
    @Expose
    private Date createdAt;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

}
