package com.abbiya.broadr.jobs;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.dao.Message;
import com.abbiya.broadr.events.UpdateMessageStatusEvent;
import com.abbiya.broadr.events.UpdatedMessageStatusEvent;
import com.abbiya.broadr.repositories.MessageRepo;
import com.abbiya.broadr.utility.Constants;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.greenrobot.event.EventBus;

/**
 * Created by seshachalam on 11/12/14.
 */
public class UpdateMessageStatusJob extends Job {

    private static final AtomicInteger jobCounter = new AtomicInteger(0);
    private final int id;

    private int fromStatus;
    private int toStatus;

    public UpdateMessageStatusJob(int fromStatus, int toStatus) {
        super(new Params(Priority.MID).requireNetwork().persist().groupBy(Constants.UPDATE_MESSAGES));

        id = jobCounter.incrementAndGet();
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    @Override
    protected void onCancel() {

    }

    @Override
    public void onAdded() {
        EventBus.getDefault().post(new UpdateMessageStatusEvent());
    }

    @Override
    public void onRun() throws Throwable {
        if (id != jobCounter.get()) {
            return;
        }
        MessageRepo messageRepo = BroadrApp.getMessageRepo();
        Integer[] statuses = {fromStatus};
        List<Message> toUpdate = messageRepo.getMessagesOfStatus(statuses);
        for (Message message : toUpdate) {
            message.setStatus(toStatus);
        }
        messageRepo.updateMessages(toUpdate);

        EventBus.getDefault().post(new UpdatedMessageStatusEvent());
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return throwable instanceof Exception;
    }
}
