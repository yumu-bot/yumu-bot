package com.now.nowbot.aop;

import com.now.nowbot.config.Permission;
import com.now.nowbot.dao.ServiceCallStatisticsDao;
import com.now.nowbot.entity.OsuBindUserLite;
import com.now.nowbot.entity.ServiceCallStatisticLite;
import com.now.nowbot.mapper.ServiceCallRepository;
import com.now.nowbot.mapper.UserProfileMapper;
import com.now.nowbot.model.osu.LazerScore;
import com.now.nowbot.model.osu.OsuUser;
import com.now.nowbot.model.osu.OsuUserPlus;
import com.now.nowbot.model.osu.ScoreWithUserProfile;
import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.enums.Role;
import com.now.nowbot.qq.event.GroupMessageEvent;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.onebot.contact.GroupContact;
import com.now.nowbot.throwable.botRuntimeException.PermissionException;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Aspect
@Component
public class CheckAspect {
    private static final Logger log              = LoggerFactory.getLogger(CheckAspect.class);
    private static final String USER_PROFILE_KEY = "#user_profile";
    Permission            permission;
    ServiceCallRepository serviceCall;
    ServiceCallStatisticsDao serviceCallStatisticsDao;
    UserProfileMapper     userProfileMapper;

    @Autowired
    public CheckAspect(Permission permission,
                       ServiceCallRepository serviceCallRepository,
                       ServiceCallStatisticsDao serviceCallStatisticsDao,
                       UserProfileMapper userProfileMapper) {
        this.permission = permission;
        this.serviceCallStatisticsDao = serviceCallStatisticsDao;
        this.userProfileMapper = userProfileMapper;
        serviceCall = serviceCallRepository;
    }

    static final List<MessageEvent> workList = new CopyOnWriteArrayList<>();

    //所有实现MessageService的HandMessage方法切入点
    @Pointcut("within(com.now.nowbot.service.MessageService+) &&  execution(* handleMessage(com.now.nowbot.qq.event.MessageEvent, ..))")
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
            // var dobj = point.getSignature();
            log.info("新增用户：{} ({})", u.getOsuID(), u.getOsuName());
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
     */
//    @Before(value = "@annotation(CheckPermission) && @target(Service)", argNames = "point,CheckPermission,Service")
    public Object checkPermission(JoinPoint point, CheckPermission CheckPermission, Service Service) {
        if (ContextUtil.isBreakAop()) {
            return point.getArgs();
        }
        var args = point.getArgs();
        var event = (MessageEvent) args[0];
        var name = Service.value();

        var qq = event.getSender().getId();

        if (Permission.isSuperAdmin(qq)) {
            //超管无视任何限制
            return args;
        }
        //超管权限判断
        if (CheckPermission.isGroupAdmin()) {
            if (event.getSender() instanceof GroupContact groupUser && !(groupUser.getRole().equals(Role.ADMIN) || groupUser.getRole().equals(Role.OWNER))) {
                throw new PermissionException.RoleException.NormalUserUseAdminService(name, qq);
            }
        }
        if (CheckPermission.isSuperAdmin()) {
            throw new PermissionException.RoleException.AdminUseAdminService(name, qq);
        }
        // test 功能
        if (CheckPermission.test() && !Permission.isTester(qq)) {
            throw new PermissionException.RoleException.SomebodyUseTestService(name, qq);
        }
        //服务权限判断
        //白/黑名单
        if (CheckPermission.isWhite()) {
            if (CheckPermission.friend() && !permission.hasUser(name, qq)) {
                throw new PermissionException.WhiteListException.UserFilter(name, qq);
            }
            if (CheckPermission.group() && event instanceof GroupMessageEvent g && !permission.hasGroup(name, g.getGroup().getId())) {
                throw new PermissionException.WhiteListException.GroupFilter(name, qq);
            }
        } else {
            if (CheckPermission.friend() && permission.hasUser(name, qq)) {
                throw new PermissionException.BlackListException.UserFilter(name, qq);
            }
            if (CheckPermission.group() && event instanceof GroupMessageEvent g && permission.hasGroup(name, g.getGroup().getId())) {
                throw new PermissionException.BlackListException.GroupFilter(name, qq);
            }
        }
        return args;
    }

    //    @Before("servicePoint() && @target(Service)")
    public Object[] checkRepeat(JoinPoint point, Service Service) {
        if (ContextUtil.isBreakAop()) {
            return point.getArgs();
        }
        var args = point.getArgs();
        var event = (MessageEvent) args[0];

        var name = Service.value();
        var qq = event.getSender().getId();
//        var name = AopUtils.getTargetClass(point.getTarget()).getAnnotation(Service.class).value();

        if (Permission.isSuperAdmin(qq)) {
            //超管无视任何限制
            return args;
        }
        if (permission.isAllWhite() && permission.containsAllW(event instanceof GroupMessageEvent g ? g.getGroup().getId() : null)) {
            return args;
        }
        // 群跟人的id进行全局黑名单校验
        else if (permission.containsAll(event instanceof GroupMessageEvent g ? g.getGroup().getId() : null, qq)) {
            return args;
        }
        throw new PermissionException.BlackListException.Blocked(name, qq);

    }

//    @After("servicePoint() && @target(Service)")
//    public void endRepeat(JoinPoint point, Service Service){
//        var event = (MessageEvent) point.getArgs()[0];
//        workList.remove(event);
//    }

    Set<Contact> sended;


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
        var ser = pjp.getTarget().getClass().getAnnotation(Service.class);
        String name = "unknown";
        if (ser != null) {
            name = ser.value();
        }
        if (pjp.getArgs()[0] instanceof MessageEvent e) {
            if (e.getSubject().getId() < 0) {
                log.debug("官方bot [uid {}] 调用 -> {}", -e.getSender().getId(), name);
            } else {
                log.debug("{} 调用 -> {}", e.getSender().getId(), name);
            }
        }
        Object result = null;
        long start = System.currentTimeMillis();
        try {
            result = pjp.proceed(pjp.getArgs());
        } finally {
            long end = System.currentTimeMillis();
            long duration = end - start;
            if (result instanceof ServiceCallStatisticLite call) {
                // 新版的统计
                call.setOther(name, start, duration);
                serviceCallStatisticsDao.saveService(call);
            }

            // 原来的可以下线了
            serviceCall.saveCall(name, duration);
        }
    }


    static final long MY_ID = 17064371L;

    private static boolean isMyScore(LazerScore score) {
        return score.getUserID() == MY_ID;
    }

    private OsuUser getUser(OsuUser user) {
        if (user.getUserID() == 0L) return user;
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
        if (score == null || score.getUser().getUserID() == 0L) return score;
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
/*
    @AfterReturning(value = "scoreApi()", returning = "result")
    public void afterApiService(Object result) {
        if (result instanceof BeatmapUserScore s && isMyScore(s.score)) {
            s.score.setRank("X");
        } else if (result instanceof List<?> l) {
            if (l.isEmpty()) return;
            if (l.getFirst() instanceof LazerScore s && isMyScore(s)) {
                for (var i : l) {
                    ((LazerScore)i).setRank("X");
                }
            }
        }
    }
*/
}
