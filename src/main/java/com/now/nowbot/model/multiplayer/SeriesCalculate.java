package com.now.nowbot.model.multiplayer;

import com.now.nowbot.model.JsonData.Match;
import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.model.JsonData.Series;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SeriesCalculate extends MatchCalculate {
    Series series;

    SeriesData seriesData;

    private List<MicroUser> players = new ArrayList<>();

    private final List<Match.MatchRound> rounds = new ArrayList<>();

    private final List<Match.MatchScore> scores = new ArrayList<>();

    public SeriesCalculate(List<Match> matches, List<CalculateParam> params, OsuBeatmapApiService beatmapApiService) {
        this.series = new Series(matches);
        this.beatmapApiService = beatmapApiService;

        if (params.size() < matches.size()) return;

        List<MatchData> dataList = new ArrayList<>(matches.size());
        for (int i = 0; i < matches.size(); i++) {
            var m = matches.get(i);
            var p = params.get(i);

            var c = new MatchCalculate(m, p, beatmapApiService);

            dataList.add(c.getMatchData());
            players.addAll(c.getMatch().getPlayers());
            rounds.addAll(c.getRounds());
            scores.addAll(c.getScores());
        }

        this.players = this.players.stream().distinct().toList();

        this.seriesData = new SeriesData(dataList);
    }

    public class SeriesData {
        public boolean teamVs;

        public double averageStar;

        public long firstMapSID = 0L;

        // 玩家数据图和比分图

        public transient HashMap<Long, PlayerData> playerDataMap = new LinkedHashMap<>();

        public List<PlayerData> playerDataList = new ArrayList<>();

        public final HashMap<String, Integer> teamPointMap  = new LinkedHashMap<>();

        //比赛次数
        public int matchCount;

        // 对局次数，比如 3:5 就是 8 局
        public int roundCount;

        // 玩家数量
        public int playerCount;

        // 分数数量
        public int scoreCount;

        // 用于计算 mra
        private double seriesAMG = 0d;

        private double minMQ = 100d;

        private double scalingFactor;

        public SeriesData(List<MatchData> dataList) {
            if (CollectionUtils.isEmpty(dataList)) return;

            this.teamVs = dataList.getFirst().isTeamVs();
            this.matchCount = dataList.size();

            for (var d : dataList) {
                this.roundCount += d.getRoundCount();
                this.scoreCount += d.getScoreCount();

                this.averageStar += d.averageStar * d.getRoundCount();
            }

            this.averageStar /= this.roundCount;

            this.firstMapSID = dataList.getFirst().firstMapSID;

            // initPlayerDataMap();

            // 合并 playerDataMap
            for (var d : dataList) {
                for (var e : d.getPlayerDataMap().entrySet()) {
                    if (playerDataMap.containsKey(e.getKey())) {
                        playerDataMap.put(e.getKey(), MatchCalculate.merge2PlayerData(playerDataMap.get(e.getKey()), e.getValue()));
                    } else {
                        playerDataMap.put(e.getKey(), e.getValue());
                    }
                }
            }

            // 去掉没打的
            for (var e : playerDataMap.entrySet()) {
                if (e.getValue().TMG <= 0) playerDataMap.remove(e.getKey());
            }

            this.playerCount = playerDataMap.size();

            /*
            this.playerDataMap.putAll(dataList.getFirst().getPlayerDataMap());

            for (var d : dataList) {
                var m = d.getPlayerDataMap();
                this.playerDataMap.forEach(
                        (k, v) -> m.merge(k, v, MatchCalculate::merge2PlayerData)
                );
            }

             */

            calculate();
        }

        /*
        private void initPlayerDataMap() {
            playerDataMap = players.stream().collect(
                    Collectors.toMap(MicroUser::getUserID, PlayerData::new, (a, b) -> b, LinkedHashMap::new)
            );
        }

         */

        //玩家数据已经录入组，现在只需要遍历
        public void calculate(){
            // 合并，方便遍历
            var playerDataList = this.playerDataMap.values().stream().toList();

            for (var player : playerDataList) {
                player.setTeam(null);
                player.calculateRWS();
                player.calculateTotalScore();
                player.calculateAverageScore();
                seriesAMG += player.getAMG();
            }

            for (var player : playerDataList) {
                player.calculateMQ(seriesAMG / playerCount); //除以的是该玩家所有人数

                minMQ = Math.min(minMQ, player.getMQ());
            }

            calculateScalingFactor();

            for (var player : playerDataList) {
                player.calculateERA(minMQ, scalingFactor);
                player.calculateDRA(playerCount, scoreCount);
                player.calculateMRA();
            }

            for (var player : playerDataList) {
                player.calculateClass();
            }

            // 展开
            this.playerDataMap = playerDataList.stream().collect(Collectors.toMap(
                    p -> p.getPlayer().getUserID(), p -> p, (p1, p2) -> p2, LinkedHashMap::new
            ));

            calculateIndex();

            calculateSort();
        }

        //缩放因子 Scaling Factor
        private void calculateScalingFactor() {
            if (playerCount <= 2) {
                scalingFactor = 0d;
            } else {
                scalingFactor = 2d / (1d + Math.exp(0.5d - 0.25d * playerCount)) - 1d;
            }
        }

        private void calculateIndex() {
            AtomicInteger ai1 = new AtomicInteger(1);
            AtomicInteger ai2 = new AtomicInteger(1);
            AtomicInteger ai3 = new AtomicInteger(1);
            AtomicInteger ai4 = new AtomicInteger(1);

            var l = new ArrayList<>(playerDataMap.values());
            l.sort(Comparator.comparing(PlayerData::getERA).reversed());
            l.forEach(r -> r.setERAIndex(1D * ai1.getAndIncrement() / playerCount));
            l.sort(Comparator.comparing(PlayerData::getDRA).reversed());
            l.forEach(r -> r.setDRAIndex(1D * ai2.getAndIncrement() / playerCount));
            l.sort(Comparator.comparing(PlayerData::getRWS).reversed());
            l.forEach(r -> r.setRWSIndex(1D * ai3.getAndIncrement() / playerCount));
            l.sort(Comparator.comparing(PlayerData::getMRA).reversed());
            l.forEach(r -> r.setRanking(ai4.getAndIncrement()));
        }

        private void calculateSort() {
            // 根据 MRA 高低排序，重新写图
            playerDataMap = playerDataMap.entrySet()
                    .stream()
                    .sorted(Comparator.<Map.Entry<Long, PlayerData>>comparingDouble(e -> e.getValue().getMRA()).reversed())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));

            this.playerDataList = playerDataMap.values().stream().toList();
        }

        public boolean isTeamVs() {
            return teamVs;
        }

        public void setTeamVs(boolean teamVs) {
            this.teamVs = teamVs;
        }

        public double getAverageStar() {
            return averageStar;
        }

        public void setAverageStar(double averageStar) {
            this.averageStar = averageStar;
        }

        public long getFirstMapSID() {
            return firstMapSID;
        }

        public void setFirstMapSID(long firstMapSID) {
            this.firstMapSID = firstMapSID;
        }

        public HashMap<Long, PlayerData> getPlayerDataMap() {
            return playerDataMap;
        }

        public void setPlayerDataMap(HashMap<Long, PlayerData> playerDataMap) {
            this.playerDataMap = playerDataMap;
        }

        public List<PlayerData> getPlayerDataList() {
            return playerDataList;
        }

        public void setPlayerDataList(List<PlayerData> playerDataList) {
            this.playerDataList = playerDataList;
        }

        public HashMap<String, Integer> getTeamPointMap() {
            return teamPointMap;
        }

        public int getMatchCount() {
            return matchCount;
        }

        public void setMatchCount(int matchCount) {
            this.matchCount = matchCount;
        }

        public int getRoundCount() {
            return roundCount;
        }

        public void setRoundCount(int roundCount) {
            this.roundCount = roundCount;
        }

        public int getPlayerCount() {
            return playerCount;
        }

        public void setPlayerCount(int playerCount) {
            this.playerCount = playerCount;
        }

        public int getScoreCount() {
            return scoreCount;
        }

        public void setScoreCount(int scoreCount) {
            this.scoreCount = scoreCount;
        }
    }

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }

    public SeriesData getSeriesData() {
        return seriesData;
    }

    public void setSeriesData(SeriesData seriesData) {
        this.seriesData = seriesData;
    }
}
