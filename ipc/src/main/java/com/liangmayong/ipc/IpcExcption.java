package com.liangmayong.ipc;

/**
 * Created by LiangMaYong on 2017/4/19.
 */
public class IpcExcption extends Exception {

    public static final int UNKOWN = 0;
    public static final int SERVICE_DISCONNECT = -1;

    // code
    private int code = 0;

    public IpcExcption(int code) {
        this.code = code;
    }

    public IpcExcption(int code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public IpcExcption(int code, String message) {
        super(message);
        this.code = code;
    }

    public IpcExcption(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * getCode
     *
     * @return code
     */
    public int getCode() {
        return code;
    }

    /**
     * setCode
     *
     * @param code code
     */
    public void setCode(int code) {
        this.code = code;
    }
}