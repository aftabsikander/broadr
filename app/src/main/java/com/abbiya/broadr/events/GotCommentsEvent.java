package com.abbiya.broadr.events;

import com.abbiya.broadr.api.CommentResponseMap;

/**
 * Created by seshachalam on 2/10/14.
 */
public class GotCommentsEvent {

    private CommentResponseMap commentResponseMap;

    public GotCommentsEvent(CommentResponseMap commentResponseMap) {
        this.commentResponseMap = commentResponseMap;
    }

    public CommentResponseMap getCommentResponseMap() {
        return commentResponseMap;
    }
}
