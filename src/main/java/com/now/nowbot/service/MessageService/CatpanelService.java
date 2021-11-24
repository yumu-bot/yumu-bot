package com.now.nowbot.service.MessageService;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.dao.QQMessageDao;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.QuoteReply;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("cpanel")
/*
 我这是写了个啥,要不要删掉
 */
/*
 我趣，你为啥把猫bot的界面搬过来了
 */

public class CatpanelService implements MessageService {
    QQMessageDao qqMessageDao;
    @Autowired
    public CatpanelService(QQMessageDao qqMessageDao){
        this.qqMessageDao = qqMessageDao;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {

        boolean stl = matcher.group("yl") != null;
        int an = matcher.group("bk") == null ? 0 : Integer.parseInt(matcher.group("bk"));
        if (an>100) an = 100;
        Image img;
        QuoteReply reply = event.getMessage().get(QuoteReply.Key);
        if (reply != null) {
            var msg = qqMessageDao.getReply(reply);
            img = (Image) msg.stream().filter(it -> it instanceof Image).findFirst().orElse(
                    event.getMessage().stream().filter(it -> it instanceof Image).findFirst().orElse(null)
            );
        }else {
            img = (Image) event.getMessage().stream().filter(it -> it instanceof Image).findFirst().orElse(null);
        }

        if (img == null) {
            event.getSubject().sendMessage("没有任何图片");
            return;
        }
        var skijaimg = SkiaUtil.getScaleCenterImage(SkiaUtil.lodeNetWorkImage(Image.queryUrl(img)), 1200,857);

        var surface = Surface.makeRasterN32Premul(1200,857);
        var t1 = SkiaUtil.fileToImage(NowbotConfig.BG_PATH + "panel05.png");
        var t2 = SkiaUtil.fileToImage(NowbotConfig.BG_PATH + (stl?"ylbx.png":"lbx.png"));

        byte[] data;
        try(skijaimg;surface;t1;t2){
            var canvas = surface.getCanvas();
            canvas.drawImage(skijaimg,0,0);
            canvas.drawRect(Rect.makeWH(surface.getWidth(),surface.getHeight()),new Paint().setColor(Color.makeRGB(0,0,0)).setAlphaf(an/100f));
            canvas.drawImage(t1,0,0);
            canvas.drawImage(t2,0,0);
            data = surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG).getBytes();
            event.getSubject().sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(data),event.getSubject()));
        }
    }
}
