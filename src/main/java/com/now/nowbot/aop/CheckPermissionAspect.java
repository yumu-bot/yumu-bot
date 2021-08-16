package com.now.nowbot.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CheckPermissionAspect {
    @Pointcut("@annotation(com.now.nowbot.aop.CheckPermission)")
    public void annotatedMethods() {
    }

    @Pointcut("@within(com.now.nowbot.aop.CheckPermission)")
    public void annotatedClasses() {
    }
    @Before("annotatedClasses() || annotatedMethods()")
    public Object checkPermission(@NotNull JoinPoint point){
        var args = point.getArgs();

//        MessageEvent e = (MessageEvent) args[1];
        System.out.println(args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.println(point.getTarget().toString()+(int)args[i]);
        }
        return args;
    }
}
