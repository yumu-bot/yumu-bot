package com.now.nowbot.aop

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ServiceLimit( // 限制次数, 毫秒数内不能再次调用, 0为不限制
    val cooldownMillis: Long = 0L
)
