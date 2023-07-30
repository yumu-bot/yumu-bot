package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.Ymp;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.ScoreException;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.QQMsgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.regex.Matcher;

@Service("ScorePr")
public class PassRecentService implements MessageService {
    private static final Logger log = LoggerFactory.getLogger(PassRecentService.class);

    RestTemplate template;
    OsuGetService osuGetService;
    BindDao      bindDao;
    ImageService imageService;

    @Autowired
    public PassRecentService(RestTemplate restTemplate, OsuGetService osuGetService, BindDao bindDao, ImageService image) {
        template = restTemplate;
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
        imageService = image;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        boolean isRecent;

        if (matcher.group("recent") != null) isRecent = true;
        else if (matcher.group("pass") != null) isRecent = false;
        else throw new ScoreException(ScoreException.Type.SCORE_Send_Error);

        //from.sendMessage(isAll?"正在查询24h内的所有成绩":"正在查询24h内的pass成绩");
        String name = matcher.group("name");
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        BinUser user = null;
        if (at != null) {
            user = bindDao.getUser(at.getTarget());
        } else {
            if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                user = new BinUser();
                user.setOsuID(osuGetService.getOsuId(matcher.group("name").trim()));
            } else {
                if (event.getSender().getId() == 365246692L) {
                    var mode = OsuMode.getMode(matcher.group("mode"));
                    byte[] img = new byte[0];
                    try {
                        img = getSPanel(mode, isRecent);
                    } catch (RuntimeException e) {
                        throw new TipsException("24h内无记录");
                    }
                    event.getSender().sendImage(img);
                    return;
                }
                user = bindDao.getUser(event.getSender().getId());
            }
        }
        var mode = OsuMode.getMode(matcher.group("mode"));
        //处理默认mode
        if (mode == OsuMode.DEFAULT && user != null && user.getMode() != null) mode = user.getMode();
        List<Score> dates;
        if (user.getAccessToken() != null) {
            dates = getDates(user, mode, isRecent);
        } else {
            dates = getDates(user.getOsuID(), mode, isRecent);
        }
        if (dates.size() == 0) {
            throw new TipsException("24h内无记录");
        }
        try {
            var osuUser = osuGetService.getPlayerInfo(user, mode);
            var data = imageService.getPanelE(osuUser, dates.get(0),osuGetService);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            log.error("???", e);
            handleText(dates.get(0), isRecent, from);
        }

    }

    private byte[] getSPanel(OsuMode m, boolean all) {
        var s = getDates(bindDao.getUser(365246692L), m, all);
        if (CollectionUtils.isEmpty(s)) {
            throw new RuntimeException("没打");
        }
        return imageService.spInfo(s.get(0));
    }

    private void handleText(Score score, boolean isAll, Contact from) throws TipsException {

        var d = Ymp.getInstance(score);
        HttpEntity<Byte[]> httpEntity = (HttpEntity<Byte[]>) HttpEntity.EMPTY;
        var bytes = template.exchange(d.getUrl(), HttpMethod.GET, httpEntity, byte[].class).getBody();
        from.sendMessage(new MessageChain.MessageChainBuilder().addImage(bytes).addText(d.getOut()).build());
    }

    private List<Score> getDates(BinUser user, OsuMode mode, boolean isAll) {
        if (isAll)
            return osuGetService.getAllRecentN(user, mode, 0, 1);
        else
            return osuGetService.getRecentN(user, mode, 0, 1);
    }

    private List<Score> getDates(Long id, OsuMode mode, boolean isAll) {
        if (isAll)
            return osuGetService.getAllRecentN(id, mode, 0, 1);
        else
            return osuGetService.getRecentN(id, mode, 0, 1);
    }
}
