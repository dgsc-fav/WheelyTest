package com.github.dgsc_fav.wheelytest.ui.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.github.dgsc_fav.wheelytest.R;

/**
 * Created by DG on 19.10.2016.
 */
public class LoginActivity extends PermissionsActivity {

    private EditText    mUsername;
    private EditText    mPassword;
    private Button      mConnect;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.title_activity_login);

        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        mConnect = (Button) findViewById(R.id.connect);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        mConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isUsernameAccept() && isPasswordAccepted()) {
                    doConnect();
                }
            }
        });

        // проверка наличия permissions
        checkLocationServicePermissions();
    }

    private void enableInputs() {
        mUsername.setEnabled(true);
        mPassword.setEnabled(true);
        mConnect.setEnabled(true);
        mProgressBar.setVisibility(View.GONE);
    }

    private void disableInputs() {
        mUsername.setEnabled(false);
        mPassword.setEnabled(false);
        mConnect.setEnabled(false);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    public boolean isUsernameAccept() {
        boolean usernameAccept = false;
        String username = mUsername
                                  .getText()
                                  .toString()
                                  .trim();
        if(TextUtils.isEmpty(username)) {
            mUsername.requestFocus();
            mUsername.setError(getString(R.string.username_error));
        } else {
            usernameAccept = true;
        }
        return usernameAccept;
    }

    public boolean isPasswordAccepted() {
        boolean passwordAccepted = false;
        String password = mPassword
                                  .getText()
                                  .toString()
                                  .trim();
        if(TextUtils.isEmpty(password)) {
            mPassword.requestFocus();
            mPassword.setError(getString(R.string.password_error));
        } else {
            passwordAccepted = true;
        }
        return passwordAccepted;
    }

    private void doConnect() {

        disableInputs();

        // connect

        // TODO: 19.10.2016 as successfull
        startActivity(new Intent(this, MapsActivity.class));
        finish(); // не возвращаемся по назад из MapsActivity
    }

    @Override
    public void processWithPermissionsGranted() {
        enableInputs();
    }

    @Override
    public void processWithPermissionsDenied() {
        showMessageOKCancel(R.string.about_location_permissions_info, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        }, null);
    }
}

