package com.now.nowbot.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)

public @interface CheckPermission {
    /***
     * 黑/白 名单类型 true白  false黑
     */
    boolean isWhite() default false;

    /***
     * 群组记录
     */
    boolean group() default true;

    /***
     * 个人记录
     */
    boolean friend() default true;

    /***
     * true群主/管理员可修改 false 禁止修改
     * @return false
     */
    boolean userSet() default false;

    /**
     * 超级管理员专用功能
     */
    boolean isSuperAdmin() default false;

    /**
     * 群聊管理员专用功能
     */
    boolean isGroupAdmin() default false;
    boolean test() default false;

}
