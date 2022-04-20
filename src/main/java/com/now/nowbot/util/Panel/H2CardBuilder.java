package com.now.nowbot.util.Panel;


import com.now.nowbot.model.match.UserMatchData;
import com.now.nowbot.util.SkiaImageUtil;
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
            canvas.drawRRect(RRect.makeXYWH(0,0,40,110,20,0,0,20),new Paint().setColor(usermatchdata.getRating().color));
        } else if(team.equals("BLUE")){

        }

        canvas.restore();
    }

    public void drawSpe(){
        canvas.save();
        canvas.translate(100,10);
        canvas.restore();
    }

    public void drawMvp(){

    }
}

