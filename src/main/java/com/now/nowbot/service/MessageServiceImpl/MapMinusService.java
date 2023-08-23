package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.beatmapParse.OsuFile;
import com.now.nowbot.model.ppminus3.MapMinus;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.MapMinusException;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;

@Service("MapMinus")
public class MapMinusService implements MessageService {

    OsuGetService osuGetService;
    RestTemplate template;
    ImageService imageService;


    @Autowired
    public MapMinusService (OsuGetService osuGetService, RestTemplate template, ImageService image) {
        this.osuGetService = osuGetService;
        this.template = template;
        imageService = image;
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
            mode = OsuMode.getMode(osuGetService.getBeatMapInfo(bid).getModeInt());
            fileStr = osuGetService.getBeatMapFile(bid);
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

        var beatMap = osuGetService.getMapInfoFromDB(bid);
        var mapMinus = MapMinus.getInstance(file);

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
