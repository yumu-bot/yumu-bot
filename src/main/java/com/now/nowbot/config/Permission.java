package com.now.nowbot.config;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.dao.PermissionDao;
import com.now.nowbot.service.MessageService.MessageService;
import net.mamoe.mirai.event.events.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

@Component
public class Permission {
    private static Logger log = LoggerFactory.getLogger(Permission.class);
    private static Set<Long> supetList;

    private static PermissionDao permissionDao;
    @Autowired
    public void PermissionDao(PermissionDao permissionDao) {
        Permission.permissionDao = permissionDao;
    }
    //全局名单
    private static PermissionData ALL_W;
    private static PermissionData ALL_B;
    private static Map<String ,PermissionData> PERMISSIONS = new ConcurrentHashMap<>();
    private static Map<String ,Boolean> PERMISSIONS_MODE = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        //初始化全局名单
        if (permissionDao == null) System.out.println("*****************************************");
        assert permissionDao != null;
        var AllFw = permissionDao.getQQList("PERMISSION_ALL", PermissionType.FRIEND_W);
        var AllGw = permissionDao.getQQList("PERMISSION_ALL", PermissionType.GROUP_W);
        ALL_W = new PermissionData(Set.copyOf(AllFw), Set.copyOf(AllGw));
        var AllFb = permissionDao.getQQList("PERMISSION_ALL", PermissionType.FRIEND_B);
        var AllGb = permissionDao.getQQList("PERMISSION_ALL", PermissionType.GROUP_B);
        ALL_B = new PermissionData(Set.copyOf(AllFb), Set.copyOf(AllGb));
        //初始化各功能名单
        ApplicationContext applicationContext = NowbotConfig.applicationContext;
        assert applicationContext != null;
        var beans = applicationContext.getBeansOfType(MessageService.class);
        beans.forEach((name, bean) -> {
//            //拿到方法
//            Method method = null;
//            try {
//                var $beanClassName = bean.getClass().getName();
//                //通过 代理类 拿到 具体类
//                Class $beanClass = Class.forName($beanClassName.substring(0, $beanClassName.indexOf('$')));
//                //反射得到方法
//                method = $beanClass.getMethod("HandleMessage", MessageEvent.class, Matcher.class);
//            } catch (Exception e) {
//                log.error("反射获取service类异常",e);
//            }
            CheckPermission $beansCheck = null;
//            if (method != null) {
//                //方法不为空则获取注解信息
//                $beansCheck = method.getAnnotation(CheckPermission.class);
//            }
            /*
             * AopUtils.getTargetClass(point.getTarget()).getAnnotation(Service.class).value();
             * AopUtils.getTargetClass(point.getTarget()).getMethod("HandleMessage", MessageEvent.class, java.util.regex.Matcher.class).getAnnotation(com.now.nowbot.aop.CheckPermission.class);
             */
            try {
                $beansCheck = AopUtils.getTargetClass(bean).getMethod("HandleMessage", MessageEvent.class, Matcher.class).getAnnotation(CheckPermission.class);
            } catch (NoSuchMethodException e) {
                log.error("反射获取service类异常",e);
            }

            if ($beansCheck != null) {
                Permission.PERMISSIONS_MODE.put(name,$beansCheck.isWhite());
                Set<Long> friend = null;
                Set<Long> group = null;
                if ($beansCheck.friend()){
                    if($beansCheck.isWhite()) friend = Set.copyOf(permissionDao.getQQList(name,PermissionType.FRIEND_W));
                    else  friend = Set.copyOf(permissionDao.getQQList(name,PermissionType.FRIEND_B));
                }
                if ($beansCheck.group()){
                    if($beansCheck.isWhite()) group = Set.copyOf(permissionDao.getQQList(name,PermissionType.GROUP_W));
                    else  group = Set.copyOf(permissionDao.getQQList(name,PermissionType.GROUP_B));
                }
                var obj = new PermissionData(friend, group);
                obj.setSupper($beansCheck.isSuper());
                Permission.PERMISSIONS.put(name, obj);
            }
        });
        //初始化暗杀名单(
        supetList = Set.of(1340691940L,3145729213L,365246692L,2480557535L,1968035918L,2429299722L);
        log.info("名单初始化完成");
    }
    public boolean containsGroup(String sName,Long id){
        //不存在该名单默认为无限制
        if (!PERMISSIONS.containsKey(sName)) return true;
        //全局黑名单
        if(ALL_B.hasGroup(id)) return false;
        //全局白名单
        if(ALL_W.hasGroup(id)) return true;
        var p = PERMISSIONS.get(sName);
        p.isWhite();
        p.hasGroup(id);
        /*   emmm很明显是
        * w   1  0  1  0
        * h   0  0  1  1
        * y   0  1  1  0
        * */
        return p.isWhite() == p.hasGroup(id);
    }
    public boolean containsFriend(String sName, Long id){
        //超管权最大
        if(isSupper(id)) return true;
        if (!PERMISSIONS.containsKey(sName)) return true;
        //全局黑名单
        if(ALL_B.hasGroup(id)) return false;
        //全局白名单
        if(ALL_W.hasGroup(id)) return true;
        var p = PERMISSIONS.get(sName);
        return p.isWhite() == p.hasFriend(id);
    }

    public static boolean isSupper(Long id){
        return supetList.contains(id);
    }
}
