package com.abbiya.broadr.jobs;

import android.os.Bundle;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.events.RegisteringGCMEvent;
import com.abbiya.broadr.utility.Constants;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import de.greenrobot.event.EventBus;

/**
 * Created by seshachalam on 29/9/14.
 */
public class SendRegistrationMessageJob extends Job {

    private static final AtomicInteger jobCounter = new AtomicInteger(0);
    private final int id;
    private String email;
    private String uuid;

    public SendRegistrationMessageJob(String email, String uuid) {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.SEND_REGISTRATION_MESSAGES));
        this.email = email;
        if (uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        } else {
            this.uuid = uuid;
        }

        id = jobCounter.incrementAndGet();
    }

    @Override
    public void onRun() throws Throwable {

        if (id != jobCounter.get()) {
            return;
        }

        Bundle data = new Bundle();
        data.putString("t", "r");
        data.putString("fn", "");
        data.putString("ln", "");
        data.putString("em", email);
        data.putString("uuid", uuid);

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(BroadrApp.getInstance().getApplicationContext());
        gcm.send(Constants.GCM_PROJECT_ID + "@gcm.googleapis.com", Constants.GCM_REGISTRATION + uuid, 0, data);
        gcm.close();
    }

    @Override
    public void onAdded() {
        //store it in db
        EventBus.getDefault().post(new RegisteringGCMEvent("Registering with Google"));
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {

        return throwable instanceof IOException;
    }

    @Override
    protected void onCancel() {

    }
}
