package com.now.nowbot.config;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.aop.ServiceOrder;
import com.now.nowbot.dao.PermissionDao;
import com.now.nowbot.entity.ServiceSwitchLite;
import com.now.nowbot.mapper.ServiceSwitchMapper;
import com.now.nowbot.permission.PermissionType;
import com.now.nowbot.qq.Bot;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.contact.GroupContact;
import com.now.nowbot.qq.enums.Role;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.messageServiceImpl.ServiceSwitchService;
import com.now.nowbot.throwable.TipsRuntimeException;
import com.now.nowbot.util.ContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@Component()
public class Permission {
    private static final Logger    log                 = LoggerFactory.getLogger(Permission.class);
    private static       Set<Long> superSet;
    private static       Set<Long> testerList;
    private static final String    PERMISSION_ALL      = "PERMISSION_ALL";

    private static PermissionDao       permissionDao;
    private static ServiceSwitchMapper serviceSwitchMapper;

    private final boolean isAllWhite = true;

    @Autowired
    public Permission(PermissionDao permissionDao) {
        Permission.permissionDao = permissionDao;
    }

    //全局名单
    private static       PermissionParam              WHITELIST;
    private static       PermissionParam              BLACKLIST;
    //service名单
    private static final Map<String, PermissionParam> PERMISSIONS = new ConcurrentHashMap<>();

    private static ArrayList<String>           ALL_SERVICE = null;
    private static CopyOnWriteArraySet<String> OFF_SERVICE = null;

    public static void closeService(String name) {
        String service = getServiceName(name);
        OFF_SERVICE.add(service);
        serviceSwitchMapper.save(new ServiceSwitchLite(service, false));
    }

    public Set<String> list() {
        Set<String> out = new HashSet<>();
        PERMISSIONS.forEach((name, perm) -> {
            if (perm.isAdministrator()) {
                out.add(perm.getMessage(name));
            }
        });
        return out;
    }

    public static PermissionParam getWhiteList() {
        return WHITELIST;
    }

    public static PermissionParam getBlackList() {
        return BLACKLIST;
    }

    public boolean hasGroup(String name, Long id) {
        //不存在该名单默认为无限制
        if (!PERMISSIONS.containsKey(name)) return true;
        var p = PERMISSIONS.get(name);
        /*   真值表
        p.isWhite();
        p.hasGroup(id);
        * w   1  0  1  0
        * h   0  0  1  1
        * y   0  1  1  0
        * */
        return p.hasGroup(id);
    }

    public boolean hasUser(String name, Long id) {
        //不存在该名单默认为无限制
        if (!PERMISSIONS.containsKey(name)) return true;
        var p = PERMISSIONS.get(name);
        return p.hasUser(id);
    }

    public static void openService(String name) {
        String service = getServiceName(name);
        OFF_SERVICE.remove(service);
        serviceSwitchMapper.save(new ServiceSwitchLite(service, true));
    }


    public boolean addGroup(Long id, boolean isWhite, boolean isSuper) {
        var param = isWhite ? WHITELIST : BLACKLIST;
        return addGroup(PERMISSION_ALL, id, isSuper, param);
    }

    private boolean addGroup(String name, Long id, boolean isSuper, PermissionParam param) {
        if (param == null || (!isSuper && param.isAdministrator())) {
            return false;
        }
        if (param.isWhite()) {
            if (param.getGroupList().add(id)) {
                permissionDao.addGroup(name, PermissionType.GROUP_W, id);
                return true;
            } else {
                throw new TipsRuntimeException("已经有了");
            }
        } else {
            if (param.getGroupList().add(id)) {
                permissionDao.addGroup(name, PermissionType.GROUP_B, id);
                return true;
            } else {
                throw new TipsRuntimeException("已经有了");
            }
        }
    }

    @CheckPermission
    static void CheckPermission() {
    }


    public boolean addUser(Long id, boolean isWhite) {
        var param = isWhite ? WHITELIST : BLACKLIST;
        return addUser(id, param, PERMISSION_ALL);
    }

    private boolean addUser(Long id, PermissionParam param, String name) {
        if (param == null) {
            return false;
        }

        var type = param.isWhite() ? PermissionType.FRIEND_W : PermissionType.FRIEND_B;

        if (param.getUserList().add(id)) {
            permissionDao.addUser(name, type, id);
            return true;
        } else {
            throw new TipsRuntimeException("本身不存在");
        }
    }

    void init(ApplicationContext applicationContext) {
        //初始化全局名单
        assert permissionDao != null;

        var whiteUserList = permissionDao.getQQList(PERMISSION_ALL, PermissionType.FRIEND_W);
        var whiteGroupList = permissionDao.getQQList(PERMISSION_ALL, PermissionType.GROUP_W);
        WHITELIST = new PermissionParam(new HashSet<>(whiteUserList), new HashSet<>(whiteGroupList));
        WHITELIST.setWhite(true);

        var blackUserList = permissionDao.getQQList(PERMISSION_ALL, PermissionType.FRIEND_B);
        var blackGroupList = permissionDao.getQQList(PERMISSION_ALL, PermissionType.GROUP_B);
        BLACKLIST = new PermissionParam(new HashSet<>(blackUserList), new HashSet<>(blackGroupList));
        BLACKLIST.setWhite(false);

        OFF_SERVICE = new CopyOnWriteArraySet<>();
        //初始化功能关闭菜单
        serviceSwitchMapper = applicationContext.getBean(ServiceSwitchMapper.class);

        //初始化各功能名单
        var beans = applicationContext.getBeansOfType(MessageService.class);
        Map<String, Integer> sortServiceMap = new HashMap<>();
        beans.forEach((name, bean) -> {
            CheckPermission $beansCheck = null;
            /*
                获得代理后的class AopUtils.getTargetClass
             * AopUtils.getTargetClass(point.getTarget()).getAnnotation(Service.class).value();
             * AopUtils.getTargetClass(point.getTarget()).getMethod("handleMessage", MessageEvent.class, java.util.regex.Matcher.class).getAnnotation(com.now.nowbot.aop.CheckPermission.class);
             */
            Method method = null;
            for (var m : AopUtils.getTargetClass(bean).getMethods()) {
                if (m.getName().equals("handleMessage")) method = m;
            }

            if (method == null) return;

            var p = serviceSwitchMapper.findById(name);
            if (p.isPresent() && !p.get().isSwitch()) {
                OFF_SERVICE.add(name);
            }

            // 拿到方法上的权限注解
            $beansCheck = method.getAnnotation(CheckPermission.class);


            var sort = method.getAnnotation(ServiceOrder.class);

            if (sort != null) {
                sortServiceMap.put(name, sort.sort());
            } else {
                sortServiceMap.put(name, 0);
            }

            // 读取一条默认的注解
            if ($beansCheck == null) {
                try {
                    $beansCheck = Permission.class.getDeclaredMethod("CheckPermission").getAnnotation(CheckPermission.class);
                } catch (NoSuchMethodException ignore) {
                }
            }

            // 如果包含权限注解 则初始化权限列表
            if ($beansCheck != null) {
                if ($beansCheck.isSuperAdmin()) {
                    var obj = new PermissionParam(true);
                    Permission.PERMISSIONS.put(name, obj);
                } else {
                    Set<Long> user = null;
                    Set<Long> group = null;
                    // 存放好友名单
                    if ($beansCheck.friend()) {
                        if ($beansCheck.isWhite()) {
                            user = Set.copyOf(permissionDao.getQQList(name, PermissionType.FRIEND_W));
                        } else {
                            user = Set.copyOf(permissionDao.getQQList(name, PermissionType.FRIEND_B));
                        }
                    }
                    // 存放群聊名单
                    if ($beansCheck.group()) {
                        if ($beansCheck.isWhite()) {
                            group = Set.copyOf(permissionDao.getQQList(name, PermissionType.GROUP_W));
                        } else {
                            group = Set.copyOf(permissionDao.getQQList(name, PermissionType.GROUP_B));
                        }
                    }
                    //写入存储对象
                    var obj = new PermissionParam(user, group);
                    obj.setAdministrator($beansCheck.userSet());
                    obj.setWhite($beansCheck.isWhite());
                    Permission.PERMISSIONS.put(name, obj);
                }
            }
        });

        ALL_SERVICE = sortServiceMap.entrySet()
                .stream()
                .sorted(Comparator.comparingInt((ToIntFunction<Map.Entry<String, Integer>>) Map.Entry::getValue).reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(ArrayList::new));


        //初始化暗杀名单(-17064371L 作为本地测试用户
        superSet = Set.of(-17064371L, 732713726L, 3228981717L, 1340691940L, 3145729213L, 365246692L, 2480557535L, 1968035918L, 2429299722L, 447503971L);
        testerList = Set.of(-17064371L, 732713726L, 3228981717L, 1340691940L, 3145729213L, 365246692L, 2480557535L, 1968035918L, 2429299722L, 447503971L);

        log.info("名单初始化完成");
    }

    public boolean removeUser(Long id, boolean isWhite) {
        var param = isWhite ? WHITELIST : BLACKLIST;
        return removeUser(id, param, PERMISSION_ALL);
    }

    private boolean removeUser(Long id, PermissionParam param, String name) {
        if (param == null) {
            return false;
        }

        var type = param.isWhite() ? PermissionType.FRIEND_W : PermissionType.FRIEND_B;

        if (param.getUserList().remove(id)) {
            permissionDao.deleteUser(name, type, id);
            return true;
        } else {
            throw new TipsRuntimeException("本身不存在");
        }
    }

    public boolean addGroup(String name, Long id, boolean isSuper) {
        String service = getServiceName(name);
        var perm = PERMISSIONS.get(service);
        return addGroup(service, id, isSuper, perm);
    }

    public boolean addGroup(String name, Long id, boolean isSuper, boolean isWhite) {
        String service = getServiceName(name);
        var perm = PERMISSIONS.get(service);
        perm.setWhite(isWhite);
        return addGroup(service, id, isSuper, perm);
    }

    public static String getServiceName(String name) {
        for (String s : ALL_SERVICE) {
            if (s.equalsIgnoreCase(name)) {
                return s;
            }
        }
        throw new RuntimeException("未找到对应的服务");
    }

    public boolean removeGroup(Long id, boolean isSuper) {
        var param = WHITELIST;
        return removeGroup(PERMISSION_ALL, id, isSuper, param);
    }

    public boolean removeGroup(Long id, boolean isWhite, boolean isSuper) {
        var param = isWhite ? WHITELIST : BLACKLIST;
        return removeGroup(PERMISSION_ALL, id, isSuper, param);
    }

    public boolean removeGroup(String name, Long id, boolean isSuper, boolean isWhite) {
        String service = getServiceName(name);
        var perm = PERMISSIONS.get(service);
        perm.setWhite(isWhite);
        return removeGroup(service, id, isSuper, perm);
    }

    private boolean removeGroup(String name, Long id, boolean isSuper, PermissionParam param) {
        if (param == null || (!isSuper && param.isAdministrator())) {
            return false;
        }

        var type = param.isWhite() ? PermissionType.GROUP_W : PermissionType.GROUP_B;

        if (param.getGroupList().remove(id)) {
            permissionDao.deleteGroup(name, type, id);
            return true;
        } else {
            throw new TipsRuntimeException("本身不存在");
        }
    }

    public boolean isAllWhite() {
        return isAllWhite;
    }

    /**
     * 仅针对全局黑白名单
     */
    public boolean containsAll(Long group, Long id) {
        //全局黑名单
        return (group == null || !BLACKLIST.hasGroup(group)) && !BLACKLIST.hasUser(id);
    }

    public boolean containsAllW(Long group) {
        //全局白名单
        return group != null && WHITELIST.hasGroup(group);
    }

    public static boolean isSuperAdmin(Long id) {
        return superSet.contains(id);
    }

    public static boolean isSuperAdmin(MessageEvent event) {
        return superSet.contains(event.getSender().getId());
    }

    /**
     * 单功能开关
     */
    public static boolean isServiceClose(String name) {
        return OFF_SERVICE.contains(name) && !name.equals(ServiceSwitchService.SWITCH_SERVICE_NAME);
    }

    public boolean addUser(String name, Long id) {
        String service = getServiceName(name);
        var param = PERMISSIONS.get(service);
        return addUser(id, param, service);
    }

    public boolean removeUser(String name, Long id) {
        String service = getServiceName(name);
        var param = PERMISSIONS.get(service);
        return removeUser(id, param, service);
    }

    /**
     * 功能列表
     *
     * @return 所有的功能
     */
    public static Set<String> getClosedService() {
        return OFF_SERVICE;
    }

    public static Collection<String> getAllService() {
        return ALL_SERVICE;
    }

    public static boolean isTester(long qq) {
        return testerList.contains(qq);
    }

    public static boolean isBotGroupAdmin(Bot bot, long groupID) {
        if (bot == null) return false;
        Group group;
        if ((group = bot.getGroup(groupID)) == null) return false;
        GroupContact botMyself;
        if ((botMyself = group.getUser(bot.getBotID())) == null) return false;
        return botMyself.getRole() == Role.ADMIN || botMyself.getRole() == Role.OWNER;
    }

    /**
     * 对方是否为群聊管理员。超级管理员无视此限制
     * @param bot 机器人
     * @param groupID 群聊 ID
     * @param qq （对方）成员 ID
     */
    public static boolean isGroupAdmin(Bot bot, long groupID, long qq) {
        if (bot == null) return false;
        Group group;
        if ((group = bot.getGroup(groupID)) == null) return false;
        GroupContact member;
        if ((member = group.getUser(qq)) == null) return false;
        return member.getRole() == Role.ADMIN || member.getRole() == Role.OWNER;
    }

    public static boolean isGroupAdmin(MessageEvent event) {
        return isGroupAdmin(event.getBot(), event.getSubject().getId(), event.getSender().getId()) || isSuperAdmin(event.getSender().getId());
    }

    public static boolean isCommonUser(MessageEvent event) {
        return !isGroupAdmin(event);
    }

    public static void stopListener() {
        ContextUtil.setContext("StopListener", Boolean.TRUE);
    }

    public static boolean checkStopListener() {
        return Boolean.TRUE.equals(ContextUtil.getContext("StopListener", Boolean.class));
    }

    public boolean removeGroup(String name, Long id, boolean isSuper) {
        String service = getServiceName(name);
        var param = PERMISSIONS.get(service);
        return removeGroup(service, id, isSuper, param);
    }

    public void removeGroupAll(String name, boolean isSuper) {
        String service = getServiceName(name);
        var param = PERMISSIONS.get(service);
        if (param == null || (!isSuper && param.isAdministrator())) {
            return;
        }

        var type = param.isWhite() ? PermissionType.GROUP_W : PermissionType.GROUP_B;
        param.getGroupList().clear();
        permissionDao.deleteGroupAll(name, type);
    }
}
