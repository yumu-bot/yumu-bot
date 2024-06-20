package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.JsonData.ScoreWithFcPP;
import com.now.nowbot.model.enums.OsuMod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.throwable.GeneralTipsException;
import com.now.nowbot.util.ContextUtil;
import com.now.nowbot.util.HandleUtil;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service("BP_FIX")
public class BPFixService implements MessageService<BPFixService.BPFixParam> {
    private static final Logger log = LoggerFactory.getLogger(BPFixService.class);

    @Resource
    ImageService         imageService;
    @Resource
    OsuBeatmapApiService beatmapApiService;

    public record BPFixParam(OsuUser user, Map<Integer, Score> bpMap, OsuMode mode) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<BPFixParam> data) throws Throwable {
        var matcher = Instructions.BP_FIX.matcher(messageText);
        if (! matcher.find()) return false;

        var mode = HandleUtil.getMode(matcher);
        var user = HandleUtil.getOtherUser(event, matcher, mode, 100);

        if (Objects.isNull(user)) {
            user = HandleUtil.getMyselfUser(event, mode);
        }

        mode = HandleUtil.getModeOrElse(mode, user);

        var bpMap = HandleUtil.getOsuBPMap(user, mode, 0, 100);

        data.setValue(new BPFixParam(user, bpMap, mode));

        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, BPFixParam param) throws Throwable {
        var from = event.getSubject();

        var bpMap = param.bpMap();
        var mode = param.mode();
        var user = param.user();

        if (CollectionUtils.isEmpty(bpMap)) throw new GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerRecord, mode.getName());

        var fixData = fix(Objects.requireNonNullElse(user.getPP(), 0d), bpMap);

        if (CollectionUtils.isEmpty(fixData)) throw new GeneralTipsException(GeneralTipsException.Type.G_Null_TheoreticalBP);

        byte[] image;

        try {
            image = imageService.getPanelA7(user, fixData);
        } catch (Exception e) {
            log.error("理论最好成绩：渲染失败", e);
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "理论最好成绩");
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("理论最好成绩：发送失败", e);
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "理论最好成绩");
        }
    }

    // 主计算
    @Nullable
    public Map<String, Object> fix(@NonNull double playerPP, @Nullable Map<Integer, Score> bpMap) {
        if (CollectionUtils.isEmpty(bpMap)) return null;

        var bpList = new ArrayList<Score>(bpMap.size());
        AtomicReference<Float> beforeBpSumAtomic = new AtomicReference<>(0f);

        bpMap.forEach((index, score) -> {
            beforeBpSumAtomic.updateAndGet(v -> v + score.getWeight().weightedPP());
            var beatmap = beatmapApiService.getMapInfoFromDB(score.getBeatMap().getId());
            score.setBeatMap(beatmap);

            int max = beatmap.getMaxCombo();
            int combo = score.getMaxCombo();

            int miss = Objects.requireNonNullElse(score.getStatistics().getCountMiss(), 0);
            int all = Objects.requireNonNullElse(score.getStatistics().getCountAll(score.getMode()), 1);

            // 断连击，mania 模式不参与此项筛选
            boolean isChoke = (miss == 0) && (combo < Math.round(max * 0.98f)) && (score.getMode() != OsuMode.MANIA);

            // 含有 <1% 的失误
            boolean has1pMiss = (miss > 0) && ((1f * miss / all) <= 0.01f);

            // 并列关系，miss 不一定 choke（断尾不会计入 choke），choke 不一定 miss（断滑条
            if (isChoke || has1pMiss) {
                bpList.add(
                        initFixScore(score, index + 1, miss)
                );
            } else {
                bpList.add(score);
            }
        });

        bpList.sort((s1, s2) -> {
            float pp1;
            float pp2;
            if (s1 instanceof ScoreWithFcPP s) pp1 = s.getFcPP(); else pp1 = s1.getPP();
            if (s2 instanceof ScoreWithFcPP s) pp2 = s.getFcPP(); else pp2 = s2.getPP();
            return Math.round(pp2 * 100 - pp1 * 100);
        });

        AtomicReference<Float> afterBpSumAtomic = new AtomicReference<>(0f);
        bpList.forEach(ContextUtil.consumerWithIndex((score, index) -> {
            double weight = Math.pow(0.95f, index);
            float pp;
            if (score instanceof ScoreWithFcPP fc) {
                pp = fc.getFcPP();
                fc.setIndexAfter(index + 1);
            } else {
                pp = score.getPP();
            }
            afterBpSumAtomic.updateAndGet(v -> v + (float) (weight * pp));
        }));

        float beforeBpSum = beforeBpSumAtomic.get();
        float afterBpSum = afterBpSumAtomic.get();
        float newPlayerPP = (float) (playerPP + afterBpSum - beforeBpSum);

        var scoreList = bpList.stream().filter(s -> s instanceof ScoreWithFcPP).map(s -> (ScoreWithFcPP)s).toList();
        var result = new HashMap<String, Object>(2);
        result.put("scores", scoreList);
        result.put("pp", newPlayerPP);

        return result;
    }

    private ScoreWithFcPP initFixScore(Score score, int index, int countMiss) {
        var result = ScoreWithFcPP.copyOf(score);
        result.setIndex(index + 1);
        var statistics = score.getStatistics();
        statistics.handleNull();
        if (countMiss > 0) {
            statistics.setCountMiss(0);
            int count300 = Objects.requireNonNullElse(statistics.getCount300(), 0);
            statistics.setCount300(count300 + countMiss);
        }
        statistics.setMaxCombo(score.getBeatMap().getMaxCombo());
        var bid = score.getBeatMap().getId();
        var mods = OsuMod.getModsValueFromAbbrList(score.getMods());

        try {
            var pp = beatmapApiService.getPP(bid, score.getMode(), mods, statistics);
            result.setFcPP((float) pp.getPp());
        } catch (Exception e) {
            log.error("bp 计算 pp 出错:", e);
        }
        return result;
    }
}
