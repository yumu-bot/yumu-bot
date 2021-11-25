package com.now.nowbot.aop;

import com.now.nowbot.config.Permission;
import com.now.nowbot.throwable.LogException;
import com.now.nowbot.throwable.TipsException;
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
    Permission permission;
    @Autowired
    public CheckAspect(Permission permission){
        this.permission = permission;
    }

    @Pointcut("execution(public void com.now.nowbot.service.MessageService..HandleMessage(net.mamoe.mirai.event.events.MessageEvent,java.util.regex.Matcher)) && @within(org.springframework.stereotype.Service)")
    public void servicePoint(){
    }

    /***
     * 注解权限切点
     * @param point
     * @param CheckPermission
     * @return
     * @throws TipsException
     */
    @Before("@annotation(CheckPermission)")
    public Object checkPermission(JoinPoint point, @NotNull CheckPermission CheckPermission) throws Exception {
        var args = point.getArgs();
        var event = (MessageEvent)args[0];
        var servicename = AopUtils.getTargetClass(point.getTarget()).getAnnotation(Service.class).value();

        if (CheckPermission.supperOnly() && !Permission.isSupper(event.getSender().getId())){
            throw new LogException("有人使用最高权限", new RuntimeException(event.getSender().getId()+" -> "+servicename));
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

    @Before("servicePoint()")
    public Object[] checkRepeat(JoinPoint point) throws Exception {
        var args = point.getArgs();
        var event = (MessageEvent) args[0];
        var servicename = AopUtils.getTargetClass(point.getTarget()).getAnnotation(Service.class).value();
        //todo

        if (Permission.isSupper(event.getSender().getId())){
            //超管无视任何限制
            return args;
        }
        if (Permission.isSupper(event.getSender().getId())) {
            return point.getArgs();
        }
        if (permission.containsFriend(servicename, event.getSender().getId())) {
            return point.getArgs();
        }
        if (event instanceof GroupMessageEvent g && !permission.containsGroup(servicename, g.getGroup().getId())){
            return point.getArgs();
        }

        throw new LogException("权限禁止",new Exception(""));
    }

    @After("servicePoint()")
    public void endRepeat(JoinPoint point){
    }
}
