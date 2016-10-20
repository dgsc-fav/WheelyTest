package com.github.dgsc_fav.wheelytest.util;

import com.github.dgsc_fav.wheelytest.api.model.IdentifableLocation;

import java.util.List;

/**
 * Created by DG on 20.10.2016.
 */

public class LocationUtils {

    public static double getMinLat(List<IdentifableLocation> identifableLocations) {
        double minLat = Double.MAX_VALUE;

        for(IdentifableLocation identifableLocation : identifableLocations) {
            double lat = identifableLocation.getLatitude();
            if(minLat > lat) {
                minLat = lat;
            }
        }

        return minLat;
    }

    public static double getMinLon(List<IdentifableLocation> identifableLocations) {
        double minLon = Double.MAX_VALUE;

        for(IdentifableLocation identifableLocation : identifableLocations) {
            double lon = identifableLocation.getLongitude();
            if(minLon > lon) {
                minLon = lon;
            }
        }

        return minLon;
    }

    public static double getMaxLat(List<IdentifableLocation> identifableLocations) {
        double maxLat = Double.MIN_NORMAL;

        for(IdentifableLocation identifableLocation : identifableLocations) {
            double lat = identifableLocation.getLatitude();
            if(maxLat < lat) {
                maxLat = lat;
            }
        }

        return maxLat;
    }

    public static double getMaxLon(List<IdentifableLocation> identifableLocations) {
        double maxLon = Double.MIN_NORMAL;

        for(IdentifableLocation identifableLocation : identifableLocations) {
            double lon = identifableLocation.getLongitude();
            if(maxLon < lon) {
                maxLon = lon;
            }
        }

        return maxLon;
    }
}
