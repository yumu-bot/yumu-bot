package com.now.nowbot.config;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.service.MessageService.MessageService;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

@Component
@ConfigurationProperties(prefix = "mirai.permission")
public class Permission {
    public Set<Long> groupBlacklist;
    public Set<Long> friendBlacklist;
    public Set<Long> groupWhitelist;
    public Set<Long> friendWhitelist;
    public static Set<Long> superUser;
    private Map<TYPE, Set<Long>> AllPerm = new ConcurrentHashMap<>();
    public static enum TYPE{
        GROUP_OFF,
        FRIEND_OFF,
        GROUP_ON,
        FRIEND_ON
    }
    public static final Map<String , Permission> PERMISSION_ALL = new ConcurrentHashMap<>();

    public Permission(){
        if (!PERMISSION_ALL.containsKey("public"))
            PERMISSION_ALL.put("public", this);
    }

    public Permission(String name){
        PERMISSION_ALL.put(name, this);
    }

    public static void init(){
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
                var p = new Permission(name);
                var $public = PERMISSION_ALL.get("public");
                if($beansCheck.openWF()){
                    p.setFriendWhitelist(new LinkedHashSet<>($public.friendWhitelist));
                }
                if($beansCheck.openBF()){
                    p.setFriendBlacklist(new LinkedHashSet<>($public.friendBlacklist));
                }
                if($beansCheck.openWG()){
                    p.setGroupWhitelist(new LinkedHashSet<>($public.groupWhitelist));
                }
                if($beansCheck.openBG()){
                    p.setGroupBlacklist(new LinkedHashSet<>($public.groupBlacklist));
                }
            }
        });
    }

    public Set<Long> getGroupBlacklist() {
        return groupBlacklist;
    }

    public void setGroupBlacklist(Set<Long> groupBlacklist) {
        this.groupBlacklist = groupBlacklist;
    }

    public Set<Long> getFriendBlacklist() {
        return friendBlacklist;
    }

    public void setFriendBlacklist(Set<Long> friendBlacklist) {
        this.friendBlacklist = friendBlacklist;
    }

    public Set<Long> getGroupWhitelist() {
        return groupWhitelist;
    }

    public void setGroupWhitelist(Set<Long> groupWhitelist) {
        this.groupWhitelist = groupWhitelist;
    }

    public Set<Long> getFriendWhitelist() {
        return friendWhitelist;
    }

    public void setFriendWhitelist(Set<Long> friendWhitelist) {
        this.friendWhitelist = friendWhitelist;
    }

    public Set<Long> getSuperUser() {
        return superUser;
    }

    public void setSuperUser(Set<Long> superUser) {
        this.superUser = superUser;
    }
}
