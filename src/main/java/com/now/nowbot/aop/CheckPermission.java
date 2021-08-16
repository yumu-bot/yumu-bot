package com.now.nowbot.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckPermission {

    String[] roles() default {};

    String[] noRoles() default {};

    boolean isGroupOwnerOnly() default false;

    boolean isAdminOnly() default false;

    String description() default "权限校验注解";
}
