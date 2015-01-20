package com.abbiya.broadr.activities.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.R;
import com.abbiya.broadr.activities.SettingsActivity;
import com.abbiya.broadr.service.NotificationService;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_general);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SettingsActivity.NOTIF_PERIOD)) {

            Context context = BroadrApp.getInstance();
            long notifInterval = Long.valueOf(BroadrApp.getSettingsPreferences().getString(SettingsActivity.NOTIF_PERIOD, "1")).longValue();

            Intent notificationService = new Intent(context, NotificationService.class);
            if (notifInterval <= 1) {
                context.stopService(notificationService);
            } else {
                context.stopService(notificationService);
                context.startService(notificationService);
            }
        }
    }
}
