package com.now.nowbot.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.mapper.DrawLogLiteRepository;
import com.now.nowbot.model.enums.DrawGrade;

import java.util.*;

public class DrawConfig {
    public record Config(String name, int weight) {
    }

    public record Card(String name, int weight, String info) {
    }

    public record CardLog(DrawGrade grade, String card) {
    }

    public Map<DrawGrade, Config> gradeConfigMap;
    public Map<DrawGrade, List<Card>> cardList;
    public Map<DrawGrade, List<Double>> cardWeightList;

    private final Map<String, DrawGrade> gradeMap;
    private static final Random random = new Random();

    public DrawConfig(JsonNode jsonData) {
        gradeConfigMap = new HashMap<>();
        gradeMap = new HashMap<>();
        cardList = new HashMap<>();
        cardWeightList = new TreeMap<>();

        for (var grade : DrawGrade.values()) {
            if (jsonData.has(grade.name())) {
                var gradeData = jsonData.get(grade.name());
                var conf = new DrawConfig.Config(
                        gradeData.get("name").asText("no name"),
                        gradeData.get("weight").asInt(100));
                this.gradeConfigMap.put(grade, conf);
                if (gradeData.get("cards").isArray() && !gradeData.get("cards").isEmpty()) {
                    var cards = new ArrayList<DrawConfig.Card>(gradeData.get("cards").size());
                    int weightSum = 0;
                    for (var cardData : gradeData.get("cards")) {
                        var card = new DrawConfig.Card(
                                cardData.get("name").asText("no name"),
                                cardData.get("weight").asInt(100),
                                cardData.get("info").asText("default")
                        );
                        cards.add(card);
                        this.gradeMap.put(card.info(), grade);
                        weightSum += card.weight;
                    }
                    int thisWeight = 0;
                    var weightList = new ArrayList<Double>(cards.size());
                    for (var card : cards) {
                        weightList.add((double) thisWeight / weightSum);
                        thisWeight += card.weight();
                    }
                    this.cardList.put(grade, cards);
                    this.cardWeightList.put(grade, weightList);
                }
            }
        }
    }

    public DrawGrade getGrade(String cardKey) {
        return gradeMap.get(cardKey);
    }

    /***
     * 随机品级
     * @param uid 玩家
     * @param repository 仓库
     * @return 随机品级
     */
    public DrawGrade getGrade(long uid, DrawLogLiteRepository repository) {
        // 20抽之内出ssr的次数
        int countSSR = repository.getGradeCount(uid, 20, DrawGrade.SSR);
        // 距离上次出ssr,已经抽了多少次
        int countBefSSR = repository.getBeforeCount(uid, DrawGrade.SSR);
        return checkGrade(countBefSSR, countSSR);
    }

    public List<DrawGrade> getGrade10(long uid, DrawLogLiteRepository repository) {
        int countSSR = repository.getGradeCount(uid, 20, DrawGrade.SSR);
        int beforeSSR = repository.getBeforeCount(uid, DrawGrade.SSR);

        var gradeList = new ArrayList<DrawGrade>(10);
        boolean isSRShown = false;
        for (int i = 0; i < 9; i++) {
            var grade = checkGrade(beforeSSR, countSSR);
            if (grade == DrawGrade.SSR) {
                countSSR += 1;
                beforeSSR = 0;
                isSRShown = true;
            } else if (grade == DrawGrade.SR) {
                isSRShown = true;
            }
            gradeList.add(grade);
            beforeSSR++;
        }
        if (!isSRShown) {
            var grade = checkGrade(beforeSSR, countSSR);
            if (grade != DrawGrade.SSR && grade != DrawGrade.SR) grade = DrawGrade.SR;
            gradeList.add(grade);
        }
        return gradeList;
    }

    private DrawGrade checkGrade(int beforeSSR, int countSSR) {
        double chanceSSR = 0.006 + Math.max(0, (beforeSSR - 73) * 0.06D);
        // 20抽之内出现两个 ssr, 必定不不会ssr
        if (countSSR >= 2) chanceSSR = 0;
        // 十连保底在在十连抽做
        double ChanceSR = 0.051;
        double ChanceR = 0.243;

        var r = random.nextDouble();
        if (r - chanceSSR < 0) {
            return DrawGrade.SSR;
        }
        r -= chanceSSR;

        if (r - ChanceSR < 0) {
            return DrawGrade.SR;
        }
        r -= ChanceSR;

        if (r - ChanceR < 0) {
            return DrawGrade.R;
        }
        return DrawGrade.N;
    }

    /***
     * 随机卡片
     * @return 随机卡片
     */
    public Card getCard(DrawGrade grade) {
        var cards = cardWeightList.get(grade);
        int index = find(cards, 0, cards.size()-1, random.nextDouble());
        return cardList.get(grade).get(index);
    }

    private static int find(List<Double> l, int i, int j, double n) {
        if (i == j) {
            if (l.get(i) <= n) {
                return i;
            } else {
                return -1;  // 递归出口
            }
        }
        int mid = (i + j) / 2;
        if (l.get(mid) <= n) {
            if (mid == j || l.get(mid + 1) > n) {
                return mid;
            } else {
                return find(l, mid + 1, j, n);
            }
        } else {
            return find(l, i, mid - 1, n);
        }
    }
}
