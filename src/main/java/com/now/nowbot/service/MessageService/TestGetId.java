package com.now.nowbot.service.MessageService;

import com.now.nowbot.config.Permission;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("t-id")
public class TestGetId implements MessageService{
    OsuGetService osuGetService;

    @Autowired
    public TestGetId(OsuGetService osuGetService){
        this.osuGetService = osuGetService;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        if (Permission.isSupper(event.getSender().getId())){
            var idsStr = matcher.group("ids").split(",");
            StringBuilder sb = new StringBuilder();
            for(var idStr : idsStr){
                var id = Integer.parseInt(idStr);
                var data = osuGetService.getPlayerInfo((long) id, OsuMode.OSU);
                sb.append(id).append("***").append(data.getUsername()).append('\n');
            }
            event.getSubject().sendMessage(sb.toString());
        }
    }
}
