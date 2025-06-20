#ifndef PROTOCOLDECODER_H
#define PROTOCOLDECODER_H

#include <vector>

class MessageResponse {
public:
    int messageType;
    std::vector<uint8_t> data;

    MessageResponse(int type, const std::vector<uint8_t> &d) : messageType(type), data(d) {}
};

class ProtocolConstants {
public:
    static const char MESSAGE_TOP_CHAR = '?';
    static const char MESSAGE_HEADER_BYTE = '#';
};

class ProtocolDecoder {
private:
    long msgDataLen;
    int messageType;
    std::vector<uint8_t> buffer;

    // 私有构造函数、析构函数、拷贝构造函数和赋值运算符
    ProtocolDecoder() {
        msgDataLen = 0L;
        messageType = 0;
    }

    ~ProtocolDecoder() = default;

    ProtocolDecoder(const ProtocolDecoder &) = delete;

    ProtocolDecoder &operator=(const ProtocolDecoder &) = delete;

    void clear();

    bool isHeaderChunk(const std::vector<uint8_t> &chunk);

    int decode16BE(const std::vector<uint8_t> &src, int offset);

    long decode32BE(const std::vector<uint8_t> &src, int offset);

public:
    // 获取单例实例的静态方法
    static ProtocolDecoder &getInstance() {
        static ProtocolDecoder instance;
        return instance;
    }

    bool packetCompletionCheck(const uint8_t *value, size_t length);

    MessageResponse decode();

    std::vector<uint8_t> getRawData();
};

#endif // PROTOCOLDECODER_H
