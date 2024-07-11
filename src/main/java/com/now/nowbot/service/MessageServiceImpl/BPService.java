package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.throwable.GeneralTipsException;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.HandleUtil;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import static com.now.nowbot.service.MessageServiceImpl.ScorePRService.getScore4PanelE5;

@Service("BP")
public class BPService implements MessageService<BPService.BPParam> {
    private static final Logger log = LoggerFactory.getLogger(BPService.class);
    @Resource
    OsuBeatmapApiService beatmapApiService;
    @Resource
    ImageService imageService;

    public record BPParam(OsuUser user, OsuMode mode, Map<Integer, Score> BPMap, boolean isMyself) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<BPParam> data) throws Throwable {
        var matcher = Instructions.BP.matcher(messageText);
        if (! matcher.find()) return false;

        boolean isMultiple = StringUtils.hasText(matcher.group("s"));

        var isMyself = false;

        // 处理 range
        var mode = HandleUtil.getMode(matcher);
        var user = HandleUtil.getOtherUser(event, matcher, mode, 100);

        if (Objects.isNull(user)) {
            isMyself = true;

            try {
                user = HandleUtil.getMyselfUser(event, mode);
            } catch (BindException e) {
                if (HandleUtil.isAvoidance(messageText, "bp")) {
                    log.info(String.format("指令退避：BP 退避成功，被退避的玩家：%s", event.getSender().getName()));
                    return false;
                } else {
                    throw e;
                }
            }
        }

        var BPMap = HandleUtil.getOsuBPMap(user, matcher, HandleUtil.getModeOrElse(mode, user), isMultiple ? 20 : 1, isMultiple);

        data.setValue(new BPParam(user, mode, BPMap, isMyself));
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
                var rankList = new ArrayList<Integer>();
                var scoreList = new ArrayList<Score>();
                for (var e : BPMap.entrySet()) {
                    rankList.add(e.getKey() + 1);
                    scoreList.add(e.getValue());
                }
                image = imageService.getPanelA4(user, scoreList, rankList);
            } else {
                Score score = null;

                for (var e : BPMap.entrySet()) {
                    score = e.getValue();
                }

                var e5Param = getScore4PanelE5(user, score, beatmapApiService);
                var excellent = DataUtil.isExcellentScore(e5Param.score(), user.getPP());

                if (excellent) {
                    image = imageService.getPanelE5(e5Param);
                } else {
                    image = imageService.getPanelE(user, e5Param.score());
                }
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
