package com.github.dgsc_fav.wheelytest.ws;

import android.text.TextUtils;

import com.github.dgsc_fav.wheelytest.api.Consts;
import com.github.dgsc_fav.wheelytest.exception.BadCredientalsException;
import com.neovisionaries.ws.client.OpeningHandshakeException;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketState;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.dgsc_fav.wheelytest.service.SocketService.MANUAL;
import static com.github.dgsc_fav.wheelytest.service.SocketService.ON_ERROR;

/**
 * Created by DG on 21.10.2016.
 */
public class WebSocketClient implements Consts {

    /**
     * Listener соединения с сокетом
     */
    public interface ISocketServiceConnectionListener {
        void onSocketServiceConnected();

        void onSocketServiceError(Throwable throwable);

        void onSocketServiceDisconnect(String msg, int reason);
    }

    /**
     * Listener сообщений от сокета
     */
    public interface IMessageListener {
        void onMessage(String msg);
    }

    private final ExecutorService  mExecutorService  = Executors.newCachedThreadPool();
    private final WebSocketAdapter mWebSocketAdapter = new WebSocketAdapter() {
        @Override
        public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {
            super.onStateChanged(websocket, newState);
            // следим за состоянием
            mWebSocketState = newState;
        }

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            super.onConnected(websocket, headers);

            if(mConnectionListener != null) {
                mConnectionListener.onSocketServiceConnected();
            }
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
            super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);

            // если сами отключились
            if(!closedByServer && mRequestDisconnect) {
                mNeedConnected = false;

                if(mConnectionListener != null) {
                    mConnectionListener.onSocketServiceDisconnect("disconnected by user",
                                                                  getDisconnectReason());
                }
            } else if(closedByServer) {
                // если сервер, то пробуем переподключиться
                reconnect();
            }
        }

        @Override
        public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
            super.onError(websocket, cause);
            reconnect();
        }

        @Override
        public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
            super.onUnexpectedError(websocket, cause);
            reconnect();
        }

        @Override
        public void onTextMessage(WebSocket websocket, String message) throws Exception {
            super.onTextMessage(websocket, message);
            // Received a text message.
            mLastMessage = message;
            if(mMessageListener != null) {
                mMessageListener.onMessage(message);
            }
        }

        @Override
        public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            super.onPongFrame(websocket, frame);
            websocket.sendPong(frame.getPayloadText());
        }
    };

    private ISocketServiceConnectionListener mConnectionListener;
    private IMessageListener                 mMessageListener;
    private boolean                          mRequestDisconnect;
    private WebSocketState                   mWebSocketState;
    private String                           mLastMessage;
    // true, если мы должны быть Connected
    private boolean                          mNeedConnected;
    private String                           mUsername;
    private String                           mPassword;
    private WebSocket                        mWebSocket;

    public void setSocketServiceConnectionListener(ISocketServiceConnectionListener iSocketServiceConnectionListener) {
        mConnectionListener = iSocketServiceConnectionListener;
    }

    public void setMessageListener(IMessageListener iMessageListener) {
        mMessageListener = iMessageListener;
        if(mMessageListener != null) {
            // вдруг, подключились, а сообщения уже были
            mMessageListener.onMessage(mLastMessage);
        }
    }

    public void connect(String username, String password, ISocketServiceConnectionListener iSocketServiceConnectionListener, IMessageListener iMessageListener) {
        mUsername = username;
        mPassword = password;
        setSocketServiceConnectionListener(iSocketServiceConnectionListener);
        setMessageListener(iMessageListener);
        mRequestDisconnect = false;

        if(!TextUtils.isEmpty(mUsername) && !TextUtils.isEmpty(mPassword)) {
            connect();
        } else {
            dispatchError(new BadCredientalsException());
        }
    }

    public void disconnect() {
        mRequestDisconnect = true;
        mNeedConnected = false;

        if(mWebSocket != null) {
            mWebSocket.disconnect();
        }
    }

    public int getDisconnectReason() {
        return mRequestDisconnect ? MANUAL : ON_ERROR;
    }

    private void connect() {
        mNeedConnected = true;

        // Create a WebSocket factory and set 5000 milliseconds as a timeout
        // value for socket connection.
        final WebSocketFactory factory = new WebSocketFactory().setConnectionTimeout(5000);

        // Create a WebSocket. The timeout value set above is used.
        try {
            mWebSocket = factory.createSocket(String.format("ws://mini-mdt.wheely.com/?%s=%s&%s=%s",
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

        // Register a listener to receive WebSocket events.
        mWebSocket.addListener(mWebSocketAdapter);
        // Send a ping per 60 seconds.
        mWebSocket.setPingInterval(60 * 1000);
        // Make this library report an error when the end of the input stream
        // of the WebSocket connection is reached before a close frame is read.
        mWebSocket.setMissingCloseFrameAllowed(false);

        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Connect to the server asynchronously.
                    Future<WebSocket> future = mWebSocket.connect(mExecutorService);

                    // Wait for the opening handshake to complete.
                    future.get();
                } catch(ExecutionException e) {

                    if(e.getCause() instanceof OpeningHandshakeException) {
                        // если не удалось соединиться
                        OpeningHandshakeException exception = (OpeningHandshakeException) e.getCause();
                        int mStatusCode = exception.getStatusLine().getStatusCode();
                        if(mStatusCode == 403) {
                            // неверный логин и/или пароль
                            dispatchError(new BadCredientalsException());
                            disconnect();
                            return;
                        }
                    } else {
                        dispatchError(e);
                    }
                } catch(InterruptedException e) {
                    dispatchError(e);
                }
            }
        });
    }

    private void reconnect() {
        try {
            mWebSocket = mWebSocket.recreate().connect();
        } catch(WebSocketException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void sendText(final String msg) {
        if(mWebSocket != null && mWebSocket.isOpen()) {
            mExecutorService.execute(new Runnable() {
                public void run() {
                    mWebSocket.sendText(msg);
                }
            });
        }
    }

    public void onNetworkAvailableChanged(boolean isNetworkAvailable) {
        // todo возможно, лишний метод, потому что в mWebSocketAdapter переподключится при ошибке
        if(isNetworkAvailable && mNeedConnected && !isConnected() && !isConnecting()) {
            connect();
        }
    }

    public boolean isConnected() {
        return mWebSocket != null && mWebSocket.isOpen();
    }

    public boolean isConnecting() {
        return mWebSocketState == WebSocketState.CONNECTING;
    }

    private void dispatchError(Throwable throwable) {
        if(mConnectionListener != null) {
            mConnectionListener.onSocketServiceError(throwable);
        }
    }
}
