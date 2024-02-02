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
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.ServiceException.MapStatisticsException;
import com.now.nowbot.util.Instructions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service("MAP")
public class MapStatisticsService implements MessageService<MapStatisticsService.MapParam> {
    private static final Logger log = LoggerFactory.getLogger(MapStatisticsService.class);
    OsuScoreApiService scoreApiService;
    OsuBeatmapApiService beatmapApiService;
    OsuUserApiService osuUserApiService;
    BindDao bindDao;
    ImageService imageService;
    @Autowired
    public MapStatisticsService(OsuBeatmapApiService beatmapApiService, OsuUserApiService osuUserApiService,
                                OsuScoreApiService scoreApiService, BindDao bindDao, ImageService imageService) {
        this.beatmapApiService = beatmapApiService;
        this.osuUserApiService = osuUserApiService;
        this.scoreApiService = scoreApiService;
        this.bindDao = bindDao;
        this.imageService = imageService;
    }

    public record MapParam (Long bid, OsuMode osuMode, Double accuracy, Double combo, Integer miss, String modStr) {

    }

    public record Expected (OsuMode mode, Double accuracy, Integer combo, Integer miss, List<String> mods) {

    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<MapParam> data) {
        var matcher = Instructions.MAP.matcher(messageText);
        if (!matcher.find()) return false;

        var mode = OsuMode.getMode(matcher.group("mode"));

        long bid;
        double accuracy;
        double combo;
        int miss;

        try {
            bid = Long.parseLong(matcher.group("bid"));
        } catch (RuntimeException e) {
            bid = 0L;
        }

        try {
            accuracy = Double.parseDouble(matcher.group("accuracy"));
        } catch (RuntimeException e) {
            accuracy = 1d;
        }

        try {
            combo = Double.parseDouble(matcher.group("combo"));
        } catch (RuntimeException e) {
            combo = 1d;
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

        BeatMap beatMap;
        int combo;
        double acc;
        OsuMode mode;

        Expected expected;

        BinUser binUser;
        Optional<OsuUser> osuUser;

        var qq = event.getSender().getId();
        try {
            binUser = bindDao.getUserFromQQ(qq);
            try {
                osuUser = Optional.of(osuUserApiService.getPlayerInfo(binUser, binUser.getMode()));
            } catch (WebClientResponseException e) {
                osuUser = Optional.empty();
            }
        } catch (BindException e) {
            //传null过去，让面板生成一个默认的 card A1
            osuUser = Optional.empty();
        } catch (Exception e) {
            log.error("谱面信息：无法生成空对象", e);
            throw new MapStatisticsException(MapStatisticsException.Type.M_Fetch_Error);
        }

        //没有bid，且有绑定
        if (param.bid == 0 && osuUser.isPresent()) {

            try {
                var score = scoreApiService.getRecentIncludingFail(osuUser.get().getUID(), osuUser.get().getOsuMode(), 0,1).getFirst();
                beatMap = beatmapApiService.getBeatMapInfo(score.getBeatMap().getId());
                expected = new Expected(score.getMode(), score.getAccuracy(), score.getMaxCombo(), score.getStatistics().getCountMiss(), score.getMods());

            } catch (Exception ignored) {
                try {
                /*
                var md = DataUtil.getMarkdownFile("Help/maps.md");
                var image = imageService.getPanelA6(md, "help");
                 */
                    var image = imageService.getPanelA6(MapStatisticsException.Type.M_Instructions.message, "help");
                    from.sendImage(image);
                    return;
                } catch (Exception e) {
                    throw new MapStatisticsException(MapStatisticsException.Type.M_Instructions);
                }
            }

        } else {
            try {
                beatMap = beatmapApiService.getBeatMapInfo(param.bid);
            } catch (WebClientResponseException e) {
                throw new MapStatisticsException(MapStatisticsException.Type.M_Map_NotFound);
            } catch (Exception e) {
                log.error("谱面信息：谱面获取失败", e);
                throw new MapStatisticsException(MapStatisticsException.Type.M_Fetch_Error);
            }


            // 标准化 acc 和 combo

            {
                Integer maxCombo = beatMap.getMaxCombo();

                if (Objects.isNull(maxCombo)) {
                    combo = (int) Math.round(param.combo);
                } else if (param.combo > 0D && param.combo < 1D) {
                    combo = Math.toIntExact(Math.round(maxCombo * param.combo));
                } else if (param.combo == 1D) {
                    combo = maxCombo;
                } else if (param.combo > 1D) {
                    combo = Math.min(Math.toIntExact(Math.round(param.combo)), maxCombo);
                } else {
                    throw new MapStatisticsException(MapStatisticsException.Type.M_Parameter_ComboError);
                }
            }

            {
                if (param.accuracy > 0D && param.accuracy <= 1D) {
                    acc = param.accuracy;
                } else if (param.accuracy > 1D && param.accuracy <= 100D) {
                    acc = param.accuracy / 100d;
                } else if (param.accuracy > 100D && param.accuracy <= 10000D) {
                    acc = param.accuracy / 10000d;
                } else {
                    throw new MapStatisticsException(MapStatisticsException.Type.M_Parameter_AccuracyError);
                }
            }

            //只有转谱才能赋予游戏模式
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

            expected = new Expected(mode, acc, combo, param.miss, mods);
        }

        try {
            var image = imageService.getPanelE2(osuUser, beatMap, expected);
            from.sendImage(image);
        } catch (Exception e) {
            log.error("谱面信息：发送失败", e);
            throw new MapStatisticsException(MapStatisticsException.Type.M_Send_Error);
        }
    }
}

