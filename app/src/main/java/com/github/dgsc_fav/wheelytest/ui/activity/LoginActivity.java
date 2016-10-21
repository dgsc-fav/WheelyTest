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
import com.github.dgsc_fav.wheelytest.ws.WebSocketClient;

/**
 * Created by DG on 19.10.2016.
 */
public class LoginActivity extends PermissionsActivity implements WebSocketClient.ISocketServiceConnectionListener, WebSocketClient.IMessageListener {

    private final ServiceConnection mSocketServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((SocketService.MyBinder) service).getService();

            if(mService.getWebSocketClient().isConnected()) {
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
        }
    };
    private SocketService mService;
    private boolean       mIsBound;
    private EditText      mUsername;
    private EditText      mPassword;
    private Button        mConnect;
    private ProgressBar   mProgressBar;

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

        disableInputs();

        // connect
        final String username = mUsername.getText().toString();
        final String password = mPassword.getText().toString();

        if(mIsBound) {
            mService.getWebSocketClient().connect(username, password, this, this);
        }
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
    }

    @Override
    public void processWithPermissionsDenied() {
        finishWithDialog();
    }

    private void startSocketService() {
        startService(SocketService.getIntent(this));
    }

    private void bindSocketService() {
        if(!mIsBound) {
            bindService(SocketService.getIntent(this), mSocketServiceConnection, BIND_AUTO_CREATE);
            mIsBound = true;
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
        if(mIsBound && !mService.getWebSocketClient().isConnected() && mService
                                                                               .getWebSocketClient()
                                                                               .getDisconnectReason() == SocketService.MANUAL) {
            unbindService(mSocketServiceConnection);
            mIsBound = false;
            stopService(SocketService.getIntent(this));
        } else if(mIsBound) {
            // иначе, просто отсоединяемся
            unbindService(mSocketServiceConnection);
            mIsBound = false;
        }
    }

    @Override
    public void onSocketServiceConnected() {
        // если сокетное соединение есть, то переход на карту
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switchToMap();
            }
        });
    }

    @Override
    public void onSocketServiceError(final Throwable throwable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this, throwable.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                enableInputs();
            }
        });
    }

    @Override
    public void onSocketServiceDisconnect(final String msg, int reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                enableInputs();
            }
        });
    }

    @Override
    public void onMessage(String msg) {
        // ignore
    }
}

