package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.ScoreLegacy;
import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.ScoreException;
import com.now.nowbot.util.HandleUtil;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Service("UU_PR")
public class UUPRService implements MessageService<ScorePRService.ScorePRParam> {
    private static final Logger log = LoggerFactory.getLogger(UUPRService.class);

    @Resource
    RestTemplate template;
    @Resource
    OsuUserApiService userApiService;
    @Resource
    OsuScoreApiService scoreApiService;
    @Resource
    OsuBeatmapApiService beatmapApiService;
    @Resource
    BindDao bindDao;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<ScorePRService.ScorePRParam> data) throws Throwable {
        var matcher = Instructions.UU_PR.matcher(messageText);

        if (! matcher.find()) return false;

        var mode = HandleUtil.getMode(matcher);

        boolean isPass = ! StringUtils.hasText(matcher.group("recent"));
/*
        var ur = HandleUtil.getUserAndRange(matcher, 1, false);
        OsuUser user;

        if (Objects.isNull(ur.user())) {
            user = HandleUtil.getMyselfUser(event, mode);
        } else {
            user = HandleUtil.getOsuUser(ur.user(), mode);
        }

        mode = HandleUtil.getModeOrElse(mode, user);

        var scores = HandleUtil.getOsuScoreList(user, mode, ur.range(), isPass);

        data.setValue(new ScorePRService.ScorePRParam(user, scores, mode));
*/
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, ScorePRService.ScorePRParam param) throws Throwable {
        var from = event.getSubject();

        var user = param.user();
        var scores = param.scores();

        if (CollectionUtils.isEmpty(scores)) {
            throw new ScoreException(ScoreException.Type.SCORE_Recent_NotFound, user.getUsername());
        }

        //单成绩发送
        try {
            getTextOutput(scores.getFirst(), from);
        } catch (ScoreException e) {
            throw e;
        } catch (Exception e) {
            from.sendMessage("最近成绩文字：发送失败，请重试");
            log.error("最近成绩文字：发送失败", e);
        }
    }

    private void getTextOutput(Score score, Contact from) throws ScoreException {
        var d = ScoreLegacy.getInstance(score, beatmapApiService);

        @SuppressWarnings("unchecked")
        HttpEntity<Byte[]> httpEntity = (HttpEntity<Byte[]>) HttpEntity.EMPTY;
        var imgBytes = template.exchange(d.getUrl(), HttpMethod.GET, httpEntity, byte[].class).getBody();

        QQMsgUtil.sendImageAndText(from, imgBytes, d.getScoreLegacyOutput());
    }
}

