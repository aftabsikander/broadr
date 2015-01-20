package com.abbiya.broadr.events;

import com.abbiya.broadr.api.FlickrResult;

public class FlickrEvent {
    public FlickrResult result;

    public FlickrEvent(FlickrResult flickrResult) {
        this.result = flickrResult;
    }

    public FlickrResult getResult() {
        return this.result;
    }
}
