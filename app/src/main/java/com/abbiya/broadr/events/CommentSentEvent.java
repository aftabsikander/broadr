package com.abbiya.broadr.events;

import com.abbiya.broadr.dao.Comment;

/**
 * Created by seshachalam on 4/9/14.
 */
public class CommentSentEvent {
    private Comment comment;

    public CommentSentEvent(Comment comment) {
        this.comment = comment;
    }

    private Comment getComment() {
        return comment;
    }

    private void setComment(Comment comment) {
        this.comment = comment;
    }
}
