package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.Service.UserParam;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.OldAvatarException;
import com.now.nowbot.util.Pattern4ServiceImpl;
import com.now.nowbot.util.QQMsgUtil;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Objects;

@Service("OLDAVATAR")
public class OldAvatarService implements MessageService<UserParam> {
    private static final Logger log = LoggerFactory.getLogger(OldAvatarService.class);
    OsuUserApiService userApiService;
    BindDao bindDao;
    ImageService imageService;
    @Autowired
    public OldAvatarService(OsuUserApiService userApiService, BindDao bindDao, ImageService imageService) {
        this.userApiService = userApiService;
        this.bindDao = bindDao;
        this.imageService = imageService;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<UserParam> data) {
        var matcher = Pattern4ServiceImpl.OLDAVATAR.matcher(event.getRawMessage().trim());
        if (!matcher.find()) return false;

        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        if (Objects.nonNull(at)) {
            data.setValue(new UserParam(at.getTarget(), null, null, true));
            return true;
        }

        String qq = matcher.group("qq");
        if (Objects.nonNull(qq) && Strings.isNotBlank(qq)) {
            data.setValue(new UserParam(Long.parseLong(qq), null, null, false));
            return true;
        }

        String name = matcher.group("name");
        if (Objects.nonNull(name) && Strings.isNotBlank(name)) {
            data.setValue(new UserParam(null, name, null, false));
            return true;
        }

        data.setValue(new UserParam(event.getSender().getId(), null, null, false));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, UserParam param) throws Throwable {
        var from = event.getSubject();
        OsuUser osuUser;

        if (Objects.nonNull(param.qq())) {
            BinUser binUser;
            try {
                binUser = bindDao.getUserFromQQ(param.qq());
            } catch (Exception e) {
                if (event.getSender().getId() == param.qq()) {
                    throw new OldAvatarException(OldAvatarException.Type.OA_Me_NotBind);
                } else {
                    throw new OldAvatarException(OldAvatarException.Type.OA_Player_TokenExpired);
                }
            }
            try {
                osuUser = userApiService.getPlayerInfo(binUser);
            } catch (WebClientResponseException e) {
                if (param.at()) {
                    throw new OldAvatarException(OldAvatarException.Type.OA_Player_FetchFailed);
                } else {
                    throw new OldAvatarException(OldAvatarException.Type.OA_Me_FetchFailed);
                }
            } catch (Exception e) {
                log.error("OA 获取玩家信息失败: ", e);
                throw new OldAvatarException(OldAvatarException.Type.OA_Me_FetchFailed);
            }
        } else {
            String name = param.name().trim();
            long id;

            try {
                id = userApiService.getOsuId(name);
            } catch (Exception ignored) {
                try {
                    id = Long.parseLong(name);
                } catch (NumberFormatException e) {
                    throw new OldAvatarException(OldAvatarException.Type.OA_Parameter_Error);
                }
            }

            try {
                osuUser = userApiService.getPlayerInfo(id);
            } catch (Exception e) {
                throw new OldAvatarException(OldAvatarException.Type.OA_Player_NotFound);
            }
        }

        try {
            var data = imageService.getPanelEpsilon(osuUser.getUsername(), osuUser.getUID());
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            NowbotApplication.log.error("OA 发送失败: ", e);
            throw new OldAvatarException(OldAvatarException.Type.OA_Send_Error);
        }
    }
}
