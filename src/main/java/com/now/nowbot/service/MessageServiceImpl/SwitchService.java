package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.config.Permission;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.Instructions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;


@Service("SWITCH") //修改service名 "switch" 一定要修改 Permission
public class SwitchService implements MessageService<Matcher> {
    Permission permission;
    ImageService imageService;
    @Autowired
    public SwitchService(Permission permission, ImageService imageService){
        this.permission = permission;
        this.imageService = imageService;
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) throws Throwable {
        var m = Instructions.SWITCH.matcher(messageText);
        if (m.find()) {

            if (!Permission.isSuperAdmin(event.getSender().getId())) {
                throw new TipsException("只有超级管理员 (OP，原批) 可以使用此功能！");
            }

            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    @CheckPermission(isSuperAdmin = true)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        String p1 = matcher.group("p1");
        String p2 = matcher.group("p2");

        if (Objects.isNull(p1)) {
            var tips = """
                    [sleep] wake/sleep <time>
                    [list] export all available service: <servicename> on/off
                    [banlist] export all operational name: ban/unban <name/"ALL">
                    """;
            var image = imageService.getPanelA6(Arrays.toString(tips.split("\n")));
            from.sendImage(image);

            // 等同于 case list
            var image2 = imageService.getPanelA6(getServiceListMarkdown());
            from.sendImage(image2);
            return;
        }

        // sleep awake 功能应该放在其他的地方吧，比如新开一个重启bot的服务，里面附带这个功能
        switch (p1.toLowerCase()) {
            case "sleep" -> {
                if (Objects.nonNull(p2)){
                    try {
                        int time = Integer.parseInt(p2);
                        Thread.sleep(Math.max(time * 1000, 8 * 60 * 1000));
                        from.sendMessage("晚安！");
                    } catch (NumberFormatException e){
                        from.sendMessage("请输入正确的休眠参数！");
                    }
                } else {
                    from.sendMessage("请输入休眠参数！");
                }
            }

            case "wake" -> from.sendMessage("早安！");

            case "list" -> {
                var image = imageService.getPanelA6(getServiceListMarkdown());
                from.sendImage(image);
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

        if (Objects.nonNull(p2)) {
            switch (p2.toLowerCase()){
                case "off" -> {
                    try {
                        Permission.closeService(p1);
                        from.sendMessage(STR."已关闭 \{p1} 服务");
                    } catch (IllegalArgumentException e) {
                        from.sendMessage("请输入正确的服务名");
                    }
                }
                case "on" -> {
                    try {
                        Permission.openService(p1);
                        from.sendMessage(STR."已启动 \{p1} 服务");
                    } catch (IllegalArgumentException e) {
                        from.sendMessage("请输入正确的服务名");
                    }
                }
            }
        }
    }

    private String getServiceListMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("## 服务：开关状态\n");

        sb.append("""
                | 状态 | 服务名 |
                | :-: | :-- |
                """);

        var list = Permission.getCloseServices();

        for (String serviceName : Permission.getAllService()) {
            sb.append("| ").append(list.contains(serviceName) ? "-" : "O")
                    .append(" | ").append(serviceName)
                    .append(" |\n");
        }

        return sb.toString();
    }
}
