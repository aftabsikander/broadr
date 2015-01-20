package com.abbiya.broadr.events;

import com.abbiya.broadr.dao.Comment;

/**
 * Created by seshachalam on 4/9/14.
 */
public class CommentSendingEvent {
    private Comment comment;

    public CommentSendingEvent(Comment comment) {
        this.comment = comment;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }
}
