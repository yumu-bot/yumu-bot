package com.now.nowbot.service.msgServiceImpl;

import com.now.nowbot.entity.BinUser;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.GroupTempMessageEvent;
import net.mamoe.mirai.event.events.StrangerMessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageUtils;
import net.mamoe.mirai.message.data.PlainText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BindingServiceImpl extends MessageService{
    @Autowired
    OsuGetService osuGetService;
    Logger log = LoggerFactory.getLogger(BindingServiceImpl.class);
    public BindingServiceImpl(){
        super("bind");
    }

    @Override
    public void handleMsg(FriendMessageEvent event) {
        String state = String.valueOf(event.getSender().getId());
        event.getSender().sendMessage(osuGetService.getOauthUrl(state));
    }

    @Override
    public void handleMsg(StrangerMessageEvent event) {
        String state = String.valueOf(event.getSender().getId());
        event.getSender().sendMessage(osuGetService.getOauthUrl(state));
    }

    @Override
    public void handleMsg(GroupTempMessageEvent event) {
        String state = String.valueOf(event.getSender().getId());
        event.getSender().sendMessage(osuGetService.getOauthUrl(state));
    }

    @Override
    public void handleMsg(GroupMessageEvent event) {

        BinUser user = BindingUtil.readUser(event.getSender().getId());
        if(user == null){
            String state = event.getSender().getId() + "+" + event.getGroup().getId();
            event.getGroup().sendMessage(osuGetService.getOauthUrl(state));
        }else {
            event.getGroup().sendMessage(MessageUtils.newChain(new At(event.getSender().getId()),new PlainText("您已经绑定，若要修改请私聊bot bind")));
        }
    }
}
