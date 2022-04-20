package com.now.nowbot.util.Panel;


import com.now.nowbot.model.match.UserMatchData;
import com.now.nowbot.util.SkiaImageUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;

import java.io.IOException;
import java.util.ArrayList;

public class H2CardBuilder extends PanelBuilder {
    public static final ArrayList<Float> F24L = new ArrayList<>();
    public static final ArrayList<Float> F36L = new ArrayList<>();

    public H2CardBuilder(UserMatchData usermatchdata, int playerN) throws IOException {
        super(900,110);

        //画底层圆角矩形
        canvas.save();
        canvas.drawRRect(RRect.makeXYWH(0,0,900,110,20),new Paint().setARGB(255,56,46,50));
        canvas.restore();

        //画玩家评价圆角矩形
        var colorA = usermatchdata.getRating().color; //getJudge
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
        Image H2CardLightBG = SkiaImageUtil.getImage();// 这里获取玩家头像吧，但是如果有玩家卡片或者玩家主页背景图更好
        Image H2CardLightBGSC = SkiaImageUtil.getScaleCenterImage(H2CardLightBG,620,140); //缩放至合适大小，这里放大了一点，以应对模糊带来的负面效果
        canvas.drawImage(H2CardLightBGSC,0, 0,new Paint().setAlphaf(0.2f).setImageFilter(ImageFilter.makeBlur(5, 5, FilterTileMode.REPEAT)));
        canvas.restore();


        //画队伍指示矩形
        canvas.save();
        var team = usermatchdata.getTeam().toUpperCase();
        if (team.equals("RED")) {
            canvas.drawRRect(RRect.makeXYWH(0,0,40,110,20,0,0,20),new Paint().setARGB(255,227,0,123));
        } else if(team.equals("BLUE")){
            canvas.drawRRect(RRect.makeXYWH(0,0,40,110,20,0,0,20),new Paint().setARGB(255,0,168,236));
        }

        canvas.restore();
    }

    // MRA V3.1 新增部分
    public void drawUserRatingIndex(UserMatchData.Rating Rating){
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

    public void drawUserRatingMVP(){
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS24 = new Font(TorusSB, 24);

        TextLine MVP = TextLine.make("MVP",fontS24);

        canvas.save();
        canvas.translate(700 - 40 - MVP.getWidth(),54);
        canvas.drawRRect(RRect.makeXYWH(0,0,40 + MVP.getWidth(),34,17),new Paint().setARGB(255,56,46,50)); // 画评价背景矩形
        canvas.translate(20,8);
        canvas.drawTextLine(MVP,0,MVP.getHeight()-MVP.getXHeight(),new Paint().setARGB(255,255,255,255));// 画评价字
        canvas.restore();
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