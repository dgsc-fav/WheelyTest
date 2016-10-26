package com.github.dgsc_fav.wheelytest.service;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.github.dgsc_fav.wheelytest.api.ApiConsts;
import com.github.dgsc_fav.wheelytest.api.model.SimpleLocation;
import com.github.dgsc_fav.wheelytest.util.NetUtils;
import com.github.dgsc_fav.wheelytest.ws.WebSocketClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.lang.ref.WeakReference;

/**
 * Created by DG on 19.10.2016.
 */
public class SocketService extends ForegroundService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, ApiConsts {


    public static final int MANUAL   = 0;
    public static final int ON_ERROR = 1;

    private static final int NOTIFICATION_ID = 101;

    private final IBinder           mBinder            = new MyBinder(this);
    private final WebSocketClient   mWebSocketClient   = new WebSocketClient();
    private final IntentFilter      mIntentFilter      = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isNetworkAvailable = NetUtils.isNetworkAvailable(context);
            // todo возможно, лишний метод
            mWebSocketClient.onNetworkAvailableChanged(isNetworkAvailable);
        }
    };

    private LocationRequest          mLocationRequest;
    private FusedLocationProviderApi mFusedLocationProviderApi;
    private GoogleApiClient          mGoogleApiClient;

    public static Intent getIntent(Context context) {
        Intent intent = new Intent(context, SocketService.class);
        intent.putExtra(KEY_FOREGROUND, true); // неубиваемый сервис
        intent.putExtra(KEY_TICKER, "SocketService.TICKER");
        intent.putExtra(KEY_TITLE, "SocketService.TITLE");
        intent.putExtra(KEY_CONTENT, "SocketService.CONTENT");
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

        // подпишемся на сообщения о смени статуса сети
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, mIntentFilter);

        startHandleLocation();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        // подпишемся на сообщения о смени статуса сети
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);

        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        mWebSocketClient.disconnect();

        super.onDestroy();
    }

    private void startHandleLocation() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(LocationServiceConsts.POLLING_FREQ);
        mLocationRequest.setFastestInterval(LocationServiceConsts.FASTEST_UPDATE_FREQ);

        mFusedLocationProviderApi = LocationServices.FusedLocationApi;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                                   .addApi(LocationServices.API)
                                   .addConnectionCallbacks(this)
                                   .addOnConnectionFailedListener(this)
                                   .build();
        mGoogleApiClient.connect();
    }

    @NonNull
    public WebSocketClient getWebSocketClient() {
        return mWebSocketClient;
    }

    @Override
    public void onConnected(Bundle arg0) {
        //noinspection ResourceType
        mFusedLocationProviderApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if(location != null) {
            // send
            SimpleLocation simpleLocation = new SimpleLocation(location.getLatitude(),
                                                               location.getLongitude());
            String msg = simpleLocation.toJson();

            mWebSocketClient.sendText(msg);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

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
