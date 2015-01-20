package com.abbiya.broadr.jobs;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.api.CommentResponseMap;
import com.abbiya.broadr.dao.Comment;
import com.abbiya.broadr.dao.Message;
import com.abbiya.broadr.events.GettingCommentsEvent;
import com.abbiya.broadr.events.GotCommentsEvent;
import com.abbiya.broadr.repositories.CommentRepo;
import com.abbiya.broadr.retrofit.ApiService;
import com.abbiya.broadr.utility.AppSingleton;
import com.abbiya.broadr.utility.Constants;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.util.concurrent.atomic.AtomicInteger;

import de.greenrobot.event.EventBus;
import retrofit.RetrofitError;

/**
 * Created by seshachalam on 2/10/14.
 */
public class GetCommentsJob extends Job {

    private static final AtomicInteger jobCounter = new AtomicInteger(0);
    private final int id;

    private String registrationId;
    private String messageId;
    private int direction;

    public GetCommentsJob(String registrationId, String messageId) {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.GET_COMMENTS));

        id = jobCounter.incrementAndGet();

        this.registrationId = registrationId;
        this.messageId = messageId;
    }

    public GetCommentsJob(String registrationId, String messageId, int direction) {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.GET_COMMENTS));

        id = jobCounter.incrementAndGet();

        this.registrationId = registrationId;
        this.messageId = messageId;
        this.direction = direction;
    }

    @Override
    public void onRun() throws Throwable {
        if (id != jobCounter.get()) {
            return;
        }
        ApiService apiService = AppSingleton.getApiService();
        Comment lastComment;
        String lastCommentUri;

        Message message = BroadrApp.getMessageRepo().getMessage(messageId);
        CommentRepo commentRepo = BroadrApp.getCommentRepo();

        if (direction == 0) {
            lastComment = commentRepo.getFirstComment(message);
        } else {
            lastComment = commentRepo.getLastComment(message);
        }
        if (lastComment != null) {
            lastCommentUri = lastComment.getUuid();
        } else {
            lastCommentUri = "0";
        }

        CommentResponseMap commentResponseMap = apiService.getDeltaComments(registrationId, messageId, lastCommentUri, direction);

        EventBus.getDefault().post(new GotCommentsEvent(commentResponseMap));
    }

    @Override
    public void onAdded() {
        EventBus.getDefault().post(new GettingCommentsEvent());
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return throwable instanceof RetrofitError;
    }

}
