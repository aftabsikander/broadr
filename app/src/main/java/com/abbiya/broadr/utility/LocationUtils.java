/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.abbiya.broadr.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.R;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;

/**
 * Defines app-wide constants and utilities
 */
public final class LocationUtils {

    //1Km is equivalent to 0.6214 miles.

    /*
     * Constants for location update parameters
     */
    // Milliseconds per second
    public static final int MILLISECONDS_PER_SECOND = 1000;

    // The update interval
    public static final int UPDATE_INTERVAL_IN_SECONDS = 600;
    // Update interval in milliseconds
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // A fast interval ceiling
    public static final int FAST_CEILING_IN_SECONDS = 1;
    // A fast ceiling of update intervals, used when the app is visible
    public static final long FAST_INTERVAL_CEILING_IN_MILLISECONDS =
            MILLISECONDS_PER_SECOND * FAST_CEILING_IN_SECONDS;

    // Create an empty string for initializing strings
    public static final String EMPTY_STRING = new String();

    /**
     * Get the latitude and longitude from the Location object returned by
     * Location Services.
     *
     * @param currentLocation A Location object containing the current location
     * @return The latitude and longitude of the current location, or null if no
     * location is available.
     */
    public static String getLatLng(Context context, Location currentLocation) {
        // If the location is valid
        if (currentLocation != null) {

            // Return the latitude and longitude as strings
            return currentLocation.getLatitude() + "," + currentLocation.getLongitude();
        } else {

            // Otherwise, return the empty string
            return EMPTY_STRING;
        }
    }

    public synchronized static String getGeoHash() {
        SharedPreferences sharedPreferences = BroadrApp.getSharedPreferences();
        Double latitude = sharedPreferences.getString(Constants.LATEST_LATITUDE, null) == null ? null : Double.valueOf(sharedPreferences.getString(Constants.LATEST_LATITUDE, null));
        Double longitude = sharedPreferences.getString(Constants.LATEST_LONGITUDE, null) == null ? null : Double.valueOf(sharedPreferences.getString(Constants.LATEST_LONGITUDE, null));
        //calculate geoHash string
        if (latitude != null || longitude != null) {
            return GeoHash.withCharacterPrecision(latitude, longitude, 15).toBase32();
        }

        return "";
    }

    public static String getPrettyDistance(String hash1, String hash2, boolean kmOrMi) {
        double startLatitude = 0.0;
        double startLongitude = 0.0;
        double endLatitude = 0.0;
        double endLongitude = 0.0;
        float[] results = new float[3];
        GeoHash from;
        GeoHash to;

        if (hash1 == null) {
            SharedPreferences sharedPreferences = BroadrApp.getSharedPreferences();
            String str1 = sharedPreferences.getString(Constants.LATEST_LATITUDE, null) == null ? null : sharedPreferences.getString(Constants.LATEST_LATITUDE, null);
            String str2 = sharedPreferences.getString(Constants.LATEST_LONGITUDE, null) == null ? null : sharedPreferences.getString(Constants.LATEST_LONGITUDE, null);
            if (str1 != null) {
                startLatitude = Double.valueOf(str1);
            }
            if (str2 != null) {
                startLongitude = Double.valueOf(str2);
            }
        } else {
            from = GeoHash.fromGeohashString(hash1);
            WGS84Point point = from.getPoint();
            startLatitude = point.getLatitude();
            startLongitude = point.getLongitude();
        }

        to = GeoHash.fromGeohashString(hash2);
        WGS84Point point = to.getPoint();
        endLatitude = point.getLatitude();
        endLongitude = point.getLongitude();

        try {
            Location.distanceBetween(Double.valueOf(startLatitude), Double.valueOf(startLongitude), Double.valueOf(endLatitude), Double.valueOf(endLongitude), results);
        } catch (IllegalArgumentException e) {
            Log.d("Abbiya", e.getMessage());
        }

        if (results.length > 0) {
            Float distance = Float.valueOf(results[0]);
            String message;
            if (distance <= 100) {
                message = BroadrApp.getInstance().getString(R.string.location_near_by);
            } else if (distance > 100 && distance < 500) {
                message = BroadrApp.getInstance().getString(R.string.location_little_far);
            } else {
                if (kmOrMi) {
                    message = (float) Math.round((distance / 1000) * 100) / 100 + " Km";
                } else {
                    message = (float) Math.round(LocationUtils.convertToMiles((distance / 1000) * 100) / 100) + " Mi";
                }
            }

            return message;
        } else {

            return BroadrApp.getInstance().getString(R.string.location_out_of_moon);
        }
    }

    public static String getLocationAsString(String geoHash) {
        GeoHash to = GeoHash.fromGeohashString(geoHash);
        WGS84Point point = to.getPoint();
        if (point != null) {
            return point.getLatitude() + "," + point.getLongitude();
        }

        return "";
    }

    public static int getDistanceDiff(String hash1, String hash2) {
        int diff = 0;
        double startLatitude = 0.0;
        double startLongitude = 0.0;
        double endLatitude = 0.0;
        double endLongitude = 0.0;
        float[] results = new float[3];
        GeoHash from;
        GeoHash to;
        if (hash1 == null) {
            SharedPreferences sharedPreferences = BroadrApp.getSharedPreferences();
            String str1 = sharedPreferences.getString(Constants.LATEST_LATITUDE, null) == null ? null : sharedPreferences.getString(Constants.LATEST_LATITUDE, null);
            String str2 = sharedPreferences.getString(Constants.LATEST_LONGITUDE, null) == null ? null : sharedPreferences.getString(Constants.LATEST_LONGITUDE, null);
            if (str1 != null) {
                startLatitude = Double.valueOf(str1);
            }
            if (str2 != null) {
                startLongitude = Double.valueOf(str2);
            }
        } else {
            from = GeoHash.fromGeohashString(hash1);
            WGS84Point point = from.getPoint();
            startLatitude = point.getLatitude();
            startLongitude = point.getLongitude();
        }

        to = GeoHash.fromGeohashString(hash2);
        WGS84Point point2 = to.getPoint();
        endLatitude = point2.getLatitude();
        endLongitude = point2.getLongitude();

        try {
            Location.distanceBetween(Double.valueOf(startLatitude), Double.valueOf(startLongitude), Double.valueOf(endLatitude), Double.valueOf(endLongitude), results);
        } catch (IllegalArgumentException e) {
            Log.d("Abbiya", e.getMessage());
        }

        if (results.length > 0) {
            Float distance = Float.valueOf(results[0]);
            diff = (int) Math.round((distance / 1000) * 100) / 100;
        }

        return diff;
    }

    public static String getDeviceId(Context context) {
        SharedPreferences sharedPreferences = BroadrApp.getSharedPreferences();
        return sharedPreferences.getString(Constants.DEVICE_ID, null);
    }

    public static float convertToMiles(float km) {
        return (float) 0.6214 * km;
    }

    public synchronized static boolean isSetupOkay() {

        Boolean isGcmOk;
        Boolean isLocOk;

        SharedPreferences mPrefs = BroadrApp.getSharedPreferences();
        //check if gcm registration is success and reached server
        isGcmOk = mPrefs.getBoolean(Constants.IS_REGISTRATION_MESSAGE_SENT, false);

        //check if location is available to start

        isLocOk = mPrefs.getBoolean(Constants.IS_LOCATION_SENT, false);

        //check if all these reached server
        return isGcmOk && isLocOk;
    }
}
