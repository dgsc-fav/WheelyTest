package com.github.dgsc_fav.wheelytest.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.github.dgsc_fav.wheelytest.service.IntentConsts;

/**
 * Created by DG on 21.10.2016.
 */
public class ConnectionChangeReceiver extends BroadcastReceiver implements IntentConsts {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            // отправим локальным
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        } catch(Exception e) {
            e.printStackTrace();
            // может быть никто не зарегистрирован получателем и будет NullPointer
            /** но ожидается {@link com.github.dgsc_fav.wheelytest.service.SocketService} */
        }
    }
}
