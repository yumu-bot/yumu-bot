package com.now.nowbot.service.MessageService;

import com.now.nowbot.model.BinUser;
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

@Service("bind")
public class bindService implements MessageService {
    public static final Map<Long, MessageReceipt> msgs = new ConcurrentHashMap<>();
    @Autowired
    OsuGetService osuGetService;

    bindService() {
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
        //将当前毫秒时间戳作为 key
        long d = System.currentTimeMillis();
        //群聊验证是否绑定
        if ((event.getSubject() instanceof Group)) {
            BinUser user = null;
            try {
                user = BindingUtil.readUser(event.getSender().getId());
            } catch (Exception e) {//未绑定时会出现file not find
                String state = event.getSender().getId() + "+" + d;
                //将消息回执作为 value
                var ra = event.getSubject().sendMessage(new At(event.getSender().getId()).plus(osuGetService.getOauthUrl(state)));
                //默认110秒后撤回
                ra.recallIn(110 * 1000);
                //此处在 controller.msgController 处理
                msgs.put(d, ra);
                return;
            }
            event.getSubject().sendMessage(new At(event.getSender().getId()).plus("您已绑定，若要修改绑定请私发bot绑定命令"));
            return;
        }
        //私聊不验证是否绑定
        String state = event.getSender().getId() + "+" + d;
        var e = event.getSubject().sendMessage(osuGetService.getOauthUrl(state));
        e.recallIn(110 * 1000);
        msgs.put(d, e);
        return;
    }
}
