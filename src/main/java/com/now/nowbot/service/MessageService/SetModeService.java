package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("set-mode")
public class SetModeService implements MessageService{
    BindDao bindDao;
    @Autowired
    public SetModeService(BindDao bindDao){
        this.bindDao = bindDao;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var user = bindDao.getUser(event.getSender().getId());
        var from = event.getSubject();

        var modeStr = matcher.group("mode");
        var mode = OsuMode.getMode(modeStr);
        if (mode == OsuMode.DEFAULT) {
            from.sendMessage("未知的格式,修改请使用0(osu),1(taiko),2(catch),3(mania)");
            return;
        }
        user.setMode(mode);
        bindDao.saveUser(user);
        from.sendMessage("已修改主模式为:"+mode.getName());
    }
}
