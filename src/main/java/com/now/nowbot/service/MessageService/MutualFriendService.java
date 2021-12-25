package com.now.nowbot.service.MessageService;

import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("mu")
public class MutualFriendService implements MessageService{
    private OsuGetService osuGetService;
    @Autowired
    MutualFriendService(OsuGetService osuGetService){
        this.osuGetService = osuGetService;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        if (matcher.group("t") != null){
            Pattern pattern = Pattern.compile("([\\s,]+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))");
            var m = pattern.matcher(matcher.group("names"));
            StringBuilder sb = new StringBuilder();
            while (m.find()){
                String name = m.group("name").trim();
                int id = osuGetService.getOsuId(name);
                sb.append(name).append(" : ").append(id).append("\n");
            }
            event.getSubject().sendMessage(sb.toString());
            return;
        }
        var user = BindingUtil.readUser(event.getSender().getId());
        var id = user.getOsuID();

        event.getSubject().sendMessage("https://osu.ppy.sh/users/" + id);
    }
}
