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
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.ScoreException;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.regex.Matcher;

@Service("UUPR")
public class UUPRService implements MessageService {

    RestTemplate template;
    @Autowired
    OsuGetService osuGetService;
    @Autowired
    BindDao bindDao;

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

            offset = n;
            limit = 1;
        }

        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        BinUser binUser;
        OsuUser osuUser;

        if (at != null) {
            binUser = bindDao.getUser(at.getTarget());
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
                binUser = bindDao.getUser(event.getSender().getId());
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
        } catch (HttpClientErrorException e) {
            throw new ScoreException(ScoreException.Type.SCORE_Me_NoAuthorization);
        }

        if (scoreList == null || scoreList.isEmpty()) {
            throw new ScoreException(ScoreException.Type.SCORE_Recent_NotFound);
        }

        try {
            osuUser = osuGetService.getPlayerInfo(binUser, mode);
        } catch (Exception e) {
            throw new ScoreException(ScoreException.Type.SCORE_Player_NotFound);
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
        var d = ScoreLegacy.getInstance(score);
        HttpEntity<Byte[]> httpEntity = (HttpEntity<Byte[]>) HttpEntity.EMPTY;
        var imgBytes = template.exchange(d.getUrl(), HttpMethod.GET, httpEntity, byte[].class).getBody();

        //from.sendMessage(new MessageChain.MessageChainBuilder().addImage(imgBytes).addText(d.getScoreLegacyOutput()).build());
        QQMsgUtil.sendTextAndImage(from, d.getScoreLegacyOutput(), imgBytes);
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

