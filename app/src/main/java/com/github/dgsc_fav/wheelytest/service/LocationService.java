package com.github.dgsc_fav.wheelytest.service;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by DG on 19.10.2016.
 */
public class LocationService extends ForegroundService {
    private static final String TAG = LocationService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 933;

    private final IBinder mBinder = new ServiceStub(this);

    private int    mServiceStartId;
    private String mInstanceId;

    public static Intent getIntent(Context context) {
        Intent intent = new Intent(context, LocationService.class);
        intent.putExtra(KEY_FOREGROUND, true); // неубиваемый сервис
        intent.putExtra(KEY_TICKER, "LocationService.TICKER");
        intent.putExtra(KEY_TITLE, "LocationService.TITLE");
        intent.putExtra(KEY_CONTENT, "LocationService.CONTENT");
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
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void serviceTask() {
        TimerTask heartBeatTask = new TimerTask() {
            @Override
            public void run() {
                Log.i(TAG, mInstanceId + ": " + mServiceStartId + " ***I'm  Unstoppable Today!***");
            }
        };
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(heartBeatTask, 0, 15000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.e(TAG, mInstanceId + ": " + mServiceStartId + " ***Oops, destroyed***");
    }

    public Location getLocation() {
        return null;
    }

    static class ServiceStub extends ILocationAidlInterface.Stub {

        WeakReference<LocationService> mService;

        ServiceStub(LocationService locationService) {
            mService = new WeakReference<>(locationService);
        }

        @Override
        public Location getLocation() throws RemoteException {
            return mService.get().getLocation();
        }
    }
}
