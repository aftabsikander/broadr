package com.abbiya.broadr.jobs;

import android.os.Bundle;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.dao.Comment;
import com.abbiya.broadr.dao.Message;
import com.abbiya.broadr.events.CommentSendingEvent;
import com.abbiya.broadr.events.CommentSentEvent;
import com.abbiya.broadr.repositories.CommentRepo;
import com.abbiya.broadr.repositories.MessageRepo;
import com.abbiya.broadr.utility.Constants;
import com.abbiya.broadr.utility.LocationUtils;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.io.IOException;
import java.util.Date;

import de.greenrobot.event.EventBus;

/**
 * Created by seshachalam on 29/9/14.
 */
public class SendCommentJob extends Job {

    private Long id;
    private String content;
    private String messageId;
    private String uuid;

    public SendCommentJob(String content, String parentId, String uuid) {
        super(new Params(Priority.MID).requireNetwork().persist().groupBy(Constants.SEND_COMMENTS));
        this.content = content;
        this.messageId = parentId;
        this.uuid = uuid;
    }

    public SendCommentJob(Long id, String content, String uuid) {
        super(new Params(Priority.MID).requireNetwork().delayInMs(3000).persist().groupBy(Constants.SEND_COMMENTS));

        this.id = id;
        this.content = content;
        this.uuid = uuid;
    }

    @Override
    public void onRun() throws Throwable {
        Bundle data = new Bundle();
        data.putString("t", "c");
        data.putString("c", content);
        data.putString("mid", messageId);
        data.putString("uuid", uuid);

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(BroadrApp.getInstance().getApplicationContext());
        gcm.send(Constants.GCM_PROJECT_ID + "@gcm.googleapis.com", Constants.COMMENT_PREFIX + uuid, 0, data);
        gcm.close();

        CommentRepo commentRepo = BroadrApp.getCommentRepo();
        Comment comment = commentRepo.getComment(uuid);
        comment.setUpdatedAt(new Date());
        comment.setStatus(Constants.SENT);
        commentRepo.updateComment(comment);

        EventBus.getDefault().post(new CommentSentEvent(comment));
    }

    @Override
    public void onAdded() {
        if (id == null) {
            MessageRepo messageRepo = BroadrApp.getMessageRepo();
            Message message = messageRepo.getMessage(messageId);

            Comment comment = new Comment(content, message.getId(), uuid);
            comment.setHappenedAt(new Date());
            String address = BroadrApp.getSharedPreferences().getString(Constants.LAST_KNOWN_ADDRESS, null);
            comment.setAddress(address);
            comment.setGeoHash(LocationUtils.getGeoHash());
            CommentRepo commentRepo = BroadrApp.getCommentRepo();
            commentRepo.insertOrReplace(comment);
            EventBus.getDefault().post(new CommentSendingEvent(comment));
        } else {
            Comment comment;
            CommentRepo commentRepo = BroadrApp.getCommentRepo();
            comment = commentRepo.getComment(id);
            if (comment != null) {
                comment.setUpdatedAt(new Date());
                commentRepo.insertOrReplace(comment);
                EventBus.getDefault().post(new CommentSendingEvent(comment));
            }
        }

    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {

        return throwable instanceof IOException;
    }
}
