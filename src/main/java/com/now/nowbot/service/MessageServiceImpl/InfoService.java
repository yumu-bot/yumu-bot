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

    public record InfoParam(BinUser user, OsuMode mode, boolean isMyself) {
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<InfoParam> data) throws InfoException {
        var matcher = Instructions.INFO.matcher(messageText);
        if (! matcher.find()) return false;

        OsuMode mode = OsuMode.getMode(matcher.group("mode"));
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        var qq = matcher.group("qq");

        if (Objects.nonNull(at)) {
            data.setValue(new InfoParam(
                    getBinUser(at.getTarget(), messageText.toLowerCase()),
                    mode, false));
            return true;
        }
        if (Objects.nonNull(qq)) {
            data.setValue(new InfoParam(
                    getBinUser(Long.parseLong(qq), messageText.toLowerCase()),
                    mode, false));
            return true;
        }

        String name = matcher.group("name");
        if (Strings.isNotBlank(name)) {
            var user = new BinUser();
            long id;

            try {
                id = userApiService.getOsuId(name);
            } catch (WebClientResponseException.NotFound e) {
                throw new InfoException(InfoException.Type.I_Player_NotFound);
            }
            user.setOsuID(id);
            user.setMode(mode);
            data.setValue(new InfoParam(user, mode, false));
            return true;
        } else {
            data.setValue(new InfoParam(
                    getBinUser(event.getSender().getId(), messageText.toLowerCase()),
                    mode, true));
            return true;
        }
    }

    private BinUser getBinUser(long qq, String messageText) throws InfoException {

        try {
            return bindDao.getUserFromQQ(qq);
        } catch (BindException e) {
            if (! messageText.contains("information") && messageText.contains("info")) {
                throw new LogException("info 退避成功");
            } else {
                throw new InfoException(InfoException.Type.I_Me_TokenExpired);
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
        //List<Score> recents;

        try {
            osuUser = userApiService.getPlayerInfo(user, mode);
        } catch (WebClientResponseException.NotFound e) {
            if (param.isMyself()) {
                throw new InfoException(InfoException.Type.I_Me_NotFound);
            } else {
                throw new InfoException(InfoException.Type.I_Player_NotFound);
            }
        } catch (WebClientResponseException.Unauthorized | BindException e) {
            throw new InfoException(InfoException.Type.I_Me_TokenExpired);
        } catch (WebClientResponseException.BadGateway | WebClientResponseException.ServiceUnavailable e) {
            log.error("玩家信息：连接失败", e);
            throw new InfoException(InfoException.Type.I_API_Unavailable);
        } catch (Exception e) {
            log.error("玩家信息：其他错误", e);
            throw new InfoException(InfoException.Type.I_Player_FetchFailed);
        }

        try {
            BPs = scoreApiService.getBestPerformance(user, mode, 0, 100);
        } catch (WebClientResponseException.NotFound e) {
            throw new InfoException(InfoException.Type.I_Player_NoBP, param.mode());
        } catch (Exception e) {
            log.error("玩家信息：无法获取玩家 BP", e);
            throw new InfoException(InfoException.Type.I_BP_FetchFailed);
        }

        //recents = scoreApiService.getRecent(user, mode, 0, 3);

        Optional<OsuUser> infoOpt;

        infoOpt = infoDao.getLastFrom(osuUser.getUID(), OsuMode.DEFAULT.equals(mode) ? osuUser.getOsuMode() : mode, LocalDate.now().minusDays(1))
                /*
                .map(arch -> {
                    if (osuUser.getUID().equals(17064371L))
                        log.info("arch: {}", JacksonUtil.objectToJsonPretty(arch));
                    return arch;
                })
                */
                .map(OsuUserInfoDao::fromArchive);
        /*
        log.info("old: {}\nJson: {}", infoOpt.map(OsuUser::toString).orElse(""), JacksonUtil.objectToJsonPretty(infoOpt.orElse(null)));
         */
        byte[] image;

        try {
            //image = imageService.getPanelD(osuUser, infoOpt, BPs, recents, mode);
            image = imageService.getPanelD(osuUser, infoOpt, BPs, mode);
        } catch (Exception e) {
            log.error("玩家信息：图片渲染失败", e);
            throw new InfoException(InfoException.Type.I_Fetch_Error);
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("玩家信息：发送失败", e);
            throw new InfoException(InfoException.Type.I_Send_Error);
        }
    }
}
