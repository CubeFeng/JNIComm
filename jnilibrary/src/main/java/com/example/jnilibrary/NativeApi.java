package com.example.jnilibrary;

import android.util.Log;

public class NativeApi {

    private static final String TAG = "NativeApi";


    static {
        System.loadLibrary("jnilibrary");
    }

    /**
     * 接收来自 Native 的数据
     *
     * @param data
     */
    public static void onNativeDataReceived(byte[] data) {
        Log.i(TAG, "[Java] recvNativeData: " + HexString.byteArrayToHex(data));
        HyperMateAdapter.getInstance().writeAndWaitForResponse(data);
    }


    public static native void initNative();

    public static native void sendDataToNative(byte[] data);

    public static native byte[] getFeatures();
}
