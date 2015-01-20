package com.abbiya.broadr.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.activities.SettingsActivity;
import com.abbiya.broadr.jobs.NotificationJob;
import com.path.android.jobqueue.JobManager;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by seshachalam on 11/12/14.
 */
public class NotificationService extends Service {

    public static int RUNNING = 0;
    private SharedPreferences sPrefs;
    private JobManager jobManager;

    private Timer mTimer = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        RUNNING = 1;

        jobManager = BroadrApp.getInstance().getJobManager();
        sPrefs = BroadrApp.getSettingsPreferences();
        if (mTimer != null) {
            mTimer.cancel();
        } else {
            mTimer = new Timer();
        }

        long when = Long.valueOf(sPrefs.getString(SettingsActivity.NOTIF_PERIOD, "1")).longValue();
        if (when > 1) {
            mTimer.scheduleAtFixedRate(new NotificationTask(), 0, when);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RUNNING = 0;

    }

    private class NotificationTask extends TimerTask {
        @Override
        public void run() {
            jobManager.addJobInBackground(new NotificationJob());
        }
    }
}
