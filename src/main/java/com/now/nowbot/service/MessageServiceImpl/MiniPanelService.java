package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.ServiceException.MiniPanelException;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("MiniPanel")
public class MiniPanelService implements MessageService<Matcher> {
    OsuGetService osuGetService;
    BindDao bindDao;
    RestTemplate template;
    ImageService imageService;

    @Autowired
    public MiniPanelService(OsuGetService osuGetService, BindDao bindDao, RestTemplate template, ImageService image) {
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
        this.template = template;
        imageService = image;
    }
    Pattern pattern = Pattern.compile("^[!！](?i)\\s*((ym)?)((?<ymx>x(?!\\w))|(?<ymy>y(?!\\w)))+");

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
        boolean isScore = (matcher.group("ymx") != null);
        boolean isInfo = (matcher.group("ymy") != null);

        BinUser user;
        long qqId;

        try {
            qqId = event.getSender().getId();
            user = bindDao.getUser(qqId);
        } catch (BindException e) {
            throw new MiniPanelException(MiniPanelException.Type.MINI_Me_LoseBind);
        }

        var mode = user.getMode();
        var id = user.getOsuID();

        if (isScore) {
            Score score;
            try {
                score = osuGetService.getAllRecentN(id, mode, 0, 1).get(0);
                var map = osuGetService.getBeatMapInfo(score.getBeatMap().getId());
                score.setBeatMap(map);
                score.setBeatMapSet(map.getBeatMapSet());
            } catch (Exception e) {
                throw new MiniPanelException(MiniPanelException.Type.MINI_Recent_NotFound);
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
                osuUser = osuGetService.getPlayerInfo(id, mode);
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
