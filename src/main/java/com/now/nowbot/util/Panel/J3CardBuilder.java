package com.now.nowbot.util.Panel;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.MessageService.PPmService;
import com.now.nowbot.util.SkiaImageUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public class J3CardBuilder extends PanelBuilder{

    public J3CardBuilder(OsuUser user,OsuMode mode) throws IOException {
        super(430, 355);

        drawBaseRRect();
        drawHexagon();
        drawHexagramGraph();
        drawPPMIndex(user,mode);
    }

    private void drawBaseRRect(){
        //画底层圆角矩形
        canvas.save();
        canvas.drawRRect(RRect.makeXYWH(0, 0, 430, 355, 20), new Paint().setARGB(255, 56, 46, 50));
        canvas.restore();
    }

    private void drawHexagon(){
        //画六边形
        Image Hexagon = null;
        try {
            Hexagon = SkiaImageUtil.getImage("F://【osu! 文件大全】/【BOT相关】/【SP07】0163 YumuBot/s0706 Yumu Panel V3 Chocolate/ExportFileV3/object-hexagon-mini.png");
        } catch (IOException e) {
            e.printStackTrace();
        }
        canvas.save();
        canvas.translate(100,100);
        canvas.drawImage(Hexagon,0,0,new Paint());
        canvas.restore();
    }

    private void drawHexagramGraph(){
        //画折线图，这层需要在object-hexagon-mini.png层上面
    }


    @Autowired
    PPmService PPmService;

    private void drawPPMIndex(OsuUser user, OsuMode mode) {

        //画指标
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS24 = new Font(TorusSB, 24);
        Font fontS36 = new Font(TorusSB, 36);

        String Jlu1t = "PP+";
        String Jru1t = "San";

        //六个指标，从正左开始顺时针排下去
        double J1t = 0D;
        double J2t = 0D;
        double J3t = 0D;
        double J4t = 0D;
        double J5t = 0D;
        double J6t = 0D;
        double Jru2t = 0D; //san

        J1t = SkiaUtil.getRoundedNumber(J1t,2);
        J2t = SkiaUtil.getRoundedNumber(J2t,2);
        J3t = SkiaUtil.getRoundedNumber(J3t,2);
        J4t = SkiaUtil.getRoundedNumber(J4t,2);
        J5t = SkiaUtil.getRoundedNumber(J5t,2);
        J6t = SkiaUtil.getRoundedNumber(J6t,2);

        String J1it = "accuracy";
        String J2it = "potential";
        String J3it = "stamina";
        String J4it = "stability";
        String J4it2 = "precision";
        String J5it = "energy";
        String J6it = "strength";

        TextLine Jlu1 = TextLine.make(Jlu1t, fontS36);
        TextLine Jru1 = TextLine.make(Jru1t, fontS24);
        TextLine Jru2 = TextLine.make(String.valueOf(Jru2t), fontS36);

        TextLine J1 = TextLine.make(String.valueOf(J1t), fontS36);
        TextLine J2 = TextLine.make(String.valueOf(J2t), fontS36);
        TextLine J3 = TextLine.make(String.valueOf(J3t), fontS36);
        TextLine J4 = TextLine.make(String.valueOf(J4t), fontS36);
        TextLine J5 = TextLine.make(String.valueOf(J5t), fontS36);
        TextLine J6 = TextLine.make(String.valueOf(J6t), fontS36);

        if (mode == OsuMode.MANIA) { //PRE STB互换
            J4it = J4it2;
        }

        TextLine J1i = TextLine.make(J1it, fontS24);
        TextLine J2i = TextLine.make(J2it, fontS24);
        TextLine J3i = TextLine.make(J3it, fontS24);
        TextLine J4i = TextLine.make(J4it, fontS24);
        TextLine J5i = TextLine.make(J5it, fontS24);
        TextLine J6i = TextLine.make(J6it, fontS24);

        //画左上角 PPM
        canvas.save();
        canvas.translate(20,20);
        canvas.drawTextLine(Jlu1,0,Jlu1.getHeight()-Jlu1.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.restore();

        //画右上角 San
        canvas.save();
        canvas.translate(430 - 20 - Jru2.getWidth(),20);
        canvas.drawTextLine(Jru2,0,Jru2.getHeight()-Jru2.getXHeight(),new Paint().setARGB(255,170,170,170));
        canvas.translate(- 10 - Jru1.getWidth(),20);
        canvas.drawTextLine(Jru1,0,Jru1.getHeight()-Jru1.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.restore();

        //画六个指标数字
        canvas.save();
        canvas.translate(85 - J1.getWidth() / 2,70);
        canvas.drawTextLine(J1,0,J1.getHeight()-J1.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.translate(260 + (J1.getWidth() - J2.getWidth()) / 2,0);
        canvas.drawTextLine(J2,0,J2.getHeight()-J2.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.translate(-290 + (J2.getWidth() - J3.getWidth()) / 2,100);
        canvas.drawTextLine(J3,0,J3.getHeight()-J3.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.translate(320 + (J3.getWidth() - J4.getWidth()) / 2,0);
        canvas.drawTextLine(J4,0,J4.getHeight()-J4.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.translate(-290 + (J4.getWidth() - J5.getWidth()) / 2,100);
        canvas.drawTextLine(J5,0,J5.getHeight()-J5.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.translate(260 + (J5.getWidth() - J6.getWidth()) / 2,0);
        canvas.drawTextLine(J6,0,J6.getHeight()-J6.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.restore();
        //画六个指标名称
        canvas.save();
        canvas.translate(85 - J1i.getWidth() / 2,70 + 32);//向下偏移 32 px 即可绘制小字部分
        canvas.drawTextLine(J1i,0,J1i.getHeight()-J1i.getXHeight(),new Paint().setARGB(255,170,170,170));
        canvas.translate(260 + (J1i.getWidth() - J2i.getWidth()) / 2,0);
        canvas.drawTextLine(J2i,0,J2i.getHeight()-J2i.getXHeight(),new Paint().setARGB(255,170,170,170));
        canvas.translate(-290 + (J2i.getWidth() - J3i.getWidth()) / 2,100);
        canvas.drawTextLine(J3i,0,J3i.getHeight()-J3i.getXHeight(),new Paint().setARGB(255,170,170,170));
        canvas.translate(320 + (J3i.getWidth() - J4i.getWidth()) / 2,0);
        canvas.drawTextLine(J4,0,J4i.getHeight()-J4i.getXHeight(),new Paint().setARGB(255,170,170,170));
        canvas.translate(-290 + (J4i.getWidth() - J5i.getWidth()) / 2,100);
        canvas.drawTextLine(J5i,0,J5i.getHeight()-J5i.getXHeight(),new Paint().setARGB(255,170,170,170));
        canvas.translate(260 + (J5i.getWidth() - J6i.getWidth()) / 2,0);
        canvas.drawTextLine(J6i,0,J6i.getHeight()-J6i.getXHeight(),new Paint().setARGB(255,170,170,170));
        canvas.restore();
    }

    public Image build() {
        return super.build(20);
    }
}
