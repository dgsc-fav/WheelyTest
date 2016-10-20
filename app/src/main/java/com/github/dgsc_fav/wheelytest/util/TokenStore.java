package com.github.dgsc_fav.wheelytest.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by DG on 20.10.2016.
 */

public class TokenStore {
    private static final String PREF_NAME = "name";
    private static final String PREF_PSW = "psw";

    private SharedPreferences mSharedPreferences;

    public TokenStore(Context context) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getLastUsername() {
        return mSharedPreferences.getString(PREF_NAME, "");
    }

    public String getLastPassword() {
        return mSharedPreferences.getString(PREF_PSW, "");
    }

    public void setLastUsername(String username) {
        mSharedPreferences.edit().putString(PREF_NAME, username).apply();
    }

    public void setLastPassword(String password) {
        mSharedPreferences.edit().putString(PREF_PSW, password).apply();
    }
}
