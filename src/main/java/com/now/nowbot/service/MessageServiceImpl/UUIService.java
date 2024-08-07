package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.CmdUtil;
import com.now.nowbot.util.Instruction;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Service("UU_INFO")
public class UUIService implements MessageService<UUIService.UUIParam> {
    private static final Logger log = LoggerFactory.getLogger(UUIService.class);

    @Resource
    RestTemplate template;

    public record UUIParam(OsuUser user, OsuMode mode) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<UUIParam> data) throws TipsException {
        var m = Instruction.UU_INFO.matcher(messageText);
        if (m.find()) {
            var mode = CmdUtil.getMode(m);
            var isMyself = new AtomicBoolean();
            var user = CmdUtil.getUserWithOutRange(event, m, mode, isMyself);
            data.setValue(new UUIParam(user, mode.getData()));
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, UUIParam data) throws Throwable {
        var from = event.getSubject();
        var user = data.user();
        var mode = data.mode();

        @SuppressWarnings("unchecked")
        HttpEntity<Byte[]> httpEntity = (HttpEntity<Byte[]>) HttpEntity.EMPTY;

        byte[] avatar = template.exchange(STR."https://a.ppy.sh/\{user.getUserID()}", HttpMethod.GET, httpEntity, byte[].class).getBody();

        String message = getText(user, mode);
        try {
            QQMsgUtil.sendImageAndText(from, avatar, message);
        } catch (Exception e) {
            log.error("UUI 数据发送失败", e);
            from.sendMessage("UUI 请求超时。\n请重试。或使用增强的 !yminfo。");
        }
    }

    //这是 v0.1.0 的 ymi 文字版本，移到这里
    private String getText(OsuUser user, OsuMode mode) {

        StringBuilder sb = new StringBuilder();
        // Muziyami(osu):10086PP
        sb.append(user.getUsername()).append(' ').append('(').append(mode).append(')').append(':').append(' ').append(Math.round(user.getPP())).append("PP").append('\n');
        // #114514 CN#1919 (LV.100(32%))
        sb.append('#').append(user.getGlobalRank()).append(' ')
                .append(user.getCountry().code()).append('#').append(user.getCountryRank()).append(' ')
                .append("(LV.").append(user.getLevelCurrent()).append('(').append(user.getLevelProgress()).append("%))").append('\n');
        // PC: 2.01w TTH: 743.52w
        sb.append("PC: ");
        long PC = user.getPlayCount();
        if (PC > 10_000) {
            sb.append(Math.round(PC / 100D) / 100D).append('w');
        } else {
            sb.append(PC);
        }
        sb.append(" TTH: ");
        long TTH = user.getTotalHits();
        if (TTH > 10_000) {
            sb.append(Math.round(TTH / 100D) / 100D).append('w');
        } else {
            sb.append(TTH);
        }
        sb.append('\n');
        // PT:24d2h7m ACC:98.16%
        sb.append("PT: ");
        long PT = user.getPlayTime();
        if (PT > 86400) {
            sb.append(PT / 86400).append('d');
        }
        if (PT > 3600) {
            sb.append((PT % 86400) / 3600).append('h');
        }
        if (PT > 60) {
            sb.append((PT % 3600) / 60).append('m');
        }
        sb.append(" ACC: ").append(user.getAccuracy()).append('%').append('\n');
        // ♡:320 kds:245 SVIP2
        sb.append("♡: ").append(user.getFollowerCount())
                .append(" kds: ").append(user.getKudosu().total()).append('\n');
        // SS:26(107) S:157(844) A:1083
        sb.append("SS: ").append(user.getStatistics().getSS())
                .append('(').append(user.getStatistics().getSSH()).append(')')
                .append(" S: ").append(user.getStatistics().getS())
                .append('(').append(user.getStatistics().getSH()).append(')')
                .append(" A: ").append(user.getStatistics().getA()).append('\n');
        // uid:7003013
        sb.append('\n');
        sb.append("uid: ").append(user.getUserID()).append('\n');

        String occupation = user.getOccupation();
        String discord = user.getDiscord();
        String interests = user.getInterests();
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
