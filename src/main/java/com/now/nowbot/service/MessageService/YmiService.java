package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;

@Service("ymi")
public class YmiService implements MessageService{
    private static final Logger log = LoggerFactory.getLogger(YmiService.class);
    @Autowired
    RestTemplate template;

    @Autowired
    OsuGetService osuGetService;
    @Autowired
    BindDao bindDao;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        //from.sendMessage("正在查询您的信息");
        String name = matcher.group("name");
        OsuUser date;
        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
        BinUser user = null;
        if (at != null){
            user = bindDao.getUser(at.getTarget());
        }else {
            if (name != null && !name.trim().equals("")){
                var id = osuGetService.getOsuId(matcher.group("name").trim());
                user = new BinUser();
                user.setOsuID(id);
            }else {
                user = bindDao.getUser(event.getSender().getId());
            }
        }
        var mode = OsuMode.getMode(matcher.group("mode"));
        //处理默认mode
        if (mode == OsuMode.DEFAULT && user != null && user.getMode() != null) mode = user.getMode();
        date = osuGetService.getPlayerInfo(user,mode);

//        if(date.size()==0){
//            throw new TipsException("没有查询到您的信息呢");
//        }
        StringBuilder sb = new StringBuilder();
        var statistics = date.getStatistics();
        // Muziyami(osu):10086PP
        sb.append(date.getUsername()).append('(').append(mode).append(')').append(':').append(statistics.getPp()).append("PP").append('\n');
        // #114514 CN#1919 (LV.100(32%))
        sb.append('#').append(statistics.getGlobalRank()).append(' ')
                .append(date.getCountry().countryCode()).append('#').append(statistics.getCountryRank()).append(' ')
                .append("(LV.").append(statistics.getLevelCurrent()).append('(').append(statistics.getLevelProgress()).append("%))").append('\n');
        // PC:2.01w TTH:743.52w
        sb.append("PC:");
        long PC = statistics.getPlayCount();
        if (PC>10_000) {
            sb.append((PC / 100) / 100D).append('w');
        }else {
            sb.append(PC);
        }
        sb.append(" TTH:");
        long TTH = statistics.getTotalHits();
        if (TTH>10_000) {
            sb.append((TTH / 100) / 100D).append('w');
        }else {
            sb.append(TTH);
        }
        sb.append('\n');
        // PT:24d2h7m ACC:98.16%
        sb.append("PT:");
        long PT = statistics.getPlayTime();
        if(PT>86400){
            sb.append(PT/86400).append('d');
        }
        if(PT>3600){
            sb.append((PT%86400)/3600).append('h');
        }
        if(PT>60){
            sb.append((PT%3600)/60).append('m');
        }
        sb.append(" ACC:").append(statistics.getAccuracy()).append('%').append('\n');
        // ♡:320 kds:245 SVIP2
        sb.append("♡: ").append(date.getFollowerCount())
                .append(" kds:").append(date.getKudosu().total()).append('\n');
        // SS:26(107) S:157(844) A:1083
        sb.append("SS:").append(statistics.getSS())
                .append('(').append(statistics.getSSH()).append(')')
                .append(" S:").append(statistics.getS())
                .append('(').append(statistics.getSH()).append(')')
                .append(" A:").append(statistics.getA()).append('\n');
        // uid:7003013
        sb.append("UID:").append(date.getId()).append('\n');

        String occupation =  date.getOccupation();
        String discord =  date.getDiscord();
        String interests =  date.getInterests();
        if (occupation != null && !occupation.trim().equals("")) {
            sb.append('\n').append("occupation: ").append(occupation.trim());
        }if (discord != null && !discord.trim().equals("")) {
            sb.append('\n').append("discord: ").append(discord.trim());
        }if (interests != null && !interests.trim().equals("")) {
            sb.append('\n').append("interests: ").append(interests.trim());
        }
//        Image img = from.uploadImage(ExternalResource.create());
        from.sendMessage(sb.toString());
    }
}
