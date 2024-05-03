package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.beatmapParse.OsuFile;
import com.now.nowbot.model.enums.OsuMode;
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
        OsuMode mode;
        String fileStr;
        BeatMap beatMap;

        try {
            bid = Long.parseLong(matcher.group("id"));
        } catch (NumberFormatException e) {
            throw new MapMinusException(MapMinusException.Type.MM_Bid_Error);
        }

        try {
            beatMap = beatmapApiService.getBeatMapInfo(bid);
            mode = OsuMode.getMode(beatMap.getModeInt());
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
            mapMinus = PPMinus3.getInstanceTest(file);
        }

        try {
            var image = imageService.getPanelB2(beatMap, mapMinus);
            from.sendImage(image);
        } catch (Exception e) {
            log.error("MapMinus", e);
            throw new MapMinusException(MapMinusException.Type.MM_Send_Error);
        }
    }
}
