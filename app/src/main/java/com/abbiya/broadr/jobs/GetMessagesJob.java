package com.abbiya.broadr.jobs;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.api.MessageResponseMap;
import com.abbiya.broadr.dao.Board;
import com.abbiya.broadr.dao.Message;
import com.abbiya.broadr.events.GettingMessagesEvent;
import com.abbiya.broadr.events.GotMessagesEvent;
import com.abbiya.broadr.repositories.MessageRepo;
import com.abbiya.broadr.retrofit.ApiService;
import com.abbiya.broadr.utility.AppSingleton;
import com.abbiya.broadr.utility.Constants;
import com.abbiya.broadr.utility.LocationUtils;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.util.concurrent.atomic.AtomicInteger;

import de.greenrobot.event.EventBus;
import retrofit.RetrofitError;

/**
 * Created by seshachalam on 2/10/14.
 */
public class GetMessagesJob extends Job {

    private static final AtomicInteger jobCounter = new AtomicInteger(0);
    private final int id;

    private String registrationId;
    private String boardId;
    private int direction;

    public GetMessagesJob(String registrationId, String boardId) {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.GET_MESSAGES));

        id = jobCounter.incrementAndGet();

        this.registrationId = registrationId;
        this.boardId = boardId;
    }

    public GetMessagesJob(String registrationId, String boardId, int direction) {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.GET_MESSAGES));

        id = jobCounter.incrementAndGet();

        this.registrationId = registrationId;
        if (boardId != null && boardId.length() > 4) {
            boardId = boardId.substring(0, 4);
        }
        this.boardId = boardId;
        this.direction = direction;
    }

    @Override
    protected void onCancel() {

    }

    @Override
    public void onAdded() {
        EventBus.getDefault().post(new GettingMessagesEvent());
    }

    @Override
    public void onRun() throws Throwable {
        if (id != jobCounter.get()) {
            return;
        }

        MessageRepo messageRepo = BroadrApp.getMessageRepo();
        Message lastMessage;
        Board board = BroadrApp.getBoardRepo().getBoard(LocationUtils.getGeoHash().substring(0, 4));
        MessageResponseMap messageResponseMap;

        String lastMessageUri;

        if (direction == 0) {
            lastMessage = messageRepo.getFirstMessage(board);
        } else {
            lastMessage = messageRepo.getLastMessage(board);
        }

        if (lastMessage != null) {
            lastMessageUri = lastMessage.getUuid();
        } else {
            lastMessageUri = "0";
        }

        if (!LocationUtils.isSetupOkay()) {
            throw new RuntimeException();
        }

        if (boardId == null) {
            boardId = BroadrApp.getSharedPreferences().getString(Constants.LAST_BOARD, null);
        }

        if (boardId == null) {
            boardId = LocationUtils.getGeoHash();
        }

        if (boardId != null && boardId.length() > 4) {
            boardId = boardId.substring(0, 4);
        }
        ApiService apiService = AppSingleton.getApiService();
        messageResponseMap = apiService.getDeltaMessages(registrationId, boardId, lastMessageUri, direction);

        EventBus.getDefault().post(new GotMessagesEvent(messageResponseMap));
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return throwable instanceof RetrofitError || throwable instanceof RuntimeException;
    }
}
