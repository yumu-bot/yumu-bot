package com.now.nowbot.service.MessageServiceImpl;

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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

@Service("UU_PR")
public class UUPRService implements MessageService<Matcher> {
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
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instructions.UU_PR.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        var name = matcher.group("name");
        var hasHash = StringUtils.hasText(matcher.group("hash"));

        int offset;
        int limit;
        boolean isRecent;

        if (StringUtils.hasText(matcher.group("recent"))) {
            isRecent = true;
        } else if (StringUtils.hasText(matcher.group("pass"))) {
            isRecent = false;
        } else {
            throw new ScoreException(ScoreException.Type.SCORE_Send_Error);
        }

        //处理 n
        {
            long n = 1L;
            var nStr = matcher.group("n");

            var noSpaceAtEnd = StringUtils.hasText(name) && ! name.endsWith(" ") && ! hasHash;

            if (StringUtils.hasText(nStr)) {
                if (noSpaceAtEnd) {
                    // 如果名字后面没有空格，并且有 n 匹配，则主观认为后面也是名字的一部分（比如 !t lolol233）
                    name += nStr;
                    nStr = "";
                } else {
                    // 如果输入的有空格，并且有名字，后面有数字，则主观认为后面的是天数（比如 !t osu 420），如果找不到再合起来
                    // 没有名字，但有 n 匹配的也走这边 parse
                    try {
                        n = Long.parseLong(nStr);
                    } catch (NumberFormatException e) {
                        throw new ScoreException(ScoreException.Type.SCORE_Score_RankError);
                    }
                }
            }

            //避免 !b 970 这样子被错误匹配
            var isIllegalN = n < 1L || n > 100L;
            if (isIllegalN) {
                if (StringUtils.hasText(name)) {
                    name += nStr;
                } else {
                    name = nStr;
                }

                // nStr = "";
                n = 1L;
            }

            offset = Math.toIntExact(n - 1);
            limit = 1;
        }

        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        BinUser binUser;

        if (Objects.nonNull(at)) {
            binUser = bindDao.getUserFromQQ(at.getTarget());
        } else if (StringUtils.hasText(name)) {
            binUser = new BinUser();
            Long id;
            try {
                id = userApiService.getOsuId(name.trim());
                binUser.setOsuID(id);
            } catch (HttpClientErrorException | WebClientResponseException e) {
                throw new ScoreException(ScoreException.Type.SCORE_Player_NotFound, binUser.getOsuName());
            }
        } else {
            binUser = bindDao.getUserFromQQ(event.getSender().getId());
        }

        if (Objects.isNull(binUser)) throw new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired);

        //处理默认mode
        var mode = OsuMode.getMode(matcher.group("mode"));
        if (mode == OsuMode.DEFAULT) {
            mode = binUser.getMode();
        }

        List<Score> scoreList;

        try {
            scoreList = scoreApiService.getRecent(binUser.getOsuID(), mode, offset, limit, ! isRecent);
        } catch (HttpClientErrorException | WebClientResponseException e) {
            throw new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired);
        }

        if (CollectionUtils.isEmpty(scoreList)) {
            throw new ScoreException(ScoreException.Type.SCORE_Recent_NotFound, binUser.getOsuName());
        }

        //单成绩发送
        try {
            getTextOutput(scoreList.getFirst(), from);
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

