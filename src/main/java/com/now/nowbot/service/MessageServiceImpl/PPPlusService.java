package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.PerformancePlusService;
import com.now.nowbot.throwable.ServiceException.PPPlusException;
import com.now.nowbot.util.AsyncMethodExecutor;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        var handle = messageText.equals("屁屁加");
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
        int size = ppPlus.size();

        double aim = 0;
        double jumpAim = 0;
        double flowAim = 0;
        double precision = 0;
        double speed = 0;
        double stamina = 0;
        double accuracy = 0;
        double total = 0;
        List<AsyncMethodExecutor.Supplier<String>> suppliers = new ArrayList<>(7);
        Map<String, List<Double>> ppPlusMap = new ConcurrentHashMap<>(7);

        // 逐个排序
        suppliers.add(() -> {
            var stream = ppPlus.stream();
            ppPlusMap.put("aim",
                    stream
                            .map(p -> p.getPerformance().aim())
                            .sorted(Comparator.reverseOrder())
                            .toList()
            );
            return "aim";
        });
        suppliers.add(() -> {
            var stream = ppPlus.stream();
            ppPlusMap.put("jumpAim",
                    stream
                            .map(p -> p.getPerformance().jumpAim())
                            .sorted(Comparator.reverseOrder())
                            .toList()
            );
            return "jumpAim";
        });
        suppliers.add(() -> {
            var stream = ppPlus.stream();
            ppPlusMap.put("flowAim",
                    stream
                            .map(p -> p.getPerformance().flowAim())
                            .sorted(Comparator.reverseOrder())
                            .toList()
            );
            return "flowAim";
        });
        suppliers.add(() -> {
            var stream = ppPlus.stream();
            ppPlusMap.put("precision",
                    stream
                            .map(p -> p.getPerformance().precision())
                            .sorted(Comparator.reverseOrder())
                            .toList()
            );
            return "precision";
        });
        suppliers.add(() -> {
            var stream = ppPlus.stream();
            ppPlusMap.put("speed",
                    stream
                            .map(p -> p.getPerformance().speed())
                            .sorted(Comparator.reverseOrder())
                            .toList()
            );
            return "speed";
        });
        suppliers.add(() -> {
            var stream = ppPlus.stream();
            ppPlusMap.put("stamina",
                    stream
                            .map(p -> p.getPerformance().stamina())
                            .sorted(Comparator.reverseOrder())
                            .toList()
            );
            return "stamina";
        });
        suppliers.add(() -> {
            var stream = ppPlus.stream();
            ppPlusMap.put("accuracy",
                    stream
                            .map(p -> p.getPerformance().accuracy())
                            .sorted(Comparator.reverseOrder())
                            .toList()
            );
            return "accuracy";
        });

        suppliers.add(() -> {
            var stream = ppPlus.stream();
            ppPlusMap.put("jumpAim",
                    stream
                            .map(p -> p.getPerformance().aim())
                            .sorted(Comparator.reverseOrder())
                            .toList()
            );
            return "aim";
        });

        AsyncMethodExecutor.AsyncSupplier(suppliers);

        // 计算加权和
        for (int n = 0; n < size; n++) {
            double proportion = Math.pow(0.95, n);
            aim += ppPlusMap.get("aim").get(n) * proportion;
            jumpAim += ppPlusMap.get("jumpAim").get(n) * proportion;
            flowAim += ppPlusMap.get("flowAim").get(n) * proportion;
            precision += ppPlusMap.get("precision").get(n) * proportion;
            speed += ppPlusMap.get("speed").get(n) * proportion;
            stamina += ppPlusMap.get("stamina").get(n) * proportion;
            accuracy += ppPlusMap.get("accuracy").get(n) * proportion;
        }
        total += (aim + precision + speed + stamina + accuracy);

        var sb = new StringBuilder("掐指算了算你的屁屁加\n");
        sb.append("Aim: ").append(String.format("%.2f", aim)).append('\n');
        sb.append("JumpAim: ").append(String.format("%.2f", jumpAim)).append('\n');
        sb.append("FlowAim: ").append(String.format("%.2f", flowAim)).append('\n');
        sb.append("Precision: ").append(String.format("%.2f", precision)).append('\n');
        sb.append("Speed: ").append(String.format("%.2f", speed)).append('\n');
        sb.append("Stamina: ").append(String.format("%.2f", stamina)).append('\n');
        sb.append("Accuracy: ").append(String.format("%.2f", accuracy)).append('\n');
        sb.append("Total: ").append(String.format("%.2f", total)).append('\n');

        event.getSubject().sendMessage(
                new MessageChain.MessageChainBuilder()
                        .addAt(event.getSender().getId())
                        .addText(sb.toString())
                        .build()
        );
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
