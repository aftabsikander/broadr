package com.abbiya.broadr.api;

import com.abbiya.broadr.dao.Comment;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by seshachalam on 10/9/14.
 */
public class CommentResponseMap {
    @SerializedName("error")
    @Expose
    private Boolean error;

    @SerializedName("message")
    @Expose
    private String message;

    @SerializedName("address")
    @Expose
    private String address;

    @SerializedName("link")
    @Expose
    private String link;

    @SerializedName("comments")
    @Expose
    private List<Comment> comments;

    public Boolean getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getAddress() {
        return address;
    }

    public String getLink() {
        return link;
    }

    public List<Comment> getComments() {
        return comments;
    }
}
