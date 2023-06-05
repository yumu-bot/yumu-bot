package com.now.nowbot.service.MessageService;

import com.now.nowbot.mapper.MessageMapper;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.regex.Matcher;

@Service("countQQmsg")
public class CountQQMessageService implements MessageService{
    @Resource
    MessageMapper messageMapper;
    @Resource
    ImageService imageService;

    private record Res(long qq, int n){}

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
            if (res.size() == 0){
                event.getSubject().sendMessage("无消息");
            }
            var data = res.get(0);
            int n = data.get("sum").intValue();
            long i = data.get("qq").longValue();
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
            throw new TipsException("不在群里");
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
        var resList = res.stream().map(l -> new Res(l.get("qq").longValue(), l.get("sum").intValue()))
                .sorted(Comparator.comparingInt(Res::n).reversed()).toList();
        StringBuilder sb = new StringBuilder();
        for (var m: resList){
            long qq = m.qq();
            int n = m.n();
            var u = group.getMembers().get(qq);
            if (u == null) continue;
            String name = u.getNameCard();
            sb.append('[').append(name).append(']').append(' ')
                    .append(n).append('\n');
        }
        var b = imageService.drawLine(sb.toString().split("\n"));

        QQMsgUtil.sendImage(event.getSubject(), b);
    }
}
