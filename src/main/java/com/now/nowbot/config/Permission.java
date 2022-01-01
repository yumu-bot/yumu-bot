package com.now.nowbot.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.dao.PermissionDao;
import com.now.nowbot.service.MessageService.MessageService;
import com.now.nowbot.util.Instruction;
import net.mamoe.mirai.event.events.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;

@Component()
public class Permission {
    private static final Logger log = LoggerFactory.getLogger(Permission.class);
    private static Set<Long> supetList;

    private static PermissionDao permissionDao;
    @Autowired
    public Permission(PermissionDao permissionDao){
        Permission.permissionDao = permissionDao;
    }
    private static final ObjectMapper mapper = new ObjectMapper();

    //全局名单
    private static PermissionData ALL_W;
    private static PermissionData ALL_B;
    //service名单
    private static Map<String ,PermissionData> PERMISSIONS = new ConcurrentHashMap<>();
    private static CopyOnWriteArraySet<Instruction> OFF_SERVICE = null;

    void init(ApplicationContext applicationContext) {
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
                log.error("反射获取service类异常",e);
            }

            //如果包含权限注解 则初始化权限列表
            if ($beansCheck != null) {
                if ($beansCheck.supperOnly()){
                    var obj = new PermissionData(true);
                    Permission.PERMISSIONS.put(name, obj);
                } else {
                    Set<Long> friend = null;
                    Set<Long> group = null;
                    // 存放好友名单
                    if ($beansCheck.friend()) {
                        if ($beansCheck.isWhite())
                            friend = Set.copyOf(permissionDao.getQQList(name, PermissionType.FRIEND_W));
                        else friend = Set.copyOf(permissionDao.getQQList(name, PermissionType.FRIEND_B));
                    }
                    // 存放群组名单
                    if ($beansCheck.group()) {
                        if ($beansCheck.isWhite())
                            group = Set.copyOf(permissionDao.getQQList(name, PermissionType.GROUP_W));
                        else group = Set.copyOf(permissionDao.getQQList(name, PermissionType.GROUP_B));
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
        supetList = Set.of(1340691940L,3145729213L,365246692L,2480557535L,1968035918L,2429299722L,447503971L);

        //初始化功能关闭菜单
        var path = Path.of(NowbotConfig.RUN_PATH+"switch.json");
        if (Files.isRegularFile(path)){
            try {
                JavaType type = mapper.getTypeFactory().constructParametricType(CopyOnWriteArraySet.class, Instruction.class);
                OFF_SERVICE = mapper.readValue(new File(NowbotConfig.RUN_PATH+"switch.json"), CopyOnWriteArraySet.class);
            } catch (IOException e) {
                e.printStackTrace();
                OFF_SERVICE = new CopyOnWriteArraySet<>();
            }
        }else {
            OFF_SERVICE = new CopyOnWriteArraySet<>();
        }

        log.info("名单初始化完成");
    }

    public Set<String> list(){
        Set<String> out = new HashSet<>();
        PERMISSIONS.forEach((name, perm) -> {
            if (perm.isSupper()) {
                out.add(perm.getMsg(name));
            }
        });
        return out;
    }

    public boolean containsGroup(String sName,Long id){
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
    public boolean containsFriend(String sName, Long id){
        //不存在该名单默认为无限制
        if (!PERMISSIONS.containsKey(sName)) return true;
        var p = PERMISSIONS.get(sName);
        return p.hasFriend(id);
    }

    /**
     * 仅针对全局黑白名单
     */
    public boolean containsAll(Long group, Long id){
        //全局黑名单
        return (group == null || !ALL_B.hasGroup(group)) && !ALL_B.hasFriend(id);
    }

    public static boolean isSupper(Long id){
        return supetList.contains(id);
    }

    /**
     * 基于文件保存的 单功能开关
     * @param i
     * @return
     */
    public static boolean serviceIsClouse(Instruction i){
        return OFF_SERVICE.contains(i);
    }

    public static void clouseService(Instruction i){
        OFF_SERVICE.add(i);
        try {
            String s = mapper.writeValueAsString(OFF_SERVICE);
            Files.writeString(Path.of(NowbotConfig.RUN_PATH+"switch.json"), s);
        } catch (JsonProcessingException e) {
            log.error("序列化失败", e);
        } catch (IOException e) {
            log.error("文件写入失败", e);
        }
    }

    public static void openService(Instruction i){
        OFF_SERVICE.remove(i);
        try {
            String s = mapper.writeValueAsString(OFF_SERVICE);
            Files.writeString(Path.of(NowbotConfig.RUN_PATH+"switch.json"), s);
        } catch (JsonProcessingException e) {
            log.error("序列化失败", e);
        } catch (IOException e) {
            log.error("文件写入失败", e);
        }
    }

    /**
     * 功能列表
     * @return 所有的功能
     */
    public static List<Instruction> getClouseServices(){
        return OFF_SERVICE.stream().toList();
    }

}
