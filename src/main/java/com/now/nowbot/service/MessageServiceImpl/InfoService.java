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
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.ServiceException.InfoException;
import com.now.nowbot.util.HandleUtil;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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

    public record InfoParam(BinUser user, OsuMode mode, int day, boolean isMyself) {
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<InfoParam> data) throws InfoException {
        var matcher = Instructions.INFO.matcher(messageText);
        if (! matcher.find()) return false;

        OsuMode mode = OsuMode.getMode(matcher.group("mode"));
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        var qqStr = matcher.group("qq");
        var name = matcher.group("name");
        var dayStr = matcher.group("day");
        boolean isMyself = false;

        BinUser user;

        try {
            if (Objects.nonNull(at)) {
                user = bindDao.getUserFromQQ(at.getTarget());

            } else if (Objects.nonNull(qqStr)) {
                user = bindDao.getUserFromQQ(Long.parseLong(qqStr));

            } else if (StringUtils.hasText(name)) {
                long id;

                try {
                    id = userApiService.getOsuId(name);
                } catch (WebClientResponseException.NotFound e) {
                    throw new InfoException(InfoException.Type.I_Player_NotFound);
                }
                user = new BinUser();
                user.setOsuID(id);
                user.setOsuMode(mode);

            } else {
                user = bindDao.getUserFromQQ(event.getSender().getId());
                isMyself = true;
            }
        } catch (BindException e) {
            if (! messageText.contains("information") && messageText.contains("info")) {
                log.info("info 退避成功");
                return false;
            } else {
                log.error("玩家信息：获取绑定信息失败", e);
                throw new InfoException(InfoException.Type.I_Player_NotFound);
            }
        }

        // 处理回溯天数，默认比对昨天的
        int day;
        if (StringUtils.hasText(dayStr)) {
            try {
                day = Integer.parseInt(dayStr);
            } catch (NumberFormatException e) {
                day = 1;
            }
        } else {
            day = 1;
        }

        data.setValue(new InfoParam(user, HandleUtil.getModeOrElse(mode, user), day, isMyself));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, InfoParam param) throws Throwable {
        var from = event.getSubject();

        var user = param.user;
        var mode = param.mode;

        OsuUser osuUser;
        List<Score> BPs;
        //List<Score> recents;

        try {
            osuUser = userApiService.getPlayerInfo(user, mode);
        } catch (WebClientResponseException.NotFound e) {
            if (param.isMyself) {
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

        Optional<OsuUser> historyUser =
                infoDao.getLastFrom(osuUser.getUID(),
                                OsuMode.DEFAULT.equals(mode) ? osuUser.getOsuMode() : mode,
                                LocalDate.now().minusDays(param.day))
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
            image = imageService.getPanelD(osuUser, historyUser, param.day, BPs, mode);
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
