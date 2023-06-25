package com.now.nowbot.service.MessageService;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.Panel.ACardBuilder;
import com.now.nowbot.util.Panel.CardBuilder;
import com.now.nowbot.util.Panel.FriendPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import org.jetbrains.skija.EncodedImageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.regex.Matcher;

@Service("friend")
public class FriendService implements MessageService{
//    static final ThreadPoolExecutor threads = new ThreadPoolExecutor(0, 12, 100, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(256));
    private static final Logger log = LoggerFactory.getLogger(FriendService.class);

    BindDao bindDao;
    OsuGetService osuGetService;
    @Autowired
    public FriendService(OsuGetService osuGetService,BindDao bindDao){
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        var user = bindDao.getUser(event.getSender().getId());

        var at = QQMsgUtil.getType(event.getMessage(), At.class);
        if (at != null){ // 转为好友鉴定功能
            StringBuilder sb = new StringBuilder();


            final BinUser userFriend ;
            try {
                userFriend = bindDao.getUser(at.getTarget());
            } catch (BindException ignored) {
                throw new TipsException("对方没有绑定哦");
            }

            var myList = osuGetService.getFriendList(user);
            boolean otherIsMyFriend = myList.stream().filter(o-> o.getId().equals(userFriend.getOsuID())).findFirst().orElse(null) != null;
            if (otherIsMyFriend){
                sb.append("你已经mu了").append(userFriend.getOsuName()).append('\n');
            } else {
                sb.append("你还没有mu").append(userFriend.getOsuName()).append(",快速连接 https://osu.ppy.sh/users/").append(userFriend.getOsuID()).append('\n');
            }
            var otherList = osuGetService.getFriendList(userFriend);
            boolean IisOtherFriend = otherList.stream().filter(o-> o.getId().equals(user.getOsuID())).findFirst().orElse(null) != null;
            if (IisOtherFriend){
                sb.append("ta已经mu了你");
            } else {
                sb.append("ta还没有mu你,快速连接 https://osu.ppy.sh/users/").append(user.getOsuID());
            }
            from.sendMessage(sb.toString());
            return;
        }

        //拿到参数,默认1-24个
        int n1 = 0,n2=0;
        boolean doRandom = true;
        if (matcher.group("m") == null){
            n2 = matcher.group("n") == null? 24 : Integer.parseInt(matcher.group("n"));
        }else {
            doRandom = false;
            n1 = Integer.parseInt(matcher.group("n"));
            n2 = Integer.parseInt(matcher.group("m"));
            if(n1 > n2) {n1 ^= n2; n2 ^= n1; n1 ^= n2;}
            n1--;
        }
        n2--;
        if (n2 == 0 || 100 < n2-n1 ){
            throw new TipsException("参数范围错误!");
        }

        var allFriend = osuGetService.getFriendList(user);
        final var p = new FriendPanelBuilder();
        //构造自己的卡片
        var infoMe = osuGetService.getPlayerInfo(user);
        var card = CardBuilder.getUserCard(infoMe);

        p.drawBanner(PanelUtil.getBanner(user));
        p.mainCard(card.build());
       int[] index = null;
       if (doRandom) {
           //构造随机数组
           index = new int[allFriend.size()];
           for (int i = 0; i < index.length; i++) {
               index[i] = i;
           }
           for (int i = 0; i < index.length; i++) {
               int rand = rand(i,index.length);
               if (rand != 1) {
                   int temp = index[rand];
                   index[rand] = index[i];
                   index[i] = temp;
               }
           }
       }
        //好友绘制
        for (int i = n1; i <= n2 && i < allFriend.size(); i++) {
            try {
                MicroUser infoO;
                if (doRandom){
                    infoO = allFriend.get(index[i]);
                }else {
                    infoO = allFriend.get(i);
                }

                var cardO = new ACardBuilder(PanelUtil.getBgUrl(null,infoO.getCover().url,true));
                cardO.drawA1(infoO.getAvatar())
                        .drawA2(PanelUtil.getFlag(infoO.getCountry().countryCode()))
                        .drawA3(infoO.getUserName());
                if (infoO.getIsSupporter()){
                    cardO.drawA2(PanelUtil.OBJECT_CARD_SUPPORTER);
                }
                //对bot特殊处理
                if(infoO.getIsBot()){
                    cardO.drawB1("U" + infoO.getId()).drawC1("Bot");
                } else {
                    cardO.drawB2("#" + infoO.getStatustucs().getGlobalRank())
                            .drawB1("U" + infoO.getId())
                            .drawC2(infoO.getStatustucs().getAccuracy(4) + "% Lv." +
                                    infoO.getStatustucs().getLevelCurrent() +
                                    "(" + infoO.getStatustucs().getLevelProgress() + "%)")
                            .drawC1(Math.round(infoO.getStatustucs().getPP()) + "PP");
                }
                p.addFriendCard(cardO.build());
            } catch (Exception e) {
                log.error("卡片加载第{}个失败,数据为\n{}",i,allFriend.get(i).toString(),e);
            }
        }

        QQMsgUtil.sendImage(from, p.build().encodeToData(EncodedImageFormat.JPEG,80).getBytes());
        card.build().close();
        p.build().close();
    }
    static final Random random = new Random();
    static int rand(int min, int max){
        return min + random.nextInt(max-min);
    }

}
