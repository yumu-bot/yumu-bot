package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
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
        OsuUser osuUser;
        var name = matcher.group("name");
        if (name != null && !name.trim().isEmpty()) {
            //查询其他人 bpht [name]
            name = name.trim();
            long id;
            try {
                id = osuGetService.getOsuId(name);
            } catch (Exception e) {
                throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_NotFound);
            }
            try {
                if (mode != OsuMode.DEFAULT) {
                    bps = osuGetService.getBestPerformance(id, mode, 0, 100);
                    osuUser = osuGetService.getPlayerInfo(id, mode);
                    osuUser.setPlayMode(mode.getName());
                } else {
                    osuUser = osuGetService.getPlayerInfo(id);
                    bps = osuGetService.getBestPerformance(id, osuUser.getPlayMode(), 0, 100);
                }
            } catch (Exception e) {
                throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_FetchFailed);
            }
        } else {
            var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
            BinUser binUser;

            if (at != null) {
                binUser = bindDao.getUser(at.getTarget());
            } else {
                binUser = bindDao.getUser(event.getSender().getId());
            }

            try {
                if (mode != OsuMode.DEFAULT) {
                    osuUser = osuGetService.getPlayerInfo(binUser, mode);
                    osuUser.setPlayMode(mode.getName());
                    bps = osuGetService.getBestPerformance(binUser, mode, 0, 100);
                } else {
                    bps = osuGetService.getBestPerformance(binUser, binUser.getMode(), 0, 100);
                    osuUser = osuGetService.getPlayerInfo(binUser, binUser.getMode());
                }
            } catch (Exception e) {
                if (at != null) {
                    throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_FetchFailed);
                } else {
                    throw new BPAnalysisException(BPAnalysisException.Type.BPA_Me_FetchFailed);
                }
            }
        }

        if (bps == null || bps.size() <= 5) {
            throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_NotEnoughBP);
        }

        try {
            var data = imageService.getPanelJ(osuUser, bps, osuGetService);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            NowbotApplication.log.error("BPA Error: ", e);
            throw new BPAnalysisException(BPAnalysisException.Type.BPA_Send_Error);
            //from.sendMessage("出错了出错了,问问管理员");
        }
    }
}
