package com.now.nowbot.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.mapper.DrawLogLiteRepository;
import com.now.nowbot.model.enums.DrawKind;

import java.util.*;

public class DrawConfig {
    public record Config(String name, int weight) {
    }

    public record Card(String name, int weight, String info) {
    }

    public record CardLog(DrawKind kind, String card) {
    }

    public Map<DrawKind, Config> kindConfig;
    public Map<DrawKind, List<Card>> cardList;
    public Map<DrawKind, List<Double>> cardWeightList;

    private Map<String, DrawKind> kindMap;
    private static final Random random = new Random();

    public DrawConfig(JsonNode jsonData) {
        kindConfig = new HashMap<>();
        kindMap = new HashMap<>();
        cardList = new HashMap<>();
        cardWeightList = new TreeMap<>();

        for (var kind : DrawKind.values()) {
            if (jsonData.has(kind.name())) {
                var kindData = jsonData.get(kind.name());
                var conf = new DrawConfig.Config(
                        kindData.get("name").asText("no name"),
                        kindData.get("weight").asInt(100));
                this.kindConfig.put(kind, conf);
                if (kindData.get("cards").isArray() && kindData.get("cards").size() > 0) {
                    var cards = new ArrayList<DrawConfig.Card>(kindData.get("cards").size());
                    int weightSum = 0;
                    for (var cardData : kindData.get("cards")) {
                        var card = new DrawConfig.Card(
                                cardData.get("name").asText("no name"),
                                cardData.get("weight").asInt(100),
                                cardData.get("info").asText("default")
                        );
                        cards.add(card);
                        this.kindMap.put(card.info(), kind);
                        weightSum += card.weight;
                    }
                    int thisWeight = 0;
                    var weightList = new ArrayList<Double>(cards.size());
                    for (var card : cards) {
                        weightList.add((double) thisWeight / weightSum);
                        thisWeight += card.weight();
                    }
                    this.cardList.put(kind, cards);
                    this.cardWeightList.put(kind, weightList);
                }
            }
        }
    }

    public DrawKind getKind(String cardInfo) {
        return kindMap.get(cardInfo);
    }

    /***
     * 随机品级
     * @param uid
     * @param repository
     * @return
     */
    public DrawKind getRandomKind(long uid, DrawLogLiteRepository repository) {
        // 可能需要的数据: 具体是什么看注释
        int countSSR = repository.getKindCount(uid, 20, DrawKind.SSR);
        int countBefSSR = repository.getBeforCount(uid, DrawKind.SSR);
        return checkKind(countBefSSR, countSSR);
    }

    public List<DrawKind> getRandomKindTenTimes(long uid, DrawLogLiteRepository repository) {
        int countSSR = repository.getKindCount(uid, 20, DrawKind.SSR);
        int countBefSSR = repository.getBeforCount(uid, DrawKind.SSR);

        var datas = new ArrayList<DrawKind>(10);
        boolean srFlag = false;
        for (int i = 0; i < 9; i++) {
            var kind = checkKind(countBefSSR, countSSR);
            if (kind == DrawKind.SSR) {
                countSSR += 1;
                countBefSSR = 0;
                srFlag = true;
            } else if (kind == DrawKind.SR) {
                srFlag = true;
            }
            datas.add(kind);
            countBefSSR++;
        }
        if (!srFlag) {
            var kind = checkKind(countBefSSR, countSSR);
            if (kind != DrawKind.SSR && kind != DrawKind.SR) kind = DrawKind.SR;
            datas.add(kind);
        }
        return datas;
    }

    private DrawKind checkKind(int countBefSSR, int countSSR) {
        double ssrProb = 0.006 + Math.max(0, (countBefSSR - 73) * 0.06D);
        // 20抽之内出现两个 ssr, 必定不不会ssr
        if (countSSR >= 20) ssrProb = 0;
        // 十连保底在在十连抽做
        double srProb = 0.051;
        double rProb = 0.243;

        var randomValue = random.nextDouble();
        if (randomValue - ssrProb < 0) {
            return DrawKind.SSR;
        }
        randomValue -= ssrProb;

        if (randomValue - srProb < 0) {
            return DrawKind.SR;
        }
        randomValue -= srProb;

        if (randomValue - rProb < 0) {
            return DrawKind.R;
        }
        return DrawKind.N;
    }

    /***
     * 随机卡片
     * @return
     */
    public Card getRandomCard(DrawKind kind) {
        var cards = cardWeightList.get(kind);
        int index = find(cards, 0, cards.size()-1, random.nextDouble());
        return cardList.get(kind).get(index);
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
