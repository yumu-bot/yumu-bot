package com.now.nowbot.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContextUtil {
    static ThreadLocal<Map<String, Object>> threadLocalService = new ThreadLocal<>();

    public static <T> T getContext(String name, Class<T> tClass){
        return tClass.cast( threadLocalService.get().get(name) );
    }

    public static void setContext(String name, Object o){
        if (threadLocalService.get() == null){
            threadLocalService.set(new ConcurrentHashMap<>());
        }
        threadLocalService.get().put(name, o);
    }

    public static void remove(){
        threadLocalService.remove();
    }
}
