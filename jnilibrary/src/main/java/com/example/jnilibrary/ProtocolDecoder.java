package com.example.jnilibrary;

import com.example.jnilibrary.MessageResponse;
import com.example.jnilibrary.ProtocolConstants;

import java.util.Arrays;

import okio.Buffer;

/**
 * 指令格式校验工具类
 *
 * @author FS
 * @time 2024/1/25 15:26
 */
public class ProtocolDecoder {

    private static long msgDataLen = 0L;
    private static int messageType = 0;

    private static final Buffer buffer = new Buffer();

    private ProtocolDecoder() {
    }

    /**
     * 验证是否首包数据
     * 格式：?##<msg type><data len><data>
     *
     * @param chunk 待验证的字节数组数据块
     * @return 若为符合格式的首包数据返回 true，否则返回 false
     */
    public static boolean isHeaderChunk(byte[] chunk) {
        if (chunk == null || chunk.length < 9) {
            return false;
        }

        byte magicQuestionMark = chunk[0];
        byte sharp1 = chunk[1];
        byte sharp2 = chunk[2];

        return magicQuestionMark == ProtocolConstants.MESSAGE_TOP_CHAR &&
                sharp1 == ProtocolConstants.MESSAGE_HEADER_BYTE &&
                sharp2 == ProtocolConstants.MESSAGE_HEADER_BYTE;
    }

    /**
     * 从字节数组中按大端序解码 16 位整数
     *
     * @param src    源字节数组
     * @param offset 解码起始偏移位置
     * @return 解码后的 16 位整数
     */
    public static int decode16BE(byte[] src, int offset) {
        return (int) Byte.toUnsignedInt(src[offset + 1])
                | (int) Byte.toUnsignedInt(src[offset + 0]) << 8;
    }

    /**
     * 计算有效数据长度，从字节数组中按大端序解码 32 位长整数
     *
     * @param src    源字节数组
     * @param offset 解码起始偏移位置
     * @return 解码后的 32 位长整数
     */
    public static long decode32BE(byte[] src, int offset) {
        return (long) Byte.toUnsignedInt(src[offset + 3])
                | (long) Byte.toUnsignedInt(src[offset + 2]) << 8
                | (long) Byte.toUnsignedInt(src[offset + 1]) << 16
                | (long) Byte.toUnsignedInt(src[offset + 0]) << 24;
    }

    /**
     * 检查数据包是否接收完成
     *
     * @param value 接收到的字节数组数据
     * @return 若数据包接收完成返回 true，否则返回 false
     */
    public static boolean packetCompletionCheck(byte[] value) {
        if (value == null) {
            return false;
        }

        int packetSize = value.length;
        if (ProtocolDecoder.isHeaderChunk(value)) {
            // 新指令，清空 buffer
            clear();
            // ?##<msg type><data len><data>
            messageType = ProtocolDecoder.decode16BE(value, 3);
            msgDataLen = ProtocolDecoder.decode32BE(value, 5);
            // ?##<msg type><data len>
            msgDataLen += 1 + 2 + 2 + 4;
            // 只要 payload
            buffer.write(Arrays.copyOfRange(value, 9, packetSize));
        } else {
            buffer.write(Arrays.copyOfRange(value, 0, packetSize));
        }
        msgDataLen -= packetSize;

        return msgDataLen <= 0;
    }

    /**
     * trezor 协议解码
     *
     * @return 解码后的字节数组
     */
    public static MessageResponse decode() {
        // readByteArray() 会清空 buffer，所以只能调用一次
        return new MessageResponse(messageType, buffer.snapshot().toByteArray());
//        return new MessageResponse(messageType, buffer.readByteArray());
    }

    private static void clear(){
        buffer.clear();
        msgDataLen = 0L;
        messageType = 0;
    }
}