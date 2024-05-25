package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.GeneralTipsException;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.HandleUtil;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("MAP")
public class MapStatisticsService implements MessageService<MapStatisticsService.MapParam> {
    private static final Logger log = LoggerFactory.getLogger(MapStatisticsService.class);
    @Resource
    ImageService imageService;

    public record MapParam(@Nullable OsuUser user, BeatMap beatMap, Expected expected) {}

    public record Expected(OsuMode mode, double accuracy, int combo, int miss, List<String> mods) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<MapParam> data) throws Throwable {
        var matcher = Instructions.MAP.matcher(messageText);
        if (!matcher.find()) return false;

        var beatMap = HandleUtil.getOsuBeatMap(matcher);

        if (beatMap == null) {
            if (HandleUtil.isAvoidance(event, "！m", "!m")) {
                log.info(String.format("指令退避：M 退避成功，被退避的玩家：%s", event.getSender().getName()));
            }
            return false;
        }

        OsuUser user;
        var mode = OsuMode.getMode(matcher.group("mode"));

        try {
            user = HandleUtil.getMyselfUser(event, mode);
        } catch (BindException e) {
            user = null;
        }

        var expected = HandleUtil.getExpectedScore(matcher, beatMap, mode);

        data.setValue(new MapParam(user, beatMap, expected));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, MapParam param) throws Throwable {
        var from = event.getSubject();

        /*
        BinUser binUser;
        Optional<OsuUser> osuUser;

        var qq = event.getSender().getId();
        try {
            binUser = bindDao.getUserFromQQ(qq);
            try {
                osuUser = Optional.of(osuUserApiService.getPlayerInfo(binUser, binUser.getMode()));
            } catch (HttpClientErrorException | WebClientResponseException e) {
                osuUser = Optional.empty();
            }
        } catch (BindException e) {
            //传null过去，让面板生成一个默认的 card A1
            osuUser = Optional.empty();
        } catch (Exception e) {
            log.error("谱面信息：无法生成空对象", e);
            throw new MapStatisticsException(MapStatisticsException.Type.M_Fetch_Error);
        }

         */

        byte[] image;

        try {
            image = imageService.getPanelE2(param.user(), param.beatMap(), param.expected());
            //var image = getImage(param, osuUser);
        } catch (Exception e) {
            log.error("谱面信息：渲染失败", e);
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "谱面信息");
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("谱面信息：发送失败", e);
            throw new GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "谱面信息");
        }
    }

    /*
    public byte[] getImage(MapParam param, Optional<OsuUser> osuUser) throws Exception {

        BeatMap beatMap;
        int combo;
        double acc;
        OsuMode mode;

        Expected expected;


        //没有bid，且有绑定
        if (param.bid == 0 && osuUser.isPresent()) {
            var o = osuUser.get();

            try {
                var score = scoreApiService.getRecentIncludingFail(o.getUID(), o.getOsuMode(), 0, 1).getFirst();
                beatMap = beatmapApiService.getBeatMapInfo(score.getBeatMap().getId());
                expected = new Expected(score.getMode(), score.getAccuracy(), score.getMaxCombo(), score.getStatistics().getCountMiss(), score.getMods());

            } catch (Exception ignored) {
                try {
                // var md = DataUtil.getMarkdownFile("Help/maps.md");
                // var image = imageService.getPanelA6(md, "help");
                    return imageService.getPanelA6(MapStatisticsException.Type.M_Instructions.message, "help");
                } catch (Exception e) {
                    throw new MapStatisticsException(MapStatisticsException.Type.M_Instructions);
                }
            }

        } else {
            try {
                beatMap = beatmapApiService.getBeatMapInfo(param.bid);
            } catch (HttpClientErrorException | WebClientResponseException e) {
                throw new MapStatisticsException(MapStatisticsException.Type.M_Map_NotFound);
            } catch (Exception e) {
                log.error("谱面信息：谱面获取失败", e);
                throw new MapStatisticsException(MapStatisticsException.Type.M_Fetch_Error);
            }




            List<String> mods = null;
            if (Objects.nonNull(param.modStr)) {
                mods = Mod.getModsList(param.modStr).stream().map(Mod::getAbbreviation).toList();
            }

            expected = new Expected(mode, acc, combo, param.miss, mods);
        }
        return imageService.getPanelE2(osuUser, beatMap, expected);
    }
    */
}

