package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.multiplayer.MatchData;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.GeneralTipsException;
import com.now.nowbot.util.HandleUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service("MATCH_MAP")
public class MatchMapService implements MessageService<MatchMapService.MatchMapParam> {
    private static final Logger log = LoggerFactory.getLogger(MatchMapService.class);

    @Resource
    ImageService imageService;

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

        if (param.bid == 0) return;

        var beatMap = HandleUtil.getOsuBeatMap(param.bid);

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

        var expected = new MapStatisticsService.Expected(mode, 1d, combo, 0, Mod.getModsAbbrList(param.modStr));

        try {
            var image = imageService.getPanelE3(param.matchData, beatMap, expected);
            from.sendImage(image);
        } catch (Exception e) {
            log.error("比赛谱面信息：发送失败: ", e);
            from.sendMessage(new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "比赛谱面信息").getMessage());
        }
    }
}
