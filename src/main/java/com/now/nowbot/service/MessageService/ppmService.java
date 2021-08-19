package com.now.nowbot.service.MessageService;

import com.now.nowbot.entity.PPmObject;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.service.msgServiceImpl.FriendServiceImpl;
import com.now.nowbot.util.BindingUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.PokeMessage;
import net.mamoe.mirai.message.data.QuoteReply;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("ppm")
public class ppmService extends MsgSTemp implements MessageService {
    @Autowired
    OsuGetService osuGetService;

    ppmService() {
        super(Pattern.compile("(?i)ppm\\s+((?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))?"));
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        At at = (At) event.getMessage().stream().filter(it -> it instanceof At).findFirst().orElse(null);
        PPmObject userinfo;


        if (at != null) {
            var user = BindingUtil.readUser(at.getTarget());
            if (user == null) {
                from.sendMessage(new QuoteReply(event.getMessage()).plus("该用户未绑定!"));
                return;
            }
            var userdate = osuGetService.getPlayerOsuInfo(user);
            var bpdate = osuGetService.getOsuBestMap(user, 0, 100);
            userinfo = PPmObject.presOsu(userdate, bpdate);
        } else {
            if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
                int id = osuGetService.getOsuId(matcher.group("name").trim());
                var userdate = osuGetService.getPlayerOsuInfo(id);
                var bpdate = osuGetService.getOsuBestMap(id, 0, 100);
                userinfo = PPmObject.presOsu(userdate, bpdate);
            }else {
                var user = BindingUtil.readUser(event.getSender().getId());
                if (user == null) {
                    from.sendMessage(PokeMessage.ChuoYiChuo);
                    from.sendMessage("您未绑定，请绑定后使用");
                    return;
                }
                var userdate = osuGetService.getPlayerOsuInfo(user);
                var bpdate = osuGetService.getOsuBestMap(user, 0, 100);
                userinfo = PPmObject.presOsu(userdate, bpdate);
            }
        }

        try(Surface surface = Surface.makeRasterN32Premul(600,830);
            Font smileFont = new Font(FriendServiceImpl.face,20);
            Font lagerFont = new Font(FriendServiceImpl.face,50);
            Font middleFont = new Font(FriendServiceImpl.face, 30);
            Paint bg1 = new Paint().setARGB(40,0,0,0);
            Paint bg2 = new Paint().setARGB(220,0,0,0);
            Paint wp = new Paint().setARGB(255,200,200,200);
            Paint edP = new Paint().setARGB(200,0,0,0)) {

            var canvas = surface.getCanvas();
            canvas.clear(Color.makeRGB(65, 40, 49));

            var line = TextLine.make(userinfo.getName(), lagerFont);
            canvas.drawTextLine(line, (600 - line.getWidth()) / 2, line.getHeight(), new Paint().setARGB(255, 255, 255, 255));

            canvas.save();
            canvas.translate(300, 325);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f), bg1);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f), bg1);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.75f, 0.75f, 0.75f, 0.75f, 0.75f, 0.75f), bg1);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 1, 1, 1, 1, 1, 1), bg1);


            Path pt = SkiaUtil.creat6(250, 3,
                    (float) Math.pow((userinfo.getPtt() < 0.6 ? 0.01f : userinfo.getPtt() - 0.6) * 2.5f, 0.8),
                    (float) Math.pow((userinfo.getSta() < 0.6 ? 0.01f : userinfo.getSta() - 0.6) * 2.5f, 0.8),
                    (float) Math.pow((userinfo.getStb() < 0.6 ? 0.01f : userinfo.getStb() - 0.6) * 2.5f, 0.8),
                    (float) Math.pow((userinfo.getSth() < 0.6 ? 0.01f : userinfo.getSth() - 0.6) * 2.5f, 0.8),
                    (float) Math.pow((userinfo.getEng() < 0.6 ? 0.01f : userinfo.getEng() - 0.6) * 2.5f, 0.8),
                    (float) Math.pow((userinfo.getFa() < 0.6 ? 0.01f : userinfo.getFa() - 0.6) * 2.5f, 0.8));


            canvas.drawPath(pt, new Paint().setStrokeWidth(3).setStroke(true).setARGB(255, 240, 167, 50));
            canvas.drawPath(pt, new Paint().setStrokeWidth(3).setStroke(false).setARGB(80, 240, 167, 50));

            canvas.drawRRect(RRect.makeXYWH(-150, -226.5f, 60, 25, 5), bg2);
            canvas.drawString("ptt", -144, -208f, smileFont, wp);

            canvas.drawRRect(RRect.makeXYWH(100, -226.5f, 60, 25, 5), bg2);
            canvas.drawString("sta", 108, -208.5f, smileFont, wp);

            canvas.drawRRect(RRect.makeXYWH(230, -10, 50, 25, 5), bg2);
            canvas.drawString("stb", 240, 8, smileFont, wp);

            canvas.drawRRect(RRect.makeXYWH(105, 206.5f, 50, 25, 5), bg2);
            canvas.drawString("sth", 114, 224.5f, smileFont, wp);

            canvas.drawRRect(RRect.makeXYWH(-145, 206.5f, 50, 25, 5), bg2);
            canvas.drawString("eng", -139, 223.5f, smileFont, wp);

            canvas.drawRRect(RRect.makeXYWH(-270, -10, 50, 25, 5), bg2);
            canvas.drawString("acc", -260, 8f, smileFont, wp);

            canvas.restore();
            canvas.translate(0, 575);

            DecimalFormat dx = new DecimalFormat("0.00");
            canvas.drawRRect(RRect.makeXYWH(50, 0, 225, 50, 10), edP);
            canvas.drawString("PTT:" + dx.format(userinfo.getPtt() * 100), 60, 35, middleFont, wp);
            canvas.drawRRect(RRect.makeXYWH(325, 0, 225, 50, 10), edP);
            canvas.drawString("STA:" + dx.format(userinfo.getSta() * 100), 335, 35, middleFont, wp);

            canvas.drawRRect(RRect.makeXYWH(50, 75, 225, 50, 10), edP);
            canvas.drawString("STB:" + dx.format(userinfo.getStb() * 100), 60, 110, middleFont, wp);
            canvas.drawRRect(RRect.makeXYWH(325, 75, 225, 50, 10), edP);
            canvas.drawString("ACC:" + dx.format(userinfo.getFa() * 100), 335, 110, middleFont, wp);

            canvas.drawRRect(RRect.makeXYWH(50, 150, 225, 50, 10), edP);
            canvas.drawString("STH:" + dx.format(userinfo.getSth() * 100), 60, 185, middleFont, wp);
            canvas.drawRRect(RRect.makeXYWH(325, 150, 225, 50, 10), edP);
            canvas.drawString("ENG:" + dx.format(userinfo.getEng() * 100), 335, 185, middleFont, wp);

//            var fromx = TextLine.make("Thanks for Muziyami",smileFont);
//            canvas.drawTextLine(fromx,(600-line.getWidth())/2,surface.getHeight()-2*line.getHeight(),new Paint().setARGB(255,255,255,255));

            var datebyte = surface.makeImageSnapshot().encodeToData().getBytes();
            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(datebyte), from));
        }
    }
}
