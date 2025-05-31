package com.example.jnilibrary;

public enum MessageType {
    Features(17),
    ButtonRequest(26),
    ButtonAck(27),
    EthereumGetAddress(56),
    EthereumGetPublicKey(450),

    EthereumTransaction(58),
    EthereumTxRequest(59),
    // 未知消息类型
    UNKNOWN(-1);

    private final int value;

    MessageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }


    /**
     * 根据代码获取对应的 MessageType
     *
     * @param code
     * @return
     */
    public static MessageType fromCode(int code) {
        for (MessageType type : values()) {
            if (type.value == code) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
