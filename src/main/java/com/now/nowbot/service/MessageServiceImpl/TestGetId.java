package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.config.Permission;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("t-id")
public class TestGetId implements MessageService<Matcher> {
    OsuGetService osuGetService;

    @Autowired
    public TestGetId(OsuGetService osuGetService){
        this.osuGetService = osuGetService;
    }

    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)testid\\s*(?<ids>((\\d)+(,)?)+)");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = pattern.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        if (Permission.isSuper(event.getSender().getId())){
            var idsStr = matcher.group("ids").split(",");
            StringBuilder sb = new StringBuilder();
            for(var idStr : idsStr){
                var id = Integer.parseInt(idStr);
                try {
                    var data = osuGetService.getPlayerInfo((long) id, OsuMode.OSU);
                    sb.append(data.getUsername()).append(',');
                } catch (Exception e) {
                    sb.append(id).append("!!!").append("getError").append('\n');
                }
            }

            event.getSubject().sendMessage(sb.toString());
        }
    }
}
