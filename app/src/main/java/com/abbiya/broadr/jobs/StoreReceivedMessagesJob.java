package com.abbiya.broadr.jobs;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.dao.Board;
import com.abbiya.broadr.dao.Message;
import com.abbiya.broadr.events.SavedReceivedMessagesEvent;
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
public class StoreReceivedMessagesJob extends Job {

    private List<Message> fetchedMessages;

    public StoreReceivedMessagesJob(List<Message> fetchedMessages) {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.STORE_RECEIVED_MESSAGES));

        this.fetchedMessages = fetchedMessages;
    }

    @Override
    public void onRun() throws Throwable {
        if (fetchedMessages != null && fetchedMessages.size() > 0) {
            Board currentBoard = (Board) AppSingleton.getObj(Constants.CURRENT_BOARD_OBJ);

            if (currentBoard == null) {
                String board = BroadrApp.getSharedPreferences().getString(Constants.LAST_BOARD, null);
                if (board != null && board.length() >= 4) {
                    currentBoard = BroadrApp.getBoardRepo().getBoard(board.substring(0, 4));
                }
            }
            for (Message message : fetchedMessages) {
                if (message.getHappenedAt().getTime() > new Date().getTime()) {
                    message.setHappenedAt(new Date());
                }
                message.setBoard(currentBoard);
            }
            BroadrApp.getMessageRepo().insertMessages(fetchedMessages);
        }

        EventBus.getDefault().post(new SavedReceivedMessagesEvent());
    }

    @Override
    public void onAdded() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return throwable instanceof Exception;
    }

    @Override
    protected void onCancel() {

    }
}
