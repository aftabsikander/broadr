package com.abbiya.broadr.activities;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.R;
import com.abbiya.broadr.adapters.MessagesRecyclerLazyAdapter;
import com.abbiya.broadr.dao.Board;
import com.abbiya.broadr.dao.Message;
import com.abbiya.broadr.events.StoredBoardEvent;
import com.abbiya.broadr.listeners.RecyclerItemClickListener;
import com.abbiya.broadr.provider.SuggestionsProvider;
import com.abbiya.broadr.tasks.SimpleBackgroundTask;
import com.abbiya.broadr.utility.AppSingleton;
import com.abbiya.broadr.utility.Constants;
import com.abbiya.broadr.utility.LocationUtils;

import de.greenrobot.dao.query.LazyList;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

@ContentView(R.layout.activity_search_results)
public class SearchResultsActivity extends BaseActivity {

    @InjectView(R.id.base)
    RelativeLayout base;

    MessagesRecyclerLazyAdapter adapter;
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        recyclerView = (RecyclerView) findViewById(R.id.searchMessagesView);

        handleIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();

        setBaseBackground(base);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH == intent.getAction()) {
            recyclerView.addOnItemTouchListener(
                    new RecyclerItemClickListener(context, recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                        @Override
                        public void onItemClick(View view, int position) {
                            Message currentMessage = adapter.getItem(position);
                            if (currentMessage != null && currentMessage instanceof Message) {
                                //move to detailed view. show comments and stuff there
                                int messageStatus = currentMessage.getStatus();

                                if (messageStatus == Constants.RECEIVED || messageStatus == Constants.DELIVERED) {
                                    Intent detailedView = new Intent(SearchResultsActivity.this, DetailedViewActivity.class);
                                    detailedView.putExtra(Constants.SELECTED_MESSAGE_ID, currentMessage.getId());
                                    startActivity(detailedView);
                                } else {
                                    Toast.makeText(SearchResultsActivity.this, R.string.message_not_yet_sent, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                        @Override
                        public void onItemLongClick(View view, int position) {
                        }
                    })
            );

            String query = intent.getStringExtra(SearchManager.QUERY).trim();

            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    SuggestionsProvider.AUTHORITY, SuggestionsProvider.MODE);
            suggestions.saveRecentQuery(query, null);

            adapter = new MessagesRecyclerLazyAdapter(getDistanceType());

            recyclerView.setHasFixedSize(true);
            final LinearLayoutManager llm = new LinearLayoutManager(this);
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(llm);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setAdapter(adapter);

            if (query != null && !query.trim().isEmpty()) {
                setTitle("*" + query + "*");
                refreshList(query);
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(StoredBoardEvent event) {
        currentBoard = event.getBoard();
        if (currentBoard == null) {
            currentBoard = (Board) AppSingleton.getObj(Constants.CURRENT_BOARD_OBJ);
        } else {
            AppSingleton.addObj(Constants.CURRENT_BOARD_OBJ, currentBoard);
        }
    }

    private void refreshList(final String query) {
        new SimpleBackgroundTask<LazyList<Message>>(this) {
            @Override
            protected LazyList<Message> onRun() {
                if (currentBoard == null) {
                    String boardId = LocationUtils.getGeoHash();
                    currentBoard = BroadrApp.getBoardRepo().getBoard(BroadrApp.getSharedPreferences().getString(LocationUtils.getGeoHash().substring(0, 4), boardId.substring(0, 4)));
                }

                return BroadrApp.getMessageRepo().findMessages(currentBoard, query);
            }

            @Override
            protected void onSuccess(LazyList<Message> result) {
                adapter.replaceLazyList(result);
            }
        }.execute();
    }
}
