package com.now.nowbot.config;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.aop.ServiceOrder;
import com.now.nowbot.dao.PermissionDao;
import com.now.nowbot.mapper.ServiceSwitchMapper;
import com.now.nowbot.qq.event.GroupMessageEvent;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.ASyncMessageUtil;
import com.now.nowbot.util.ContextUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;

@Component
public class PermissionNew {
    private static final Logger                         log               = LoggerFactory.getLogger(PermissionNew.class);
    private static final ScheduledExecutorService       EXECUTOR          = Executors.newScheduledThreadPool(Integer.MAX_VALUE, AsyncSetting.V_THREAD_FACORY);
    private static final String                         GLOBAL_PERMISSION = "PERMISSION_ALL";
    private static final Long                           LOCAL_GROUP_ID    = - 10086L;
    private static final Set<String>                    superService      = new CopyOnWriteArraySet<>();
    private static final Map<String, PermissionService> permissionMap     = new LinkedHashMap<>();
    private static final Map<String, MessageService>    servicesMap       = new LinkedHashMap<>();

    private static Set<Long>         supetList;
    private static Set<Long>         testerList;
    private static PermissionService AllService;

    @Resource
    private PermissionDao       permissionDao;
    @Resource
    private ServiceSwitchMapper serviceSwitchMapper;

    public static void onMessage(MessageEvent event, BiConsumer<MessageEvent, Throwable> errorHandle) {
        ASyncMessageUtil.put(event);
        String textMessage = event.getTextMessage().trim();
        servicesMap.forEach((serviceName, service) -> {
            try {
                // 服务截止
                if (checkStopListener()) return;
                // super 用户不受检查
                if (! isSuper(event.getSender().getId())) {
                    // 是否再黑名单内
                    if (isBlock(serviceName, event)) {
                        // 被黑名单禁止
                        log.debug("黑名单禁止, 请求功能:[{}] ,请求人: {}", serviceName, event.getSender().getId());
                        return;
                    }
                }

                var data = new MessageService.DataValue<>();
                if (service.isHandle(event, textMessage, data)) {
                    service.HandleMessage(event, data);
                }
            } catch (Throwable e) {
                errorHandle.accept(event, e);
            }
        });
    }

    private static boolean checkStopListener() {
        return Boolean.TRUE.equals(ContextUtil.getContext("StopListener", Boolean.class));
    }

    private static boolean isSuper(Long id) {
        return supetList.contains(id);
    }

    private static boolean isBlock(String name, MessageEvent event) {
        var servicePermission = permissionMap.get(name);
        var globalPermission = permissionMap.get(GLOBAL_PERMISSION);
        assert servicePermission != null;
        if (event instanceof GroupMessageEvent group) {
            var gid = group.getGroup().getId();
            var uid = group.getSender().getId();
            return ! globalPermission.check(gid, uid) || ! servicePermission.check(gid, uid);
        } else {
            var uid = event.getSender().getId();
            return ! globalPermission.check(null, uid) || ! servicePermission.check(null, uid);
        }
    }

    private static PermissionRecord getService(String name) {
        for (var entry : permissionMap.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return PermissionRecord.fromEntry(entry);
            }
        }
        throw new RuntimeException("未找到对应的服务");
    }

    public void init(
            Map<String, MessageService> services

    ) {
        // 初始化 全局服务控制
        var globalGroupList = permissionDao.getQQList(GLOBAL_PERMISSION, PermissionType.GROUP_B);
        var globalUserList = permissionDao.getQQList(GLOBAL_PERMISSION, PermissionType.FRIEND_B);
        AllService = new PermissionService(false, false, false, globalGroupList, globalUserList, List.of());
        // 初始化顺序
        Map<String, Integer> sortServiceMap = new HashMap<>();
        Map<String, MessageService<?>> sortServiceCache = new HashMap<>();
        services.forEach((name, service) -> {
            CheckPermission $beansCheck;
            /*
             * 获取 service 的执行函数
             */
            Method method = null;
            for (var m : AopUtils.getTargetClass(service).getMethods()) {
                if (m.getName().equals("HandleMessage")) method = m;
            }
            // 必定获取到对应函数
            assert Objects.nonNull(method);

            // 处理排序
            var sort = method.getAnnotation(ServiceOrder.class);
            if (Objects.isNull(sort)) {
                sortServiceMap.put(name, 0);
            } else {
                sortServiceMap.put(name, sort.sort());
            }

            // 处理权限注解
            $beansCheck = method.getAnnotation(CheckPermission.class);
            if (Objects.isNull($beansCheck)) {
                try {
                    $beansCheck = Permission.class.getDeclaredMethod("CheckPermission").getAnnotation(CheckPermission.class);
                } catch (NoSuchMethodException ignore) {
                }
            }
            // 必定有对应注解
            assert Objects.nonNull($beansCheck);

            if ($beansCheck.isSuperAdmin()) {
                superService.add(name);
                return;
            }

            List<Long> groups;
            List<Long> groupsSelf;
            List<Long> users;

            groups = permissionDao.getQQList(name, $beansCheck.isWhite() ? PermissionType.GROUP_W : PermissionType.GROUP_B);
            users = permissionDao.getQQList(name, $beansCheck.isWhite() ? PermissionType.FRIEND_W : PermissionType.FRIEND_B);
            groupsSelf = permissionDao.getQQList(name, $beansCheck.isWhite() ? PermissionType.FRIEND_W : PermissionType.GROUP_SELF_B);

            var param = new PermissionService($beansCheck.userWhite(), $beansCheck.groupWhite(), $beansCheck.userSet(), groups, users, groupsSelf);
            sortServiceCache.put(name, service);
            permissionMap.put(name, param);
        });

        // 处理完服务排序
        sortServiceMap.entrySet()
                .stream()
                .sorted(Comparator.comparingInt(
                                (ToIntFunction<Map.Entry<String, Integer>>) Map.Entry::getValue)
                        .reversed())
                .map(Map.Entry::getKey)
                .forEach(name -> services.put(name, sortServiceCache.get(name)));


        //初始化暗杀名单
        supetList = Set.of(732713726L, 3228981717L, 1340691940L, 3145729213L, 365246692L, 2480557535L, 1968035918L, 2429299722L, 447503971L, LOCAL_GROUP_ID);
        testerList = Set.of(732713726L, 3228981717L, 1340691940L, 3145729213L, 365246692L, 2480557535L, 1968035918L, 2429299722L, 447503971L, LOCAL_GROUP_ID);

        log.info("权限模块初始化完成");
    }

    public void switchService(String name, boolean open) {

    }

    record PermissionRecord(String name, PermissionService permission) {
        static PermissionRecord fromEntry(Map.Entry<String, PermissionService> e) {
            return new PermissionRecord(e.getKey(), e.getValue());
        }
    }
}
