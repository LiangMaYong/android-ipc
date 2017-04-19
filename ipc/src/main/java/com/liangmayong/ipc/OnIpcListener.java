package com.liangmayong.ipc;

import android.os.Bundle;

/**
 * Created by LiangMaYong on 2017/2/21.
 */
public interface OnIpcListener {

    void onSuccess(Bundle extras);

    void onFailure(IpcExcption excption);

}
