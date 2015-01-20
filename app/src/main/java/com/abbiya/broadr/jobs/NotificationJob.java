package com.abbiya.broadr.jobs;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.R;
import com.abbiya.broadr.activities.BaseActivity;
import com.abbiya.broadr.activities.MainActivity;
import com.abbiya.broadr.dao.Message;
import com.abbiya.broadr.gcm.GcmIntentService;
import com.abbiya.broadr.repositories.MessageRepo;
import com.abbiya.broadr.utility.Constants;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by seshachalam on 11/12/14.
 */
public class NotificationJob extends Job {

    private static final AtomicInteger jobCounter = new AtomicInteger(0);
    private final int id;

    public NotificationJob() {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.NOTIFICATION));

        id = jobCounter.incrementAndGet();
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        if (id != jobCounter.get()) {
            return;
        }

        MessageRepo messageRepo = BroadrApp.getMessageRepo();
        Integer[] statuses = {Constants.RECEIVED_GCM};
        List<Message> receivedMessages = messageRepo.getMessagesOfStatus(statuses);

        if (receivedMessages != null && receivedMessages.size() > 0) {
            int lenMessages = receivedMessages.size();
            if (!MainActivity.class.getCanonicalName().equals(BaseActivity.active_activity)) {
                postNotification(BroadrApp.getInstance().getString(R.string.gcm_notif_checkout) + lenMessages + " " + BroadrApp.getInstance().getString(R.string.gcm_notif_new_messages), GcmIntentService.NOTIFICATION_ID);
            }
        }
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return false;
    }

    private void postNotification(String msg, int notificationId) {
        Context context = BroadrApp.getInstance();
        NotificationManager mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(context.getString(R.string.app_name))
                        .setTicker(context.getString(R.string.messages_new_arrival))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg)
                        .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                        .setAutoCancel(true);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(notificationId, mBuilder.build());
    }
}
