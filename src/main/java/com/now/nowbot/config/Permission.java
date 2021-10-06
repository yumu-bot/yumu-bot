package com.now.nowbot.config;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.dao.PermissionDao;
import com.now.nowbot.mapper.PermissionMapper;
import com.now.nowbot.service.MessageService.MessageService;
import io.ktor.util.collections.ConcurrentSet;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

public class Permission {
    boolean isInit = false;
    //权限类型
    TYPE type;
    //列表
    public Set<Long> List;
    //类型enum
    public enum TYPE{
        GROUP_W,
        GROUP_B,
        FRIEND_W,
        FRIEND_B,
    }
    //直接简化 true->仅超级管理员能操作      false->群管也能操作
    boolean Supper;

    private static PermissionDao PermissionDao;
    @Autowired
    public void PermissionDao(PermissionDao permissionDao){
        PermissionDao = permissionDao;
    }
    public static final Set<Long> PERMISSION_ALL = new ConcurrentSet<>();

    public static void init(){
        var AllFw = PermissionDao.getQQList("PERMISSION_ALL",TYPE.FRIEND_W);
        var AllFb = PermissionDao.getQQList("PERMISSION_ALL",TYPE.FRIEND_B);
        var AllGw = PermissionDao.getQQList("PERMISSION_ALL",TYPE.GROUP_W);
        var AllGb = PermissionDao.getQQList("PERMISSION_ALL",TYPE.GROUP_B);
        ApplicationContext applicationContext = NowbotConfig.applicationContext;
        assert applicationContext != null;
        var beans = applicationContext.getBeansOfType(MessageService.class);
        beans.forEach((name, bean)->{
            Method method = null;
            try {
                var $beanClassName = bean.getClass().getName();
                Class $beanClass = Class.forName($beanClassName.substring(0,$beanClassName.indexOf('$')));
                method = $beanClass.getMethod("HandleMessage", MessageEvent.class, Matcher.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
            CheckPermission $beansCheck = null;
            if (method != null) {
                $beansCheck = method.getAnnotation(CheckPermission.class);
            }
            if ($beansCheck != null){

            }
        });
    }

    public Set<Long> getG_Blacklist() {
        return List;
    }

    public void setG_Blacklist(Set<Long> g_Blacklist) {
        this.List = g_Blacklist;
    }
}
