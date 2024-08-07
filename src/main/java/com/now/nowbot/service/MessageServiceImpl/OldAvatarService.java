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
import com.now.nowbot.throwable.GeneralTipsException;
import com.now.nowbot.util.Instruction;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
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
        var name = matcher.group(FLAG_DATA);

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
        OsuUser user;

        if (Objects.nonNull(param.uid)) {
            try {
                user = userApiService.getPlayerInfo(param.uid);
            } catch (Exception e) {
                throw new GeneralTipsException(GeneralTipsException.Type.G_Null_Player, param.uid);
            }

        } else if (Objects.nonNull(param.qq)) {
            BinUser binUser;
            try {
                binUser = bindDao.getUserFromQQ(param.qq);
            } catch (Exception e) {
                if (param.isMyself) {
                    throw new GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Me);
                } else {
                    throw new GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Player);
                }
            }

            try {
                user = userApiService.getPlayerInfo(binUser);
            } catch (WebClientResponseException e) {
                throw new GeneralTipsException(GeneralTipsException.Type.G_Null_Player, binUser.getOsuName());
            } catch (Exception e) {
                log.error("旧头像：获取玩家信息失败: ", e);
                throw new GeneralTipsException(GeneralTipsException.Type.G_Fetch_PlayerInfo);
            }
        } else {
            List<OsuUser> users = parseDataString(param.name);

            if (CollectionUtils.isEmpty(users)) throw new GeneralTipsException(GeneralTipsException.Type.G_Fetch_List);

            var images = new ArrayList<byte[]>(users.size());

            try {
                for (var u : users) {
                    images.add(imageService.getPanelEpsilon(u));
                }

                QQMsgUtil.sendImages(event, images);
                return;
            } catch (Exception e) {
                log.error("旧头像：发送失败", e);
                throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "官网头像");
            }
        }

        try {
            var image = imageService.getPanelEpsilon(user);
            from.sendImage(image);
        } catch (Exception e) {
            log.error("旧头像：发送失败", e);
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "官网头像");
        }
    }

    private List<OsuUser> parseDataString(String dataStr) throws GeneralTipsException {
        String[] dataStrArray = dataStr.trim().split("[,，|:：]+");
        if (dataStr.isBlank() || dataStrArray.length == 0) return null;

        var ids = new ArrayList<Long>();
        var users = new ArrayList<OsuUser>();

        for (String s: dataStrArray) {
            if (! StringUtils.hasText(s)) continue;

            try {
                ids.add(userApiService.getOsuId(s.trim()));
            } catch (WebClientResponseException e) {
                try {
                    ids.add(Long.parseLong(s.trim()));
                } catch (NumberFormatException e1) {
                    throw new GeneralTipsException(GeneralTipsException.Type.G_Null_UserName);
                }
            }
        }

        for (var id : ids) {
            try {
                users.add(userApiService.getPlayerInfo(id));
            } catch (WebClientResponseException e) {
                try {
                    users.add(userApiService.getPlayerInfo(id.toString()));
                } catch (WebClientResponseException e1) {
                    throw new GeneralTipsException(GeneralTipsException.Type.G_Null_Player, id);
                }
            }
        }

        return users;
    }
}
