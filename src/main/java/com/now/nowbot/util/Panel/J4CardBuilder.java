package com.now.nowbot.util.Panel;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.util.SkiaImageUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;

import java.io.IOException;

public class J4CardBuilder extends PanelBuilder{

    public J4CardBuilder(OsuUser user){
        super(430, 335);

        drawBaseRRect();
        drawHexagon(user);
        drawLevelIndex(user);

    }
    private void drawBaseRRect(){
        //画底层圆角矩形
        canvas.save();
        canvas.drawRRect(RRect.makeXYWH(0, 0, 430, 355, 20), new Paint().setARGB(255, 56, 46, 50));
        canvas.restore();
    }

    private void drawHexagon(OsuUser user){
        //画六边形
        Image Hexagon = null;
        try {
            Hexagon = SkiaImageUtil.getImage("F://【osu! 文件大全】/【BOT相关】/【SP07】0163 YumuBot/s0706 Yumu Panel V3 Chocolate/ExportFileV3/object-hexagon-level.png");
        } catch (IOException e) {
            e.printStackTrace();
        }

        float Jmu1t = user.getLevelCurrent();
        float clipAngle = - 90 + (Jmu1t / 100) * 360;//裁剪角度

        //画路径的蒙版
        Path pathArc = new Path();
        Rect rectArc = new Rect(200,200,200,200);
        pathArc.addArc(rectArc,-90,clipAngle);

        canvas.save();

        if (Jmu1t >= 100){
            canvas.translate(140,20);
        } else {
            canvas.translate(115,0);
            canvas.clipPath(pathArc);
            canvas.translate(140 - 115,20);
        }

        canvas.drawImage(Hexagon,0,0,new Paint());
        canvas.restore();
    }

    private void drawLevelIndex(OsuUser user) {
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS24 = new Font(TorusSB, 24);
        Font fontS36 = new Font(TorusSB, 36);
        Font fontS60 = new Font(TorusSB, 60);
        Font fontS72 = new Font(TorusSB, 72);

        String Jlu1t = "Level";
        int Jru1t = 0; //进度
        char Jru2t = '%';

        int Jmu1t = 0; //等级
        String Jm1t = "SS";
        String Jm2t = "S";
        String Jm3t = "A";

        int Jmb1t = 0; //数量
        int Jmb2t = 0;
        int Jmb3t = 0;
        int Jb1t = 0; //数量（带HD）
        int Jb2t = 0;

        Jru1t = user.getLevelProgress();
        Jmu1t = user.getLevelCurrent();

        Jmb1t = user.getStatistics().getSS();
        Jmb2t = user.getStatistics().getS();
        Jmb3t = user.getStatistics().getA();
        Jb1t = user.getStatistics().getSSH();
        Jb2t = user.getStatistics().getSH();

        TextLine Jlu1 = TextLine.make(Jlu1t, fontS36);
        TextLine Jru1 = TextLine.make(String.valueOf(Jru1t), fontS36);
        TextLine Jru2 = TextLine.make(String.valueOf(Jru2t), fontS24);
        TextLine Jmu1 = TextLine.make(String.valueOf(Jmu1t), fontS72);
        TextLine Jm1 = TextLine.make(Jm1t, fontS60);
        TextLine Jm2 = TextLine.make(Jm2t, fontS60);
        TextLine Jm3 = TextLine.make(Jm3t, fontS60);
        TextLine Jmb1 = TextLine.make(String.valueOf(Jmb1t), fontS36);
        TextLine Jmb2 = TextLine.make(String.valueOf(Jmb2t), fontS36);
        TextLine Jmb3 = TextLine.make(String.valueOf(Jmb3t), fontS36);
        TextLine Jb1 = TextLine.make('(' + String.valueOf(Jb1t) + ')', fontS24);
        TextLine Jb2 = TextLine.make('(' + String.valueOf(Jb2t) + ')', fontS24);

        //画左上角 Level
        canvas.save();
        canvas.translate(20,20);
        canvas.drawTextLine(Jlu1,0,Jlu1.getHeight()-Jlu1.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.restore();

        //画右上角 Progress
        canvas.save();
        canvas.translate(430 - 20 - Jru2.getWidth(),20);
        canvas.drawTextLine(Jru2,0,Jru2.getHeight()-Jru2.getXHeight(),new Paint().setARGB(255,170,170,170));
        canvas.translate(- 10 - Jru1.getWidth(),20);
        canvas.drawTextLine(Jru1,0,Jru1.getHeight()-Jru1.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.restore();

        //画三个rank和正中的level，100以上变成金黄色
        canvas.save();
        canvas.translate(215 - Jmu1.getWidth() / 2,72);
        if (Jmu1t >= 100){
            canvas.drawTextLine(Jmu1,0,Jmu1.getHeight()-Jmu1.getXHeight(),new Paint().setARGB(255,254,246,104));
        } else {
            canvas.drawTextLine(Jmu1,0,Jmu1.getHeight()-Jmu1.getXHeight(),new Paint().setARGB(255,255,255,255));
        }
        canvas.restore();

        canvas.save();
        canvas.translate(70 - Jm1.getWidth() / 2,195);
        canvas.drawTextLine(Jm1,0,Jm1.getHeight()-Jm1.getXHeight(),new Paint().setARGB(255,254,246,104));
        canvas.translate(145 - (Jm2.getWidth() - Jm1.getWidth())/ 2,0);
        canvas.drawTextLine(Jm2,0,Jm2.getHeight()-Jm2.getXHeight(),new Paint().setARGB(255,240,148,80));
        canvas.translate(145 - (Jm3.getWidth() - Jm2.getWidth())/ 2,0);
        canvas.drawTextLine(Jm3,0,Jm3.getHeight()-Jm3.getXHeight(),new Paint().setARGB(255,124,198,35));
        canvas.restore();

        //画五个指标
        canvas.save();
        canvas.translate(70 - Jmb1.getWidth() / 2,195 + 60);
        canvas.drawTextLine(Jmb1,0,Jmb1.getHeight()-Jmb1.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.translate(145 - (Jmb2.getWidth() - Jmb1.getWidth())/ 2,0);
        canvas.drawTextLine(Jmb2,0,Jmb2.getHeight()-Jmb2.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.translate(145 - (Jmb3.getWidth() - Jmb2.getWidth())/ 2,0);
        canvas.drawTextLine(Jmb3,0,Jmb3.getHeight()-Jmb3.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.restore();

        canvas.translate(70 - Jb1.getWidth() / 2,195 + 100);
        canvas.drawTextLine(Jb1,0,Jb1.getHeight()-Jb1.getXHeight(),new Paint().setARGB(255,170,170,170));
        canvas.translate(145 - (Jb2.getWidth() - Jb1.getWidth())/ 2,0);
        canvas.drawTextLine(Jb2,0,Jb2.getHeight()-Jb2.getXHeight(),new Paint().setARGB(255,170,170,170));
        canvas.restore();
    }

    public Image build() {
        return super.build(20);
    }
}
