package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.ServiceException.InfoException;
import com.now.nowbot.util.Pattern4ServiceImpl;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Objects;

@Service("INFO")
public class InfoService implements MessageService<InfoService.InfoParam> {
    private static final Logger log = LoggerFactory.getLogger(InfoService.class);
    @Resource
    OsuUserApiService userApiService;
    @Resource
    OsuScoreApiService scoreApiService;
    @Resource
    BindDao       bindDao;
    @Resource
    ImageService  imageService;
    public record InfoParam(String name, Long qq, OsuMode mode){}

    @Override
    public boolean isHandle(MessageEvent event, DataValue<InfoParam> data) {
        var matcher = Pattern4ServiceImpl.INFO.matcher(event.getRawMessage().trim());
        if (!matcher.find()) return false;

        OsuMode mode = OsuMode.getMode(matcher.group("mode"));
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        var qq = matcher.group("qq");

        if (Objects.nonNull(at)) {
            data.setValue(new InfoParam(null, at.getTarget(), mode));
            return true;
        }
        if (Objects.nonNull(qq)) {
            data.setValue(new InfoParam(null, Long.parseLong(qq), mode));
            return true;
        }
        String name = matcher.group("name");
        if (Strings.isNotBlank(name)) {
            data.setValue(new InfoParam(name, null, mode));
            return true;
        }
        data.setValue(new InfoParam(null, event.getSender().getId(), mode));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, InfoParam param) throws Throwable {
        var from = event.getSubject();
        BinUser user;
        if (param.name() != null) {
            long id;
            try {
                id = userApiService.getOsuId(param.name().trim());
            } catch (WebClientResponseException.NotFound e) {
                throw new InfoException(InfoException.Type.INFO_Player_NotFound);
            }
            user = new BinUser();
            user.setOsuID(id);
            user.setMode(OsuMode.DEFAULT);
        } else {
            try {
                user = bindDao.getUserFromQQ(param.qq());
            } catch (BindException e) {
                //退避 !info
                if (!event.getRawMessage().toLowerCase().contains("information") && event.getRawMessage().toLowerCase().contains("info")) {
                    log.info("info 退避成功");
                    return;
                } else {
                    throw new InfoException(InfoException.Type.INFO_Me_TokenExpired);
                }
            }
        }

        //处理默认mode
        var mode = param.mode();
        if (mode == OsuMode.DEFAULT && user != null && user.getMode() != null) mode = user.getMode();

        OsuUser osuUser;
        List<Score> BPs;
        List<Score> Recents;

        try {
            osuUser = userApiService.getPlayerInfo(user, mode);
        } catch (WebClientResponseException.NotFound e) {
            throw new InfoException(InfoException.Type.INFO_Me_NotFound);
        } catch (WebClientResponseException.Unauthorized e) {
            throw new InfoException(InfoException.Type.INFO_Me_TokenExpired);
        } catch (Exception e) {
            log.error("Info 异常：获取玩家信息", e);
            throw new InfoException(InfoException.Type.INFO_Player_FetchFailed);
        }

        try {
            BPs = scoreApiService.getBestPerformance(user, mode, 0, 100);
        } catch (WebClientResponseException.NotFound e) {
            throw new InfoException(InfoException.Type.INFO_Player_NoBP);
        } catch (Exception e) {
            log.error("Info 异常：获取玩家 BP", e);
            throw new InfoException(InfoException.Type.INFO_Player_FetchFailed);
        }

        Recents = scoreApiService.getRecent(user, mode, 0, 3);

        try {
            var img = imageService.getPanelD(osuUser, BPs, Recents, mode);
            QQMsgUtil.sendImage(from, img);
        } catch (Exception e) {
            log.error("Info 发送异常", e);
            throw new InfoException(InfoException.Type.INFO_Send_Error);
        }
    }
}
