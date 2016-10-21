package com.github.dgsc_fav.wheelytest.ui.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import java.util.Observable;
import java.util.Observer;

/**
 * Created by DG on 18.10.2016.
 */
public class MapsActivity extends PermissionsActivity implements OnMapReadyCallback, Observer, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, SocketService.ISocketServiceConnectionListener, SocketService.IMessageListener {

    protected final ServiceConnection mSocketServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((SocketService.MyBinder) service).getService();
            mIsBound = true;
            mService.setISocketServiceConnectionListener(MapsActivity.this);
            mService.setIMessageListener(MapsActivity.this);

            if(mService.isConnecting() || mService.isConnected()) {
                // если сокетное соединение есть

            } else {
                //
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mIsBound = false;
        }
    };
    private SocketService            mService;
    private boolean                  mIsBound;
    private Button                   mDisconnect;
    private View                     mInfo;
    private List<IdentifableLocation>     mLocations;
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

        mInfo = findViewById(R.id.textinfo);

        showSatelliteSearchInfo();

        checkLocationServicePermissions();
    }

    private void doDisconnect() {
        Toast.makeText(this, getString(R.string.disconnect_button_text), Toast.LENGTH_SHORT).show();

        mFusedLocationProviderApi.removeLocationUpdates(mGoogleApiClient, this);

        if(mIsBound) {
            mService.disconnect();
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

        fillMarkers(mLocations);

        // сперва зум, потом координаты, иначе промахнётся
        mMap.moveCamera(CameraUpdateFactory.zoomTo(zoomFactory));

        updateMapToMyLocation();

        //noinspection ResourceType
        mMap.setMyLocationEnabled(true);
    }

    private void updateMapToMyLocation() {
        if(mMyLocation != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(mMyLocation.getLatitude(),
                                                                        mMyLocation.getLongitude())));
            hideSatelliteSearchInfo();
        } else {
            showSatelliteSearchInfo();
        }
    }

    private void fillMarkers(@Nullable List<IdentifableLocation> locations) {
        mMap.clear();

        if(locations == null) {
            return;
        }

        for(int i = 0; i < locations.size(); i++) {
            IdentifableLocation identifableLocation = locations.get(i);
            // Add a marker in M and move the camera
            LatLng m = new LatLng(identifableLocation.getLatitude(), identifableLocation.getLongitude());

            mMap.addMarker(new MarkerOptions()
                                   .position(m)
                                   .title(String.valueOf(identifableLocation.getId())));
        }
    }

    private void showSatelliteSearchInfo() {
        mInfo.setVisibility(View.VISIBLE);
    }

    private void hideSatelliteSearchInfo() {
        mInfo.setVisibility(View.GONE);
    }

    @Override
    @Deprecated
    public void update(Observable o, Object arg) {
        if(arg instanceof List) {
            fillMarkers((List<IdentifableLocation>) arg);
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
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
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
        }
    }

    @Override
    public void onSocketServiceConnected() {

    }

    @Override
    public void onSocketServiceError(String error) {

    }

    @Override
    public void onSocketServiceDisconnect(String msg, int reason) {

        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public void onMessage(String msg) {
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

    // // TODO: 20.10.2016 добавить кнопочку на экран, рядом с кнопочкой Где я
    // и пусть fillMarker bounds вычисляет
    /**
     * Наверное, надо сделать так, чтобы маркеры были видны на экране
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

        LatLngBounds latLngBounds = new LatLngBounds(new LatLng(minLat, minLon), new LatLng(maxLat,
                                                                                         maxLon));
        // Set the camera to the greatest possible zoom level that includes the
        // bounds
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, getResources().getDimensionPixelSize(R.dimen.map_bounds_padding)));
    }
}
