package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
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
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.ScoreException;
import com.now.nowbot.util.QQMsgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("SCOREPR")
public class ScorePRService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(ScorePRService.class);

    RestTemplate template;
    OsuGetService osuGetService;
    BindDao bindDao;
    ImageService imageService;

    @Autowired
    public ScorePRService(RestTemplate restTemplate, OsuGetService osuGetService, BindDao bindDao, ImageService image) {
        template = restTemplate;
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
        imageService = image;
    }
    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)((ym)?(?<pass>(pass|p(?![a-zA-Z_])))|(ym)?(?<recent>(recent|r(?!\\w))))+\\s*([:：](?<mode>[\\w\\d]+))?(?![\\w])(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*?))?\\s*(#?(?<n>\\d+)([-－](?<m>\\d+))?)?$");

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
                    id = osuGetService.getOsuId(name.trim());
                    binUser.setOsuID(id);
                } catch (HttpClientErrorException e) {
                    throw new ScoreException(ScoreException.Type.SCORE_Player_NotFound);
                }
            } else {
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
                if (isRecent && matcher.group("recent").equalsIgnoreCase("recent")) {
                    NowbotApplication.log.info("recent 退避成功");
                    return;
                } else {
                    throw new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired);
                }
            }
        } catch (HttpClientErrorException e) {
            //退避 !recent
            if (isRecent && matcher.group("recent").equalsIgnoreCase("recent")) {
                NowbotApplication.log.info("recent 退避成功");
                return;
            } else if (e instanceof HttpClientErrorException.Unauthorized) {
                throw new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired);
            } else if (e instanceof HttpClientErrorException.NotFound) {
                throw new ScoreException(ScoreException.Type.SCORE_Player_Banned);
            } else {
                log.error("Score List 获取失败", e);
                throw new ScoreException(ScoreException.Type.SCORE_Score_FetchFailed);
            }
        }

        if (scoreList == null || scoreList.isEmpty()) {
            throw new ScoreException(ScoreException.Type.SCORE_Recent_NotFound);
        }

        try {
            osuUser = osuGetService.getPlayerInfo(binUser, mode);
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
                var data = imageService.getPanelE(osuUser, scoreList.get(0), osuGetService);
                QQMsgUtil.sendImage(from, data);
            } catch (Exception e) {
                log.error("为什么要转 Legacy 方法发送呢？直接重试不就好了", e);
                getTextOutput(scoreList.get(0), from);
            }
        }
    }

    private byte[] getAlphaPanel(OsuMode mode, int offset, int limit, boolean isRecent) throws ScoreException {
        var s = getData(bindDao.getUserFromQQ(365246692L), mode, offset, limit, isRecent);
        if (CollectionUtils.isEmpty(s)) throw new ScoreException(ScoreException.Type.SCORE_Recent_NotFound);
        return imageService.spInfo(s.get(0));
    }

    private void getTextOutput(Score score, Contact from) {
        var d = ScoreLegacy.getInstance(score, osuGetService);
        HttpEntity<Byte[]> httpEntity = (HttpEntity<Byte[]>) HttpEntity.EMPTY;
        var imgBytes = template.exchange(d.getUrl(), HttpMethod.GET, httpEntity, byte[].class).getBody();

        //from.sendMessage(new MessageChain.MessageChainBuilder().addImage(imgBytes).addText(d.getScoreLegacyOutput()).build());
        QQMsgUtil.sendImageAndText(from, imgBytes, d.getScoreLegacyOutput());
    }

    private List<Score> getData(BinUser user, OsuMode mode, int offset, int limit, boolean isRecent) {
        if (isRecent)
            return osuGetService.getAllRecentN(user, mode, offset, limit);
        else
            return osuGetService.getRecentN(user, mode, offset, limit);
    }

    private List<Score> getData(Long id, OsuMode mode, int offset, int limit, boolean isRecent) {
        if (isRecent)
            return osuGetService.getAllRecentN(id, mode, offset, limit);
        else
            return osuGetService.getRecentN(id, mode, offset, limit);
    }
}