package com.github.dgsc_fav.wheelytest.ui.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.github.dgsc_fav.wheelytest.R;

/**
 * Created by DG on 19.10.2016.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText mUsername;
    private EditText mPassword;
    private Button   mConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //ServiceHelper.ensureServices(this);

        setContentView(R.layout.activity_login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.title_activity_login);

        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        mConnect = (Button) findViewById(R.id.connect);

        mConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isUsernameAccept() && isPasswordAccepted()) {
                    doConnect();
                }
            }
        });
    }

    public boolean isUsernameAccept() {
        boolean usernameAccept = false;
        String username = mUsername.getText().toString().trim();
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
        String password = mPassword.getText().toString().trim();
        if(TextUtils.isEmpty(password)) {
            mPassword.requestFocus();
            mPassword.setError(getString(R.string.password_error));
        } else {
            passwordAccepted = true;
        }
        return passwordAccepted;
    }

    private void doConnect() {
        Toast.makeText(this, getString(R.string.connect_button_text), Toast.LENGTH_SHORT).show();
    }
}

