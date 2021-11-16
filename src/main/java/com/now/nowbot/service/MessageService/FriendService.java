package com.now.nowbot.service.MessageService;

import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.BindingUtil;
import com.now.nowbot.util.Panel.ACardBuilder;
import com.now.nowbot.util.Panel.FriendPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.EncodedImageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("friend")
public class FriendService implements MessageService{
//    static final ThreadPoolExecutor threads = new ThreadPoolExecutor(0, 12, 100, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(256));
    private static final Logger log = LoggerFactory.getLogger(FriendService.class);

    OsuGetService osuGetService;
    @Autowired
    public FriendService(OsuGetService osuGetService){
        this.osuGetService = osuGetService;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        var user = BindingUtil.readUser(event.getSender().getId());
//        var user = BindingUtil.readUser(2480557535L); 调试代码

        //拿到参数,默认1-24个
        int n1 = 0,n2=0;
        if (matcher.group("m") == null){
            n2 = matcher.group("n") == null? 23 : Integer.parseInt(matcher.group("n"));
        }else {
            n1 = Integer.parseInt(matcher.group("n"));
            n2 = Integer.parseInt(matcher.group("m"));
            if(n1 > n2) {n1 ^= n2; n2 ^= n1; n1 ^= n2;}
        }
        if (n2 == 0 || 100 < n2-n1 ){
            throw new TipsException("参数范围错误!");
        }

        var allFriend = osuGetService.getFrendList(user);
        final var p = new FriendPanelBuilder();
        //构造自己的卡片
        var infoMe = osuGetService.getPlayerOsuInfo(user);
        var card = new ACardBuilder(PanelUtil.getBgUrl(null/*"自定义路径"*/,infoMe.getJSONObject("cover").getString("url"),true));
        card.drawA1(infoMe.getString("avatar_url"))
                .drawA2(PanelUtil.getFlag(infoMe.getJSONObject("country").getString("code")))
                .drawA3(infoMe.getString("username"));
        if (infoMe.getBoolean("is_supporter")){
            card.drawA2(PanelUtil.OBJECT_CARD_SUPPORTER);
        }
        card.drawB3("")
                .drawB2(infoMe.getJSONObject("country").getString("code") + "#" + infoMe.getJSONObject("statistics").getString("country_rank"))
                .drawB1("U" + infoMe.getString("id"))
                .drawC2(infoMe.getJSONObject("statistics").getString("hit_accuracy").substring(0, 4) + "% Lv." +
                        infoMe.getJSONObject("statistics").getJSONObject("level").getString("current") +
                        "(" + infoMe.getJSONObject("statistics").getJSONObject("level").getString("progress") + "%)")
                .drawC1(infoMe.getJSONObject("statistics").getIntValue("pp") + "PP");

        p.drawBanner(PanelUtil.getBgFile(null/*"个人banner路径"*/, PanelUtil.EXPORT_FOLE_V3.resolve("Banner/b3.png").toString(),false));
        p.mainCard(card.build());
        //单线程实现好友绘制
        for (int i = n1; i <= n2 && i < allFriend.size(); i++) {
            try {
                var infoO = allFriend.get(i);

                var cardO = new ACardBuilder(PanelUtil.getBgUrl(null,infoO.findValue("url").asText(),true));
                cardO.drawA1(infoO.findValue("avatar_url").asText())
                        .drawA2(PanelUtil.getFlag(infoO.findValue("country_code").asText()))
                        .drawA3(infoO.findValue("username").asText());
                if (infoO.findValue("is_supporter").asBoolean(false)){
                    cardO.drawA2(PanelUtil.OBJECT_CARD_SUPPORTER);
                }
                //对bot特殊处理
                if(infoO.findValue("is_bot").asBoolean(false)){
                    cardO.drawB1("U" + infoO.findValue("id").asText("NaN")).drawC1("Bot");
                } else {
                    cardO.drawB2("#" + infoO.findValue("global_rank").asText("0"))
                            .drawB1("U" + infoO.findValue("id").asText("NaN"))
                            .drawC2(infoO.findValue("hit_accuracy").asText().substring(0, 4) + "% Lv." +
                                    infoO.findValue("current").asText("NaN") +
                                    "(" + infoO.findValue("progress").asText("NaN") + "%)")
                            .drawC1(infoO.findValue("pp").asInt() + "PP");
                }
                p.addFriendCard(cardO.build());
            } catch (Exception e) {
                log.error("卡片加载第{}个失败,数据为\n{}",i,allFriend.get(i).toString(),e);
            }
        }

        from.sendMessage(from.uploadImage(ExternalResource.create(p.build().encodeToData(EncodedImageFormat.JPEG,80).getBytes())));
//        Files.write(Path.of("D:/ffxx.jpg"),p.build().encodeToData(EncodedImageFormat.JPEG,80).getBytes());调试代码,勿动
        card.build().close();
        p.build().close();
    }
}
