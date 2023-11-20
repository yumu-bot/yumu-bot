package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.ScoreLegacy;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.ScoreException;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("UUPR")
public class UUPRService implements MessageService<Matcher> {

    RestTemplate template;
    OsuUserApiService userApiService;
    OsuScoreApiService scoreApiService;
    OsuBeatmapApiService beatmapApiService;
    BindDao bindDao;

    @Autowired
    public UUPRService(RestTemplate restTemplate,
                       OsuUserApiService userApiService,
                       OsuScoreApiService scoreApiService,
                       OsuBeatmapApiService beatmapApiService,
                       BindDao bindDao) {
        template = restTemplate;
        this.userApiService = userApiService;
        this.scoreApiService = scoreApiService;
        this.beatmapApiService = beatmapApiService;
        this.bindDao = bindDao;
    }

    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(uu(?<pass>(pass|p(?![a-zA-Z_])))|uu(?<recent>(recent|r(?!\\w))))+\\s*([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*?))?\\s*(#?(?<n>\\d+))?$");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = pattern.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        var name = matcher.group("name");

        int offset;
        int limit;
        boolean isRecent;

        if (matcher.group("recent") != null) {
            isRecent = true;
        } else if (matcher.group("pass") != null) {
            isRecent = false;
        } else {
            throw new ScoreException(ScoreException.Type.SCORE_Send_Error);
        }

        //处理 n
        {
            int n;
            var nStr = matcher.group("n");

            if (nStr == null || nStr.isBlank()) {
                n = 1;
            } else {
                try {
                    n = Integer.parseInt(nStr);
                } catch (NumberFormatException e) {
                    throw new ScoreException(ScoreException.Type.SCORE_Score_RankError);
                }
            }

            //避免 !b lolol233 这样子被错误匹配
            if (n < 1 || n > 100) {
                name += nStr;
                n = 1;
            }

            offset = n - 1;
            limit = 1;
        }

        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        BinUser binUser;

        if (at != null) {
            binUser = bindDao.getUserFromQQ(at.getTarget());
        } else {
            if (name != null && !name.trim().isEmpty()) {
                binUser = new BinUser();
                Long id;
                try {
                    id = userApiService.getOsuId(name.trim());
                    binUser.setOsuID(id);
                } catch (WebClientResponseException e) {
                    throw new ScoreException(ScoreException.Type.SCORE_Player_NotFound);
                }

            } else {
                binUser = bindDao.getUserFromQQ(event.getSender().getId());
            }
        }

        //处理默认mode
        var mode = OsuMode.getMode(matcher.group("mode"));
        if (mode == OsuMode.DEFAULT && binUser != null && binUser.getMode() != null) mode = binUser.getMode();

        List<Score> scoreList = null;

        try {
            if (binUser != null && binUser.isAuthorized()) {
                scoreList = getData(binUser, mode, offset, limit, isRecent);
            } else if (binUser != null) {
                scoreList = getData(binUser.getOsuID(), mode, offset, limit, isRecent);
            }
        } catch (WebClientResponseException e) {
            throw new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired);
        }

        if (scoreList == null || scoreList.isEmpty()) {
            throw new ScoreException(ScoreException.Type.SCORE_Recent_NotFound);
        }

        //单成绩发送
        try {
            getTextOutput(scoreList.get(0), from);
        } catch (Exception e) {
            from.sendMessage("UUPR 发送失败，请重试");
            NowbotApplication.log.error("UUPR 发送失败：", e);
        }
    }

    private void getTextOutput(Score score, Contact from) {
        var d = ScoreLegacy.getInstance(score, beatmapApiService);
        HttpEntity<Byte[]> httpEntity = (HttpEntity<Byte[]>) HttpEntity.EMPTY;
        var imgBytes = template.exchange(d.getUrl(), HttpMethod.GET, httpEntity, byte[].class).getBody();

        //from.sendMessage(new MessageChain.MessageChainBuilder().addImage(imgBytes).addText(d.getScoreLegacyOutput()).build());
        QQMsgUtil.sendImageAndText(from, imgBytes, d.getScoreLegacyOutput());
    }

    private List<Score> getData(BinUser user, OsuMode mode, int offset, int limit, boolean isRecent) {
        if (isRecent)
            return scoreApiService.getRecentIncludingFail(user, mode, offset, limit);
        else
            return scoreApiService.getRecent(user, mode, offset, limit);
    }

    private List<Score> getData(Long id, OsuMode mode, int offset, int limit, boolean isRecent) {
        if (isRecent)
            return scoreApiService.getRecentIncludingFail(id, mode, offset, limit);
        else
            return scoreApiService.getRecent(id, mode, offset, limit);
    }
}

