package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;


@Service("LEVER_TEST")
public class LeverTestService implements MessageService<BinUser> {
    @Resource
    BindDao            bindDao;
    @Resource
    OsuScoreApiService scoreApiService;


    @Override
    public boolean isHandle(MessageEvent event, DataValue<BinUser> data) throws Throwable {
        if (!event.getRawMessage().equals("测测我的")) {
            return false;
        }
        var qqId = event.getSender().getId();
        var user = bindDao.getUserFromQQ(qqId);
        if (user.getMode() != OsuMode.OSU) {
            throw new RuntimeException("只能查 osu 模式");
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

    }

    int[] testBpIndex = new int[]{7, 13, 19, 29, 37};

    public String getLevel(List<Score> bp) {
        var mapidSet = new HashSet<Long>();
        for (var index : testBpIndex) {
            mapidSet.add(bp.get(index).getBeatMap().getId());
        }
        bp.stream()
                .filter(s -> !mapidSet.contains(s.getBeatMap().getId()))
                .sorted(Comparator.comparingInt(Score::getMaxCombo).reversed())
                .limit(5)
                .forEach(s -> mapidSet.add(s.getBeatMap().getId()));
        bp.stream()
                .filter(s -> !mapidSet.contains(s.getBeatMap().getId()))
                .sorted(Comparator.comparingDouble(s -> (1 - s.getAccuracy()) + (s.getBeatMap().getMaxCombo() - s.getMaxCombo())))
                .limit(5)
                .forEach(s ->mapidSet.add(s.getBeatMap().getId()));

        float scoreRank = 0;

        return "ok";
    }
}
