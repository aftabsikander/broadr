package com.abbiya.broadr.activities;

import android.os.Bundle;

import com.abbiya.broadr.R;
import com.abbiya.broadr.activities.fragments.SettingsFragment;

import roboguice.inject.ContentView;

@ContentView(R.layout.activity_settings)
public class SettingsActivity extends BaseActivity {

    public static final String USE_FLICKR_IMAGES = "use_flickr_images";
    public static final String DISTANCE_TYPE = "distance_type";
    public static final String NOTIF_PERIOD = "notif_period";
    public static final String LOCATION_NOTIF_PERIOD = "loc_notif_period";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction().replace(R.id.content_frame, new SettingsFragment()).commit();
    }
}
