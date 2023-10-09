package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.Service.UserParm;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.BPAnalysisException;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.QQMsgUtil;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Service("BPA")
public class BPAnalysisService implements MessageService<UserParm> {
    OsuGetService osuGetService;
    BindDao bindDao;
    ImageService imageService;



    @Autowired
    public BPAnalysisService(OsuGetService osuGetService, BindDao bindDao, ImageService imageService) {
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
        this.imageService = imageService;
    }

    static final Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(ym)?((bpanalysis)|(blue\\s*archive)|bpa(?![a-zA-Z_])|ba(?![a-zA-Z_]))+(\\s*[:：](?<mode>\\w+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<UserParm> data) {
        var matcher = pattern.matcher(event.getRawMessage().trim());
        if (!matcher.find()) return false;
        var mode = OsuMode.getMode(matcher.group("mode"));
        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        if (Objects.nonNull(at)) {
            data.setValue(new UserParm(at.getTarget(), null, mode, true));
            return true;
        }
        String name = matcher.group("name");
        if (Objects.nonNull(name) && Strings.isNotBlank(name)) {
            data.setValue(new UserParm(null, name, mode, false));
            return true;
        }
        data.setValue(new UserParm(event.getSender().getId(), null, mode, false));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, UserParm parm) throws Throwable {
        var from = event.getSubject();
        var mode = parm.mode();

        //bp列表
        List<Score> bps;
        OsuUser osuUser;
        if (Objects.nonNull(parm.qq())) {
            BinUser binUser = bindDao.getUser(parm.qq());
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
                BPAnalysisException.Type etype;
                if (parm.at()) {
                    etype = BPAnalysisException.Type.BPA_Player_FetchFailed;
                } else {
                    etype = BPAnalysisException.Type.BPA_Me_FetchFailed;
                }
                throw new BPAnalysisException(etype);
            }
        } else {
            String name = parm.name().trim();
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
        }
    }
}
