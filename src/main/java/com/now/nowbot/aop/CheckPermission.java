package com.now.nowbot.aop;

import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.event.Event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)

public @interface CheckPermission {
    boolean isWhite() default false;

    boolean froup() default true;

    boolean friend() default true;

    /***
     * 仅允许超级管理员使用
     * @return
     */
    boolean isSuper() default true;

    String ServiceName() default "public";
}
