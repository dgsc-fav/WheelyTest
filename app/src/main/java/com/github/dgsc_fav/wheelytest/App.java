package com.github.dgsc_fav.wheelytest;

import android.support.multidex.MultiDexApplication;

import com.github.dgsc_fav.wheelytest.util.TokenStore;

/**
 * Created by DG on 20.10.2016.
 */

public class App extends MultiDexApplication {

    private static TokenStore sTokenStore;

    @Override
    public void onCreate() {
        super.onCreate();

        sTokenStore = new TokenStore(this);
    }

    public static TokenStore getTokenStore() {
        return sTokenStore;
    }
}
