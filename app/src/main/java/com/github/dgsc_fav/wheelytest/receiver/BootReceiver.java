package com.github.dgsc_fav.wheelytest.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by DG on 19.10.2016.
 */
@Deprecated
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // в задании не было сказано про запуск после перезагрузки.
        // поэтому ничего не запускаем. и параметры последнего успешного входа не храним
        //ServiceHelper.ensureSocketService(context);
    }
}
