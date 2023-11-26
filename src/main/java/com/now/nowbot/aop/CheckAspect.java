package com.now.nowbot.aop;

import com.now.nowbot.config.Permission;
import com.now.nowbot.entity.OsuBindUserLite;
import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.enums.Role;
import com.now.nowbot.qq.event.GroupMessageEvent;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.onebot.contact.GroupContact;
import com.now.nowbot.throwable.PermissionException;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.ContextUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@Aspect
@Component
public class CheckAspect {
    private static final Logger log = LoggerFactory.getLogger(CheckAspect.class);
    Permission permission;

    @Autowired
    public CheckAspect(Permission permission) {
        this.permission = permission;
    }

    static final List<MessageEvent> workList = new CopyOnWriteArrayList<>();

    //所有实现MessageService的HandMessage方法切入点
    @Pointcut("within(com.now.nowbot.service.MessageService+) &&  execution(void HandleMessage(com.now.nowbot.qq.event.MessageEvent, ..))")
    public void servicePoint() {}

    @Pointcut("within(org.springframework.web.client.RestTemplate) && !execution(void *(..))")
    public void restTemplate() {}

    @Pointcut("execution(* com.now.nowbot.mapper.BindUserMapper.save(..))")
    public void userSave() {}

    @Pointcut("execution(* com.now.nowbot.service.OsuApiService.OsuBeatmapApiService.*(..)) ||" +
            "execution(* com.now.nowbot.service.OsuApiService.OsuUserApiService.*(..)) ||" +
            "execution(* com.now.nowbot.service.OsuApiService.OsuMatchApiService.*(..)) ||" +
            "execution(* com.now.nowbot.service.OsuApiService.OsuScoreApiService.*(..))")
    public void apiService() {}


    @Before(value = "userSave()")
    public Object userSaveLogger(JoinPoint point) {
        Object[] args = point.getArgs();
        if (args.length > 0 && args[0] instanceof OsuBindUserLite u) {
            var dobj = point.getSignature();
            log.info("--*-**- 保存用户[{}] ({}), 调用者: {}", u.getOsuId(), u.getOsuName(), dobj.toString());
        }
        return args;
    }

    /***
     * 注解权限切点
     * 加了@CheckPermission注解的
     * @throws TipsException
     */
    @Before(value = "@annotation(CheckPermission) && @target(Service)", argNames = "point,CheckPermission,Service")
    public Object checkPermission(JoinPoint point, CheckPermission CheckPermission, Service Service) throws Exception {
        if (ContextUtil.isBreakAop()) {
            return point.getArgs();
        }
        var args = point.getArgs();
        var event = (MessageEvent) args[0];
        var servicename = Service.value();

        if (Permission.isSuper(event.getSender().getId())) {
            //超管无视任何限制
            return args;
        }
        //超管权限判断
        if (CheckPermission.isGroupAdmin()) {
            if (event.getSender() instanceof GroupContact groupUser && !(groupUser.getRoll().equals(Role.ADMIN) || groupUser.getRoll().equals(Role.OWNER))) {
                throw new PermissionException(servicename + "非管理员使用管理功能", event.getSender().getId() + " -> " + servicename);
            }
        }
        if (CheckPermission.isSuperAdmin()) {
            throw new PermissionException(servicename + "使用超管功能", event.getSender().getId() + " -> " + servicename);
        }
        // test 功能
        if (CheckPermission.test() && !Permission.isTester(event.getSender().getId())) {
            throw new PermissionException(servicename + "有人使用测试功能 ", event.getSender().getId() + " -> " + servicename);
        }
        //服务权限判断
        //白/黑名单
        if (CheckPermission.isWhite()) {
            if (CheckPermission.friend() && !permission.containsFriend(servicename, event.getSender().getId())) {
                throw new PermissionException(servicename + " 白名单过滤(个人)", event.getSender().getId() + " -> " + servicename);
            }
            if (CheckPermission.group() && event instanceof GroupMessageEvent g && !permission.containsGroup(servicename, g.getGroup().getId())) {
                throw new PermissionException(servicename + " 白名单过滤(群组)", event.getSender().getId() + " -> " + servicename);
            }
        } else {
            if (CheckPermission.friend() && permission.containsFriend(servicename, event.getSender().getId())) {
                throw new PermissionException(servicename + " 黑名单过滤(个人)", event.getSender().getId() + " -> " + servicename);
            }
            if (CheckPermission.group() && event instanceof GroupMessageEvent g && permission.containsGroup(servicename, g.getGroup().getId())) {
                throw new PermissionException(servicename + " 黑名单过滤(群组)", event.getSender().getId() + " -> " + servicename);
            }
        }
        return args;
    }

    @Before("servicePoint() && @target(Service)")
    public Object[] checkRepeat(JoinPoint point, Service Service) throws Exception {
        if (ContextUtil.isBreakAop()) {
            return point.getArgs();
        }
        var args = point.getArgs();
        var event = (MessageEvent) args[0];

        var servicename = Service.value();
//        var servicename = AopUtils.getTargetClass(point.getTarget()).getAnnotation(Service.class).value();
        try {
            if (Permission.isSuper(event.getSender().getId())) {
                //超管无视任何限制
                return args;
            }
            if (permission.allIsWhite() && permission.containsAllW(event instanceof GroupMessageEvent g ? g.getGroup().getId() : null)) {
                return args;
            }
            // 群跟人的id进行全局黑名单校验
            else if (permission.containsAll(event instanceof GroupMessageEvent g ? g.getGroup().getId() : null, event.getSender().getId())) {
                return args;
            }
            throw new PermissionException("权限禁止", "禁止的权限,请求功能: " + servicename + " ,请求人: " + event.getSender().getId());
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

    public void doEnd() {
        sended = new HashSet<>();
        if (workList.size() > 0) {
            var s = workList.get(0).getBot().getFriend(2480557535L);
            if (s != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("work").append('\n');
                workList.forEach((event) -> {
                    sb.append(event.getSender().getName()).append("(->)").append(event.getMessage()).append('\n');
                });
                s.sendMessage(sb.toString());
            }
        }
        workList.forEach(this::sendWorn);
    }

    public void sendWorn(MessageEvent event) {
        var s = event.getSubject();
        if (sended.add(s)) {
            s.sendMessage("bot即将重启,放弃所有未完成任务,请稍后重试(具体时间请联系管理员)");
        }
    }

    //    @Around(value = "execution (public * com.now.nowbot..*(..))", argNames = "pjp,point")
    @Around(value = "servicePoint()", argNames = "pjp")
    public void setContext(ProceedingJoinPoint pjp) throws Throwable {
        long now = System.currentTimeMillis();
        var ser = pjp.getTarget().getClass().getAnnotation(Service.class);
        String name = "unknown";
        if (ser != null) {
            name = ser.value();
        }
        if (pjp.getArgs()[0] instanceof MessageEvent e) {
            log.debug("[{}] 调用 -> {}", e.getSender().getId(), name);
        }
        pjp.proceed(pjp.getArgs());
        long end = System.currentTimeMillis();
        if (end - now > 3000) {
            log.debug("[{}] 执行结束,用时:{}", name, end - now);
        }
    }

    @Around(value = "apiService()")
    public Object doRetry(ProceedingJoinPoint joinPoint) {
        return Mono.defer(() -> {
                    try {
                        return Mono.just(joinPoint.proceed(joinPoint.getArgs()));
                    } catch (Throwable e) {
                        return Mono.error(e);
                    }
                })
                .retryWhen(Retry
                        .backoff(3, Duration.ofSeconds(2))
                        .jitter(0.75)
                        .doAfterRetry(a -> log.warn("Retrying request"))
                        .filter(e -> e instanceof WebClientException)
                        .filter(e -> !(e instanceof WebClientResponseException.NotFound))
                )
                .block();
    }
}
