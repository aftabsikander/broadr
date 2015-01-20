package com.abbiya.broadr.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.R;
import com.abbiya.broadr.adapters.MessagesRecyclerLazyAdapter;
import com.abbiya.broadr.api.FlickrPhoto;
import com.abbiya.broadr.api.FlickrPhotos;
import com.abbiya.broadr.api.FlickrResult;
import com.abbiya.broadr.api.MessageResponseMap;
import com.abbiya.broadr.dao.Message;
import com.abbiya.broadr.events.AnyoneActiveEvent;
import com.abbiya.broadr.events.DeliveredRavenEvent;
import com.abbiya.broadr.events.FlickrEvent;
import com.abbiya.broadr.events.GeoCoderResultEvent;
import com.abbiya.broadr.events.GettingMessagesEvent;
import com.abbiya.broadr.events.GotMessagesEvent;
import com.abbiya.broadr.events.IAMActiveEvent;
import com.abbiya.broadr.events.ReceivedGCMMessageEvent;
import com.abbiya.broadr.events.SavedReceivedMessagesEvent;
import com.abbiya.broadr.events.SendingRavenEvent;
import com.abbiya.broadr.events.SentLocationEvent;
import com.abbiya.broadr.events.SentRavenEvent;
import com.abbiya.broadr.events.StoredBoardEvent;
import com.abbiya.broadr.events.UpdateMessageStatusEvent;
import com.abbiya.broadr.events.UpdatedMessageStatusEvent;
import com.abbiya.broadr.gcm.GcmIntentService;
import com.abbiya.broadr.jobs.GetMessagesJob;
import com.abbiya.broadr.jobs.RetrySendingMessagesJob;
import com.abbiya.broadr.jobs.SendMessageJob;
import com.abbiya.broadr.jobs.StoreReceivedMessagesJob;
import com.abbiya.broadr.jobs.UpdateMessageStatusJob;
import com.abbiya.broadr.listeners.RecyclerItemClickListener;
import com.abbiya.broadr.provider.SuggestionsProvider;
import com.abbiya.broadr.repositories.MessageRepo;
import com.abbiya.broadr.retrofit.ApiService;
import com.abbiya.broadr.tasks.SimpleBackgroundTask;
import com.abbiya.broadr.utility.AppSingleton;
import com.abbiya.broadr.utility.Constants;
import com.abbiya.broadr.utility.LocationUtils;
import com.abbiya.broadr.utility.StringUtilities;

import org.json.JSONArray;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.greenrobot.dao.query.LazyList;
import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

@ContentView(R.layout.activity_main)
public class MainActivity extends BaseActivity {

    private static final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    int firstVisibleItem, visibleItemCount, totalItemCount;
    MessageRepo messageRepo;
    @InjectView(R.id.swipe)
    SwipeRefreshLayout swipeView;
    @InjectView(R.id.btn_broadcast)
    Button broadcastBtn;
    @InjectView(R.id.listMessagesView)
    RecyclerView recyclerMessagesListView;
    @InjectView(R.id.etxt_message)
    EditText broadcastEditText;
    @InjectView(R.id.base)
    RelativeLayout base;
    MessagesRecyclerLazyAdapter messagesRecyclerLazyAdapter;
    private int previousTotal = 0;
    private boolean loading = true;
    private int visibleThreshold = 5;
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private String requiredImageSize;
    private String screenSizeInDp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        // enable "type-to-search" functionality
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        swipeView.setEnabled(true);
        swipeView.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        EventBus.getDefault().register(this);

        requiredImageSize = getRequiredImageSize();
        screenSizeInDp = getScreenSizeInDp();

        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);

        messageRepo = BroadrApp.getMessageRepo();

        broadcastBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String typedMessage = broadcastEditText.getText().toString();
                if (typedMessage.trim().length() > 0) {
                    int locationPrompt = getLocation();
                    if (locationPrompt == Constants.LOCATION_OK) {
                        broadcastEditText.setText("");
                        jobManager.addJobInBackground(new SendMessageJob(typedMessage, UUID.randomUUID().toString()));
                    } else {
                        if (locationPrompt == Constants.LOCATION_ERROR) {
                            promptToSwitchGPS(getString(R.string.location_need_alert_title), getString(R.string.location_need_alert_msg), getString(R.string.location_need_yes_btn), getString(R.string.location_need_no_btn));
                        } else if (locationPrompt == Constants.OLD_LOCATION) {
                            promptToSwitchGPS(getString(R.string.location_old_title), getString(R.string.location_old_alert_title), getString(R.string.location_old_yes), getString(R.string.location_old_no));
                        }
                    }
                }
            }
        });

        recyclerMessagesListView.addOnItemTouchListener(new RecyclerItemClickListener(context, recyclerMessagesListView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Message currentMessage = messagesRecyclerLazyAdapter.getItem(position);
                if (currentMessage != null && currentMessage instanceof Message) {
                    //move to detailed view. show comments and stuff there
                    int messageStatus = currentMessage.getStatus();

                    if (messageStatus == Constants.RECEIVED || messageStatus == Constants.DELIVERED || messageStatus == Constants.RECEIVED_GCM) {
                        Intent detailedView = new Intent(context, DetailedViewActivity.class);
                        detailedView.putExtra(Constants.SELECTED_MESSAGE_ID, currentMessage.getId());
                        startActivity(detailedView);
                    } else {
                        Toast.makeText(context, getString(R.string.message_not_yet_sent), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                //long press to block
                Message currentMessage = (Message) messagesRecyclerLazyAdapter.getItem(position);
                if (currentMessage != null) {

                }
            }
        }));

        messagesRecyclerLazyAdapter = new MessagesRecyclerLazyAdapter(kmOrMi);

        recyclerMessagesListView.setHasFixedSize(true);
        final LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerMessagesListView.setLayoutManager(llm);
        recyclerMessagesListView.setItemAnimator(new DefaultItemAnimator());
        recyclerMessagesListView.setAdapter(messagesRecyclerLazyAdapter);

        recyclerMessagesListView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                visibleItemCount = recyclerView.getChildCount();
                totalItemCount = llm.getItemCount();
                firstVisibleItem = llm.findFirstVisibleItemPosition();

                if (loading) {
                    if (totalItemCount > previousTotal) {
                        loading = false;
                        previousTotal = totalItemCount;
                    }
                }
                if (!loading && (totalItemCount - visibleItemCount)
                        <= (firstVisibleItem + visibleThreshold)) {
                    // End has been reached
                    loading = true;
                    if (currentBoard != null && LocationUtils.isSetupOkay()) {
                        syncMessages(0);
                    }
                }
            }
        });

        final ApiService apiService = AppSingleton.getApiService();

        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!swipeView.isRefreshing()) {
                    swipeView.setRefreshing(true);
                }
                if (currentBoard != null) {

                    Message lastMessage = messageRepo.getLastMessage(currentBoard);
                    String lastMessageUri = "0";
                    if (lastMessage != null) {
                        lastMessageUri = lastMessage.getUuid();
                    }

                    String localBoardId = currentBoard.getName();
                    String localRegId = mPrefs.getString(Constants.PROPERTY_REG_ID, "");
                    apiService.getDeltaMessages(localRegId, localBoardId, lastMessageUri, 1, new Callback<MessageResponseMap>() {
                        @Override
                        public void success(MessageResponseMap messageResponseMap, Response response) {
                            swipeView.setRefreshing(false);
                            updateList(messageResponseMap);
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            swipeView.setRefreshing(false);
                            if (error != null && error.getMessage() != null) {
                                //error.getMessage()
                                Toast.makeText(context, getString(R.string.bad_happened), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });

        receiveFromOuterWorld();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mNotifyManager.cancel(GcmIntentService.NOTIFICATION_ID);

        BaseActivity.active_activity = MainActivity.class.getCanonicalName();

        int locationPrompt = getLocation();
        if (locationPrompt == Constants.LOCATION_ERROR) {
            promptToSwitchGPS(getString(R.string.location_need_alert_title), getString(R.string.location_need_alert_msg), getString(R.string.location_need_yes_btn), getString(R.string.location_need_no_btn));
        } else if (locationPrompt == Constants.OLD_LOCATION) {
            promptToSwitchGPS(getString(R.string.location_old_title), getString(R.string.location_old_alert_title), getString(R.string.location_old_yes), getString(R.string.location_old_no));
        } else {
            sendLocationUpdate(true);
        }

        if (LocationUtils.isSetupOkay() && currentBoard != null) {
            refreshList();
        }
        messagesRecyclerLazyAdapter.setKmOrMi(kmOrMi);
        syncMessages(1);

        //add a job to resend the "SENT" messages
        jobManager.addJobInBackground(new RetrySendingMessagesJob());
        jobManager.addJobInBackground(new UpdateMessageStatusJob(Constants.RECEIVED_GCM, Constants.RECEIVED_GCM_VIEWD));

        setBaseBackground(base);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t) {
            //this may crash if registration did not go through. just be safe
        }
    }

    private void updateList(MessageResponseMap messageResponseMap) {
        Boolean error = messageResponseMap.getError();
        if (!error && currentBoard != null) {
            jobManager.addJobInBackground(new StoreReceivedMessagesJob(messageResponseMap.getMessages()));
        } else {
            toolbar.setSubtitle(messageResponseMap.getMessage());
            toolbar.setSubtitleTextColor(Color.BLACK);

            Runnable task = new Runnable() {
                @Override
                public void run() {
                    toolbar.setSubtitle(null);
                }
            };

            toolbar.postDelayed(task, 2000);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        if (!isFlickrImages || latestBGUri == null) {
            menu.removeItem(R.id.action_show_bg);
        }

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setSubmitButtonEnabled(true);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_settings:
                showSettingsActivity();
                return true;
            case R.id.action_current_location:
                if (locationEnabled()) {
                    startLocationSettings();
                } else {
                    promptToSwitchGPS(getString(R.string.location_update_title), getString(R.string.location_update_message), getString(R.string.location_alert_yes), getString(R.string.location_alert_no));
                }
                return true;
            case R.id.action_show_bg:
                String currentBg = mPrefs.getString(Constants.CURRENT_BG, null);
                if (currentBg != null) {
                    openLink(currentBg);
                    //showUrlInDailog(currentBg);
                } else {
                    Toast.makeText(context, getString(R.string.background_not_available), Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_clear_suggestions:
                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                        SuggestionsProvider.AUTHORITY, SuggestionsProvider.MODE);
                suggestions.clearHistory();
                Toast.makeText(this, getString(R.string.search_history_cleared), Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showUrlInDailog(String url) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        //alert.setTitle("Background");
        final WebView wv = new WebView(this);
        wv.loadUrl(url);
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);

                return true;
            }
        });
        alert.setView(wv);
        alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                wv.destroy();
            }
        });
        alert.show();
    }

    private void showSettingsActivity() {
        Intent settingsView = new Intent(context, SettingsActivity.class);
        startActivity(settingsView);
    }

    private void promptToSwitchGPS(String title, String message, String yesBtn, String noBtn) {
        if (locationEnabled()) {
            return;
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(title);
        alert.setMessage(message);

        alert.setPositiveButton(yesBtn, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                startLocationSettings();
            }
        });

        alert.setNegativeButton(noBtn, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    private void refreshList() {
        if (currentBoard != null) {
            new SimpleBackgroundTask<LazyList<Message>>(this) {
                @Override
                protected LazyList<Message> onRun() {
                    return messageRepo.lazyLoadMessages(currentBoard);
                }

                @Override
                protected void onSuccess(LazyList<Message> result) {
                    messagesRecyclerLazyAdapter.replaceLazyList(result);
                }
            }.execute();
        }
    }

    private void syncMessages(int direction) {
        String boardId;
        if (currentBoard == null) {
            boardId = LocationUtils.getGeoHash();
        } else {
            boardId = currentBoard.getName().substring(0, 4);
        }

        jobManager.addJobInBackground(new GetMessagesJob(registrationId, boardId, direction));
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(SavedReceivedMessagesEvent event) {
        onUpdateEvent();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(SendingRavenEvent event) {
        //we could just add this to top or replace element instead of refreshing whole list
        moveListToTop();
        onUpdateEvent();
        //show notification bar
        mBuilder.setContentTitle(getString(R.string.app_name))
                .setTicker(getString(R.string.message_sending))
                .setContentText(event.getMessage().getContent())
                .setSmallIcon(R.drawable.ic_launcher)
                .setProgress(0, 0, true);
        mNotifyManager.notify(StringUtilities.safeLongToInt(event.getMessage().getId()), mBuilder.build());
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(final SentRavenEvent event) {
        //we could just add this to top or replace element instead of refreshing whole list
        //dismiss notification
        Runnable task = new Runnable() {
            @Override
            public void run() {
                mBuilder.setTicker(getString(R.string.notif_sent_raven));
                mBuilder.setContentText(event.getMessage().getContent());
                mNotifyManager.cancel(StringUtilities.safeLongToInt(event.getMessage().getId()));
            }
        };

        worker.schedule(task, 2, TimeUnit.SECONDS);
        moveListToTop();
        onUpdateEvent();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(GeoCoderResultEvent event) {
        String address = event.getAddress();
        broadcastEditText.setHint(address);

        sendLocationUpdate(false);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(GotMessagesEvent event) {
        swipeView.setRefreshing(false);

        MessageResponseMap messageResponseMap = event.getMessageResponseMap();
        updateList(messageResponseMap);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(SentLocationEvent event) {
        mEditor.putBoolean(Constants.IS_LOCATION_SENT, true);
        mEditor.commit();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(FlickrEvent event) {
        FlickrResult fr = event.getResult();
        if (fr != null) {
            FlickrPhotos photos = fr.getPhotos();
            if (photos != null) {
                List<FlickrPhoto> pics = photos.getPhotos();

                int num = pics.size();
                String url;
                if (num > 0) {
                    //prepare json to store
                    JSONArray arrPics = new JSONArray();
                    for (FlickrPhoto p : pics) {
                        String tmpUrl = StringUtilities.constructFlickrImgUrl(p, requiredImageSize);
                        arrPics.put(tmpUrl);
                    }
                    //set the list view background using picasso
                    int ran = StringUtilities.randInt(0, num);
                    if (ran == num) {
                        ran -= 1;
                    }
                    url = StringUtilities.constructFlickrImgUrl(pics.get(ran), requiredImageSize);
                    mEditor.putString(Constants.FLICKR_IMAGE_URLS, arrPics.toString());
                } else {
                    String latLon = LocationUtils.getLocationAsString(LocationUtils.getGeoHash());
                    url = Constants.GOOGLE_MAPS_STATIC_IMAGES_URL + "center=" + latLon + "&zoom=15&size=" + screenSizeInDp + "&sensor=true";
                }

                mEditor.putString(Constants.LAST_IMAGE_URL, url);
                mEditor.putString(Constants.CURRENT_BOARD, LocationUtils.getGeoHash().substring(0, 4));
                mEditor.commit();

                latestBGUri = url;
                setBaseBackground(base);
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(GettingMessagesEvent event) {

    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(ReceivedGCMMessageEvent event) {
        makeNoise();
        onUpdateEvent();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(final DeliveredRavenEvent event) {
        makeNoise();
        onUpdateEvent();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(StoredBoardEvent event) {
        currentBoard = event.getBoard();

        AppSingleton.addObj(Constants.CURRENT_BOARD_OBJ, currentBoard);

        onUpdateEvent();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(AnyoneActiveEvent event) {
        EventBus.getDefault().post(new IAMActiveEvent(MainActivity.class.getCanonicalName()));
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(UpdateMessageStatusEvent event) {
        onUpdateEvent();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(UpdatedMessageStatusEvent event) {
        onUpdateEvent();
    }

    private void moveListToTop() {
        recyclerMessagesListView.post(new Runnable() {
            @Override
            public void run() {
                recyclerMessagesListView.smoothScrollToPosition(0);
            }
        });
    }

    private void onUpdateEvent() {
        if (isVisible()) {
            if (LocationUtils.isSetupOkay()) {
                refreshList();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        // Choose what to do based on the request code
        switch (requestCode) {
            // If the request code matches the code sent in onConnectionFailed
            case Constants.LOCATION_SETTINGS:
                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:

                        break;

                    // If any other result was returned by Google Play services
                    default:

                        break;
                }
                break;
            case Constants.PLAY_SERVICES_RESOLUTION_REQUEST:
                switch (resultCode) {
                    case Activity.RESULT_OK:

                        break;

                    // If any other result was returned by Google Play services
                    default:

                        break;
                }
                break;


            // If any other request code was received
            default:
                // Report that this Activity received an unknown requestCode

                break;
        }
    }

    private void receiveFromOuterWorld() {
        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null && sharedText.length() > 0) {
                    // Update UI to reflect text being shared
                    if (subject != null) {
                        broadcastEditText.setText(subject + " " + sharedText);
                    } else {
                        broadcastEditText.setText(sharedText);
                    }
                }
            }
        }
    }

    private String getRequiredImageSize() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        final int height = dm.heightPixels;
        final int width = dm.widthPixels;

        if (height <= 75 || width <= 75) {
            return "s";
        } else if (height <= 100 || width <= 100) {
            return "t";
        } else if (height <= 150 || width <= 150) {
            return "q";
        } else if (height <= 240 || width <= 240) {
            return "m";
        } else if (height <= 320 || width <= 320) {
            return "n";
        } else if (height <= 500 || width <= 500) {
            return "z";
        } else if (height <= 650 || width <= 650) {
            return "z";
        } else if (height <= 800 || width <= 800) {
            return "c";
        } else if (height <= 1024 || width <= 1024) {
            return "b";
        } else if (height <= 1600 || width <= 1600) {
            return "h";
        } else if (height <= 2048 || width <= 2048) {
            return "k";
        } else {
            return "o";
        }
    }

    private String getScreenSizeInDp() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        final int height = dm.heightPixels;
        final int width = dm.widthPixels;

        return height + "x" + width;
    }

}
