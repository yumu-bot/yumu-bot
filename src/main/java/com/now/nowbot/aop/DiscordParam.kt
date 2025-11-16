package com.now.nowbot.aop

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CLASS
) @Retention(AnnotationRetention.RUNTIME)
annotation class DiscordParam(val name: String = "", val description: String = "", val required: Boolean = false)
