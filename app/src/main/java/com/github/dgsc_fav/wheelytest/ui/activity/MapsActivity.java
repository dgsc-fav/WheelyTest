package com.github.dgsc_fav.wheelytest.ui.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.dgsc_fav.wheelytest.R;
import com.github.dgsc_fav.wheelytest.api.model.IdentifableLocation;
import com.github.dgsc_fav.wheelytest.service.LocationServiceConsts;
import com.github.dgsc_fav.wheelytest.service.ServiceHelper;
import com.github.dgsc_fav.wheelytest.service.SocketService;
import com.github.dgsc_fav.wheelytest.util.LocationUtils;
import com.github.dgsc_fav.wheelytest.util.NetUtils;
import com.github.dgsc_fav.wheelytest.ws.WebSocketClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by DG on 18.10.2016.
 */
public class MapsActivity extends PermissionsActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, WebSocketClient.ISocketServiceConnectionListener, WebSocketClient.IMessageListener {

    private final IntentFilter      mIntentFilter            = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    private final BroadcastReceiver mBroadcastReceiver       = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWaitingNetworkInfo(context);
        }
    };
    private final ServiceConnection mSocketServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((SocketService.MyBinder) service).getService();

            // зарегистрируемся слушателями WebSocketClient
            WebSocketClient mWebSocketClient = mService.getWebSocketClient();
            mWebSocketClient.setSocketServiceConnectionListener(MapsActivity.this);
            mWebSocketClient.setMessageListener(MapsActivity.this);

            if(!mWebSocketClient.isConnecting() && !mWebSocketClient.isConnected()) {
                // если соединения нет, то закрываем activity
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };
    private SocketService            mService;
    private boolean                  mIsBound;
    private Button                   mDisconnect;
    private View                     mWaitingSatellitesInfo;
    private View                     mWaitingNetworkInfo;
    private Location                 mMyLocation;
    private GoogleMap                mMap;
    private LocationRequest          mLocationRequest;
    private FusedLocationProviderApi mFusedLocationProviderApi;
    private GoogleApiClient          mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.title_activity_maps);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDisconnect = (Button) findViewById(R.id.disconnect);

        mDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doDisconnect();
            }
        });

        mWaitingSatellitesInfo = findViewById(R.id.waiting_satellites);
        mWaitingNetworkInfo = findViewById(R.id.waiting_network);

        showWaitingSatellitesInfo();

        checkLocationServicePermissions();
    }

    private void doDisconnect() {
        Toast.makeText(this, getString(R.string.disconnect_button_text), Toast.LENGTH_SHORT).show();

        mDisconnect.setEnabled(false);

        mFusedLocationProviderApi.removeLocationUpdates(mGoogleApiClient, this);

        if(mIsBound) {
            mService.getWebSocketClient().disconnect();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        float zoomFactory = 10;

        mMap = googleMap;

        // сперва зум, потом координаты, иначе промахнётся
        mMap.moveCamera(CameraUpdateFactory.zoomTo(zoomFactory));

        //noinspection ResourceType
        mMap.setMyLocationEnabled(true);
    }

    private void updateMapToMyLocation() {
        if(mMap != null && mMyLocation != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(mMyLocation.getLatitude(),
                                                                        mMyLocation.getLongitude())));
            hideWaitingSatellitesInfo();
        } else {
            showWaitingSatellitesInfo();
        }
    }

    private void showWaitingSatellitesInfo() {
        mWaitingSatellitesInfo.setVisibility(View.VISIBLE);
    }

    private void hideWaitingSatellitesInfo() {
        mWaitingSatellitesInfo.setVisibility(View.GONE);
    }

    private void showWaitingNetworkInfo() {
        mWaitingNetworkInfo.setVisibility(View.VISIBLE);
    }

    private void hideWaitingNetworkInfo() {
        mWaitingNetworkInfo.setVisibility(View.GONE);
    }

    private void updateWaitingNetworkInfo(Context context) {
        boolean isNetworkAvailable = NetUtils.isNetworkAvailable(context);
        if(isNetworkAvailable) {
            hideWaitingNetworkInfo();
        } else {
            showWaitingNetworkInfo();
        }
    }

    @Override
    public void processWithPermissionsGranted() {
        // permissions есть

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(
                R.id.map);
        mapFragment.getMapAsync(this);

        getLocation();
        if(!ServiceHelper.isSocketServiceRunning(this)) {
            // если сервис не запущен, то запускаем
            startSocketService();
            // и подключаемся. именно так, чтобы при unbindService он не останавливался
            bindSocketService();
        } else {
            bindSocketService();
        }
    }

    @Override
    public void processWithPermissionsDenied() {
        finishWithDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mGoogleApiClient != null && !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }

        updateWaitingNetworkInfo(this);

        // подпишемся на сообщения о смени статуса сети
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        // подпишемся на сообщения о смени статуса сети
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    private void getLocation() {
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
        mMyLocation = location;
        updateMapToMyLocation();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void startSocketService() {
        startService(SocketService.getIntent(this));
    }

    private void bindSocketService() {
        if(!mIsBound) {
            bindService(SocketService.getIntent(this), mSocketServiceConnection, BIND_AUTO_CREATE);
            mIsBound = true;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        bindSocketService();
    }

    @Override
    public void onStop() {
        super.onStop();

        if(mIsBound) {
            unbindService(mSocketServiceConnection);
            mIsBound = false;
        }
    }

    @Override
    public void onSocketServiceConnected() {

    }

    @Override
    public void onSocketServiceError(final Throwable throwable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MapsActivity.this,
                               throwable.getLocalizedMessage(),
                               Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onSocketServiceDisconnect(String msg, int reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(MapsActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    @Override
    public void onMessage(String msg) {
        if(mMap == null) {
            return;
        }
        Type listType = new TypeToken<ArrayList<IdentifableLocation>>() {
        }.getType();
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        final List<IdentifableLocation> identifableLocations = gson.fromJson(msg, listType);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fillMarkers(identifableLocations);
                updateCameraPosition(identifableLocations);
            }
        });

    }

    private void fillMarkers(@Nullable List<IdentifableLocation> locations) {
        mMap.clear();

        if(locations == null) {
            return;
        }

        for(int i = 0; i < locations.size(); i++) {
            IdentifableLocation identifableLocation = locations.get(i);
            // Add a marker in M and move the camera
            LatLng m = new LatLng(identifableLocation.getLatitude(),
                                  identifableLocation.getLongitude());

            mMap.addMarker(new MarkerOptions()
                                   .position(m)
                                   .title(String.valueOf(identifableLocation.getId())));
        }
    }

    /**
     * Чтобы маркеры были видны на экране
     *
     * @param identifableLocations
     */
    private void updateCameraPosition(@Nullable final List<IdentifableLocation> identifableLocations) {
        if(identifableLocations == null || identifableLocations.isEmpty()) {
            return;
        }

        double minLat = LocationUtils.getMinLat(identifableLocations);
        double minLon = LocationUtils.getMinLon(identifableLocations);
        double maxLat = LocationUtils.getMaxLat(identifableLocations);
        double maxLon = LocationUtils.getMaxLon(identifableLocations);

        LatLngBounds latLngBounds = new LatLngBounds(new LatLng(minLat, minLon),
                                                     new LatLng(maxLat, maxLon));

        // Set the camera to the greatest possible zoom level that includes the bounds
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds,
                                                               getResources().getDimensionPixelSize(
                                                                       R.dimen.map_bounds_padding)));
    }
}
