package com.abbiya.broadr.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.abbiya.broadr.R;
import com.abbiya.broadr.events.AnyoneActiveEvent;
import com.abbiya.broadr.events.IAMActiveEvent;
import com.abbiya.broadr.events.RegisteredGCMEvent;
import com.abbiya.broadr.events.RegisteringGCMEvent;
import com.abbiya.broadr.events.RegisteringGCMFailedEvent;
import com.abbiya.broadr.events.SentRegistrationMessageEvent;
import com.abbiya.broadr.jobs.RegisterGCMJob;
import com.abbiya.broadr.jobs.SendRegistrationMessageJob;
import com.abbiya.broadr.service.NotificationService;
import com.abbiya.broadr.utility.AppSingleton;
import com.abbiya.broadr.utility.Constants;

import java.util.UUID;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;

@ContentView(R.layout.activity_bootup)
public class BootupActivity extends BaseActivity {

    @InjectView(R.id.progressBarTaskStatus)
    private ProgressBar progressBarTaskStatus;

    @InjectView(R.id.tvTaskStatus)
    private TextView tvTaskStatus;

    @InjectView(R.id.btnContinue)
    private Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

        if (notifInterval > 1) {
            startService(new Intent(context, NotificationService.class));
        }

        getSupportActionBar().hide();

        EventBus.getDefault().register(this);

        if (!AppSingleton.isNetworkAvailable(context)) {
            tvTaskStatus.setText(getString(R.string.network_not_available));
            tvTaskStatus.setTextColor(Color.RED);
        }

        btnContinue.setEnabled(false);
        tvTaskStatus.setText(getString(R.string.booting_up));
        tvTaskStatus.setTextColor(Color.WHITE);

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moveToMainActivity();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        //play services check
        if (checkPlayServices()) {
            String regId = getRegistrationId(context);
            if (((regId == null || regId.isEmpty()) && !locationEnabled())) {
                tvTaskStatus.setText(getString(R.string.need_location));
                tvTaskStatus.setTextColor(Color.RED);
                turnOnLocation();
            } else {
                tvTaskStatus.setText(getString(R.string.got_location));
                tvTaskStatus.setTextColor(Color.WHITE);
                addAndSendRegistration();
            }

        } else {
            //do something here
            tvTaskStatus.setText(getString(R.string.need_play_services));
            tvTaskStatus.setTextColor(Color.RED);
            Toast.makeText(context, getString(R.string.need_play_services), Toast.LENGTH_LONG).show();
            //finish();
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.LOCATION_SETTINGS:
                switch (resultCode) {
                    case RESULT_OK:
                        addAndSendRegistration();
                        break;
                }
                break;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(RegisteringGCMEvent event) {
        tvTaskStatus.setText(getString(R.string.tech_stuff));
        tvTaskStatus.setTextColor(Color.WHITE);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(RegisteredGCMEvent event) {
        tvTaskStatus.setText(getString(R.string.tech_stuff));
        tvTaskStatus.setTextColor(Color.WHITE);
        storeRegistrationId(context, event.getRegId());
        sendRegistrationMessage();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(SentRegistrationMessageEvent event) {
        tvTaskStatus.setText(getString(R.string.tech_stuff));
        tvTaskStatus.setTextColor(Color.WHITE);
        mEditor.putBoolean(Constants.IS_REGISTRATION_MESSAGE_SENT, true);
        mEditor.commit();

        tvTaskStatus.setText(getString(R.string.touch_continue));
        progressBarTaskStatus.setVisibility(View.INVISIBLE);
        btnContinue.setEnabled(true);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(RegisteringGCMFailedEvent event) {
        tvTaskStatus.setText(getString(R.string.gcm_registration_failed));
        tvTaskStatus.setTextColor(Color.RED);
        sendRegistrationMessage();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(AnyoneActiveEvent event) {
        EventBus.getDefault().post(new IAMActiveEvent(BootupActivity.class.getCanonicalName()));
    }

    private void sendRegistrationMessage() {
        String email = findUserEmail();
        mEditor.putString(Constants.USER_EMAIL, email);
        mEditor.putBoolean(Constants.SENDING_GCM_REGISTRATION_MESSAGE, true);
        mEditor.commit();

        jobManager.addJobInBackground(new SendRegistrationMessageJob(email, UUID.randomUUID().toString()));
    }

    private String findUserEmail() {
        Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
        Account[] accounts = AccountManager.get(context).getAccountsByType("com.google");
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                return account.name;
            }
        }

        return "";
    }

    private void storeRegistrationId(Context context, String regId) {
        int appVersion = getAppVersion(context);
        mEditor.putString(Constants.PROPERTY_REG_ID, regId);
        mEditor.putInt(Constants.PROPERTY_APP_VERSION, appVersion);
        mEditor.commit();
    }

    private void moveToMainActivity() {
        Intent i = new Intent(BootupActivity.this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(i);
        finish();
        overridePendingTransition(0, 0);
    }

    private void addAndSendRegistration() {
        String regId = getRegistrationId(context);
        if (regId.isEmpty()) {
            jobManager.addJobInBackground(new RegisterGCMJob());

            mEditor.putBoolean(Constants.IS_GCM_REGISTATION_JOB_ADDED, true);
            mEditor.commit();
        } else {
            //if registration is sent then move to main activity
            if (mPrefs.getBoolean(Constants.IS_REGISTRATION_MESSAGE_SENT, false)) {
                moveToMainActivity();
            } else {
                sendRegistrationMessage();
            }
        }
    }

    private void turnOnLocation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.location_alert_title));
        builder.setMessage(getString(R.string.location_alert_message))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.location_alert_positive_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startLocationSettings();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

}
