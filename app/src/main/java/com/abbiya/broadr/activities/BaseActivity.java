package com.abbiya.broadr.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.R;
import com.abbiya.broadr.dao.Board;
import com.abbiya.broadr.jobs.GeocoderJob;
import com.abbiya.broadr.jobs.GetFlickrPhotosJob;
import com.abbiya.broadr.jobs.SendLocationJob;
import com.abbiya.broadr.jobs.StoreBoardJob;
import com.abbiya.broadr.utility.AppSingleton;
import com.abbiya.broadr.utility.Constants;
import com.abbiya.broadr.utility.LocationUtils;
import com.abbiya.broadr.utility.StringUtilities;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.path.android.jobqueue.JobManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import roboguice.activity.RoboActionBarActivity;

public class BaseActivity extends RoboActionBarActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {
    public static String active_activity = "";
    protected boolean visible = false;
    protected SharedPreferences mPrefs;
    protected SharedPreferences sPrefs;
    protected SharedPreferences.Editor mEditor;
    protected Context context;
    protected JobManager jobManager;

    //location stuff
    protected Board currentBoard;
    protected boolean mUpdatesRequested = false;
    protected LocationRequest mLocationRequest;
    protected LocationClient mLocationClient;

    protected long locNotifInterval;
    protected boolean kmOrMi = true;
    protected boolean isFlickrImages;
    protected long notifInterval;
    protected String latestBGUri;
    protected String registrationId;
    protected Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BaseActivity.active_activity = "";
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        context = getApplicationContext();
        jobManager = BroadrApp.getInstance().getJobManager();
        mPrefs = BroadrApp.getSharedPreferences();
        sPrefs = BroadrApp.getSettingsPreferences();
        mEditor = mPrefs.edit();

        mUpdatesRequested = true;
        mLocationClient = new LocationClient(context, this, this);

        mLocationRequest = LocationRequest.create()
                .setInterval(15000)
                .setFastestInterval(5000)
                .setSmallestDisplacement(50)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        visible = true;

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode == ConnectionResult.SUCCESS) {
            if (!mLocationClient.isConnected()) {
                mLocationClient.connect();
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(resultCode, this, 1).show();
        }

        init();
    }

    @Override
    protected void onPause() {
        super.onPause();
        visible = false;

        BaseActivity.active_activity = "";

        if (mLocationClient.isConnected()) {
            mLocationClient.removeLocationUpdates(this);
            mLocationClient.disconnect();
        }
    }

    public boolean isVisible() {
        return visible;
    }

    protected boolean getDistanceType() {
        String distanceType = sPrefs.getString(SettingsActivity.DISTANCE_TYPE, "km");

        return distanceType.contentEquals("km");
    }

    protected void makeNoise() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (resultCode == ConnectionResult.SUCCESS) {
            return true;
        } else {
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    resultCode,
                    this,
                    Constants.PLAY_SERVICES_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment =
                        new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(getSupportFragmentManager(),
                        "Need play services");
            }
            return false;
        }
    }

    protected int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    protected String getRegistrationId(Context context) {
        SharedPreferences prefs = BroadrApp.getSharedPreferences();
        String registrationId = prefs.getString(Constants.PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            return "";
        }
        int registeredVersion = prefs.getInt(Constants.PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            return "";
        }
        return registrationId;
    }

    protected Boolean locationEnabled() {
        LocationManager lm;
        boolean gps_enabled = false;
        boolean network_enabled = false;

        lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {

        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {

        }

        return gps_enabled || network_enabled;
    }

    private void init() {
        kmOrMi = getDistanceType();
        registrationId = mPrefs.getString(Constants.PROPERTY_REG_ID, "");
        latestBGUri = mPrefs.getString(Constants.LAST_IMAGE_URL, null);
        isFlickrImages = sPrefs.getBoolean(SettingsActivity.USE_FLICKR_IMAGES, true);
        notifInterval = Long.valueOf(sPrefs.getString(SettingsActivity.NOTIF_PERIOD, "1")).longValue();
        locNotifInterval = Long.valueOf(sPrefs.getString(SettingsActivity.LOCATION_NOTIF_PERIOD, "172800000")).longValue();
        currentBoard = (Board) AppSingleton.getObj(Constants.CURRENT_BOARD_OBJ);
    }

    protected void sendLocationUpdate(Boolean isGeocoderNeeded) {
        String geoHash = LocationUtils.getGeoHash();
        GeoHash from = GeoHash.fromGeohashString(geoHash);
        WGS84Point point = from.getPoint();
        Double startLatitude = point.getLatitude();
        Double startLongitude = point.getLongitude();

        SharedPreferences sharedPreferences = BroadrApp.getSharedPreferences();

        String address = sharedPreferences.getString(Constants.LAST_KNOWN_ADDRESS, "");

        jobManager.addJobInBackground(new SendLocationJob(String.valueOf(startLatitude), String.valueOf(startLongitude), address, UUID.randomUUID().toString()));

        if (isGeocoderNeeded) {
            jobManager.addJobInBackground(new GeocoderJob(startLatitude, startLongitude));
        }

        fetchFlickrPhotos(String.valueOf(startLatitude), String.valueOf(startLongitude));
    }

    protected int getLocation() {
        Double longitude = null;
        Double latitude = null;

        //check if latest location is available
        Date now = new Date();

        String lastLocationUpdate = mPrefs.getString(Constants.LOCATION_UPDATE_TIME, now.toString());
        //yyyy-MM-dd'T'HH:mm:ssZ
        //EEE MMM dd HH:mm:ss z yyy
        SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
        Date lastLocationUpdateTime;
        try {
            lastLocationUpdateTime = dateFormat.parse(lastLocationUpdate);
        } catch (ParseException e) {
            lastLocationUpdateTime = now;
            e.printStackTrace();
        }
        long diff = now.getTime() - lastLocationUpdateTime.getTime();

        if (diff >= locNotifInterval) {
            return Constants.OLD_LOCATION;
        } else {
            String lastLongitude = mPrefs.getString(Constants.LATEST_LONGITUDE, null);
            String lastLatitude = mPrefs.getString(Constants.LATEST_LATITUDE, null);

            if (lastLatitude == null || lastLatitude == null) {
                Location lastLocation = null;
                try {
                    if (mLocationClient.isConnecting() || !mLocationClient.isConnected()) {
                        //Toast.makeText(MainActivity.this, "Connecting to get the location", Toast.LENGTH_LONG).show();
                    }
                    if (mLocationClient.isConnected()) {
                        lastLocation = mLocationClient.getLastLocation();
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
                if (mLocationClient != null && lastLocation != null) {
                    lastLocation = mLocationClient.getLastLocation();
                    longitude = lastLocation.getLongitude();
                    latitude = lastLocation.getLatitude();
                }
            } else {
                longitude = Double.valueOf(lastLongitude);
                latitude = Double.valueOf(lastLatitude);
            }

        }
        if (longitude != null || latitude != null) {
            storeLocation(latitude, longitude);

            return Constants.LOCATION_OK;
        }

        return Constants.LOCATION_ERROR;
    }

    private void fetchFlickrPhotos(String lat, String lon) {
        SharedPreferences sharedPreferences = BroadrApp.getSharedPreferences();
        String address = sharedPreferences.getString(Constants.LAST_KNOWN_ADDRESS, "");
        String city = null;
        String tags = null;

        if (!address.equals("")) {
            String[] parts = address.split(",");
            if (parts.length >= 3) {
                city = parts[parts.length - 2];
                tags = address;
            }
        }

        String flickrUrls = mPrefs.getString(Constants.FLICKR_IMAGE_URLS, null);
        String lastBoard = mPrefs.getString(Constants.CURRENT_BOARD, null);
        if ((lastBoard != null && !lastBoard.equals(LocationUtils.getGeoHash().substring(0, 4))) || flickrUrls == null) {
            jobManager.addJobInBackground(new GetFlickrPhotosJob(lat, lon, city, tags));
        } else {
            try {
                JSONArray urls = new JSONArray(flickrUrls);
                latestBGUri = urls.get(StringUtilities.randInt(0, urls.length())).toString();
            } catch (JSONException e) {

            }
        }
    }

    protected void setBaseBackground(View base) {
        String randFlickrUrl = mPrefs.getString(Constants.FLICKR_IMAGE_URLS, null);
        if (randFlickrUrl != null) {
            try {
                JSONArray tmpArray = new JSONArray(randFlickrUrl);
                randFlickrUrl = tmpArray.get(StringUtilities.randInt(0, tmpArray.length())).toString();
            } catch (JSONException e) {

            }
        }

        if (randFlickrUrl != null) {
            latestBGUri = randFlickrUrl;
        } else {
            String url = mPrefs.getString(Constants.LAST_IMAGE_URL, null);
            if (url != null) {
                latestBGUri = url;
            }
        }

        if (isFlickrImages) {
            if (latestBGUri != null) {
                mEditor.putString(Constants.CURRENT_BG, latestBGUri);
                mEditor.commit();
                AppSingleton.setBaseBackground(context, latestBGUri, base);
            }
        } else {
            base.setBackgroundResource(R.drawable.backtile);
        }
    }

    protected void storeLocation(Double lat, Double lon) {
        mEditor.putString(Constants.LATEST_LATITUDE, Double.toString(lat));
        mEditor.putString(Constants.LATEST_LONGITUDE, Double.toString(lon));
        mEditor.putString(Constants.LOCATION_UPDATE_TIME, new Date().toString());
        String lastBoard = LocationUtils.getGeoHash();
        if (lastBoard != null && lastBoard.length() >= 4) {
            jobManager.addJobInBackground(new StoreBoardJob(lastBoard.substring(0, 4)));
            mEditor.putString(Constants.LAST_BOARD, lastBoard.substring(0, 4));
        }
        mEditor.commit();
    }

    protected void startLocationSettings() {
        Intent gpsOptionsIntent = new Intent(
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(gpsOptionsIntent, Constants.LOCATION_SETTINGS);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(
                        this,
                        Constants.PLAY_SERVICES_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDisconnected() {
        mLocationClient.removeLocationUpdates(this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        storeLocation(location.getLatitude(), location.getLongitude());
        sendLocationUpdate(true);
    }

    protected void openLink(String uri) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(browserIntent);
    }

    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;

        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
}
