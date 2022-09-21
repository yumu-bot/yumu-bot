package com.now.nowbot.util.Panel;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.util.SkiaImageUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class K2CardBuilder extends PanelBuilder {

    Paint colorRRect = new Paint().setARGB(255,56,46,50);
    Paint colorDarkGrey = new Paint().setARGB(255,100,100,100);
    Paint colorGrey = new Paint().setARGB(255,170,170,170); //Meh 漏掉的小果
    Paint colorWhite = new Paint().setARGB(255,255,255,255);
    Paint colorLightBlue = new Paint().setARGB(255,141,207,244); //Perfect 300 良 大果
    Paint colorYellow = new Paint().setARGB(255,254,246,103); //Great 50 小果
    Paint colorGreen = new Paint().setARGB(255,121,196,113); //Good 100 可 中果
    Paint colorBlue = new Paint().setARGB(255,94,138,198); //Ok
    Paint colorRed = new Paint().setARGB(255,236,107,118); //miss 不可

    public K2CardBuilder(Score score) {
        //这是 pr panel 右上角最主要的信息矩形。
        super(1000,420);

        drawBaseRRect();
        drawRank(score);
        drawPassStatus(score);
        drawScore(score);
        drawMods(score);
        drawScoreIndex(score);
        drawScoreInfo(score);
    }

    private void drawBaseRRect() {
        canvas.clear(Color.makeRGB(56, 46, 50));
    }

    private void drawRank(Score score){
        double ArcDegree = score.getAccuracy() * 360f;
        Image RankImage = getRankImage(score.getRank());
        Image AccuracyImage = null;
        Image ColoredCircle = null;
        try {
            AccuracyImage = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-coloredring.png"));
            ColoredCircle = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-coloredcircle.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 270x270的环，圆中心175,155
        var ArcRect = org.jetbrains.skija.Rect.makeXYWH(40f,20f,270f,270f);
        var ArcPath = new org.jetbrains.skija.Path().addArc(ArcRect,-90f, (float) (-90f + ArcDegree))
                .lineTo(175,155)
                .lineTo(175,20)
                .closePath();

        canvas.save();
        canvas.clipPath(ArcPath);
        canvas.translate(40,20);
        canvas.drawImage(AccuracyImage,0,0,new Paint());
        canvas.restore();

        canvas.save();
        canvas.translate(70,50);
        canvas.drawImage(ColoredCircle,0,0,new Paint());
        canvas.translate(59,46);
        canvas.drawImage(RankImage,0,0,new Paint());
        canvas.restore();
    }

    private void drawPassStatus(Score score){
        Image Play0 = null;
        Image Clear0 = null;
        Image NoMiss0 = null;
        Image FullCombo0 = null;
        Image Play1 = null;
        Image Clear1 = null;
        Image NoMiss1 = null;
        Image FullCombo1 = null;

        try {
            Play0 = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-playdefault.png"));
            Clear0 = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-cleardefault.png"));
            NoMiss0 = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-nomissdefault.png"));
            FullCombo0 = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-fullcombodefault.png"));
            Play1 = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-play.png"));
            Clear1 = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-clear.png"));
            NoMiss1 = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-nomiss.png"));
            FullCombo1 = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-fullcombo.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean con1 = SkiaUtil.getV3ScoreProgress(score) >= 0.5D; //这里应该是 游玩时间长于一半，才可算作 play 条件，目前用这个下下策方法
        boolean con2 = score.getPassed();
        boolean con3 = score.getStatistics().getCountMiss() == 0;
        boolean con4 = score.getPerfect();

        Image a1,a2,a3,a4;

        if (con4){
            a1 = Play0;
            a2 = Clear0;
            a3 = NoMiss0;
            a4 = FullCombo1;
        } else if (con3){
            a1 = Play0;
            a2 = Clear0;
            a3 = NoMiss1;
            a4 = FullCombo0;
        } else if (con2){
            a1 = Play0;
            a2 = Clear1;
            a3 = NoMiss0;
            a4 = FullCombo0;
        } else if (con1){
            a1 = Play1;
            a2 = Clear0;
            a3 = NoMiss0;
            a4 = FullCombo0;
        } else {
            a1 = Play0;
            a2 = Clear0;
            a3 = NoMiss0;
            a4 = FullCombo0;
        }

        canvas.save();
        canvas.translate(20,310);
        canvas.drawImage(a1,0,0,new Paint());
        canvas.translate(160,0);
        canvas.drawImage(a2,0,0,new Paint());
        canvas.translate(-160,50);
        canvas.drawImage(a3,0,0,new Paint());
        canvas.translate(160,0);
        canvas.drawImage(a4,0,0,new Paint());
        canvas.restore();
    }

    private void drawScore(Score score){
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        
        Font fontS84 = new Font(TorusSB, 84);
        Font fontS60 = new Font(TorusSB, 60);

        String Scr = SkiaUtil.getV3Score(score);
        String s1t,s2t;

        s1t = Scr.substring(0,3);
        s2t = Scr.substring(4,7);

        TextLine s1 = TextLine.make(s1t, fontS84);
        TextLine s2 = TextLine.make(s2t, fontS60);

        canvas.save();
        canvas.translate(435,20);
        canvas.drawTextLine(s1, 0, s1.getHeight() - s1.getXHeight(), colorWhite);
        canvas.translate(s1.getWidth() + 4,16);
        canvas.drawTextLine(s2, 0, s2.getHeight() - s2.getXHeight(), colorWhite);
        canvas.restore();
    }

    private void drawMods(Score score){
        List<String> mods = score.getMods();

        canvas.save();
        canvas.translate(880,20);

        if (mods.size() >= 4){
            for(var mod : mods){
                try {
                    var modImg = SkiaImageUtil.getImage(NowbotConfig.BG_PATH + "ExportFileV3/Mods/" + mod + ".png");
                    canvas.drawImage(modImg,0,0);
                    canvas.translate(-40,0);
            } catch (IOException ignored) {}}
        } else if (mods.size() >0){
            for(var mod : mods){
                try {
                    var modImg = SkiaImageUtil.getImage(NowbotConfig.BG_PATH + "ExportFileV3/Mods/" + mod + ".png");
                    canvas.drawImage(modImg,0,0);
                    canvas.translate(-100,0);
                } catch (IOException ignored) {}}
        }
        canvas.restore();
    }

    private void drawScoreIndex(Score score){

        int s_300 = score.getStatistics().getCount300();
        int s_100 = score.getStatistics().getCount100();
        int s_50 = score.getStatistics().getCount50();
        int s_g = score.getStatistics().getCountGeki();
        int s_k = score.getStatistics().getCountKatu();
        int s_0 = score.getStatistics().getCountMiss();

        int s = score.getBeatMap().getMaxCombo();

        canvas.save();
        canvas.translate(400,100);
        switch (score.getMode()){
            case OSU:{
                canvas.translate(0,40);
                drawScoreUnit("o_300",s_300,s);
                canvas.translate(0,40);
                drawScoreUnit("o_100",s_100,s);
                canvas.translate(0,40);
                drawScoreUnit("o_50",s_50,s);
                canvas.translate(0,40);
                drawScoreUnit("o_0",s_0,s);
            } break;

            case TAIKO:{
                canvas.translate(0,40);
                drawScoreUnit("t_300",s_300,s);
                canvas.translate(0,40);
                drawScoreUnit("t_150",s_100,s);
                canvas.translate(0,80);
                drawScoreUnit("t_0",s_0,s);
            } break;

            case CATCH:{
                canvas.translate(0,40);
                drawScoreUnit("c_300",s_300,s);
                canvas.translate(0,40);
                drawScoreUnit("c_100",s_100,s);
                canvas.translate(0,40);
                drawScoreUnit("c_50",s_50,s);
                canvas.translate(0,40);
                drawScoreUnit("c_0",s_0,s);
                canvas.translate(0,40);
                drawScoreUnit("c_dl",s_k,s); //miss droplet
            } break;

            case MANIA:{
                drawScoreUnit("m_320",s_g,s);
                canvas.translate(0,40);
                drawScoreUnit("m_300",s_300,s);
                canvas.translate(0,40);
                drawScoreUnit("m_200",s_k,s);
                canvas.translate(0,40);
                drawScoreUnit("m_100",s_100,s);
                canvas.translate(0,40);
                drawScoreUnit("m_50",s_50,s);
                canvas.translate(0,40);
                drawScoreUnit("m_0",s_0,s);
            } break;
        }
        canvas.restore();
    }

    private void drawScoreInfo(Score score){
        OsuMode mode = score.getMode();
        double acc = 100D * score.getAccuracy();
        double accGoal;
        int combo = score.getMaxCombo();
        int maxcombo = score.getBeatMap().getMaxCombo();
        float pp = Math.round(score.getPP());

        switch (mode) {
            case OSU:
                if (acc < 60D) {
                    accGoal = acc - 60D;
                } else if (acc < 75D) {
                    accGoal = acc - 75D;
                } else if (acc < 250D / 3D) {//83.33
                    accGoal = acc - 250D / 3D;
                } else if (acc < 280D / 3D) {//93.17
                    accGoal = acc - 280D / 3D;
                } else {
                    accGoal = acc - 100D;
                } break;
            case TAIKO, MANIA:
                if (acc < 70D) {
                    accGoal = acc - 70D;
                } else if (acc < 80D) {
                    accGoal = acc - 80D;
                } else if (acc < 90D) {
                    accGoal = acc - 90D;
                } else if (acc < 95D) {
                    accGoal = acc - 95D;
                } else {
                    accGoal = acc - 100D;
                } break;
            case CATCH:
                if (acc < 85D) {
                    accGoal = acc - 85D;
                } else if (acc < 90D) {
                    accGoal = acc - 90D;
                } else if (acc < 94D) {
                    accGoal = acc - 94D;
                } else if (acc < 98D) {
                    accGoal = acc - 98D;
                } else {
                    accGoal = acc - 100D;
                } break;
            case default: accGoal = acc - 100D;
        }

        Image accII = null;
        String accIN = "accuracy";
        String accLI = String.valueOf(acc).substring(0,3); // 99.
        String accSI = String.valueOf(acc).substring(4,6) + "%"; // (.)43%
        String accAI = String.valueOf(accGoal).substring(0,5) + "%"; //这是个负数，取五位 -3.454%
        
        Image cbII = null;
        String cbIN = "combo";
        String cbLI = String.valueOf(combo);
        String cbSI = "x";
        String cbAI = maxcombo + "x"; //谱面最大连击

        Image ppII = null;
        String ppIN = "PP";
        String ppLI = String.valueOf(pp);
        String ppSI = null;
        String ppAI = "-"; //to do:获取谱面理论pp，还没写呢

        try {
            accII = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-accuracy.png"));
            cbII = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-combo.png"));
            ppII = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-pp.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        canvas.save();
        canvas.translate(440,350);
        drawInfoUnit(accII,accIN,accLI,accSI,accAI);
        canvas.translate(210,0);
        drawInfoUnit(cbII,cbIN,cbLI,cbSI,cbAI);
        canvas.translate(210,0);
        drawInfoUnit(ppII,ppIN,ppLI,ppSI,ppAI);
        canvas.restore();
    }
    private Image getRankImage (String Rank){
        Image RankImage = null;

        try {
            RankImage = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-score-" + Rank + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return RankImage;
    }

    private void drawScoreUnit (String ScoreIndex, int ScoreCount, int TotalCount){
        //这是分数显示的组件，因为复用较多，写成私有方法方便调用
        Paint RRectPaint;
        Paint IndexPaint = colorWhite;
        Paint ScorePaint = colorWhite;
        String ScoreName;
        String ScoreCountStr;
        
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS30 = new Font(TorusSB, 30);

        switch (ScoreIndex){
            case "o_300" :
            case "t_300" :
            case "c_300" : {ScoreName = "300"; RRectPaint = colorLightBlue;} break;
            case "o_100" :
            case "c_100" : {ScoreName = "100"; RRectPaint = colorGreen;} break;
            case "o_50" :
            case "c_50" : {ScoreName = "50"; RRectPaint = colorYellow;} break;
            case "t_150" : {ScoreName = "150"; RRectPaint = colorGreen;} break;
            case "m_320" : {ScoreName = "320"; RRectPaint = colorLightBlue;} break;
            case "m_300" : {ScoreName = "300"; RRectPaint = colorYellow;} break;
            case "m_200" : {ScoreName = "200"; RRectPaint = colorGreen;} break;
            case "m_100" : {ScoreName = "100"; RRectPaint = colorBlue;} break;
            case "m_50" : {ScoreName = "50"; RRectPaint = colorGrey;} break;

            case "c_dl" : {ScoreName = "DL"; RRectPaint = colorGrey;} break;
            case "o_0" :
            case "t_0" :
            case "c_0" :
            case "m_0" : {ScoreName = "0"; RRectPaint = colorRed;} break;

            default : {ScoreName = "??"; RRectPaint = colorDarkGrey;}
        }

        if (ScoreCount > 9999) { //超大和超小数据处理
            ScoreCountStr = "####";
        } else if (ScoreCount > 0){
            ScoreCountStr = String.valueOf(ScoreCount);
        } else {
            ScoreCountStr = "0";
            RRectPaint = colorDarkGrey;
            ScorePaint = colorGrey;
        }

        TextLine L = TextLine.make(ScoreName, fontS30);
        TextLine R = TextLine.make(ScoreCountStr, fontS30);

        float RRectLength = 480f * ScoreCount / TotalCount + 20f;  //20是圆角矩形的最小宽度
        if (RRectLength > 500f) RRectLength = 500f;

        canvas.save();
        if (RRectLength != 0f){
            canvas.drawRRect(RRect.makeXYWH(0,0, RRectLength,28,10), RRectPaint);
            canvas.restore();
        }
        canvas.translate(- 14 - L.getWidth(),2);
        canvas.drawTextLine(L, 0, L.getHeight() - L.getXHeight(), IndexPaint);
        canvas.restore();

        canvas.translate(512,2);
        canvas.drawTextLine(R, 0, R.getHeight() - R.getXHeight(), ScorePaint);
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
