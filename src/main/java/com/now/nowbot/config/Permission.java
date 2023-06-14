package com.now.nowbot.config;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.dao.PermissionDao;
import com.now.nowbot.entity.ServiceSwitchLite;
import com.now.nowbot.mapper.ServiceSwitchMapper;
import com.now.nowbot.service.MessageService.MessageService;
import com.now.nowbot.throwable.TipsRuntimeException;
import com.now.nowbot.util.Instruction;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.event.events.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;

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
    private static Map<String, PermissionData> PERMISSIONS = new ConcurrentHashMap<>();
    private static CopyOnWriteArraySet<Instruction> OFF_SERVICE = null;

    private static Map<Instruction, String> SERVICE_NAME = new TreeMap<>();

    void init(ApplicationContext applicationContext) {
        //初始化全局名单
        assert permissionDao != null;
        var AllFw = permissionDao.getQQList(PERMISSION_ALL, PermissionType.FRIEND_W);
        var AllGw = permissionDao.getQQList(PERMISSION_ALL, PermissionType.GROUP_W);
        ALL_W = new PermissionData(Set.copyOf(AllFw), Set.copyOf(AllGw));
        ALL_W.setWhite(true);
        var AllFb = permissionDao.getQQList(PERMISSION_ALL, PermissionType.FRIEND_B);
        var AllGb = permissionDao.getQQList(PERMISSION_ALL, PermissionType.GROUP_B);
        ALL_B = new PermissionData(Set.copyOf(AllFb), Set.copyOf(AllGb));
        ALL_B.setWhite(false);
        //初始化各功能名单

        var beans = applicationContext.getBeansOfType(MessageService.class);
        beans.forEach((name, bean) -> {
            CheckPermission $beansCheck = null;
            /*
                获得代理后的class AopUtils.getTargetClass
             * AopUtils.getTargetClass(point.getTarget()).getAnnotation(Service.class).value();
             * AopUtils.getTargetClass(point.getTarget()).getMethod("HandleMessage", MessageEvent.class, java.util.regex.Matcher.class).getAnnotation(com.now.nowbot.aop.CheckPermission.class);
             */

            try {
                // 拿到方法上的权限注解
                $beansCheck = AopUtils.getTargetClass(bean).getMethod("HandleMessage", MessageEvent.class, Matcher.class).getAnnotation(CheckPermission.class);
            } catch (NoSuchMethodException e) {
                log.error("反射获取service类异常", e);
            }

            //如果包含权限注解 则初始化权限列表
            if ($beansCheck != null) {
                if ($beansCheck.supperOnly()) {
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
        //初始化暗杀名单(
        Bot bot = applicationContext.getBean(Bot.class);
        Permission.bot = bot;
        var devGroup = bot.getGroup(746671531);
        if (devGroup != null) {
            supetList = Set.copyOf(devGroup.getMembers().stream().map(NormalMember::getId).toList());
        } else {
            supetList = Set.of(1340691940L, 3145729213L, 365246692L, 2480557535L, 1968035918L, 2429299722L, 447503971L);
        }
        var testGroup = bot.getGroup(722292097);
        if (testGroup != null) {
            testerList = Set.copyOf(testGroup.getMembers().stream().map(NormalMember::getId).toList());
        }

        //初始化功能关闭菜单
        serviceSwitchMapper = applicationContext.getBean(ServiceSwitchMapper.class);
        OFF_SERVICE = new CopyOnWriteArraySet<>();

        for (var i : Instruction.values()) {
            var names = applicationContext.getBeanNamesForType(i.getaClass());
            if (names.length > 0) {
                SERVICE_NAME.put(i, String.join(",", names));
                var p = serviceSwitchMapper.findById(names[0]);
                if (p.isPresent() && !p.get().isSwitch()) {
                    OFF_SERVICE.add(i);
                }
            }
        }

        for (var i : Instruction.values()) {
            for (String s : getServiceName(i)) {
                var p = serviceSwitchMapper.findById(s);
                if (p.isPresent() && !p.get().isSwitch()) {
                    OFF_SERVICE.add(i);
                }
            }
        }
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

    public static boolean isSupper(Long id) {
        return supetList.contains(id);
    }

    /**
     * 单功能开关
     *
     * @param i
     * @return
     */
    public static boolean serviceIsClouse(Instruction i) {
        return OFF_SERVICE.contains(i) && i != Instruction.SWITCH;
    }

    public static void clouseService(Instruction i) {
        OFF_SERVICE.add(i);
        for (String s : getServiceName(i)) {
            serviceSwitchMapper.save(new ServiceSwitchLite(s, false));
        }
    }

    public static void openService(Instruction i) {
        OFF_SERVICE.remove(i);
        for (String s : getServiceName(i)) {
            serviceSwitchMapper.save(new ServiceSwitchLite(s, true));
        }
    }

    public static String[] getServiceName(Instruction i) {
        String[] out = new String[0];
        if (SERVICE_NAME != null) {
            out = SERVICE_NAME.get(i).split(",");
        }
        return out;
    }

    /**
     * 功能列表
     *
     * @return 所有的功能
     */
    public static List<Instruction> getClouseServices() {
        return OFF_SERVICE.stream().toList();
    }

    public static boolean isTester(long qq) {
        return testerList.contains(qq);
    }

    public static boolean isGroupAdmin(long groupId, long qq) {
        if (bot == null) return false;
        Group group;
        if ((group = bot.getGroup(groupId)) == null) return false;
        NormalMember member;
        if ((member = group.get(qq)) == null) return false;
        return member.getPermission() == MemberPermission.ADMINISTRATOR || member.getPermission() == MemberPermission.OWNER;
    }
}
