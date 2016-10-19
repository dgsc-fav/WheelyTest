package com.github.dgsc_fav.wheelytest.provider;

import android.content.Context;

import java.util.Observable;

/**
 * Created by DG on 19.10.2016.
 */
public final class DataManager extends Observable {

    private static volatile DataManager sInstance;

    private Context context;

    public DataManager(Context context) {
        this.context = context;
    }

    public static DataManager getInstance(Context context) {
        if(sInstance == null) {
            synchronized(DataManager.class) {
                if(sInstance == null) {
                    sInstance = new DataManager(context);
                }
            }
        }
        return sInstance;
    }
}
