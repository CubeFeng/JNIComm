#include "native-log.h"
#include <cstdint>

// 辅助函数实现
void __android_log_hex_print(int level, const char* tag, const char* msg, const uint8_t* data, size_t len) {
    if (data == nullptr || len == 0) {
        return;
    }

    // 为缓冲区预留足够空间，包含前缀、分隔符和十六进制数据
    char buffer[2048];
    size_t pos = 0;
    if (msg) {
        pos += snprintf(buffer + pos, sizeof(buffer) - pos, "%s: ", msg);
    }

    for (size_t i = 0; i < len; ++i) {
        if (pos + 3 >= sizeof(buffer)) {
            break;
        }
        pos += snprintf(buffer + pos, sizeof(buffer) - pos, "%02X", data[i]);
    }
    if (pos > 0 && buffer[pos - 1] == ' ') {
        buffer[pos - 1] = '\0';
    }
    __android_log_print(level, tag, "%s", buffer);
}
