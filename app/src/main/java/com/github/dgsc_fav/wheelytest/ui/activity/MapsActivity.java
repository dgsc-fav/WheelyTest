package com.github.dgsc_fav.wheelytest.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.dgsc_fav.wheelytest.R;
import com.github.dgsc_fav.wheelytest.api.model.SimpleLocation;
import com.github.dgsc_fav.wheelytest.service.IntentConsts;
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
public class MapsActivity extends BaseActivity implements OnMapReadyCallback, Observer {

    @Deprecated
    public static final String EXTRA_KEY_LOCATIONS   = "locations";
    @Deprecated
    public static final String EXTRA_KEY_MY_LOCATION = "my_location";

    private GoogleMap            mMap;
    private List<SimpleLocation> mLocations;
    private Location             mMyLocation;
    private Button               mDisconnect;
    private View               mInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.title_activity_maps);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(getIntent() != null) {
            mLocations = getIntent().getParcelableArrayListExtra(EXTRA_KEY_LOCATIONS);
            mMyLocation = getIntent().getParcelableExtra(EXTRA_KEY_MY_LOCATION);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                                                      .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mDisconnect = (Button) findViewById(R.id.disconnect);

        mDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doDisconnect();
            }
        });

        mInfo = findViewById(R.id.textinfo);

        showSatelliteSearchInfo();
    }

    private void doDisconnect() {
        Toast.makeText(this, getString(R.string.disconnect_button_text), Toast.LENGTH_SHORT).show();

        if(mIsBound) {
            mService.stopSelf();
        }
        // // TODO: 19.10.2016 в логин
        onBackPressed();
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

        // потом координаты, иначе промахнётся
        if(mIsBound) {
            mMyLocation = mService.getLocation();
        }

        updateMyLocation();

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
        mMap.setMyLocationEnabled(true);
    }

    private void updateMyLocation() {
        if(mMyLocation != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(mMyLocation.getLatitude(),
                                                                        mMyLocation
                                                                                .getLongitude())));
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

            mMap.addMarker(new MarkerOptions().position(m)
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("LOG_TAG", "requestCode = " + requestCode + ", resultCode = " + resultCode);

        if(requestCode == REQUEST_CODE_OBSERVE_LOCATION && resultCode == RESULT_OK) {
            if(data != null && data.getExtras() != null) {
                mMyLocation = data.getExtras().getParcelable(IntentConsts.KEY_LOCATION);
                updateMyLocation();
            }
        } else if(requestCode == REQUEST_CODE_OBSERVE_MARKERS && resultCode == RESULT_OK) {
            if(data != null && data.getExtras() != null) {
                List<SimpleLocation> markers = data.getExtras().getParcelableArrayList(IntentConsts.KEY_MARKERS);
                fillMarkers(markers);
            }
        }
    }
}
