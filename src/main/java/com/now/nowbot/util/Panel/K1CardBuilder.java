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
        Typeface ExtraSB = SkiaUtil.getEXTRA();
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

        float SR = beatMap.getDifficultyRating();
        int SRIntI = (int) Math.floor(SR);
        float SRDecI = SR - SRIntI;

        String SRIntStr = String.format("%d", SRIntI);
        String SRDecStr = String.format("%.2f", SRDecI);

        TextLine SRInt = TextLine.make(SRIntStr, fontS48); // 3.
        TextLine SRDec = TextLine.make(SRDecStr.substring(SRDecStr.indexOf('.'), SRDecStr.indexOf('.')+2), fontS36); // (.)46
        TextLine SRInt2 = TextLine.make(SRIntStr, fontS48);// 这个是 dec 为 0 的时候输出的

        //画底层圆角矩形
        canvas.save();
        canvas.translate(40,40);
        canvas.drawRRect(RRect.makeXYWH(0,0,190,60,20),colorRRect);
        canvas.restore();

        //画星数
        canvas.save();
        if (SRDecI >= 0.005f){ // 正常输出
            canvas.translate(160 - (SRInt.getWidth() + SRDec.getWidth()) / 2,50);
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
        canvas.translate(47,78); //本来是 47 44 但是实测好像还要再往下 34px
        canvas.drawTextLine(luMode, 0, luMode.getHeight() - luMode.getXHeight(), colorWhite);
        //这里应该需要获取谱面“加mod“后的星数难度，需要getBeatmapAttribute什么的
        //这里还需要获取谱面难度，并上色 color
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
        int SRInt = (int) Math.floor(SR); //给最小的那颗星星做y偏移用的

        canvas.save();
        canvas.translate(40,105);

        if (SR > 10f) SR = 10f;

        while (SR >= 1f){
            canvas.drawImage(Star,0,0,new Paint());
            canvas.translate(0,35);
            SR -= 1f;
        }

        if (SR > 0f) {
            var x = (1 - SR)/2f * 39f + 40;
            var y = (1 - SR)/2f * 39f + SRInt * 35 + 105;
            SkiaCanvasUtil.drawScaleImage(canvas, Star, x, y, SR * 39f, SR * 39f); //Star 图片宽 39x39
        }
        canvas.restore();
    }

    private void drawRightRRect(BeatMap beatMap) {
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();

        Font fontS24 = new Font(TorusSB, 24);

        String MapFavorite = DataUtil.getRoundedNumberStr(beatMap.getBeatMapSet().getFavourite(), 1);
        String MapPlayCount = DataUtil.getRoundedNumberStr(beatMap.getPlaycount(),1);

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
        canvas.translate(840 - MapFav.getWidth(),46);
        canvas.drawTextLine(MapFav, 0, MapFav.getHeight() - MapFav.getXHeight(), colorWhite);
        canvas.translate(MapFav.getWidth() - MapPC.getWidth(),28);
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

        String J1Str = SkiaUtil.getShortenStr(beatMap.getBeatMapSet().getTitle(), 840);// TitleStr
        String J2Str = SkiaUtil.getShortenStr(beatMap.getBeatMapSet().getTitleUTF(), 840);// TitleUnicodeStr
        String J3Str = SkiaUtil.getShortenStr(beatMap.getVersion(), 840);// DifficultyStr

        String J4Str1 = SkiaUtil.getShortenStr(beatMap.getBeatMapSet().getArtist(), 600);// ArtistStr

        StringBuilder J4sb = new StringBuilder()
                .append(J4Str1)
                .append(" - ")
                .append(beatMap.getBeatMapSet().getCreator()) // J4Str2
                .append(" - b")
                .append(beatMap.getId()); // J4Str3

        //歌曲名，歌曲名UTF，谱面难度，曲师 - 谱师 - BID
        TextLine J1 = TextLine.make(J1Str, fontS48);
        TextLine J2 = TextLine.make(J2Str, fontP36);
        TextLine J3 = TextLine.make(J3Str, fontS36);
        TextLine J4 = TextLine.make(String.valueOf(J4sb), fontS24);

        canvas.save();
        canvas.translate(440 - J1.getWidth() / 2,560);
        canvas.drawTextLine(J1, 0, J1.getHeight() - J1.getXHeight(), colorWhite); //y:850

        //如果没有Unicode曲名，则省略J2
        if (Objects.equals(J1Str, J2Str)){
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
