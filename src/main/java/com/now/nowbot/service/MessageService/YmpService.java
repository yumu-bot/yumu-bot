package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.Ymp;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

@Service("ymp")
public class YmpService implements MessageService {
    private static final Logger log = LoggerFactory.getLogger(YmpService.class);

    RestTemplate template;
    OsuGetService osuGetService;
    BindDao bindDao;

    @Autowired
    public YmpService(RestTemplate restTemplate, OsuGetService osuGetService, BindDao bindDao) {
        template = restTemplate;
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        boolean isAll = matcher.group("isAll").toLowerCase().charAt(0) == 'r';
        //from.sendMessage(isAll?"正在查询24h内的所有成绩":"正在查询24h内的pass成绩");
        String name = matcher.group("name");
        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
        BinUser user = null;
        if (at != null) {
            user = bindDao.getUser(at.getTarget());
        } else {
            if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                user = new BinUser();
                user.setOsuID(osuGetService.getOsuId(matcher.group("name").trim()));
            } else {
                user = bindDao.getUser(event.getSender().getId());
            }
        }
        var mode = OsuMode.getMode(matcher.group("mode"));
        //处理默认mode
        if (mode == OsuMode.DEFAULT && user != null && user.getMode() != null) mode = user.getMode();
        List<Score> dates;
        if (user.getAccessToken() != null) {
            dates = getDates(user, mode, isAll);
        } else {
            dates = getDates(user.getOsuID(), mode, isAll);
        }
        if (dates.size() == 0) {
            throw new TipsException("24h内无记录");
        }
        try {
            var osuUser = osuGetService.getPlayerInfo(user, mode);
            var data = postImage(osuUser, dates.get(0));
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            log.error("???", e);
            handleText(dates.get(0), isAll, from);
        }

    }

    private void handleText(Score score, boolean isAll, Contact from) throws TipsException {

        var d = Ymp.getInstance(score);
        HttpEntity<Byte[]> httpEntity = (HttpEntity<Byte[]>) HttpEntity.EMPTY;
        var bytes = template.exchange(d.getUrl(), HttpMethod.GET, httpEntity, byte[].class).getBody();
        QQMsgUtil.sendImageAndText(from, bytes, d.getOut());
    }

    private List<Score> getDates(BinUser user, OsuMode mode, boolean isAll) {
        if (isAll)
            return osuGetService.getRecentN(user, mode, 0, 1);
        else
            return osuGetService.getRecentN(user, mode, 0, 1);
    }

    private List<Score> getDates(Long id, OsuMode mode, boolean isAll) {
        if (isAll)
            return osuGetService.getRecentN(id, mode, 0, 1);
        else
            return osuGetService.getRecentN(id, mode, 0, 1);
    }

    public byte[] postImage(OsuUser user, Score score) {
        var map = osuGetService.getMapInfo(score.getBeatMap().getId());
        score.setBeatMap(map);
        score.setBeatMapSet(map.getBeatMapSet());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        var body = Map.of("user", user,
                "score", score
        );
        HttpEntity httpEntity = new HttpEntity(body, headers);
        ResponseEntity<byte[]> s = template.exchange(URI.create("http://127.0.0.1:1611/panel_E"), HttpMethod.POST, httpEntity, byte[].class);
        return s.getBody();
    }
}
