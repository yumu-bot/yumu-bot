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

import java.util.HashMap;
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


        Map<String, Object> requestBody = new HashMap<>();
        // 谱面信息
        requestBody.put("beatmap", osuGetService.getMapInfoFromDB(bid));
        // 你的其他列表
        List<Double> stream = null; // 替换成数据,比如 xxx.toList()
        // 如果是数组也一样,但是把 `List<Double>` 声明改成数组,list与 数组 转换成的json是一样的
        // double[] stream = xxx;
        requestBody.put("stream", stream);

        // 其他的一样,这里用 字符串数组 举例
        String[] huhuhu = new String[]{"huhuhu1", "huhuhu2", "huhuhu3"};
        requestBody.put("hu", huhuhu);

        /**
         这里的requestBody 转成json就是
         {
         "beatmap" :{
            //beatmap结构
         },
         "stream": [],
         "hu": ["huhuhu1","huhuhu2","huhuhu3"]
         }
         */

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
