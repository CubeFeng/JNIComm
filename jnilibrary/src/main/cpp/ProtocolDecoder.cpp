#include "ProtocolDecoder.h"
#include <algorithm>
#include <iostream>

bool ProtocolDecoder::isHeaderChunk(const std::vector<uint8_t> &chunk) {
    if (chunk.size() < 9) {
        return false;
    }

    char magicQuestionMark = chunk[0];
    char sharp1 = chunk[1];
    char sharp2 = chunk[2];

    return magicQuestionMark == ProtocolConstants::MESSAGE_TOP_CHAR &&
           sharp1 == ProtocolConstants::MESSAGE_HEADER_BYTE &&
           sharp2 == ProtocolConstants::MESSAGE_HEADER_BYTE;
}

int ProtocolDecoder::decode16BE(const std::vector<uint8_t> &src, int offset) {
    return (static_cast<int>(src[offset + 1]) & 0xFF) |
           ((static_cast<int>(src[offset + 0]) & 0xFF) << 8);
}

long ProtocolDecoder::decode32BE(const std::vector<uint8_t> &src, int offset) {
    return (static_cast<long>(src[offset + 3]) & 0xFFL) |
           ((static_cast<long>(src[offset + 2]) & 0xFFL) << 8) |
           ((static_cast<long>(src[offset + 1]) & 0xFFL) << 16) |
           ((static_cast<long>(src[offset + 0]) & 0xFFL) << 24);
}

bool ProtocolDecoder::packetCompletionCheck(const uint8_t *value, size_t length) {
    if (value == nullptr || length == 0) {
        return false;
    }

    size_t packetSize = length;
    // 将 std::vector 操作改为指针操作
    std::vector<uint8_t> chunk(value, value + length);
    if (ProtocolDecoder::isHeaderChunk(chunk)) {
        // 新指令，清空 buffer
        clear();
        msgDataLen = ProtocolDecoder::decode32BE(chunk, 5);
        // ?##<msg type><data len>
        msgDataLen += 1 + 2 + 2 + 4;
    }
    // 缓存所有数据
    buffer.insert(buffer.end(), value, value + length);
    msgDataLen -= packetSize;

    return msgDataLen <= 0;
}

MessageResponse ProtocolDecoder::decode() {
    // ?##<msg type><data len><data>
    messageType = ProtocolDecoder::decode16BE(buffer, 3);
    // data 域
    std::vector<uint8_t> data(buffer.begin() + 9, buffer.end());
    return MessageResponse(messageType, data);
}

std::vector<uint8_t> ProtocolDecoder::getRawData() {
    return buffer;
}

void ProtocolDecoder::clear() {
    buffer.clear();
    msgDataLen = 0L;
    messageType = 0;
}
