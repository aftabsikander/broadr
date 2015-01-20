package com.abbiya.broadr.jobs;

import android.os.Bundle;
import android.util.Log;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.dao.Board;
import com.abbiya.broadr.dao.Message;
import com.abbiya.broadr.events.SendingRavenEvent;
import com.abbiya.broadr.events.SentRavenEvent;
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
public class SendMessageJob extends Job {
    private Long id;
    private String message;
    private String uuid;

    public SendMessageJob(String message, String uuid) {
        super(new Params(Priority.MID).requireNetwork().persist().groupBy(Constants.SEND_MESSAGES));

        this.message = message;
        this.uuid = uuid;
    }

    public SendMessageJob(Long id, String message, String uuid) {
        super(new Params(Priority.MID).requireNetwork().delayInMs(3000).persist().groupBy(Constants.SEND_MESSAGES));

        this.id = id;
        this.message = message;
        this.uuid = uuid;
    }

    @Override
    public void onAdded() {
        //job has been secured to disk, add item to database
        //create a new message and save it to db
        MessageRepo messageRepo = BroadrApp.getMessageRepo();
        Message newMessage = null;
        if (id != null) {
            newMessage = messageRepo.getMessage(id);
        }

        if (newMessage == null) {
            newMessage = new Message(message, Constants.MESSAGE, Constants.SENDING);
            newMessage.setUuid(uuid);
            newMessage.setGeoHash(LocationUtils.getGeoHash());
            String address = BroadrApp.getSharedPreferences().getString(Constants.LAST_KNOWN_ADDRESS, null);
            newMessage.setAddress(address);

            newMessage.setHappenedAt(new Date());
        } else {
            newMessage.setStatus(Constants.SENDING);
            newMessage.setUpdatedAt(new Date());
        }
        Board currentBoard = BroadrApp.getBoardRepo().getBoard(LocationUtils.getGeoHash().substring(0, 4));
        newMessage.setBoard(currentBoard);

        messageRepo.insertOrReplace(newMessage);

        EventBus.getDefault().post(new SendingRavenEvent(newMessage));
    }

    @Override
    public void onRun() throws Throwable {
        //send the message via gcm
        Bundle data = new Bundle();
        MessageRepo messageRepo = BroadrApp.getMessageRepo();
        Message storedMessage = messageRepo.getMessage(uuid);
        data.putString("t", "m");
        data.putString("c", storedMessage.getContent());
        data.putString("uuid", uuid);

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(BroadrApp.getInstance().getApplicationContext());
        gcm.send(Constants.GCM_PROJECT_ID + "@gcm.googleapis.com", Constants.MESSAGE_PREFIX + uuid, 0, data);
        gcm.close();

        storedMessage.setStatus(Constants.SENT);
        storedMessage.setUpdatedAt(new Date());
        messageRepo.updateMessage(storedMessage);

        Log.d(Constants.APPTAG, "sending a message with id " + storedMessage.getUuid() + " " + storedMessage.getId());

        EventBus.getDefault().post(new SentRavenEvent(storedMessage));
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {

        return throwable instanceof IOException;
    }
}
