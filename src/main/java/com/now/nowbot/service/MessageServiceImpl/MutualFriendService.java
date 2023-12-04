package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("MUTUAL")
public class MutualFriendService implements MessageService<Matcher> {
    private final OsuUserApiService userApiService;


    BindDao bindDao;

    MutualFriendService(OsuUserApiService userApiService, BindDao bindDao) {
        this.userApiService = userApiService;
        this.bindDao = bindDao;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = Instructions.MUTUAL.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {

        var atList = QQMsgUtil.getTypeAll(event.getMessage(), AtMessage.class);
        if (!atList.isEmpty()){
            var data = new MessageChain.MessageChainBuilder();
            atList.forEach(at->{
                data.addAt(at.getTarget());

                    try {
                        var u = bindDao.getUserFromQQ(at.getTarget());
                        data.addText(" https://osu.ppy.sh/users/" + u.getOsuID() + '\n');
                    } catch (BindException e) {
                        data.addText(" 未绑定\n");
                    }
            });
            event.getSubject().sendMessage(data.build());
            return;
        }
        var s = matcher.group("names");
        if (s != null && !s.trim().isEmpty()){
            var names = s.split(",");
            StringBuilder sb = new StringBuilder();
            for (var name:names){
                Long id = userApiService.getOsuId(name);
                sb.append(name).append(" : https://osu.ppy.sh/users/").append(id).append("\n");
            }
            event.getSubject().sendMessage(sb.toString());
            return;
        }


        var user = bindDao.getUserFromQQ(event.getSender().getId());
        var id = user.getOsuID();

        event.getSubject().sendMessage("https://osu.ppy.sh/users/" + id);
    }
}
