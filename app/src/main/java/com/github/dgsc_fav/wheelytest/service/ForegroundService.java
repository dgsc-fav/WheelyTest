package com.github.dgsc_fav.wheelytest.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import com.github.dgsc_fav.wheelytest.R;
import com.github.dgsc_fav.wheelytest.ui.activity.LoginActivity;

/**
 * Created by DG on 19.10.2016.
 */
public abstract class ForegroundService extends Service implements IntentConsts {

    private static final int DEFAULT_NOTIFICATION_ID = 64;

    private NotificationManager mNotificationManager;
    private int                 mNotificationId;

    @Override
    public void onCreate() {
        super.onCreate();

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean mIsForeground = true;

        if(intent != null) {
            mIsForeground = intent.getBooleanExtra(KEY_FOREGROUND, false);
            String mTicker = intent.getStringExtra(KEY_TICKER);
            String mTitle = intent.getStringExtra(KEY_TITLE);
            String mContent = intent.getStringExtra(KEY_CONTENT);
            mNotificationId = intent.getIntExtra(KEY_ID, DEFAULT_NOTIFICATION_ID);

            // foreground notification
            sendNotification(mTicker, mTitle, mContent);
        }

        serviceTask();

        // если в intent как foreground, то с перезапуском, иначе ждать, когда запустят "вручную"
        return mIsForeground ? START_REDELIVER_INTENT : START_NOT_STICKY;
    }

    protected abstract void serviceTask();

    protected void sendNotification(String ticker, String title, String text) {
        Intent notificationIntent = new Intent(this, LoginActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                                                                notificationIntent,
                                                                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentIntent(contentIntent)
               .setOngoing(true)
               .setAutoCancel(false)
               .setSmallIcon(R.mipmap.ic_launcher)
               .setTicker(ticker)
               .setContentTitle(title)
               .setContentText(text)
               .setWhen(System.currentTimeMillis());

        Notification notification;
        if(android.os.Build.VERSION.SDK_INT <= 15) {
            //noinspection deprecation
            notification = builder.getNotification();
        } else {
            notification = builder.build();
        }

        startForeground(mNotificationId, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // удаление notification
        mNotificationManager.cancel(mNotificationId);

        // останов сервиса
        stopSelf();
    }
}
