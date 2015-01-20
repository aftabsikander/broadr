package com.abbiya.broadr.jobs;

import com.abbiya.broadr.api.FlickrResult;
import com.abbiya.broadr.events.FlickrEvent;
import com.abbiya.broadr.retrofit.FlickrApiService;
import com.abbiya.broadr.utility.AppSingleton;
import com.abbiya.broadr.utility.Constants;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import de.greenrobot.event.EventBus;
import retrofit.RetrofitError;

/**
 * Created by seshachalam on 8/10/14.
 */
public class GetFlickrPhotosJob extends Job {

    private static final AtomicInteger jobCounter = new AtomicInteger(0);
    private final int id;
    private String lat;
    private String lon;
    private String text;
    private String tags;

    public GetFlickrPhotosJob(String lat, String lon, String text, String tags) {
        super(new Params(Priority.LOW).requireNetwork().persist().groupBy(Constants.GET_FLICKR_PHOTOS));

        this.lat = lat;
        this.lon = lon;
        this.text = text;
        this.tags = tags;

        id = jobCounter.incrementAndGet();
    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {

        if (id != jobCounter.get()) {
            return;
        }

        Map<String, String> options = new HashMap<String, String>();

        options.put("method", Constants.FLICKR_QUERY);
        options.put("api_key", Constants.FLICKR_KEY);
        options.put("format", "json");
        options.put("nojsoncallback", "1");

        options.put("lat", lat);
        options.put("lon", lon);

        if (tags != null) {
            options.put("tags", tags);
        }
        if (text != null) {
            options.put("text", text);
        }

        FlickrApiService flickrApiService = AppSingleton.getFlickrApiService();
        FlickrResult flickrResult = flickrApiService.searchFlickr(options);

        EventBus.getDefault().post(new FlickrEvent(flickrResult));
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return throwable instanceof RetrofitError;
    }
}
