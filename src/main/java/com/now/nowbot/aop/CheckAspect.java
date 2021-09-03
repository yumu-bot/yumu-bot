package com.now.nowbot.aop;

import com.now.nowbot.config.Permission;
import com.now.nowbot.error.TipsError;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CheckAspect {
    @Autowired
    Permission permission;
    @Pointcut("@annotation(com.now.nowbot.aop.CheckPermission)")
    public void annotatedMethodsPerm() {
    }

    @Pointcut("@within(com.now.nowbot.aop.CheckPermission)")
    public void annotatedClassesPerm() {
    }

    @Pointcut("target(com.now.nowbot.service.MessageService.MessageService)))")
    public void annotatedMethodsRep(){
    }

    @Before("(annotatedClassesPerm() || annotatedMethodsPerm()) && @annotation(CheckPermission)")
    public Object checkPermission(@NotNull JoinPoint point, @NotNull CheckPermission CheckPermission) throws TipsError{
        var args = point.getArgs();
        var event = (MessageEvent)args[0];

        if (CheckPermission.isBotSuper()){
            if(!Permission.superUser.contains(event.getSender().getId()))
                throw new TipsError("此功能已关闭");
        }

        if (CheckPermission.openWF()){
            if (!permission.friendWhitelist.contains(event.getSender().getId()))
                throw new TipsError("此功能已关闭");
        }
        if (CheckPermission.openWG()){
            if (event instanceof GroupMessageEvent){
                if (!permission.groupWhitelist.contains(((GroupMessageEvent) event).getGroup().getId()))
                    throw new TipsError("此功能已关闭");
            }
        }
        if (CheckPermission.openBF()){
            if (permission.groupBlacklist.contains(event.getSender().getId()))
                throw new TipsError("此功能已关闭");
        }
        if(CheckPermission.openBG()){
            if (event instanceof GroupMessageEvent){
                if (permission.groupBlacklist.contains(((GroupMessageEvent) event).getGroup().getId())){
                    throw new TipsError("此功能已关闭");
                }
            }
        }
        return args;
    }

    Contact r = null;
    @Before("annotatedMethodsRep()")
    public void checkRepeat(@NotNull JoinPoint point){
        var event = (MessageEvent)point.getArgs()[0];
        //todo
    }

    @After("annotatedMethodsRep()")
    public void endRepeat(JoinPoint point){
    }
}
