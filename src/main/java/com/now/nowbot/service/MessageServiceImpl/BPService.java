package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.throwable.GeneralTipsException;
import com.now.nowbot.util.CmdRange;
import com.now.nowbot.util.CmdUtil;
import com.now.nowbot.util.ContextUtil;
import com.now.nowbot.util.Instruction;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service("BP")
public class BPService implements MessageService<BPService.BPParam> {
    private static final Logger log              = LoggerFactory.getLogger(BPService.class);
    private static final int    DEFAULT_BP_COUNT = 20;
    @Resource
    OsuBeatmapApiService beatmapApiService;
    @Resource
    OsuScoreApiService   scoreApiService;
    @Resource
    ImageService         imageService;

    public record BPParam(OsuUser user, OsuMode mode, Map<Integer, Score> BPMap, boolean isMyself) {
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<BPParam> data) throws Throwable {
        var matcher = Instruction.BP.matcher(messageText);
        if (!matcher.find()) return false;

        boolean isMultiple = StringUtils.hasText(matcher.group("s"));
        var isMyself = new AtomicBoolean();
        // 处理 range
        var mode = CmdUtil.getMode(matcher);
        CmdRange<OsuUser> range;
        range = CmdUtil.getUserAndRangeWithBackoff(event, matcher, mode, isMyself, messageText, "bp");

        int offset = range.getValue(1, true) - 1;
        int limit = range.getValue(0, false);

        if (isMultiple) {
            offset = Math.max(0, offset);
            if (limit < 1) {
                limit = offset == 0 ? DEFAULT_BP_COUNT : offset + 1;
                offset = 0;
            }
        } else if (limit <= 1) {
            limit = 1;
        } else {
            limit = Math.max(1, limit - offset);
        }


        var user = range.getData();
        if (Objects.isNull(user)) return false;

        var bpList = scoreApiService.getBestPerformance(user.getUserID(), mode.getData(), offset, limit);
        var bpMap = new TreeMap<Integer, Score>();
        int finalOffset = offset;
        bpList.forEach(
                ContextUtil.consumerWithIndex(
                        (s, index) -> bpMap.put(index + finalOffset, s)
                )
        );


        data.setValue(new BPParam(user, mode.getData(), bpMap, isMyself.get()));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, BPParam param) throws Throwable {
        var from = event.getSubject();

        var BPMap = param.BPMap();
        var mode = param.mode();
        var user = param.user();

        if (CollectionUtils.isEmpty(BPMap))
            throw new GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerRecord, mode);

        byte[] image;

        try {
            if (BPMap.size() > 1) {
                var ranks = new ArrayList<Integer>();
                var scores = new ArrayList<Score>();
                for (var e : BPMap.entrySet()) {
                    ranks.add(e.getKey() + 1);
                    scores.add(e.getValue());
                }

                beatmapApiService.applySRAndPP(scores);

                image = imageService.getPanelA4(user, scores, ranks);
            } else {
                Score score = null;

                for (var e : BPMap.entrySet()) {
                    score = e.getValue();
                }

                var e5Param = ScorePRService.getScore4PanelE5(user, score, beatmapApiService);
                image = imageService.getPanelE5(e5Param);
                /*
                var excellent = DataUtil.isExcellentScore(e5Param.score(), user);

                if (excellent || Permission.isSuperAdmin(event.getSender().getId())) {

                } else {
                    image = imageService.getPanelE(user, e5Param.score());
                }

                 */
            }
        } catch (Exception e) {
            log.error("最好成绩：渲染失败", e);
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "最好成绩");
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("最好成绩：发送失败", e);
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "最好成绩");
        }
    }
}
