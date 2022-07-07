package com.now.nowbot.util.Panel;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;

public class J5CardBuilder extends PanelBuilder {
    public J5CardBuilder(OsuUser user) {
        super(900, 335);

        drawBaseRRect();
        drawUserIndex();
        drawUserData(user);
    }

    private void drawBaseRRect(){
        canvas.clear(Color.makeRGB(56, 46, 50));
    }

    private void drawUserIndex() {
        //画数据指标
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS24 = new Font(TorusSB, 24);
        Font fontS36 = new Font(TorusSB, 36);

        String Jlu1t = "Data";
        String Jll1t = "R.Score"; // Ranked Score
        String Jll2t = "T.Score"; // Total Score
        String Jll3t = "PC"; // PlayCount
        String Jll4t = "PT"; // PlayTime

        String Jr1t = "Map.PC"; // Beatmap Playcount
        String Jr2t = "Rep.WC"; // Replay Watched by others count
        String Jr3t = "Fans"; // Follower
        String Jr4t = "Map.Fans"; // Map Follower

        TextLine Jlu1 = TextLine.make(Jlu1t, fontS36);
        TextLine Jll1 = TextLine.make(Jll1t, fontS36);
        TextLine Jll2 = TextLine.make(Jll2t, fontS36);
        TextLine Jll3 = TextLine.make(Jll3t, fontS36);
        TextLine Jll4 = TextLine.make(Jll4t, fontS36);
        TextLine Jr1 = TextLine.make(Jr1t, fontS36);
        TextLine Jr2 = TextLine.make(Jr2t, fontS36);
        TextLine Jr3 = TextLine.make(Jr3t, fontS36);
        TextLine Jr4 = TextLine.make(Jr4t, fontS36);

        //画左上角 Data
        canvas.save();
        canvas.translate(20,20);
        canvas.drawTextLine(Jlu1,0,Jlu1.getHeight()-Jlu1.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.restore();

        var color_170_170_170 = new Paint().setARGB(255,170,170,170);
        //画指标
        canvas.save();
        canvas.translate(20,75);
        canvas.drawTextLine(Jll1,0,Jll1.getHeight()-Jll1.getXHeight(),color_170_170_170);
        canvas.translate(0,60);
        canvas.drawTextLine(Jll2,0,Jll2.getHeight()-Jll2.getXHeight(),color_170_170_170);
        canvas.translate(0,60);
        canvas.drawTextLine(Jll3,0,Jll3.getHeight()-Jll3.getXHeight(),color_170_170_170);
        canvas.translate(0,60);
        canvas.drawTextLine(Jll4,0,Jll4.getHeight()-Jll4.getXHeight(),color_170_170_170);
        canvas.restore();

        canvas.save();
        canvas.translate(470,75);
        canvas.drawTextLine(Jr1,0,Jr1.getHeight()-Jr1.getXHeight(),color_170_170_170);
        canvas.translate(0,60);
        canvas.drawTextLine(Jr2,0,Jr2.getHeight()-Jr2.getXHeight(),color_170_170_170);
        canvas.translate(0,60);
        canvas.drawTextLine(Jr3,0,Jr3.getHeight()-Jr3.getXHeight(),color_170_170_170);
        canvas.translate(0,60);
        canvas.drawTextLine(Jr4,0,Jr4.getHeight()-Jr4.getXHeight(),color_170_170_170);
        canvas.restore();
    }

    private void drawUserData(OsuUser user) {
        //画用户个人数据
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS24 = new Font(TorusSB, 24);
        Font fontS36 = new Font(TorusSB, 36);

        String Jl1t = DataUtil.getRoundedNumberStr(user.getStatistics().getRankedScore(), 2);
        String Jl2t = DataUtil.getRoundedNumberStr(user.getStatistics().getTotalScore(), 2);
        String Jl3t = DataUtil.getRoundedNumberStr(user.getStatistics().getPlayCount(), 2);
        String Jl4t = DataUtil.getRoundedNumberStr(user.getStatistics().getPlayTime(), 2);

        String Jrr1t = DataUtil.getRoundedNumberStr(user.getBeatmapSetCountPlaycounts(), 2);
        String Jrr2t = DataUtil.getRoundedNumberStr(user.getStatistics().getReplaysWatchedByOthers(), 2);
        String Jrr3t = DataUtil.getRoundedNumberStr(user.getFollowerCount(), 2);
        String Jrr4t = DataUtil.getRoundedNumberStr(user.getMappingFollowerCount(), 2);

/*
        String Jml1t = "0"; //这里是以上值与上次查询的变化值，需要数据库一类的东西，我不会写  //好像还没写
        String Jml2t = "0";
        String Jml3t = "0";
        String Jml4t = "0";

        String Jmr1t = "0";
        String Jmr2t = "0";
        String Jmr3t = "0";
        String Jmr4t = "0";
*/
        
        TextLine Jl1 = TextLine.make(Jl1t, fontS36);
        TextLine Jl2 = TextLine.make(Jl2t, fontS36);
        TextLine Jl3 = TextLine.make(Jl3t, fontS36);
        TextLine Jl4 = TextLine.make(Jl4t, fontS36);
        TextLine Jrr1 = TextLine.make(Jrr1t, fontS36);
        TextLine Jrr2 = TextLine.make(Jrr2t, fontS36);
        TextLine Jrr3 = TextLine.make(Jrr3t, fontS36);
        TextLine Jrr4 = TextLine.make(Jrr4t, fontS36);
/*

        TextLine Jml1 = TextLine.make(getNumberSign(Jml1t) + String.valueOf(Jml1t) + DataUtil.getRoundedNumberUnit(Jml1t, 2), fontS24);
        TextLine Jml2 = TextLine.make(getNumberSign(Jml2t) + String.valueOf(Jml2t) + DataUtil.getRoundedNumberUnit(Jml2t, 2), fontS24);
        TextLine Jml3 = TextLine.make(getNumberSign(Jml3t) + String.valueOf(Jml3t) + DataUtil.getRoundedNumberUnit(Jml3t, 2), fontS24);
        TextLine Jml4 = TextLine.make(getNumberSign(Jml4t) + String.valueOf(Jml4t) + DataUtil.getRoundedNumberUnit(Jml4t, 2), fontS24);
        TextLine Jmr1 = TextLine.make(getNumberSign(Jmr1t) + String.valueOf(Jmr1t) + DataUtil.getRoundedNumberUnit(Jmr1t, 2), fontS24);
        TextLine Jmr2 = TextLine.make(getNumberSign(Jmr2t) + String.valueOf(Jmr2t) + DataUtil.getRoundedNumberUnit(Jmr2t, 2), fontS24);
        TextLine Jmr3 = TextLine.make(getNumberSign(Jmr3t) + String.valueOf(Jmr3t) + DataUtil.getRoundedNumberUnit(Jmr3t, 2), fontS24);
        TextLine Jmr4 = TextLine.make(getNumberSign(Jmr4t) + String.valueOf(Jmr4t) + DataUtil.getRoundedNumberUnit(Jmr4t, 2), fontS24);
*/

        var color_170_170_170 = new Paint().setARGB(255,170,170,170);
        canvas.save();
        canvas.translate(450 - Jl1.getWidth(),75);
        canvas.drawTextLine(Jl1,0,Jl1.getHeight()-Jl1.getXHeight(),color_170_170_170);
        canvas.translate(Jl1.getWidth() - Jl2.getWidth(),60);
        canvas.drawTextLine(Jl2,0,Jl2.getHeight()-Jl2.getXHeight(),color_170_170_170);
        canvas.translate(Jl2.getWidth() - Jl3.getWidth(),60);
        canvas.drawTextLine(Jl3,0,Jl3.getHeight()-Jl3.getXHeight(),color_170_170_170);
        canvas.translate(Jl3.getWidth() - Jl4.getWidth(),60);
        canvas.drawTextLine(Jl4,0,Jl4.getHeight()-Jl4.getXHeight(),color_170_170_170);
        canvas.restore();

        canvas.save();
        canvas.translate(880 - Jrr1.getWidth(),75);
        canvas.drawTextLine(Jrr1,0,Jrr1.getHeight()-Jrr1.getXHeight(),color_170_170_170);
        canvas.translate(Jrr1.getWidth() - Jrr2.getWidth(),60);
        canvas.drawTextLine(Jrr2,0,Jrr2.getHeight()-Jrr2.getXHeight(),color_170_170_170);
        canvas.translate(Jrr2.getWidth() - Jrr3.getWidth(),60);
        canvas.drawTextLine(Jrr3,0,Jrr3.getHeight()-Jrr3.getXHeight(),color_170_170_170);
        canvas.translate(Jrr3.getWidth() - Jrr4.getWidth(),60);
        canvas.drawTextLine(Jrr4,0,Jrr4.getHeight()-Jrr4.getXHeight(),color_170_170_170);
        canvas.restore();
        /*
        canvas.save();
        canvas.translate(450 - Jml1.getWidth() - Jl1.getWidth(),75 + 8);
        canvas.drawTextLine(Jml1,0,Jml1.getHeight()-Jml1.getXHeight(), SignedNumberPaintColor(Jml1t));
        canvas.translate(Jml1.getWidth() + Jl1.getWidth() - Jml2.getWidth() - Jl2.getWidth(),60);
        canvas.drawTextLine(Jml2,0,Jml2.getHeight()-Jml2.getXHeight(), SignedNumberPaintColor(Jml2t));
        canvas.translate(Jml2.getWidth() + Jl2.getWidth() - Jml3.getWidth() - Jl3.getWidth(),60);
        canvas.drawTextLine(Jml3,0,Jml3.getHeight()-Jml3.getXHeight(), SignedNumberPaintColor(Jml3t));
        canvas.translate(Jml3.getWidth() + Jl3.getWidth() - Jml4.getWidth() - Jl4.getWidth(),60);
        canvas.drawTextLine(Jml4,0,Jml4.getHeight()-Jml4.getXHeight(), SignedNumberPaintColor(Jml4t));
        canvas.restore();

        canvas.save();
        canvas.translate(880 - Jmr1.getWidth() - Jrr1.getWidth(),75 + 8);
        canvas.drawTextLine(Jmr1,0,Jmr1.getHeight()-Jmr1.getXHeight(), SignedNumberPaintColor(Jmr1t));
        canvas.translate(Jmr1.getWidth() + Jrr1.getWidth() - Jmr2.getWidth() - Jrr2.getWidth(),60);
        canvas.drawTextLine(Jmr2,0,Jmr2.getHeight()-Jmr2.getXHeight(), SignedNumberPaintColor(Jmr2t));
        canvas.translate(Jmr2.getWidth() + Jrr2.getWidth() - Jmr3.getWidth() - Jrr3.getWidth(),60);
        canvas.drawTextLine(Jmr3,0,Jmr3.getHeight()-Jmr3.getXHeight(), SignedNumberPaintColor(Jmr3t));
        canvas.translate(Jmr3.getWidth() + Jrr3.getWidth() - Jmr4.getWidth() - Jrr4.getWidth(),60);
        canvas.drawTextLine(Jmr4,0,Jmr4.getHeight()-Jmr4.getXHeight(), SignedNumberPaintColor(Jmr4t));
        canvas.restore();
        */
    }
    
    private char getNumberSign (double Number){
        char Sign = 0;
        if (Number >= 0d){
            Sign = '+';
        } else if (Number < 0d) {
            Sign = '-';
        }
        return Sign;
    }

    private Paint SignedNumberPaintColor(double Number){
        Paint Color;
        if (Number > 0d){
            Color = new Paint().setARGB(255,124,198,35);
        } else if (Number < 0d) {
            Color = new Paint().setARGB(255,237,108,158);
        } else {
            Color = new Paint().setARGB(255,124,198,35);
        }
        return Color;
    }

    public Image build() {
        return super.build(20);
    }
}
