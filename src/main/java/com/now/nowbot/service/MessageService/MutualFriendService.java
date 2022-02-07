package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.RequestException;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("mu")
public class MutualFriendService implements MessageService{
    private OsuGetService osuGetService;
    @Autowired
    BindDao bindDao;
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
                try {
                    Long id = osuGetService.getOsuId(name);
                    sb.append(name).append(" : ").append(id).append("\n");
                } catch (Exception res) {
                    if (res instanceof RequestException r && r.status.getReasonPhrase().equals("Not Found")) {
                        sb.append(name).append(" : ").append("请求目标不存在").append("\n");
                    } else {
                        sb.append(name).append(" : ").append("未知错误").append("\n");
                        res.printStackTrace();
                    }
                }
            }
            event.getSubject().sendMessage(sb.toString());
            return;
        }
        var user = bindDao.getUser(event.getSender().getId());
        var id = user.getOsuID();

        event.getSubject().sendMessage("https://osu.ppy.sh/users/" + id);
    }
}
