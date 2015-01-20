package com.abbiya.broadr.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.activities.SettingsActivity;
import com.abbiya.broadr.service.NotificationService;
import com.abbiya.broadr.utility.Constants;

/**
 * Created by seshachalam on 11/12/14.
 */
public class NotificationServiceStarter extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences mPrefs = BroadrApp.getSettingsPreferences();
        if (mPrefs.getBoolean(Constants.IS_REGISTRATION_MESSAGE_SENT, false)) {

            long notifInterval = Long.valueOf(BroadrApp.getSettingsPreferences().getString(SettingsActivity.NOTIF_PERIOD, "1")).longValue();

            if (notifInterval > 1) {
                Intent i = new Intent(NotificationService.class.getCanonicalName());
                i.setClass(context, NotificationService.class);
                context.startService(i);
            }
        }
    }
}
