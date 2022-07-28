package com.now.nowbot.util.Panel;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.util.SkiaImageUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class K2CardBuilder extends PanelBuilder {

    Paint colorRRect = new Paint().setARGB(255,56,46,50);
    Paint colorDarkGrey = new Paint().setARGB(255,100,100,100);
    Paint colorGrey = new Paint().setARGB(255,170,170,170);
    Paint colorWhite = new Paint().setARGB(255,255,255,255);

    public K2CardBuilder(Score score) {
        //这是 pr panel 右上角最主要的信息矩形。
        super(1000,420);

        BeatMap beatMap = score.getBeatMap();
        drawBaseRRect();
        drawLeftRank(score);
        drawLeftPassStatus(score);
        drawScoreIndex(beatMap);
        drawScoreInfo(beatMap);
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
        canvas.drawImage(a1,0,0,new Paint());
        canvas.translate(160,0);
        canvas.drawImage(a2,0,0,new Paint());
        canvas.translate(-160,50);
        canvas.drawImage(a3,0,0,new Paint());
        canvas.translate(160,0);
        canvas.drawImage(a4,0,0,new Paint());
        canvas.restore();
    }

    private void drawScoreIndex(BeatMap beatMap){

    }
    private void drawScoreInfo(BeatMap beatMap){

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
