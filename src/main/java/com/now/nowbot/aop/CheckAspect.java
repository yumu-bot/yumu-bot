package com.now.nowbot.aop;

import com.now.nowbot.config.Permission;
import com.now.nowbot.throwable.LogException;
import com.now.nowbot.throwable.TipsException;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.jetbrains.annotations.NotNull;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

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
    public Object checkPermission(@NotNull JoinPoint point, @NotNull CheckPermission CheckPermission) throws LogException {
        var args = point.getArgs();
        var event = (MessageEvent)args[0];
        var servicename = AopUtils.getTargetClass(point.getTarget()).getAnnotation(Service.class).value();

        if (!(event instanceof GroupMessageEvent)){
            if (!permission.containsFriend(servicename, event.getSender().getId())) {
                throw new LogException("已关闭 个人", new RuntimeException(event.getSender().getId()+" -> "+servicename));
            }
        }else {
            if (!permission.containsGroup(servicename, ((GroupMessageEvent) event).getGroup().getId())) {
                throw new LogException("已关闭 群组", new RuntimeException(((GroupMessageEvent) event).getGroup().getId()+" -> "+servicename));
            }
        }
//        if (CheckPermission.isBotSuper()){
//            if(!Permission.superUser.contains(event.getSender().getId()))
//                throw new TipsException("此功能已关闭");
//        }
//
//        if (CheckPermission.openWF()){
//            if (!permission.F_Whitelist.contains(event.getSender().getId()))
//                throw new TipsException("此功能已关闭");
//        }
//        if (CheckPermission.openWG()){
//            if (event instanceof GroupMessageEvent){
//                if (!permission.G_Whitelist.contains(((GroupMessageEvent) event).getGroup().getId()))
//                    throw new TipsException("此功能已关闭");
//            }
//        }
//        if (CheckPermission.openBF()){
//            if (permission.List.contains(event.getSender().getId()))
//                throw new TipsException("此功能已关闭");
//        }
//        if(CheckPermission.openBG()){
//            if (event instanceof GroupMessageEvent){
//                if (permission.List.contains(((GroupMessageEvent) event).getGroup().getId())){
//                    throw new TipsException("此功能已关闭");
//                }
//            }
//        }
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
