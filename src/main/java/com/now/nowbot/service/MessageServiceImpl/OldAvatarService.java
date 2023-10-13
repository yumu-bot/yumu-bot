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
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.OldAvatarException;
import com.now.nowbot.util.QQMsgUtil;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.regex.Pattern;

@Service("OLDAVATAR")
public class OldAvatarService implements MessageService<UserParam> {

    static final Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(ymoldavatar|((ym)?oa(?![a-zA-Z_])))\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*)?");

    OsuGetService osuGetService;
    BindDao bindDao;
    ImageService imageService;
    @Autowired
    public OldAvatarService(OsuGetService osuGetService, BindDao bindDao, ImageService imageService) {
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
        this.imageService = imageService;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<UserParam> data) {
        var matcher = pattern.matcher(event.getRawMessage().trim());
        if (!matcher.find()) return false;

        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        if (Objects.nonNull(at)) {
            data.setValue(new UserParam(at.getTarget(), null, null, true));
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
            BinUser binUser = bindDao.getUser(param.qq());
            try {
                osuUser = osuGetService.getPlayerInfo(binUser);
            } catch (Exception e) {
                if (param.at()) {
                    throw new OldAvatarException(OldAvatarException.Type.OA_Player_FetchFailed);
                } else {
                    throw new OldAvatarException(OldAvatarException.Type.OA_Me_FetchFailed);
                }
            }
        } else {
            String name = param.name().trim();
            long id;

            try {
                id = osuGetService.getOsuId(name);
            } catch (Exception ignored) {
                try {
                    id = Long.parseLong(name);
                } catch (NumberFormatException e) {
                    throw new OldAvatarException(OldAvatarException.Type.OA_Parameter_Error);
                }
            }

            try {
                osuUser = osuGetService.getPlayerInfo(id);
            } catch (Exception e) {
                throw new OldAvatarException(OldAvatarException.Type.OA_Player_NotFound);
            }
        }

        try {
            var data = imageService.getPanelEpsilon(osuUser.getUsername(), osuUser.getUID());
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            NowbotApplication.log.error("OA Error: ", e);
            throw new OldAvatarException(OldAvatarException.Type.OA_Send_Error);
        }
    }
}
