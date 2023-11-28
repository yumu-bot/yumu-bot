package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.model.beatmapParse.OsuFile;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.ppminus3.MapMinus;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.throwable.ServiceException.MapMinusException;
import com.now.nowbot.util.Pattern4ServiceImpl;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;

@Service("MAPMINUS")
public class MapMinusService implements MessageService<Matcher> {
    OsuBeatmapApiService beatmapApiService;
    RestTemplate template;
    ImageService imageService;


    @Autowired
    public MapMinusService(OsuBeatmapApiService beatmapApiService, RestTemplate template, ImageService image) {
        this.beatmapApiService = beatmapApiService;
        this.template = template;
        imageService = image;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = Pattern4ServiceImpl.MAPMINUS.matcher(event.getRawMessage().trim());
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

        try {
            bid = Long.parseLong(matcher.group("id"));
        } catch (NullPointerException e) {
            throw new MapMinusException(MapMinusException.Type.MM_Bid_Error);
        }

        try {
            mode = OsuMode.getMode(beatmapApiService.getBeatMapInfo(bid).getModeInt());
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

        var beatMap = beatmapApiService.getMapInfoFromDB(bid);
        MapMinus mapMinus = null;
        if (file != null) {
            mapMinus = MapMinus.getInstance(file);
        }

        try {
            var data = imageService.getPanelB2(beatMap, mapMinus);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            NowbotApplication.log.error("MapMinus", e);
            throw new MapMinusException(MapMinusException.Type.MM_Send_Error);
            //from.sendMessage("出错了出错了,问问管理员");
        }
    }
}
