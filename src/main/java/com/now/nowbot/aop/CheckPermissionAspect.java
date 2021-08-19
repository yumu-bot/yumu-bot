package com.now.nowbot.aop;

import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
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

    @Before("(annotatedClasses() || annotatedMethods()) && @annotation(CheckPermission)")
    public Object checkPermission(@NotNull JoinPoint point, @NotNull CheckPermission CheckPermission){
        var args = point.getArgs();
        var event = (MessageEvent)args[0];

        if (CheckPermission.isBotSuper()){
            event.getSubject().sendMessage("权限禁止");
            throw new RuntimeException("权限禁止");
        }

        if(CheckPermission.openBG()){
            if (event instanceof GroupMessageEvent){
                if (((GroupMessageEvent) event).getGroup().getId() == 928936255L){
                    event.getSubject().sendMessage("权限禁止");
                    throw new RuntimeException("权限禁止");
                }
            }
        }
        return args;
    }

}
