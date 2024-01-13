package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.LogException;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.ServiceException.ScoreException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service("SCORE")
public class ScoreService implements MessageService<ScoreService.ScoreParam> {
    private static final Logger log = LoggerFactory.getLogger(ScoreService.class);
    OsuUserApiService userApiService;
    OsuScoreApiService scoreApiService;
    OsuBeatmapApiService beatmapApiService;
    BindDao bindDao;
    RestTemplate template;
    ImageService imageService;

    @Autowired
    public ScoreService(OsuUserApiService userApiService,
                        OsuScoreApiService scoreApiService,
                        OsuBeatmapApiService beatmapApiService,
                        BindDao bindDao,
                        RestTemplate template,
                        ImageService image) {
        this.userApiService = userApiService;
        this.scoreApiService = scoreApiService;
        this.beatmapApiService = beatmapApiService;
        this.bindDao = bindDao;
        this.template = template;
        imageService = image;
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<ScoreParam> data) throws ScoreException {
        var matcher = Instructions.SCORE.matcher(messageText);
        if (! matcher.find()) {
            return false;
        }
        BinUser binUser;
        OsuMode mode;
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        boolean isOther = true;
        var name = matcher.group("name");
        if (Objects.nonNull(at)) {
            try {
                binUser = bindDao.getUserFromQQ(at.getTarget());
            } catch (Exception e) {
                throw new ScoreException(ScoreException.Type.SCORE_Player_TokenExpired);
            }
        } else if (StringUtils.hasText(name)) {
            binUser = new BinUser();
            Long id;
            try {
                id = userApiService.getOsuId(name.trim());
                binUser.setOsuID(id);
            } catch (WebClientResponseException.NotFound e) {
                throw new ScoreException(ScoreException.Type.SCORE_Player_NotFound);
            }
        } else {
            try {
                binUser = bindDao.getUserFromQQ(event.getSender().getId());
                isOther = false;
            } catch (BindException e) {
                //退避 !score
                if (messageText.toLowerCase().contains("score")) {
                    throw new LogException("score 退避成功");
                } else {
                    throw new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired);
                }
            }
        }

        mode = OsuMode.getMode(matcher.group("mode"));
        boolean isDefault = OsuMode.DEFAULT.equals(mode);
        if (isDefault && Objects.nonNull(binUser.getMode())) {
            mode = binUser.getMode();
        }
        var bid = Long.parseLong(matcher.group("bid"));
        var modsStr = matcher.group("mod");
        var result = new ScoreParam(binUser, mode, bid, modsStr, isDefault, isOther);
        data.setValue(result);
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
        List<Mod> mods = null;
        if (Objects.nonNull(modsStr)) {
            mods = Mod.getModsList(modsStr);
        }

        Score score = null;
        if (!CollectionUtils.isEmpty(mods)) {
            List<Score> scoreall;
            try {
                scoreall = scoreApiService.getScoreAll(bid, binUser, mode);
            } catch (WebClientResponseException.NotFound e) {
                throw new ScoreException(ScoreException.Type.SCORE_Player_NotFound);
            } catch (WebClientResponseException.Unauthorized e) {
                throw new ScoreException(ScoreException.Type.SCORE_Player_NoScore);
            }

            for (var s : scoreall) {
                if (Objects.isNull(s.getMods())) {
                    continue;
                }
                if (CollectionUtils.isEmpty(s.getMods()) && mods.size() == 1 && mods.getFirst() == Mod.None) {
                    score = s;
                    break;
                }
                if (mods.size() != s.getMods().size()) {
                    continue;
                }
                if (new HashSet<>(s.getMods()).containsAll(mods.stream().map(Mod::getAbbreviation).toList())) {
                    score = s;
                    break;
                }
            }
            if (score == null) {
                throw new ScoreException(ScoreException.Type.SCORE_Mod_NotFound);
            } else {
                var beatMap = new BeatMap();
                beatMap.setId(bid);
                score.setBeatMap(beatMap);
            }
        } else {
            try {
                score = scoreApiService.getScore(bid, binUser, mode).getScore();
            } catch (WebClientResponseException.NotFound e) {
                //当在玩家设定的模式上找不到时，寻找基于谱面获取的游戏模式的成绩
                if (isDefault) {
                    score = getDefaultScore(bid, binUser);
                } else {
                    throw new ScoreException(ScoreException.Type.SCORE_Mode_SpecifiedNotFound);
                }
            } catch (WebClientResponseException.Unauthorized e) {
                if (param.isOther()) {
                    throw new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired);
                } else {
                    throw new ScoreException(ScoreException.Type.SCORE_Player_TokenExpired);
                }
            }
        }

        //这里的mode必须用谱面传过来的
        var userInfo = userApiService.getPlayerInfo(binUser, score.getMode());

        try {
            var data = imageService.getPanelE(userInfo, score, beatmapApiService);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            log.error("SCORE：渲染和发送失败", e);
            throw new ScoreException(ScoreException.Type.SCORE_Send_Error);
        }
    }

    public record ScoreParam(BinUser user, OsuMode mode, long bid, String modsStr, boolean isDefault, boolean isOther) {
    }

    private Score getDefaultScore(long bid, BinUser binUser) throws ScoreException {
        try {
            return scoreApiService.getScore(bid, binUser, OsuMode.DEFAULT).getScore();
        } catch (WebClientResponseException e) {
            throw new ScoreException(ScoreException.Type.SCORE_Mode_NotFound);
        }
    }
}
