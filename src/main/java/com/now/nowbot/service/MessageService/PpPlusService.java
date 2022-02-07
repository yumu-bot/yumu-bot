package com.now.nowbot.service.MessageService;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.dao.PPPlusDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.PpPlus;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.LogException;
import com.now.nowbot.throwable.serviceException.PppException;
import com.now.nowbot.util.BindingUtil;
import com.now.nowbot.util.Panel.PPPlusPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.SkiaImageUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.regex.Matcher;

@Service("ppp")
public class PpPlusService implements MessageService{
    Logger log = LoggerFactory.getLogger(PpPlusService.class);
    @Autowired
    OsuGetService osuGetService;
    @Autowired
    PPPlusDao ppPlusDao;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable{
        Contact from = event.getSubject();

        BinUser user = null;
        Long id;
        At at;
        at = (At)event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
        if (at != null){
            user = bindDao.getUser(at.getTarget());
            id = user.getOsuID();
        }else {
            String name = matcher.group("name");
            if(name == null || name.trim().equals("")){
                user = bindDao.getUser(event.getSender().getId());
                id = user.getOsuID();
            }else {
                id = (long) osuGetService.getOsuId(name);
            }
        }

        String idString,headUrl;
        JSONObject userData;
        idString = String.valueOf(id);
        if (user != null) {
            userData = osuGetService.getPlayerOsuInfo(user);
        }else {
            userData = osuGetService.getPlayerOsuInfo(Math.toIntExact(id));
        }
        headUrl = userData.getString("avatar_url");


        PpPlus pppData = null;
        try {
            pppData = ppPlusDao.getobject(idString);
        } catch (Exception e) {
            log.info("ppp 错误",e);
            throw new PppException(PppException.Type.PPP_Default_APIConnectFailed);
        }
        ppp(from, pppData, userData);

        float[] hex1 = ppPlusDao.ppsize(pppData);

        byte[] datebyte = null;
        try (Surface surface = Surface.makeRasterN32Premul(1920,1080);
             Typeface fontface = SkiaUtil.getTorusRegular();
             Font fontA = new Font(fontface, 80);
             Font fontB = new Font(fontface, 64);
             Paint white = new Paint().setARGB(255,255,255,255);
        ){
            var canvas = surface.getCanvas();

            Image bg1 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH,"PPPlusBG.png")));
            Image bg2 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH,"PPHexPanel.png")));
            Image bg3 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH,"PPPlusOverlay.png")));
            Image bg4 = Image.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(NowbotConfig.BG_PATH,"mascot.png")));
            canvas.drawImage(bg1,0,0);
            canvas.drawImage(bg2,0,0);
            //在底下
            canvas.drawImage(bg4,surface.getWidth()-bg4.getWidth(),surface.getHeight()-bg4.getHeight(),new Paint().setAlpha(51));

            canvas.save();
            canvas.translate(960,440);
            var paths = SkiaUtil.creat6(390, 10, hex1);
            canvas.drawPath(paths[0],new Paint().setARGB(255,42,98,183).setStroke(true).setStrokeWidth(5));
            canvas.drawPath(paths[0],new Paint().setARGB(102,42,98,183).setStroke(false).setStrokeWidth(5));
            canvas.drawPath(paths[1],new Paint().setARGB(255,42,98,183).setStroke(false).setStrokeWidth(5));
            TextLine ppm$ = TextLine.make("PP+",fontA);
            canvas.drawTextLine(ppm$, -0.5f*ppm$.getWidth(), 0.5f*ppm$.getCapHeight(),white);
            canvas.restore();
            canvas.drawImage(bg3,513,74);

            canvas.save();
            canvas.translate(280,440);
            TextLine text = TextLine.make(pppData.getName(), fontA);
            if (text.getWidth() > 500) text = TextLine.make(pppData.getName().substring(0,8)+"...",fontA);
            canvas.drawTextLine(text, -0.5f*text.getWidth(),0.25f*text.getHeight(),white);
            canvas.restore();

            DecimalFormat dx = new DecimalFormat("0");
            canvas.save();
            canvas.translate(100,520);
            TextLine k1 = TextLine.make("Jump",fontB);
            TextLine v1 = TextLine.make(dx.format(pppData.getJump()),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Flow",fontB);
            v1 = TextLine.make(dx.format(pppData.getFlow()),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Acc",fontB);
            v1 = TextLine.make(dx.format(pppData.getAcc()),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Sta",fontB);
            v1 = TextLine.make(dx.format(pppData.getSta()),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Spd",fontB);
            v1 = TextLine.make(dx.format(pppData.getSpd()),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.translate(0,90);
            k1 = TextLine.make("Pre",fontB);
            v1 = TextLine.make(dx.format(pppData.getPre()),fontB);
            canvas.drawTextLine(k1 ,0,v1.getCapHeight(),white);
            canvas.drawTextLine(v1 ,360-v1.getWidth(),v1.getCapHeight(),white);
            canvas.restore();

            canvas.save();
            canvas.translate(920,880);
            v1 = TextLine.make(dx.format(pppData.getTotal()),fontA);
            canvas.drawTextLine(v1,-v1.getWidth(),v1.getCapHeight(),white);
            canvas.restore();

            PpPlusVsService.drawLhead(canvas, SkiaImageUtil.getImage(headUrl));

            datebyte = surface.makeImageSnapshot().encodeToData().getBytes();
        }
        if (datebyte != null ){
            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(datebyte),from));
        }
    }
    private void ppp(Contact from, PpPlus pppData, JSONObject userData) throws IOException, LogException {
        var card = PanelUtil.getA1Builder(PanelUtil.getBgUrl("用户自定义路径", userData.getString("cover_url"), true));
        card.drawA1(userData.getString("avatar_url"))
                .drawA2(PanelUtil.getFlag(userData.getJSONObject("country").getString("code")))
                .drawA3(userData.getString("username"))
                .drawB2("#" + userData.getJSONObject("statistics").getString("global_rank"))
                .drawB1(userData.getJSONObject("country").getString("code") + "#" + userData.getJSONObject("statistics").getString("country_rank"))
                .drawC2(userData.getJSONObject("statistics").getString("hit_accuracy").substring(0, 4) + "% Lv." +
                        userData.getJSONObject("statistics").getJSONObject("level").getString("current") +
                        "(" + userData.getJSONObject("statistics").getJSONObject("level").getString("progress") + "%)")
                .drawC1(userData.getJSONObject("statistics").getIntValue("pp") + "PP");
        if (userData.getBoolean("is_supporter")) {
            card.drawA2(PanelUtil.OBJECT_CARD_SUPPORTER);
        }

        var hexValue = ppPlusDao.ppsize(pppData);
        DecimalFormat dx = new DecimalFormat("0");
        var panel = new PPPlusPanelBuilder();
        panel.drawBanner(PanelUtil.getBanner(null))
                .drawOverImage()
                .drawValueName()
                .drawLeftCard(card.build());
        panel.drawLeftValueN(0,String.valueOf(pppData.getJump().intValue()),PanelUtil.cutDecimalPoint(pppData.getJump()));
        panel.drawLeftValueN(1,String.valueOf(pppData.getFlow().intValue()),PanelUtil.cutDecimalPoint(pppData.getFlow()));
        panel.drawLeftValueN(2,String.valueOf(pppData.getAcc().intValue()),PanelUtil.cutDecimalPoint(pppData.getAcc()));
        panel.drawLeftValueN(3,String.valueOf(pppData.getSta().intValue()),PanelUtil.cutDecimalPoint(pppData.getSta()));
        panel.drawLeftValueN(4,String.valueOf(pppData.getSpd().intValue()),PanelUtil.cutDecimalPoint(pppData.getSpd()));
        panel.drawLeftValueN(5,String.valueOf(pppData.getPre().intValue()),PanelUtil.cutDecimalPoint(pppData.getPre()));

        panel.drawLeftTotal(String.valueOf(pppData.getTotal().intValue()),PanelUtil.cutDecimalPoint(pppData.getTotal()));
        panel.drawRightTotal(String.valueOf(userData.getJSONObject("statistics").getIntValue("pp")));
        panel.drawHexagon(hexValue, true);

        var panelImage = panel.drawImage(SkiaImageUtil.getImage(NowbotConfig.BG_PATH + "ExportFileV3/overlay-ppplusv3.2.png")).build("PANEL-PPP dev.0.0.1");
        try (panelImage) {
            card.build().close();
            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(panelImage.encodeToData().getBytes()), from));
        }
        throw new LogException("结束pp+",null);
    }
}
