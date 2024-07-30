package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.enums.OsuMod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.multiplayer.MatchCalculate;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.throwable.GeneralTipsException;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service("MATCH_MAP")
public class MatchMapService implements MessageService<MatchMapService.MatchMapParam> {
    private static final Logger log = LoggerFactory.getLogger(MatchMapService.class);

    @Resource
    ImageService         imageService;
    @Resource
    OsuBeatmapApiService beatmapApiService;

    public record MatchMapParam(Long bid, OsuMode osuMode, MatchCalculate mc, String modStr) {

    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<MatchMapParam> data) throws Throwable {
        //这个只能通过父服务 MatchListenerService 调用得到
        return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, MatchMapService.MatchMapParam param) throws Throwable {
        var from = event.getSubject();

        if (param.bid == 0) return;

        var beatMap = beatmapApiService.getBeatMapInfo(param.bid);

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
        var beatMapMode = OsuMode.getMode(beatMap.getMode());

        if (beatMapMode != OsuMode.OSU && OsuMode.isDefaultOrNull(param.osuMode())) {
            mode = beatMapMode;
        } else {
            mode = param.osuMode();
        }

        var expected = new MapStatisticsService.Expected(mode, 1d, combo, 0, OsuMod.getModsAbbrList(param.modStr));

        try {
            var image = imageService.getPanelE3(param.mc, beatMap, expected);
            from.sendImage(image);
        } catch (Exception e) {
            log.error("比赛谱面信息：发送失败: ", e);
            from.sendMessage(new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "比赛谱面信息").getMessage());
        }
    }
}
