package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.regex.Matcher;

@Service("UU_INFO")
public class UUIService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(UUIService.class);

    @Resource
    RestTemplate template;
    @Resource
    OsuUserApiService userApiService;
    @Resource
    BindDao bindDao;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instructions.UU_INFO.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        //from.sendMessage("正在查询您的信息");
        String name = matcher.group("name");
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        BinUser user;
        if (at != null) {
            user = bindDao.getUserFromQQ(at.getTarget());
        } else {
            if (name != null && !name.trim().isEmpty()) {
                var id = userApiService.getOsuId(matcher.group("name").trim());
                user = new BinUser();
                user.setOsuID(id);
            } else {
                user = bindDao.getUserFromQQ(event.getSender().getId());
            }
        }
        var mode = OsuMode.getMode(matcher.group("mode"));
        //处理默认mode
        if (mode == OsuMode.DEFAULT && user != null && user.getMode() != null) mode = user.getMode();

        @SuppressWarnings("unchecked")
        HttpEntity<Byte[]> httpEntity = (HttpEntity<Byte[]>) HttpEntity.EMPTY;

        byte[] image;
        if (user != null) {
            image = template.exchange(STR."https://a.ppy.sh/\{user.getOsuID()}", HttpMethod.GET, httpEntity, byte[].class).getBody();
        } else {
            image = null;
        }

        String message = getText(user, mode);
        try {
            QQMsgUtil.sendImageAndText(from, image, message);
            //event.getSubject().sendMessage(message);
        } catch (Exception e) {
            log.error("UUI 数据发送失败", e);
            from.sendMessage("UUI 请求超时。\n请重试。或使用增强的 !yminfo。");
        }
    }

    //这是 v0.1.0 的 ymi 文字版本，移到这里
    private String getText(BinUser user, OsuMode mode) {
        var u = userApiService.getPlayerInfo(user, mode);

        StringBuilder sb = new StringBuilder();
        // Muziyami(osu):10086PP
        sb.append(u.getUsername()).append(' ').append('(').append(mode).append(')').append(':').append(' ').append(Math.round(u.getPP())).append("PP").append('\n');
        // #114514 CN#1919 (LV.100(32%))
        sb.append('#').append(u.getGlobalRank()).append(' ')
                .append(u.getCountry().code()).append('#').append(u.getCountryRank()).append(' ')
                .append("(LV.").append(u.getLevelCurrent()).append('(').append(u.getLevelProgress()).append("%))").append('\n');
        // PC: 2.01w TTH: 743.52w
        sb.append("PC: ");
        long PC = u.getPlayCount();
        if (PC > 10_000) {
            sb.append(Math.round(PC / 100D) / 100D).append('w');
        } else {
            sb.append(PC);
        }
        sb.append(" TTH: ");
        long TTH = u.getTotalHits();
        if (TTH > 10_000) {
            sb.append(Math.round(TTH / 100D) / 100D).append('w');
        } else {
            sb.append(TTH);
        }
        sb.append('\n');
        // PT:24d2h7m ACC:98.16%
        sb.append("PT: ");
        long PT = u.getPlayTime();
        if (PT > 86400) {
            sb.append(PT / 86400).append('d');
        }
        if (PT > 3600) {
            sb.append((PT % 86400) / 3600).append('h');
        }
        if (PT > 60) {
            sb.append((PT % 3600) / 60).append('m');
        }
        sb.append(" ACC: ").append(u.getAccuracy()).append('%').append('\n');
        // ♡:320 kds:245 SVIP2
        sb.append("♡: ").append(u.getFollowerCount())
                .append(" kds: ").append(u.getKudosu().total()).append('\n');
        // SS:26(107) S:157(844) A:1083
        sb.append("SS: ").append(u.getStatistics().getSS())
                .append('(').append(u.getStatistics().getSSH()).append(')')
                .append(" S: ").append(u.getStatistics().getS())
                .append('(').append(u.getStatistics().getSH()).append(')')
                .append(" A: ").append(u.getStatistics().getA()).append('\n');
        // uid:7003013
        sb.append('\n');
        sb.append("uid: ").append(u.getUID()).append('\n');

        String occupation = u.getOccupation();
        String discord = u.getDiscord();
        String interests = u.getInterests();
        if (Objects.nonNull(occupation)) {
            sb.append("occupation: ").append(occupation.trim()).append('\n');
        }
        if (Objects.nonNull(discord)) {
            sb.append("discord: ").append(discord.trim()).append('\n');
        }
        if (Objects.nonNull(interests)) {
            sb.append("interests: ").append(interests.trim());
        }

        return sb.toString();
    }


}
