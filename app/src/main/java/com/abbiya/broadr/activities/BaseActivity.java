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
import android.util.Log;
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
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import com.path.android.jobqueue.JobManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import roboguice.activity.RoboActionBarActivity;

public class BaseActivity extends RoboActionBarActivity implements
        ConnectionCallbacks,
        OnConnectionFailedListener,
        LocationListener {
    protected static final String TAG = "location-updates";

    public static String active_activity = "";
    protected boolean visible = false;
    protected SharedPreferences mPrefs;
    protected SharedPreferences sPrefs;
    protected SharedPreferences.Editor mEditor;
    protected Context context;
    protected JobManager jobManager;

    //location stuff
    protected Board currentBoard;

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;

    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;

    protected long locNotifInterval;
    protected boolean kmOrMi = true;
    protected boolean isFlickrImages;
    protected long notifInterval;
    protected String latestBGUri;
    protected String registrationId;
    protected Toolbar toolbar;


    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates;

    /**
     * Time when the location was updated represented as a String.
     */
    protected String mLastUpdateTime;

    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

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

        mRequestingLocationUpdates = true;
        mLastUpdateTime = "";

        buildGoogleApiClient();

        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        visible = true;

        init();

        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.

        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        visible = false;

        BaseActivity.active_activity = "";

        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
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
                if (mGoogleApiClient.isConnected() && mCurrentLocation != null) {
                    longitude = mCurrentLocation.getLongitude();
                    latitude = mCurrentLocation.getLatitude();
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


    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
        }
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");

        // If the initial location was never previously requested, we use
        // FusedLocationApi.getLastLocation() to get it. If it was previously requested, we store
        // its value in the Bundle and check for it in onCreate(). We
        // do not request it again unless the user specifically requests location updates by pressing
        // the Start Updates button.
        //
        // Because we cache the value of the initial location in the Bundle, it means that if the
        // user launches the activity,
        // moves to a new location, and then changes the device orientation, the original location
        // is displayed as the activity is re-created.
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        }

        // If the user presses the Start Updates button before GoogleApiClient connects, we set
        // mRequestingLocationUpdates to true (see startUpdatesButtonHandler()). Here, we check
        // the value of mRequestingLocationUpdates and if it is true, we start location updates.
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    /**
     * Callback that fires when the location changes.
     */
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

        storeLocation(location.getLatitude(), location.getLongitude());
        sendLocationUpdate(true);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        if (result.hasResolution()) {
            try {
                result.startResolutionForResult(
                        this,
                        Constants.PLAY_SERVICES_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }
}
