package com.github.dgsc_fav.wheelytest.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.github.dgsc_fav.wheelytest.service.LocationService;
import com.github.dgsc_fav.wheelytest.service.ServiceHelper;
import com.github.dgsc_fav.wheelytest.util.ServiceUtils;

/**
 * Created by DG on 19.10.2016.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // start services
        ServiceHelper.ensureServices(context);
    }
}
