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
import com.now.nowbot.throwable.ServiceException.MiniCardException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service("INFO_CARD")
public class InfoCardService implements MessageService<InfoService.InfoParam> {

    @Resource
    OsuUserApiService userApiService;
    @Resource
    ImageService imageService;
    @Resource
    BindDao bindDao;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<InfoService.InfoParam> data) throws Throwable {
        var matcher2 = Instructions.DEPRECATED_YMY.matcher(messageText);
        if (matcher2.find()) throw new MiniCardException(MiniCardException.Type.MINI_Deprecated_Y);

        var matcher = Instructions.INFO_CARD.matcher(messageText);
        if (! matcher.find()) return false;

        OsuMode mode = OsuMode.getMode(matcher.group("mode"));
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        var qq = matcher.group("qq");

        String name = matcher.group("name");

        if (Objects.nonNull(at)) {
            data.setValue(new InfoService.InfoParam(
                    bindDao.getUserFromQQ(at.getTarget()), mode, 1, false
            ));
        } else if (StringUtils.hasText(qq)) {
            data.setValue(new InfoService.InfoParam(
                    bindDao.getUserFromQQ(Long.parseLong(qq)),
                    mode, 1, false
            ));
        } else if (StringUtils.hasText(name)) {
            var user = new BinUser();

            user.setOsuName(name.trim());
            user.setMode(mode);
            data.setValue(new InfoService.InfoParam(
                    user, mode, 1, false
            ));
        } else {
            data.setValue(new InfoService.InfoParam(
                    bindDao.getUserFromQQ(event.getSender().getId()),
                    mode, 1, true));
        }
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, InfoService.InfoParam param) throws Throwable {
        var from = event.getSubject();

        OsuUser osuUser;
        try {
            osuUser = userApiService.getPlayerInfo(param.user().getOsuName(), param.mode());
        } catch (Exception e) {
            throw new MiniCardException(MiniCardException.Type.MINI_Player_NotFound, param.user().getOsuName());
        }

        byte[] image;

        try {
            image = imageService.getPanelGamma(osuUser);
        } catch (Exception e) {
            throw new MiniCardException(MiniCardException.Type.MINI_Render_Error);
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            throw new MiniCardException(MiniCardException.Type.MINI_Send_Error);
        }
    }
}
