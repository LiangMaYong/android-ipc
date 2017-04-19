// IIpcBinder.aidl
package com.liangmayong.ipc;
import com.liangmayong.ipc.IIpcListener;

// Declare any non-default types here with import statements

interface IIpcBinder {

     void setListener(IIpcListener listener);

     void onAction(int requestCode, in Bundle extras);

}
