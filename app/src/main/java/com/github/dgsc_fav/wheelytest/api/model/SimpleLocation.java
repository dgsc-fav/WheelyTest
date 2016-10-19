package com.github.dgsc_fav.wheelytest.api.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.dgsc_fav.wheelytest.api.Consts;
import com.google.gson.annotations.SerializedName;

/**
 * Created by DG on 19.10.2016.
 */
public class SimpleLocation implements Parcelable, Consts {
    @SerializedName(ID)
    private int   id;
    @SerializedName(LATITUDE)
    private float latitude;
    @SerializedName(LONGITUDE)
    private float longitude;

    public SimpleLocation() {

    }

    protected SimpleLocation(Parcel in) {
        id = in.readInt();
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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeFloat(latitude);
        dest.writeFloat(longitude);
    }
}
