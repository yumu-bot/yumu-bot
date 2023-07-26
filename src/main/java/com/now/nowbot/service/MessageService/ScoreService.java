package com.now.nowbot.service.MessageService;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.ScoreException;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;

@Service("score")
public class ScoreService implements MessageService {
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

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        BinUser user = null;

        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        if (at != null) {
            try {
                user = bindDao.getUser(at.getTarget());
            } catch (Exception e) {
                throw new ScoreException(ScoreException.Type.SCORE_Player_NoBind);
            }
        } else {
            user = bindDao.getUser(event.getSender().getId());
        }

        var mode = OsuMode.getMode(matcher.group("mode"));
        boolean isDefault = (mode == OsuMode.DEFAULT && user != null && user.getMode() != null);

        var bid = Long.parseLong(matcher.group("bid"));

        // 处理 mods
        var modsStr = matcher.group("mod");
        List<Mod> mods = null;
        if (modsStr != null) {
            mods = Mod.getModsList(matcher.group("mod"));
        }

        Score score = null;
        if (mods != null && mods.size() > 0) {
            var scoreall = osuGetService.getScoreAll(bid, user, isDefault ? user.getMode() : mode);
            for (var s : scoreall) {
                if (s.getMods().size() == 0 && mods.size() == 1 && mods.get(0) == Mod.None) {
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
                var bm = new BeatMap();
                bm.setId(bid);
                score.setBeatMap(bm);
            }
        } else {
            ScoreException.Type error = null;
            try {
                score = osuGetService.getScore(bid, user, isDefault ? user.getMode() : mode).getScore();
            } catch (Exception e) {
                //当在玩家设定的模式上找不到时，寻找基于谱面获取的游戏模式的成绩
                if (isDefault) {
                    try {
                        score = osuGetService.getScore(bid, user, OsuMode.DEFAULT).getScore();
                    } catch (Exception e2) {
                        error = ScoreException.Type.SCORE_Mode_MainNotFound;
                    }
                } else {
                    error = ScoreException.Type.SCORE_Mode_NotFound;
                }
            }
            if (error != null) throw new ScoreException(error);
        }

        //这里的mode必须用谱面传过来的
        var userInfo = osuGetService.getPlayerInfo(user, score.getMode());

        try {
            var data = imageService.getPanelE(userInfo, score, osuGetService);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            NowbotApplication.log.error("err", e);
            throw new ScoreException(ScoreException.Type.SCORE_Send_Error);
            //from.sendMessage("出错了出错了,问问管理员");
        }
    }
}
