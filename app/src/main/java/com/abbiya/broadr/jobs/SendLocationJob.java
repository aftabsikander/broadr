package com.abbiya.broadr.jobs;

import android.os.Bundle;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.utility.Constants;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by seshachalam on 29/9/14.
 */
public class SendLocationJob extends Job {

    private static final AtomicInteger jobCounter = new AtomicInteger(0);
    private final int id;
    private String latitude;
    private String longitude;
    private String address;
    private String uuid;

    public SendLocationJob(String latitude, String longitude, String address, String uuid) {
        super(new Params(Priority.MID).requireNetwork().persist().groupBy(Constants.SEND_LOCATION));
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.uuid = uuid;

        id = jobCounter.incrementAndGet();
    }

    @Override
    public void onRun() throws Throwable {

        if (id != jobCounter.get()) {
            return;
        }

        Bundle data = new Bundle();
        data.putString("t", "l");
        data.putString("lt", latitude);
        data.putString("ln", longitude);
        data.putString("a", address);
        data.putString("uuid", uuid);

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(BroadrApp.getInstance().getApplicationContext());
        gcm.send(Constants.GCM_PROJECT_ID + "@gcm.googleapis.com", Constants.LOCATION_PREFIX + uuid, 0, data);
        gcm.close();
    }

    @Override
    protected void onCancel() {

    }

    @Override
    public void onAdded() {
        //store it in db
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return throwable instanceof IOException;
    }
}
