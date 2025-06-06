//
// Created by feng on 2025/6/6.
//

#ifndef JNICOMM_COMMON_JNI_H
#define JNICOMM_COMMON_JNI_H

#include <jni.h>
#include "jni_log.h"


// 尝试附加当前线程并返回 JNIEnv 指针
JNIEnv* attachCurrentThread(JavaVM* vm) {
    JNIEnv* env = nullptr;
    if (vm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
        LOGE("Failed to attach current thread");
        return nullptr;
    }
    return env;
}



// 检查并处理 Java 异常
void checkAndClearJavaException(JNIEnv* env) {
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
}

#endif //JNICOMM_COMMON_JNI_H
