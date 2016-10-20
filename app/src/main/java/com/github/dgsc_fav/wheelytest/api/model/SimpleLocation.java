package com.github.dgsc_fav.wheelytest.api.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.dgsc_fav.wheelytest.api.Consts;
import com.google.gson.annotations.SerializedName;

/**
 * Created by DG on 19.10.2016.
 */
public class SimpleLocation implements Parcelable, Consts {
    @SerializedName(LATITUDE)
    protected double latitude;
    @SerializedName(LONGITUDE)
    protected double longitude;

    public SimpleLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    protected SimpleLocation(Parcel in) {
        latitude = in.readFloat();
        longitude = in.readFloat();
    }

    public static final Creator<SimpleLocation> CREATOR = new Creator<SimpleLocation>() {
        @Override
        public SimpleLocation createFromParcel(Parcel in) {
            return new SimpleLocation(in);
        }

        @Override
        public SimpleLocation[] newArray(int size) {
            return new SimpleLocation[size];
        }
    };

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
    }
}
