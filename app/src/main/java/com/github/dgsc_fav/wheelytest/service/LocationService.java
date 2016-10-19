package com.github.dgsc_fav.wheelytest.service;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
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
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
    private static final long MIN_TIME_BW_UPDATES             = 1000 * 60 * 1; // 1 minute

    private final ServiceStub mBinder = new ServiceStub(this);

    private boolean isGPSEnabled     = false;
    private boolean isNetworkEnabled = false;
    private boolean canGetLocation   = false;
    private Location        location;
    private double          latitude;
    private double          longitude;
    private LocationManager locationManager;
    private int             mServiceStartId;
    private String          mInstanceId;

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
        Log.i(TAG, "onCreate");
        mInstanceId = Long.toHexString(System.currentTimeMillis());
        getLocation();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;
        Log.i(TAG, "onStartCommand:" + mServiceStartId);
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
        if(locationManager != null) {
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
            locationManager.removeUpdates(this);
        }
    }

    public Location getLocation() {
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
            return null;
        }

        try {

            locationManager = (LocationManager) getApplicationContext().getSystemService(
                    LOCATION_SERVICE);
            if(locationManager != null) {
                // getting GPS status
                isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                // getting network status
                isNetworkEnabled = locationManager
                                           .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if(isGPSEnabled || isNetworkEnabled) {
                    this.canGetLocation = true;
                    if(isNetworkEnabled) {

                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                                                               MIN_TIME_BW_UPDATES,
                                                               MIN_DISTANCE_CHANGE_FOR_UPDATES,
                                                               this);

                        location = locationManager
                                           .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                        if(location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                    if(isGPSEnabled) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                                               MIN_TIME_BW_UPDATES,
                                                               MIN_DISTANCE_CHANGE_FOR_UPDATES,
                                                               this);

                        location = locationManager
                                           .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                        if(location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                } else {
                    locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,
                                                           MIN_TIME_BW_UPDATES,
                                                           MIN_DISTANCE_CHANGE_FOR_UPDATES,
                                                           this);

                    location = locationManager
                                       .getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

                    if(location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        return location;
    }

    @Override
    public void onLocationChanged(Location location) {

        if(ActivityCompat.checkSelfPermission(this,
                                              Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat
                                                                                                                                        .checkSelfPermission(
                                                                                                                                                this,
                                                                                                                                                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        this.location = location;
        Log.i(TAG, "onLocationChanged:" + location);

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
