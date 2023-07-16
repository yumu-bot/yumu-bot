package com.now.nowbot.service.MessageService;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.BPAException;
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
        BinUser nu = null;
        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        // 是否为绑定用户
        boolean isBind = true;
        if (at != null) {
            nu = bindDao.getUser(at.getTarget());
        }
        if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
            //查询其他人 bpht [name]
            String name = matcher.group("name").trim();
            long id = osuGetService.getOsuId(name);
            try {
                nu = bindDao.getUserFromOsuid(id);
            } catch (BindException e) {
                throw new BPAException(BPAException.Type.BPA_Other_NotFound);
                //do nothing ....?
            }
            if (nu == null) {
                //构建只有 name + id 的对象
                nu = new BinUser();
                nu.setOsuID(id);
                nu.setOsuName(name);
                isBind = false;
            }
        } else {
            //处理没有参数的情况 查询自身
            try {
                nu = bindDao.getUser(event.getSender().getId());
            } catch (BindException e) {
                throw new BPAException(BPAException.Type.BPA_Me_NoBind);
            }
        }
        //bp列表
        List<Score> Bps;
        //分别处理mode
        var mode = OsuMode.getMode(matcher.group("mode"));
        //处理默认mode
        if (mode == OsuMode.DEFAULT && nu.getMode() != null) mode = nu.getMode();
        Bps = osuGetService.getBestPerformance(nu, mode, 0, 100);

        var user = osuGetService.getPlayerInfo(nu, mode);

        try {
            var data = imageService.getPanelJ(user, Bps, osuGetService);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            NowbotApplication.log.error("err", e);
            throw new BPAException(BPAException.Type.BPA_Send_Error);
            //from.sendMessage("出错了出错了,问问管理员");
        }
    }
}
