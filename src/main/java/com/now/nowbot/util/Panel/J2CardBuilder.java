package com.now.nowbot.util.Panel;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;

import java.io.IOException;

public class J2CardBuilder extends PanelBuilder{

    public J2CardBuilder(OsuUser user) throws IOException {
        super(900, 355);

        drawBaseRRect();
        drawUserRankingCurve(user);
    }

    private void drawBaseRRect() {
        //画底层圆角矩形
        canvas.save();
        canvas.drawRRect(RRect.makeXYWH(0, 0, 900, 355, 20), new Paint().setARGB(255, 56, 46, 50));
        canvas.restore();
    }

    private void drawUserRankingCurve(OsuUser user) {
        //用户Rank曲线，要求折线描边5px，颜色255,204,34，需要标出最大值和最小值。
        //最大值数字一般在曲线最大的右上角，(数字文本的左上角：曲线最大坐标：左移20px，上移20px)，24px高度，颜色同上，
        //但如果最大值出现在靠近当前天数的五天内，则改为在曲线最大坐标左上角显示。)
        //当前值数字一般在曲线当前值位置的右上角(要求同上)
        //但如果当前值与最大值靠得太近，则改为在曲线最大坐标右下角显示。移动方法也是像上面那样的20px、20px。
        //当前值和最大值所用的小标识我会用小png来表示。
        //注意，这里还需要 2 套大数字省略方法，具体内容如下：
        //1-99-0.1K-9.9K-10K-99K-0.1M-9.9M-10M-99M-0.1G-9.9G-10G-99G-0.1T-9.9T-10T-99T-Inf.
        //1-999-1.00K-999.99K-1.00M-999.99M-1.00G-999.99G-...-999.9T-Inf
    }
    private void drawCardText(OsuUser user) {
        //画卡片基础信息
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS24 = new Font(TorusSB, 24);
        Font fontS36 = new Font(TorusSB, 36);

        String Jlu1t = "Ranking"; //左上

        double Jl1t = 0D; //正左，坐标参数，需要根据折线数据来判断！
        double Jl2t = 0D;
        double Jl3t = 0D;

        double Jb1t = 0D; //正下，坐标参数
        double Jb2t = 0D;
        double Jb3t = 0D;

        String Jur1t = "#0"; //右上
        String Jur2t = "??";
        String Jur3t = "#0";

        Jl1t = SkiaUtil.getRoundedNumber(Jl1t,1);
        Jl2t = SkiaUtil.getRoundedNumber(Jl2t,1);
        Jl3t = SkiaUtil.getRoundedNumber(Jl3t,1);
        Jb1t = SkiaUtil.getRoundedNumber(Jb1t,1);
        Jb2t = SkiaUtil.getRoundedNumber(Jb2t,1);
        Jb3t = SkiaUtil.getRoundedNumber(Jb3t,1);

        TextLine Jlu1 = TextLine.make(Jlu1t, fontS36);

        TextLine Jl1 = TextLine.make(String.valueOf(Jl1t) + SkiaUtil.getRoundedNumberUnit(Jl1t,1), fontS24);
        TextLine Jl2 = TextLine.make(String.valueOf(Jl2t) + SkiaUtil.getRoundedNumberUnit(Jl2t,1), fontS24);
        TextLine Jl3 = TextLine.make(String.valueOf(Jl3t) + SkiaUtil.getRoundedNumberUnit(Jl3t,1), fontS24);

        TextLine Jb1 = TextLine.make(String.valueOf(Jb1t) + SkiaUtil.getRoundedNumberUnit(Jb1t,1), fontS24);
        TextLine Jb2 = TextLine.make(String.valueOf(Jb2t) + SkiaUtil.getRoundedNumberUnit(Jb2t,1), fontS24);
        TextLine Jb3 = TextLine.make(String.valueOf(Jb3t) + SkiaUtil.getRoundedNumberUnit(Jb3t,1), fontS24);

        TextLine Jur1 = TextLine.make(Jur1t, fontS36);
        TextLine Jur2 = TextLine.make(Jur2t, fontS24);
        TextLine Jur3 = TextLine.make(Jur3t, fontS24);


        canvas.save();
        canvas.translate(20,20);
        canvas.drawTextLine(Jlu1,0,Jlu1.getHeight()-Jlu1.getXHeight(),new Paint().setARGB(255,255,255,255));

        canvas.translate(-20 + ( 60 - Jl1.getWidth() / 2 ),36);//居中处理
        canvas.drawTextLine(Jl1,0,Jl1.getHeight()-Jl1.getXHeight(),new Paint().setARGB(255,195,160,30));
        canvas.translate(( Jl1.getWidth() - Jl2.getWidth() )/ 2,107);//居中处理
        canvas.drawTextLine(Jl2,0,Jl2.getHeight()-Jl2.getXHeight(),new Paint().setARGB(255,195,160,30));
        canvas.translate(( Jl2.getWidth() - Jl3.getWidth() )/ 2,107);//居中处理
        canvas.drawTextLine(Jl3,0,Jl3.getHeight()-Jl3.getXHeight(),new Paint().setARGB(255,195,160,30));
        canvas.restore();

        canvas.save();
        canvas.translate(60,300);
        canvas.drawTextLine(Jb1,0,Jb1.getHeight()-Jb1.getXHeight(),new Paint().setARGB(255,195,160,30));
        canvas.translate(330 + (( 120 - Jb2.getWidth()) / 2),0);
        canvas.drawTextLine(Jb2,0,Jb2.getHeight()-Jb2.getXHeight(),new Paint().setARGB(255,195,160,30));
        canvas.translate(330 + (Jb2.getWidth() / 2) + 60,0);
        canvas.drawTextLine(Jb3,0,Jb3.getHeight()-Jb3.getXHeight(),new Paint().setARGB(255,195,160,30));
        canvas.restore();

        canvas.save();
        canvas.translate(880 - Jur3.getWidth(),28);
        canvas.drawTextLine(Jur3,0,Jur3.getHeight()-Jur3.getXHeight(),new Paint().setARGB(255,170,170,170));
        canvas.translate(0 - Jur2.getWidth(),0);
        canvas.drawTextLine(Jur2,0,Jur2.getHeight()-Jur2.getXHeight(),new Paint().setARGB(255,170,170,170));
        canvas.translate(-20 - Jur1.getWidth(),-8);
        canvas.drawTextLine(Jur1,0,Jur1.getHeight()-Jur1.getXHeight(),new Paint().setARGB(255,255,255,255));
        canvas.restore();
    }
}
