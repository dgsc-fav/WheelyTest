package com.github.dgsc_fav.wheelytest.service;


import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.github.dgsc_fav.wheelytest.api.Consts;
import com.github.dgsc_fav.wheelytest.api.model.SimpleLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.neovisionaries.ws.client.OpeningHandshakeException;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketState;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by DG on 19.10.2016.
 */
public class SocketService extends ForegroundService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, Consts {
    private static final String TAG = SocketService.class.getSimpleName();

    public interface ISocketServiceConnectionListener {
        void onSocketServiceConnected();

        void onSocketServiceError(String error);

        void onSocketServiceDisconnect(String msg, int reason);
    }

    public interface IMessageListener {
        void onMessage(String msg);
    }

    private static final int NOTIFICATION_ID = 101;

    private final IBinder mBinder = new MyBinder(this);

    @Deprecated
    private int    mServiceStartId; // только для теста
    @Deprecated
    private String mInstanceId;// только для теста

    private LocationRequest                  mLocationRequest;
    private FusedLocationProviderApi         mFusedLocationProviderApi;
    private GoogleApiClient                  mGoogleApiClient;
    private String                           mUsername;
    private String                           mPassword;
    private Location                         mLastLocation;
    private String                         mLastMessage;
    private boolean                          mIsConnected;
    private ISocketServiceConnectionListener mConnectionListener;
    private IMessageListener                 mMessageListener;

    public static Intent getIntent(Context context/*, String username, String password*/) {
        Intent intent = new Intent(context, SocketService.class);
        intent.putExtra(KEY_FOREGROUND, true); // неубиваемый сервис
        intent.putExtra(KEY_TICKER, "SocketService.TICKER");
        intent.putExtra(KEY_TITLE, "SocketService.TITLE");
        intent.putExtra(KEY_CONTENT, "SocketService.CONTENT");
        //        intent.putExtra(KEY_USERNAME, username);
        //        intent.putExtra(KEY_PASSWORD, password);
        intent.putExtra(KEY_ID, NOTIFICATION_ID);
        return intent;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstanceId = Long.toHexString(System.currentTimeMillis());

        startHandleLocation();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;

        w("onStartCommand", "");

        //        if(intent != null) {
        //            mUsername = intent.getStringExtra(KEY_USERNAME);
        //            mPassword = intent.getStringExtra(KEY_TICKER);
        //        }
        //        Log.d(TAG, " onStartCommand: " + mUsername + "|" + mPassword);
        //        if(TextUtils.isEmpty(mUsername) || TextUtils.isEmpty(mPassword)) {
        //            stopSelf();
        //            return START_STICKY_COMPATIBILITY;
        //        }

        return super.onStartCommand(intent, flags, startId);
    }

    TimerTask heartBeatTask;
    Timer     timer;

    protected void serviceTask() {

        w("serviceTask", "");

        heartBeatTask = new TimerTask() {
            @Override
            public void run() {
                i(" ***RUNNING*** ", mIsConnected + ":" + mUsername + "|" + mPassword);
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(heartBeatTask, 0, 15000);
    }

    private void stopServiceTask() {
        w("stopServiceTask", "");

        try {

            if(timer != null) {
                timer.cancel();
                timer = null;
            }
            if(heartBeatTask != null) {
                heartBeatTask.cancel();
                heartBeatTask = null;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopServiceTask();

        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        e(" ***Oops, destroyed***", "");
    }

    public List getPlaces() {
        return null;
    }

    public static final int MANUAL   = 0;
    public static final int ON_ERROR = 1;
    private boolean requestDisconnect;

    public boolean isConnected() {
        return mIsConnected;
    }

    public void connect(String username, String password, ISocketServiceConnectionListener iSocketServiceConnectionListener, IMessageListener iMessageListener) {
        mUsername = username;
        mPassword = password;
        setISocketServiceConnectionListener(iSocketServiceConnectionListener);
        setIMessageListener(iMessageListener);


        if(!TextUtils.isEmpty(mUsername) && !TextUtils.isEmpty(mPassword)) {
            // try connect

            tryConnect();

            // blah-blah-blah
            if(false) {
                mIsConnected = requestDisconnect = false;

                if(mIsConnected) {
                    serviceTask();
                } else {
                    stopServiceTask();
                }

                if(mConnectionListener != null) {
                    if(mIsConnected) {
                        mConnectionListener.onSocketServiceConnected();
                    } else {
                        mConnectionListener.onSocketServiceError("bad credientals");
                    }
                }
            }
        } else {
            if(mConnectionListener != null) {
                mConnectionListener.onSocketServiceError("bad credientals");
            }
        }
    }

    public void setISocketServiceConnectionListener(ISocketServiceConnectionListener iSocketServiceConnectionListener) {
        mConnectionListener = iSocketServiceConnectionListener;
    }

    public void setIMessageListener(IMessageListener iMessageListener) {
        mMessageListener = iMessageListener;
        if(mMessageListener != null) {
            mMessageListener.onMessage(mLastMessage);
        }
    }

    public void disconnect() {
        requestDisconnect = true;
        mNeedConnected = false;

        // blah-blah-blah

        if(ws != null) {
            ws.disconnect();
        }

        mIsConnected = false;

        if(mIsConnected) {
            serviceTask();
        } else {
            stopServiceTask();
        }

        if(mConnectionListener != null) {
            mConnectionListener.onSocketServiceDisconnect("blah-blah-blah", getDisconnectReason());
        }
    }

    public int getDisconnectReason() {
        return requestDisconnect ? MANUAL : ON_ERROR;
    }

    private void startHandleLocation() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(LocationServiceConsts.POLLING_FREQ);
        mLocationRequest.setFastestInterval(LocationServiceConsts.FASTEST_UPDATE_FREQ);

        mFusedLocationProviderApi = LocationServices.FusedLocationApi;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                                   .addApi(LocationServices.API)
                                   .addConnectionCallbacks(this)
                                   .addOnConnectionFailedListener(this)
                                   .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle arg0) {
        //noinspection ResourceType
        mFusedLocationProviderApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        w(" onLocationChanged:", String.valueOf(mLastLocation));

        if(mIsConnected && mLastLocation != null) {
            // send
            SimpleLocation simpleLocation = new SimpleLocation(mLastLocation.getLatitude(),
                                                               mLastLocation.getLongitude());
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            String msg = gson.toJson(simpleLocation);

            Log.w(" sendText:", msg);

            ws.sendText(msg);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    // мы должны быть Connected
    private boolean mNeedConnected;
    WebSocket ws = null;

    private void tryConnect() {
        mNeedConnected = true;

        // Create a WebSocket factory and set 5000 milliseconds as a timeout
        // value for socket connection.
        final WebSocketFactory factory = new WebSocketFactory().setConnectionTimeout(5000);


        // Create a WebSocket. The timeout value set above is used.
        try {
            ws = factory.createSocket(String.format("ws://mini-mdt.wheely.com/?%s=%s&%s=%s",
                                                    USERNAME,
                                                    mUsername,
                                                    PASSWORD,
                                                    mPassword));
        } catch(IOException e) {
            e.printStackTrace();

            if(mConnectionListener != null) {
                mConnectionListener.onSocketServiceDisconnect(e.getLocalizedMessage(), ON_ERROR);
            }
            return;
        }

        if(ws == null) {
            if(mConnectionListener != null) {
                mConnectionListener.onSocketServiceError("ws == null");
            }
        }


        // Register a listener to receive WebSocket events.
        assert ws != null;
        ws.addListener(new WebSocketAdapter() {
            @Override
            public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {
                w(" onStateChanged:", String.valueOf(newState));

                if(newState == WebSocketState.CLOSED) {
                    mIsConnected = false;
                    // // TODO: 20.10.2016  перезапустить

                }
                if(newState == WebSocketState.OPEN) {
                    mIsConnected = true;


                }
            }


            @Override
            public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
                w(" onConnected:", String.valueOf(headers));
                if(mConnectionListener != null) {
                    mConnectionListener.onSocketServiceConnected();
                }
            }


            @Override
            public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
                e(" onConnectError:", exception.getLocalizedMessage());
            }


            @Override
            public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
                w(" onDisconnected:", String.valueOf(closedByServer));
            }

            @Override
            public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
                e(" onError:", cause);
            }

            @Override
            public void onMessageError(WebSocket websocket, WebSocketException cause, List<WebSocketFrame> frames) throws Exception {
                e(" onMessageError:", cause);
            }

            @Override
            public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data) throws Exception {
                e(" onTextMessageError:", cause);
            }


            @Override
            public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
                e(" onSendError:", cause);
            }


            @Override
            public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
                e(" onUnexpectedError:", cause);
            }


            @Override
            public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception {
                e(" handleCallbackError:", cause);
            }


            @Override
            public void onSendingHandshake(WebSocket websocket, String requestLine, List<String[]> headers) throws Exception {
                d(" onSendingHandshake:", requestLine);

                for(String[] strings : headers) {
                    StringBuilder sb = new StringBuilder();
                    for(String string : strings) {
                        if(sb.length() > 0) {
                            sb.append(":");
                        }
                        sb.append(string);
                    }
                    v(" header [", sb + "]");
                }

            }

            @Override
            public void onTextMessage(WebSocket websocket, String message) throws Exception {
                // Received a text message.
                i(" onTextMessage:", message);
                mLastMessage = message;
                if(mMessageListener != null) {
                    mMessageListener.onMessage(message);
                }
            }
        });

        //ws.setUserInfo(mUsername, mPassword);
        //ws.addHeader("username", mUsername); ws.addHeader("password", mPassword);


        // Connect to the server asynchronously.
        Future<WebSocket> future = ws.connect(mExecutorService);

        try {
            // Wait for the opening handshake to complete.
            future.get();
        } catch(ExecutionException e) {

            if(e.getCause() instanceof OpeningHandshakeException) {
                OpeningHandshakeException exception = (OpeningHandshakeException) e.getCause();
                e(" OpeningHandshakeException:", "");
                v(" StatusLine:", exception.getStatusLine());
                v(" StatusCode:", exception.getStatusLine().getStatusCode());

                int mStatusCode = exception.getStatusLine().getStatusCode();
                if(mStatusCode == 403) {
                    if(mConnectionListener != null) {
                        mConnectionListener.onSocketServiceError("bad credientals");
                    }
                    disconnect();
                    return;
                }
            } else {
                e.printStackTrace();
                if(mConnectionListener != null) {
                    mConnectionListener.onSocketServiceError(e.getLocalizedMessage());
                }
            }
        } catch(InterruptedException e) {
            if(mConnectionListener != null) {
                mConnectionListener.onSocketServiceError(e.getLocalizedMessage());
            }
        }

        // Send a ping per 60 seconds.
        ws.setPingInterval(60 * 1000);

        //        JSONObject jsonObject = new JSONObject();
        //        try {
        //            jsonObject.put("username", mUsername);
        //            jsonObject.put("password", mPassword);
        //        } catch(JSONException e) {
        //            e.printStackTrace();
        //        }
        //        Log.w(TAG, mInstanceId + ": " + mServiceStartId + " sendText:" + jsonObject.toString());


        //ws.sendText(jsonObject.toString());
    }

    public static class MyBinder extends Binder {
        WeakReference<SocketService> mService;

        MyBinder(SocketService socketService) {
            mService = new WeakReference<>(socketService);
        }

        public SocketService getService() {
            return mService.get();
        }
    }

    private void d(String header, Object msg) {
        Log.d(TAG,
              String.format("%s: %d %s : %s",
                            mInstanceId,
                            mServiceStartId,
                            header,
                            String.valueOf(msg)));
    }

    private void v(String header, Object msg) {
        Log.v(TAG,
              String.format("%s: %d %s : %s",
                            mInstanceId,
                            mServiceStartId,
                            header,
                            String.valueOf(msg)));
    }

    private void i(String header, Object msg) {
        Log.i(TAG,
              String.format("%s: %d %s : %s",
                            mInstanceId,
                            mServiceStartId,
                            header,
                            String.valueOf(msg)));
    }

    private void w(String header, Object msg) {
        Log.w(TAG,
              String.format("%s: %d %s : %s",
                            mInstanceId,
                            mServiceStartId,
                            header,
                            String.valueOf(msg)));
    }

    private void e(String header, Object msg) {
        Log.e(TAG,
              String.format("%s: %d %s : %s",
                            mInstanceId,
                            mServiceStartId,
                            header,
                            String.valueOf(msg)));
    }
}
