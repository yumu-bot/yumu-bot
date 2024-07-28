package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.BeatmapUserScore;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.ServiceException.ScoreException;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.CmdUtil;
import com.now.nowbot.util.Instruction;
import com.now.nowbot.util.QQMsgUtil;
import com.now.nowbot.util.command.CmdPatternStaticKt;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Service("SCORE")
public class ScoreService implements MessageService<ScoreService.ScoreParam> {
    private static final Logger log = LoggerFactory.getLogger(ScoreService.class);
    @Resource
    OsuScoreApiService   scoreApiService;
    @Resource
    OsuBeatmapApiService beatmapApiService;
    @Resource
    ImageService         imageService;

    public record ScoreParam(OsuUser user, OsuMode mode, long bid, String modsStr, boolean isDefault,
                             boolean isMyself) {
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<ScoreParam> data) throws TipsException {
        var matcher = Instruction.SCORE.matcher(messageText);
        if (!matcher.find()) {
            return false;
        }

        var mode = CmdUtil.getMode(matcher);
        var isMyself = new AtomicBoolean(false);
        var isDefault = OsuMode.isDefaultOrNull(mode.getData());
        OsuUser user;
        try {
            user = CmdUtil.getUserWithOutRange(event, matcher, mode, isMyself);
        } catch (BindException e) {
            if (isMyself.get() && messageText.toLowerCase().contains("score")) {
                log.info("score 退避");
                return false;
            }
            throw isMyself.get() ?
                    new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired) :
                    new ScoreException(ScoreException.Type.SCORE_Player_TokenExpired);
        }

        if (Objects.isNull(user)) {
            throw new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired);
        }

        var bid = CmdUtil.getBid(matcher);
        data.setValue(new ScoreParam(user, mode.getData(), bid, CmdUtil.getMod(matcher), isDefault, isMyself.get()));

        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, ScoreParam param) throws Throwable {
        var from = event.getSubject();
        var mode = param.mode();
        var user = param.user();
        boolean isDefault = param.isDefault();

        var bid = param.bid();

        // 处理 mods
        var modsStr = param.modsStr();

        Score score;
        if (StringUtils.hasText(modsStr)) {
            BeatmapUserScore scoreall;
            List<OsuMod> osuMods = OsuMod.getModsList(modsStr);
            try {
                scoreall = scoreApiService.getScore(bid, user.getUserID(), mode, osuMods);
                score = scoreall.getScore();
            } catch (WebClientResponseException e) {
                throw new ScoreException(ScoreException.Type.SCORE_Score_NotFound, String.valueOf(bid));
            }
            beatmapApiService.applyBeatMapExtend(score);

        } else {
            try {
                score = scoreApiService.getScore(bid, user.getUserID(), mode).getScore();
            } catch (WebClientResponseException.NotFound e) {
                //当在玩家设定的模式上找不到时，寻找基于谱面获取的游戏模式的成绩
                if (isDefault) {
                    try {
                        score = scoreApiService.getScore(bid, user.getUserID(), OsuMode.DEFAULT).getScore();
                    } catch (WebClientResponseException e1) {
                        throw new ScoreException(ScoreException.Type.SCORE_Mode_NotFound);
                    }
                } else {
                    throw new ScoreException(ScoreException.Type.SCORE_Mode_SpecifiedNotFound, mode.getName());
                }
            } catch (WebClientResponseException.Unauthorized e) {
                if (param.isMyself()) {
                    throw new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired);
                } else {
                    throw new ScoreException(ScoreException.Type.SCORE_Player_TokenExpired);
                }
            }
        }

        byte[] image;
        var e5Param = ScorePRService.getScore4PanelE5(user, score, beatmapApiService);

        try {
            image = imageService.getPanelE5(e5Param);

            /*
            var excellent = DataUtil.isExcellentScore(e5Param.score(), user);

            if (excellent || Permission.isSuperAdmin(event.getSender().getId())) {
            } else {
                image = imageService.getPanelE(user, e5Param.score());
            }
            */

        } catch (Exception e) {
            log.error("成绩：渲染失败", e);
            throw new ScoreException(ScoreException.Type.SCORE_Render_Error);
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("成绩：发送失败", e);
            throw new ScoreException(ScoreException.Type.SCORE_Send_Error);
        }
    }
}
