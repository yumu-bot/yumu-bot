package com.now.nowbot.service.MessageService;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.osufile.OsuFile;
import com.now.nowbot.model.osufile.OsuFileMania;
import com.now.nowbot.model.ppminus3.MapMinus;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.LeaderBoardException;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;

@Service("MapMinus")
public class MapMinusService implements MessageService{

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
        int bid = 0;
        OsuMode mode = null;
        String fileStr = null;

        try {
            bid = Integer.parseInt(matcher.group("id"));
            mode = OsuMode.getMode(osuGetService.getBeatMapInfo(bid).getModeInt());
            fileStr = osuGetService.getBeatMapFile(bid, mode.getName());
//            fileStr = Files.readString(Path.of("/home/spring/DJ SHARPNEL - BLUE ARMY (Raytoly's Progressive Hardcore Sped Up Edit) (Critical_Star) [Insane].osu"));
        } catch (Exception e) {

        }

        OsuFile file = null;
        if (mode != null) {
            switch (mode) {
                case MANIA -> file = new OsuFileMania(fileStr);
                default -> throw new TipsException("抱歉，本功能暂不支持除Mania模式以外的谱面！");//file = new OsuFile(fileStr);
            }
        }

        var beatMap = osuGetService.getMapInfoFromDB(bid);
        var mapMinus = MapMinus.getInstance(file);

        try {
            var data = imageService.getPanelB2(beatMap, mapMinus);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            NowbotApplication.log.error("MapMinus", e);
            throw new LeaderBoardException(LeaderBoardException.Type.LIST_Send_Error);
            //from.sendMessage("出错了出错了,问问管理员");
        }
    }
}
