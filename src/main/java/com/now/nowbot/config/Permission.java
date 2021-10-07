package com.now.nowbot.config;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.dao.PermissionDao;
import com.now.nowbot.service.MessageService.MessageService;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

public class Permission {
    //是否为白名单
    private boolean isWhite;
    //列表
    private Set<Long> GroupList;
    private Set<Long> FriendList;
    //直接简化 true->仅超级管理员能操作      false->群管也能操作
    private boolean Supper;
    private static Set<Long> supetList;

    private static PermissionDao PermissionDao;
    @Autowired
    public void PermissionDao(PermissionDao permissionDao) {
        PermissionDao = permissionDao;
    }
    //全局名单
    public static Permission ALL_W;
    public static Permission ALL_B;
    static Map<String ,Permission> PERMISSIONS;

    private static boolean isInit = false;

    public static void init() {
        //避免多次初始化
        if (isInit) return;
        //初始化全局名单
        if (PermissionDao == null) System.out.println("*****************************************");
        assert PermissionDao != null;
        var AllFw = PermissionDao.getQQList("PERMISSION_ALL", PermissionType.FRIEND_W);
        var AllGw = PermissionDao.getQQList("PERMISSION_ALL", PermissionType.GROUP_W);
        ALL_W = new Permission(Set.copyOf(AllFw), Set.copyOf(AllGw));
        var AllFb = PermissionDao.getQQList("PERMISSION_ALL", PermissionType.FRIEND_B);
        var AllGb = PermissionDao.getQQList("PERMISSION_ALL", PermissionType.GROUP_B);
        ALL_B = new Permission(Set.copyOf(AllFb), Set.copyOf(AllGb));
        //初始化各功能名单
        ApplicationContext applicationContext = NowbotConfig.applicationContext;
        assert applicationContext != null;
        var beans = applicationContext.getBeansOfType(MessageService.class);
        beans.forEach((name, bean) -> {
            //拿到方法
            Method method = null;
            try {
                var $beanClassName = bean.getClass().getName();
                Class $beanClass = Class.forName($beanClassName.substring(0, $beanClassName.indexOf('$')));
                method = $beanClass.getMethod("HandleMessage", MessageEvent.class, Matcher.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            CheckPermission $beansCheck = null;
            if (method != null) {
                $beansCheck = method.getAnnotation(CheckPermission.class);
            }
            if ($beansCheck != null) {

            }
        });
        //初始化暗杀名单(
        supetList = Set.of(1340691940L,3145729213L,365246692L,2480557535L,1968035918L,2429299722L);
        isInit = true;
    }

    public Permission(Set<Long> friend, Set<Long> group) {
        this.FriendList = friend;
        this.GroupList = group;
    }
    public boolean containsGroup(Long id){
        if (GroupList == null) return false;
        return GroupList.contains(id);
    }
    public boolean containsFriend(Long id){
        if (FriendList == null) return false;
        return FriendList.contains(id);
    }

    public static boolean isSupper(Long id){
        return supetList.contains(id);
    }
}
