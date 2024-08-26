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
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

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

    public record ScorePRParam(OsuUser user, int offset, int limit, boolean isRecent, boolean isMultipleScore, OsuMode mode) { }

    public record SingleScoreParam(OsuUser user, Score score, int[] density, Double progress, Map<String, Object> original, Map<String, Object> attributes) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<ScorePRParam> data) throws Throwable {
        var matcher = Instruction.SCORE_PR.matcher(messageText);
        if (! matcher.find()) return false;

        var s = matcher.group("s");
        var es = matcher.group("es");

        int offset = 0;
        int limit = 1;
        boolean isRecent;
        boolean isMultipleScore;

        if (matcher.group("recent") != null) {
            isRecent = true;
        } else if (matcher.group("pass") != null) {
            isRecent = false;
        } else {
            log.error("成绩分类失败：");
            throw new ScoreException(ScoreException.Type.SCORE_Send_Error);
        }

        var isMyself = new AtomicBoolean();
        var mode = CmdUtil.getMode(matcher);
        var range = CmdUtil.getUserAndRangeWithBackoff(event, matcher, mode, isMyself, messageText, "recent");

        if (Objects.isNull(range.getData())) {
            throw new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired);
        }

        offset = range.getValue(0, true);
        limit = range.getValue(1, false);
        offset = Math.max(0, offset - 1);
        limit = Math.max(1, limit - offset);
        if ((Objects.nonNull(s) || Objects.nonNull(es)) && range.allNull()) {
            limit = 20;
        }

        isMultipleScore = limit > 1;

        data.setValue(new ScorePRParam(range.getData(), offset, limit, isRecent, isMultipleScore, mode.getData()));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, ScorePRParam param) throws Throwable {
        var from = event.getSubject();

        int offset = param.offset();
        int limit = param.limit();
        boolean isRecent = param.isRecent();
        boolean isMultipleScore = param.isMultipleScore();

        OsuUser user = param.user();

        List<Score> scoreList;

        try {
            scoreList = scoreApiService.getRecent(user.getUserID(), param.mode(), offset, limit, !isRecent);
        } catch (WebClientResponseException e) {
            //退避 !recent
            if (event.getRawMessage().toLowerCase().contains("recent")) {
                log.info("recent 退避成功");
                return;
            } else if (e instanceof WebClientResponseException.Unauthorized) {
                throw new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired);
            } else if (e instanceof WebClientResponseException.Forbidden) {
                throw new ScoreException(ScoreException.Type.SCORE_Player_Banned);
            } else {
                throw new ScoreException(ScoreException.Type.SCORE_Player_NoScore, user.getUsername());
            }
        } catch (Exception e) {
            log.error("成绩：列表获取失败", e);
            throw new ScoreException(ScoreException.Type.SCORE_Score_FetchFailed);
        }

        if (CollectionUtils.isEmpty(scoreList)) {
            throw new ScoreException(ScoreException.Type.SCORE_Recent_NotFound, user.getUsername());
        }

        //成绩发送
        byte[] image;

        if (isMultipleScore) {
            if (event.getSubject().getId() == 595985887L) ContextUtil.setContext("isNewbie", true);
            int scoreSize = scoreList.size();

            //M太大
            if (scoreSize < offset + limit) limit = scoreSize - offset;
            if (limit <= 0) throw new ScoreException(ScoreException.Type.SCORE_Score_OutOfRange);

            var scores = scoreList.subList(offset, offset + limit);
            beatmapApiService.applySRAndPP(scoreList);

            try {
                image = imageService.getPanelA5(user, scores);
                from.sendImage(image);
            } catch (Exception e) {
                log.error("成绩发送失败：", e);
                throw new ScoreException(ScoreException.Type.SCORE_Send_Error);
            }

        } else {
            //单成绩发送
            var score = scoreList.getFirst();
            var e5Param = ScorePRService.getScore4PanelE5(user, score, beatmapApiService);
            try {
                image = imageService.getPanelE5(e5Param);
                from.sendImage(image);
            } catch (Exception e) {
                log.error("成绩：绘图出错, 成绩信息:\n {}", JacksonUtil.objectToJsonPretty(e5Param.score()), e);
                getTextOutput(e5Param.score(), from);
            }
        }
    }

    private void getTextOutput(Score score, Contact from) throws ScoreException {
        var d = ScoreLegacy.getInstance(score, beatmapApiService);

        @SuppressWarnings("unchecked")
        HttpEntity<Byte[]> httpEntity = (HttpEntity<Byte[]>) HttpEntity.EMPTY;
        var imgBytes = template.exchange(d.getUrl(), HttpMethod.GET, httpEntity, byte[].class).getBody();

        //from.sendMessage(new MessageChain.MessageChainBuilder().addImage(imgBytes).addText(d.getScoreLegacyOutput()).build());
        QQMsgUtil.sendImageAndText(from, imgBytes, d.getScoreLegacyOutput());
    }

    // 这些本来都是绘图模块算的，任务太重！
    public static SingleScoreParam getScore4PanelE5(OsuUser user, Score score, OsuBeatmapApiService beatmapApiService) throws Exception {
        var b = score.getBeatMap();

        beatmapApiService.applyBeatMapExtend(score);

        var original = new HashMap<String, Object>(6);
        original.put("cs", b.getCS());
        original.put("ar", b.getAR());
        original.put("od", b.getOD());
        original.put("hp", b.getHP());
        original.put("bpm", b.getBPM());
        original.put("drain", b.getHitLength());
        original.put("total", b.getTotalLength());

        beatmapApiService.applySRAndPP(score);

        var attributes = beatmapApiService.getStatistics(score);
        attributes.put("full_pp", beatmapApiService.getFcPP(score).getPp());
        attributes.put("perfect_pp", beatmapApiService.getMaxPP(score).getPp());

        var density = beatmapApiService.getBeatmapObjectGrouping26(b);
        var progress = beatmapApiService.getPlayPercentage(score);

        return new SingleScoreParam(user, score, density, progress, original, attributes);
    }
}
