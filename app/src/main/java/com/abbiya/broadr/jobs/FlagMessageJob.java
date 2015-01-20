package com.abbiya.broadr.jobs;

import com.abbiya.broadr.utility.Constants;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

/**
 * Created by seshachalam on 10/12/14.
 */
public class FlagMessageJob extends Job {

    public FlagMessageJob() {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.FLAG_MESSAGE));
    }

    @Override
    public void onRun() throws Throwable {

    }

    @Override
    public void onAdded() {

    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return false;
    }
}
