package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.throwable.ServiceException.BPException;
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

    public record BPParam(OsuUser user, OsuMode mode, Map<Integer, Score> scores, boolean isMyself) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<BPParam> data) throws Throwable {
        Matcher matcher;
        boolean isMultiple;

        var singleMatcher = Instructions.BP.matcher(messageText);
        var multipleMatcher = Instructions.BS.matcher(messageText);

        // 都没找到才 false
        if (singleMatcher.find() || multipleMatcher.find()) {
            if (singleMatcher.find()) {
                matcher = singleMatcher;
                isMultiple = false;
            } else {
                matcher = multipleMatcher;
                isMultiple = true;
            }
        } else {
            return false;
        }

        var isMySelf = false;
        var mode = HandleUtil.getMode(matcher);
        var user = HandleUtil.getOtherUser(event, matcher, mode);

        if (Objects.isNull(user)) {
            isMySelf = true;

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

        var scores = HandleUtil.getOsuBPList(user, matcher, mode, isMultiple);

        data.setValue(new BPParam(user, mode, scores, isMySelf));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, BPParam param) throws Throwable {
        var from = event.getSubject();

        var bpMap = param.scores();
        var mode = param.mode();

        if (CollectionUtils.isEmpty(bpMap)) throw new BPException(BPException.Type.BP_Player_NoBP, mode);
        var osuUser = param.user();

        byte[] image;

        try {
            if (bpMap.size() > 1) {
                var rankList = new ArrayList<Integer>();
                var scoreList = new ArrayList<Score>();
                for (var e : bpMap.entrySet()) {
                    rankList.add(e.getKey());
                    scoreList.add(e.getValue());
                }
                //log.info("{}'s score: {}", osuUser.getUsername(), JacksonUtil.toJson(rankList));
                image = imageService.getPanelA4(osuUser, scoreList, rankList);
            } else {
                Score score = null;
                for (var e : bpMap.entrySet()) {
                    score = e.getValue();
                }
                //log.info("{}'s score: {}", osuUser.getUsername(), score.getPP());
                image = imageService.getPanelE(osuUser, score, beatmapApiService);
            }
            // 玩家信息获取已经移动至 HandleUtil，故删去不可能进入的 catch
        } catch (Exception e) {
            log.error("最好成绩：渲染失败", e);
            throw new BPException(BPException.Type.BP_Render_Failed);
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("最好成绩：发送失败", e);
            throw new BPException(BPException.Type.BP_Send_Failed);
        }
    }
}
