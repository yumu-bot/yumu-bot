package com.now.nowbot.util.Panel;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.util.*;
import org.jetbrains.skija.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class K1CardBuilder extends PanelBuilder {

    Paint colorRRect = new Paint().setARGB(255,56,46,50);
    Paint colorGrey = new Paint().setARGB(255,170,170,170);
    Paint colorWhite = new Paint().setARGB(255,255,255,255);

    public K1CardBuilder(Score score) {
        //这是 pr panel的左侧物件合集，严格来说不算卡片。但写一起方便管理。
        //这个卡片没有作为背景的圆角矩形，是透明的，注意！
        super(880,790);
        //测试时添加的背景
        canvas.clear(Color.makeRGB(15,25,35));
        BeatMap beatMap = score.getBeatMap();

        drawLeftRRect(beatMap);
        drawLeftMapSR(beatMap);
        drawRightRRect(beatMap);
        drawBeatMapHexagon(beatMap);
        drawBeatMapInfo(beatMap);
    }

    private void drawLeftRRect(BeatMap beatMap) {

        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Typeface ExtraSB = null;
        try {
            ExtraSB = SkiaUtil.getEXTRA();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Font fontS36 = new Font(TorusSB, 36);
        Font fontS48 = new Font(TorusSB, 48);
        Font ExtraS48 = new Font(ExtraSB, 48);
        String c;
        switch (beatMap.getModeInt()){
            default:
            case 0: c = PanelUtil.MODE_OSU;break;
            case 1: c = PanelUtil.MODE_TAIKO;break;
            case 2: c = PanelUtil.MODE_CATCH;break;
            case 3: c = PanelUtil.MODE_MANIA;break;
        }
        TextLine luMode = TextLine.make(c, ExtraS48);

        //这里应该需要获取谱面“加mod“后的星数难度，需要getBeatmapAttribute什么的
        int SRIntI = (int) Math.floor(beatMap.getDifficultyRating());
        double SRDecI = (beatMap.getDifficultyRating() - Math.floor(beatMap.getDifficultyRating()));

        StringBuilder SRintSB = new StringBuilder()
                .append(SRIntI)
                .append('.');

        TextLine SRInt = TextLine.make(String.valueOf(SRintSB), fontS48);
        TextLine SRDec = TextLine.make(String.valueOf(SRDecI).substring(2,4), fontS36);
        TextLine SRInt2 = TextLine.make(String.valueOf(SRIntI), fontS48);//这个是dec为0的时候输出的

        //画底层圆角矩形
        canvas.save();
        canvas.translate(40,40);
        canvas.drawRRect(RRect.makeXYWH(0,0,190,60,20),colorRRect);
        canvas.restore();

        //画星数
        canvas.save();
        if (SRDecI >= 0.005f){ // 正常输出
            canvas.translate(160 - (SRInt.getWidth() - SRDec.getWidth()) / 2,50);
            canvas.drawTextLine(SRInt, 0, SRInt.getHeight() - SRInt.getXHeight(), colorWhite);
            canvas.translate(SRInt.getWidth(),8);
            canvas.drawTextLine(SRDec, 0, SRDec.getHeight() - SRDec.getXHeight(), colorWhite);
        } else { //舍去输出
            canvas.translate(160 - SRInt2.getWidth()/ 2,50);
            canvas.drawTextLine(SRInt2, 0, SRInt2.getHeight() - SRInt2.getXHeight(), colorWhite);
        }
        canvas.restore();

        //画游戏模式
        canvas.save();
        canvas.translate(47,44);
        canvas.drawTextLine(luMode, 0, luMode.getHeight() - luMode.getXHeight(), colorWhite);
        //这里应该需要获取谱面“加mod“后的星数难度，需要getBeatmapAttribute什么的
        canvas.restore();
    }

    private void drawLeftMapSR(BeatMap beatMap) {
        Image Star = null;
        try {
            Star = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-beatmap-star.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Float SR = beatMap.getDifficultyRating();

        canvas.save();
        canvas.translate(40,105);

        if (SR > 10f) SR = 10f;

        while (SR >= 1f){
            canvas.drawImage(Star,0,0,new Paint());
            canvas.translate(0,35);
            SR -= 1f;
        }

        if (SR > 0f) {
            canvas.translate(0,35);
            SkiaCanvasUtil.drawScaleImage(canvas, Star, 1 - SR, 1 - SR, SR, SR);
        }
        canvas.restore();
    }

    private void drawRightRRect(BeatMap beatMap) {
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();

        Font fontS24 = new Font(TorusSB, 24);

        String MapFavorite = DataUtil.getRoundedNumberStr(beatMap.getBeatMapSet().getFavourite(), 1) +
                DataUtil.getRoundedNumberUnit(beatMap.getBeatMapSet().getFavourite(), 1);
        String MapPlayCount = DataUtil.getRoundedNumberStr(beatMap.getPlaycount(),1) +
                DataUtil.getRoundedNumberUnit(beatMap.getPlaycount(),1);

        TextLine MapFav = TextLine.make(MapFavorite, fontS24);
        TextLine MapPC = TextLine.make(MapPlayCount, fontS24);

        Image MapFavImage = null;
        Image MapPCImage = null;
        try {
            MapFavImage = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-beatmap-favorite.png"));
            MapPCImage = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-beatmap-playcount.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String MapStatusStr = beatMap.getStatus();
        Image MapStatusImage;

        switch (MapStatusStr){
            case "ranked": MapStatusImage = PanelUtil.OBJECT_MAPSTATUS_RANKED; break;
            case "qualified": MapStatusImage = PanelUtil.OBJECT_MAPSTATUS_QUALIFIED; break;
            case "loved": MapStatusImage = PanelUtil.OBJECT_MAPSTATUS_LOVED; break;
            case "unranked", default: MapStatusImage = PanelUtil.OBJECT_MAPSTATUS_UNRANKED;
        }

        //画底层圆角矩形
        canvas.save();
        canvas.translate(670,40);
        canvas.drawRRect(RRect.makeXYWH(0,0,190,60,20),colorRRect);
        canvas.restore();

        //画谱面状态
        canvas.save();
        canvas.translate(684,45);
        canvas.drawImage(MapStatusImage,0,0,new Paint());
        canvas.restore();

        //画图标
        canvas.save();
        canvas.translate(746,48);
        canvas.drawImage(MapFavImage,0,0,new Paint());
        canvas.translate(0,26);
        canvas.drawImage(MapPCImage,0,0,new Paint());
        canvas.restore();

        //画数据
        canvas.save();
        canvas.translate(840 - MapFav.getWidth(),47);
        canvas.drawTextLine(MapFav, 0, MapFav.getHeight() - MapFav.getXHeight(), colorWhite);
        canvas.translate(MapFav.getWidth() - MapPC.getWidth(),27);
        canvas.drawTextLine(MapPC, 0, MapPC.getHeight() - MapPC.getXHeight(), colorWhite);
        canvas.restore();

    }
    private void drawBeatMapHexagon(BeatMap beatMap) {
        //创建一个正六边形，尖角朝上，选最上面那个尖角作为原点。
        int size = 220; //这是六边形相对（邻，间，对）点距离的一半，也是一边的边长（相邻点）。

        float[] ponX = new float[6];
        float[] ponY = new float[6];
        ponX[0] = 0f;
        ponY[0] = 0f;
        ponX[1] = size * 0.886f;
        ponY[1] = size * 0.5f;
        ponX[2] = size * 0.886f;
        ponY[2] = size * 1.5f;
        ponX[3] = 0f;
        ponY[3] = size * 2f;
        ponX[4] = size * -0.886f;
        ponY[4] = size * 1.5f;
        ponX[5] = size * -0.886f;
        ponY[5] = size * 0.5f;

        var HexagonPath = new org.jetbrains.skija.Path();

        HexagonPath.moveTo(ponX[0], ponY[0]);
        HexagonPath.lineTo(ponX[1], ponY[1]);
        HexagonPath.lineTo(ponX[2], ponY[2]);
        HexagonPath.lineTo(ponX[3], ponY[3]);
        HexagonPath.lineTo(ponX[4], ponY[4]);
        HexagonPath.lineTo(ponX[5], ponY[5]);
        HexagonPath.closePath();

        Image beatMapBG = null;
        Image beatMapDefaultBG = null;
        Image beatMapHexagon = null;

        try {
            beatMapBG = SkiaImageUtil.getImage(beatMap.getBeatMapSet().getCovers().getList2x());
            beatMapDefaultBG = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-beatmap-mask.png"));
            beatMapHexagon = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-beatmap-hexagon.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }


        canvas.save();
        canvas.translate(252,104);
        canvas.drawImage(beatMapDefaultBG,0,0,new Paint());
        canvas.restore();

        canvas.save();
        canvas.translate(440,94);
        canvas.clipPath(HexagonPath);
        canvas.translate(-200,0); // 170 - 30, 多出去一点
        canvas.drawImage(SkiaImageUtil.getScaleCenterImage(beatMapBG,380,420),0,0,new Paint());//实测是370-400左右，放大一点好
        canvas.restore();

        canvas.save();
        canvas.translate(230,82);
        canvas.drawImage(beatMapHexagon,0,0,new Paint());
        canvas.restore();
    }

    private void drawBeatMapInfo(BeatMap beatMap) {
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Typeface PUHUITISB = SkiaUtil.getPUHUITIMedium();

        Font fontS24 = new Font(TorusSB, 24);
        Font fontS36 = new Font(TorusSB, 36);
        Font fontS48 = new Font(TorusSB, 48);
        Font fontP36 = new Font(PUHUITISB, 36);

        String J1t = beatMap.getBeatMapSet().getTitle();
        String J2t = beatMap.getBeatMapSet().getTitleUTF();
        String J3t = beatMap.getVersion();

        //计算字符长度，最好写个公共方法，这里先往这个方法里放着，本质是把Artist缩短，字符最大宽度600

        StringBuilder sb = new StringBuilder();
        var titleChar = beatMap.getBeatMapSet().getArtist().toCharArray();//这个可能会超长，需要优先缩短

        int maxWidth = 600;
        float allWidth = 0;
        int backL = 0;

        for (var thisChar : titleChar) {
            if (allWidth > maxWidth){
                break;
            }
            sb.append(thisChar);
            if ((allWidth) < maxWidth){
                backL++;
            }
        }
        if (allWidth > maxWidth){
            sb.delete(backL,sb.length());
            sb.append("...");
        }

        String J4t1 = sb.toString();

        sb.delete(0,sb.length());
        allWidth = 0;
        backL = 0;

        String J4t2 = beatMap.getBeatMapSet().getCreator();
        String J4t3 = String.valueOf(beatMap.getId());

        StringBuilder J4t = new StringBuilder()
                .append(J4t1)
                .append(" - ")
                .append(J4t2)
                .append(" - b")
                .append(J4t3);

        //歌曲名，歌曲名UTF，谱面难度，曲师 - 谱师 - BID
        TextLine J1 = TextLine.make(J1t, fontS48);
        TextLine J2 = TextLine.make(J2t, fontP36);
        TextLine J3 = TextLine.make(J3t, fontS36);
        TextLine J4 = TextLine.make(String.valueOf(J4t), fontS24);

        canvas.save();
        canvas.translate(440 - J1.getWidth() / 2,560);
        canvas.drawTextLine(J1, 0, J1.getHeight() - J1.getXHeight(), colorWhite); //y:850

        //如果没有Unicode曲名，则省略J2
        if (Objects.equals(J1t, J2t)){
            canvas.translate(J1.getWidth() - J3.getWidth() / 2,130); //y:980
        } else{
            canvas.translate((J1.getWidth() - J2.getWidth()) / 2,55);
            canvas.drawTextLine(J2, 0, J2.getHeight() - J2.getXHeight(), colorWhite); //y:905
            canvas.translate((J2.getWidth() - J3.getWidth()) / 2,75);
        }

        canvas.drawTextLine(J3, 0, J3.getHeight() - J3.getXHeight(), colorWhite); //y:980
        canvas.translate((J3.getWidth() - J4.getWidth()) / 2,40);
        canvas.drawTextLine(J4, 0, J4.getHeight() - J4.getXHeight(), colorWhite); //y:1020
        canvas.restore();
    }
    public Image build() {
        return super.build(20);
    }
}
