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
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.ServiceException.ScoreException;
import com.now.nowbot.util.QQMsgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("SCORE")
public class ScoreService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(ScoreService.class);
    OsuGetService osuGetService;
    BindDao bindDao;
    RestTemplate template;
    ImageService imageService;

    @Autowired
    public ScoreService(OsuGetService osuGetService, BindDao bindDao, RestTemplate template, ImageService image) {
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
        this.template = template;
        imageService = image;
    }

    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(?<score>(ym)?(score|s(?![a-zA-Z_])))\\s*([:：](?<mode>[\\w\\d]+))?\\s*(?<bid>\\d+)\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*)?\\s*(\\+(?<mod>( ?[EZNMFHTDRSPCLO]{2})+))?");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = pattern.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        var name = matcher.group("name");
        BinUser binUser;

        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);

        if (at != null) {
            try {
                binUser = bindDao.getUserFromQQ(at.getTarget());
            } catch (Exception e) {
                throw new ScoreException(ScoreException.Type.SCORE_Player_TokenExpired);
            }
        } else if (name != null && !name.trim().isEmpty()) {
            binUser = new BinUser();
            Long id;
            try {
                id = osuGetService.getOsuId(name.trim());
                binUser.setOsuID(id);
            } catch (HttpClientErrorException e) {
                throw new ScoreException(ScoreException.Type.SCORE_Player_NotFound);
            }
        } else {
            try {
                binUser = bindDao.getUserFromQQ(event.getSender().getId());
            } catch (BindException e) {
                //退避 !score
                if (event.getRawMessage().toLowerCase().contains("score")) {
                    log.info("score 退避成功");
                    return;
                } else {
                    throw new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired);
                }
            }
        }

        var mode = OsuMode.getMode(matcher.group("mode"));
        boolean isDefault = (mode == OsuMode.DEFAULT && binUser != null && binUser.getMode() != null);

        var bid = Long.parseLong(matcher.group("bid"));

        // 处理 mods
        var modsStr = matcher.group("mod");
        List<Mod> mods = null;
        if (modsStr != null) {
            mods = Mod.getModsList(matcher.group("mod"));
        }

        Score score = null;
        if (mods != null && !mods.isEmpty()) {
            List<Score> scoreall;
            try {
                scoreall = osuGetService.getScoreAll(bid, binUser, isDefault ? binUser.getMode() : mode);
            } catch (HttpClientErrorException.NotFound e) {
                throw new ScoreException(ScoreException.Type.SCORE_Player_NotFound);
            } catch (HttpClientErrorException.Unauthorized e) {
                throw new ScoreException(ScoreException.Type.SCORE_Player_NoScore);
            }

            for (var s : scoreall) {
                if (s.getMods().isEmpty() && mods.size() == 1 && mods.get(0) == Mod.None) {
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
                beatMap.setBID(bid);
                score.setBeatMap(beatMap);
            }
        } else {
            try {
                score = osuGetService.getScore(bid, binUser, isDefault ? binUser.getMode() : mode).getScore();
            } catch (HttpClientErrorException.NotFound e) {
                //当在玩家设定的模式上找不到时，寻找基于谱面获取的游戏模式的成绩
                if (isDefault) {
                    score = getDefaultScore(bid, binUser);
                } else {
                    throw new ScoreException(ScoreException.Type.SCORE_Mode_SpecifiedNotFound);
                }
            } catch (HttpClientErrorException.Unauthorized e) {
                if (name == null || name.trim().isEmpty() && at == null) {
                    throw new ScoreException(ScoreException.Type.SCORE_Me_TokenExpired);
                } else {
                    throw new ScoreException(ScoreException.Type.SCORE_Player_TokenExpired);
                }
            }
        }

        //这里的mode必须用谱面传过来的
        var userInfo = osuGetService.getPlayerInfo(binUser, score.getMode());

        try {
            var data = imageService.getPanelE(userInfo, score, osuGetService);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            log.error("SCORE：渲染和发送失败", e);
            throw new ScoreException(ScoreException.Type.SCORE_Send_Error);
        }
    }

    private Score getDefaultScore(long bid, BinUser binUser) throws ScoreException {
        try {
            return osuGetService.getScore(bid, binUser, OsuMode.DEFAULT).getScore();
        } catch (HttpClientErrorException e) {
            throw new ScoreException(ScoreException.Type.SCORE_Mode_NotFound);
        }
    }
}
