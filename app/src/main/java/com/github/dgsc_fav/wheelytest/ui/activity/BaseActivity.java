package com.github.dgsc_fav.wheelytest.ui.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.github.dgsc_fav.wheelytest.R;
import com.github.dgsc_fav.wheelytest.service.ILocationAidlInterface;
import com.github.dgsc_fav.wheelytest.service.LocationService;
import com.github.dgsc_fav.wheelytest.service.ServiceHelper;

/**
 * Created by DG on 19.10.2016.
 */

public abstract class BaseActivity extends AppCompatActivity {
    public static final int PERMISSIONS_REQUEST_ACCESS_LOCATION = 100;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ILocationAidlInterface.Stub.asInterface(service);

            mIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mIsBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!ServiceHelper.isLocationServiceRunning(this)) {
            startLocationServiceWrapper();
        } else {
            bindLocationService();
        }
    }

    public void startLocationServiceWrapper() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Display UI and wait for user interaction
                showMessageOKCancel(R.string.dialog_location_permission_message, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestLocationPermission();
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startLocationService();
                    }
                });
            } else {
                requestLocationPermission();
            }
        } else {
            startLocationService();
        }
    }

    private void startLocationService() {
        startService(LocationService.getIntent(this));
        bindLocationService();
    }

    protected ILocationAidlInterface mService;
    protected boolean                mIsBound;

    private void bindLocationService() {
        if(!mIsBound) {

            bindService(LocationService.getIntent(this), serviceConnection, BIND_AUTO_CREATE);
        }
    }

    public void onStop() {
        super.onStop();

        if(mIsBound) {
            unbindService(serviceConnection);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PERMISSIONS_REQUEST_ACCESS_LOCATION) {
            if(grantResults.length == 2 &&
                       grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                       grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // success!
                startLocationService();
            } else {
                // Permission was denied or request was cancelled
                Toast.makeText(this, getString(R.string.permission_location_denied), Toast.LENGTH_SHORT).show();
            }

            return;
        }

        // обработка фрагментами. если они есть
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void showMessageOKCancel(int messageId, DialogInterface.OnClickListener okListener, DialogInterface.OnClickListener cancelListener) {
        new AlertDialog.Builder(this).setMessage(messageId).setPositiveButton(android.R.string.ok,
                                                                              okListener).setNegativeButton(android.R.string.cancel, cancelListener)
                                     .create().show();
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // Setting Icon to Dialog
        //alertDialog.setIcon(R.drawable.delete);

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    public void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                                          new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                          PERMISSIONS_REQUEST_ACCESS_LOCATION);
    }
}
