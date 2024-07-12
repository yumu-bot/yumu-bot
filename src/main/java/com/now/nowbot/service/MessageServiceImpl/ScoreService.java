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
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Objects;

@Service("SCORE")
public class ScoreService implements MessageService<ScoreService.ScoreParam> {
    private static final Logger log = LoggerFactory.getLogger(ScoreService.class);
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

    public record ScoreParam(BinUser user, OsuMode mode, long bid, String modsStr, boolean isDefault, boolean isMyself) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<ScoreParam> data) throws ScoreException {
        var matcher = Instructions.SCORE.matcher(messageText);
        if (! matcher.find()) {
            return false;
        }

        // 构建参数
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        String qqStr = matcher.group("qq");
        BinUser user;
        OsuMode mode = OsuMode.getMode(matcher.group("mode"));
        boolean isMyself = false;
        boolean isDefault = false;

        var bid = Long.parseLong(matcher.group("bid"));
        var mod = matcher.group("mod");
        var name = matcher.group("name");

        if (Objects.nonNull(at)) {
            try {
                user = bindDao.getUserFromQQ(at.getTarget());
            } catch (Exception e) {
                throw new ScoreException(ScoreException.Type.SCORE_Player_TokenExpired);
            }
        } else if (StringUtils.hasText(name)) {
            user = new BinUser();
            Long id;
            try {
                id = userApiService.getOsuId(name.trim());
                user.setOsuID(id);
                user.setOsuName(name.trim());
            } catch (WebClientResponseException.NotFound | NumberFormatException e) {
                throw new ScoreException(ScoreException.Type.SCORE_Player_NotFound, name.trim());
            }
        } else if (StringUtils.hasText(qqStr)) {
            try {
                user = bindDao.getUserFromQQ(Long.parseLong(qqStr));
            } catch (BindException | NumberFormatException e) {
                throw new ScoreException(ScoreException.Type.SCORE_QQ_NotFound, qqStr);
            }
        } else {
            try {
                user = bindDao.getUserFromQQ(event.getSender().getId());
                isMyself = true;
            } catch (BindException e) {
                //退避 !score
                if (messageText.toLowerCase().contains("score")) {
                    log.info("score 退避成功");
                    return false;
                } else {
                    throw new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired);
                }
            }
        }

        if (Objects.isNull(user)) {
            throw new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired);
        }

        if (OsuMode.isDefaultOrNull(mode)) {
            isDefault = true;
            mode = user.getOsuMode();
        }

        data.setValue(
                new ScoreParam(user, mode, bid, mod, isDefault, isMyself)
        );
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, ScoreParam param) throws Throwable {
        var from = event.getSubject();
        var mode = param.mode();
        var binUser = param.user();
        boolean isDefault = param.isDefault();

        var bid = param.bid();

        // 处理 mods
        var modsStr = param.modsStr();

        Score score;
        if (StringUtils.hasText(modsStr)) {
            BeatmapUserScore scoreall;
            List<OsuMod> osuMods = OsuMod.getModsList(modsStr);
            try {
                scoreall = scoreApiService.getScore(bid, binUser, mode, osuMods);
                score = scoreall.getScore();
            } catch (WebClientResponseException e) {
                throw new ScoreException(ScoreException.Type.SCORE_Score_NotFound, String.valueOf(bid));
            }

            /*
            var beatMap = new BeatMap();
            beatMap.setBeatMapID(bid);
            score.setBeatMap(beatMap);

             */

        } else {
            try {
                score = scoreApiService.getScore(bid, binUser, mode).getScore();
            } catch (WebClientResponseException.NotFound e) {
                //当在玩家设定的模式上找不到时，寻找基于谱面获取的游戏模式的成绩
                if (isDefault) {
                    try {
                        score = scoreApiService.getScore(bid, binUser, OsuMode.DEFAULT).getScore();
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

        //这里的mode必须用谱面传过来的
        OsuUser user;
        try {
            user = userApiService.getPlayerInfo(binUser, score.getMode());
        } catch (Exception e) {
            log.error("成绩：获取失败", e);
            throw new ScoreException(ScoreException.Type.SCORE_Player_NoScore, binUser.getOsuName());
        }

        byte[] image;

        try {
            var e5Param = ScorePRService.getScore4PanelE5(user, score, beatmapApiService);
            var excellent = DataUtil.isExcellentScore(e5Param.score(), user);


            if (excellent) {
                image = imageService.getPanelE5(e5Param);
            } else {
                image = imageService.getPanelE(user, e5Param.score());
            }

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
