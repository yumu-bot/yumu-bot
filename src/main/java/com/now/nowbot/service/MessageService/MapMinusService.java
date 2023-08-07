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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
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
        OsuMode mode = null;
        String fileStr = null;

        try {
            bid = Integer.parseInt(matcher.group("bid"));
            mode = OsuMode.getMode(osuGetService.getBeatMapInfo(bid).getModeInt());
            fileStr = osuGetService.getBeatMapFile(bid, mode.getName());
//            fileStr = Files.readString(Path.of("/home/spring/DJ SHARPNEL - BLUE ARMY (Raytoly's Progressive Hardcore Sped Up Edit) (Critical_Star) [Insane].osu"));
        } catch (Exception e) {

        }

        OsuFile file;
        switch (mode) {
            case MANIA -> file = new OsuFileMania(fileStr);
            case null,default ->  file = new OsuFile(fileStr);
        }

        var mapMinus = MapMinus.getInstance(file);


        Map<String, Object> requestBody = new HashMap<>();
        // 谱面信息
        requestBody.put("beatmap", osuGetService.getMapInfoFromDB(bid));
        // 你的其他列表
        requestBody.put("ppm", mapMinus);

        // 参数传递直接用这个 `requestBody`
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
