package com.now.nowbot.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckPermission {

    /***
     * 群组白名单模式，只允许白名单群组使用
     * @return
     */
    boolean openWG() default false;

    /***
     * 群组黑名单模式，黑名单禁用，其他组启用白名单 若同时开启黑/白名单，非白名单禁用且黑名单优先级更高(同时存在于黑/白名单时也禁用
     * @return
     */
    boolean openBG() default false;

    /***
     * 个人白名单 类似
     * @return
     */
    boolean openWF() default false;

    /***
     * 个人黑名单
     * @return
     */
    boolean openBF() default false;

    /***
     * 仅允许机器人超级管理员
     * 优先级最大
     * @return
     */
    boolean isBotSuper() default false;

    /***
     * 仅允许群主及管理员使用
     * @return
     */
    boolean isGroupVip() default false;

    String description() default "权限校验注解";
}
