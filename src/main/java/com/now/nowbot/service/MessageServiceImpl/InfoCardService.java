package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.ServiceException.MiniCardException;
import com.now.nowbot.util.CmdUtil;
import com.now.nowbot.util.Instruction;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service("INFO_CARD")
public class InfoCardService implements MessageService<OsuUser> {

    private static final Logger log = LoggerFactory.getLogger(InfoCardService.class);
    @Resource
    ImageService imageService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<OsuUser> data) throws Throwable {
        var matcher2 = Instruction.DEPRECATED_YMY.matcher(messageText);
        if (matcher2.find()) throw new MiniCardException(MiniCardException.Type.MINI_Deprecated_Y);

        var matcher = Instruction.INFO_CARD.matcher(messageText);
        if (! matcher.find()) return false;

        var mode = CmdUtil.getMode(matcher);
        var isMyself = new AtomicBoolean();
        var user = CmdUtil.getUserWithOutRange(event, matcher, mode, isMyself);

        data.setValue(user);
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, OsuUser osuUser) throws Throwable {
        var from = event.getSubject();

        byte[] image;

        try {
            image = imageService.getPanelGamma(osuUser);
        } catch (Exception e) {
            log.error("迷你信息面板：渲染失败", e);
            throw new MiniCardException(MiniCardException.Type.MINI_Render_Error);
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("迷你信息面板：发送失败", e);
            throw new MiniCardException(MiniCardException.Type.MINI_Send_Error);
        }
    }
}
