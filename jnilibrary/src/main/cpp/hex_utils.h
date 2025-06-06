//
// Created by feng on 2025/6/6.
//

#ifndef JNICOMM_HEX_UTILS_H
#define JNICOMM_HEX_UTILS_H

#include <vector>
#include <string>
#include <stdexcept>


std::vector<unsigned char> hexStringToByteArray(const std::string& hexString) {
    if (hexString.length() % 2 != 0) {
        throw std::invalid_argument("Invalid hex string length");
    }

    std::vector<unsigned char> byteData;
    byteData.reserve(hexString.length() / 2);

    try {
        for (size_t i = 0; i < hexString.length(); i += 2) {
            unsigned char byte = static_cast<unsigned char>(std::stoul(hexString.substr(i, 2), nullptr, 16));
            byteData.push_back(byte);
        }
    } catch (const std::invalid_argument& e) {
        throw std::invalid_argument(std::string("Invalid hex string: ") + e.what());
    } catch (const std::out_of_range& e) {
        throw std::out_of_range(std::string("Hex value out of range: ") + e.what());
    }

    return byteData;
}




#endif //JNICOMM_HEX_UTILS_H
