package com.now.nowbot.config;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.aop.ServiceOrder;
import com.now.nowbot.dao.PermissionDao;
import com.now.nowbot.entity.ServiceSwitchLite;
import com.now.nowbot.mapper.ServiceSwitchMapper;
import com.now.nowbot.qq.Bot;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.contact.GroupContact;
import com.now.nowbot.qq.enums.Role;
import com.now.nowbot.service.MessageService;
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
    private static final Logger log = LoggerFactory.getLogger(Permission.class);
    private static Set<Long> supetList;
    private static Set<Long> testerList;
    private static final String PERMISSION_ALL = "PERMISSION_ALL";

    private static PermissionDao permissionDao;
    private static ServiceSwitchMapper serviceSwitchMapper;

    private static Bot bot;

    private boolean isAllWhite = true;

    @Autowired
    public Permission(PermissionDao permissionDao) {
        Permission.permissionDao = permissionDao;
    }

    //全局名单
    private static PermissionData ALL_W;
    private static PermissionData ALL_B;
    //service名单
    private static final Map<String, PermissionData> PERMISSIONS = new ConcurrentHashMap<>();

    private static ArrayList<String> ALL_SERVICE = null;
    private static CopyOnWriteArraySet<String> OFF_SERVICE = null;

    void init(ApplicationContext applicationContext) {
        //初始化全局名单
        assert permissionDao != null;
        var AllFw = permissionDao.getQQList(PERMISSION_ALL, PermissionType.FRIEND_W);
        var AllGw = permissionDao.getQQList(PERMISSION_ALL, PermissionType.GROUP_W);
        ALL_W = new PermissionData(new HashSet<>(AllFw), new HashSet<>(AllGw));
        ALL_W.setWhite(true);
        var AllFb = permissionDao.getQQList(PERMISSION_ALL, PermissionType.FRIEND_B);
        var AllGb = permissionDao.getQQList(PERMISSION_ALL, PermissionType.GROUP_B);
        ALL_B = new PermissionData(new HashSet<>(AllFb), new HashSet<>(AllGb));
        ALL_B.setWhite(false);
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
             * AopUtils.getTargetClass(point.getTarget()).getMethod("HandleMessage", MessageEvent.class, java.util.regex.Matcher.class).getAnnotation(com.now.nowbot.aop.CheckPermission.class);
             */
            Method method = null;
            for (var m : AopUtils.getTargetClass(bean).getMethods()) {
                if (m.getName().equals("HandleMessage")) method = m;
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

            //如果包含权限注解 则初始化权限列表
            if ($beansCheck != null) {
                if ($beansCheck.isSuperAdmin()) {
                    var obj = new PermissionData(true);
                    Permission.PERMISSIONS.put(name, obj);
                } else {
                    Set<Long> friend = null;
                    Set<Long> group = null;
                    // 存放好友名单
                    if ($beansCheck.friend()) {
                        if ($beansCheck.isWhite()) {
                            friend = Set.copyOf(permissionDao.getQQList(name, PermissionType.FRIEND_W));
                        } else {
                            friend = Set.copyOf(permissionDao.getQQList(name, PermissionType.FRIEND_B));
                        }
                    }
                    // 存放群组名单
                    if ($beansCheck.group()) {
                        if ($beansCheck.isWhite()) {
                            group = Set.copyOf(permissionDao.getQQList(name, PermissionType.GROUP_W));
                        } else {
                            group = Set.copyOf(permissionDao.getQQList(name, PermissionType.GROUP_B));
                        }
                    }
                    //写入存储对象
                    var obj = new PermissionData(friend, group);
                    obj.setSupper($beansCheck.userSet());
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


        //初始化暗杀名单(
        supetList = Set.of(1340691940L, 3145729213L, 365246692L, 2480557535L, 1968035918L, 2429299722L, 447503971L);
        testerList = Set.of(1340691940L, 3145729213L, 365246692L, 2480557535L, 1968035918L, 2429299722L, 447503971L);

        log.info("名单初始化完成");
    }

    public Set<String> list() {
        Set<String> out = new HashSet<>();
        PERMISSIONS.forEach((name, perm) -> {
            if (perm.isSupper()) {
                out.add(perm.getMsg(name));
            }
        });
        return out;
    }

    public boolean containsGroup(String sName, Long id) {
        //不存在该名单默认为无限制
        if (!PERMISSIONS.containsKey(sName)) return true;
        var p = PERMISSIONS.get(sName);
        /*   真值表
        p.isWhite();
        p.hasGroup(id);
        * w   1  0  1  0
        * h   0  0  1  1
        * y   0  1  1  0
        * */
        return p.hasGroup(id);
    }

    public boolean containsFriend(String sName, Long id) {
        //不存在该名单默认为无限制
        if (!PERMISSIONS.containsKey(sName)) return true;
        var p = PERMISSIONS.get(sName);
        return p.hasFriend(id);
    }

    public boolean addGroup(String sName, Long id, boolean isSuper) {
        var perm = PERMISSIONS.get(sName);
        return addGroup(sName, id, isSuper, perm);
    }


    public boolean addGroup(Long id, boolean isSuper, boolean isBlack) {
        var perm = isBlack ? ALL_B : ALL_W;
        return addGroup(PERMISSION_ALL, id, isSuper, perm);
    }

    public static PermissionData getAllW() {
        return ALL_W;
    }

    private boolean addGroup(String sName, Long id, boolean isSuper, PermissionData perm) {
        if (perm == null || (!isSuper && perm.isSupper())) {
            return false;
        }
        if (perm.isWhite()) {
            if (perm.getGroupList().add(id)) {
                permissionDao.addGroup(sName, PermissionType.GROUP_W, id);
                return true;
            } else {
                throw new TipsRuntimeException("已经有了");
            }
        } else {
            if (perm.getGroupList().add(id)) {
                permissionDao.addGroup(sName, PermissionType.GROUP_B, id);
                return true;
            } else {
                throw new TipsRuntimeException("已经有了");
            }
        }
    }

    public boolean addFriend(String sName, Long id) {
        var perm = PERMISSIONS.get(sName);
        return addFriend(id, perm, sName);
    }

    public boolean addFriend(Long id) {
        var perm = ALL_B;
        return addFriend(id, perm, PERMISSION_ALL);
    }

    private boolean addFriend(Long id, PermissionData perm, String sName) {
        if (perm == null) {
            return false;
        }
        PermissionType type;
        if (perm.isWhite()) {
            type = PermissionType.FRIEND_W;
        } else {
            type = PermissionType.FRIEND_B;
        }

        if (perm.getFriendList().add(id)) {
            permissionDao.addFriend(sName, type, id);
            return true;
        } else {
            throw new TipsRuntimeException("本身不存在");
        }
    }

    public boolean deleteFriend(String sName, Long id) {
        var perm = PERMISSIONS.get(sName);
        return delFriend(id, perm, sName);
    }

    public boolean deleteFriend(Long id) {
        var perm = ALL_B;
        return delFriend(id, perm, PERMISSION_ALL);
    }

    private boolean delFriend(Long id, PermissionData perm, String sName) {
        if (perm == null) {
            return false;
        }
        PermissionType type;
        if (perm.isWhite()) {
            type = PermissionType.FRIEND_W;
        } else {
            type = PermissionType.FRIEND_B;
        }

        if (perm.getFriendList().remove(id)) {
            permissionDao.delFriend(sName, type, id);
            return true;
        } else {
            throw new TipsRuntimeException("本身不存在");
        }
    }

    public boolean deleteGroup(String sName, Long id, boolean isSuper) {
        var perm = PERMISSIONS.get(sName);
        return deletGroup(sName, id, isSuper, perm);
    }

    public boolean deleteGroup(Long id, boolean isSuper) {
        var perm = ALL_B;
        return deletGroup(PERMISSION_ALL, id, isSuper, perm);
    }

    private boolean deletGroup(String sName, Long id, boolean isSuper, PermissionData perm) {
        if (perm == null || (!isSuper && perm.isSupper())) {
            return false;
        }
        PermissionType type;
        if (perm.isWhite()) {
            type = PermissionType.GROUP_W;
        } else {
            type = PermissionType.GROUP_B;
        }

        if (perm.getGroupList().remove(id)) {
            permissionDao.delGroup(sName, type, id);
            return true;
        } else {
            throw new TipsRuntimeException("本身不存在");
        }
    }

    public boolean allIsWhite() {
        return isAllWhite;
    }

    /**
     * 仅针对全局黑白名单
     */
    public boolean containsAll(Long group, Long id) {
        //全局黑名单
        return (group == null || !ALL_B.hasGroup(group)) && !ALL_B.hasFriend(id);
    }

    public boolean containsAllW(Long group) {
        //全局白名单
        return group != null && ALL_W.hasGroup(group);
    }

    public static boolean isSuper(Long id) {
        return supetList.contains(id);
    }

    /**
     * 单功能开关
     *
     * @return
     */
    public static boolean isServiceClose(String name) {
        return OFF_SERVICE.contains(name) && !name.equals("switch");
    }

    public static void closeService(String name) {
        OFF_SERVICE.add(name);
        serviceSwitchMapper.save(new ServiceSwitchLite(name, false));
    }

    public static void openService(String name) {
        OFF_SERVICE.remove(name);
        serviceSwitchMapper.save(new ServiceSwitchLite(name, true));
    }

    /**
     * 功能列表
     *
     * @return 所有的功能
     */
    public static Set<String> getCloseServices() {
        return OFF_SERVICE;
    }

    public static Collection<String> getAllService() {
        return ALL_SERVICE;
    }

    public static boolean isTester(long qq) {
        return testerList.contains(qq);
    }

    public static boolean isGroupAdmin(long groupId, long qq) {
        if (bot == null) return false;
        Group group;
        if ((group = bot.getGroup(groupId)) == null) return false;
        GroupContact member;
        if ((member = group.getUser(qq)) == null) return false;
        return member.getRoll() == Role.ADMIN || member.getRoll() == Role.OWNER;
    }

    public static void stopListener() {
        ContextUtil.setContext("StopListener", Boolean.TRUE);
    }

    public static boolean checkStopListener() {
        return Boolean.TRUE.equals(ContextUtil.getContext("StopListener", Boolean.class));
    }
}
