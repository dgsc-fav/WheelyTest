package com.github.dgsc_fav.wheelytest.api.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.dgsc_fav.wheelytest.api.ApiConsts;
import com.google.gson.annotations.SerializedName;

/**
 * Created by DG on 19.10.2016.
 */
public class IdentifableLocation extends SimpleLocation implements Parcelable, ApiConsts {
    public static final Creator<IdentifableLocation> CREATOR = new Creator<IdentifableLocation>() {
        @Override
        public IdentifableLocation createFromParcel(Parcel in) {
            return new IdentifableLocation(in);
        }

        @Override
        public IdentifableLocation[] newArray(int size) {
            return new IdentifableLocation[size];
        }
    };
    @SerializedName(ID)
    protected int id;

    public IdentifableLocation(int id, float latitude, float longitude) {
        super(latitude, longitude);
    }

    protected IdentifableLocation(Parcel in) {
        super(in);
        id = in.readInt();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(id);
    }
}
