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
import com.now.nowbot.util.HandleUtil;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

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
        Matcher matcher;

        var singleMatcher = Instructions.BP.matcher(messageText);
        var multipleMatcher = Instructions.BS.matcher(messageText);

        boolean isSingle = singleMatcher.find();
        boolean isMultiple = multipleMatcher.find();

        // 都没找到才 false
        if (isSingle || isMultiple) {
            if (isSingle) {
                matcher = singleMatcher;
            } else {
                matcher = multipleMatcher;
            }
        } else {
            return false;
        }

        var isMyself = false;
        var mode = HandleUtil.getMode(matcher);
        var user = HandleUtil.getOtherUser(event, matcher, mode, 100);

        if (Objects.isNull(user)) {
            isMyself = true;

            try {
                user = HandleUtil.getMyselfUser(event, mode);
            } catch (BindException e) {
                if (HandleUtil.isAvoidance(event, "bp")) {
                    log.info(String.format("指令退避：BP 退避成功，被退避的玩家：%s", event.getSender().getName()));
                    return false;
                } else {
                    throw e;
                }
            }
        }

        var BPMap = HandleUtil.getOsuBPMap(user, matcher, mode, isMultiple);

        data.setValue(new BPParam(user, mode, BPMap, isMyself));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, BPParam param) throws Throwable {
        var from = event.getSubject();

        var BPMap = param.BPMap();
        var mode = param.mode();
        var user = param.user();

        if (CollectionUtils.isEmpty(BPMap)) throw new GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerRecord, mode);

        byte[] image;

        try {
            if (BPMap.size() > 1) {
                var rankList = new ArrayList<Integer>();
                var scoreList = new ArrayList<Score>();
                for (var e : BPMap.entrySet()) {
                    rankList.add(e.getKey());
                    scoreList.add(e.getValue());
                }
                image = imageService.getPanelA4(user, scoreList, rankList);
            } else {
                Score score = null;
                for (var e : BPMap.entrySet()) {
                    score = e.getValue();
                }
                image = imageService.getPanelE(user, score, beatmapApiService);
            }
            // 玩家信息获取已经移动至 HandleUtil，故删去不可能进入的 catch
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
