package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.multiplayer.MatchData;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.throwable.ServiceException.MapStatisticsException;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Objects;

@Service("MATCHMAP")
public class MatchMapService implements MessageService<MatchMapService.MatchMapParam> {
    static final Logger log = LoggerFactory.getLogger(MatchMapService.class);

    @Resource
    OsuBeatmapApiService beatmapApiService;

    @Resource
    ImageService imageService;

    @Autowired
    public MatchMapService(OsuBeatmapApiService beatmapApiService, ImageService imageService) {
        this.beatmapApiService = beatmapApiService;
        this.imageService = imageService;
    }

    public record MatchMapParam (Long bid, OsuMode osuMode, MatchData matchData, String modStr) {

    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<MatchMapParam> data) throws Throwable {
        //这个只能通过父服务 MatchListenerService 调用得到
        return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, MatchMapService.MatchMapParam param) throws Throwable {
        var from = event.getSubject();

        if (param.bid == 0) from.sendMessage(MapStatisticsException.Type.M_Parameter_None.message);

        var beatMap = new BeatMap();

        try {
            beatMap = beatmapApiService.getBeatMapInfo(param.bid);
        } catch (HttpClientErrorException.NotFound e) {
            from.sendMessage(MapStatisticsException.Type.M_Map_NotFound.message);
        } catch (Exception e) {
            from.sendMessage(MapStatisticsException.Type.M_Fetch_Error.message);
        }

        // 标准化 combo
        int combo;

        {
            Integer maxCombo = beatMap.getMaxCombo();

            if (Objects.nonNull(maxCombo)) {
                combo = maxCombo;
            } else {
                combo = 99999;
            }
        }


        //只有转谱才能赋予游戏模式
        OsuMode mode;
        {
            if (! (param.osuMode.equals(OsuMode.DEFAULT)) && OsuMode.getMode(beatMap.getMode()).equals(OsuMode.OSU)) {
                mode = param.osuMode;
            } else {
                mode = OsuMode.getMode(beatMap.getMode());
            }
        }

        List<String> mods = null;
        if (Objects.nonNull(param.modStr)) {
            mods = Mod.getModsList(param.modStr).stream().map(Mod::getAbbreviation).toList();
        }

        var expected = new MapStatisticsService.Expected(mode, 1d, combo, 0, mods);

        try {
            var data = imageService.getPanelE3(param.matchData, beatMap, expected);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            log.error("Map 发送失败: ", e);
            from.sendMessage(MapStatisticsException.Type.M_Send_Error.message);
        }
    }
}
