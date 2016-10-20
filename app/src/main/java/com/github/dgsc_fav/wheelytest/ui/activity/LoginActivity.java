package com.github.dgsc_fav.wheelytest.ui.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.dgsc_fav.wheelytest.R;
import com.github.dgsc_fav.wheelytest.service.ServiceHelper;
import com.github.dgsc_fav.wheelytest.service.SocketService;

/**
 * Created by DG on 19.10.2016.
 */
public class LoginActivity extends PermissionsActivity implements SocketService.ISocketServiceConnectionListener {

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

        disableInputs();

        // проверка наличия сервисов google
        if(isServicesAvailable()) {
            // проверка наличия permissions
            checkLocationServicePermissions();
        }
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
        final String username = mUsername.getText().toString();
        final String password = mPassword.getText().toString();

        if(mIsBound) {
            mService.connect(username, password, this);
        }

        //ServiceHelper.ensureSocketService(this, username, password);

        // надо ли?

        //App.getTokenStore().setLastUsername(username);
        //App.getTokenStore().setLastPassword(password);

        // это уже если подключились к сервису
        //switchToMap();
    }

    private void switchToMap() {
        startActivity(new Intent(this, MapsActivity.class));
        finish(); // не возвращаемся по назад из MapsActivity
    }

    @Override
    public void processWithPermissionsGranted() {
        // permissions есть
        if(!ServiceHelper.isSocketServiceRunning(this)) {
            // если сервис не запущен, то запускаем
            startSocketService();
            // и подключаемся. именно так, чтобы при unbindService он не останавливался
            bindSocketService();
        } else {
            bindSocketService();
        }


//        // проверка, работает ли SocketService
//        if(ServiceHelper.isSocketServiceRunning(this)) {
//            // открываем карту
//            //switchToMap();
//
//            bindSocketService();
//
//        } else {
//            // обычный логин
//            enableInputs();
//        }
    }

    @Override
    public void processWithPermissionsDenied() {
        finishWithDialog();
    }

    protected SocketService mService;
    protected boolean         mIsBound;
    protected final ServiceConnection mSocketServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((SocketService.MyBinder) service).getService();
            mIsBound = true;

            if(mService.isConnected()) {
                // если сокетное соединение есть, то переход на карту
                switchToMap();
            } else {
                // иначе надо залогиниться
                enableInputs();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mIsBound = false;
        }
    };

    private void startSocketService() {
        startService(SocketService.getIntent(this));
    }

    private void bindSocketService() {
        if(!mIsBound) {
            bindService(SocketService.getIntent(this),
                        mSocketServiceConnection,
                        BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        bindSocketService();
    }

    @Override
    public void onStop() {
        super.onStop();

        // если сервис отключил соединение вручную (не по ошибке)
        // это когда нажали disconnect в MapsActivity
        if(mIsBound && !mService.isConnected() && mService.getDisconnectReason() == SocketService.MANUAL) {
            //mService.requestStopSelf();
            stopService(SocketService.getIntent(this));
        }
        if(mIsBound) {
            unbindService(mSocketServiceConnection);
        }
    }

    @Override
    public void onSocketServiceConnected() {
        // если сокетное соединение есть, то переход на карту
        switchToMap();
    }

    @Override
    public void onSocketServiceError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        enableInputs();
    }

    @Override
    public void onSocketServiceDisconnect(String msg, int reason) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        enableInputs();
    }
}

