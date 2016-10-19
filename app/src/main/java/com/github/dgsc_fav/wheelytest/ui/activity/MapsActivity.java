package com.github.dgsc_fav.wheelytest.ui.activity;

import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.dgsc_fav.wheelytest.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by DG on 18.10.2016.
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final String PARAMS_EXTRA_KEY = "city";
    private GoogleMap mMap;
    private Location  mLocation;
    private Button    mDisconnect;

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
            mLocation = (Location) getIntent().getParcelableExtra(PARAMS_EXTRA_KEY);
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
    }

    private void doDisconnect() {
        Toast.makeText(this, getString(R.string.disconnect_button_text), Toast.LENGTH_SHORT).show();
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


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        float zoomFactory = 15;

        if(mLocation == null) {
            // отправим на бермуды, если открыли активити локации
            mLocation = new Location(LocationManager.PASSIVE_PROVIDER);
            mLocation.setLatitude((float) 26.629167);
            mLocation.setLongitude((float) -70.883611);
            zoomFactory = 5;
        }

        mMap = googleMap;

        // Add a marker in M and move the camera
        LatLng m = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());

        mMap.addMarker(new MarkerOptions().position(m).title("1"));

        // сперва зум, потом координаты, иначе промахнётся
        mMap.moveCamera(CameraUpdateFactory.zoomTo(zoomFactory));
        // потом координаты, иначе промахнётся
        mMap.animateCamera(CameraUpdateFactory.newLatLng(m));
        //mMap.setMyLocationEnabled(true);
    }
}
