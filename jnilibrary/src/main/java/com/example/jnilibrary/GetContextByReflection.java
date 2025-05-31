package com.example.jnilibrary;

import android.app.Application;
import android.content.Context;

public class GetContextByReflection {
    /**
     *
     * @return
     */
    public static Context getApplicationContext() {
        try {
            // 获取 ActivityThread 类
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");

            // 获取 currentActivityThread 静态方法
            java.lang.reflect.Method currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread");

            // 调用 currentActivityThread 方法获取 ActivityThread 实例
            Object activityThread = currentActivityThreadMethod.invoke(null);

            // 获取 getApplication 方法
            java.lang.reflect.Method getApplicationMethod = activityThreadClass.getMethod("getApplication");

            // 调用 getApplication 方法获取 Application 实例
            Application application = (Application) getApplicationMethod.invoke(activityThread);

            return application;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
