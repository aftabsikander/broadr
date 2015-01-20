package com.abbiya.broadr;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import com.abbiya.broadr.dao.DaoMaster;
import com.abbiya.broadr.dao.DaoMaster.DevOpenHelper;
import com.abbiya.broadr.repositories.BoardRepo;
import com.abbiya.broadr.repositories.CommentRepo;
import com.abbiya.broadr.repositories.MessageRepo;
import com.abbiya.broadr.utility.AppSingleton;
import com.abbiya.broadr.utility.Constants;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.config.Configuration;

import roboguice.RoboGuice;

/**
 * Created by seshachalam on 29/8/14.
 */
public class BroadrApp extends Application {
    private static BroadrApp instance;
    private static MessageRepo messageRepo;
    private static CommentRepo commentRepo;
    private static BoardRepo boardRepo;
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences settingsPreferences;
    private JobManager jobManager;

    public BroadrApp() {
        instance = this;
    }

    public static BroadrApp getInstance() {
        return instance;
    }

    public synchronized static SharedPreferences getSharedPreferences() {
        if (sharedPreferences == null) {
            sharedPreferences = BroadrApp.getInstance().getApplicationContext().getSharedPreferences(Constants.SHARED_PREFS_KEY, Context.MODE_PRIVATE);
        }

        return sharedPreferences;
    }

    public synchronized static SharedPreferences getSettingsPreferences() {
        if (settingsPreferences == null) {
            settingsPreferences = PreferenceManager.getDefaultSharedPreferences(instance);
        }

        return settingsPreferences;
    }

    public synchronized static MessageRepo getMessageRepo() {
        if (messageRepo == null) {
            messageRepo = new MessageRepo(instance);
        }
        return messageRepo;
    }

    public synchronized static CommentRepo getCommentRepo() {
        if (commentRepo == null) {
            commentRepo = new CommentRepo(instance);
        }
        return commentRepo;
    }

    public synchronized static BoardRepo getBoardRepo() {
        if (boardRepo == null) {
            boardRepo = new BoardRepo(instance);
        }
        return boardRepo;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        RoboGuice.setUseAnnotationDatabases(false);
        generateDB();
        configureJobManager();
    }

    @Override
    public void onTerminate() {
        try {
            messageRepo.close();
            commentRepo.close();
            boardRepo.close();
            AppSingleton.deleteAllObjects();
        } catch (Exception e) {

        }
        super.onTerminate();
    }

    private void configureJobManager() {
        Configuration configuration = new Configuration.Builder(this)
                .minConsumerCount(1)// always keep at least one consumer alive
                .maxConsumerCount(3)// up to 3 consumers at a time
                .loadFactor(3)// 3 jobs per consumer
                .consumerKeepAlive(120)// wait 2 minutes
                .build();
        jobManager = new JobManager(this, configuration);
    }

    public JobManager getJobManager() {
        if (jobManager == null) {
            configureJobManager();
        }
        return jobManager;
    }

    public void generateDB() {
        DevOpenHelper dbHelper = new DaoMaster.DevOpenHelper(this,
                Constants.SQLITE_DB_NAME, null);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        DaoMaster.createAllTables(db, true);
        db.close();
        dbHelper.close();
    }

}
