package com.github.dgsc_fav.wheelytest.ui.activity;

import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.dgsc_fav.wheelytest.R;
import com.github.dgsc_fav.wheelytest.api.model.SimpleLocation;
import com.github.dgsc_fav.wheelytest.provider.DataManager;
import com.github.dgsc_fav.wheelytest.provider.IDataProvider;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by DG on 18.10.2016.
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, Observer {

    public static final String EXTRA_KEY_LOCATIONS   = "locations";
    public static final String EXTRA_KEY_MY_LOCATION = "my_location";

    private IDataProvider        mDataProvider;
    private GoogleMap            mMap;
    private List<SimpleLocation> mLocations;
    private Location             mMyLocation;
    private Button               mDisconnect;

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
    }

    @Override
    public void onStart() {
        super.onStart();

        // todo подписаться на события о новых маркерах

        DataManager.getInstance(this).addObserver(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        // todo отписаться от событий о новых маркерах

        DataManager.getInstance(this).deleteObserver(this);
    }

    private void doDisconnect() {
        Toast.makeText(this, getString(R.string.disconnect_button_text), Toast.LENGTH_SHORT).show();
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

        if(mLocations == null) {
            mLocations = new ArrayList<>();
            // отправим на бермуды, если открыли активити локации
            SimpleLocation location = new SimpleLocation();
            location.setId(0);
            location.setLatitude((float) 26.629167);
            location.setLongitude((float) -70.883611);
            mLocations.add(location);
            zoomFactory = 5;
        }

        mMap = googleMap;

        fillMarkers(mLocations);

        // сперва зум, потом координаты, иначе промахнётся
        mMap.moveCamera(CameraUpdateFactory.zoomTo(zoomFactory));
        // потом координаты, иначе промахнётся
        if(mMyLocation != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(
                    new LatLng(mMyLocation.getLatitude(), mMyLocation.getLongitude())));
        }

        //mMap.setMyLocationEnabled(true);
    }

    private void fillMarkers(List<SimpleLocation> locations) {
        mMap.clear();

        for(int i = 0; i < locations.size(); i++) {
            SimpleLocation simpleLocation = locations.get(i);
            // Add a marker in M and move the camera
            LatLng m = new LatLng(simpleLocation.getLatitude(), simpleLocation.getLongitude());

            mMap.addMarker(
                    new MarkerOptions().position(m).title(String.valueOf(simpleLocation.getId())));
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if(arg instanceof List) {
            fillMarkers((List<SimpleLocation>) arg);
        }
    }
}
