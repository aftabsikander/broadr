package com.abbiya.broadr.utility;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;

import com.path.android.jobqueue.network.NetworkEventProvider;
import com.path.android.jobqueue.network.NetworkUtil;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PingNetworkUtil implements NetworkUtil, NetworkEventProvider {
    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private Listener listener;
    private boolean isConnected;

    public PingNetworkUtil(Context context) {
        context.getApplicationContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                checkConnection();
            }
        }, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        checkConnection();
    }

    @Override
    public boolean isConnected(Context context) {
        return isConnected;
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void notifyListener() {
        HANDLER.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null)
                    listener.onNetworkChange(isConnected);
            }
        });
    }

    private void checkConnection() {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                isConnected = ping();
                notifyListener();
            }
        });
    }

    private boolean ping() {
        Runtime runtime = Runtime.getRuntime();
        try {

            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int exitValue = ipProcess.waitFor();
            return (exitValue == 0);

        } catch (IOException | InterruptedException ignored) {
        }

        return false;
    }
}
