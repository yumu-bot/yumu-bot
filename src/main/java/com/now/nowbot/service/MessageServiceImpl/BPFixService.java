package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.JsonData.ScoreFc;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.GeneralTipsException;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.HandleUtil;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;

@Service("BP_FIX")
public class BPFixService implements MessageService<BPFixService.BPFixParam> {
    private static final Logger log = LoggerFactory.getLogger(BPFixService.class);
    @Resource
    ImageService imageService;

    public record BPFixParam(OsuUser user, Map<Integer, Score> bpMap, OsuMode mode) {}

    public record BPFix(Long id, Float fixPP) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<BPFixParam> data) throws Throwable {
        var matcher = Instructions.BP_FIX.matcher(messageText);
        if (! matcher.find()) return false;


        var mode = HandleUtil.getMode(matcher);
        var user = HandleUtil.getOtherUser(event, matcher, mode, 100);

        if (Objects.isNull(user)) {
            user = HandleUtil.getMyselfUser(event, mode);
        }

        var bpMap = HandleUtil.getOsuBPMap(user, mode, 0, 100);

        data.setValue(
                new BPFixParam(user, bpMap, mode)
        );

        return true;


    }

    @Override
    public void HandleMessage(MessageEvent event, BPFixParam param) throws Throwable {
        var from = event.getSubject();

        var bpMap = param.bpMap();
        var mode = param.mode();
        var user = param.user();

        if (CollectionUtils.isEmpty(bpMap)) throw new GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerRecord, mode.getName());

        var fixes = getBPFixList(bpMap);

        if (CollectionUtils.isEmpty(fixes)) throw new GeneralTipsException(GeneralTipsException.Type.G_Null_TheoreticalBP);

        byte[] image;

        try {
            image = imageService.getPanelA7(user, fixes);
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
    public Map<String, Object> getBPFixList(@Nullable Map<Integer, Score> BPMap) throws TipsException {
        if (CollectionUtils.isEmpty(BPMap)) return null;

        // 筛选需要 fix 的图，带 miss 的
        var rankList = new ArrayList<Integer>();
        var scoreList = new ArrayList<ScoreFc>();

        for (var e : BPMap.entrySet()) {
            int miss = Objects.requireNonNullElse(e.getValue().getStatistics().getCountMiss(), 0);

            // 1-10miss 是可以 fix 的
            if (miss > 0 && miss <= 10) {
                rankList.add(e.getKey());
                scoreList.add(ScoreFc.copyOf(e.getValue()));
            }
        }

        if (CollectionUtils.isEmpty(scoreList)) return null;

        Map<Long, Float> fixMap;

        try {
            fixMap = imageService.getBPFix(scoreList);
        } catch (ResourceAccessException | HttpServerErrorException.InternalServerError e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Overtime_ExchangeTooMany, "理论最好成绩");
        } catch (WebClientResponseException e) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_RenderDisconnected, "理论最好成绩");
        }


        for (var s : scoreList) {
            var f = fixMap.get(s.getBeatMap().getId());
            s.setFcPP(f);
        }

        var result = new HashMap<String, Object>(2);
        result.put("scores", scoreList);
        result.put("ranks", rankList);

        return result;
    }
}
