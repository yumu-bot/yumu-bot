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
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.regex.Pattern;

@Service("INFO")
public class InfoService implements MessageService<InfoService.InfoParam> {
    Logger log = LoggerFactory.getLogger(InfoService.class);
    @Resource
    OsuUserApiService userApiService;
    @Resource
    OsuScoreApiService scoreApiService;
    @Resource
    BindDao       bindDao;
    @Resource
    ImageService  imageService;
    public record InfoParam(String name, Long qq, OsuMode mode){}

    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(ym)?(information|info(?![a-zA-Z_])|i(?![a-zA-Z_]))\\s*([:：](?<mode>[\\w\\d]+))?(?![\\w])\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*)?");
    Pattern pattern4QQ = Pattern.compile("^[!！]\\s*(?i)(ym)?(information|info(?![a-zA-Z_])|i(?![a-zA-Z_]))\\s*(qq=)\\s*(?<qq>\\d+)");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<InfoParam> data) {
        var m4qq = pattern4QQ.matcher(event.getRawMessage().trim());
        if (m4qq.find()) {
            data.setValue(new InfoParam(null, Long.parseLong(m4qq.group("qq")), OsuMode.DEFAULT));
            return true;
        }
        var matcher = pattern.matcher(event.getRawMessage().trim());
        if (!matcher.find()) {
            return false;
        }
        OsuMode mode = OsuMode.getMode(matcher.group("mode"));
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);

        if (at != null) {
            data.setValue(new InfoParam(null, at.getTarget(), mode));
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
