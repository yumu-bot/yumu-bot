package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.PerformancePlusService;
import com.now.nowbot.throwable.ServiceException.PPPlusException;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("PP_PLUS")
public class PPPlusService implements MessageService<PPPlusService.PPPlusParam> {
    private static final Logger log = LoggerFactory.getLogger(PPPlusService.class);
    @Resource
    OsuScoreApiService     scoreApiService;
    @Resource
    BindDao                bindDao;
    @Resource
    PerformancePlusService performancePlusService;
    @Resource
    ImageService imageService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<PPPlusParam> data) throws Throwable {
        var handle = messageText.equals("+");
        if (! handle) return false;
        var user = bindDao.getUserFromQQ(event.getSender().getId());
        data.setValue(new PPPlusParam(user.getOsuID(), user.getMode()));
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
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, PPPlusParam param) throws Throwable {
        var from = event.getSubject();

        // 不支持其他模式
        if (! param.mode().equals(OsuMode.OSU))
            throw new PPPlusException(PPPlusException.Type.PL_Function_NotSupported);

        var bps = scoreApiService.getBestPerformance(param.uid(), param.mode(), 0, 100);

        var ppPlus = performancePlusService.getScorePerformancePlus(bps);

        double aim = 0;
        double jumpAim = 0;
        double flowAim = 0;
        double precision = 0;
        double speed = 0;
        double stamina = 0;
        double accuracy = 0;
        double total = 0;

        int n = 0;
        for (var ppp : ppPlus) {
            double proportion = Math.pow(0.95, n);
            aim += ppp.getPerformance().aim() * proportion;
            jumpAim += ppp.getPerformance().jumpAim() * proportion;
            flowAim += ppp.getPerformance().flowAim() * proportion;
            precision += ppp.getPerformance().precision() * proportion;
            speed += ppp.getPerformance().speed() * proportion;
            stamina += ppp.getPerformance().stamina() * proportion;
            accuracy += ppp.getPerformance().accuracy() * proportion;
            total += ppp.getPerformance().total() * proportion;
            n++;
        }

        var sb = new StringBuilder("算了算你的pp加\n");
        sb.append("Aim: ").append(aim).append('\n');
        sb.append("JumpAim: ").append(jumpAim).append('\n');
        sb.append("FlowAim: ").append(flowAim).append('\n');
        sb.append("Precision: ").append(precision).append('\n');
        sb.append("Speed: ").append(speed).append('\n');
        sb.append("Stamina: ").append(stamina).append('\n');
        sb.append("Accuracy: ").append(accuracy).append('\n');
        sb.append("Total: ").append(total).append('\n');

        event.getSubject().sendMessage(sb.toString());
/*        try {
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
*/
    }

    public record PPPlusParam(long uid, OsuMode mode) {
    }
/*
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
*/

}
