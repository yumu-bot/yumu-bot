package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.beatmapParse.OsuFile;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.imag.MapAttrGet;
import com.now.nowbot.model.ppminus3.PPMinus3;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.throwable.ServiceException.MapMinusException;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("MAP_MINUS")
public class MapMinusService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(MapMinusService.class);
    @Resource
    OsuBeatmapApiService beatmapApiService;
    @Resource
    ImageService imageService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instructions.MAP_MINUS.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        long bid;
        double rate;
        OsuMode mode;
        String fileStr;
        BeatMap beatMap;

        int modsValue = Mod.getModsValue(matcher.group("mod"));
        boolean isChangedRating = Mod.hasChangeRating(modsValue);


        try {
            bid = Long.parseLong(matcher.group("id"));
        } catch (NumberFormatException e) {
            throw new MapMinusException(MapMinusException.Type.MM_Bid_Error);
        }

        try {
            rate = Double.parseDouble(matcher.group("rate"));
        } catch (NumberFormatException e) {
            throw new MapMinusException(MapMinusException.Type.MM_Rate_Error);
        } catch (NullPointerException e) {
            rate = 1d;
        }

        if (rate < 0.1d) throw new MapMinusException(MapMinusException.Type.MM_Rate_TooSmall);
        if (rate > 5d) throw new MapMinusException(MapMinusException.Type.MM_Rate_TooLarge);


        try {
            beatMap = beatmapApiService.getBeatMapInfo(bid);
            mode = OsuMode.getMode(beatMap.getModeInt());

            //todo 很复杂的 获取变化星级的谱面的方式。感觉以后这种事情不要交给绘图面板做，应该写进 Beatmap 或者相关的 util 类里。

            if (isChangedRating) {
                var mapAttrGet = new MapAttrGet(mode);
                mapAttrGet.addMap(beatMap.getSID(), beatMap.getId(), modsValue, beatMap.getRanked());
                var changedAttrsMap = imageService.getMapAttr(mapAttrGet);

                var attr = changedAttrsMap.get(beatMap.getSID().longValue());
                beatMap.setStarRating(attr.getStars());
                beatMap.setBPM(attr.getBpm());
                beatMap.setAR(attr.getAr());
                beatMap.setCS(attr.getCs());
                beatMap.setOD(attr.getOd());
                beatMap.setHP(attr.getHp());
                if (Mod.hasDt(modsValue)) {
                    beatMap.setTotalLength(Math.round(beatMap.getTotalLength() / 1.5f));
                } else if (Mod.hasHt(modsValue)) {
                    beatMap.setTotalLength(Math.round(beatMap.getTotalLength() / 0.75f));
                }
            }


            fileStr = beatmapApiService.getBeatMapFile(bid);
            //fileStr = Files.readString(Path.of("/home/spring/DJ SHARPNEL - BLUE ARMY (Raytoly's Progressive Hardcore Sped Up Edit) (Critical_Star) [Insane].osu"));
        } catch (Exception e) {
            throw new MapMinusException(MapMinusException.Type.MM_Map_NotFound);
        }

        OsuFile file = null;
        if (mode != null) {
            try {
                switch (mode) {
                    case MANIA -> file = OsuFile.getInstance(fileStr);
                    default -> throw new MapMinusException(MapMinusException.Type.MM_Function_NotSupported);
                    //throw new TipsException("抱歉，本功能暂不支持除Mania模式以外的谱面！");//file = new OsuFile(fileStr);
                }

            } catch (NullPointerException e) {
                throw new MapMinusException(MapMinusException.Type.MM_Map_FetchFailed);
            }
        }

        PPMinus3 mapMinus = null;
        if (file != null) {
            mapMinus = PPMinus3.getInstance(file, isChangedRating ? Mod.getModsClockRate(modsValue) : rate);
        }

        byte[] image;

        try {
            image = imageService.getPanelB2(beatMap, mapMinus);
        } catch (Exception e) {
            log.error("谱面 Minus：渲染失败", e);
            throw new MapMinusException(MapMinusException.Type.MM_Render_Error);
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("谱面 Minus：发送失败", e);
            throw new MapMinusException(MapMinusException.Type.MM_Send_Error);
        }
    }
}
