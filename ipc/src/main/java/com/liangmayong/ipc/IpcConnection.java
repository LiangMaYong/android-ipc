package com.liangmayong.ipc;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by LiangMaYong on 2017/2/22.
 */
public class IpcConnection {

    private final String TAG = IpcConnection.class.getSimpleName();
    private final int WHAT_RESULT = 0;
    private final int WHAT_REQUEST_CONNECTED = 1;
    private final int WHAT_REQUEST_DISCONNECT = 2;
    // listeners
    private Map<Integer, WeakReference<OnIpcListener>> listeners = new HashMap<Integer, WeakReference<OnIpcListener>>();
    // connection
    private ConnectionImpl connection;
    // context
    private Context context;
    // waitRequests
    private ArrayList<IpcRequest> waitRequests = null;
    // requestCode
    private int requestCode = 0xFF000001;
    // services
    private final Class<? extends IpcService> serviceClass;
    // isConnecting
    private boolean isConnecting = false;
    //
    // handler
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == WHAT_RESULT) {
                if (msg.obj instanceof IpcResult) {
                    ((IpcResult) msg.obj).onCallback();
                }
            } else if (msg.what == WHAT_REQUEST_CONNECTED) {
                for (int i = 0; i < waitRequests.size(); i++) {
                    IpcRequest request = waitRequests.get(i);
                    postRequest(request.extras, request.listener);
                }
                waitRequests.clear();
            } else if (msg.what == WHAT_REQUEST_DISCONNECT) {
                for (int i = 0; i < waitRequests.size(); i++) {
                    waitRequests.get(i).listener.onFailure(new IpcExcption(IpcExcption.SERVICE_DISCONNECT, "Connect Service Error"));
                }
                waitRequests.clear();
            }
        }
    };

    // defualtListener
    private Listener defualtListener = new Listener() {
        @Override
        public void onConnected(IpcConnection messageConnection) {
            Log.d(TAG, "onConnected:" + serviceClass.getName());
            handler.obtainMessage(WHAT_REQUEST_CONNECTED).sendToTarget();
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "onDisconnected:" + serviceClass.getName());
            handler.obtainMessage(WHAT_REQUEST_DISCONNECT).sendToTarget();
        }
    };

    public IpcConnection(Context context, Class<? extends IpcService> ipcService) {
        this.context = context;
        this.serviceClass = ipcService;
        this.waitRequests = new ArrayList<IpcRequest>();
        this.connection = new ConnectionImpl();
    }

    /**
     * checkUpBindService
     */
    private void checkUpBindService() {
        Log.d(TAG, "checkUpBindService:" + serviceClass.getName());
        if (!isConnected()) {
            if (!isConnecting) {
                this.isConnecting = true;
                this.context.startService(new Intent(context, serviceClass));
                this.context.bindService(new Intent(context, serviceClass), connection, Context.BIND_IMPORTANT);
            }
        }
    }

    /**
     * postRequest
     *
     * @param bundle   bundle
     * @param listener messageActionListener
     */
    public void postRequest(Bundle bundle, OnIpcListener listener) {
        if (isConnected()) {
            if (listener != null) {
                listeners.put(requestCode, new WeakReference<OnIpcListener>(listener));
            }
            connection.doAction(requestCode, bundle);
            requestCode++;
        } else {
            checkUpBindService();
            waitRequests.add(new IpcRequest(bundle, listener));
        }
    }

    /**
     * isConnected
     *
     * @return isConnected
     */
    public boolean isConnected() {
        return connection.isConnected();
    }


    /**
     * disconnect
     */
    public void disconnect() {
        try {
            context.unbindService(connection);
        } catch (Exception e) {
        }
    }

    /**
     * IpcRequest
     */
    private class IpcRequest {
        private Bundle extras;
        private OnIpcListener listener;

        public IpcRequest(Bundle extras, OnIpcListener listener) {
            this.extras = extras;
            this.listener = listener;
        }
    }

    /**
     * IpcResult
     */
    private class IpcResult {
        private Bundle extras;
        private OnIpcListener listener;
        private IpcExcption excption;
        private boolean isSuccess = false;

        public IpcResult(Bundle extras, OnIpcListener listener, IpcExcption excption, boolean isSuccess) {
            this.extras = extras;
            this.listener = listener;
            this.excption = excption;
            this.isSuccess = isSuccess;
        }

        public void onCallback() {
            if (isSuccess) {
                if (listener != null) {
                    listener.onSuccess(extras);
                }
            } else {
                if (listener != null) {
                    listener.onFailure(excption);
                }
            }
        }

    }


    //////////////////////////////////////////////////////////////////////////////
    //////// Protected
    //////////////////////////////////////////////////////////////////////////////

    /**
     * onActionSuccess
     *
     * @param requestCode requestCode
     * @param extras      extras
     */
    protected void onActionSuccess(int requestCode, Bundle extras) {
        if (listeners.containsKey(requestCode)) {
            WeakReference<OnIpcListener> listenerReference = listeners.get(requestCode);
            if (listenerReference.get() != null) {
                handler.obtainMessage(WHAT_RESULT, new IpcResult(extras, listenerReference.get(), null, true)).sendToTarget();
            } else {
                listeners.remove(requestCode);
            }
        }
    }

    /**
     * onActionFailure
     *
     * @param requestCode requestCode
     * @param excption    excption
     */
    protected void onActionFailure(int requestCode, IpcExcption excption) {
        if (listeners.containsKey(requestCode)) {
            WeakReference<OnIpcListener> listenerReference = listeners.get(requestCode);
            if (listenerReference.get() != null) {
                handler.obtainMessage(WHAT_RESULT, new IpcResult(null, listenerReference.get(), excption, false)).sendToTarget();
            } else {
                listeners.remove(requestCode);
            }
        }
    }


    //////////////////////////////////////////////////////////////////////////////
    //////// Class
    //////////////////////////////////////////////////////////////////////////////

    /**
     * Listener
     */
    private interface Listener {

        void onConnected(IpcConnection messageConnection);

        void onDisconnected();
    }

    /**
     * ConnectionImpl
     */
    private class ConnectionImpl implements ServiceConnection {

        private IIpcBinder binderImpl = null;
        private boolean isConnected = false;

        public boolean isConnected() {
            return isConnected;
        }


        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            isConnected = true;
            isConnecting = false;
            try {
                binderImpl = IIpcBinder.Stub.asInterface(iBinder);
                binderImpl.setListener(new IIpcListener.Stub() {
                    @Override
                    public void onSuccess(int requestCode, Bundle bundle) throws RemoteException {
                        IpcConnection.this.onActionSuccess(requestCode, bundle);
                    }

                    @Override
                    public void onFailure(int requestCode, Bundle bundle) throws RemoteException {
                        IpcExcption exception = null;
                        try {
                            exception = (IpcExcption) bundle.getSerializable(IpcConstant.INTENT_EXTRA_EXCEPTION);
                        } catch (Exception e) {
                            exception = new IpcExcption(IpcExcption.UNKOWN, "Unkown");
                        }
                        IpcConnection.this.onActionFailure(requestCode, exception);
                    }
                });
            } catch (RemoteException e) {
            }
            defualtListener.onConnected(IpcConnection.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            binderImpl = null;
            isConnected = false;
            isConnecting = false;
            defualtListener.onDisconnected();
        }

        public void doAction(int requestCode, Bundle bundle) {
            if (isConnected) {
                try {
                    binderImpl.onAction(requestCode, bundle);
                } catch (RemoteException e) {
                }
            }
        }
    }

}
