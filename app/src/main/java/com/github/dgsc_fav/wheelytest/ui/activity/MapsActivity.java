package com.github.dgsc_fav.wheelytest.ui.activity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.dgsc_fav.wheelytest.R;
import com.github.dgsc_fav.wheelytest.api.model.SimpleLocation;
import com.github.dgsc_fav.wheelytest.service.LocationServiceConsts;
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
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by DG on 18.10.2016.
 */
public class MapsActivity extends PermissionsActivity implements OnMapReadyCallback, Observer, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private Button                   mDisconnect;
    private View                     mInfo;
    private List<SimpleLocation>     mLocations;
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

        startActivity(new Intent(this, LoginActivity.class));
        finish();
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
        float zoomFactory = 15;

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

    private void fillMarkers(@Nullable List<SimpleLocation> locations) {
        mMap.clear();

        if(locations == null) {
            return;
        }

        for(int i = 0; i < locations.size(); i++) {
            SimpleLocation simpleLocation = locations.get(i);
            // Add a marker in M and move the camera
            LatLng m = new LatLng(simpleLocation.getLatitude(), simpleLocation.getLongitude());

            mMap.addMarker(new MarkerOptions()
                                   .position(m)
                                   .title(String.valueOf(simpleLocation.getId())));
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
            fillMarkers((List<SimpleLocation>) arg);
        }
    }

    @Override
    public void processWithPermissionsGranted() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(
                R.id.map);
        mapFragment.getMapAsync(this);

        getLocation();
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
}
