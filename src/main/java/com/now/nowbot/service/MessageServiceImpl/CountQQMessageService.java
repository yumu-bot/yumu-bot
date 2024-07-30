package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.qq.contact.GroupContact;
import com.now.nowbot.qq.enums.Role;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.TipsException;
import jakarta.annotation.Resource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CountQQMessageService implements MessageService<Matcher> {
    @Resource
    ImageService  imageService;

    private record Res(long qq, int n) {
    }

    Pattern pattern1 = Pattern.compile("^[!！]\\s*(?i)(ym)?((cm(?![a-zA-Z_]))|(countmessage)|(countmsg))+\\s*(?<d>(n)|(a)|(h))");
    Pattern pattern2 = Pattern.compile("^#统计(?<d>(新人)|(进阶)|(高阶))群管理$");


    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        /*
         * 不再记录qq消息, 所以本功能作废, 等待删掉
         */
        var m = pattern1.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else {
            m = pattern2.matcher(messageText);
            if (m.find()) {
                data.setValue(m);
                return true;
            }
        }
        return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var bot = event.getBot();
        var from = event.getSubject();

        String groupType = matcher.group("d");
        if (false) {
            var end = LocalDateTime.now();
            var start = end.plusDays(-1);
            var res = new ArrayList<HashMap<String, Number>>(0);
            if (res.isEmpty()) {
                event.getSubject().sendMessage("无消息");
            }
            var data = res.getFirst();
            int n = data.get("sum").intValue();
            long i = data.get("QQ").longValue();
            event.getSubject().sendMessage(i + " -> " + n);
            return;
        }
        long groupId = switch (groupType) {
            default -> 595985887;
            case "a", "A", "进阶" -> 928936255;
            case "h", "H", "高阶" -> 281624271;
        };
        var group = bot.getGroup(groupId);
        if (group == null) {
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
        var res = new ArrayList<HashMap<String, Number>>(0);
        var resList = res.stream().map(l -> new Res(l.get("QQ").longValue(), l.get("sum").intValue()))
                .sorted(Comparator.comparingInt(Res::n).reversed()).toList();
        StringBuilder sb = new StringBuilder("|消息数量|QQ|群名片|\n|--:|:--:|:--|\n");
        for (var m : resList) {
            long qq = m.qq();
            var u = group.getUser(qq);
            if (u == null) continue;
            String name = u.getName();
            sb.append('|').append(m.n).append('|').append(qq).append('|').append(name.replace("|", "\\|")).append('|').append('\n');
        }
        //var b = imageService.getMarkdownImage(sb.toString(), 600); //要不要考虑用 Markdown?
        var image = imageService.getPanelA6(sb.toString());

        from.sendImage(image);
    }
}
