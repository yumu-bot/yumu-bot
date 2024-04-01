package com.now.nowbot.service.MessageServiceImpl;

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
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Objects;

@Service("INFO_CARD")
public class InfoCardService implements MessageService<InfoService.InfoParam> {

    @Resource
    OsuUserApiService userApiService;
    @Resource
    ImageService imageService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<InfoService.InfoParam> data) throws Throwable {
        var matcher2 = Instructions.DEPRECATED_YMY.matcher(messageText);
        if (matcher2.find()) throw new MiniCardException(MiniCardException.Type.MINI_Deprecated_Y);

        var matcher = Instructions.INFO_CARD.matcher(messageText);
        if (! matcher.find()) return false;

        OsuMode mode = OsuMode.getMode(matcher.group("mode"));
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        var qq = matcher.group("qq");

        if (Objects.nonNull(at)) {
            data.setValue(new InfoService.InfoParam(
                    new BinUser(at.getTarget(), messageText.toLowerCase()),
                    mode, false));
            return true;
        }
        if (Objects.nonNull(qq)) {
            data.setValue(new InfoService.InfoParam(
                    new BinUser(Long.parseLong(qq), messageText.toLowerCase()),
                    mode, false));
            return true;
        }

        String name = matcher.group("name");
        if (Strings.isNotBlank(name)) {
            var user = new BinUser();
            long id;

            try {
                id = userApiService.getOsuId(name.trim());
            } catch (WebClientResponseException.NotFound e) {
                throw new MiniCardException(MiniCardException.Type.MINI_Player_NotFound, name.trim());
            }
            user.setOsuID(id);
            user.setMode(mode);
            data.setValue(new InfoService.InfoParam(user, mode, false));
            return true;
        } else {
            data.setValue(new InfoService.InfoParam(
                    new BinUser(event.getSender().getId(), messageText.toLowerCase()),
                    mode, true));
            return true;
        }
    }

    @Override
    public void HandleMessage(MessageEvent event, InfoService.InfoParam param) throws Throwable {
        var from = event.getSubject();

        OsuUser osuUser;
        try {
            osuUser = userApiService.getPlayerInfo(param.user(), param.mode());
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
