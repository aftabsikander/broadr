package com.abbiya.broadr.jobs;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.events.RegisteredGCMEvent;
import com.abbiya.broadr.events.RegisteringGCMEvent;
import com.abbiya.broadr.events.RegisteringGCMFailedEvent;
import com.abbiya.broadr.utility.Constants;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import de.greenrobot.event.EventBus;

/**
 * Created by seshachalam on 30/8/14.
 */
public class RegisterGCMJob extends Job {

    private static final AtomicInteger jobCounter = new AtomicInteger(0);
    private static long GCM_RETRY_LIMIT = 0;
    private final int id;
    String regId = null;
    String msg = null;

    public RegisterGCMJob() {
        super(new Params(Priority.HIGH).delayInMs(GCM_RETRY_LIMIT).requireNetwork().persist().groupBy(Constants.REGISTER_WITH_GCM));

        id = jobCounter.incrementAndGet();
    }

    @Override
    public void onAdded() {
        if (GCM_RETRY_LIMIT == 0) {
            GCM_RETRY_LIMIT = 60000;
        }

        EventBus.getDefault().post(new RegisteringGCMEvent("Trying to register with GCM"));

    }

    @Override
    public void onRun() throws Throwable {

        if (id != jobCounter.get()) {
            return;
        }

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(BroadrApp.getInstance().getApplicationContext());
        regId = gcm.register(Constants.GCM_PROJECT_ID);
        EventBus.getDefault().post(new RegisteredGCMEvent(regId));
        gcm.close();
    }

    @Override
    protected void onCancel() {
        //reset retry limit
        GCM_RETRY_LIMIT = 60000;
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        GCM_RETRY_LIMIT *= 2;
        EventBus.getDefault().post(new RegisteringGCMFailedEvent());
        return throwable instanceof IOException;
    }
}
