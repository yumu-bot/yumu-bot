package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.OsuUserInfoDao;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.throwable.ServiceException.InfoException;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.CmdUtil;
import com.now.nowbot.util.Instruction;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service("INFO")
public class InfoService implements MessageService<InfoService.InfoParam> {
    private static final Logger log = LoggerFactory.getLogger(InfoService.class);
    @Resource
    OsuScoreApiService scoreApiService;
    @Resource
    OsuUserInfoDao     infoDao;
    @Resource
    ImageService       imageService;

    public record InfoParam(OsuUser user, OsuMode mode, int day, boolean isMyself) {
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<InfoParam> data) throws TipsException {
        var matcher = Instruction.INFO.matcher(messageText);
        if (!matcher.find()) return false;

        var mode = CmdUtil.getMode(matcher);
        var isMyself = new AtomicBoolean();
        var user = CmdUtil.getUserWithOutRange(event, matcher, mode, isMyself);

        var dayStr = matcher.group("day");
        int day;
        // 处理回溯天数，默认比对昨天的
        if (StringUtils.hasText(dayStr)) {
            try {
                day = Integer.parseInt(dayStr);
            } catch (NumberFormatException e) {
                day = 1;
            }
        } else {
            day = 1;
        }
        data.setValue(new InfoParam(user, mode.getData(), day, isMyself.get()));

        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, InfoParam param) throws Throwable {
        var from = event.getSubject();

        var user = param.user;
        var mode = param.mode;

        OsuUser osuUser = param.user();
        List<Score> BPs;

        try {
            BPs = scoreApiService.getBestPerformance(user.getUserID(), mode, 0, 100);
        } catch (WebClientResponseException.NotFound e) {
            throw new InfoException(InfoException.Type.I_Player_NoBP, param.mode());
        } catch (Exception e) {
            log.error("玩家信息：无法获取玩家 BP", e);
            throw new InfoException(InfoException.Type.I_BP_FetchFailed);
        }


        var historyUser =
                infoDao.getLastFrom(osuUser.getUserID(),
                                mode == OsuMode.DEFAULT ? osuUser.getCurrentOsuMode() : mode,
                                LocalDate.now().minusDays(param.day))
                        .map(OsuUserInfoDao::fromArchive);

//        if (ContextUtil.isTestUser() && historyUser.isPresent()) {
//            log.info("Json: {}", JacksonUtil.objectToJsonPretty(historyUser.orElse(null)));
//            log.info("info  {} -> {}", historyUser.get().getGlobalRank(), osuUser.getGlobalRank());
//        }
        byte[] image;

        try {
            image = imageService.getPanelD(osuUser, historyUser, BPs, mode);
        } catch (Exception e) {
            log.error("玩家信息：图片渲染失败", e);
            throw new InfoException(InfoException.Type.I_Fetch_Error);
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("玩家信息：发送失败", e);
            throw new InfoException(InfoException.Type.I_Send_Error);
        }
    }
}
