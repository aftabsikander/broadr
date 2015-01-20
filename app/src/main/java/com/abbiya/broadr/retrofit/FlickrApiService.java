package com.abbiya.broadr.retrofit;

import com.abbiya.broadr.api.FlickrResult;

import java.util.Map;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.QueryMap;

/**
 * Created by seshachalam on 8/10/14.
 */
public interface FlickrApiService {
    @GET("/rest")
    void searchFlickr(@QueryMap Map<String, String> options, Callback<FlickrResult> cb);

    @GET("/rest")
    FlickrResult searchFlickr(@QueryMap Map<String, String> options);
}
