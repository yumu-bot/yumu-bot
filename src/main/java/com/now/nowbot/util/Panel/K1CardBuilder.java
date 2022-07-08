package com.now.nowbot.util.Panel;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.util.*;
import org.jetbrains.skija.*;

import java.io.IOException;
import java.nio.file.Path;

public class K1CardBuilder extends PanelBuilder {

    Paint colorRRect = new Paint().setARGB(255,56,46,50);
    Paint colorGrey = new Paint().setARGB(255,170,170,170);
    Paint colorWhite = new Paint().setARGB(255,255,255,255);

    public K1CardBuilder(BeatMap beatMap) {
        //这是 pr panel的左侧物件合集，严格来说不算卡片。但写一起方便管理。
        //这个卡片没有作为背景的圆角矩形，是透明的，注意！
        super(880,790);

        drawLeftRRect(beatMap);
        drawLeftMapSR(beatMap);
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

        TextLine luMode = TextLine.make(String.valueOf(beatMap.getModeInt()), ExtraS48);

        //这里应该需要获取谱面“加mod“后的星数难度，需要getBeatmapAttribute什么的
        int SRIntI = (int) Math.floor(beatMap.getDifficultyRating());
        int SRDecI = (int) (beatMap.getDifficultyRating() - Math.floor(beatMap.getDifficultyRating()));

        StringBuilder SRintSB = new StringBuilder()
                .append(SRIntI)
                .append('.');

        TextLine SRInt = TextLine.make(String.valueOf(SRintSB), fontS48);
        TextLine SRDec = TextLine.make(String.valueOf(SRDecI), fontS36);
        TextLine SRInt2 = TextLine.make(String.valueOf(SRIntI), fontS48);//这个是dec为0的时候输出的

        //画底层圆角矩形
        canvas.save();
        canvas.translate(40,40);
        canvas.drawRRect(RRect.makeXYWH(0,0,190,60,20),colorRRect);
        canvas.restore();

        //画星数
        canvas.save();
        if (SRDecI != 0){
            canvas.translate(160 - (SRInt.getWidth() - SRDec.getWidth()) / 2,50);
            canvas.drawTextLine(SRInt, 0, SRInt.getHeight() - SRInt.getXHeight(), colorWhite);
            canvas.translate(SRInt.getWidth(),8);
            canvas.drawTextLine(SRDec, 0, SRDec.getHeight() - SRDec.getXHeight(), colorWhite);
        } else {
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

        while (SR > 1f){
            canvas.drawImage(Star,0,0,new Paint());
            canvas.translate(0,35);
            SR -= 1f;
        }

        if (SR != 0f) {
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
            case "ranked": MapStatusImage = PanelUtil.OBJECT_MAPSTATUS_RANKED;
            case "qualified": MapStatusImage = PanelUtil.OBJECT_MAPSTATUS_QUALIFIED;
            case "loved": MapStatusImage = PanelUtil.OBJECT_MAPSTATUS_LOVED;
            case "unranked": MapStatusImage = PanelUtil.OBJECT_MAPSTATUS_UNRANKED;
            default: MapStatusImage = PanelUtil.OBJECT_MAPSTATUS_UNRANKED;
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
}
