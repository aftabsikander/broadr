package com.abbiya.broadr.utility;

/**
 * Created by seshachalam on 29/8/14.
 */
public class Constants {

    public static final String API_ENDPOINT = "http://broadr.in";

    public static final String SHARED_PREFS_KEY = "broadr.shared_prefs";

    public static final String SQLITE_DB_NAME = "broadr.sqlite";

    //JOBS
    public static final String SEND_RAVENS = "send_ravens";
    public static final String GEO_CODER = "geo_coder";
    public static final String SEND_MESSAGES = "send_messages";
    public static final String SEND_REGISTRATION_MESSAGES = "send_registration";
    public static final String SEND_LOCATION = "send_location";
    public static final String SEND_COMMENTS = "send_comments";
    public static final String GET_MESSAGES = "get_messages";
    public static final String GET_COMMENTS = "get_comments";
    public static final String GET_FLICKR_PHOTOS = "get_flickr_photos";
    public static final String STORE_RECEIVED_MESSAGES = "store_received_messages";
    public static final String STORE_RECEIVED_COMMENTS = "store_received_comments";
    public static final String FLAG_MESSAGE = "flag_message";
    public static final String FLAG_COMMENT = "flag_comment";
    public static final String NOTIFICATION = "notification";
    public static final String UPDATE_MESSAGES = "update_messages";

    //GCM
    public static final String GCM_PROJECT_ID = "1026645507924";
    public static final String REGISTER_WITH_GCM = "register_with_gcm";
    public static final String IS_GCM_REGISTATION_JOB_ADDED = "is_registration_with_gcm_added";
    public static final String SENDING_GCM_REGISTRATION_MESSAGE = "sending_registration_message";
    public static final String IS_REGISTRATION_MESSAGE_SENT = "is_registration_message_sent";
    public static final String USER_EMAIL = "user_email";

    //Location
    public static final String LATEST_LATITUDE = "latest_latitude";
    public static final String LATEST_LONGITUDE = "latest_longitude";
    public static final String LOCATION_UPDATE_TIME = "location_update_time";
    public static final String LAST_KNOWN_ADDRESS = "last_known_address";
    public static final String LAST_KNOWN_ADDRESS_FETCH_TIME = "last_known_address_fetch_time";
    public static final String LAST_KNOWN_ADDRESS_REQUESTED_TIME = "last_known_address_requested_time";
    public static final String LAST_BOARD = "last_board";

    public static final int OLD_LOCATION = 1;
    public static final int LOCATION_OK = 2;
    public static final int LOCATION_ERROR = 3;

    public static final String IS_LOCATION_SENT = "is_location_sent";

    //INTENTS
    public static final String SELECTED_MESSAGE_ID = "selected_message_id";

    //SETTINGS DEVICE ID
    public static final String DEVICE_ID = "device_id";

    //MESSAGE STATUSES
    public static final Integer SENDING = 1;
    public static final Integer SENT = 2;
    public static final Integer DELIVERED = 3;
    public static final Integer RECEIVED = 4;
    public static final Integer SEND_ERROR = 5;
    public static final Integer RECEIVED_GCM = 6;
    public static final Integer RECEIVED_GCM_VIEWD = 7;

    //MESSAGE TYPES
    public static final Integer REGISTRATION = 0;
    public static final Integer LOCATION = 1;
    public static final Integer MESSAGE = 2;
    public static final Integer COMMENT = 3;
    public static final Integer LIKE = 4;
    public static final Integer ACTIVE = 5;

    // Debugging tag for the application
    public static final String APPTAG = "Abbiya";

    public static final String MESSAGE_PREFIX = "message-";
    public static final String COMMENT_PREFIX = "comment-";
    public static final String LOCATION_PREFIX = "location-";
    public static final String GCM_REGISTRATION = "registration-";

    //DATE FORMAT
    public static final String ISO8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    //FLICKR CONSTANTS
    public static final String FLICKR_ENDPOINT = "https://api.flickr.com/services";
    public static final String FLICKR_QUERY = "flickr.photos.search";
    public static final String FLICKR_KEY = "FLICKR_API_KEY";
    public static final String LAST_IMAGE_URL = "last_image_url";
    public static final String FLICKR_IMAGE_URLS = "flickr_image_urls";
    public static final String CURRENT_BOARD = "current_board";

    public static final String PROPERTY_REG_ID = "registration_id";
    public static final String PROPERTY_APP_VERSION = "app_version";
    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String CURRENT_BOARD_OBJ = "currentBoard";
    public static final int LOCATION_SETTINGS = 103;

    public static final String CURRENT_MESSAGE = "current_message";
    public static final String CURRENT_MESSAGE_OBJ = "current_message_obj";
    public static final String CURRENT_BG = "current_bg";
    public static final String DATE_FORMAT = "EEE MMM dd HH:mm:ss z yyy";
    public static final String GOOGLE_MAPS_URL = "https://www.google.com/maps/@";
    public static final String GOOGLE_MAPS_STATIC_IMAGES_URL = "http://maps.google.com/maps/api/staticmap?";

}
