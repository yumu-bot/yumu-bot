package com.now.nowbot.service.MessageService;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.BPAnalysisException;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;

@Service("BPA")
public class BPAnalysisService implements MessageService {
    OsuGetService osuGetService;
    BindDao bindDao;
    ImageService imageService;

    @Autowired
    public BPAnalysisService(OsuGetService osuGetService, BindDao bindDao, ImageService imageService) {
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
        this.imageService = imageService;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        var mode = OsuMode.getMode(matcher.group("mode"));
        //bp列表
        List<Score> bps;
        OsuUser user;
        var name = matcher.group("name");
        if (name != null && !name.trim().equals("")) {
            //查询其他人 bpht [name]
            name = name.trim();
            long id;
            try {
                id = osuGetService.getOsuId(name);
            } catch (Exception e) {
                throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_NotFound);
            }
            if (mode != OsuMode.DEFAULT) {
                bps = osuGetService.getBestPerformance(id, mode, 0, 100);
                user = osuGetService.getPlayerInfo(id, mode);
                user.setPlayMode(mode.getName());
            } else {
                user = osuGetService.getPlayerInfo(id);
                bps = osuGetService.getBestPerformance(id, user.getPlayMode(), 0, 100);
            }
        } else {
            var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
            BinUser b;
            if (at != null) {
                try {
                    b = bindDao.getUser(at.getTarget());
                } catch (BindException e) {
                    throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_FetchFailed);
                }
            } else {
                b = bindDao.getUser(event.getSender().getId());
            }
            if (mode != OsuMode.DEFAULT) {
                user = osuGetService.getPlayerInfo(b, mode);
                user.setPlayMode(mode.getName());
                bps = osuGetService.getBestPerformance(b, mode, 0, 100);
            } else {
                bps = osuGetService.getBestPerformance(b, b.getMode(), 0, 100);
                user = osuGetService.getPlayerInfo(b);
            }
        }

        try {
            var data = imageService.getPanelJ(user, bps, osuGetService);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            NowbotApplication.log.error("BPA Error: ", e);
            throw new BPAnalysisException(BPAnalysisException.Type.BPA_Send_Error);
            //from.sendMessage("出错了出错了,问问管理员");
        }
    }
}
