package com.now.nowbot.permission;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.aop.ServiceOrder;
import com.now.nowbot.config.AsyncSetting;
import com.now.nowbot.config.Permission;
import com.now.nowbot.dao.PermissionDao;
import com.now.nowbot.entity.ServiceSwitchLite;
import com.now.nowbot.mapper.ServiceSwitchMapper;
import com.now.nowbot.qq.event.GroupMessageEvent;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.qq.tencent.TencentMessageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.BotException;
import com.now.nowbot.util.ASyncMessageUtil;
import com.now.nowbot.util.ContextUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

@Component
public class PermissionImplement implements PermissionController {
    private static final Logger                                     log               = LoggerFactory.getLogger(PermissionImplement.class);
    private static final ScheduledExecutorService                   EXECUTOR          = Executors.newScheduledThreadPool(Integer.MAX_VALUE, AsyncSetting.V_THREAD_FACORY);
    public static final  String                                     GLOBAL_PERMISSION = "PERMISSION_ALL";
    private static final Long                                       LOCAL_GROUP_ID    = -10086L;
    private static final Set<String>                                superService      = new CopyOnWriteArraySet<>();
    private static final Map<String, PermissionService>             permissionMap     = new LinkedHashMap<>();
    private static final Map<String, MessageService<Object>>        servicesMap       = new LinkedHashMap<>();
    private static final Map<String, TencentMessageService<Object>> serviceMap4TX     = new LinkedHashMap<>();
    private static final Map<String, ScheduledFuture<?>>            futureMap         = new ConcurrentHashMap<>();

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
                if (!isSuper(event.getSender().getId())) {
                    // 是否再黑名单内
                    if (isBlock(serviceName, event)) {
                        // 被黑名单禁止
                        log.debug("黑名单禁止, 请求功能:[{}] ,请求人: {}", serviceName, event.getSender().getId());
                        return;
                    }
                }

                var data = new MessageService.DataValue<>();
                if (service.isHandle(event, textMessage, data)) {
                    service.HandleMessage(event, data.getValue());
                }
            } catch (Throwable e) {
                errorHandle.accept(event, e);
            }
        });
    }

    public static void onTencentMessage(MessageEvent event, Consumer<MessageChain> onMessage) {
        for (var entry : serviceMap4TX.entrySet()) {
            var service = entry.getValue();
            MessageChain reply = null;
            try {
                var data = service.accept(event, event.getTextMessage());
                if (data == null) {
                    continue;
                }
                reply = service.reply(event, data);
            } catch (Throwable e) {
                if (e instanceof BotException) {
                    reply = new MessageChain(e.getMessage());
                } else {
                    log.error("其他错误", e);
                }
            }

            if (reply == null) {
                reply = new MessageChain("没有响应呢, 一会再试试吧");
            }
            onMessage.accept(reply);
            return;
        }
        onMessage.accept(new MessageChain("没找到对应的功能, 是不是打错命令了呢"));
    }

    private static boolean checkStopListener() {
        return Boolean.TRUE.equals(ContextUtil.getContext("StopListener", Boolean.class));
    }

    private static boolean isSuper(Long id) {
        return supetList.contains(id);
    }

    private static boolean isBlock(String name, MessageEvent event) {
        if (superService.contains(name)) return true;
        PermissionRecord record = getService(name);
        var servicePermission = record.permission;
        var globalPermission = AllService;

        if (event instanceof GroupMessageEvent group) {
            var gid = group.getGroup().getId();
            var uid = group.getSender().getId();
            return !globalPermission.check(gid, uid) || !servicePermission.check(gid, uid);
        } else {
            var uid = event.getSender().getId();
            return !globalPermission.check(null, uid) || !servicePermission.check(null, uid);
        }
    }

    private static PermissionRecord getService(String name) {
        for (var entry : permissionMap.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return PermissionRecord.fromEntry(entry);
            }
        }
        log.debug("未找到对应的服务 [{}], {}", name, permissionMap.size());
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
            permissionMap.put(name, param);
        });

        // 处理完服务排序
        sortServiceMap.entrySet()
                .stream()
                .sorted(Comparator.comparingInt(
                        (ToIntFunction<Map.Entry<String, Integer>>) Map.Entry::getValue).reversed()
                )
                .map(Map.Entry::getKey)
                .forEach(name -> {
                    var service = services.get(name);
                    servicesMap.put(name, service);
                    if (service instanceof TencentMessageService tx) {
                        serviceMap4TX.put(name, tx);
                    }
                });


        //初始化暗杀名单
        supetList = Set.of(732713726L, 3228981717L, 1340691940L, 3145729213L, 365246692L, 2480557535L, 1968035918L, 2429299722L, 447503971L, LOCAL_GROUP_ID);

        log.info("权限模块初始化完成");

    }

    /**
     * 锁定
     *
     * @param id      id
     * @param isGroup 是否为群
     * @param time    撤销时间
     */
    private void blockService(String name, PermissionService perm, Long id, boolean isGroup, Long time) {
        var key = STR."\{name}:\{isGroup ? 'g' : 'u'}\{id}";
        if (time != null) {
            futureMap.put(key, EXECUTOR.schedule(() -> unblockService(name, perm, id, isGroup, null), time, TimeUnit.MILLISECONDS));
        } else {
            futureMap.computeIfPresent(key, this::cancelFuture);
        }
        if (isGroup) {
            if (perm.isGroupWhite()) {
                permissionDao.addGroup(name, PermissionType.GROUP_W, id);
            } else {
                permissionDao.addGroup(name, PermissionType.GROUP_B, id);
            }
            perm.addGroup(id);
        } else {
            if (perm.isUserWhite()) {
                permissionDao.addUser(name, PermissionType.FRIEND_W, id);
            } else {
                permissionDao.addUser(name, PermissionType.FRIEND_B, id);
            }
            perm.addUser(id);
        }
    }

    /**
     * 解锁
     *
     * @param id      id
     * @param isGroup 是否为群
     * @param time    撤销时间
     */
    private void unblockService(String name, PermissionService perm, Long id, boolean isGroup, Long time) {
        var key = STR."\{name}:\{isGroup ? 'g' : 'u'}\{id}";
        if (time != null && time > 0) {
            futureMap.put(key, EXECUTOR.schedule(() -> blockService(name, perm, id, isGroup, null), time, TimeUnit.MILLISECONDS));
        } else {
            futureMap.computeIfPresent(key, this::cancelFuture);
        }

        if (isGroup) {
            if (perm.isGroupWhite()) {
                permissionDao.deleteGroup(name, PermissionType.GROUP_W, id);
            } else {
                permissionDao.deleteGroup(name, PermissionType.GROUP_B, id);
            }
            perm.deleteGroup(id);
        } else {
            if (perm.isUserWhite()) {
                permissionDao.deleteUser(name, PermissionType.FRIEND_W, id);
            } else {
                permissionDao.deleteUser(name, PermissionType.FRIEND_B, id);
            }
            perm.deleteUser(id);
        }
    }

    private void blockServiceSelf(String name, PermissionService perm, Long id, Long time) {
        var key = STR."\{name}:sg\{id}";
        if (time != null) {
            futureMap.put(key, EXECUTOR.schedule(() -> unblockServiceSelf(name, perm, id, null), time, TimeUnit.MILLISECONDS));
        } else {
            futureMap.computeIfPresent(key, this::cancelFuture);
        }
        permissionDao.addGroup(name, PermissionType.GROUP_SELF_B, id);
        perm.addSelfGroup(id);
    }

    private void unblockServiceSelf(String name, PermissionService perm, Long id, Long time) {
        var key = STR."\{name}:sg\{id}";
        if (time != null) {
            futureMap.put(key, EXECUTOR.schedule(() -> blockServiceSelf(name, perm, id, null), time, TimeUnit.MILLISECONDS));
        } else {
            futureMap.computeIfPresent(key, this::cancelFuture);
        }
        permissionDao.deleteGroup(name, PermissionType.GROUP_SELF_B, id);
        perm.deleteSelfGroup(id);
    }

    record PermissionRecord(String name, PermissionService permission) {
        static PermissionRecord fromEntry(Map.Entry<String, PermissionService> e) {
            return new PermissionRecord(e.getKey(), e.getValue());
        }
    }

    @Override
    public void switchService(String name, boolean open) {
        var record = getService(name);
        serviceSwitchMapper.save(new ServiceSwitchLite(name, open));
        record.permission.setEnable(open);
        futureMap.computeIfPresent(name, this::cancelFuture);
    }


    @Override
    public void switchService(String name, boolean open, Long time) {
        switchService(name, open);
        if (Objects.nonNull(time)) {
            futureMap.computeIfPresent(name, this::cancelFuture);
            var future = EXECUTOR.schedule(() -> switchService(name, !open), time, TimeUnit.MILLISECONDS);
            futureMap.put(name, future);
        }
    }


    @Override
    public void blockGroup(Long id) {
        blockService(GLOBAL_PERMISSION, AllService, id, true, null);
    }

    @Override
    public void blockGroup(Long id, Long time) {
        blockService(GLOBAL_PERMISSION, AllService, id, true, time);
    }

    @Override
    public void blockGroup(String service, Long id) {
        var record = getService(service);
        blockService(record.name, record.permission, id, true, null);
    }

    @Override
    public void blockGroup(String service, Long id, Long time) {
        var record = getService(service);
        blockService(record.name, record.permission, id, true, time);
    }

    @Override
    public void unblockGroup(Long id) {
        unblockService(GLOBAL_PERMISSION, AllService, id, true, null);
    }

    @Override
    public void unblockGroup(String service, Long id) {
        var record = getService(service);
        unblockService(record.name, record.permission, id, true, null);
    }

    @Override
    public void unblockGroup(String service, Long id, Long time) {
        var record = getService(service);
        unblockService(record.name, record.permission, id, true, time);
    }


    @Override
    public void blockUser(Long id) {
        blockService(GLOBAL_PERMISSION, AllService, id, false, null);
    }

    @Override
    public void blockUser(Long id, Long time) {
        blockService(GLOBAL_PERMISSION, AllService, id, false, time);
    }

    @Override
    public void blockUser(String service, Long id) {
        var record = getService(service);
        blockService(record.name, record.permission, id, false, null);
    }

    @Override
    public void blockUser(String service, Long id, Long time) {
        var record = getService(service);
        blockService(record.name, record.permission, id, false, time);
    }

    @Override
    public void unblockUser(Long id) {
        unblockService(GLOBAL_PERMISSION, AllService, id, false, null);
    }

    @Override
    public void unblockUser(String service, Long id) {
        var record = getService(service);
        unblockService(record.name, record.permission, id, false, null);
    }

    @Override
    public void unblockUser(String service, Long id, Long time) {
        var record = getService(service);
        unblockService(record.name, record.permission, id, false, time);
    }


    @Override
    public void ignoreAll(Long id) {
        blockServiceSelf(GLOBAL_PERMISSION, AllService, id, null);
    }

    @Override
    public void ignoreAll(Long id, Long time) {
        blockServiceSelf(GLOBAL_PERMISSION, AllService, id, time);
    }

    @Override
    public void ignoreAll(String service, Long id) {
        var record = getService(service);
        blockServiceSelf(record.name, record.permission, id, null);
    }

    @Override
    public void ignoreAll(String service, Long id, Long time) {
        var record = getService(service);
        blockServiceSelf(record.name, record.permission, id, time);
    }

    @Override
    public void unignoreAll(Long id) {
        unblockServiceSelf(GLOBAL_PERMISSION, AllService, id, null);
    }

    @Override
    public void unignoreAll(Long id, Long time) {
        unblockServiceSelf(GLOBAL_PERMISSION, AllService, id, time);
    }

    @Override
    public void unignoreAll(String service, Long id) {
        var record = getService(service);
        unblockServiceSelf(record.name, record.permission, id, null);
    }

    @Override
    public void unignoreAll(String service, Long id, Long time) {
        var record = getService(service);
        unblockServiceSelf(record.name, record.permission, id, time);
    }


    private ScheduledFuture<?> cancelFuture(String name, ScheduledFuture<?> future) {
        future.cancel(true);
        return null;
    }

    @Override
    public List<LockRecord> queryAllBlock() {
        var result = new ArrayList<LockRecord>(permissionMap.size() + 1);
        result.add(queryGlobal());
        permissionMap.forEach((name, p) -> result.add(new LockRecord(
                name,
                !p.isDisable(),
                Objects.requireNonNullElseGet(p.getGroupList(), HashSet::new),
                Objects.requireNonNullElseGet(p.getUserList(), HashSet::new),
                Objects.requireNonNullElseGet(p.getGroupSelfBlackList(), HashSet::new)
        )));
        return result;
    }

    public LockRecord queryGlobal() {
        return new LockRecord(
                GLOBAL_PERMISSION,
                true,
                AllService.getGroupList(),
                AllService.getUserList(),
                new HashSet<>()
        );
    }

    @Override
    public LockRecord queryBlock(String name) {
        var p = getService(name);

        return new LockRecord(
                p.name,
                !p.permission.isDisable(),
                Objects.requireNonNullElseGet(p.permission().getGroupList(), HashSet::new),
                Objects.requireNonNullElseGet(p.permission().getUserList(), HashSet::new),
                Objects.requireNonNullElseGet(p.permission().getGroupSelfBlackList(), HashSet::new)
        );
    }
}
