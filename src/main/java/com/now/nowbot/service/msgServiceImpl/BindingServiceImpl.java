package com.now.nowbot.service.msgServiceImpl;

import com.now.nowbot.entity.BinUser;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.GroupTempMessageEvent;
import net.mamoe.mirai.event.events.StrangerMessageEvent;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageUtils;
import net.mamoe.mirai.message.data.PlainText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service("bindservice")
public class BindingServiceImpl extends MessageService{
    public final Map<Long, MessageReceipt> msgs = new ConcurrentHashMap<>();
    @Autowired
    OsuGetService osuGetService;
    Logger log = LoggerFactory.getLogger(BindingServiceImpl.class);
    public BindingServiceImpl(){
        super("bind");
        new Thread(this::delpassed).start();
    }

    void delpassed(){
        try{
            while(true) {
                wait(60 * 1000);
                msgs.keySet().removeIf(k -> (k + 120 * 1000) < System.currentTimeMillis());
            }
        }catch (Exception e){
            //TODO 处理异常
        }
    }

    @Override
    public void handleMsg(FriendMessageEvent event) {
        String state = String.valueOf(event.getSender().getId());
        var e = event.getSender().sendMessage(osuGetService.getOauthUrl(state));
        msgs.put(System.currentTimeMillis() ,e);
    }

    @Override
    public void handleMsg(StrangerMessageEvent event) {
        String state = String.valueOf(event.getSender().getId());
        var e = event.getSender().sendMessage(osuGetService.getOauthUrl(state));
        msgs.put(System.currentTimeMillis() ,e);
    }

    @Override
    public void handleMsg(GroupTempMessageEvent event) {
        String state = String.valueOf(event.getSender().getId());
        var e = event.getSender().sendMessage(osuGetService.getOauthUrl(state));
        msgs.put(System.currentTimeMillis() ,e);
    }

    @Override
    public void handleMsg(GroupMessageEvent event) {

        BinUser user = BindingUtil.readUser(event.getSender().getId());
        if(user == null){
            String state = event.getSender().getId() + "+" + event.getGroup().getId();
            var e = event.getGroup().sendMessage(new At(event.getSender().getId()).plus(osuGetService.getOauthUrl(state)));
            msgs.put(System.currentTimeMillis() ,e);
        }else {
            event.getGroup().sendMessage(MessageUtils.newChain(new At(event.getSender().getId()),new PlainText("您已经绑定，若要修改请私聊bot bind")));
        }
    }
}
