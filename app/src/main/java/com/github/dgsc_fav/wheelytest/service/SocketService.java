package com.github.dgsc_fav.wheelytest.service;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.github.dgsc_fav.wheelytest.api.Consts;
import com.github.dgsc_fav.wheelytest.api.model.SimpleLocation;
import com.github.dgsc_fav.wheelytest.util.NetUtils;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by DG on 19.10.2016.
 */
public class SocketService extends ForegroundService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, Consts {

    /**
     * Listener соединения с сокетом
     */
    public interface ISocketServiceConnectionListener {
        void onSocketServiceConnected();

        void onSocketServiceError(String error);

        void onSocketServiceDisconnect(String msg, int reason);
    }

    /**
     * Listener сообщений от сокета
     */
    public interface IMessageListener {
        void onMessage(String msg);
    }

    public static final int MANUAL   = 0;
    public static final int ON_ERROR = 1;

    private static final int NOTIFICATION_ID = 101;

    private final IBinder           mBinder            = new MyBinder(this);
    private final ExecutorService   mExecutorService   = Executors.newSingleThreadExecutor();
    private final IntentFilter      mIntentFilter      = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isNetworkAvailable = NetUtils.isNetworkAvailable(context);
            e(" mWebSocketState", mWebSocketState);
            e(" isNetworkAvailable", isNetworkAvailable);

            if((mWebSocketState == WebSocketState.CREATED || mWebSocketState == WebSocketState.CLOSED) && mNeedConnected) {
                tryConnect();
            }
        }
    };
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
    private String                           mLastMessage;
    private boolean                          mRequestDisconnect;
    private WebSocketState                   mWebSocketState;
    private ISocketServiceConnectionListener mConnectionListener;
    private IMessageListener                 mMessageListener;
    // true, если мы должны быть Connected
    private boolean                          mNeedConnected;
    // число попыток соединения
    private int                              mConnectTimes;
    private WebSocket ws = null;

    private final WebSocketAdapter mWebSocketAdapter = new WebSocketAdapter() {

        @Override
        public void onFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            v(" onFrame:", frame);
        }


        @Override
        public void onContinuationFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            v(" onContinuationFrame:", frame);
        }


        @Override
        public void onTextFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            v(" onTextFrame:", frame);
        }


        @Override
        public void onBinaryFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            v(" onBinaryFrame:", frame);
        }


        @Override
        public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            v(" onCloseFrame:", frame);
        }




        @Override
        public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
            v(" onBinaryMessage:", binary);
        }


        @Override
        public void onSendingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            v(" onSendingFrame:", frame);
        }


        @Override
        public void onFrameSent(WebSocket websocket, WebSocketFrame frame) throws Exception {
            v(" onFrameSent:", frame);
        }


        @Override
        public void onFrameUnsent(WebSocket websocket, WebSocketFrame frame) throws Exception {
            e(" onFrameUnsent:", frame);
        }



        @Override
        public void onFrameError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
            e(" onFrameError:", cause);
        }

        @Override
        public void onMessageDecompressionError(WebSocket websocket, WebSocketException cause, byte[] compressed) throws Exception {
            e(" onMessageDecompressionError:", cause);
        }





//////
        @Override
        public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {
            w(" onStateChanged:", String.valueOf(newState));

            mWebSocketState = newState;
        }

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            w(" onConnected:", String.valueOf(headers));

            mConnectTimes = 0;

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

            // если сами отключились
            if(mRequestDisconnect) {
                mNeedConnected = false;

                if(mConnectionListener != null) {
                    mConnectionListener.onSocketServiceDisconnect("disconnected by user",
                                                                  getDisconnectReason());
                }
            } else {
                if(closedByServer) {
                    reconnect();
                }
//                // если сеть доступна
//                if(NetUtils.isNetworkAvailable(SocketService.this)) {
//                    // пробуем соединиться
//                    tryConnect();
//                } // else будем ждать сигнала свыше
            }
        }

        @Override
        public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
            e(" onError:", cause);
            reconnect();
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
            reconnect();
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
        int pingcnt;
        @Override
        public void onPingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            v(" onPingFrame:", frame);
//            v(" onPingFrame:", websocket.isOpen());
//            super.onPingFrame(websocket, frame);
//            websocket.sendPing("Are you there?");
//            pingcnt++;
//            v(" pingcnt :", pingcnt);
//
//            if(pingcnt > 3) {
//                //pingcnt = 0;
//                //ws.recreate().connect();
//            }
        }


        @Override
        public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            v(" onPongFrame:", frame);
            super.onPongFrame(websocket, frame);
            websocket.sendPong(frame.getPayloadText());
            pingcnt = 0;
        }
    };

    public static Intent getIntent(Context context) {
        Intent intent = new Intent(context, SocketService.class);
        intent.putExtra(KEY_FOREGROUND, true); // неубиваемый сервис
        intent.putExtra(KEY_TICKER, "SocketService.TICKER");
        intent.putExtra(KEY_TITLE, "SocketService.TITLE");
        intent.putExtra(KEY_CONTENT, "SocketService.CONTENT");
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

        e(" registerReceiver:", mIntentFilter.getAction(0));
        // подпишемся на сообщения о смени статуса сети
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, mIntentFilter);

        startHandleLocation();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;

        w("onStartCommand", "");

        return super.onStartCommand(intent, flags, startId);
    }

    public void setISocketServiceConnectionListener(ISocketServiceConnectionListener iSocketServiceConnectionListener) {
        mConnectionListener = iSocketServiceConnectionListener;
    }

    public void setIMessageListener(IMessageListener iMessageListener) {
        mMessageListener = iMessageListener;
        if(mMessageListener != null) {
            // вдруг, подключились, а сообщения уже были
            mMessageListener.onMessage(mLastMessage);
        }
    }

    @Override
    public void onDestroy() {
        e(" unregisterReceiver:", mIntentFilter.getAction(0));
        // подпишемся на сообщения о смени статуса сети
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);

        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        disconnect();

        super.onDestroy();

        e(" ***Oops, destroyed***", "");
    }

    public boolean isConnected() {
        return mWebSocketState == WebSocketState.OPEN;
    }

    public boolean isConnecting() {
        return mWebSocketState == WebSocketState.CONNECTING;
    }

    public void connect(String username, String password, ISocketServiceConnectionListener iSocketServiceConnectionListener, IMessageListener iMessageListener) {
        mUsername = username;
        mPassword = password;
        setISocketServiceConnectionListener(iSocketServiceConnectionListener);
        setIMessageListener(iMessageListener);
        mRequestDisconnect = false;

        if(!TextUtils.isEmpty(mUsername) && !TextUtils.isEmpty(mPassword)) {
            tryConnect();
        } else {
            if(mConnectionListener != null) {
                mConnectionListener.onSocketServiceError("bad credientals");
            }
        }
    }

    public void disconnect() {
        mRequestDisconnect = true;
        mNeedConnected = false;

        if(ws != null) {
            ws.disconnect();
        }
    }

    public int getDisconnectReason() {
        return mRequestDisconnect ? MANUAL : ON_ERROR;
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

        if(mWebSocketState == WebSocketState.OPEN && mLastLocation != null) {
            // send
            SimpleLocation simpleLocation = new SimpleLocation(mLastLocation.getLatitude(),
                                                               mLastLocation.getLongitude());
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            String msg = gson.toJson(simpleLocation);

            w(" sendText:", msg);

            if(Thread.currentThread() == Looper.getMainLooper().getThread()) {
                w("MainThread:", "sendText");
            }
            ws.sendText(msg);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void tryConnect() {
        mNeedConnected = true;
        mConnectTimes++;

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
            if(mConnectionListener != null) {
                mConnectionListener.onSocketServiceDisconnect(e.getLocalizedMessage(), ON_ERROR);
            }
            return;
        }

        if(ws == null) {
            if(mConnectionListener != null) {
                mConnectionListener.onSocketServiceError("ws == null");
            }
            return;
        }

        // Register a listener to receive WebSocket events.
        ws.addListener(mWebSocketAdapter);
        // Send a ping per 60 seconds.
        ws.setPingInterval(60 * 1000);
        // Make this library report an error when the end of the input stream
        // of the WebSocket connection is reached before a close frame is read.
        ws.setMissingCloseFrameAllowed(false);

        // Connect to the server asynchronously.
        Future<WebSocket> future = ws.connect(mExecutorService);

        try {
            if(Thread.currentThread() == Looper.getMainLooper().getThread()) {
                w("MainThread:", "connect");
            }
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
                if(mConnectionListener != null) {
                    mConnectionListener.onSocketServiceError(e.getLocalizedMessage());
                }
            }
        } catch(InterruptedException e) {
            if(mConnectionListener != null) {
                mConnectionListener.onSocketServiceError(e.getLocalizedMessage());
            }
        }
    }

    private void reconnect() {
        try {
            ws = ws.recreate().connect();
        } catch(WebSocketException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }
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


    private static final String TAG = SocketService.class.getSimpleName();

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
