package com.example.jnilibrary;

public class ProtocolConstants {
    public static final int MESSAGE_TOP_CHAR = 0x003F;
    public static final int MESSAGE_HEADER_BYTE = 0x23;
    public static final int HEADER_SIZE = 1 + 1 + 4 + 2;
    public static final int BUFFER_SIZE = 63;

    // 固定头部 ?##
    public static final int COMMON_HEADER_SIZE = 6;

    public static final int ANDROID_BLE_PACKET_SIZE = 192;
    public static final int IOS_BLE_PACKET_SIZE = 128;
}
