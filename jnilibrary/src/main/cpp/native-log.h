#include <jni.h>
#include <string>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <pthread.h>
// 添加日志头文件
#include <android/log.h>

// 定义日志标签
#define LOG_TAG "NativeLib"
// 定义日志宏，方便使用
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// 以十六进制打印日志的宏
#define LOG_HEX(msg, data, len) __android_log_hex_print(ANDROID_LOG_INFO, LOG_TAG, msg, data, len)

// 辅助函数声明
void __android_log_hex_print(int level, const char* tag, const char* msg, const uint8_t* data, size_t len);
