package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.Service.UserParam;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BPAnalysisException;
import com.now.nowbot.util.QQMsgUtil;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Service("BPANALYSIS")
public class BPAnalysisService implements MessageService<UserParam> {
    OsuUserApiService userApiService;
    OsuScoreApiService scoreApiService;
    BindDao bindDao;
    ImageService imageService;

    @Autowired
    public BPAnalysisService(OsuUserApiService userApiService, OsuScoreApiService scoreApiService, BindDao bindDao, ImageService imageService) {
        this.userApiService = userApiService;
        this.scoreApiService = scoreApiService;
        this.bindDao = bindDao;
        this.imageService = imageService;
    }

    static final Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(ym)?((bpanalysis)|(blue\\s*archive)|bpa(?![a-zA-Z_])|ba(?![a-zA-Z_]))+(\\s*[:：](?<mode>\\w+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<UserParam> data) {
        var matcher = pattern.matcher(event.getRawMessage().trim());
        if (!matcher.find()) return false;
        var mode = OsuMode.getMode(matcher.group("mode"));
        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        if (Objects.nonNull(at)) {
            data.setValue(new UserParam(at.getTarget(), null, mode, true));
            return true;
        }
        String name = matcher.group("name");
        if (Objects.nonNull(name) && Strings.isNotBlank(name)) {
            data.setValue(new UserParam(null, name, mode, false));
            return true;
        }
        data.setValue(new UserParam(event.getSender().getId(), null, mode, false));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, UserParam param) throws Throwable {
        var from = event.getSubject();
        var mode = param.mode();

        //bp列表
        List<Score> bps;
        OsuUser osuUser;
        if (Objects.nonNull(param.qq())) {
            BinUser binUser = bindDao.getUserFromQQ(param.qq());
            try {
                if (mode != OsuMode.DEFAULT) {
                    osuUser = userApiService.getPlayerInfo(binUser, mode);
                    osuUser.setPlayMode(mode.getName());
                    bps = scoreApiService.getBestPerformance(binUser, mode, 0, 100);
                } else {
                    bps = scoreApiService.getBestPerformance(binUser, binUser.getMode(), 0, 100);
                    osuUser = userApiService.getPlayerInfo(binUser, binUser.getMode());
                }
            } catch (Exception e) {
                if (!param.at()) {
                    throw new BPAnalysisException(BPAnalysisException.Type.BPA_Me_FetchFailed);
                } else {
                    throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_FetchFailed);
                }
            }
        } else {
            String name = param.name().trim();
            long id;
            try {
                id = userApiService.getOsuId(name);
            } catch (Exception e) {
                throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_NotFound);
            }
            try {
                if (mode != OsuMode.DEFAULT) {
                    bps = scoreApiService.getBestPerformance(id, mode, 0, 100);
                    osuUser = userApiService.getPlayerInfo(id, mode);
                    osuUser.setPlayMode(mode.getName());
                } else {
                    osuUser = userApiService.getPlayerInfo(id);
                    bps = scoreApiService.getBestPerformance(id, osuUser.getPlayMode(), 0, 100);
                }
            } catch (Exception e) {
                throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_FetchFailed);
            }
        }

        if (bps == null || bps.size() <= 5) {
            if (param.qq() == event.getSender().getId()) {
                throw new BPAnalysisException(BPAnalysisException.Type.BPA_Me_NotEnoughBP);
            } else {
                throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_NotEnoughBP);
            }
        }

        try {
            var data = imageService.getPanelJ(osuUser, bps, userApiService);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            NowbotApplication.log.error("BPA Error: ", e);
            throw new BPAnalysisException(BPAnalysisException.Type.BPA_Send_Error);
        }
    }
}
