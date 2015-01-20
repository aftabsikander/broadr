package com.abbiya.broadr.jobs;

import android.app.NotificationManager;
import android.content.Context;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.dao.Message;
import com.abbiya.broadr.repositories.MessageRepo;
import com.abbiya.broadr.utility.Constants;
import com.abbiya.broadr.utility.StringUtilities;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.Params;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by seshachalam on 22/11/14.
 */
public class RetrySendingMessagesJob extends Job {

    private static final AtomicInteger jobCounter = new AtomicInteger(0);
    private final int id;

    public RetrySendingMessagesJob() {
        super(new Params(Priority.MID).requireNetwork().persist().groupBy(Constants.SEND_MESSAGES));

        id = jobCounter.incrementAndGet();
    }

    @Override
    protected void onCancel() {

    }

    @Override
    public void onAdded() {
        if (id != jobCounter.get()) {
            return;
        }
    }

    @Override
    public void onRun() throws Throwable {
        Integer[] statuses = {Constants.SENT};
        MessageRepo messageRepo = BroadrApp.getMessageRepo();
        List<Message> toBeSent = messageRepo.getMessagesOfStatus(statuses);
        JobManager jobManager = BroadrApp.getInstance().getJobManager();
        if (toBeSent != null && toBeSent.size() > 0) {
            NotificationManager mNotificationManager = (NotificationManager)
                    BroadrApp.getInstance().getSystemService(Context.NOTIFICATION_SERVICE);
            for (Message message : toBeSent) {
                mNotificationManager.cancel(StringUtilities.safeLongToInt(message.getId()));
                jobManager.addJobInBackground(new SendMessageJob(message.getContent(), message.getUuid()));
            }
        }
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return throwable instanceof IOException;
    }

}
