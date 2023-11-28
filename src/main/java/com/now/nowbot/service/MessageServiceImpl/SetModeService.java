package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.Pattern4ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("SETMODE")
public class SetModeService implements MessageService<Matcher> {
    BindDao bindDao;
    @Autowired
    public SetModeService(BindDao bindDao){
        this.bindDao = bindDao;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = Pattern4ServiceImpl.SETMODE.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var user = bindDao.getUserFromQQ(event.getSender().getId());
        var from = event.getSubject();

        var modeStr = matcher.group("mode");
        var mode = OsuMode.getMode(modeStr);
        if (mode == OsuMode.DEFAULT) {
            from.sendMessage("未知的格式,修改请使用0(osu),1(taiko),2(catch),3(mania)");
            return;
        }
        user.setMode(mode);
        bindDao.updateMod(user.getOsuID(), mode);
        from.sendMessage("已修改主模式为:"+mode.getName());
    }
}
