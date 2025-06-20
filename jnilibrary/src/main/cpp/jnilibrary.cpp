#include <jni.h>
#include "native-log.h"
#include "ProtocolDecoder.h"
#include "ProtocolEncoder.h"
#include <vector>
#include <mutex>
#include <condition_variable>

// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("jnilibrary");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("jnilibrary")
//      }
//    }


static JavaVM *g_vm = nullptr;
static jclass g_clazz = nullptr;
static jmethodID g_method = nullptr;

// 线程同步
static std::mutex g_mutex;
static std::condition_variable g_cv;
static bool g_allDataReceived = false;


#define CLASS_NATIVE_API "com/example/jnilibrary/NativeApi"
#define RECV_METHOD "onNativeDataReceived"


MessageResponse waitForResponse() {
    std::unique_lock<std::mutex> lock(g_mutex);
    g_cv.wait(lock, [] { return g_allDataReceived; });
    return ProtocolDecoder::getInstance().decode();
}


extern "C"
void sendDataToJava(JNIEnv *env, std::vector<unsigned char> data) {
    if (nullptr == env) {
        g_vm->AttachCurrentThread(&env, nullptr);
        LOGE("env is nullptr, attached now");
    }

    // 创建 jbyteArray，长度为 data 的长度
    jbyteArray byteArray = env->NewByteArray(data.size());
    if (byteArray == nullptr) {
        LOGE("Failed to create jbyteArray");
        return;
    }

    env->SetByteArrayRegion(byteArray, 0, data.size(),
                            reinterpret_cast<const jbyte *>(data.data()));
    // 调用 Java 静态方法，传入 jbyteArray
    env->CallStaticVoidMethod(g_clazz, g_method, byteArray);

    // 删除局部引用，避免内存泄漏
    env->DeleteLocalRef(byteArray);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_jnilibrary_NativeApi_initNative(JNIEnv *env, jclass clazz) {

    env->GetJavaVM(&g_vm);

    g_clazz = (jclass) env->FindClass(CLASS_NATIVE_API);
    if (nullptr == clazz) {
        LOGE("Class not found");
        return;
    }

    g_method = env->GetStaticMethodID(g_clazz, RECV_METHOD, "([B)V");
    if (nullptr == g_method) {
        LOGE("recv method not found");
        return;
    }
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_jnilibrary_NativeApi_getFeatures(JNIEnv *env, jclass clazz) {

    std::vector<unsigned char> emptyUCharVector;
    std::vector<uint8_t> cmd = ProtocolEncoder::getInstance().encodeProtocol(0, emptyUCharVector);
    sendDataToJava(env, cmd);

    // 等待 Java 层返回响应
    MessageResponse response = waitForResponse();

    LOG_HEX("[C++] Response", response.data.data(), response.data.size());
    // 转换为 jstring
    std::vector<unsigned char> receivedData = response.data;

    // 创建 jbyteArray
    jbyteArray byteArray = env->NewByteArray(receivedData.size());
    if (byteArray == nullptr) {
        LOGE("Failed to create jbyteArray");
        return nullptr;
    }

    env->SetByteArrayRegion(byteArray, 0, receivedData.size(),
                            reinterpret_cast<const jbyte *>(receivedData.data()));

    return byteArray;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_example_jnilibrary_NativeApi_sendDataToNative(JNIEnv *env, jclass clazz, jbyteArray data) {
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);
    jint length = env->GetArrayLength(data);
    LOG_HEX("[C++] Received", reinterpret_cast<const uint8_t *>(bytes), length);

    g_allDataReceived = ProtocolDecoder::getInstance().packetCompletionCheck(
            reinterpret_cast<const uint8_t *>(bytes), length);
    if (!g_allDataReceived) {
        return;
    }
    LOGD("[C++] 接收完成");
    // 唤醒等待线程
    g_cv.notify_one();

    env->ReleaseByteArrayElements(data, bytes, 0);
}