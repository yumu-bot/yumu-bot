package com.now.nowbot.service.msgServiceImpl;

import com.now.nowbot.entity.BinUser;
import com.now.nowbot.entity.PPmObject;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;

@Service
public class ppmServiceImpl extends MessageService{
    public ppmServiceImpl() {
        super("ppm");
    }
    @Autowired
    OsuGetService osuGetService;

    @Override
    public void handleMsg(MessageEvent event) {
        Contact from;
        if(event instanceof GroupMessageEvent) {
            from = ((GroupMessageEvent) event).getGroup();
        }else {
            from = event.getSender();
        }

        BinUser user = BindingUtil.readUser(event.getSender().getId());
        if (user == null){
            from.sendMessage("您未绑定，请绑定后使用");
        }
        PPmObject userinfo;
        {
            var userdate = osuGetService.getPlayerOsuInfo(user);
            var bpdate = osuGetService.getOsuBestMap(user, 0, 100);
            userinfo = PPmObject.presOsu(userdate, bpdate);
        }

        try(Surface surface = Surface.makeRasterN32Premul(600,830);
            Font smileFont = new Font(FriendServiceImpl.face,20);
            Font lagerFont = new Font(FriendServiceImpl.face,50);
            Font middleFont = new Font(FriendServiceImpl.face, 30);
            Paint bg1 = new Paint().setARGB(40,0,0,0);
            Paint bg2 = new Paint().setARGB(220,0,0,0);
            Paint wp = new Paint().setARGB(255,200,200,200);
            Paint edP = new Paint().setARGB(200,0,0,0)){

            var canvas = surface.getCanvas();
            canvas.clear(Color.makeRGB(65, 40, 49));

            var line = TextLine.make(userinfo.getName(),lagerFont);
            canvas.drawTextLine(line,(600-line.getWidth())/2,line.getHeight(),new Paint().setARGB(255,255,255,255));

            canvas.save();
            canvas.translate(300,325);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f), bg1);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f), bg1);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 0.75f, 0.75f, 0.75f, 0.75f, 0.75f, 0.75f), bg1);
            canvas.drawPath(SkiaUtil.creat6(250, 0, 1, 1, 1, 1, 1, 1), bg1);

            Path pt = SkiaUtil.creat6(250, 3,(float) userinfo.getPtt(),(float) userinfo.getSta(),(float) userinfo.getStb(),(float) userinfo.getSth(),(float) userinfo.getEng(),(float) userinfo.getFa());

            canvas.drawPath(pt, new Paint().setStrokeWidth(3).setStroke(true).setARGB(255,240,167,50));
            canvas.drawPath(pt, new Paint().setStrokeWidth(3).setStroke(false).setARGB(80, 240, 167, 50));

            canvas.drawRRect(RRect.makeXYWH(-150,-226.5f,60,25,5),bg2);
            canvas.drawString("成长值",-144,-208f,smileFont,wp);

            canvas.drawRRect(RRect.makeXYWH(100,-226.5f,60,25,5),bg2);
            canvas.drawString("持久力",108,-208.5f,smileFont,wp);

            canvas.drawRRect(RRect.makeXYWH(230,-10,50,25,5),bg2);
            canvas.drawString("稳定性",239,7,smileFont,wp);

            canvas.drawRRect(RRect.makeXYWH(105,206.5f,50,25,5),bg2);
            canvas.drawString("实力",114,223.5f,smileFont,wp);

            canvas.drawRRect(RRect.makeXYWH(-145,206.5f,50,25,5),bg2);
            canvas.drawString("努力值",-137,223.5f,smileFont,wp);

            canvas.drawRRect(RRect.makeXYWH(-270,-10,50,25,5),bg2);
            canvas.drawString("精准度",-261,7f,smileFont,wp);

            canvas.restore();
            canvas.translate(0,575);

            DecimalFormat dx = new DecimalFormat("0.00");
            canvas.drawRRect(RRect.makeXYWH(50,0,225,50,10),edP);
            canvas.drawString("成长值:"+dx.format(userinfo.getPtt()*100),60,35,middleFont,wp);
            canvas.drawRRect(RRect.makeXYWH(325,0,225,50,10),edP);
            canvas.drawString("持久力:"+dx.format(userinfo.getStb()*100),335,35,middleFont,wp);

            canvas.drawRRect(RRect.makeXYWH(50,75,225,50,10),edP);
            canvas.drawString("稳定性:"+dx.format(userinfo.getStb()*100),60,110,middleFont,wp);
            canvas.drawRRect(RRect.makeXYWH(325,75,225,50,10),edP);
            canvas.drawString("实力:"+dx.format(userinfo.getFa()*100),335,110,middleFont,wp);

            canvas.drawRRect(RRect.makeXYWH(50,150,225,50,10),edP);
            canvas.drawString("努力值:"+dx.format(userinfo.getSth()*100),60,185,middleFont,wp);
            canvas.drawRRect(RRect.makeXYWH(325,150,225,50,10),edP);
            canvas.drawString("精准度:"+dx.format(userinfo.getEng()*100),335,185,middleFont,wp);

            var fromx = TextLine.make("感谢Muziyami提供计算方法",smileFont);
            canvas.drawTextLine(fromx,(600-line.getWidth())/2,surface.getHeight()-line.getHeight(),new Paint().setARGB(255,255,255,255));

            var datebyte = surface.makeImageSnapshot().encodeToData().getBytes();
            from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(datebyte),from));

        }

//        StringBuffer sb = new StringBuffer();
//        DecimalFormat dx = new DecimalFormat("0.00");
//        sb.append("计算结果：").append('\n')
//                .append("fACC ").append(dx.format(userinfo.getFa()*100)).append('\n')
//                .append("PTT ").append(dx.format(userinfo.getPtt()*100)).append('\n')
//                .append("STA ").append(dx.format(userinfo.getSta()*100)).append('\n')
//                .append("STB ").append(dx.format(userinfo.getStb()*100)).append('\n')
//                .append("ENG ").append(dx.format(userinfo.getEng()*100)).append('\n')
//                .append("STH ").append(dx.format(userinfo.getSth()*100));
//        from.sendMessage(sb.toString());
    }
}
