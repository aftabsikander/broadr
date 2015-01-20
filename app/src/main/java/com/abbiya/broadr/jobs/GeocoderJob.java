package com.abbiya.broadr.jobs;

import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.events.GeoCoderResultEvent;
import com.abbiya.broadr.utility.Constants;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import de.greenrobot.event.EventBus;

/**
 * Created by seshachalam on 21/9/14.
 */
public class GeocoderJob extends Job {

    private static final AtomicInteger jobCounter = new AtomicInteger(0);
    private final int id;
    private Double latitude;
    private Double longitude;

    public GeocoderJob(Double latitude, Double longitude) {
        super(new Params(Priority.LOW).requireNetwork().persist().groupBy(Constants.GEO_CODER));
        this.latitude = latitude;
        this.longitude = longitude;

        id = jobCounter.incrementAndGet();
    }

    public GeocoderJob(String latitude, String longitude) {
        super(new Params(Priority.LOW).requireNetwork().persist().groupBy(Constants.GEO_CODER));
        this.latitude = Double.valueOf(latitude);
        this.longitude = Double.valueOf(longitude);

        id = jobCounter.incrementAndGet();
    }

    @Override
    public void onRun() throws Throwable {

        if (id != jobCounter.get()) {
            return;
        }

        if (latitude != null || longitude != null) {

            Geocoder geocoder =
                    new Geocoder(BroadrApp.getInstance().getApplicationContext(), Locale.getDefault());

            List<Address> addresses = geocoder.getFromLocation(latitude,
                    longitude, 1);

            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                String addressText = String.format(
                        "%s, %s, %s",
                        address.getMaxAddressLineIndex() > 0 ?
                                address.getAddressLine(0) : "",
                        address.getLocality(),
                        address.getCountryName());
                //store it the sharedprefs as last known location address
                if (addressText.trim().length() > 0) {
                    SharedPreferences sharedPreferences = BroadrApp.getSharedPreferences();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(Constants.LAST_KNOWN_ADDRESS, addressText);
                    editor.putString(Constants.LAST_KNOWN_ADDRESS_FETCH_TIME, new Date().toString());
                    editor.commit();

                    EventBus.getDefault().post(new GeoCoderResultEvent(addressText));
                }
            }
        }

    }

    @Override
    public void onAdded() {
        if (latitude != null || longitude != null) {
            SharedPreferences sharedPreferences = BroadrApp.getSharedPreferences();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(Constants.LATEST_LATITUDE, String.valueOf(latitude));
            editor.putString(Constants.LATEST_LONGITUDE, String.valueOf(longitude));
            editor.putString(Constants.LAST_KNOWN_ADDRESS_REQUESTED_TIME, new Date().toString());
            editor.commit();
        }
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return throwable instanceof IOException || throwable instanceof IllegalArgumentException;
    }

    @Override
    protected void onCancel() {

    }
}
