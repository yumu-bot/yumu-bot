package com.now.nowbot.service.msgServiceImpl;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.contact.*;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.SingleMessage;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InfopanelServiceImpl extends MessageService{
    private static final Logger log = LoggerFactory.getLogger(InfopanelServiceImpl.class);

    public InfopanelServiceImpl(){
        super("infopanel");
    }

    @Override
    public void handleMsg(MessageEvent event, String[] page) {
        Contact from;
        if(event instanceof GroupMessageEvent) {
            from = ((GroupMessageEvent) event).getGroup();
        }else {
            from = event.getSender();
        }
        Image pi = null;
        MessageChain data = event.getMessage();
        for (SingleMessage m : data) {
            if (m instanceof Image){
                pi = (Image) m;
                break;
            }
        }

        if (pi == null){
            from.sendMessage(new At(event.getSender().getId()).plus("\n"+getKey()+" [bk:<暗化度(1-100之间)>] [ylbx(开启圆形六边形)]\n" +
                    "指令务必使用空格分开，\":\"左右不得有空格"));
            return;
        }
        boolean stl = false;
        int tmd = 0;
        Pattern p = Pattern.compile("\\s+(bk:(?<bk>\\d+))?\\s*(?<yl>ylbx)?");
        Matcher m = p.matcher(event.getMessage().contentToString());
        while (m.find()) {
            if(m.group("bk") != null){
                try {
                    tmd = Integer.parseInt(m.group("bk"));
                    if (tmd<0 || tmd>100) throw new Exception("数值不合法");
                } catch (Exception e) {
                    from.sendMessage(e.getMessage());
                    return;
                }
            }
            if(m.group("yl") != null) stl = true;
        }
        org.jetbrains.skija.Image netImg = null;
        org.jetbrains.skija.Image topImg = null;
        try {
            netImg = SkiaUtil.lodeNetWorkImage(Image.queryUrl(pi));
            topImg = SkiaUtil.fileToImage(NowbotConfig.BG_PATH +"panel05.png");
        } catch (IOException e) {
            from.sendMessage(e.getMessage());
        }

        int imgWidth = netImg.getWidth();
        int imgHeight = netImg.getHeight();

        org.jetbrains.skija.Image siImg;
        if(1f*imgWidth/imgHeight<1200f/857) {
            org.jetbrains.skija.Image img1 = SkiaUtil.getScaleImage(netImg, 1200, 1200 * imgHeight / imgWidth);
            siImg = SkiaUtil.getCutImage(img1, 0, (img1.getHeight()-857)/2, 1200, 857);
        }else {
            org.jetbrains.skija.Image img1 = SkiaUtil.getScaleImage(netImg, 857*imgWidth/imgHeight, 857);
            siImg = SkiaUtil.getCutImage(img1, (img1.getWidth()-1200)/2, 0, 1200, 857);
        }
        Surface surface = Surface.makeRasterN32Premul(1200, 857);
        Canvas canvas = surface.getCanvas();
        canvas.drawImage(siImg,0,0);
        canvas.drawRect(Rect.makeXYWH(0,0,surface.getWidth(),surface.getHeight()),new Paint().setARGB(255*tmd/100,0,0,0));
        canvas.drawImage(topImg,0,0);
        try {
            if (stl){
                canvas.drawImage(SkiaUtil.fileToImage(NowbotConfig.BG_PATH+"ylbx.png"),0,0);
            }else {
                canvas.drawImage(SkiaUtil.fileToImage(NowbotConfig.BG_PATH+"lbx.png"),0,0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] pngBytes = surface.makeImageSnapshot().encodeToData().getBytes();

        from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(pngBytes), from));

    }
}
