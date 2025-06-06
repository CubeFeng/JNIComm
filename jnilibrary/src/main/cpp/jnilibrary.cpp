#include <jni.h>
#include "jni_log.h"
#include "hex_utils.h"
#include "common_jni.h"
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
static std::vector<unsigned char> g_receivedData;


#define CLASS_NATIVE_API "com/example/jnilibrary/NativeApi"
#define RECV_METHOD "onNativeDataReceived"

extern "C"
void sendDataToJava(JNIEnv *env, std::string data) {
    if (g_vm == nullptr) {
        LOGE("JavaVM is nullptr, cannot attach thread");
        return;
    }

    // 检查类和方法是否已经初始化
    if (g_clazz == nullptr || g_method == nullptr) {
        LOGE("Class or method ID is nullptr, call initNative first");
        return;
    }


    // 如果 env 为空，尝试附加当前线程
    if (env == nullptr) {
        if (g_vm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE("Failed to attach current thread");
            return;
        }
        // 确保在函数结束时分离线程
        struct ThreadDetacher {
            JavaVM* vm;
            ~ThreadDetacher() {
                if (vm) {
                    vm->DetachCurrentThread();
                }
            }
        } detacher{g_vm};
    }

    // 十六进制字符串转字节数组
    std::vector<unsigned char> byteData;
    try {
        byteData = hexStringToByteArray(data);
    } catch (const std::exception& e) {
        LOGE("%s", e.what());
        return;
    }

    // 创建 jbyteArray，长度为 data 的长度
    jbyteArray byteArray = env->NewByteArray(byteData.size());
    if (byteArray == nullptr) {
        LOGE("Failed to create jbyteArray");
        return;
    }

    env->SetByteArrayRegion(byteArray, 0, byteData.size(), reinterpret_cast<const jbyte*>(byteData.data()));
    // 调用 Java 静态方法，传入 jbyteArray
    env->CallStaticVoidMethod(g_clazz, g_method, byteArray);

    // 检查并处理 Java 异常
    checkAndClearJavaException(env);

    // 删除局部引用，避免内存泄漏
    env->DeleteLocalRef(byteArray);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_jnilibrary_NativeApi_initNative(JNIEnv *env, jclass clazz) {

    env->GetJavaVM(&g_vm);

    g_clazz = (jclass)env->FindClass(CLASS_NATIVE_API);
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
    std::string cmd = "3F232300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";

    sendDataToJava(env, cmd);

    // 等待 Java 层返回响应
    {
        std::unique_lock<std::mutex> lock(g_mutex);
        g_cv.wait(lock, []{ return !g_receivedData.empty(); });
    }

    LOGD("[C++] response: %s", g_receivedData.data());

    // 创建 jbyteArray
    jbyteArray byteArray = env->NewByteArray(g_receivedData.size());
    if (byteArray == nullptr) {
        LOGE("Failed to create jbyteArray");
        return nullptr;
    }

    env->SetByteArrayRegion(byteArray, 0, g_receivedData.size(), reinterpret_cast<const jbyte*>(g_receivedData.data()));

    return byteArray;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_example_jnilibrary_NativeApi_sendDataToNative(JNIEnv *env, jclass clazz, jbyteArray data) {
    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    jint length = env->GetArrayLength(data);
    LOGD("[C++] Received data: %.*s", length, reinterpret_cast<const char*>(bytes));

    // 保存接收到的数据
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        g_receivedData.insert(g_receivedData.end(), bytes, bytes + length);
    }
    // 唤醒等待线程
    g_cv.notify_one();

    env->ReleaseByteArrayElements(data, bytes, 0);
}