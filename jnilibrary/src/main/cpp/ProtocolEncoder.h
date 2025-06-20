#ifndef PROTOCOLENCODER_H
#define PROTOCOLENCODER_H

#include <vector>
#include <string>

class ProtocolEncoder {
private:

    const int TREAZOR_PACKET_SIZE = 64;
    const int ANDROID_BLE_PACKET_SIZE = 192;
    const int IOS_BLE_PACKET_SIZE = 128;

    // 协议头相关常量
    const char TREAZOR_HEADER_FIRST = '#';
    const char TREAZOR_HEADER_SECOND = '#';
    const char TREAZOR_PACKET_PREFIX = '?';

    void logError(const std::string &message);

    void logDebug(const std::string &message);

    int calculateEncodeBufferSize(const std::vector<uint8_t> &data);

    std::vector<uint8_t> createPacket(const std::vector<uint8_t> &data, int offset);

    // 私有构造函数，防止外部实例化
    ProtocolEncoder() = default;

    // 禁用拷贝构造函数
    ProtocolEncoder(const ProtocolEncoder &) = delete;

    // 禁用赋值运算符
    ProtocolEncoder &operator=(const ProtocolEncoder &) = delete;

public:
    // 获取单例实例的静态方法
    static ProtocolEncoder &getInstance() {
        static ProtocolEncoder instance;
        return instance;
    }

    std::vector<uint8_t> encodeProtocol(int messageType, const std::vector<uint8_t> &data);

    std::vector<uint8_t> encode(int messageType, const std::vector<uint8_t> &data);

    std::vector<std::vector<uint8_t>> splitDataWithHeader(const std::vector<uint8_t> &data);

    std::vector<uint8_t>
    convertListToByteArray(const std::vector<std::vector<uint8_t>> &list);

    std::vector<std::vector<uint8_t>> slice(const std::vector<uint8_t> &data, int sliceSize);

    std::vector<std::vector<uint8_t>> slice(const std::vector<uint8_t> &data);
};


#endif // PROTOCOLENCODER_H