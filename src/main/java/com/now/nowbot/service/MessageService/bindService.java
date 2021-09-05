package com.now.nowbot.service.MessageService;

import com.now.nowbot.entity.BinUser;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.At;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("bind")
public class bindService extends MsgSTemp implements MessageService {
    public final Map<Long, MessageReceipt> msgs = new ConcurrentHashMap<>();
    @Autowired
    OsuGetService osuGetService;

    bindService() {
        super(Pattern.compile("^[!！]\\s?(?i)bind"), "bind");
        setInfo("!bind 发送本命令将回复一个授权链接，授权完成即可绑定");
        new Thread(this::delpassed).start();
    }

    void delpassed() {
        try {
            while (true) {
                wait(60 * 1000);
                msgs.keySet().removeIf(k -> (k + 120 * 1000) < System.currentTimeMillis());
            }
        } catch (Exception e) {
            //TODO 处理异常
        }
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        long d = System.currentTimeMillis();
        if ((event.getSubject() instanceof Group)) {
            BinUser user = null;
            try {
                user = BindingUtil.readUser(event.getSender().getId());
            } catch (Exception e) {
                String state = event.getSender().getId() + "+" + d;
                var ra = event.getSubject().sendMessage(new At(event.getSender().getId()).plus(osuGetService.getOauthUrl(state)));
                ra.recallIn(110 * 1000);
                msgs.put(d, ra);
                return;
            }
            event.getSubject().sendMessage(new At(event.getSender().getId()).plus("您已绑定，若要修改绑定请私发bot绑定命令"));
            return;
        }
        String state = event.getSender().getId() + "+" + d;
        var e = event.getSubject().sendMessage(osuGetService.getOauthUrl(state));
        e.recallIn(110 * 1000);
        msgs.put(d, e);
        return;
    }
}
