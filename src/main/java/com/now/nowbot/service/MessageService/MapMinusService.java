package com.now.nowbot.service.MessageService;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.osufile.OsuFile;
import com.now.nowbot.model.ppminus3.MapMinus;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.LeaderBoardException;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
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
        String modeStr = "";
        String fileStr = "";

        try {
            bid = Integer.parseInt(matcher.group("bid"));
            modeStr = osuGetService.getBeatMapInfo(bid).getMode().toLowerCase();
            fileStr = osuGetService.getBeatMapFile(bid, modeStr);
        } catch (Exception e) {

        }

        OsuFile file = new OsuFile(fileStr);
        var mapMinus = MapMinus.getInstance(file);

        List<Map<String, List<Double>>> req = null;


        try {
            //var data = imageService.getPanelA3(beatMap, subScores);
            //QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            NowbotApplication.log.error("Leader", e);
            throw new LeaderBoardException(LeaderBoardException.Type.LIST_Send_Error);
            //from.sendMessage("出错了出错了,问问管理员");
        }
    }
}
