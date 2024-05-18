package com.now.nowbot.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContextUtil {
    static ThreadLocal<Map<String, Object>> threadLocalService = new ThreadLocal<>();

    public static <T> T getContext(String name, Class<T> tClass){
        if (threadLocalService.get() == null || threadLocalService.get().get(name) == null) return null;
        Object obj = threadLocalService.get().get(name);
        // 判断 obj 是否是 t 类型的实例
        if (!tClass.isInstance(obj)) return null;
        return tClass.cast( threadLocalService.get().get(name) );
    }

    public static <T> T getContext(String name, T def, Class<T> tClass) {
        if (threadLocalService.get() == null || threadLocalService.get().get(name) == null) return def;
        Object obj = threadLocalService.get().get(name);
        // 判断 obj 是否是 t 类型的实例
        if (! tClass.isInstance(obj)) return def;
        return tClass.cast(threadLocalService.get().get(name));
    }

    public static void setContext(String name, Object o){
        if (threadLocalService.get() == null){
            threadLocalService.set(new ConcurrentHashMap<>());
        }
        threadLocalService.get().put(name, o);
    }

    public static boolean isTestUser() {
        return getContext("isTest", Boolean.FALSE, Boolean.class);
    }

    public static boolean isBreakAop() {
        return ContextUtil.getContext("break aop", Object.class) != null;
    }

    public static void breakAop(){
        ContextUtil.setContext("break aop", new Object());
    }

    public static void remove(){
        threadLocalService.remove();
    }
}
