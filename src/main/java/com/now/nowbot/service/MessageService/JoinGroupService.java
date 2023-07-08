package com.now.nowbot.service.MessageService;

import com.now.nowbot.config.Permission;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent;
import net.mamoe.mirai.message.data.QuoteReply;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("group-accept")
public class JoinGroupService implements MessageService{
    public static Map<Long, BotInvitedJoinGroupRequestEvent> GROUPS = new HashMap<>();

    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        if (Permission.isSupper(event.getSender().getId())){
            var r = QQMsgUtil.getType(event.getMessage(), QuoteReply.class);
            if (r != null && r.getSource().getBotId() == r.getSource().getFromId()){
                var t = r.getSource().getOriginalMessage().contentToString();
                var m = Pattern.compile("^\\((?<x>\\d+)\\)").matcher(t);
                if (m.find()){
                    var e = GROUPS.get(Long.parseLong(m.group("x")));
                    if (e != null) {
                        e.accept();
                        event.getSubject().sendMessage("已同意"+m.group("x"));
                    } else {
                        event.getSender().sendMessage("邀请已过期");
                    }
                }
            }
        }
    }
}
