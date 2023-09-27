package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.QQMsgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.regex.Matcher;

@Service("UUI")
public class UUIService implements MessageService {
    private static final Logger log = LoggerFactory.getLogger(UUIService.class);


    RestTemplate template;
    OsuGetService osuGetService;
    BindDao bindDao;

    @Autowired
    public UUIService(RestTemplate restTemplate, OsuGetService osuGetService, BindDao bindDao) {
        template = restTemplate;
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        //from.sendMessage("正在查询您的信息");
        String name = matcher.group("name");
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        BinUser user = null;
        if (at != null) {
            user = bindDao.getUser(at.getTarget());
        } else {
            if (name != null && !name.trim().equals("")) {
                var id = osuGetService.getOsuId(matcher.group("name").trim());
                user = new BinUser();
                user.setOsuID(id);
            } else {
                user = bindDao.getUser(event.getSender().getId());
            }
        }
        var mode = OsuMode.getMode(matcher.group("mode"));
        //处理默认mode
        if (mode == OsuMode.DEFAULT && user != null && user.getMode() != null) mode = user.getMode();

        HttpEntity<Byte[]> httpEntity = (HttpEntity<Byte[]>) HttpEntity.EMPTY;

        byte[] imgBytes;
        if (user != null) {
            imgBytes = template.exchange("https://a.ppy.sh/" + user.getOsuID(), HttpMethod.GET, httpEntity, byte[].class).getBody();
        } else {
            imgBytes = null;
        }

        String message = getText(user, mode);
        try {
            QQMsgUtil.sendImageAndText(from, imgBytes, message);
            //event.getSubject().sendMessage(message);
        } catch (Exception e) {
            log.error("UUI 数据发送失败", e);
            from.sendMessage("UUI 请求超时。\n请重试。或使用增强的 !yminfo。");
        }
    }

    //这是 v0.1.0 的 ymi 文字版本，移到这里
    private String getText(BinUser user, OsuMode mode) {
        var data = osuGetService.getPlayerInfo(user, mode);

        StringBuilder sb = new StringBuilder();
        var statistics = data.getStatistics();
        // Muziyami(osu):10086PP
        sb.append(data.getUsername()).append(' ').append('(').append(mode).append(')').append(':').append(' ').append(Math.round(statistics.getPP())).append("PP").append('\n');
        // #114514 CN#1919 (LV.100(32%))
        sb.append('#').append(statistics.getGlobalRank()).append(' ')
                .append(data.getCountry().countryCode()).append('#').append(statistics.getCountryRank()).append(' ')
                .append("(LV.").append(statistics.getLevelCurrent()).append('(').append(statistics.getLevelProgress()).append("%))").append('\n');
        // PC: 2.01w TTH: 743.52w
        sb.append("PC: ");
        long PC = statistics.getPlayCount();
        if (PC > 10_000) {
            sb.append(Math.round(PC / 100D) / 100D).append('w');
        } else {
            sb.append(PC);
        }
        sb.append(" TTH: ");
        long TTH = statistics.getTotalHits();
        if (TTH > 10_000) {
            sb.append(Math.round(TTH / 100D) / 100D).append('w');
        } else {
            sb.append(TTH);
        }
        sb.append('\n');
        // PT:24d2h7m ACC:98.16%
        sb.append("PT: ");
        long PT = statistics.getPlayTime();
        if (PT > 86400) {
            sb.append(PT / 86400).append('d');
        }
        if (PT > 3600) {
            sb.append((PT % 86400) / 3600).append('h');
        }
        if (PT > 60) {
            sb.append((PT % 3600) / 60).append('m');
        }
        sb.append(" ACC: ").append(statistics.getAccuracy()).append('%').append('\n');
        // ♡:320 kds:245 SVIP2
        sb.append("♡: ").append(data.getFollowerCount())
                .append(" kds: ").append(data.getKudosu().total()).append('\n');
        // SS:26(107) S:157(844) A:1083
        sb.append("SS: ").append(statistics.getSS())
                .append('(').append(statistics.getSSH()).append(')')
                .append(" S: ").append(statistics.getS())
                .append('(').append(statistics.getSH()).append(')')
                .append(" A: ").append(statistics.getA()).append('\n');
        // uid:7003013
        sb.append('\n');
        sb.append("uid: ").append(data.getUID()).append('\n');

        String occupation = data.getOccupation();
        String discord = data.getDiscord();
        String interests = data.getInterests();
        if (occupation != null && !occupation.trim().isEmpty()) {
            sb.append("occupation: ").append(occupation.trim()).append('\n');
        }
        if (discord != null && !discord.trim().isEmpty()) {
            sb.append("discord: ").append(discord.trim()).append('\n');
        }
        if (interests != null && !interests.trim().isEmpty()) {
            sb.append("interests: ").append(interests.trim());
        }

        return sb.toString();
    }


}
