#ifndef PROTOCOLDECODER_H
#define PROTOCOLDECODER_H

#include <vector>

class MessageResponse {
public:
    int messageType;
    std::vector<uint8_t> data;

    MessageResponse(int type, const std::vector<uint8_t>& d) : messageType(type), data(d) {}
};

class ProtocolConstants {
public:
    static const char MESSAGE_TOP_CHAR = '?';
    static const char MESSAGE_HEADER_BYTE = '#';
};

class ProtocolDecoder {
private:
    static long msgDataLen;
    static int messageType;
    static std::vector<uint8_t> buffer;

    static void clear();

public:
    ProtocolDecoder() = delete;

    static bool isHeaderChunk(const std::vector<uint8_t>& chunk);
    static int decode16BE(const std::vector<uint8_t>& src, int offset);
    static long decode32BE(const std::vector<uint8_t>& src, int offset);
    static bool packetCompletionCheck(const uint8_t* value, size_t length);
    static MessageResponse decode();
};

#endif // PROTOCOLDECODER_H
