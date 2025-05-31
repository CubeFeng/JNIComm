package com.example.jnilibrary;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ProtocolEncoder {
    private static final String TAG = "ProtocolEncoder";

    private static final int TREAZOR_PACKET_SIZE = 64;
    private static final int ANDROID_BLE_PACKET_SIZE = 192;
    private static final int IOS_BLE_PACKET_SIZE = 128;

    // 协议头相关常量
    private static final byte TREAZOR_HEADER_FIRST = '#';
    private static final byte TREAZOR_HEADER_SECOND = '#';
    private static final byte TREAZOR_PACKET_PREFIX = '?';

    ///////////////////////////////////// trezor 协议编码 protobuf ///////////////////////////////////

    /**
     * trezor 协议编码 + 分包
     *
     * @param messageType
     * @param data
     * @return
     */
    public static byte[] encodeProtocol(int messageType, byte[] data) {
        if (data == null) {
            logError("encodeProtocol: 输入数据不能为 null");
            return new byte[0];
        }

        byte[] pbBytes = encode(messageType, data);
//        logDebug("encodeProtocol - pbBytes: " + StrUtil.byteArr2HexStr(pbBytes));

        List<byte[]> packets = splitDataWithHeader(pbBytes);
        byte[] cmd = convertListToByteArray(packets);
        logDebug("encodeProtocol - cmd(padding): " + HexString.byteArrayToHex(cmd));

        return cmd;
    }

    /**
     * trezor 协议编码
     * <p>
     * 首包格式：?##<msg type><data len><data>
     * 其他包格式：<?><data><补零>
     *
     * @param data
     * @return
     */
    static byte[] encode(int messageType, byte[] data) {
        if (data == null) {
            logError("encode: 输入数据不能为 null");
            return new byte[0];
        }

        int bufferSize = calculateEncodeBufferSize(data);
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

        buffer.put(TREAZOR_HEADER_FIRST)
                .put(TREAZOR_HEADER_SECOND)
                .putShort((short) messageType)
                .putInt(data.length)
                .put(data);

        return buffer.array();
    }

    /**
     * 计算编码所需的 ByteBuffer 大小
     *
     * @param data 待编码的数据
     * @return 所需的 ByteBuffer 大小
     */
    private static int calculateEncodeBufferSize(byte[] data) {
        // 2 个字节的协议头 + 2 个字节的消息类型 + 4 个字节的数据长度 + 数据本身长度
        return 2 + 2 + 4 + data.length;
    }


    /**
     * 将数据按每包 64 字节分割，每包首字节为 '?'，不足的补零
     *
     * @param data 待分割的原始数据
     * @return 分割后的数据包列表
     */
    static List<byte[]> splitDataWithHeader(byte[] data) {
        if (data == null) {
            logError("splitDataWithHeader: 输入数据不能为 null");
            return new ArrayList<>();
        }

        List<byte[]> packetList = new ArrayList<>();
        int offset = 0;

        while (offset < data.length) {
            byte[] packet = createPacket(data, offset);
            packetList.add(packet);
            offset += TREAZOR_PACKET_SIZE - 1;
        }

        return packetList;
    }

    /**
     * 创建单个分包
     *
     * @param data   原始数据
     * @param offset 当前偏移量
     * @return 单个分包
     */
    private static byte[] createPacket(byte[] data, int offset) {
        // 每包 64 字节，首字节为 '?'
        byte[] packet = new byte[TREAZOR_PACKET_SIZE];
        packet[0] = TREAZOR_PACKET_PREFIX;

        int copyLength = Math.min(data.length - offset, TREAZOR_PACKET_SIZE - 1);
        System.arraycopy(data, offset, packet, 1, copyLength);

        return packet;
    }

    /**
     * 使用 ByteBuffer 将 List<byte[]> 转换为一个 byte[] 数组
     *
     * @param list 包含多个 byte[] 的列表
     * @return 合并后的 byte[] 数组，如果输入列表为 null 或空，返回空数组
     */
    static byte[] convertListToByteArray(List<byte[]> list) {
        if (list == null || list.isEmpty()) {
            return new byte[0];
        }

        int totalLength = 0;
        for (byte[] bytes : list) {
            if (bytes != null) {
                totalLength += bytes.length;
            }
        }

        ByteBuffer buffer = ByteBuffer.allocate(totalLength);

        for (byte[] bytes : list) {
            if (bytes != null) {
                buffer.put(bytes);
            }
        }

        return buffer.array();
    }

    ////////////////////////////////////////// 蓝牙分包 /////////////////////////////////////////////

    /**
     * 分割数据为指定大小的包
     *
     * @param data
     * @param sliceSize android: 192 bytes, ios: 128 bytes
     * @return
     */
    static List<byte[]> slice(byte[] data, int sliceSize) {
        if (data == null || sliceSize <= 0) {
            logError("slice: 输入数据不能为 null 且分片大小必须大于 0");
            return new ArrayList<>();
        }

        // 分片数
        int pieces = (int) Math.floor((data.length - 1) / sliceSize) + 1;
        List<byte[]> sliceLists = new ArrayList<>(pieces);
        int offset = 0;

        while (offset < data.length) {
            int packetLength = Math.min(data.length - offset, sliceSize);
            byte[] buffer = new byte[packetLength];
            System.arraycopy(data, offset, buffer, 0, packetLength);
            sliceLists.add(buffer);
            offset += packetLength;
        }
        return sliceLists;
    }

    /**
     * 分割数据为 192 bytes 大小的包
     *
     * @param data
     * @return
     */
    public static List<byte[]> slice(byte[] data) {
        return slice(data, ANDROID_BLE_PACKET_SIZE);
    }

    private static void logError(String message) {
        Log.e(TAG, message);
    }

    private static void logDebug(String message) {
        Log.d(TAG, message);
    }

}
