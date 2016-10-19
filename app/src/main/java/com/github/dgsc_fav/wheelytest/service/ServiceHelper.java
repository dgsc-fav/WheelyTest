package com.github.dgsc_fav.wheelytest.service;

import android.content.Context;
import android.support.annotation.NonNull;

import com.github.dgsc_fav.wheelytest.util.ServiceUtils;

/**
 * Created by DG on 19.10.2016.
 */

public class ServiceHelper {
    public static void ensureSocketService(@NonNull Context context) {
        // // TODO: 19.10.2016 пока без локации. разберусь с permissions без activity
        //        if(!ServiceUtils.isServiceRunning(LocationService.class, context)) {
        //            context.startService(LocationService.getIntent(context));
        //        }
        if(!ServiceUtils.isServiceRunning(SocketService.class, context)) {
            context.startService(SocketService.getIntent(context));
        }
    }

    public static boolean isLocationServiceRunning(@NonNull Context context) {
        return ServiceUtils.isServiceRunning(LocationService.class, context);
    }
}
