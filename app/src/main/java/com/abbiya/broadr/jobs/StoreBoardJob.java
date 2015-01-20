package com.abbiya.broadr.jobs;

import com.abbiya.broadr.BroadrApp;
import com.abbiya.broadr.dao.Board;
import com.abbiya.broadr.events.StoredBoardEvent;
import com.abbiya.broadr.repositories.BoardRepo;
import com.abbiya.broadr.utility.Constants;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import de.greenrobot.event.EventBus;

/**
 * Created by seshachalam on 22/11/14.
 */
public class StoreBoardJob extends Job {
    private static final AtomicInteger jobCounter = new AtomicInteger(0);
    private final int id;
    private String name;

    public StoreBoardJob(String name) {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.SEND_REGISTRATION_MESSAGES));
        this.name = name;

        id = jobCounter.incrementAndGet();
    }

    @Override
    public void onAdded() {
        //store it in db

        BoardRepo boardRepo = BroadrApp.getBoardRepo();
        Board board = boardRepo.getBoard(name);
        if (board == null) {
            board = new Board();
            board.setName(name);
        }
        boardRepo.insertOrReplace(board);

        EventBus.getDefault().post(new StoredBoardEvent(board));
    }

    public void onRun() throws Throwable {
        if (id != jobCounter.get()) {
            return;
        }
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {

        return throwable instanceof IOException;
    }

    @Override
    protected void onCancel() {

    }

}
