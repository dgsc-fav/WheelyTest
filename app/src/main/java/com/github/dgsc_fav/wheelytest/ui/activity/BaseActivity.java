package com.github.dgsc_fav.wheelytest.ui.activity;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.github.dgsc_fav.wheelytest.service.LocationService;
import com.github.dgsc_fav.wheelytest.service.ServiceHelper;

/**
 * Created by DG on 19.10.2016.
 */
public abstract class BaseActivity extends PermissionsActivity {

    public static final int REQUEST_CODE_OBSERVE_LOCATION       = 101;
    public static final int REQUEST_CODE_OBSERVE_MARKERS        = 102;

    // через него LocationService будет передавать обновлённые координаты
    protected PendingIntent mLocationServicePendingResult;

    // соединение с LocationService
    protected final ServiceConnection mLocationServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((LocationService.MyBinder) service).getService();
            mIsBound = true;

            // подписаться на обновления локации
            mService.addPendingIntent(mLocationServicePendingResult);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mIsBound = false;
        }
    };

    protected LocationService mService;
    protected boolean         mIsBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocationServicePendingResult = createPendingResult(REQUEST_CODE_OBSERVE_LOCATION,
                                                            new Intent(),
                                                            PendingIntent.FLAG_UPDATE_CURRENT);

        if(!ServiceHelper.isLocationServiceRunning(this)) {
            startLocationService();
        } else {
            bindLocationService();
        }
    }

    private void startLocationService() {
        startService(LocationService.getIntent(this));
        bindLocationService();
    }

    private void bindLocationService() {
        if(!mIsBound) {
            bindService(LocationService.getIntent(this),
                        mLocationServiceConnection,
                        BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // подписаться на обновления локации
        if(mIsBound) {
            mService.addPendingIntent(mLocationServicePendingResult);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // отписаться от обновлений локации
        if(mIsBound) {
            mService.removePendingIntent(mLocationServicePendingResult);
            unbindService(mLocationServiceConnection);
        }
    }
}
