package com.github.dgsc_fav.wheelytest.service;


import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by DG on 19.10.2016.
 */
public class SocketService extends ForegroundService {
    private static final String TAG = SocketService.class.getSimpleName();

    public void requestStopSelf() {

    }

    public interface ISocketServiceConnectionListener {
        void onSocketServiceConnected();

        void onSocketServiceError(String error);

        void onSocketServiceDisconnect(String msg, int reason);
    }

    private static final int NOTIFICATION_ID = 101;

    private final IBinder mBinder = new MyBinder(this);

    @Deprecated
    private int    mServiceStartId; // только для теста
    @Deprecated
    private String mInstanceId;// только для теста

    String mUsername;
    String mPassword;
    private boolean connected;
    ISocketServiceConnectionListener mConnectionListener;

    public static Intent getIntent(Context context/*, String username, String password*/) {
        Intent intent = new Intent(context, SocketService.class);
        intent.putExtra(KEY_FOREGROUND, true); // неубиваемый сервис
        intent.putExtra(KEY_TICKER, "SocketService.TICKER");
        intent.putExtra(KEY_TITLE, "SocketService.TITLE");
        intent.putExtra(KEY_CONTENT, "SocketService.CONTENT");
        //        intent.putExtra(KEY_USERNAME, username);
        //        intent.putExtra(KEY_PASSWORD, password);
        intent.putExtra(KEY_ID, NOTIFICATION_ID);
        return intent;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstanceId = Long.toHexString(System.currentTimeMillis());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;
        Log.d(TAG, mInstanceId + ": " + "onStartCommand");

        //        if(intent != null) {
        //            mUsername = intent.getStringExtra(KEY_USERNAME);
        //            mPassword = intent.getStringExtra(KEY_TICKER);
        //        }
        //        Log.d(TAG, " onStartCommand: " + mUsername + "|" + mPassword);
        //        if(TextUtils.isEmpty(mUsername) || TextUtils.isEmpty(mPassword)) {
        //            stopSelf();
        //            return START_STICKY_COMPATIBILITY;
        //        }

        return super.onStartCommand(intent, flags, startId);
    }

    TimerTask heartBeatTask;
    Timer     timer;

    protected void serviceTask() {
        Log.w(TAG, mInstanceId + ": " + "serviceTask");
        heartBeatTask = new TimerTask() {
            @Override
            public void run() {
                Log.i(TAG,
                      mInstanceId + ": " + mServiceStartId + " ***RUNNING*** " + connected + ":" + mUsername + "|" + mPassword);
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(heartBeatTask, 0, 15000);
    }

    private void stopServiceTask() {
        Log.w(TAG, mInstanceId + ": " + "stopServiceTask");
        try {

            if(timer != null) {
                timer.cancel();
                timer = null;
            }
            if(heartBeatTask != null) {
                heartBeatTask.cancel();
                heartBeatTask = null;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopServiceTask();

        Log.e(TAG, mInstanceId + ": " + mServiceStartId + " ***Oops, destroyed***");
    }

    public List getPlaces() {
        return null;
    }

    public static final int MANUAL   = 0;
    public static final int ON_ERROR = 1;
    private boolean requestDisconnect;

    public boolean isConnected() {
        return connected;
    }

    public void connect(String username, String password, ISocketServiceConnectionListener iSocketServiceConnectionListener) {
        mUsername = username;
        mPassword = password;
        setISocketServiceConnectionListener(iSocketServiceConnectionListener);

        // blah-blah-blah

        connected = !TextUtils.isEmpty(mUsername) && !TextUtils.isEmpty(mPassword);
        requestDisconnect = false;

        if(connected) {
            serviceTask();
        } else {
            stopServiceTask();
        }

        if(mConnectionListener != null) {
            if(connected) {
                mConnectionListener.onSocketServiceConnected();
            } else {
                mConnectionListener.onSocketServiceError("bad credientals");
            }
        }
    }

    public void setISocketServiceConnectionListener(ISocketServiceConnectionListener iSocketServiceConnectionListener) {
        mConnectionListener = iSocketServiceConnectionListener;
    }

    public void disconnect() {
        requestDisconnect = true;


        // blah-blah-blah
        connected = false;

        if(connected) {
            serviceTask();
        } else {
            stopServiceTask();
        }

        if(mConnectionListener != null) {
            mConnectionListener.onSocketServiceDisconnect("blah-blah-blah", getDisconnectReason());
        }
    }

    public int getDisconnectReason() {
        return requestDisconnect ? MANUAL : ON_ERROR;
    }

    public static class MyBinder extends Binder {
        WeakReference<SocketService> mService;

        MyBinder(SocketService socketService) {
            mService = new WeakReference<>(socketService);
        }

        public SocketService getService() {
            return mService.get();
        }
    }
}
