package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Objects;
import java.util.regex.Matcher;

@Service("MUTUAL")
public class MutualFriendService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(MutualFriendService.class);
    @Resource
    OsuUserApiService userApiService;

    @Resource
    BindDao bindDao;
    @Resource
    ImageService imageService;

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

        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        if (Objects.nonNull(at)){
            var data = new MessageChain.MessageChainBuilder();

            data.addAt(at.getTarget());

            try {
                var u = bindDao.getUserFromQQ(at.getTarget());
                byte[] pic = imageService.getPanelA6(STR."# https://osu.ppy.sh/users/\{u.getOsuID()}");

                data.addText("\n");
                data.addImage(pic);
            } catch (BindException e) {
                data.addText(" 未绑定\n");
            }

            event.getSubject().sendMessage(data.build());
            return;
        }
        var name = matcher.group("names");
        if (Objects.nonNull(name)){
            String s = "";
            for (var n : name.split(",")){
                Long id = userApiService.getOsuId(n);
                s = STR."# \{n} : https://osu.ppy.sh/users/\{id}\n";
            }
            try {
                byte[] pic = imageService.getPanelA6(s);
                event.getSubject().sendImage(pic);
            } catch (HttpClientErrorException e) {
                event.getSubject().sendMessage(s);
            } catch (Exception e) {
                log.error("MU：", e);
            }

            return;
        }


        var user = bindDao.getUserFromQQ(event.getSender().getId());
        var id = user.getOsuID();

        event.getSubject().sendMessage("https://osu.ppy.sh/users/" + id);
    }
}
