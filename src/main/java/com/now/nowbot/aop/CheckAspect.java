package com.now.nowbot.aop;

import com.now.nowbot.config.Permission;
import com.now.nowbot.entity.OsuBindUserLite;
import com.now.nowbot.mapper.ServiceCallRepository;
import com.now.nowbot.mapper.UserProfileMapper;
import com.now.nowbot.model.json.*;
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
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Aspect
@Component
public class CheckAspect {
    private static final Logger log = LoggerFactory.getLogger(CheckAspect.class);
    private static final String USER_PROFILE_KEY = "#user_profile";
    Permission        permission;
    ServiceCallRepository serviceCall;
    UserProfileMapper userProfileMapper;

    @Autowired
    public CheckAspect(Permission permission,
                       ServiceCallRepository serviceCallRepository,
                       UserProfileMapper userProfileMapper) {
        this.permission = permission;
        this.userProfileMapper = userProfileMapper;
        serviceCall = serviceCallRepository;
    }

    static final List<MessageEvent> workList = new CopyOnWriteArrayList<>();

    //所有实现MessageService的HandMessage方法切入点
    @Pointcut("within(com.now.nowbot.service.MessageService+) &&  execution(void HandleMessage(com.now.nowbot.qq.event.MessageEvent, ..))")
    public void servicePoint() {
    }

    @Pointcut("within(org.springframework.web.client.RestTemplate) && !execution(void *(..))")
    public void restTemplate() {
    }

    @Pointcut("execution(* com.now.nowbot.mapper.BindUserMapper.save(..))")
    public void userSave() {
    }

    @Pointcut("execution(* com.now.nowbot.service.osuApiService.OsuBeatmapApiService.*(..)) ||" +
            "execution(* com.now.nowbot.service.osuApiService.OsuUserApiService.*(..)) ||" +
            "execution(* com.now.nowbot.service.osuApiService.OsuMatchApiService.*(..)) ||" +
            "execution(* com.now.nowbot.service.osuApiService.OsuDiscussionApiService.*(..)) ||" +
            "execution(* com.now.nowbot.service.osuApiService.OsuScoreApiService.*(..))")
    public void apiService() {
    }

    @Pointcut("execution(* com.now.nowbot.service.ImageService.get*(..))")
    public void imageService() {
    }


    @Before(value = "userSave()")
    public Object userSaveLogger(JoinPoint point) {
        Object[] args = point.getArgs();
        if (args.length > 0 && args[0] instanceof OsuBindUserLite u) {
            var dobj = point.getSignature();
            log.info("--*-**- 保存用户[{}] ({}), 调用者: {}", u.getOsuID(), u.getOsuName(), dobj.toString());
        }
        return args;
    }

    private static final Map<ServiceLimit, Long> SERVICE_LIMIT_MAP = new ConcurrentHashMap<>();

    @Before("servicePoint() && @annotation(ServiceLimit)")
    public Object serviceLimit(JoinPoint point, ServiceLimit ServiceLimit) {
        var limit = ServiceLimit.limit();
        if (limit == 0) return point.getArgs();
        var now = System.currentTimeMillis();
        var time = SERVICE_LIMIT_MAP.getOrDefault(ServiceLimit, 0L);
        if (now - time > limit) {
            SERVICE_LIMIT_MAP.put(ServiceLimit, now);
            return point.getArgs();
        }
        throw new PermissionException("请求过于频繁");
    }

    /***
     * 注解权限切点
     * 加了@CheckPermission注解的
     * @throws TipsException
     */
//    @Before(value = "@annotation(CheckPermission) && @target(Service)", argNames = "point,CheckPermission,Service")
    public Object checkPermission(JoinPoint point, CheckPermission CheckPermission, Service Service) throws Exception {
        if (ContextUtil.isBreakAop()) {
            return point.getArgs();
        }
        var args = point.getArgs();
        var event = (MessageEvent) args[0];
        var servicename = Service.value();

        if (Permission.isSuperAdmin(event.getSender().getId())) {
            //超管无视任何限制
            return args;
        }
        //超管权限判断
        if (CheckPermission.isGroupAdmin()) {
            if (event.getSender() instanceof GroupContact groupUser && ! (groupUser.getRoll().equals(Role.ADMIN) || groupUser.getRoll().equals(Role.OWNER))) {
                throw new PermissionException(STR."\{servicename}非管理员使用管理功能", STR."\{event.getSender().getId()} -> \{servicename}");
            }
        }
        if (CheckPermission.isSuperAdmin()) {
            throw new PermissionException(STR."\{servicename}使用超管功能", STR."\{event.getSender().getId()} -> \{servicename}");
        }
        // test 功能
        if (CheckPermission.test() && ! Permission.isTester(event.getSender().getId())) {
            throw new PermissionException(STR."\{servicename}有人使用测试功能 ", STR."\{event.getSender().getId()} -> \{servicename}");
        }
        //服务权限判断
        //白/黑名单
        if (CheckPermission.isWhite()) {
            if (CheckPermission.friend() && ! permission.hasUser(servicename, event.getSender().getId())) {
                throw new PermissionException(STR."\{servicename} 白名单过滤(个人)", STR."\{event.getSender().getId()} -> \{servicename}");
            }
            if (CheckPermission.group() && event instanceof GroupMessageEvent g && ! permission.hasGroup(servicename, g.getGroup().getId())) {
                throw new PermissionException(STR."\{servicename} 白名单过滤(群组)", STR."\{event.getSender().getId()} -> \{servicename}");
            }
        } else {
            if (CheckPermission.friend() && permission.hasUser(servicename, event.getSender().getId())) {
                throw new PermissionException(STR."\{servicename} 黑名单过滤(个人)", STR."\{event.getSender().getId()} -> \{servicename}");
            }
            if (CheckPermission.group() && event instanceof GroupMessageEvent g && permission.hasGroup(servicename, g.getGroup().getId())) {
                throw new PermissionException(STR."\{servicename} 黑名单过滤(群组)", STR."\{event.getSender().getId()} -> \{servicename}");
            }
        }
        return args;
    }

    //    @Before("servicePoint() && @target(Service)")
    public Object[] checkRepeat(JoinPoint point, Service Service) throws Exception {
        if (ContextUtil.isBreakAop()) {
            return point.getArgs();
        }
        var args = point.getArgs();
        var event = (MessageEvent) args[0];

        var servicename = Service.value();
//        var servicename = AopUtils.getTargetClass(point.getTarget()).getAnnotation(Service.class).value();
        try {
            if (Permission.isSuperAdmin(event.getSender().getId())) {
                //超管无视任何限制
                return args;
            }
            if (permission.isAllWhite() && permission.containsAllW(event instanceof GroupMessageEvent g ? g.getGroup().getId() : null)) {
                return args;
            }
            // 群跟人的id进行全局黑名单校验
            else if (permission.containsAll(event instanceof GroupMessageEvent g ? g.getGroup().getId() : null, event.getSender().getId())) {
                return args;
            }
            throw new PermissionException("权限禁止", STR."禁止的权限,请求功能: \{servicename} ,请求人: \{event.getSender().getId()}");
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
        if (! CollectionUtils.isEmpty(workList)) {
            var s = workList.getFirst().getBot().getFriend(2480557535L);
            if (s != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("work").append('\n');
                workList.forEach((event) -> sb
                        .append(event.getSender().getName())
                        .append("(->)")
                        .append(event.getMessage()).append('\n'));
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

    @Around("imageService()")
    public Object beforeGetImage(ProceedingJoinPoint point) throws Throwable {
        var args = point.getArgs();
        for (int i = 0; i < args.length; i++) {
            var param = parse(args[i]);
            if (Objects.nonNull(param)) {
                args[i] = param;
            }
        }
        return point.proceed(args);
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
        if (pjp.getArgs()[0] instanceof MessageEvent e && Objects.nonNull(e)) {
            if (e.getSubject().getId() < 0) {
                log.debug("官方bot [uid {}] 调用 -> {}", - e.getSender().getId(), name);
            } else {
                log.debug("[{}] 调用 -> {}", e.getSender().getId(), name);
            }
        }
        try {
            pjp.proceed(pjp.getArgs());
        } finally {
            long end = System.currentTimeMillis();
            long work = end - now;
            serviceCall.saveCall(name, work);
        }
    }

    static final long MY_ID = 17064371L;

    private static boolean isMyScore(LazerScore score) {
        return score.getUserID() == MY_ID;
    }

    private OsuUser getUser(OsuUser user) {
        if (Objects.isNull(user.getUserID())) return user;
        var data = userProfileMapper.findTopByUserId(user.getUserID());

        return data.map(profile -> {
            var result = OsuUserPlus.copyOf(user);
            result.setProfile(profile);
            return (OsuUser) result;
        }).orElse(user);
    }

    private Object parse(Object param) {
        if (param instanceof OsuUser user) {
            return getUser(user);
        } else if (
                param instanceof Optional<?> opt
                        && opt.isPresent()
                        && opt.get() instanceof OsuUser user
        ) {
            return Optional.ofNullable(getUser(user));
        }
        if (param instanceof LazerScore score) {
            return getScore(score);
        } else if (
                param instanceof Optional<?> opt
                        && opt.isPresent()
                        && opt.get() instanceof LazerScore score
        ) {
            return Optional.ofNullable(getScore(score));
        }

        return null;
    }

    private LazerScore getScore(LazerScore score) {
        if (score == null || score.getUser().getUserID() == null) return score;
        var data = userProfileMapper.findTopByUserId(score.getUser().getUserID());

        return data.map(profile -> {
            var result = ScoreWithUserProfile.copyOf(score);
            result.setProfile(profile);
            return (LazerScore) result;
        }).orElse(score);
    }

    @Pointcut("execution(* com.now.nowbot.service.osuApiService.OsuScoreApiService.*(..))")
    public void scoreApi() {
    }

    @AfterReturning(value = "scoreApi()", returning = "result")
    public void afterApiService(Object result) {
        if (result instanceof BeatmapUserScore s && isMyScore(s.getScore())) {
            s.getScore().setRank("X");
        } else if (result instanceof List<?> l) {
            if (l.isEmpty()) return;
            if (l.getFirst() instanceof LazerScore s && isMyScore(s)) {
                for (var i : l) {
                    ((LazerScore)i).setRank("X");
                }
            }
        }
    }
}
