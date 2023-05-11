package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.QQMsgUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;

@Service("Info")
public class InfoService implements MessageService {
    private static final Logger log = LoggerFactory.getLogger(InfoService.class);
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
        OsuUser userdata;
        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
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

//        Image img = from.uploadImage(ExternalResource.create());
        try {
            var img = postImage(user, mode);
            QQMsgUtil.sendImage(from, img);
        } catch (Exception e) {
            log.error("INFO 数据请求失败", e);
            from.sendMessage("Info 渲染图片超时，请重试。");
        }
    }

    //getText 移到了 UUIService

    public byte[] postImage(BinUser user, OsuMode mode) {
        var userInfo = osuGetService.getPlayerInfo(user, mode);
        var bps = osuGetService.getBestPerformance(user, mode, 0, 100);
        var res = osuGetService.getRecentN(user, mode, 0, 3);

        float bonus = 0f;
        if (bps.size() > 0) {
            var bppps = bps.stream().map((bpInfo) -> bpInfo.getWeight().getPP()).mapToDouble(Float::doubleValue).toArray();
            bonus = SkiaUtil.getBonusPP(bppps, userInfo.getPlayCount());
            //我不是吧方法抽到util里了吗
            //bonus = Ppm.bonusPP(bppps, userInfo.getPlayCount());
        }
        var times = bps.stream().map(Score::getCreateTime).toList();
        var now = LocalDate.now();
        var bpNum = new int[90];
        times.forEach(time -> {
            var day = (int)(now.toEpochDay() - time.toLocalDate().toEpochDay());
            if (day > 0 && day <= 90){
                bpNum[90-day] ++;
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        var body = Map.of("user",userInfo,
                    "bp-time",bpNum,
                    "bp-list", bps.subList(0,Math.min(bps.size(), 8)),
                    "re-list", res,
                    "bonus_pp", bonus,
                    "mode", mode.getName()
                );
        HttpEntity httpEntity = new HttpEntity(body, headers);
        ResponseEntity<byte[]> s = template.exchange(URI.create("http://127.0.0.1:1611/panel_D"), HttpMethod.POST, httpEntity, byte[].class);
        return s.getBody();
    }
}
