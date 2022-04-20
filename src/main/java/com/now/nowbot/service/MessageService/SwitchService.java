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
                    wake/sleep <time>
                    list->out all name; open/close <name>
                    banlist->out all changeable name; ban/unban <name or "ALL"> <qq/group>
                    """);
            return;
        }
        switch (p1.toLowerCase()){
            case "sleep" -> {
                if (p2 != null){
                    try {
                        int time = Integer.parseInt(p2);
                        SendmsgUtil.addSleep(event.getSubject().getId(), time);
                    }catch (NumberFormatException e){
                        from.sendMessage("参数异常");
                    }
                }
            }
            case "wake" -> {
                SendmsgUtil.wakeUp(event.getSubject().getId());
                from.sendMessage("恢复");
            }
            case "close" -> {
                if (p2 != null){
                    try {
                        var i = Instruction.valueOf(p2.toUpperCase());
                        Permission.clouseService(i);
                        from.sendMessage("已关闭");
                    } catch (IllegalArgumentException e) {
                        from.sendMessage("没找到这个服务");
                    }
                }
            }
            case "open" -> {
                if (p2 != null){
                    try {
                        var i = Instruction.valueOf(p2.toUpperCase());
                        Permission.openService(i);
                        from.sendMessage("已启动");
                    } catch (IllegalArgumentException e) {
                        from.sendMessage("没找到这个服务");
                    }
                }
            }
            case "list" -> {
                StringBuilder sb = new StringBuilder();
                var list = Permission.getClouseServices();
                for (Instruction value : Instruction.values()) {
                    sb.append(value).append(list.contains(value)?'×':'√').append('\n');
                }
                from.sendMessage(sb.toString());
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
    }

}
