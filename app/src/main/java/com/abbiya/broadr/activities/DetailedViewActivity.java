package com.abbiya.broadr.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ShareActionProvider;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.R;
import com.abbiya.broadr.adapters.CommentsRecyclerLazyAdapter;
import com.abbiya.broadr.api.CommentResponseMap;
import com.abbiya.broadr.dao.Comment;
import com.abbiya.broadr.dao.Message;
import com.abbiya.broadr.events.AnyoneActiveEvent;
import com.abbiya.broadr.events.CommentDeliveredEvent;
import com.abbiya.broadr.events.CommentSendingEvent;
import com.abbiya.broadr.events.CommentSentEvent;
import com.abbiya.broadr.events.GettingCommentsEvent;
import com.abbiya.broadr.events.GotCommentsEvent;
import com.abbiya.broadr.events.IAMActiveEvent;
import com.abbiya.broadr.events.SavedReceivedCommentsEvent;
import com.abbiya.broadr.jobs.GetCommentsJob;
import com.abbiya.broadr.jobs.SendCommentJob;
import com.abbiya.broadr.jobs.StoreReceivedCommentsJob;
import com.abbiya.broadr.listeners.RecyclerItemClickListener;
import com.abbiya.broadr.repositories.CommentRepo;
import com.abbiya.broadr.repositories.MessageRepo;
import com.abbiya.broadr.retrofit.ApiService;
import com.abbiya.broadr.tasks.SimpleBackgroundTask;
import com.abbiya.broadr.utility.AppSingleton;
import com.abbiya.broadr.utility.Constants;
import com.abbiya.broadr.utility.LocationUtils;
import com.google.inject.Inject;
import com.ocpsoft.pretty.time.PrettyTime;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import de.greenrobot.dao.query.LazyList;
import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

@ContentView(R.layout.activity_detailed_view)
public class DetailedViewActivity extends BaseActivity {

    private static String shareLink;
    int firstVisibleItem, visibleItemCount, totalItemCount;
    MessageRepo messageRepo;
    CommentRepo commentRepo;
    Message message;
    @Inject
    CommentsRecyclerLazyAdapter commentsRecyclerLazyAdapter;
    Long message_id;
    @InjectView(R.id.swipe)
    SwipeRefreshLayout swipeView;
    @InjectView(R.id.base)
    RelativeLayout base;
    @InjectView(R.id.textView_messageContent)
    TextView messageContent;
    @InjectView(R.id.etxt_comment)
    EditText editText;
    @InjectView(R.id.listView_comments)
    RecyclerView recyclerCommentsListView;
    @InjectView(R.id.textView_address)
    TextView addressView;
    //String geoHashViewFontPath = "RobotoTTF/Roboto-Light.ttf";
    //Typeface gtf = Typeface.createFromAsset(BroadrApp.getInstance().getAssets(), geoHashViewFontPath);
    String contentFontPath = "RobotoTTF/Roboto-Regular.ttf";
    Typeface ctf = Typeface.createFromAsset(BroadrApp.getInstance().getAssets(), contentFontPath);
    String messageStatusFontPath = "RobotoTTF/Roboto-Light.ttf";
    Typeface mtf = Typeface.createFromAsset(BroadrApp.getInstance().getAssets(), messageStatusFontPath);
    ShareActionProvider mShareActionProvider;
    private int previousTotal = 0;
    private boolean loading = true;
    private int visibleThreshold = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        swipeView.setEnabled(true);

        swipeView.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        EventBus.getDefault().register(this);

        messageContent.setTypeface(ctf);
        addressView.setTypeface(mtf);

        Intent intent = getIntent();
        message_id = intent.getLongExtra(Constants.SELECTED_MESSAGE_ID, 1L);

        mEditor.putLong(Constants.CURRENT_MESSAGE, message_id);
        mEditor.commit();

        messageRepo = BroadrApp.getMessageRepo();
        commentRepo = BroadrApp.getCommentRepo();

        setupMessage();

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    String commentStr = editText.getText().toString();
                    if (commentStr.trim().length() > 0) {
                        editText.setText("");
                        jobManager.addJobInBackground(new SendCommentJob(commentStr, message.getUuid(), UUID.randomUUID().toString()));
                    }

                    handled = true;
                }
                return handled;
            }
        });

        recyclerCommentsListView.setHasFixedSize(true);
        final LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerCommentsListView.setLayoutManager(llm);
        recyclerCommentsListView.setItemAnimator(new DefaultItemAnimator());
        recyclerCommentsListView.setAdapter(commentsRecyclerLazyAdapter);

        recyclerCommentsListView.addOnItemTouchListener(new RecyclerItemClickListener(context, recyclerCommentsListView, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        }));

        recyclerCommentsListView.setOnScrollListener(new RecyclerView.OnScrollListener() {
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
                    jobManager.addJobInBackground(new GetCommentsJob(registrationId, message.getUuid(), 0));
                }
            }
        });

        final ApiService apiService = AppSingleton.getApiService();

        swipeView.setRefreshing(true);
        int direction = 1;
        jobManager.addJobInBackground(new GetCommentsJob(registrationId, message.getUuid(), direction));

        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeView.setRefreshing(true);
                Comment lastComment = commentRepo.getLastComment(message);
                String lastCommentUri = "0";
                if (lastComment != null) {
                    lastCommentUri = lastComment.getUuid();
                }
                apiService.getDeltaComments(registrationId, message.getUuid(), lastCommentUri, 1, new Callback<CommentResponseMap>() {

                    @Override
                    public void success(CommentResponseMap commentResponseMap, Response response) {
                        swipeView.setRefreshing(false);
                        updateList(commentResponseMap);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.d(Constants.APPTAG, error.getMessage());
                        swipeView.setRefreshing(false);
                    }
                });
            }
        });

    }

    private void updateList(CommentResponseMap commentResponseMap) {
        Boolean error = commentResponseMap.getError();
        shareLink = commentResponseMap.getLink();
        if (shareLink != null) {
            if (mShareActionProvider != null) {
                supportInvalidateOptionsMenu();
                mShareActionProvider.setShareIntent(doShare());
            }
        }
        if (!error) {
            setMessageAddress(message);
            List<Comment> comments = commentResponseMap.getComments();
            jobManager.addJobInBackground(new StoreReceivedCommentsJob(comments));
        } else {
            Toast.makeText(context, commentResponseMap.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setMessageAddress(Message message) {
        if (message.getAddress() == null || message.getAddress().equals("")) {
            String geoHash = message.getGeoHash();
            if (geoHash != null && geoHash.trim().length() > 0) {
                //String link = Constants.GOOGLE_MAPS_URL + LocationUtils.getLocationAsString(geoHash) + "z";
                addressView.setText(getString(R.string.unknown));
                //Linkify.addLinks(addressView, Linkify.ALL);
            }
        } else {
            String[] parts = message.getAddress().split(",");
            if (parts.length >= 3) {
                addressView.setText(parts[0] + ", " + parts[1] + ", " + parts[2]);
            }
        }
    }

    private void refreshList() {
        new SimpleBackgroundTask<LazyList<Comment>>(this) {
            @Override
            protected LazyList<Comment> onRun() {
                return commentRepo.lazyLoadComments(message);
            }

            @Override
            protected void onSuccess(LazyList<Comment> result) {
                commentsRecyclerLazyAdapter.replaceLazyList(result);
            }
        }.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.detailed_view, menu);

        MenuItem shareItem = menu.findItem(R.id.action_share_link);
        mShareActionProvider = (ShareActionProvider)
                MenuItemCompat.getActionProvider(shareItem);

        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(doShare());
        }

        if (!isFlickrImages || latestBGUri == null) {
            menu.removeItem(R.id.action_show_bg);
        }

        shareItem.setEnabled((shareLink != null && !shareLink.isEmpty()));
        shareItem.setVisible((shareLink != null && !shareLink.isEmpty()));

        return super.onCreateOptionsMenu(menu);
    }

    private Intent doShare() {
        Intent shareIntent = createSendIntent(getString(R.string.detailview_share_link) + shareLink, message.getContent());
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return shareIntent;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_open_map:
                String latLon = LocationUtils.getLocationAsString(message.getGeoHash());
                showMap(Uri.parse("geo:" + latLon + "?z=17&q=" + latLon), latLon);
                return true;
            case R.id.action_show_bg:
                String currentBg = mPrefs.getString(Constants.CURRENT_BG, null);
                if (currentBg != null) {
                    openLink(currentBg);
                } else {
                    Toast.makeText(context, getString(R.string.background_not_available), Toast.LENGTH_SHORT).show();
                }
                return true;
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    TaskStackBuilder.create(this)
                            .addNextIntentWithParentStack(upIntent)
                            .startActivities();
                } else {
                    NavUtils.navigateUpTo(this, upIntent);
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (message == null) {
            setupMessage();
        }
        PrettyTime t = new PrettyTime(message.getHappenedAt());
        toolbar.setTitle(t.format(new Date()).replace("from now", "ago"));
        toolbar.setSubtitle(LocationUtils.getPrettyDistance(null, message.getGeoHash(), kmOrMi));

        refreshList();

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

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(SavedReceivedCommentsEvent event) {
        refreshList();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(GettingCommentsEvent event) {

    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(CommentDeliveredEvent ignored) {
        //we could just add this to top or replace element instead of refreshing whole list
        refreshList();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(CommentSendingEvent ignored) {
        //we could just add this to top or replace element instead of refreshing whole list
        refreshList();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(CommentSentEvent ignored) {
        //we could just add this to top or replace element instead of refreshing whole list
        makeNoise();
        refreshList();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(GotCommentsEvent event) {
        swipeView.setRefreshing(false);
        updateList(event.getCommentResponseMap());
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(AnyoneActiveEvent event) {
        EventBus.getDefault().post(new IAMActiveEvent(DetailedViewActivity.class.getCanonicalName()));
    }

    private void setupMessage() {
        if (message_id == null) {
            message_id = mPrefs.getLong(Constants.CURRENT_MESSAGE, 1L);
        }

        message = messageRepo.getMessage(message_id);
        AppSingleton.addObj(Constants.CURRENT_MESSAGE_OBJ, message);
        messageContent.setMovementMethod(new ScrollingMovementMethod());
        messageContent.setText(message.getContent());

        setMessageAddress(message);
    }

    private Intent createSendIntent(String subject, String body) {
        Intent actionSendIntent = new Intent(android.content.Intent.ACTION_SEND);
        actionSendIntent.setType("text/plain");
        actionSendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        actionSendIntent.putExtra(Intent.EXTRA_TEXT, body);
        return actionSendIntent;
    }

    private void showMap(Uri geoLocation, String latLon) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            String link = Constants.GOOGLE_MAPS_URL + latLon + "z";
            openLink(link);
        }
    }

}