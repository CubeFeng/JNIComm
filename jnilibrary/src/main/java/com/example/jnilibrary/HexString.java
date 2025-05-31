package com.example.jnilibrary;

public class HexString {

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param bytes 要转换的字节数组
     * @return 转换后的十六进制字符串
     */
    public static String byteArrayToHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }


    /**
     * 十六进制的str转换成byte数组（如："8AC4"转换成数组[0x8A, 0xC4]）
     *
     * @param str 长度必须是偶数，否则会抛异常
     */
    public static byte[] hexStr2ByteArr(String str) {
        if (str == null || str.length() % 2 != 0)
            throw new RuntimeException("param format error.");

        byte[] bt = new byte[str.length() / 2];
        for (int i = 0; i < bt.length; i++) {
            bt[i] = (byte) ((hexChar2Byte(str.charAt(2 * i)) << 4)
                    + hexChar2Byte(str.charAt(2 * i + 1)));
        }
        return bt;
    }

    /**
     * 十六进制的char转换成byte（如：'D'转换成十进制的13）
     *
     * @param c 必须是合法的十六进制字符0-9,a-f,A-F
     */
    public static byte hexChar2Byte(char c) {
        if (c >= '0' && c <= '9') {
            return (byte) (c - '0');
        } else if (c >= 'A' && c <= 'F') {
            return (byte) (c - 'A' + 10);
        } else if (c >= 'a' && c <= 'f') {
            return (byte) (c - 'a' + 10);
        } else {
            throw new RuntimeException("param format error.");
        }
    }

}
