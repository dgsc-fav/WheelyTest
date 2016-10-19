package com.github.dgsc_fav.wheelytest.ui.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.github.dgsc_fav.wheelytest.R;
import com.github.dgsc_fav.wheelytest.service.ServiceHelper;

/**
 * Created by DG on 19.10.2016.
 */
public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ServiceHelper.ensureServices(this);

        setContentView(R.layout.activity_login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.title_activity_login);
    }
}

