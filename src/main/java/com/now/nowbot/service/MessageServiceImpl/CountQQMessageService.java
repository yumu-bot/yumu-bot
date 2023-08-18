package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.mapper.MessageMapper;
import com.now.nowbot.qq.contact.GroupContact;
import com.now.nowbot.qq.enums.Role;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.regex.Matcher;

@Service("countQQmsg")
public class CountQQMessageService implements MessageService {
    @Resource
    MessageMapper messageMapper;
    @Resource
    ImageService imageService;

    private record Res(long qq, int n){}

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var bot = event.getBot();
        String groupType = matcher.group("d");
        if (false){
            var end = LocalDateTime.now();
            var start = end.plusDays(-1);
            var res = messageMapper.contGroupSender(start.toEpochSecond(ZoneOffset.ofHours(8)), end.toEpochSecond(ZoneOffset.ofHours(8)),
                    Long.parseLong(groupType),
                    Long.parseLong("0")
            );
            if (res.size() == 0){
                event.getSubject().sendMessage("无消息");
            }
            var data = res.get(0);
            int n = data.get("sum").intValue();
            long i = data.get("QQ").longValue();
            event.getSubject().sendMessage(i + " -> " + n);
            return;
        }
        long groupId;
        switch (groupType){
            default:
            case "n" :
            case "N" :
            case "新人" : groupId = 595985887; break;
            case "a" :
            case "A" :
            case "进阶" : groupId = 928936255; break;
            case "h" :
            case "H" :
            case "高阶" : groupId = 281624271; break;
        }
        var group = bot.getGroup(groupId);
        if (group == null){
            throw new TipsException("不在群里");
        }
        var users = group.getAllUser().stream()
                .filter(normalMember -> normalMember.getRoll() == Role.ADMIN || normalMember.getRoll() == Role.OWNER)
                .map(GroupContact::getId)
                .toList();
        var userArr = new long[users.size()];
        for (int i = 0; i < users.size(); i++) {
            userArr[i] = users.get(i);
        }
        var end = LocalDateTime.now();
        var start = end.plusDays(-7);
        var res = messageMapper.contGroupSender(start.toEpochSecond(ZoneOffset.ofHours(8)), end.toEpochSecond(ZoneOffset.ofHours(8)),
                    groupId,
                    userArr
                );
        var resList = res.stream().map(l -> new Res(l.get("QQ").longValue(), l.get("sum").intValue()))
                .sorted(Comparator.comparingInt(Res::n).reversed()).toList();
        StringBuilder sb = new StringBuilder("|消息数量|QQ|群名片|\n|--:|:--:|:--|\n");
        for (var m: resList){
            long qq = m.qq();
            var u = group.getUser(qq);
            if (u == null) continue;
            String name = u.getName();
            sb.append('|').append(m.n).append('|').append(qq).append('|').append(name.replace("|", "\\|")).append('|').append('\n');
        }
        var b = imageService.getMarkdownImage(sb.toString(), 600); //要不要考虑用 Markdown?

        QQMsgUtil.sendImage(event.getSubject(), b);
    }
}
