package com.now.nowbot.util.Panel;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.beatmap.BeatmapAttribute;
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

    public K2CardBuilder(Score score, BeatMap beatmap, BeatmapAttribute beatMapAttribute) {
        //这是 pr panel 右上角最主要的信息矩形。
        super(1000,420);

        BeatMap beatMap = score.getBeatMap();
        drawBaseRRect();
        drawLeftRank(score);
        drawLeftPassStatus(score);
        drawScoreIndex(score, beatmap);
        drawMods(score);
        drawScoreInfo(score);
        drawBeatMapInfo(beatMap);
    }

    private void drawBaseRRect() {
        canvas.clear(Color.makeRGB(56, 46, 50));
    }

    private void drawLeftRank(Score score){
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

    private void drawLeftPassStatus(Score score){
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

        boolean con1 = true; //这里应该是 游玩时间长于一半，才可算作 play 条件
        boolean con2 = score.getPassed();
        boolean con3 = score.getStatistics().getCountMiss() == 0;
        boolean con4 = score.getPerfect();

        Image a1,a2,a3,a4;

        if (!con1){
            a1 = Play0;
            a2 = Clear0;
            a3 = NoMiss0;
            a4 = FullCombo0;
        } else if (!con2){
            a1 = Play1;
            a2 = Clear0;
            a3 = NoMiss0;
            a4 = FullCombo0;
        } else if (!con3){
            a1 = Play0;
            a2 = Clear1;
            a3 = NoMiss0;
            a4 = FullCombo0;
        } else if (!con4){
            a1 = Play0;
            a2 = Clear0;
            a3 = NoMiss1;
            a4 = FullCombo0;
        } else {
            a1 = Play0;
            a2 = Clear0;
            a3 = NoMiss0;
            a4 = FullCombo1;
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

    private void drawScoreIndex(Score score, BeatMap beatmap){
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        
        Font fontS84 = new Font(TorusSB, 84);
        Font fontS60 = new Font(TorusSB, 60);

        String Scr = SkiaUtil.getV3Score(score, beatmap);
        String s1t = Scr.substring(0,2);
        String s2t = Scr.substring(3);

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

    private void drawScoreInfo(Score score){

    }
    private void drawBeatMapInfo(BeatMap beatMap){

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
        Paint paint;
        String ScoreName;
        String ScoreCountStr;

        switch (ScoreIndex){
            case "o_300", "t_300", "c_300" -> {ScoreName = "300"; paint = colorLightBlue;}
            case "o_100", "c_100" -> {ScoreName = "100"; paint = colorGreen;}
            case "o_50", "c_50" -> {ScoreName = "50"; paint = colorYellow;}
            case "t_150" -> {ScoreName = "150"; paint = colorGreen;}
            case "m_320" -> {ScoreName = "320"; paint = colorLightBlue;}
            case "m_300" -> {ScoreName = "300"; paint = colorYellow;}
            case "m_200" -> {ScoreName = "200"; paint = colorGreen;}
            case "m_100" -> {ScoreName = "100"; paint = colorBlue;}
            case "m_50" -> {ScoreName = "50"; paint = colorGrey;}

            case "c_dl" -> {ScoreName = "DL"; paint = colorGrey;}
            case "o_0", "t_0", "c_0", "m_0" -> {ScoreName = "0"; paint = colorRed;}

            default -> {ScoreName = "??"; paint = colorDarkGrey;}
        }

        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS30 = new Font(TorusSB, 30);

        if (ScoreCount > 9999) {
            ScoreCountStr = "####";
        } else if (ScoreCount >= 0){
            ScoreCountStr = String.valueOf(ScoreCount);
        } else {
            ScoreCountStr = "0";
        }

        TextLine L = TextLine.make(ScoreName, fontS30);
        TextLine R = TextLine.make(ScoreCountStr, fontS30);

        float RRectLength = 500f * ScoreCount / TotalCount;
        if (RRectLength < 20f && RRectLength > 0f) RRectLength = 20f; //这是圆角矩形的最小宽度，如果为 0 直接跳过

        canvas.save();
        if (RRectLength != 0f){
            canvas.drawRRect(RRect.makeXYWH(0,0, RRectLength,28,10), paint);
            canvas.restore();
        }
        canvas.translate(- 14 - L.getWidth(),2);
        canvas.drawTextLine(L, 0, L.getHeight() - L.getXHeight(), colorWhite);
        canvas.restore();

        canvas.translate(512,2);
        canvas.drawTextLine(R, 0, R.getHeight() - R.getXHeight(), colorWhite);
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
