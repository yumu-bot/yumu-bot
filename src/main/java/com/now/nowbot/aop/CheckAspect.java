package com.now.nowbot.aop;

import com.now.nowbot.config.Permission;
import com.now.nowbot.throwable.LogException;
import com.now.nowbot.throwable.PermissionException;
import com.now.nowbot.throwable.TipsException;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@Aspect
@Component
public class CheckAspect {
    Permission permission;
    @Autowired
    public CheckAspect(Permission permission){
        this.permission = permission;
    }
    static final List<MessageEvent> workList = new CopyOnWriteArrayList<>();

    //所有实现MessageService的HandMessage方法切入点
    @Pointcut("execution(public void com.now.nowbot.service.MessageService..HandleMessage(net.mamoe.mirai.event.events.MessageEvent,java.util.regex.Matcher))")
    public void servicePoint(){
    }

    /***
     * 注解权限切点
     * 加了@CheckPermission注解的
     * @param point
     * @param CheckPermission
     * @return
     * @throws TipsException
     */
    @Before(value = "@annotation(CheckPermission) && @target(Service)", argNames = "point,CheckPermission,Service")
    public Object checkPermission(JoinPoint point, CheckPermission CheckPermission, Service Service) throws Exception {
        var args = point.getArgs();
        var event = (MessageEvent)args[0];
        var servicename = Service.value();

        if (Permission.isSupper(event.getSender().getId())){
            //超管无视任何限制
            return args;
        }
        //超管权限判断
        if (CheckPermission.supperOnly()){
            throw new PermissionException(servicename + "有人使用最高权限 "+ event.getSender().getId()+" -> "+servicename);
        }
        // test 功能
        if (CheckPermission.test() && !permission.isTester(event.getSender().getId())) {
            throw new PermissionException(servicename + "有人使用测试功能 "+ event.getSender().getId()+" -> "+servicename);
        }
        //服务权限判断
        //白/黑名单
        if (CheckPermission.isWhite()){
            if (CheckPermission.friend() && !permission.containsFriend(servicename, event.getSender().getId())){
                throw new PermissionException(servicename + " 白名单过滤(个人)", event.getSender().getId()+" -> "+servicename);
            }
            if (CheckPermission.group() && event instanceof GroupMessageEvent g && !permission.containsGroup(servicename, g.getGroup().getId())){
                throw new PermissionException(servicename + " 白名单过滤(群组)", event.getSender().getId()+" -> "+servicename);
            }
        }else {
            if (CheckPermission.friend() && permission.containsFriend(servicename, event.getSender().getId())){
                throw new PermissionException(servicename + " 黑名单过滤(个人)", event.getSender().getId()+" -> "+servicename);
            }
            if (CheckPermission.group() && event instanceof GroupMessageEvent g && permission.containsGroup(servicename, g.getGroup().getId())){
                throw new PermissionException(servicename + " 黑名单过滤(群组)", event.getSender().getId()+" -> "+servicename);
            }
        }
        return args;
    }

    @Before("servicePoint() && @target(Service)")
    public Object[] checkRepeat(JoinPoint point, Service Service) throws Exception {
        var args = point.getArgs();
        var event = (MessageEvent) args[0];

        var servicename = Service.value();
//        var servicename = AopUtils.getTargetClass(point.getTarget()).getAnnotation(Service.class).value();
        try {
            if (Permission.isSupper(event.getSender().getId())){
                //超管无视任何限制
                return args;
            }
            if (permission.allIsWhite() && permission.containsAllW(event instanceof GroupMessageEvent g ? g.getGroup().getId() : null)){
                return args;
            }
            // 群跟人的id进行全局黑名单校验
            else if (permission.containsAll(event instanceof GroupMessageEvent g ? g.getGroup().getId() : null, event.getSender().getId())){
                return args;
            }
            throw new PermissionException("权限禁止","禁止的权限,请求功能: "+servicename+" ,请求人: "+event.getSender().getId());
        } finally {
//            workList.add(event);
        }
    }

//    @After("servicePoint() && @target(Service)")
//    public void endRepeat(JoinPoint point, Service Service){
//        var event = (MessageEvent) point.getArgs()[0];
//        workList.remove(event);
//    }

    Set<Contact> sended;
    public void doEnd(){
        sended = new HashSet<>();
        if (workList.size()>0) {
            var s = workList.get(0).getBot().getFriend(2480557535L);
            if (s != null){
                StringBuilder sb = new StringBuilder();
                sb.append("work").append('\n');
                workList.forEach((event) -> {
                    sb.append(event.getSenderName()).append("(->)").append(event.getMessage().contentToString()).append('\n');
                });
                s.sendMessage(sb.toString());
            }
        }
        workList.forEach(this::sendWorn);
    }
    public void sendWorn(MessageEvent event){
        var s = event.getSubject();
        if (sended.add(s)){
            s.sendMessage("bot即将重启,放弃所有未完成任务,请稍后重试(具体时间请联系管理员)");
        }
    }
//    @Around("execution (public * com.now.nowbot..*(..))")
//        public Object setConttext(JoinPoint point){
//        point.getSignature();
//        return null;
//    }
}
