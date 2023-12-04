package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.BeatmapUserScore;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.util.AsyncMethodExecutor;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;


@Service("LEVER_TEST")
public class LeverTestService implements MessageService<BinUser> {
    @Resource
    BindDao            bindDao;
    @Resource
    OsuScoreApiService scoreApiService;
    @Resource
    OsuBeatmapApiService beatmapApiService;


    @Override
    public boolean isHandle(MessageEvent event, DataValue<BinUser> data) throws Throwable {
        if (!event.getRawMessage().equals("测测我的")) {
            return false;
        }
        var qqId = event.getSender().getId();
        var user = bindDao.getUserFromQQ(qqId);
        if (user.getMode() != OsuMode.OSU) {
            event.getSubject().sendMessage("本功能仅支持osu!");
        }
        data.setValue(user);
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, BinUser user) throws Throwable {
        var bp = scoreApiService.getBestPerformance(user, OsuMode.OSU, 0, 100);
        var lastBp = bp.getLast();
        if (lastBp.getPP() < 130 || bp.size() != 100) {
            throw new RuntimeException("你太菜了, 不好评价");
        }
        double data = getLevel(bp, user);
        var b = new MessageChain.MessageChainBuilder();
        b.addAt(event.getSender().getId()).addText(String.format(" 你的评分 %.2f", data));
        event.getSubject().sendMessage(b.build());
    }

    int[] testBpIndex = new int[]{7, 13, 19, 29, 37};
    static final Function<Integer, Double> calculator = (i) -> {
        double r;

        r = 1 + Math.exp((i - 4000D) / 1630);
        r = 112 / r;
        r -= 3;

        return Math.max(0, Math.min(100, r));
    };

    public double getLevel(List<Score> bp, BinUser user) {
        var mapIdSet = new HashSet<Long>();
        bp.forEach(s ->s.setBeatMap(beatmapApiService.getMapInfoFromDB(s.getBeatMap().getId())));
        for (var index : testBpIndex) {
            mapIdSet.add(bp.get(index).getBeatMap().getId());
        }
        bp.stream()
                .filter(s -> !mapIdSet.contains(s.getBeatMap().getId()))
                .sorted(Comparator.comparingInt(Score::getMaxCombo).reversed())
                .limit(5)
                .forEach(s -> mapIdSet.add(s.getBeatMap().getId()));
        bp.stream()
                .filter(s -> !mapIdSet.contains(s.getBeatMap().getId()))
                .sorted(Comparator.comparingDouble(s -> (1 - s.getAccuracy()) + (s.getBeatMap().getMaxCombo() - s.getMaxCombo())))
                .limit(5)
                .forEach(s ->mapIdSet.add(s.getBeatMap().getId()));
        var suppliers = mapIdSet.stream().<AsyncMethodExecutor.Supplier<BeatmapUserScore>>map(bid -> () -> scoreApiService
                .getScore(bid, user, user.getMode())).toList();
        var scores = AsyncMethodExecutor.AsyncSupplier(suppliers);
        double sum = 0;

        for (var s : scores) {
            sum += calculator.apply(s.getPosition());
        }

        return sum;
    }
}
