package com.github.dgsc_fav.wheelytest.service;

import android.content.Context;
import android.support.annotation.NonNull;

import com.github.dgsc_fav.wheelytest.util.ServiceUtils;

/**
 * Created by DG on 19.10.2016.
 */
public class ServiceHelper {

    public static boolean isSocketServiceRunning(@NonNull Context context) {
        return ServiceUtils.isServiceRunning(SocketService.class, context);
    }
}
