package com.now.nowbot.service.MessageService;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.config.Permission;
import com.now.nowbot.util.Instruction;
import com.now.nowbot.util.SendmsgUtil;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.regex.Matcher;

@Service("switch")
public class SwitchService implements MessageService{
    Permission permission;
    @Autowired
    public SwitchService(Permission permission){
        this.permission = permission;
    }
    @Override
    @CheckPermission(supperOnly = true)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        //我忘了这玩意挖坑是干啥的...似乎是没什么用
        var from = event.getSubject();
        String p1 = matcher.group("p1");
        String p2 = matcher.group("p2");
        String p3 = matcher.group("p3");
        String p4 = matcher.group("p4");
        if (event instanceof GroupMessageEvent){
            var group = ((GroupMessageEvent) event);
//            group.getSender().mute(60*60*8); 禁言
        }
        if (p1 == null) {
            from.sendMessage("""
                    [sleep] wake/sleep <time>
                    [list] export all available service: <servicename> on/off
                    [banlist] export all operational name: ban/unban <name/"ALL"> <qq/group>
                    
                    """);
            // 等同于 case list

            StringBuilder sb = new StringBuilder();
            var list = Permission.getClouseServices();
            for (Instruction value : Instruction.values()) {
                sb.append(value).append(list.contains(value)?'OFF':'ON').append('\n');
            }
            from.sendMessage(sb.toString());
            return;
        }

        // sleep awake 功能应该放在其他的地方吧，比如新开一个重启bot的服务，里面附带这个功能
        // 不对吧，这是禁言别人然后让别人休眠的东西？
        switch (p1.toLowerCase()) {
            case "sleep" -> {
                if (p2 != null){
                    try {
                        int time = Integer.parseInt(p2);
                        from.sendMessage("晚安！");
                        SendmsgUtil.addSleep(event.getSubject().getId(), time);
                    } catch (NumberFormatException e){
                        from.sendMessage("请输入正确的休眠参数！");
                    }
                }
            }
            case "wake" -> {
                SendmsgUtil.wakeUp(event.getSubject().getId());
                from.sendMessage("早安！");
            }

            case "list" -> {
                StringBuilder sb = new StringBuilder();
                var list = Permission.getClouseServices();
                for (Instruction value : Instruction.values()) {
                    sb.append(value).append(list.contains(value)?'×':'√').append('\n');
                }
                from.sendMessage(sb.toString());
                return;
            }

            case "banlist" -> {
                var texts = permission.list();
                var random = new Random();
                for (var text : texts){
                    Thread.sleep(random.nextInt(1300,3000));
                    event.getSubject().sendMessage(text);
                }
            }
        }

        switch (p2.toLowerCase()){
            case "off" -> {
                if (p1 != null){
                    try {
                        var i = Instruction.valueOf(p1.toUpperCase());
                        Permission.clouseService(i);
                        from.sendMessage("已关闭服务");
                    } catch (IllegalArgumentException e) {
                        from.sendMessage("没找到这个服务");
                    }
                }
            }
            case "on" -> {
                if (p1 != null){
                    try {
                        var i = Instruction.valueOf(p1.toUpperCase());
                        Permission.openService(i);
                        from.sendMessage("已启动服务");
                    } catch (IllegalArgumentException e) {
                        from.sendMessage("没找到这个服务");
                    }
                }
            }
        }
    }
}
