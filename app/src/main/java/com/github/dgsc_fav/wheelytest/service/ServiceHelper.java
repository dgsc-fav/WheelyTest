package com.github.dgsc_fav.wheelytest.service;

import android.content.Context;
import android.support.annotation.NonNull;

import com.github.dgsc_fav.wheelytest.util.ServiceUtils;

/**
 * Created by DG on 19.10.2016.
 */

public class ServiceHelper {
    public static void ensureServices(@NonNull Context context) {
        if(!ServiceUtils.isServiceRunning(LocationService.class, context)) {
            context.startService(LocationService.getIntent(context));
        }
        if(!ServiceUtils.isServiceRunning(SocketService.class, context)) {
            context.startService(SocketService.getIntent(context));
        }
    }
}
