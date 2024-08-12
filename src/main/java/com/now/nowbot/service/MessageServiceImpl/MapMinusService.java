package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.beatmapParse.OsuFile;
import com.now.nowbot.model.enums.OsuMod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.ppminus3.PPMinus3;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.throwable.ServiceException.MapMinusException;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instruction;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;

import static com.now.nowbot.util.command.CommandPatternStaticKt.FLAG_MOD;

@Service("MAP_MINUS")
public class MapMinusService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(MapMinusService.class);
    @Resource
    OsuBeatmapApiService beatmapApiService;
    @Resource
    ImageService imageService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instruction.MAP_MINUS.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        long bid;
        double rate = 1d;
        OsuMode mode;
        String fileStr;
        BeatMap beatMap;

        var modsList = OsuMod.getModsList(matcher.group(FLAG_MOD));
        boolean isChangedRating = OsuMod.hasChangeRating(modsList);


        try {
            bid = Long.parseLong(matcher.group("bid"));
        } catch (NumberFormatException e) {
            throw new MapMinusException(MapMinusException.Type.MM_Bid_Error);
        }

        if (StringUtils.hasText(matcher.group("rate"))) {
            try {
                rate = Double.parseDouble(matcher.group("rate"));
            } catch (NumberFormatException e) {
                throw new MapMinusException(MapMinusException.Type.MM_Rate_Error);
            }
        }

        if (rate < 0.1d) throw new MapMinusException(MapMinusException.Type.MM_Rate_TooSmall);
        if (rate > 5d) throw new MapMinusException(MapMinusException.Type.MM_Rate_TooLarge);


        try {
            beatMap = beatmapApiService.getBeatMapInfoFromDataBase(bid);
            mode = OsuMode.getMode(beatMap.getModeInt());
            int mods = OsuMod.getModsValue(modsList);
            var s = beatmapApiService.getMaxPP(beatMap.getBeatMapID(), mods);
            beatMap.setStarRating((float) s.getStar());
            DataUtil.applyBeatMapChanges(beatMap, mods);
            fileStr = beatmapApiService.getBeatMapFile(bid);
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
            mapMinus = PPMinus3.getInstance(file, isChangedRating ? OsuMod.getModsClockRate(modsList) : rate);
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
