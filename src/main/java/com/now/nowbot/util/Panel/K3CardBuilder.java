package com.now.nowbot.util.Panel;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.JsonData.BeatmapDifficultyAttributes;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.util.SkiaImageUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class K3CardBuilder extends PanelBuilder {

    Paint colorRRect = new Paint().setARGB(255, 56, 46, 50);
    Paint colorDarkGrey = new Paint().setARGB(255, 100, 100, 100);
    Paint colorGrey = new Paint().setARGB(255, 170, 170, 170); //Meh 漏掉的小果
    Paint colorWhite = new Paint().setARGB(255, 255, 255, 255);
    Paint colorLightBlue = new Paint().setARGB(255, 141, 207, 244); //Perfect 300 良 大果
    Paint colorYellow = new Paint().setARGB(255, 254, 246, 103); //Great 50 小果
    Paint colorGoldenYellow = new Paint().setARGB(255, 255,204,34); //金黄色
    Paint colorGreen = new Paint().setARGB(255, 121, 196, 113); //Good 100 可 中果
    Paint colorBlue = new Paint().setARGB(255, 94, 138, 198); //Ok
    Paint colorRed = new Paint().setARGB(255, 236, 107, 118); //miss 不可

    public K3CardBuilder(Score score, BeatmapDifficultyAttributes beatMapAttribute) {
        //这是右下角的附加信息矩形
        super(1000, 270);

        drawBaseRRect();
        drawJudgeGraph(score);//还没有写柱状图，柱状图宽16，最高80，圆角8，间隔4，共26个
        drawRetryFailGraph(score);//还没有写两个折线图，折线图高80，宽520，起点在20,170，retry的颜色 Paint colorGoldenYellow，Fail颜色 Paint colorRed
        drawBeatMapInfo(score);
        drawStarRatingRRect(score);
    }

    private void drawBaseRRect() {
        canvas.clear(Color.makeRGB(56, 46, 50));
    }

    private void drawJudgeGraph(Score score) {
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS18 = new Font(TorusSB, 18);

        TextLine Jl = TextLine.make("Judge & Difficulty", fontS18);
        TextLine Jr = TextLine.make("rating " + "114514", fontS18);// 谱面评级

        canvas.save();
        canvas.translate(20,20);
        canvas.drawTextLine(Jl, 0, Jl.getHeight() - Jl.getXHeight(), colorGrey);
        canvas.translate(520 - Jr.getWidth(),0);
        canvas.drawTextLine(Jr, 0, Jr.getHeight() - Jr.getXHeight(), colorGrey);
        canvas.restore();
    }

    private void drawRetryFailGraph(Score score) {
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS18 = new Font(TorusSB, 18);
        StringBuilder sb = new StringBuilder();
        sb.append("r ")
                .append("114514") //重试比例
                .append("% // f ")
                .append("1919810") //失败比例
                .append("%");

        TextLine Jl = TextLine.make("Retry & Fail", fontS18);
        TextLine Jr = TextLine.make(sb.toString(), fontS18);// 谱面评级

        canvas.save();
        canvas.translate(20,140);
        canvas.drawTextLine(Jl, 0, Jl.getHeight() - Jl.getXHeight(), colorGrey);
        canvas.translate(520 - Jr.getWidth(),0);
        canvas.drawTextLine(Jr, 0, Jr.getHeight() - Jr.getXHeight(), colorGrey);
        canvas.restore();
    }

    private void drawBeatMapInfo(Score score) {
        int bpm_int = (int) Math.floor(score.getBeatMap().getBpm());
        double bpm_dec = score.getBeatMap().getBpm() - bpm_int;
        long beatLength = Math.round(60000D / score.getBeatMap().getBpm());

        int length = score.getBeatMap().getTotalLength();
        int drain = score.getBeatMap().getHitLength();

        float cs = score.getBeatMap().getCS();
        int cs_int = (int) Math.floor(cs);
        double cs_dec = cs - cs_int;
        float cs_pixel = 54.4f - 4.48f * cs;

        float ar = score.getBeatMap().getAR();
        int ar_int = (int) Math.floor(ar);
        double ar_dec = ar - ar_int;
        float ar_preempt = 0;
        if (ar < 5){
            ar_preempt = 1200 + 600 * (5 - ar) / 5;
        } else if (ar >= 5){
            ar_preempt = 1200 - 750 * (ar - 5) / 5;
        }
        
        float od = score.getBeatMap().getOD();
        int od_int = (int) Math.floor(od);
        double od_dec = od - od_int;
        float od_300window = 0;
        switch (score.getMode())
        {
            case OSU: od_300window = 80 - 6 * od; break;
            case TAIKO: od_300window = 50 - 3 * od; break;
            case CATCH: od_300window = -1 ; break;
            case MANIA: od_300window = 64 - 3 * od;
        }

        float hp = score.getBeatMap().getHP();
        int hp_int = (int) Math.floor(hp);
        double hp_dec = hp - hp_int;
            
        Image bpmII = null;
        String bpmIN = "BPM";
        String bpmLI = String.valueOf(bpm_int);
        String bpmSI;
        if (bpm_dec == 0){
            bpmSI = String.valueOf(bpm_dec).substring(1,3);
        } else {
            bpmSI = null;
        }
        String bpmAI = String.valueOf(beatLength).substring(0,5)+ "ms"; //一拍的毫秒数

        Image lengthII = null;
        String lengthIN = "Length";
        String lengthLI = String.valueOf((length - length % 60)/60);
        String lengthSI = ":" + String.format("%2d",length % 60);
        String lengthAI = (drain - drain % 60) / 60 + ":" + String.format("%2d",drain % 60); //实际时间

        Image csII = null;
        String csIN = "CS";
        String csLI = String.valueOf(cs_int);
        String csSI;
        if (bpm_dec == 0){
            csSI = String.valueOf(cs_dec).substring(1,3);
        } else {
            csSI = null;
        }
        String csAI = (int) cs_pixel + " px"; //圆圈的尺寸

        Image arII = null;
        String arIN = "AR";
        String arLI = String.valueOf(ar_int);
        String arSI;
        if (bpm_dec == 0){
            arSI = String.valueOf(ar_dec).substring(1,3);
        } else {
            arSI = null;
        }
        String arAI = ar_preempt + " ms"; //缩圈出现的总时间

        Image odII = null;
        String odIN = "HP";
        String odLI = String.valueOf(od_int);
        String odSI;
        if (bpm_dec == 0){
            odSI = String.valueOf(od_dec).substring(1,3);
        } else {
            odSI = null;
        }
        String odAI = od_300window + " ms"; //300 击打窗口

        Image hpII = null;
        String hpIN = "HP";
        String hpLI = String.valueOf(hp_int);
        String hpSI;
        if (bpm_dec == 0){
            hpSI = String.valueOf(hp_dec).substring(1,3);
        } else {
            hpSI = null;
        }
        String hpAI = "-"; //暂时不写
        
        try {
            bpmII = SkiaImageUtil.getImage(java.nio.file.Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-beatsperminute.png"));
            lengthII = SkiaImageUtil.getImage(java.nio.file.Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-length.png"));
            arII = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-approachrate.png"));
            csII = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-circlesize.png"));
            odII = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-overalldifficulty.png"));
            hpII = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-healthpoint.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        canvas.save();
        canvas.translate(560,20);
        drawInfoUnit(bpmII,bpmIN,bpmLI,bpmSI,bpmAI);
        canvas.translate(210,0);
        drawInfoUnit(lengthII,lengthIN,lengthLI,lengthSI,lengthAI);
        canvas.translate(- 210,80);
        drawInfoUnit(arII,arIN,arLI,arSI,arAI);
        canvas.translate(210,0);
        drawInfoUnit(csII,csIN,csLI,csSI,csAI);
        canvas.translate(- 210,80);
        drawInfoUnit(odII,odIN,odLI,odSI,odAI);
        canvas.translate(210,0);
        drawInfoUnit(hpII,hpIN,hpLI,hpSI,hpAI);
        canvas.restore();
    }
    private void drawStarRatingRRect(Score score) {
        List<String> mod = score.getMods();

        // if (mod.contains("EZ")) ;

        canvas.save();
        canvas.translate(560,20);
        canvas.drawRRect(RRect.makeXYWH(0,0,520,4,2),colorGrey);
        canvas.restore();
    }


    private Paint getJudgeColor(String JudgeName) {
        Paint RRectPaint;

        switch (JudgeName) {
            case "o_300":
            case "t_300":
            case "c_300":
            case "m_320": RRectPaint = colorLightBlue; break;
            case "o_100":
            case "c_100":
            case "t_150":
            case "m_200": RRectPaint = colorGreen; break;
            case "o_50":
            case "c_50":
            case "m_300": RRectPaint = colorYellow; break;
            case "m_100": RRectPaint = colorBlue; break;
            case "c_dl":
            case "m_50": RRectPaint = colorGrey; break;
            case "o_0":
            case "t_0":
            case "c_0":
            case "m_0": RRectPaint = colorRed; break;
            default: RRectPaint = colorDarkGrey;
        }
        return RRectPaint;
    }

    private int getJudge (Score score, String Name) {
        int data = 0;

        int s_300 = score.getStatistics().getCount300();
        int s_100 = score.getStatistics().getCount100();
        int s_50 = score.getStatistics().getCount50();
        int s_g = score.getStatistics().getCountGeki();
        int s_k = score.getStatistics().getCountKatu();
        int s_0 = score.getStatistics().getCountMiss();

        switch (Name) {
            case "o_300":
            case "c_300":
            case "t_300": data = s_300; break;
            case "o_100":
            case "t_150": data = s_100; break;
            case "o_50": data = s_50; break;
            case "m_320": data = s_g; break;
            case "m_200":
            case "c_dl": data = s_k; break;
            case "o_0":
            case "t_0":
            case "c_0":
            case "m_0": data = s_0;
        }
        return data;
    }

    private void drawInfoUnit (Image IndexImage, String IndexName, String LargeInfo, String SmallInfo, String AssistInfo) {
        //这是一个 200 * 50 大小的组件，因为复用较多，写成私有方法方便调用
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS36 = new Font(TorusSB, 36);
        Font fontS24 = new Font(TorusSB, 24);
        Font fontS18 = new Font(TorusSB, 18);

        TextLine U1 = TextLine.make(IndexName, fontS18);
        TextLine U2 = TextLine.make(AssistInfo, fontS18);
        TextLine U3 = TextLine.make(LargeInfo, fontS36);
        TextLine U4 = TextLine.make(SmallInfo, fontS24);

        canvas.save();
        canvas.drawImage(IndexImage, 0, 0, new Paint()); //这里可以试着用try catch环绕一下
        canvas.translate(50,0);
        canvas.drawTextLine(U1, 0, U1.getHeight() - U1.getXHeight(), colorGrey);
        canvas.translate(150 - U2.getWidth(),0);

        // combo 的 AssistInfo 是唯一一个例外，需要完全变白
        if (Objects.equals(IndexName, "combo")){
            canvas.drawTextLine(U2, 0, U2.getHeight() - U2.getXHeight(), colorWhite);
        } else {
            canvas.drawTextLine(U2, 0, U2.getHeight() - U2.getXHeight(), colorDarkGrey);
        }

        canvas.translate(56 - 150 + U2.getWidth(),20);
        canvas.drawTextLine(U3, 0, U3.getHeight() - U3.getXHeight(), colorWhite);
        canvas.translate(0,8);
        canvas.drawTextLine(U4, 0, U4.getHeight() - U4.getXHeight(), colorWhite);
        canvas.restore();
    }

    public Image build() {
        return super.build(20);
    }
}
