package com.github.dgsc_fav.wheelytest.provider;

import android.location.Location;

import com.github.dgsc_fav.wheelytest.api.model.SimpleLocation;

import java.util.List;

/**
 * Created by DG on 19.10.2016.
 */
public interface IDataProvider {
    List<SimpleLocation> getSimpleLocations();

    Location getMyLocation();
}
