package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.ServiceOrder;
import com.now.nowbot.config.Permission;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.ScoreLegacy;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.ScoreException;
import com.now.nowbot.util.Pattern4ServiceImpl;
import com.now.nowbot.util.QQMsgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.regex.Matcher;

//@Service("SCOREDELUXE")
public class ScorePRDeluxeService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(ScorePRDeluxeService.class);

    RestTemplate template;
    OsuUserApiService userApiService;
    OsuScoreApiService scoreApiService;
    OsuBeatmapApiService beatmapApiService;
    BindDao bindDao;
    ImageService imageService;

    @Autowired
    public ScorePRDeluxeService(RestTemplate restTemplate,
                                OsuUserApiService userApiService,
                                OsuScoreApiService scoreApiService,
                                OsuBeatmapApiService beatmapApiService,
                                BindDao bindDao, ImageService image) {
        template = restTemplate;
        this.userApiService = userApiService;
        this.scoreApiService = scoreApiService;
        this.beatmapApiService = beatmapApiService;
        this.bindDao = bindDao;
        imageService = image;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        if (event.getSender().getId() != 365246692L) return false;
        var m = Pattern4ServiceImpl.SCOREPR.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            Permission.stopListener();
            return true;
        } else return false;
    }

    @Override
    @ServiceOrder(sort = 15)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        var name = matcher.group("name");

        int offset;
        int limit;
        boolean isRecent;
        boolean isMultipleScore;

        if (matcher.group("recent") != null) {
            isRecent = true;
        } else if (matcher.group("pass") != null) {
            isRecent = false;
        } else {
            throw new ScoreException(ScoreException.Type.SCORE_Send_Error);
        }

        //处理 n，m
        // !p 45-55 offset/n = 44 limit/m = 11
        {
            int n;
            int m;
            var nStr = matcher.group("n");
            var mStr = matcher.group("m");

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

            if (mStr == null || mStr.isBlank()) {
                m = n;
            } else {
                try {
                    m = Integer.parseInt(mStr);
                } catch (NumberFormatException e) {
                    throw new ScoreException(ScoreException.Type.SCORE_Score_RankError);
                }
            }

            //分流：正常，相等，相反
            if (m > n) {
                offset = n - 1;
                limit = m - n + 1;
            } else if (m == n) {
                offset = n - 1;
                limit = 1;
            } else {
                offset = m - 1;
                limit = n - m + 1;
            }

            isMultipleScore = (limit != 1);
        }

        //from.sendMessage(isAll?"正在查询24h内的所有成绩":"正在查询24h内的pass成绩");
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        BinUser binUser;
        OsuUser osuUser;

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
                if (event.getSender().getId() == 365246692L && false) {
                    var mode = OsuMode.getMode(matcher.group("mode"));
                    byte[] img;
                    try {
                        img = getAlphaPanel(mode, offset, 1, isRecent); //这里的limit没法是多的
                    } catch (RuntimeException e) {
                        throw new ScoreException(ScoreException.Type.SCORE_Recent_NotFound);
                        //log.error("s: ", e);
                        //throw new TipsException("24h内无记录");
                    }
                    event.getSubject().sendImage(img);
                    return;
                }
                binUser = bindDao.getUserFromQQ(event.getSender().getId());
            }
        }

        //处理默认mode
        var mode = OsuMode.getMode(matcher.group("mode"));
        if (mode == OsuMode.DEFAULT && binUser != null && binUser.getMode() != null) mode = binUser.getMode();

        List<Score> scoreList;

        try {
            if (binUser != null && binUser.isAuthorized()) {
                scoreList = getData(binUser, mode, offset, limit, isRecent);
            } else if (binUser != null) {
                scoreList = getData(binUser.getOsuID(), mode, offset, limit, isRecent);
            } else {
                //退避 !recent
                if (event.getRawMessage().toLowerCase().contains("recent")) {
                    log.info("recent 退避成功");
                    return;
                } else {
                    throw new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired);
                }
            }
        } catch (WebClientResponseException e) {
            //退避 !recent
            if (event.getRawMessage().toLowerCase().contains("recent")) {
                log.info("recent 退避成功");
                return;
            } else {
                throw new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired);
            }
        }

        if (scoreList == null || scoreList.isEmpty()) {
            throw new ScoreException(ScoreException.Type.SCORE_Recent_NotFound);
        }

        try {
            osuUser = userApiService.getPlayerInfo(binUser, mode);
        } catch (Exception e) {
            throw new ScoreException(ScoreException.Type.SCORE_Player_NotFound);
        }

        //成绩发送
        if (isMultipleScore) {
            int scoreSize = scoreList.size();

            //M太大
            if (scoreSize < offset + limit) limit = scoreSize - offset;
            if (limit <= 0) throw new ScoreException(ScoreException.Type.SCORE_Score_OutOfRange);

            try {
                var data = imageService.getPanelA5(osuUser, scoreList.subList(offset, offset + limit));
                QQMsgUtil.sendImage(from, data);
            } catch (Exception e) {
                throw new ScoreException(ScoreException.Type.SCORE_Send_Error);
            }

        } else {
            //单成绩发送

            try {
                var data = getAlphaPanel(mode, offset, 1, isRecent);
                QQMsgUtil.sendImage(from, data);
            } catch (Exception e) {
                log.error("为什么要转 Legacy 方法发送呢？直接重试不就好了", e);
                getTextOutput(scoreList.get(0), from);
            }
        }
    }

    private byte[] getAlphaPanel(OsuMode mode, int offset, int limit, boolean isRecent) throws ScoreException {
        var s = getData(bindDao.getUserFromQQ(365246692L), mode, offset, limit, isRecent);
        if (CollectionUtils.isEmpty(s)) {
            //throw new RuntimeException("没打");
            throw new ScoreException(ScoreException.Type.SCORE_Recent_NotFound);
        }
        return imageService.getPanelBeta(s.get(0));
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
