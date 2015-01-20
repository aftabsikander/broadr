package com.abbiya.broadr.utility;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;

import com.abbiya.broadr.R;
import com.abbiya.broadr.retrofit.ApiService;
import com.abbiya.broadr.retrofit.FlickrApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.HashMap;

import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

/**
 * Created by seshachalam on 12/10/14.
 */
final public class AppSingleton {

    private static HashMap<String, Object> objectHashMap = new HashMap<String, Object>();

    private static RestAdapter restAdapter, flickrRestAdapter;
    private static ApiService apiService;
    private static FlickrApiService flickrApiService;

    private static Target bgTarget;

    public synchronized static ApiService getApiService() {

        if (restAdapter == null) {
            Gson gson = new GsonBuilder()
                    .setDateFormat(Constants.ISO8601_DATE_FORMAT)
                    .create();
            restAdapter = new RestAdapter.Builder().setEndpoint(Constants.API_ENDPOINT).setConverter(new GsonConverter(gson)).build();
        }
        if (apiService == null) {
            apiService = restAdapter.create(ApiService.class);
        }

        return apiService;
    }

    public synchronized static FlickrApiService getFlickrApiService() {

        if (flickrRestAdapter == null) {
            flickrRestAdapter = new RestAdapter.Builder().setEndpoint(Constants.FLICKR_ENDPOINT).setConverter(new GsonConverter(new Gson())).build();
        }
        if (flickrApiService == null) {
            flickrApiService = flickrRestAdapter.create(FlickrApiService.class);
        }

        return flickrApiService;
    }

    @TargetApi(16)
    public static void setBaseBackground(Context context, String url, View view) {
        bgTarget = new BackgroundTarget(context, url, view);
        Picasso.with(context)
                .load(url)
                .into(bgTarget);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public synchronized static Object getObj(String key) {
        if (objectHashMap != null || objectHashMap.containsKey(key)) {
            return objectHashMap.get(key);
        }

        return null;
    }

    public synchronized static void addObj(String key, Object obj) {
        if (objectHashMap == null) {
            objectHashMap = new HashMap<String, Object>();
        }
        objectHashMap.put(key, obj);
    }

    public synchronized static boolean deleteObj(String key) {
        if (objectHashMap != null || objectHashMap.containsKey(key)) {
            objectHashMap.remove(key);
            return true;
        }

        return false;
    }

    public synchronized static void deleteAllObjects() {
        objectHashMap.clear();
    }

    @TargetApi(16)
    private static final class BackgroundTarget implements Target {
        private Context context;
        private String url;
        private View view;

        BackgroundTarget(Context context, String url, View view) {
            this.context = context;
            this.url = url;
            this.view = view;
        }

        @Override
        public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
            int sdk = android.os.Build.VERSION.SDK_INT;

            BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);

            if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                view.setBackgroundDrawable(drawable);
            } else {
                view.setBackground(drawable);
            }
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            view.setBackgroundResource(R.drawable.backtile);
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            //view.setBackgroundResource(R.drawable.backtile);
        }
    }
}
