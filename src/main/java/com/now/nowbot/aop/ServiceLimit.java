package com.now.nowbot.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceLimit {
    // 限制次数, 毫秒数内不能再次调用, 0为不限制
    int limit() default 0;
}
