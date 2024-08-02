package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.OldAvatarException;
import com.now.nowbot.util.Instruction;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Objects;

import static com.now.nowbot.util.command.CmdPatternStaticKt.*;

@Service("OLD_AVATAR")
public class OldAvatarService implements MessageService<OldAvatarService.OAParam> {
    private static final Logger log = LoggerFactory.getLogger(OldAvatarService.class);
    @Resource
    OsuUserApiService userApiService;
    @Resource
    BindDao bindDao;
    @Resource
    ImageService imageService;

    public record OAParam(Long qq, Long uid, String name, OsuMode mode, boolean at, boolean isMyself) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<OAParam> data) {
        var matcher = Instruction.OLD_AVATAR.matcher(messageText);
        if (!matcher.find()) return false;

        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        var qqStr = matcher.group(FLAG_QQ_ID);
        var uidStr = matcher.group(FLAG_UID);
        var name = matcher.group(FLAG_NAME);

        if (Objects.nonNull(at)) {
            data.setValue(new OAParam(at.getTarget(), null, null, null, true, false));
            return true;
        } else if (StringUtils.hasText(qqStr)) {
            data.setValue(new OAParam(Long.parseLong(qqStr), null, null, null, false, false));
            return true;
        }

        if (StringUtils.hasText(uidStr)) {
            data.setValue(new OAParam(null, Long.parseLong(uidStr), null, null, false, false));
            return true;
        } else if (StringUtils.hasText(name)) {
            data.setValue(new OAParam(null, null, name.trim(), null, false, false));
            return true;
        } else {
            data.setValue(new OAParam(event.getSender().getId(), null, null, null, false, true));
            return true;
        }
    }

    @Override
    public void HandleMessage(MessageEvent event, OAParam param) throws Throwable {
        var from = event.getSubject();
        OsuUser osuUser = null;

        if (Objects.nonNull(param.uid)) {
            try {
                osuUser = userApiService.getPlayerInfo(param.uid);
            } catch (Exception e) {
                throw new OldAvatarException(OldAvatarException.Type.OA_Player_NotFound);
            }

        } else if (Objects.nonNull(param.qq)) {
            BinUser binUser;
            try {
                binUser = bindDao.getUserFromQQ(param.qq);
            } catch (Exception e) {
                if (param.isMyself) {
                    throw new OldAvatarException(OldAvatarException.Type.OA_Me_NotBind);
                } else {
                    throw new OldAvatarException(OldAvatarException.Type.OA_Player_TokenExpired);
                }
            }

            try {
                osuUser = userApiService.getPlayerInfo(binUser);
            } catch (WebClientResponseException e) {
                throw new OldAvatarException(OldAvatarException.Type.OA_Player_FetchFailed);
            } catch (Exception e) {
                log.error("旧头像：获取玩家信息失败: ", e);
                throw new OldAvatarException(OldAvatarException.Type.OA_Player_FetchFailed);
            }
        } else {
            String name = param.name;
            long id;

            try {
                id = userApiService.getOsuId(name);
            } catch (Exception e) {
                try {
                    id = Long.parseLong(name);
                } catch (NumberFormatException e1) {
                    throw new OldAvatarException(OldAvatarException.Type.OA_Parameter_Error);
                }
            }

            try {
                if (userApiService != null) {
                    osuUser = userApiService.getPlayerInfo(id);
                }
            } catch (Exception e) {
                throw new OldAvatarException(OldAvatarException.Type.OA_Player_NotFound);
            }
        }

        try {
            var image = imageService.getPanelEpsilon(osuUser);
            from.sendImage(image);
        } catch (Exception e) {
            log.error("旧头像：发送失败", e);
            throw new OldAvatarException(OldAvatarException.Type.OA_Send_Error);
        }
    }
}
