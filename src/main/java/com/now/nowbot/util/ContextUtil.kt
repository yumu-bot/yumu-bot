package com.now.nowbot.util

import java.util.concurrent.ConcurrentHashMap

object ContextUtil {
    var threadLocalService: ThreadLocal<MutableMap<String, Any>> = ThreadLocal<MutableMap<String, Any>>()

    @JvmStatic
    fun <T> getContext(name: String?, tClass: Class<T>): T? {
        if (threadLocalService.get() == null || threadLocalService.get()!![name] == null) return null
        val obj = threadLocalService.get()!![name]
        // 判断 obj 是否是 t 类型的实例
        if (!tClass.isInstance(obj)) return null
        return tClass.cast(threadLocalService.get()!![name])
    }

    @JvmStatic
    fun <T> getContext(name: String?, def: T?, tClass: Class<T>): T? {
        if (threadLocalService.get() == null || threadLocalService.get()!![name] == null) return def
        val obj = threadLocalService.get()!![name]
        // 判断 obj 是否是 t 类型的实例
        if (!tClass.isInstance(obj)) return def
        return tClass.cast(threadLocalService.get()!![name])
    }

    @JvmStatic
    fun setContext(name: String?, o: Any?) {
        if (threadLocalService.get() == null) {
            threadLocalService.set(ConcurrentHashMap<String, Any>())
        }
        if (o == null) {
            threadLocalService.get()!!.remove(name)
            if (threadLocalService.get()!!.isEmpty()) {
                threadLocalService.remove()
            }
        } else {
            name?.let { threadLocalService.get()!!.put(it, o)}
        }
    }

    val isTestUser
        get() = getContext(
            "isTest",
            java.lang.Boolean.FALSE,
            Boolean::class.java
        )!!

    @JvmStatic
    val isBreakAop
        get() = getContext("break aop", Any::class.java) != null

    fun breakAop() {
        setContext("break aop", Any())
    }

    fun remove() {
        threadLocalService.remove()
    }
}
