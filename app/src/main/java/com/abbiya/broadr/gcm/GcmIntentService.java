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

package com.abbiya.broadr.gcm;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.R;
import com.abbiya.broadr.activities.BaseActivity;
import com.abbiya.broadr.activities.MainActivity;
import com.abbiya.broadr.activities.SettingsActivity;
import com.abbiya.broadr.dao.Board;
import com.abbiya.broadr.dao.Comment;
import com.abbiya.broadr.dao.Message;
import com.abbiya.broadr.events.CommentDeliveredEvent;
import com.abbiya.broadr.events.DeliveredRavenEvent;
import com.abbiya.broadr.events.ReceivedGCMMessageEvent;
import com.abbiya.broadr.events.SavedReceivedMessagesEvent;
import com.abbiya.broadr.events.SentLocationEvent;
import com.abbiya.broadr.events.SentRegistrationMessageEvent;
import com.abbiya.broadr.jobs.SendCommentJob;
import com.abbiya.broadr.jobs.SendLocationJob;
import com.abbiya.broadr.jobs.SendMessageJob;
import com.abbiya.broadr.jobs.SendRegistrationMessageJob;
import com.abbiya.broadr.repositories.CommentRepo;
import com.abbiya.broadr.repositories.MessageRepo;
import com.abbiya.broadr.utility.AppSingleton;
import com.abbiya.broadr.utility.Constants;
import com.abbiya.broadr.utility.LocationUtils;
import com.abbiya.broadr.utility.StringUtilities;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.path.android.jobqueue.JobManager;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import de.greenrobot.event.EventBus;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        JobManager jobManager = BroadrApp.getInstance().getJobManager();
        Bundle extras = intent.getExtras();

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.
             */

            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                //retry sending
                String messageId = (String) extras.get("google.message_id");
                String uuid = null;
                Integer msgType = null;

                if (messageId != null) {
                    if (messageId.startsWith(Constants.MESSAGE_PREFIX)) {
                        uuid = messageId.substring(Constants.MESSAGE_PREFIX.length(), messageId.length());
                        msgType = Constants.MESSAGE;
                    } else if (messageId.startsWith(Constants.COMMENT_PREFIX)) {
                        uuid = messageId.substring(Constants.COMMENT_PREFIX.length(), messageId.length());
                        msgType = Constants.COMMENT;
                    } else if (messageId.startsWith(Constants.GCM_REGISTRATION)) {
                        String email = BroadrApp.getSharedPreferences().getString(Constants.USER_EMAIL, "");
                        jobManager.addJobInBackground(new SendRegistrationMessageJob(email, UUID.randomUUID().toString()));
                    } else if (messageId != null && messageId.startsWith(Constants.LOCATION_PREFIX)) {
                        String geoHash = LocationUtils.getGeoHash();
                        GeoHash from = GeoHash.fromGeohashString(geoHash);
                        WGS84Point point = from.getPoint();
                        Double startLatitude = point.getLatitude();
                        Double startLongitude = point.getLongitude();

                        SharedPreferences sharedPreferences = BroadrApp.getSharedPreferences();

                        String address = sharedPreferences.getString(Constants.LAST_KNOWN_ADDRESS, "");

                        jobManager.addJobInBackground(new SendLocationJob(String.valueOf(startLatitude), String.valueOf(startLongitude), address, UUID.randomUUID().toString()));
                    }
                }
                if (uuid != null) {
                    if (msgType == Constants.MESSAGE) {
                        MessageRepo messageRepo = BroadrApp.getMessageRepo();
                        Message message = messageRepo.getMessage(uuid);
                        if (message != null) {
                            jobManager.addJobInBackground(new SendMessageJob(message.getId(), message.getContent(), message.getUuid()));
                        }
                    } else if (msgType == Constants.COMMENT) {
                        CommentRepo commentRepo = BroadrApp.getCommentRepo();
                        Comment comment = commentRepo.getComment(uuid);
                        if (comment != null) {
                            jobManager.addJobInBackground(new SendCommentJob(comment.getId(), comment.getContent(), comment.getUuid()));
                        }
                    }
                } else {
                    //sendNotification("Send error: " + extras.toString());
                }
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " + extras.toString(), NOTIFICATION_ID);
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                //parse the message and do db operations post the event
                //Post notification of received message.
                String receivedMessageType = extras.getString("t");
                String messageId = null;
                MessageRepo messageRepo = BroadrApp.getMessageRepo();
                CommentRepo commentRepo = BroadrApp.getCommentRepo();
                Message message = null;
                Comment comment = null;
                if (null != extras.get("m_id")) {
                    messageId = String.valueOf(extras.get("m_id"));
                }

                //check the message id prefix and find the comment or message
                if (messageId != null && messageId.startsWith(Constants.MESSAGE_PREFIX)) {
                    messageId = messageId.substring(Constants.MESSAGE_PREFIX.length());
                    message = messageRepo.getMessage(messageId);
                } else if (messageId != null && messageId.startsWith(Constants.COMMENT_PREFIX)) {
                    messageId = messageId.substring(Constants.COMMENT_PREFIX.length());
                    comment = commentRepo.getComment(messageId);
                } else if (messageId != null && messageId.startsWith(Constants.GCM_REGISTRATION)) {
                    EventBus.getDefault().post(new SentRegistrationMessageEvent());
                } else if (messageId != null && messageId.startsWith(Constants.LOCATION_PREFIX)) {
                    EventBus.getDefault().post(new SentLocationEvent());
                }

                if (extras.get("o") != null) {
                    sendNotification(String.valueOf(extras.get("o")), 2);
                }

                if (message == null) {
                    int type = 0;
                    if (receivedMessageType != null) {
                        switch (receivedMessageType.charAt(0)) {
                            case 'r':
                                type = 0;
                                break;
                            case 'l':
                                type = 1;
                                break;
                            case 'm':
                                type = 2;
                                //received from gcm by others
                                String content = String.valueOf(extras.get("c"));
                                String geoHash = String.valueOf(extras.get("l"));
                                String uuid = String.valueOf(extras.get("s_id"));
                                message = messageRepo.getMessage(uuid);
                                if (message == null) {
                                    message = new Message(content, new Date(), geoHash);
                                    message.setType(Constants.MESSAGE);
                                    message.setUpdatedAt(new Date());
                                    message.setStatus(Constants.RECEIVED_GCM);
                                    message.setUuid(uuid);

                                    Board currentBoard = (Board) AppSingleton.getObj(Constants.CURRENT_BOARD_OBJ);

                                    if (currentBoard == null) {
                                        String board = BroadrApp.getSharedPreferences().getString(Constants.LAST_BOARD, null);
                                        if (board != null && board.length() >= 4) {
                                            currentBoard = BroadrApp.getBoardRepo().getBoard(board.substring(0, 4));
                                        }
                                    }
                                    if (message.getHappenedAt().getTime() > new Date().getTime()) {
                                        message.setHappenedAt(new Date());
                                    }
                                    message.setBoard(currentBoard);
                                    messageRepo.insertOrReplace(message);
                                    EventBus.getDefault().post(new SavedReceivedMessagesEvent());
                                    EventBus.getDefault().post(new ReceivedGCMMessageEvent(message));

                                    long when = Long.valueOf(BroadrApp.getSettingsPreferences().getString(SettingsActivity.NOTIF_PERIOD, "1")).longValue();
                                    if (when == 1) {
                                        checkNumberOfReceivedMessagesTillNowAndNotify(false);
                                    }
                                }
                                break;
                            case 'c':
                                type = 3;
                                break;
                            case 'a':
                                type = 5;
                                break;
                            default:
                                type = 0;
                                break;

                        }
                        if (receivedMessageType.equals("lk")) {
                            type = 4;
                        }
                    }
                } else {
                    //this guy's message
                    message.setUpdatedAt(new Date());
                    message.setStatus(Constants.DELIVERED);
                    messageRepo.updateMessage(message);
                    //message sent event
                    EventBus.getDefault().post(new DeliveredRavenEvent(message));
                    mNotificationManager.cancel(StringUtilities.safeLongToInt(message.getId()));
                }

                if (comment != null) {
                    comment.setHappenedAt(new Date());
                    comment.setUpdatedAt(new Date());
                    comment.setStatus(Constants.DELIVERED);
                    commentRepo.updateComment(comment);
                    //comment sent event
                    EventBus.getDefault().post(new CommentDeliveredEvent(comment));
                }

            } else {
                Log.d(Constants.APPTAG, messageType);
                //sendNotification(messageType);
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    /*
    public void onEvent(IAMActiveEvent event) {
        String activeActivity = event.getActivity();
        if (MainActivity.class.getCanonicalName().equals(activeActivity)) {

        } else if (DetailedViewActivity.class.getCanonicalName().equals(activeActivity)) {
            checkNumberOfReceivedMessagesTillNowAndNotify();
        } else if (BootupActivity.class.getCanonicalName().equals(activeActivity)) {
            checkNumberOfReceivedMessagesTillNowAndNotify();
        } else {
            checkNumberOfReceivedMessagesTillNowAndNotify();
        }
    }

    public void onEvent(SavedReceivedMessagesEvent event) {
        EventBus.getDefault().post(new AnyoneActiveEvent());
    }
    */

    private void checkNumberOfReceivedMessagesTillNowAndNotify(boolean len) {
        MessageRepo messageRepo = BroadrApp.getMessageRepo();
        Integer[] statuses = {Constants.RECEIVED_GCM};
        List<Message> receivedMessages = messageRepo.getMessagesOfStatus(statuses);

        if (receivedMessages != null && receivedMessages.size() > 0) {
            int lenMessages = receivedMessages.size();
            if (!MainActivity.class.getCanonicalName().equals(BaseActivity.active_activity)) {
                if (len) {
                    sendNotification(getString(R.string.gcm_notif_checkout) + lenMessages + " " + getString(R.string.gcm_notif_new_messages), NOTIFICATION_ID);
                } else {
                    sendNotification(getString(R.string.gcm_notif_checkout) + getString(R.string.gcm_notif_new_messages), NOTIFICATION_ID);
                }
            }
        }
    }

    // Put the message into a notification and post it.
    private void sendNotification(String msg, int notificationId) {
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(getString(R.string.app_name))
                        .setTicker(getString(R.string.messages_new_arrival))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg)
                        .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                        .setAutoCancel(true);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(notificationId, mBuilder.build());
    }
}
