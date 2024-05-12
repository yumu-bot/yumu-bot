package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.PPPlus;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuPPPlusApiService;
import com.now.nowbot.throwable.ServiceException.PPPlusException;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Objects;

@Service("PP_PLUS")
public class PPPlusService implements MessageService<PPPlusService.PPPlusParam> {
    private static final Logger log = LoggerFactory.getLogger(PPPlusService.class);
    @Resource
    OsuBeatmapApiService beatmapApiService;
    @Resource
    OsuPPPlusApiService ppPlusApiService;
    @Resource
    BindDao bindDao;
    @Resource
    ImageService imageService;

    public record PPPlusParam(long bid, OsuMode mode) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<PPPlusParam> data) throws Throwable {
        var matcher = Instructions.PP_PLUS.matcher(messageText);
        if (! matcher.find()) return false;

        /*
        long bid;

        try {
            bid = Long.parseLong(matcher.group("bid"));
        } catch (NumberFormatException e) {
            throw new PPPlusException(PPPlusException.Type.PL_Map_BIDParseError);
        }

        OsuMode mode = OsuMode.getMode(matcher.group("mode"));

        data.setValue(new PPPlusParam(bid, mode));
        return true;

         */
        return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, PPPlusParam param) throws Throwable {
        var from = event.getSubject();

        BeatMap beatMap = getBeatMap(param);

        // todo 按道理说 pp+ 是四模式均支持的！
        if (OsuMode.getMode(beatMap.getMode()) != OsuMode.OSU) throw new PPPlusException(PPPlusException.Type.PL_Function_NotSupported);

        PPPlus plus = getBeatMapPPPlus(beatMap, param.mode);

        byte[] image;

        try {
            image = imageService.getPanelB3(beatMap, plus);
        } catch (Exception e) {
            log.error("PP+ 渲染失败", e);
            throw new PPPlusException(PPPlusException.Type.PL_Render_Error);
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("PP+ 发送失败", e);
            throw new PPPlusException(PPPlusException.Type.PL_Send_Error);
        }
    }

    private BeatMap getBeatMap(PPPlusParam param) throws PPPlusException {
        BeatMap beatMap;

        try {
            beatMap = beatmapApiService.getBeatMapInfo(param.bid);
        } catch (WebClientResponseException ignored) {
            try {
                beatMap = beatmapApiService.getBeatMapSetInfo(param.bid).getTopDiff();
                if (Objects.isNull(beatMap)) throw new PPPlusException(PPPlusException.Type.PL_Map_NotFound);
            } catch (WebClientResponseException e) {
                throw new PPPlusException(PPPlusException.Type.PL_Map_NotFound);
            }
        }

        return beatMap;
    }

    private PPPlus getBeatMapPPPlus(BeatMap beatMap, OsuMode mode) throws PPPlusException {

        try {
            return ppPlusApiService.getBeatMapPPPlus(beatMap.getId(), beatMap.hasLeaderBoard(), mode);
        } catch (RuntimeException e) {
            log.error("PP+：获取失败");
            throw new PPPlusException(PPPlusException.Type.PL_Fetch_APIConnectFailed);
        }

    }
}
