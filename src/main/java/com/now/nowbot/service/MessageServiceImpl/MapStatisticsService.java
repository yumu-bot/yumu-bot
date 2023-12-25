package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.ServiceException.MapStatisticsException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Objects;

@Service("MAP")
public class MapStatisticsService implements MessageService<MapStatisticsService.MapParam> {
    private static final Logger log = LoggerFactory.getLogger(MapStatisticsService.class);
    OsuBeatmapApiService beatmapApiService;
    OsuUserApiService osuUserApiService;
    BindDao bindDao;
    ImageService imageService;
    @Autowired
    public MapStatisticsService(OsuBeatmapApiService beatmapApiService, OsuUserApiService osuUserApiService, BindDao bindDao, ImageService imageService) {
        this.beatmapApiService = beatmapApiService;
        this.osuUserApiService = osuUserApiService;
        this.bindDao = bindDao;
        this.imageService = imageService;
    }

    public record MapParam (Long bid, OsuMode osuMode, Double accuracy, Double combo, Integer miss, String modStr) {

    }

    public record Expected (Double accuracy, Integer combo, Integer miss, List<String> mods) {

    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<MapParam> data) {
        var matcher = Instructions.MAP.matcher(event.getRawMessage().trim());
        if (!matcher.find()) return false;

        var mode = OsuMode.getMode(matcher.group("mode"));

        long bid;
        double accuracy;
        double combo;
        int miss;

        try {
            bid = Long.parseLong(matcher.group("bid"));
        } catch (RuntimeException e) {
            bid = 0;
        }

        try {
            accuracy = Double.parseDouble(matcher.group("accuracy"));
        } catch (RuntimeException e) {
            accuracy = 1;
        }

        try {
            combo = Double.parseDouble(matcher.group("combo"));
        } catch (RuntimeException e) {
            combo = 1;
        }

        try {
            miss = Integer.parseInt(matcher.group("miss"));
        } catch (RuntimeException e) {
            miss = 0;
        }

        var modStr = matcher.group("mod");

        data.setValue(new MapParam(bid, mode, accuracy, combo, miss, modStr));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, MapParam param) throws Throwable {
        var from = event.getSubject();

        if (param.bid == 0)
            throw new MapStatisticsException(MapStatisticsException.Type.M_Parameter_None);

        BinUser binUser;
        OsuUser osuUser;

        var qq = event.getSender().getId();
        try {
            binUser = bindDao.getUserFromQQ(qq);
            try {
                osuUser = osuUserApiService.getPlayerInfo(binUser);
            } catch (WebClientResponseException e) {
                osuUser = new OsuUser();
                osuUser.setBot(true);
                osuUser.setUsername("YumuBot");
                //throw new MapStatisticsException(MapStatisticsException.Type.M_Me_NotFound);
            }
        } catch (BindException e) {
            //传null过去，让面板生成一个默认的 card A1
            osuUser = new OsuUser();
            osuUser.setBot(true);
            osuUser.setUsername("YumuBot");
            //throw new MapStatisticsException(MapStatisticsException.Type.M_Me_TokenExpired);
        } catch (Exception e) {
            log.error("M：", e);
            throw new MapStatisticsException(MapStatisticsException.Type.M_Fetch_Error);
        }

        var beatMap = new BeatMap();

        try {
            beatMap = beatmapApiService.getBeatMapInfo(param.bid);
        } catch (Exception e) {
            throw new MapStatisticsException(MapStatisticsException.Type.M_Map_NotFound);
        }

        // 标准化 acc 和 combo
        int combo;
        double acc;
        int miss = param.miss;

        {
            var maxCombo = beatMap.getMaxCombo();
            if (param.combo > 0 && param.combo <= 1) {
                combo = Math.toIntExact(Math.round(maxCombo * param.combo));
            } else if (param.combo > 1) {
                combo = Math.min(Math.toIntExact(Math.round(param.combo)), maxCombo);
            } else {
                throw new MapStatisticsException(MapStatisticsException.Type.M_Parameter_ComboError);
            }
        }

        {
            if (param.accuracy > 0 && param.accuracy <= 1) {
                acc = param.accuracy;
            } else if (param.accuracy > 1 && param.accuracy <= 100) {
                acc = param.accuracy / 100d;
            } else if (param.accuracy > 100 && param.accuracy <= 10000) {
                acc = param.accuracy / 10000d;
            } else {
                throw new MapStatisticsException(MapStatisticsException.Type.M_Parameter_AccuracyError);
            }
        }

        List<String> mods = null;
        if (Objects.nonNull(param.modStr)) {
            mods = Mod.getModsList(param.modStr).stream().map(Mod::getAbbreviation).toList();
        }

        var expected = new Expected(acc, combo, miss, mods);

        try {
            var data = imageService.getPanelE2(osuUser, beatMap, expected);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            log.error("Map 发送失败: ", e);
            throw new MapStatisticsException(MapStatisticsException.Type.M_Send_Error);
        }
    }
}

