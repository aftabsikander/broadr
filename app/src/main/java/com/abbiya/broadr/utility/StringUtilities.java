package com.abbiya.broadr.utility;

import com.abbiya.broadr.api.FlickrPhoto;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Random;

public class StringUtilities {

    public static String makeUrlEncoded(String input) throws UnsupportedEncodingException {
        return URLEncoder.encode(input, "UTF-8");
    }

    public static String constructFlickrImgUrl(FlickrPhoto photo, String size) {
        String FARMID = String.valueOf(photo.getFarm());
        String SERVERID = photo.getServer();
        String SECRET = photo.getSecret();
        String ID = photo.getId();

        StringBuilder sb = new StringBuilder();

        sb.append("http://farm");
        sb.append(FARMID);
        sb.append(".static.flickr.com/");
        sb.append(SERVERID);
        sb.append("/");
        sb.append(ID);
        sb.append("_");
        sb.append(SECRET);
        sb.append("_");
        sb.append(size);
        sb.append(".jpg");

        return sb.toString();
    }

    public static int randInt(int min, int max) {

        // NOTE: Usually this should be a field rather than a method
        // variable so that it is not re-seeded every call.
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

}
