package com.liangmayong.ipc;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

/**
 * Created by LiangMaYong on 2017/4/19.
 */
public abstract class IpcService extends Service {

    // listener
    private IIpcListener listener = null;

    @Override
    public final IBinder onBind(Intent arg0) {
        return new BinderImpl();
    }


    ////////////////////////////////////////////////////////////////////////
    //////// Protected
    ////////////////////////////////////////////////////////////////////////

    protected abstract void onRequest(int requestCode, Bundle bundle);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /**
     * returnSuccess
     *
     * @param requestCode code
     * @param extras      bundle
     */
    protected final void returnSuccess(int requestCode, Bundle extras) {
        if (listener != null) {
            try {
                if (extras == null) {
                    extras = new Bundle();
                }
                listener.onSuccess(requestCode, extras);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * returnFailure
     *
     * @param requestCode requestCode
     * @param error       error
     */
    protected final void returnFailure(int requestCode, IpcExcption error) {
        if (listener != null) {
            Bundle extras = new Bundle();
            extras.putSerializable(IpcConstant.INTENT_EXTRA_EXCEPTION, error);
            try {
                listener.onFailure(requestCode, extras);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////
    //////// Class
    ////////////////////////////////////////////////////////////////////////

    /**
     * BinderImpl
     */
    private class BinderImpl extends IIpcBinder.Stub {

        @Override
        public void setListener(IIpcListener listener) throws RemoteException {
            IpcService.this.listener = listener;
        }

        @Override
        public void onAction(int requestCode, Bundle bundle) throws RemoteException {
            IpcService.this.onRequest(requestCode, bundle);
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            try {
                super.onTransact(code, data, reply, flags);
            } catch (RemoteException e) {
                Log.e("IpcService", "Unexpected remote exception", e);
                // throw e;
            }
            return true;
        }

    }
}
