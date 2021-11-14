package com.now.nowbot.service.MessageService;

import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.BindingUtil;
import com.now.nowbot.util.Panel.FriendPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

@Service("friend")
public class FriendService implements MessageService{
    static final ThreadPoolExecutor threads = new ThreadPoolExecutor(0, 12, 100, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(256));

    OsuGetService osuGetService;
    @Autowired
    public FriendService(OsuGetService osuGetService){
        this.osuGetService = osuGetService;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        var user = BindingUtil.readUser(event.getSender().getId());

        var allFriend = osuGetService.getFrendList(user);

        allFriend.size();
        //拿到参数,默认1-24个
        int n1 = 0,n2=0;
        if (matcher.group("m") == null){
            n2 = matcher.group("n") == null? 23 : Integer.parseInt(matcher.group("n"));
        }else {
            n1 = Integer.parseInt(matcher.group("n"));
            n2 = Integer.parseInt(matcher.group("m"));
            if(n1 > n2) {n1 ^= n2; n2 ^= n1; n1 ^= n2;}
        }
        if (n1 < 0 || 100 < n2-n1 ){
            throw new TipsException("参数范围错误!");
        }
        final var p = new FriendPanelBuilder();
        p.drawBanner(PanelUtil.getBgUrl("个人banner路径", PanelUtil.EXPORT_FOLE_V3.resolve("DrawCard/Card-N-5.png").toString(),false));
    }
}
