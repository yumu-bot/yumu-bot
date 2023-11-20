package com.now.nowbot.model.multiplayer;

import com.now.nowbot.model.JsonData.MicroUser;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SeriesData {
    Series series;
    List<MatchRound> rounds = new ArrayList<>();
    boolean isTeamVS = true;

    float averageStar = 0f;

    List<Long> firstMapSIDs = new ArrayList<>();

    //比赛次数
    Integer matchCount;
    //对局次数，比如 3:5 就是 8 局
    Integer roundCount = 0;
    //玩家数量
    Integer playerCount = 0;
    //分数数量
    Integer scoreCount = 0;

    Double seriesAMG = 0d;
    private Double minMQ = 100d;
    private double scalingFactor;


    public SeriesData(Series s, @Nullable String name, List<Integer> skips, List<Integer> skipEnds, boolean remove, boolean rematch) {
        series = s;
        series.getSeriesStat().setStartTime(OffsetDateTime.MAX);
        series.getSeriesStat().setEndTime(OffsetDateTime.MIN);

        Set<Long> playerUIDSet = new HashSet<>();
        List<PlayerData> playerDataFullList = new ArrayList<>();
        Map<Long, MicroUser> playerMap = new HashMap<>();

        matchCount = s.getMatches().size();
        for (int i = 0; i < matchCount; i++) {
            var matchData = new MatchData(s.getMatches().get(i), skips.get(i), skipEnds.get(i), remove, rematch);
            matchData.calculate();

            firstMapSIDs.add(matchData.getFirstMapSID());
            series.getFirstMapSIDs().add(matchData.getFirstMapSID());

            roundCount += matchData.getRoundCount();
            scoreCount += matchData.getScoreCount();
            averageStar += matchData.getAverageStar();

            var pd = matchData.getPlayerDataList();

            playerUIDSet.addAll(
                    pd.stream().map(p -> p.getPlayer().getId()).collect(Collectors.toCollection(LinkedHashSet::new))
            );

            playerDataFullList.addAll(pd);

            //包含所有玩家的映射表
            playerMap.putAll(matchData.getPlayers().stream().collect(Collectors.toMap(MicroUser::getId, p -> p, (p1, p2) -> p2)));

            //设定早晚时间
            if (matchData.getMatchStat().getStartTime()
                    .isBefore(series.getSeriesStat().getStartTime())) {
                series.getSeriesStat().setStartTime(matchData.getMatchStat().getStartTime());
            }
            if (matchData.getMatchStat().getEndTime()
                    .isAfter(series.getSeriesStat().getEndTime())) {
                series.getSeriesStat().setEndTime(matchData.getMatchStat().getEndTime());
            }

            //导入名字
            if (name == null || name.isBlank()) {
                name = series.getMatches().get(0).getMatchStat().getName() + "...";
            }
        }

        series.getSeriesStat().setName(name);

        //合并（去重）所有 MRA 数据，并存入 playerDataList
        getSeries().setPlayers(
                playerUIDSet.stream().map(playerMap::get).toList()
        );

        playerDataFullList.stream().collect(
                Collectors.groupingBy(p -> p.getPlayer().getId(), Collectors.toList()))
                .forEach((k, v) -> v.stream().reduce(this::merge2PlayerData).ifPresent(
                        playerData -> getSeries().getPlayerDataList().add(playerData)
                ));
        playerCount += this.series.getPlayers().size();

        averageStar /= matchCount;
    }

    /**
     * 合并同一个玩家的多组玩家数据
     * @param p1 数据1
     * @param p2 数据2
     * @return 合并数据
     */
    private PlayerData merge2PlayerData(PlayerData p1, PlayerData p2) {
        if (Objects.equals(p1.getPlayer().getId(), p2.getPlayer().getId())) {
            p1.setTTS(p1.getTTS() + p2.getTTS());
            p1.setWin(p1.getWin() + p2.getWin());
            p1.setLose(p1.getLose() + p2.getLose());
            p1.setTMG(p1.getTMG() + p2.getTMG());
            p1.getScores().addAll(p2.getScores());
            p1.getRWSs().addAll(p2.getRWSs());
            p1.getRRAs().addAll(p2.getRRAs());

            return p1;
        } else {
            return p2;
        }
    }

    //玩家数据已经录入组，现在只需要遍历
    public void calculate(){
        for (var player : series.getPlayerDataList()) {
            player.setTeam(null);
            player.calculateTTS();
            player.calculateAMG();
            seriesAMG += player.getAMG();
        }

        for (var player : series.getPlayerDataList()) {
            player.calculateMQ(seriesAMG / playerCount); //除以的是该玩家所有人数

            if (player.getMQ() < minMQ) {
                minMQ = player.getMQ();
            }
        }

        for (var player : series.getPlayerDataList()) {
            calculateSF();
            player.calculateDRA(playerCount, scoreCount);
            player.calculateERA(minMQ, scalingFactor);
            player.calculateMRA();
            player.calculateRWS(roundCount);
        }

        calculateIndex();
        calculateClass();
    }

    //缩放因子 Scaling Factor
    public void calculateSF() {
        double scalingFactor = 2D / (1D + Math.exp(0.5D - 0.25D * playerCount)) - 1D;
        if (playerCount <= 2) scalingFactor = 0D;

        this.scalingFactor = scalingFactor;
    }

    public void calculateIndex() {
        AtomicInteger ai1 = new AtomicInteger(1);
        AtomicInteger ai2 = new AtomicInteger(1);
        AtomicInteger ai3 = new AtomicInteger(1);
        AtomicInteger ai4 = new AtomicInteger(1);

        this.series.setPlayerDataList(
                this.series.getPlayerDataList().stream()
                .sorted(Comparator.comparing(PlayerData::getERA).reversed())
                .peek(r -> r.setERAIndex(1D * ai1.getAndIncrement() / playerCount))
                .sorted(Comparator.comparing(PlayerData::getDRA).reversed())
                .peek(r -> r.setDRAIndex(1D * ai2.getAndIncrement() / playerCount))
                .sorted(Comparator.comparing(PlayerData::getRWS).reversed())
                .peek(r -> r.setRWSIndex(1D * ai3.getAndIncrement() / playerCount))
                .sorted(Comparator.comparing(PlayerData::getMRA).reversed())
                .peek(r -> r.setRanking(ai4.getAndIncrement()))
                .collect(Collectors.toList()));
    }

    public void calculateClass() {
        for (var player : series.getPlayerDataList()) {
            player.calculateClass();
        }
    }

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }

    public List<MatchRound> getRounds() {
        return rounds;
    }

    public void setRounds(List<MatchRound> rounds) {
        this.rounds = rounds;
    }

    public boolean isTeamVS() {
        return isTeamVS;
    }

    public void setTeamVS(boolean teamVS) {
        isTeamVS = teamVS;
    }

    public float getAverageStar() {
        return averageStar;
    }

    public void setAverageStar(float averageStar) {
        this.averageStar = averageStar;
    }

    public List<Long> getFirstMapSIDs() {
        return firstMapSIDs;
    }

    public void setFirstMapSIDs(List<Long> firstMapSIDs) {
        this.firstMapSIDs = firstMapSIDs;
    }

    public Integer getMatchCount() {
        return matchCount;
    }

    public void setMatchCount(Integer matchCount) {
        this.matchCount = matchCount;
    }

    public Integer getRoundCount() {
        return roundCount;
    }

    public void setRoundCount(Integer roundCount) {
        this.roundCount = roundCount;
    }

    public Integer getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(Integer playerCount) {
        this.playerCount = playerCount;
    }

    public Integer getScoreCount() {
        return scoreCount;
    }

    public void setScoreCount(Integer scoreCount) {
        this.scoreCount = scoreCount;
    }

    public Double getSeriesAMG() {
        return seriesAMG;
    }

    public void setSeriesAMG(Double seriesAMG) {
        this.seriesAMG = seriesAMG;
    }
}
