package com.now.nowbot.service.MessageService;

import com.now.nowbot.mapper.MessageMapper;
import com.now.nowbot.throwable.TipsException;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.regex.Matcher;

@Service("countQQmsg")
public class CountQQMessageService implements MessageService{
    @Resource
    MessageMapper messageMapper;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var bot = event.getBot();
        String groupType = matcher.group("d");
        String test = matcher.group("d1");
        if (test != null){
            var end = LocalDateTime.now();
            var start = end.plusDays(-1);
            var res = messageMapper.contGroupSender(start.toEpochSecond(ZoneOffset.ofHours(8)), end.toEpochSecond(ZoneOffset.ofHours(8)),
                    Long.parseLong(groupType),
                    Long.parseLong(test)
            );
            var data = res.get(0);
            int i = data.get("sum").intValue();
            int n = data.get("qq").intValue();
            event.getSubject().sendMessage(i + " -> " + n);
            return;
        }
        long groupId;
        switch (groupType){
            default:
            case "新人" : groupId = 595985887; break;
            case "进阶" : groupId = 928936255; break;
            case "高阶" : groupId = 281624271; break;
        }
        var group = bot.getGroup(groupId);
        if (group == null){
            throw new TipsException("不再群里");
        }
        var users = group.getMembers().stream()
                .filter(normalMember -> normalMember.getPermission() == MemberPermission.ADMINISTRATOR)
                .map(NormalMember::getId)
                .toList();
        var userArr = new long[users.size()];
        for (int i = 0; i < users.size(); i++) {
            userArr[i] = users.get(i);
        }
        var end = LocalDateTime.now();
        var start = end.plusDays(-1);
        var res = messageMapper.contGroupSender(start.toEpochSecond(ZoneOffset.ofHours(8)), end.toEpochSecond(ZoneOffset.ofHours(8)),
                    groupId,
                    userArr
                );


    }
}
