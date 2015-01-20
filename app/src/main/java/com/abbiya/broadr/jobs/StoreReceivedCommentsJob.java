package com.abbiya.broadr.jobs;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.dao.Comment;
import com.abbiya.broadr.dao.Message;
import com.abbiya.broadr.events.SavedReceivedCommentsEvent;
import com.abbiya.broadr.utility.AppSingleton;
import com.abbiya.broadr.utility.Constants;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.util.Date;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by seshachalam on 6/12/14.
 */
public class StoreReceivedCommentsJob extends Job {

    private List<Comment> fetchedComments;

    public StoreReceivedCommentsJob(List<Comment> fetchedComments) {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.STORE_RECEIVED_COMMENTS));
        this.fetchedComments = fetchedComments;
    }

    @Override
    protected void onCancel() {

    }

    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {
        if (fetchedComments != null && fetchedComments.size() > 0) {
            Message currentMessage = (Message) AppSingleton.getObj(Constants.CURRENT_MESSAGE_OBJ);
            if (currentMessage == null) {
                long message_id = BroadrApp.getSharedPreferences().getLong(Constants.CURRENT_MESSAGE, 1L);
                currentMessage = BroadrApp.getMessageRepo().getMessage(message_id);
            }
            for (Comment comment : fetchedComments) {
                if (comment.getHappenedAt().getTime() > new Date().getTime()) {
                    comment.setHappenedAt(new Date());
                }
                comment.setMessage(currentMessage);
            }

            BroadrApp.getCommentRepo().insertComments(fetchedComments);
        }

        EventBus.getDefault().post(new SavedReceivedCommentsEvent());
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return throwable instanceof Exception;
    }
}
