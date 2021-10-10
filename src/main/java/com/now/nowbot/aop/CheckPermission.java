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
    /***
     * 黑/白 名单类型 true白  false黑
     * @return
     */
    boolean isWhite() default false;

    /***
     * 群组记录
     * @return
     */
    boolean group() default true;

    /***
     * 个人记录
     * @return
     */
    boolean friend() default true;

    /***
     * true群主/管理员可修改 false 禁止修改
     * @return false
     */
    boolean isSuper() default false;

}
