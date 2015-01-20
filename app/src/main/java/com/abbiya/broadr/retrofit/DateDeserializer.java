package com.abbiya.broadr.retrofit;

import com.abbiya.broadr.utility.Constants;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by seshachalam on 30/9/14.
 */
public class DateDeserializer implements JsonDeserializer<Date> {

    @Override
    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String dateString = json.getAsJsonPrimitive().getAsString();
        Date date = null;

        SimpleDateFormat formatter = new SimpleDateFormat(Constants.ISO8601_DATE_FORMAT);

        try {
            date = formatter.parse(dateString);
            System.out.println(date);
            System.out.println(formatter.format(date));

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return date;
    }
}

