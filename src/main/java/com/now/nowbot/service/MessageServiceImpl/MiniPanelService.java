package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.ServiceException.MiniPanelException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;

@Service("MINI")
public class MiniPanelService implements MessageService<Matcher> {
    OsuUserApiService userApiService;
    OsuScoreApiService scoreApiService;
    OsuBeatmapApiService beatmapApiService;
    BindDao bindDao;
    RestTemplate template;
    ImageService imageService;

    @Autowired
    public MiniPanelService(OsuUserApiService userApiService,
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
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instructions.MINI.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        boolean isScore = (matcher.group("ymx") != null);
        boolean isInfo = (matcher.group("ymy") != null);

        BinUser user;
        long qqId;

        try {
            qqId = event.getSender().getId();
            user = bindDao.getUserFromQQ(qqId);
        } catch (BindException e) {
            throw new MiniPanelException(MiniPanelException.Type.MINI_Me_TokenExpired);
        }

        var mode = user.getMode();
        var id = user.getOsuID();

        if (isScore) {
            Score score;
            BeatMap map;


            try {
                score = scoreApiService.getRecentIncludingFail(id, mode, 0, 1).getFirst();
            } catch (Exception e) {
                throw new MiniPanelException(MiniPanelException.Type.MINI_Recent_NotFound);
            }


            try {
                map = beatmapApiService.getBeatMapInfo(score.getBeatMap().getId());
                score.setBeatMap(map);
                score.setBeatMapSet(map.getBeatMapSet());
            } catch (Exception e) {
                throw new MiniPanelException(MiniPanelException.Type.MINI_Fetch_Error);
            }

            try {
                var data = imageService.getPanelGamma(score);
                QQMsgUtil.sendImage(from, data);
            } catch (Exception e) {
                throw new MiniPanelException(MiniPanelException.Type.MINI_Send_Error);
            }
        } else if (isInfo) {
            OsuUser osuUser;
            try {
                osuUser = userApiService.getPlayerInfo(id, mode);
            } catch (Exception e) {
                throw new MiniPanelException(MiniPanelException.Type.MINI_Me_NotFound);
            }

            try {
                var data = imageService.getPanelGamma(osuUser);
                QQMsgUtil.sendImage(from, data);
            } catch (Exception e) {
                throw new MiniPanelException(MiniPanelException.Type.MINI_Send_Error);
            }

        } else {
            throw new MiniPanelException(MiniPanelException.Type.MINI_Classification_Error);
        }
    }
}
