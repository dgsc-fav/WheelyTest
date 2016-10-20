package com.github.dgsc_fav.wheelytest.service;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Created by DG on 19.10.2016.
 */
public class LocationService extends ForegroundService implements LocationListener {
    private static final String TAG = LocationService.class.getSimpleName();

    private static final int  NOTIFICATION_ID                 = 933;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 1 meters
    private static final long MIN_TIME_BW_UPDATES             = 1000; // 1 second

    private final IBinder mBinder = new MyBinder(this);

    private Location        mLocation;
    private LocationManager mLocationManager;
    @Deprecated
    private int             mServiceStartId; // только для теста
    @Deprecated
    private String          mInstanceId;// только для теста

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
        initialize();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void serviceTask() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopUsingGPS();

        Log.e(TAG, mInstanceId + ": " + mServiceStartId + " ***Oops, destroyed***");
    }

    public void stopUsingGPS() {
        if(mLocationManager != null) {
            if(ActivityCompat.checkSelfPermission(this,
                                                  Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat
                                                                                                                                            .checkSelfPermission(
                                                                                                                                                    this,
                                                                                                                                                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mLocationManager.removeUpdates(this);
        }
    }

    public void initialize() {
        if(ActivityCompat.checkSelfPermission(this,
                                              Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat
                                                                                                                                        .checkSelfPermission(
                                                                                                                                                this,
                                                                                                                                                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        try {

            mLocationManager = (LocationManager) getApplicationContext().getSystemService(
                    LOCATION_SERVICE);
            if(mLocationManager != null) {
                // getting GPS status
                boolean isGPSEnabled = mLocationManager
                                               .isProviderEnabled(LocationManager.GPS_PROVIDER);
                // getting network status
                boolean isNetworkEnabled = mLocationManager
                                                   .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if(isGPSEnabled || isNetworkEnabled) {

                    if(isNetworkEnabled) {

                        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                                                                MIN_TIME_BW_UPDATES,
                                                                MIN_DISTANCE_CHANGE_FOR_UPDATES,
                                                                this);

                        mLocation = mLocationManager
                                            .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                    if(isGPSEnabled) {
                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                                                MIN_TIME_BW_UPDATES,
                                                                MIN_DISTANCE_CHANGE_FOR_UPDATES,
                                                                this);

                        mLocation = mLocationManager
                                            .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                } else {
                    mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,
                                                            MIN_TIME_BW_UPDATES,
                                                            MIN_DISTANCE_CHANGE_FOR_UPDATES,
                                                            this);

                    mLocation = mLocationManager
                                        .getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        if(mLocation != null) {
            onLocationChanged(mLocation);
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        this.mLocation = location;
        Log.d("LOG_TAG", "onLocationChanged = " + location);
        if(!mPendingIntents.isEmpty()) {
            sendLocationToPendingIntent(location);
        }
    }

    /**
     * Передача координат
     * @see com.github.dgsc_fav.wheelytest.ui.activity.MapsActivity#onActivityResult(int, int, Intent)
     * @param location
     */
    private synchronized void sendLocationToPendingIntent(Location location) {
        Intent intent = new Intent();
        intent.putExtra(KEY_LOCATION, location);

        for(PendingIntent pendingIntent : mPendingIntents) {
            try {
                pendingIntent.send(this, Activity.RESULT_OK, intent);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public boolean addPendingIntent(PendingIntent pendingIntent) {
        boolean result = super.addPendingIntent(pendingIntent);
        // если присоединили посредника и есть координаты
        if(!mPendingIntents.isEmpty() && mLocation != null) {
            // передадим их
            sendLocationToPendingIntent(mLocation);
        }
        return result;
    }

    public Location getLocation() {
        return mLocation;
    }

    public static class MyBinder extends Binder {
        WeakReference<LocationService> mService;

        MyBinder(LocationService locationService) {
            mService = new WeakReference<>(locationService);
        }

        public LocationService getService() {
            return mService.get();
        }
    }
}
