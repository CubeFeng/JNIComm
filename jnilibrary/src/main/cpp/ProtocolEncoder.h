#ifndef PROTOCOLENCODER_H
#define PROTOCOLENCODER_H

#include <vector>
#include <string>

class ProtocolEncoder {
private:
    static const int TREAZOR_PACKET_SIZE = 64;
    static const int ANDROID_BLE_PACKET_SIZE = 192;
    static const int IOS_BLE_PACKET_SIZE = 128;

    // 协议头相关常量
    static const char TREAZOR_HEADER_FIRST = '#';
    static const char TREAZOR_HEADER_SECOND = '#';
    static const char TREAZOR_PACKET_PREFIX = '?';

    static void logError(const std::string& message);
    static void logDebug(const std::string& message);
    static int calculateEncodeBufferSize(const std::vector<uint8_t>& data);
    static std::vector<uint8_t> createPacket(const std::vector<uint8_t>& data, int offset);

public:
    static std::vector<uint8_t> encodeProtocol(int messageType, const std::vector<uint8_t>& data);
    static std::vector<uint8_t> encode(int messageType, const std::vector<uint8_t>& data);
    static std::vector<std::vector<uint8_t>> splitDataWithHeader(const std::vector<uint8_t>& data);
    static std::vector<uint8_t> convertListToByteArray(const std::vector<std::vector<uint8_t>>& list);
    static std::vector<std::vector<uint8_t>> slice(const std::vector<uint8_t>& data, int sliceSize);
    static std::vector<std::vector<uint8_t>> slice(const std::vector<uint8_t>& data);
};

#endif // PROTOCOLENCODER_H
