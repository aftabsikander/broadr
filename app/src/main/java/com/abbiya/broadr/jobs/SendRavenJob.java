package com.abbiya.broadr.jobs;

import android.os.Bundle;
import android.util.Log;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.dao.Comment;
import com.abbiya.broadr.dao.Message;
import com.abbiya.broadr.events.CommentSendingEvent;
import com.abbiya.broadr.events.CommentSentEvent;
import com.abbiya.broadr.events.SendingRavenEvent;
import com.abbiya.broadr.repositories.CommentRepo;
import com.abbiya.broadr.repositories.MessageRepo;
import com.abbiya.broadr.utility.Constants;
import com.abbiya.broadr.utility.LocationUtils;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import de.greenrobot.event.EventBus;

/**
 * Created by seshachalam on 30/8/14.
 */
public class SendRavenJob extends Job {
    private String text;
    private int type = 0;
    private String mid = null;

    public SendRavenJob(String text) {
        super(new Params(Priority.MID).requireNetwork().persist().groupBy(Constants.SEND_RAVENS));

        this.text = text;
    }

    public SendRavenJob(String text, int priority) {
        super(new Params(priority).requireNetwork().persist().groupBy(Constants.SEND_RAVENS));

        this.text = text;
    }

    public SendRavenJob(String text, int priority, int type) {
        super(new Params(priority).requireNetwork().persist().groupBy(Constants.SEND_RAVENS));

        this.text = text;
        this.type = type;
    }

    public SendRavenJob(String text, String mid, int priority, int type) {
        super(new Params(priority).requireNetwork().persist().groupBy(Constants.SEND_RAVENS));

        this.text = text;
        this.type = type;
        this.mid = mid;
    }

    @Override
    public void onAdded() {
        //job has been secured to disk, add item to database
        //create a new message and save it to db
        Date happenedAt = new Date();
        String geoHash = LocationUtils.getGeoHash();
        if (type != Constants.COMMENT) {
            Message message;
            message = new Message(text, happenedAt, geoHash);
            message.setType(type);
            message.setStatus(Constants.SENDING);
            message.setUuid(UUID.randomUUID().toString());
            BroadrApp.getMessageRepo().insertOrReplace(message);
            EventBus.getDefault().post(new SendingRavenEvent(message));
        } else {
            Comment comment = new Comment(null, text, Long.valueOf(mid));
            comment.setUuid(UUID.randomUUID().toString());
            CommentRepo commentRepo = BroadrApp.getCommentRepo();
            commentRepo.insert(comment);
            Log.d(Constants.APPTAG, String.valueOf(comment.getId()));

            EventBus.getDefault().post(new CommentSendingEvent(comment));
        }
    }

    @Override
    public void onRun() throws Throwable {
        //send the message via gcm
        Bundle data = new Bundle();
        switch (type) {
            case 0://r
                data.putString("t", "r");
                data.putString("fn", "");
                data.putString("ln", "");
                data.putString("em", text);
                break;
            case 1://l -> lt, ln
                //get current location
                String geoHash = LocationUtils.getGeoHash();
                GeoHash from = GeoHash.fromGeohashString(geoHash);
                WGS84Point point = from.getPoint();
                Double startLatitude = point.getLatitude();
                Double startLongitude = point.getLongitude();
                data.putString("t", "l");
                data.putString("lt", String.valueOf(startLatitude));
                data.putString("ln", String.valueOf(startLongitude));
                break;
            case 2://m
                data.putString("t", "m");
                data.putString("c", text);
                break;
            case 5://a
                data.putString("t", "a");
                break;
            case 4://lk
                data.putString("t", "lk");
                data.putString("mid", text);
                break;
            case 3:
                data.putString("t", "c");
                data.putString("c", text);
                data.putString("mid", mid);
            default:
                break;
        }

        if (mid != null) {
            //save comment
            //get the last comment of the message and set id
            MessageRepo messageRepo = BroadrApp.getMessageRepo();
            Message currentMessage = messageRepo.getMessage(Long.valueOf(mid));
            CommentRepo commentRepo = BroadrApp.getCommentRepo();
            Comment lastComment = commentRepo.getLastComment(currentMessage);
            data.putString("uuid", lastComment.getUuid());
            data.putString("sid", currentMessage.getUuid());
            Log.d(Constants.APPTAG, "Banana " + currentMessage.getUuid());
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(BroadrApp.getInstance().getApplicationContext());
            gcm.send(Constants.GCM_PROJECT_ID + "@gcm.googleapis.com", String.valueOf(lastComment.getId()), 0, data);
            gcm.close();
            lastComment.setStatus(Constants.SENT);
            commentRepo.updateComment(lastComment);
            EventBus.getDefault().post(new CommentSentEvent(lastComment));
        } else {
//            MessageRepo messageRepo = BroadrApp.getMessageRepo();
//            Message lastMessage = messageRepo.getLastMessage();
//            data.putString("uuid", lastMessage.getUuid());
//            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(BroadrApp.getInstance().getApplicationContext());
//            gcm.send(Constants.GCM_PROJECT_ID + "@gcm.googleapis.com", String.valueOf(lastMessage.getId()), 0, data);
//            gcm.close();
//
//            messageRepo.updateAllMessages(lastMessage.getId(), Constants.SENT);
//
//            EventBus.getDefault().post(new SentRavenEvent(lastMessage));
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
