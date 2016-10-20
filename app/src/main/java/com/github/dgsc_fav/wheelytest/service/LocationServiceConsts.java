package com.github.dgsc_fav.wheelytest.service;

/**
 * Created by DG on 20.10.2016.
 */

public interface LocationServiceConsts {
    long  ONE_MIN                = 1000 * 60;
    long  TWO_MIN                = ONE_MIN * 2;
    long  FIVE_MIN               = ONE_MIN * 5;
    long  POLLING_FREQ           = 1000 * 30; // 30 sec
    long  FASTEST_UPDATE_FREQ    = 1000 * 5;  // 5 sec
    float MIN_ACCURACY           = 25.0f;
    float MIN_LAST_READ_ACCURACY = 500.0f;
}
