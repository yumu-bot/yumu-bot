package com.now.nowbot.util.Panel;


import com.now.nowbot.model.match.UserMatchData;
import com.now.nowbot.util.SkiaImageUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;

import java.io.IOException;
import java.util.Objects;

public class H2CardBuilder extends PanelBuilder {

    public H2CardBuilder(UserMatchData usermatchdata, int index) {
        super(900,110);

        //画底层圆角矩形
        canvas.save();
        canvas.drawRRect(RRect.makeXYWH(0,0,900,110,20),new Paint().setARGB(255,56,46,50));
        canvas.restore();

        //画玩家评价圆角矩形
        canvas.save();
        canvas.translate(700,0);
        var colorA = usermatchdata.getPlayerLabelV2().color; //getJudge
        var colorB = Color.makeRGB(Color.getR(colorA)-3, Color.getG(colorA)+5, Color.getB(colorA)-3);
        canvas.drawRRect(
                RRect.makeXYWH(0,0,200,110,0,20,20,0),
                new Paint().setShader(
                        Shader.makeLinearGradient(0,0,110,110,new int[]{colorA, colorB})
                )
        );
        canvas.restore();

        //画中间层背景图
        canvas.save();
        canvas.translate(160,0);
        canvas.drawRRect(RRect.makeXYWH(0,0,570,110,20),new Paint().setARGB(255,56,46,50));
        canvas.clipRRect(RRect.makeXYWH(0,0,570,110,20));
        Image H2CardLightBG = null;   // 这里获取玩家头像吧，但是如果有玩家卡片或者玩家主页背景图(usermatchdata.getCoverUrl();)更好
        try {
            H2CardLightBG = SkiaImageUtil.getImage(usermatchdata.getCoverUrl());
        } catch (IOException e) {
            throw new RuntimeException(" get cover image error ");
        }
        Image H2CardLightBGSC = SkiaImageUtil.getScaleCenterImage(H2CardLightBG,620,140); //缩放至合适大小，这里放大了一点，以应对模糊带来的负面效果
        canvas.drawImage(H2CardLightBGSC,0, 0,new Paint().setAlphaf(0.2f).setImageFilter(ImageFilter.makeBlur(5, 5, FilterTileMode.REPEAT)));
        canvas.restore();


        //画队伍指示矩形
        canvas.save();
        var team = usermatchdata.getTeam().toUpperCase();
        if (team.equals("RED")) {
            canvas.drawRRect(RRect.makeXYWH(0,0,196,110,20),new Paint().setARGB(255,223,0,36));
        } else if(team.equals("BLUE")){
            canvas.drawRRect(RRect.makeXYWH(0,0,196,110,20),new Paint().setARGB(255,0,168,236));
        } else {
            canvas.drawRRect(RRect.makeXYWH(0,0,196,110,20),new Paint().setColor(SkiaUtil.hexToRGBInt("#a1a1a1")));
        }
        canvas.restore();

        //画主要缩略图
        canvas.save();
        canvas.translate(20,0);
        canvas.clipRRect(RRect.makeXYWH(0,0,176,110,20));
        Image H2CardMainBG = null;
        try {
            H2CardMainBG = SkiaImageUtil.getImage(usermatchdata.getHeaderUrl());
        } catch (IOException e) {
            H2CardMainBG = H2CardLightBG;
        }
        Image H2CardMainBGSC = SkiaImageUtil.getScaleCenterImage(H2CardMainBG,176,110); //缩放至合适大小
        canvas.drawImage(H2CardMainBGSC,0, 0,new Paint());
        canvas.restore();

        // 调用以下方法
        drawUserRatingIndex(usermatchdata.getPlayerLabelV2());
        drawText(usermatchdata, index);
        //drawText();
    }

    private void drawText (UserMatchData usermatchdata, int usermatchrank){
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS24 = new Font(TorusSB, 24);
        Font fontS36 = new Font(TorusSB, 36);
        Font fontS48 = new Font(TorusSB, 48);

        /*
        Muziyami
        39.2M // 99W-0L 100%
        #1 (39.46)
         */

        var MRA2f = (int)((usermatchdata.getMRA() - usermatchdata.getMRA().intValue()) * 100);

        TextLine h1 = TextLine.make(usermatchdata.getUsername(),fontS36);
        TextLine h2 = TextLine.make(String.format("%.2fM // %dW-%dL %d%%", usermatchdata.getTotalScore(),usermatchdata.getWins(),usermatchdata.getLost(),
                Math.round((double) usermatchdata.getWins() * 100 / (usermatchdata.getWins() + usermatchdata.getLost()))),fontS24);
        TextLine h3;

        // 暴力过滤掉 0.00
        if (usermatchdata.getRWS() == 0f) {
            h3 = TextLine.make(String.format("#%d (0)", usermatchrank),fontS24);
        } else  h3 = TextLine.make(String.format("#%d (%.2f)", usermatchrank, usermatchdata.getRWS() * 100f),fontS24);

        TextLine h4l = TextLine.make(String.format("%d.", usermatchdata.getMRA().intValue()),fontS48);

        //我暴力if了，用%2f能解决但我不会，看到了记得帮我简化一下
        TextLine h4r;
        if (MRA2f < 10d){
            h4r = TextLine.make(String.format("0%d", MRA2f), fontS36);

        } else {
            h4r = TextLine.make(String.format("%d", MRA2f), fontS36);
        }
        canvas.save();
        canvas.translate(210,10);
        canvas.drawTextLine(h1,0,h1.getHeight()-h1.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.translate(0,40);
        canvas.drawTextLine(h2,0,h2.getHeight()-h2.getXHeight(),new Paint().setARGB(255,170,170,170));
        canvas.translate(0,30);
        canvas.drawTextLine(h3,0,h3.getHeight()-h3.getXHeight(),new Paint().setARGB(255,170,170,170));
        canvas.translate(520,-40);
        canvas.translate((170 - h4l.getWidth() - h4r.getWidth()) / 2f,0);//居中处理，170减大减小除以2

        if (Objects.equals(usermatchdata.getPlayerLabelV2().name, "Strongest Marshal") ||
                Objects.equals(usermatchdata.getPlayerLabelV2().name, "Competent Marshal") ||
                Objects.equals(usermatchdata.getPlayerLabelV2().name, "Indomitable Marshal")) {
        canvas.drawTextLine(h4l, 0, h4l.getHeight() - h4l.getXHeight(), new Paint().setARGB(255, 43,34,39));
        canvas.translate(h4l.getWidth(), 0);
        canvas.drawTextLine(h4r, 0, h4l.getHeight() - h4l.getXHeight(), new Paint().setARGB(255, 43,34,39));
        } else {
        canvas.drawTextLine(h4l,0,h4l.getHeight()-h4l.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.translate(h4l.getWidth(),0);
        canvas.drawTextLine(h4r,0,h4l.getHeight()-h4l.getXHeight(),new Paint().setARGB(255,255,255,255));
    }
        canvas.restore();
    }

    // MRA V3.1 新增部分
    private void drawUserRatingIndex(UserMatchData.Rating Rating){
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS24 = new Font(TorusSB, 24);

        TextLine RatingName = TextLine.make(Rating.name,fontS24);

        canvas.save();
        canvas.translate(700 - 40 - RatingName.getWidth(),10);
        canvas.drawRRect(RRect.makeXYWH(0,0,40 + RatingName.getWidth(),34,17),new Paint().setARGB(255,56,46,50)); // 画评价背景矩形
        canvas.translate(20,8);
        canvas.drawTextLine(RatingName,0,RatingName.getHeight()-RatingName.getXHeight(),new Paint().setARGB(255,255,255,255));// 画评价字
        canvas.restore();
    }

    public H2CardBuilder drawUserRatingMVP(){
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS24 = new Font(TorusSB, 24);

        TextLine MVP = TextLine.make("MVP",fontS24);

        canvas.save();
        canvas.translate(700 - 40 - MVP.getWidth(),54);
        canvas.drawRRect(RRect.makeXYWH(0,0,40 + MVP.getWidth(),34,17),new Paint().setARGB(255,56,46,50)); // 画评价背景矩形
        canvas.translate(20,8);
        canvas.drawTextLine(MVP,0,MVP.getHeight()-MVP.getXHeight(),new Paint().setARGB(255,255,255,255));// 画评价字
        canvas.restore();
        return this;
    }

    public Image build() {
        return super.build(20);
    }
}
/*
        try{
            Files.write(Path.of("D:/output.png"),
            surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG).getBytes()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

 */