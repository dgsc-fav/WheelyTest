package com.github.dgsc_fav.wheelytest.util;

import android.app.ActivityManager;
import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Created by DG on 19.10.2016.
 */
public class ServiceUtils {

    public static boolean isServiceRunning(@NonNull Class<?> serviceClass, @NonNull Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if(serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
