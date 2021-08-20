package com.now.nowbot.aop;

import com.now.nowbot.config.Permission;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CheckPermissionAspect {
    @Autowired
    Permission permission;
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
            if(!permission.superUser.contains(event.getSender().getId()))
                throw new RuntimeException(event.getSender().getId()+":权限禁止");
        }

        if (CheckPermission.openWF()){
            if (!permission.friendWhitelist.contains(event.getSender().getId()))
                throw new RuntimeException(event.getSender().getId()+":权限禁止");
        }
        if (CheckPermission.openWG()){
            if (event instanceof GroupMessageEvent){
                if (!permission.groupWhitelist.contains(((GroupMessageEvent) event).getGroup().getId()))
                    throw new RuntimeException(((GroupMessageEvent) event).getGroup().getId()+":权限禁止");
            }
        }
        if (CheckPermission.openBF()){
            if (permission.groupBlacklist.contains(event.getSender().getId()))
                throw new RuntimeException(event.getSender().getId()+":权限禁止");
        }
        if(CheckPermission.openBG()){
            if (event instanceof GroupMessageEvent){
                if (permission.groupBlacklist.contains(((GroupMessageEvent) event).getGroup().getId())){
                    throw new RuntimeException(((GroupMessageEvent) event).getGroup().getId()+"权限禁止");
                }
            }
        }
        return args;
    }

}
