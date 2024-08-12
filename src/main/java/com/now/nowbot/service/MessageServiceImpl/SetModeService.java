package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.Instruction;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

import static com.now.nowbot.util.command.CommandPatternStaticKt.FLAG_MODE;

@Service("SET_MODE")
public class SetModeService implements MessageService<Matcher> {
    @Resource
    BindDao bindDao;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instruction.SET_MODE.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var user = bindDao.getUserFromQQ(event.getSender().getId());
        var from = event.getSubject();

        var modeStr = matcher.group(FLAG_MODE);
        var mode = OsuMode.getMode(modeStr);
        if (mode == OsuMode.DEFAULT) {
            from.sendMessage("未知的格式,修改请使用0(osu),1(taiko),2(catch),3(mania)");
            return;
        }
        user.setOsuMode(mode);
        bindDao.updateMod(user.getOsuID(), mode);
        from.sendMessage("已修改主模式为:"+mode.getName());
    }
}
