package com.now.nowbot.service.MessageService;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.config.Permission;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.Instruction;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("ban")
public class BanService implements MessageService{
    Permission permission;
    @Autowired
    public BanService(Permission permission){
        this.permission = permission;
    }

    @Override
    @CheckPermission
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        boolean ban = false;

        String ban_matcher = matcher.group("operate").toLowerCase();
        if (ban_matcher.equals("ban") || ban_matcher.equals("bn")) ban = true;
        else if (ban_matcher.equals("unban") || ban_matcher.equals("ub")) ban = false;
        boolean isGroup = matcher.group("group").toLowerCase().equalsIgnoreCase("g");
        Long qq;
        var at = QQMsgUtil.getType(event.getMessage(), At.class);
        if (!isGroup) {
            if (at != null){
                qq = at.getTarget();
            } else {
                qq = Long.parseLong(matcher.group("qq"));
            }
        } else {
            var qqt = matcher.group("qq");
            if (qqt != null){
                qq = Long.parseLong(qqt);
            } else if (event instanceof GroupMessageEvent group) {
                qq = group.getGroup().getId();
            } else {
                throw new TipsException("id呢");
            }
        }

        Instruction service;

        try {
            service = Instruction.valueOf(matcher.group("service").trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            if (matcher.group("service").toLowerCase().equalsIgnoreCase("all")){
                if (ban) {
                    if (isGroup){
                        permission.addGroup(qq, true);
                    } else {
                        permission.addFriend(qq);
                    }
                } else {

                }
            } else {
                throw new TipsException("无服务");
            }
            return;
        }
        for (String s : Permission.getServiceName(service)) {
            if (ban) {
                if (isGroup){
                    permission.addGroup(s, qq, true);
                } else {
                    permission.addFriend(s, qq);
                }
            }
        }
        event.getSubject().sendMessage(isGroup?"群":"人"+qq+(ban?"封禁":"解封")+"完成");
    }
}
