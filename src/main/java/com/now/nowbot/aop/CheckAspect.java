package com.now.nowbot.aop;

import com.now.nowbot.config.Permission;
import com.now.nowbot.throwable.LogException;
import com.now.nowbot.throwable.TipsException;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Image;
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

    /***
     * 注解权限切点
     * @param point
     * @param CheckPermission
     * @return
     * @throws TipsException
     */
    @Before("(annotatedClassesPerm() || annotatedMethodsPerm()) && @annotation(CheckPermission)")
    public Object checkPermission(@NotNull JoinPoint point, @NotNull CheckPermission CheckPermission) throws TipsException, LogException {
        var args = point.getArgs();
        var event = (MessageEvent)args[0];
//临时关闭群图片
//        if(event instanceof GroupMessageEvent){
//            Image img = (Image) event.getMessage().stream().filter(it -> it instanceof Image).findFirst().orElse(null);
//            if (img != null){
//                throw new LogException("暂停向群内发图片",null);
//            }
//        }
        if (CheckPermission.isBotSuper()){
            if(!Permission.superUser.contains(event.getSender().getId()))
                throw new TipsException("此功能已关闭");
        }

        if (CheckPermission.openWF()){
            if (!permission.friendWhitelist.contains(event.getSender().getId()))
                throw new TipsException("此功能已关闭");
        }
        if (CheckPermission.openWG()){
            if (event instanceof GroupMessageEvent){
                if (!permission.groupWhitelist.contains(((GroupMessageEvent) event).getGroup().getId()))
                    throw new TipsException("此功能已关闭");
            }
        }
        if (CheckPermission.openBF()){
            if (permission.groupBlacklist.contains(event.getSender().getId()))
                throw new TipsException("此功能已关闭");
        }
        if(CheckPermission.openBG()){
            if (event instanceof GroupMessageEvent){
                if (permission.groupBlacklist.contains(((GroupMessageEvent) event).getGroup().getId())){
                    throw new TipsException("此功能已关闭");
                }
            }
        }
        return args;
    }

    Contact r = null;
    @Before("annotatedMethodsRep()")
    public void checkRepeat(@NotNull JoinPoint point){
        if (point.getArgs().length>0) {
            var event = (MessageEvent) point.getArgs()[0];
        }
        //todo
    }

    @After("annotatedMethodsRep()")
    public void endRepeat(JoinPoint point){
    }
}
