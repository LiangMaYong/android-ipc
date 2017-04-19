// IIpcListener.aidl
package com.liangmayong.ipc;

// Declare any non-default types here with import statements

interface IIpcListener {

    void onSuccess(int requestCode, in Bundle extras);

    void onFailure(int requestCode, in Bundle extras);
}
