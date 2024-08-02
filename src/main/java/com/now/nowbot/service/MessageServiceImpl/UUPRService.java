package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.ScoreLegacy;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.ScoreException;
import com.now.nowbot.util.CmdUtil;
import com.now.nowbot.util.Instruction;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Service("UU_PR")
public class UUPRService implements MessageService<UUPRService.UUPRParam> {
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

    public record UUPRParam(OsuUser user, Score score, OsuMode mode) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<UUPRParam> data) throws Throwable {
        var matcher = Instruction.UU_PR.matcher(messageText);

        if (! matcher.find()) return false;

        var mode = CmdUtil.getMode(matcher);
        var range = CmdUtil.getUserWithRange(event, matcher, mode, new AtomicBoolean());
        if (Objects.isNull(range.getData())) {
            return false;
        }
        var uid = range.getData().getUserID();
        boolean includeFail = StringUtils.hasText(matcher.group("recent"));
        int offset=  range.getValue(0, true);

        var list = scoreApiService.getRecent(uid, mode.getData(), offset, 1, includeFail);
        if (list.isEmpty()) throw new ScoreException(ScoreException.Type.SCORE_Recent_NotFound, range.getData().getUsername());
        data.setValue(new UUPRParam(range.getData(), list.getFirst(), mode.getData()));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, UUPRParam param) throws Throwable {
        var from = event.getSubject();
        var score = param.score();

        //单成绩发送
        try {
            getTextOutput(score, from);
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

