package com.now.nowbot.service.MessageService;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.config.Permission;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("ban")
public class BanService implements MessageService{
    Permission permission;
    ImageService imageService;
    @Autowired
    public BanService(Permission permission, ImageService imageService){
        this.permission = permission;
        this.imageService = imageService;
    }

    @Override
    @CheckPermission
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        boolean ban = false;
        long sendQQ = event.getSender().getId();
        String msg = event.getMessage().contentToString();
        int index;
        var at = QQMsgUtil.getType(event.getMessage(), At.class);
        if ((index = msg.indexOf("list")) != -1){
            if (Permission.isSupper(sendQQ)){
                Set<Long> groups = Permission.getAllW().getGroupList();
                StringBuilder sb = new StringBuilder("白名单包含:\n");
                for (Long id : groups){
                    sb.append(id).append("\n");
                }
                QQMsgUtil.sendImage(event.getSubject(), imageService.drawLine(sb));
            } else if (event instanceof GroupMessageEvent groupMessageEvent && Permission.isGroupAdmin(groupMessageEvent.getGroup().getId(), sendQQ)){

            }
        } else if ((index = msg.indexOf("add")) != -1) {
            if (Permission.isSupper(sendQQ)){
                matcher = Pattern.compile("add\\s*(?<id>\\d+)").matcher(msg);
                if (matcher.find()) {
                    var add = permission.addGroup(Long.parseLong(matcher.group("id")), true, false);
                    if (add){
                        event.getSubject().sendMessage("添加成功");
                    }
                }
            } else if (event instanceof GroupMessageEvent groupMessageEvent && Permission.isGroupAdmin(groupMessageEvent.getGroup().getId(), sendQQ)){

            }
        }
    }
}
