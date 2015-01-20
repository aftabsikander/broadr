package com.abbiya.broadr.retrofit;

/**
 * Created by seshachalam on 10/9/14.
 */
public class CommentData {
    String uuid;
    String c;
    Integer w;

    public CommentData(String uuid, String c, Integer w) {
        this.uuid = uuid;
        this.c = c;
        this.w = w;
    }

    @Override
    public String toString() {
        return c;
    }
}
