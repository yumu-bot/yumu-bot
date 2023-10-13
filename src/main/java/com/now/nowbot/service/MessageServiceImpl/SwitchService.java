package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.config.Permission;
import com.now.nowbot.qq.event.GroupMessageEvent;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.QQMsgUtil;
import com.now.nowbot.util.SendmsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service("SWITCH") //修改service名 "switch" 一定要修改 Permission
public class SwitchService implements MessageService<Matcher> {
    Permission permission;
    ImageService imageService;
    @Autowired
    public SwitchService(Permission permission, ImageService imageService){
        this.permission = permission;
        this.imageService = imageService;
    }

    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(ym)?(switch|sw(?!\\w))+(\\s+(?<p1>[\\w\\-]+))?(\\s+(?<p2>\\w+))?(\\s+(?<p3>\\w+))?(\\s+(?<p4>\\w+))?");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = pattern.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    @CheckPermission(isSuperAdmin = true)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        //我忘了这玩意挖坑是干啥的...似乎是没什么用
        var from = event.getSubject();
        String p1 = matcher.group("p1");
        String p2 = matcher.group("p2");
        if (event instanceof GroupMessageEvent){
            var group = ((GroupMessageEvent) event);
//            group.getSender().mute(60*60*8); 禁言
        }
        if (p1 == null) {
            var tips = """
                    [sleep] wake/sleep <time>
                    [list] export all available service: <servicename> on/off
                    [banlist] export all operational name: ban/unban <name/"ALL">
                    """;

            //from.sendMessage(tips);
            QQMsgUtil.sendImage(from, imageService.drawLine(tips.split("\n")));
            // 等同于 case list

            StringBuilder sb = new StringBuilder();
            var list = Permission.getCloseServices();
            for (String value : Permission.getAllService()) {
                sb.append(list.contains(value)?"OFF":"ON").append(':').append(' ').append(value).append('\n');
            }
            //from.sendMessage(sb.toString());
            QQMsgUtil.sendImage(from, imageService.drawLine(sb));
            return;
        }

        // sleep awake 功能应该放在其他的地方吧，比如新开一个重启bot的服务，里面附带这个功能
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
                } else {
                    from.sendMessage("请输入休眠参数！");
                }
            }

            case "wake" -> {
                SendmsgUtil.wakeUp(event.getSubject().getId());
                from.sendMessage("早安！");
            }

            case "list" -> {
                StringBuilder sb = new StringBuilder();
                var list = Permission.getCloseServices();
                for (String value : Permission.getAllService()) {
                    sb.append(list.contains(value)?"OFF":"ON").append(':').append(' ').append(value).append('\n');
                }
                //from.sendMessage(sb.toString());
                QQMsgUtil.sendImage(from, imageService.drawLine(sb));
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

        if (p2 != null) {
            switch (p2.toLowerCase()){
                case "off" -> {
                    try {
                        Permission.closeService(p1);
                        from.sendMessage("已关闭 " + p1 + " 服务");
                    } catch (IllegalArgumentException e) {
                        from.sendMessage("请输入正确的服务名");
                    }
                }
                case "on" -> {
                    try {
                        Permission.openService(p1);
                        from.sendMessage("已启动 " + p1 + " 服务");
                    } catch (IllegalArgumentException e) {
                        from.sendMessage("请输入正确的服务名");
                    }
                }
            }
        }
    }
}
