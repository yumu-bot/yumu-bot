package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.ScoreLegacy;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.ScoreException;
import com.now.nowbot.util.*;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

//Multiple Score也合并进来了
@Service("SCORE_PR")
public class ScorePRService implements MessageService<ScorePRService.ScorePRParam> {
    private static final Logger log = LoggerFactory.getLogger(ScorePRService.class);

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
    @Resource
    ImageService imageService;

    public record ScorePRParam(@NonNull OsuUser user, List<Score> scores, OsuMode mode) {}


    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<ScorePRParam> data) throws Throwable {
        var matcher = Instructions.SCORE_PR.matcher(messageText);
        if (! matcher.find()) return false;

        var mode = HandleUtil.getMode(matcher);

        boolean isMultiple = StringUtils.hasText(matcher.group("s")) || StringUtils.hasText(matcher.group("es"));
        boolean isPass = ! StringUtils.hasText(matcher.group("recent"));
/*
        var ur = HandleUtil.getUserAndRange(matcher, isMultiple ? 20 : 1, isMultiple);
        OsuUser user;

        if (Objects.isNull(ur.user())) {
            user = HandleUtil.getMyselfUser(event, mode);
        } else {
            user = HandleUtil.getOsuUser(ur.user(), mode);
        }

        mode = HandleUtil.getModeOrElse(mode, user);

        var scores = HandleUtil.getOsuScoreList(user, mode, ur.range(), isPass);

        data.setValue(new ScorePRParam(user, scores, mode));
*/
        return true;
    }

    /*
    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<ScorePRParam> data) throws Throwable {
        var matcher = Instructions.SCORE_PR.matcher(messageText);
        if (! matcher.find()) return false;

        var data = matcher.group("data");
        var s = matcher.group("s");
        var es = matcher.group("es");
        var nStr = matcher.group("n");
        var mStr = matcher.group("m");
        var hasHash = StringUtils.hasText(matcher.group("hash"));

        int offset;
        int limit;
        boolean isRecent;
        boolean isMultipleScore;

        {   // !p 45-55 offset/n = 44 limit/m = 11
            //处理 n，m
            long n = 1L;
            long m;

            var noSpaceAtEnd = StringUtils.hasText(data) && ! data.endsWith(" ") && ! hasHash;

            if (StringUtils.hasText(nStr)) {
                if (noSpaceAtEnd) {
                    // 如果名字后面没有空格，并且有 n 匹配，则主观认为后面也是名字的一部分（比如 !t lolol233）
                    data += nStr;
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
                if (StringUtils.hasText(data)) {
                    data += nStr;
                } else {
                    data = nStr;
                }

                nStr = "";
                n = 1L;
            }

            if (! StringUtils.hasText(mStr)) {
                m = n;
            } else {
                try {
                    m = Long.parseLong(mStr);
                } catch (NumberFormatException e) {
                    throw new ScoreException(ScoreException.Type.SCORE_Score_RankError);
                }
            }

            offset = DataUtil.parseRange2Offset(Math.toIntExact(n), Math.toIntExact(m));
            limit = DataUtil.parseRange2Limit(Math.toIntExact(n), Math.toIntExact(m));

            //如果匹配多成绩模式，则自动设置 offset 和 limit
            if (StringUtils.hasText(s) || StringUtils.hasText(es)) {
                offset = 0;
                if (! StringUtils.hasText(nStr) || isIllegalN) {
                    limit = 20;
                } else if (! StringUtils.hasText(mStr)) {
                    limit = Math.toIntExact(n);
                }
            }
            isMultipleScore = (limit > 1);
        }

        if (matcher.group("recent") != null) {
            isRecent = true;
        } else if (matcher.group("pass") != null) {
            isRecent = false;
        } else {
            throw new ScoreException(ScoreException.Type.SCORE_Send_Error);
        }

        // 构建参数
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        String qqStr = matcher.group("qq");
        BinUser user;
        OsuMode mode = OsuMode.getMode(matcher.group("mode"));

        if (Objects.nonNull(at)) {
            user = bindDao.getUserFromQQ(at.getTarget());
        } else if (StringUtils.hasText(data)) {
            user = new BinUser();
            long id;
            try {
                id = userApiService.getOsuId(data.trim());
                user.setOsuName(data.trim());
            } catch (WebClientResponseException.NotFound e) {
                if (StringUtils.hasText(nStr)) {
                    // 补救机制 1
                    try {
                        id = userApiService.getOsuId(data.concat(nStr));
                        user.setOsuName(data.concat(nStr));
                    } catch (WebClientResponseException.NotFound e1) {
                        throw new ScoreException(ScoreException.Type.SCORE_Player_NotFound, data.concat(nStr));
                    }
                } else {
                    throw new ScoreException(ScoreException.Type.SCORE_Player_NotFound, data.trim());
                }
            } catch (Exception e) {
                throw new ScoreException(ScoreException.Type.SCORE_Player_NotFound, data.trim());
            }

            user.setOsuID(id);
            user.setOsuMode(mode);
        } else if (StringUtils.hasText(qqStr)) {
            try {
                long qq = Long.parseLong(qqStr);
                user = bindDao.getUserFromQQ(qq);
            } catch (BindException e) {
                throw new ScoreException(ScoreException.Type.SCORE_QQ_NotFound, qqStr);
            }
        } else {
            try {
                user = bindDao.getUserFromQQ(event.getSender().getId());
            } catch (BindException e) {
                //退避 !recent
                if (event.getRawMessage().toLowerCase().contains("recent")) {
                    log.info("recent 退避成功");
                    return false;
                } else {
                    throw new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired);
                }
            }
        }

        if (Objects.isNull(user)) {
            throw new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired);
        }

        data.setValue(new ScorePRParam(user, offset, limit, isRecent, isMultipleScore, HandleUtil.getModeOrElse(mode, user)));
        return true;
    }

     */

    @Override
    public void HandleMessage(MessageEvent event, ScorePRParam param) throws Throwable {
        var from = event.getSubject();

        var user = param.user();
        var scores = param.scores();

        if (CollectionUtils.isEmpty(scores)) {
            throw new ScoreException(ScoreException.Type.SCORE_Recent_NotFound, user.getUsername());
        }

        beatmapApiService.applyStarRatingChange(scores);

        if (param.scores().size() > 1) {
            // 多成绩发送
            try {
                var image = imageService.getPanelA5(user, scores);
                from.sendImage(image);
            } catch (Exception e) {
                throw new ScoreException(ScoreException.Type.SCORE_Send_Error);
            }

        } else {
            // 单成绩发送
            var score = scores.getFirst();

            try {
                var image = imageService.getPanelE(user, score, beatmapApiService);
                from.sendImage(image);
            } catch (Exception e) {
                log.error("成绩：绘图出错, 成绩信息:\n {}", JacksonUtil.objectToJsonPretty(score), e);
                getTextOutput(score, from);
            }
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
