#include "ProtocolEncoder.h"
#include <iostream>
#include <algorithm>
#include <cstring>

void ProtocolEncoder::logError(const std::string& message) {
    std::cerr << "ProtocolEncoder: " << message << std::endl;
}

void ProtocolEncoder::logDebug(const std::string& message) {
    std::cout << "ProtocolEncoder: " << message << std::endl;
}

int ProtocolEncoder::calculateEncodeBufferSize(const std::vector<uint8_t>& data) {
    // 2 个字节的协议头 + 2 个字节的消息类型 + 4 个字节的数据长度 + 数据本身长度
    return 2 + 2 + 4 + data.size();
}

std::vector<uint8_t> ProtocolEncoder::encode(int messageType, const std::vector<uint8_t>& data) {
    int bufferSize = calculateEncodeBufferSize(data);
    std::vector<uint8_t> buffer(bufferSize);

    buffer[0] = TREAZOR_HEADER_FIRST;
    buffer[1] = TREAZOR_HEADER_SECOND;
    buffer[2] = static_cast<uint8_t>(messageType >> 8);
    buffer[3] = static_cast<uint8_t>(messageType & 0xFF);
    buffer[4] = static_cast<uint8_t>(data.size() >> 24);
    buffer[5] = static_cast<uint8_t>((data.size() >> 16) & 0xFF);
    buffer[6] = static_cast<uint8_t>((data.size() >> 8) & 0xFF);
    buffer[7] = static_cast<uint8_t>(data.size() & 0xFF);
    std::memcpy(buffer.data() + 8, data.data(), data.size());

    return buffer;
}

std::vector<uint8_t> ProtocolEncoder::encodeProtocol(int messageType, const std::vector<uint8_t>& data) {
    std::vector<uint8_t> pbBytes = encode(messageType, data);

    std::vector<std::vector<uint8_t>> packets = splitDataWithHeader(pbBytes);
    std::vector<uint8_t> cmd = convertListToByteArray(packets);
    // logDebug("encodeProtocol - cmd(padding): " + HexString.byteArrayToHex(cmd));

    return cmd;
}

std::vector<std::vector<uint8_t>> ProtocolEncoder::splitDataWithHeader(const std::vector<uint8_t>& data) {
    if (data.empty()) {
        logError("splitDataWithHeader: 输入数据不能为 null");
        return {};
    }

    std::vector<std::vector<uint8_t>> packetList;
    int offset = 0;

    while (offset < data.size()) {
        std::vector<uint8_t> packet = createPacket(data, offset);
        packetList.push_back(packet);
        offset += TREAZOR_PACKET_SIZE - 1;
    }

    return packetList;
}

std::vector<uint8_t> ProtocolEncoder::createPacket(const std::vector<uint8_t>& data, int offset) {
    // 每包 64 字节，首字节为 '?'
    std::vector<uint8_t> packet(TREAZOR_PACKET_SIZE, 0);
    packet[0] = TREAZOR_PACKET_PREFIX;

    int copyLength = std::min(static_cast<int>(data.size() - offset), TREAZOR_PACKET_SIZE - 1);
    std::memcpy(packet.data() + 1, data.data() + offset, copyLength);

    return packet;
}

std::vector<uint8_t> ProtocolEncoder::convertListToByteArray(const std::vector<std::vector<uint8_t>>& list) {
    if (list.empty()) {
        return {};
    }

    size_t totalLength = 0;
    for (const auto& bytes : list) {
        totalLength += bytes.size();
    }

    std::vector<uint8_t> buffer(totalLength);
    size_t offset = 0;
    for (const auto& bytes : list) {
        std::memcpy(buffer.data() + offset, bytes.data(), bytes.size());
        offset += bytes.size();
    }

    return buffer;
}

std::vector<std::vector<uint8_t>> ProtocolEncoder::slice(const std::vector<uint8_t>& data, int sliceSize) {
    if (data.empty() || sliceSize <= 0) {
        logError("slice: 输入数据不能为 null 且分片大小必须大于 0");
        return {};
    }

    std::vector<std::vector<uint8_t>> sliceLists;
    int offset = 0;

    while (offset < data.size()) {
        int packetLength = std::min(static_cast<int>(data.size() - offset), sliceSize);
        std::vector<uint8_t> buffer(data.begin() + offset, data.begin() + offset + packetLength);
        sliceLists.push_back(buffer);
        offset += packetLength;
    }
    return sliceLists;
}

std::vector<std::vector<uint8_t>> ProtocolEncoder::slice(const std::vector<uint8_t>& data) {
    return slice(data, ANDROID_BLE_PACKET_SIZE);
}
