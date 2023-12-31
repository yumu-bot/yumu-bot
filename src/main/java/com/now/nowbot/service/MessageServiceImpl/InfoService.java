package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.dao.OsuUserInfoDao;
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
import com.now.nowbot.throwable.LogException;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.ServiceException.InfoException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service("INFO")
public class InfoService implements MessageService<InfoService.InfoParam> {
    private static final Logger log = LoggerFactory.getLogger(InfoService.class);
    @Resource
    OsuUserApiService userApiService;
    @Resource
    OsuScoreApiService scoreApiService;
    @Resource
    BindDao           bindDao;
    @Resource
    OsuUserInfoDao    infoDao;
    @Resource
    ImageService      imageService;

    @Override
    public boolean isHandle(MessageEvent event, DataValue<InfoParam> data) throws InfoException {
        var matcher = Instructions.INFO.matcher(event.getRawMessage().trim());
        if (! matcher.find()) return false;

        OsuMode mode = OsuMode.getMode(matcher.group("mode"));
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        var qq = matcher.group("qq");

        if (Objects.nonNull(at)) {
            data.setValue(new InfoParam(
                    getBinUser(at.getTarget(), event.getRawMessage().toLowerCase()),
                    mode));
            return true;
        }
        if (Objects.nonNull(qq)) {
            data.setValue(new InfoParam(
                    getBinUser(Long.parseLong(qq), event.getRawMessage().toLowerCase()),
                    mode));
            return true;
        }
        String name = matcher.group("name");
        if (Strings.isNotBlank(name)) {
            long id;
            BinUser user;
            try {
                id = userApiService.getOsuId(name);
            } catch (WebClientResponseException.NotFound e) {
                throw new InfoException(InfoException.Type.INFO_Player_NotFound);
            }
            user = new BinUser();
            user.setOsuID(id);
            user.setMode(OsuMode.DEFAULT);
            data.setValue(new InfoParam(user, mode));
            return true;
        }
        data.setValue(new InfoParam(
                getBinUser(event.getSender().getId(), event.getRawMessage().toLowerCase()),
                mode));
        return true;
    }

    private BinUser getBinUser(long qq, String cmd) throws InfoException {

        try {
            return bindDao.getUserFromQQ(qq);
        } catch (BindException e) {
            if (! cmd.contains("information") && cmd.contains("info")) {
                throw new LogException("info 退避成功");
            } else {
                throw new InfoException(InfoException.Type.INFO_Me_TokenExpired);
            }
        }
    }

    @Override
    public void HandleMessage(MessageEvent event, InfoParam param) throws Throwable {
        var from = event.getSubject();
        BinUser user = param.user;

        //处理默认mode
        var mode = param.mode();
        if (mode == OsuMode.DEFAULT && user != null && user.getMode() != null) mode = user.getMode();

        OsuUser osuUser;
        List<Score> BPs;
        List<Score> recents;

        try {
            osuUser = userApiService.getPlayerInfo(user, mode);
        } catch (WebClientResponseException.NotFound e) {
            throw new InfoException(InfoException.Type.INFO_Me_NotFound);
        } catch (WebClientResponseException.Unauthorized | BindException e) {
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
        Optional<OsuUser> infoOpt;
        recents = scoreApiService.getRecent(user, mode, 0, 3);

        infoOpt = infoDao.getLastFrom(osuUser.getUID(), OsuMode.DEFAULT.equals(mode) ? osuUser.getPlayMode() : mode, LocalDate.now().minusDays(1))
                /*
                .map(arch -> {
                    log.info("arch: {}", JacksonUtil.objectToJsonPretty(arch));
                    return arch;
                })

                 */
                .map(OsuUserInfoDao::fromArchive);
        /*
        log.info("old: {}\n new: {}", JacksonUtil.objectToJsonPretty(osuUser), JacksonUtil.objectToJsonPretty(infoOpt.orElse(null)));

         */

        try {
            var img = imageService.getPanelD(osuUser, infoOpt, BPs, recents, mode);
            QQMsgUtil.sendImage(from, img);
        } catch (Exception e) {
            log.error("Info 发送异常", e);
            throw new InfoException(InfoException.Type.INFO_Send_Error);
        }
    }

    public record InfoParam(BinUser user, OsuMode mode) {
    }
}
