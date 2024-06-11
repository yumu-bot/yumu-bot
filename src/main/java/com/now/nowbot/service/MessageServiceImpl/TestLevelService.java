package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.BeatmapUserScore;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.AsyncMethodExecutor;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;


@Service("TEST_LEVEL")
public class TestLevelService implements MessageService<BinUser> {

    // 娱乐评分
    @Resource
    BindDao            bindDao;
    @Resource
    OsuScoreApiService scoreApiService;
    @Resource
    OsuUserApiService  osuUserApiService;
    @Resource
    OsuBeatmapApiService beatmapApiService;


    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<BinUser> data) throws Throwable {
        if (messageText.equals("测测我的")) {
            var qqId = event.getSender().getId();
            var user = bindDao.getUserFromQQ(qqId);
            if (user.getOsuMode() != OsuMode.OSU) {
                event.getSubject().sendMessage("本功能仅支持osu!");
            }
            user.setOsuMode(OsuMode.OSU);
            data.setValue(user);
            return true;
        } else if (messageText.startsWith("测测他的")) {
            var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
            var name = messageText.substring(4).trim();
            if (at == null && name.isEmpty()) {
                return false;
            }
            BinUser user;
            if (at != null) {
                user = bindDao.getUserFromQQ(at.getTarget());
            } else {
                user = new BinUser();
                user.setOsuID(osuUserApiService.getOsuId(name));
            }
            user.setOsuMode(OsuMode.OSU);
            data.setValue(user);
            return true;
        }

        return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, BinUser user) throws Throwable {
        var bp = scoreApiService.getBestPerformance(user, user.getOsuMode(), 0, 100);
        var lastBp = bp.getLast();
        if (lastBp.getPP() < 100 || bp.size() != 100) {
            throw new TipsException("没测出来, 要不你再刷一下pp?");
        }
        double level = getLevel(bp, user);
        var b = new MessageChain.MessageChainBuilder();
        double s = level > 5 ? (level / 3) : 99;
        b.addAt(event.getSender().getId()).addText(String.format(" 你的评分 %s, 击败%.2f%%的人", getLevelStr(level), s));
        event.getSubject().sendMessage(b.build());
    }

    static final Function<Integer, Double> calculator = (i) -> {
        double r;

        r = 1 + Math.exp((i - 4000D) / 1630);
        r = 112 / r;
        r -= 3;

        return Math.max(0, Math.min(100, r));
    };

    public double getLevel(List<Score> bp, BinUser user) throws TipsException {
        var mapIdSet = new HashSet<Long>();
        bp.forEach(s -> s.setBeatMap(beatmapApiService.getMapInfoFromDB(s.getBeatMap().getId())));
        // 随机取数
        for (var index : getRandomIndex(user.getOsuID())) {
            mapIdSet.add(bp.get(index).getBeatMap().getId());
        }


        bp.stream()
                .filter(s -> ! mapIdSet.contains(s.getBeatMap().getId()))
                .sorted(Comparator.comparingInt(Score::getMaxCombo).reversed())
                .limit(5)
                .forEach(s -> mapIdSet.add(s.getBeatMap().getId()));

        bp.stream()
                .filter(s -> ! mapIdSet.contains(s.getBeatMap().getId()))
                .map(s -> new ScoreLite(s.getMaxCombo(), s.getBeatMap().getMaxCombo() - s.getMaxCombo(),
                        s.getAccuracy(), OsuMod.getModsValueFromAbbrList(s.getMods()), s.getBeatMap().getId()))
                .sorted(Comparator.comparingInt(ScoreLite::diff)
                        .thenComparingInt(ScoreLite::combo).reversed()
                        .thenComparingDouble(ScoreLite::acc).reversed()
                        .thenComparingInt(ScoreLite::mods))
                .limit(5).forEach(s -> mapIdSet.add(s.mapId));

        var suppliers = mapIdSet.stream().<AsyncMethodExecutor.Supplier<BeatmapUserScore>>map(bid -> () -> scoreApiService
                .getScore(bid, user, user.getOsuMode())).toList();
        var scores = AsyncMethodExecutor.AsyncSupplier(suppliers);
        double sum = 0;

        try {
            for (var s : scores) {
                double score = calculator.apply(s.getPosition());
                // acc 修正
                score *= 2 - s.getScore().getAccuracy();
                sum += score;
            }
            sum /= 5;
        } catch (Exception e) {
            throw new TipsException("哎呀计算出错了, 一会再试试");
        }

        return sum;
    }

    private String getLevelStr(double score) {
        if (score > 250) {
            return "X";
        } else if (score > 220) {
            return "SS";
        } else if (score > 200) {
            return "S";
        } else if (score > 170) {
            return "A+";
        } else if (score > 130) {
            return "A";
        } else if (score > 100) {
            return "B";
        } else if (score > 70) {
            return "C";
        } else if (score > 5) {
            return "D";
        } else {
            return "X+";
        }
    }

    private int[] getRandomIndex(long uid) {
        long c = 45648973;
        long m = 81901;
        long a = 143519;
        int[] index = new int[5];
        var set = new HashSet<Integer>();
        for (int i = 0; i < 5; i++) {
            long random = (a * uid + c) % m;
            int tmp = (i * 5) + (int) (random % 5 * (i + 1));
            if (set.add(tmp)) {
                index[i] = tmp;
            } else {
                i--;
            }
        }
        return index;
    }

    private double proportion(double score) {
        return 1 - Math.pow((score - 300) / 300, 2);
    }

    private short getLevel(double score) {
        if (score > 250) {
            return 0;
        } else if (score > 220) {
            return 1;
        } else if (score > 200) {
            return 2;
        } else if (score > 170) {
            return 3;
        } else if (score > 130) {
            return 4;
        } else if (score > 100) {
            return 5;
        } else if (score > 70) {
            return 6;
        } else if (score > 30) {
            return 7;
        } else {
            return 8;
        }
    }

    record ScoreLite(int combo, int diff, double acc, int mods, long mapId) {
    }
}
