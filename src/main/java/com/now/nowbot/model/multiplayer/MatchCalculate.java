package com.now.nowbot.model.multiplayer;

import com.now.nowbot.model.LazerMod;
import com.now.nowbot.model.enums.OsuMod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.json.Match;
import com.now.nowbot.model.json.MicroUser;
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService;
import jakarta.annotation.Resource;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MatchCalculate {
    Match match;

    MatchData matchData;

    private List<MicroUser> players;

    private List<Match.MatchRound> rounds;

    private List<Match.MatchScore> scores;

    @Resource
    OsuBeatmapApiService beatmapApiService;

    /**
     * @param delete 是否保留低于 1w 的成绩，true 为删除，false 为保留
     * @param rematch 是否去重赛, true 为包含; false 为去重, 去重操作为保留最后一个
     */
    public record CalculateParam(int skip, int ignore, List<Integer> remove, double easy, boolean delete, boolean rematch) {}

    public MatchCalculate() {}

    // 默认设置 param，适合比赛监听使用
    public MatchCalculate(@NonNull Match match, OsuBeatmapApiService beatmapApiService) {
        var param = new MatchCalculate.CalculateParam(0, 0, null, 1d, true, true);

        this.match = match;
        this.beatmapApiService = beatmapApiService;

        getRounds(param);
        getScores(param);
        getPlayers();

        this.matchData = new MatchData(param);
    }

    public MatchCalculate(@NonNull Match match, int skip, OsuBeatmapApiService beatmapApiService) {
        var param = new MatchCalculate.CalculateParam(skip, 0, null, 1d, true, true);

        this.match = match;
        this.beatmapApiService = beatmapApiService;

        getRounds(param);
        getScores(param);
        getPlayers();

        this.matchData = new MatchData(param);
    }

    public MatchCalculate(@NonNull Match match, CalculateParam param, OsuBeatmapApiService beatmapApiService) {
        this.match = match;
        this.beatmapApiService = beatmapApiService;

        getRounds(param);
        getScores(param);
        getPlayers();

        this.matchData = new MatchData(param);
    }

    private void getRounds(CalculateParam param) {
        // 去掉不包含分数和未完成 ·的对局
        List<Match.MatchRound> rounds = match.getEvents().stream().map(Match.MatchEvent::getRound)
                .filter(Objects::nonNull)
                .filter(round -> ! CollectionUtils.isEmpty(round.getScores()))
                .filter(round -> round.getEndTime() != null)
                .toList();

        int size = rounds.size();
        int limit = size - param.ignore;

        var skip = param.skip;
        var remove = param.remove;
        var delete = param.delete;
        var rematch = param.rematch;

        // rematch and delete
        if (delete) {
            rounds = rounds.stream().peek(round -> {
                if (! CollectionUtils.isEmpty(round.getScores()) && round.getScores().size() > 1) {
                    try {
                        round.getScores().removeIf(s -> s.getScore() <= 10000);
                    } catch (UnsupportedOperationException ignored) {
                        //我不清楚为什么不能移除，难道是数组内最后一个元素？
                    }
                }
                //我不清楚为什么不能移除，难道是数组内最后一个元素？
            }).collect(Collectors.toList());
        }

        if (! rematch) {
            rounds = new ArrayList<>(
                    rounds.stream().collect(
                                    Collectors.toMap(Match.MatchRound::getBeatMapID, e -> e, (o, n) -> n, LinkedHashMap::new))
                            .values()
            );
        }

        // skip and remove
        if (skip < 0 || skip > size || limit < 0 || limit > size || limit - skip < 0) return;

        rounds = rounds.stream().limit(limit).skip(skip)
                .collect(Collectors.toList());

        if (Objects.nonNull(remove) && ! remove.isEmpty()) {
            remove = remove.stream()
                    .map(i -> i - skip)
                    .filter(i -> i < limit - skip && i > 0)
                    .map(i -> i - 1)
                    .toList();

            for (int i = 0; i < rounds.size(); i++) {
                for (var m : remove) {
                    if (i == m) {
                        rounds.set(i, null);
                        break;
                    }
                }
            }

            rounds = rounds.stream().filter(Objects::nonNull).collect(Collectors.toList());
        }


        // easy multiplier
        if (param.easy != 1d) {
            rounds = rounds.stream().peek(r -> r.setScores(
                    Objects.requireNonNullElse(r.getScores(), new ArrayList<Match.MatchScore>()).stream().peek(
                            s -> {
                                if (OsuMod.hasEz(OsuMod.getModsValue(s.getMods()))) {
                                    s.setScore((int) (s.getScore() * param.easy));
                                }
                            }).collect(Collectors.toList()))
            ).collect(Collectors.toList());
        }

        // add ranking
        for (var r : rounds) {
            AtomicInteger i = new AtomicInteger(1);

            r.setScores(Objects.requireNonNullElse(r.getScores(), new ArrayList<Match.MatchScore>()).stream()
                    .filter(s -> s.getScore() > (param.delete ? 10000 : 0))
                    .sorted(Comparator.comparing(Match.MatchScore::getScore).reversed())
                    .peek(s -> s.setRanking(i.getAndIncrement()))
                    .toList());
        }

        // add user
        for (var r : rounds) {
            for (var s : Objects.requireNonNullElse(r.getScores(), new ArrayList<Match.MatchScore>())) {
                for (var p : this.match.getPlayers()) {
                    if (Objects.equals(p.getUserID(), s.getUserID())) {
                        s.setUser(p);
                        break;
                    }
                }
            }
        }

        // apply sr change
        for (var r : rounds) {
            var b = r.getBeatMap();

            if (b == null) continue;

            var m = OsuMode.getMode(r.getMode());

            // extend beatmap
            b = beatmapApiService.getBeatMapFromDataBase(b.getBeatMapID());

            // apply changes
            beatmapApiService.applySRAndPP(b, m, LazerMod.getModsList(r.getMods()));

            r.setBeatMap(b);
        }

        this.rounds = rounds;
    }

    private void getScores(CalculateParam param) {
        this.scores = this.rounds.stream()
                .flatMap(r -> Objects.requireNonNullElse(r.getScores(), new ArrayList<Match.MatchScore>()).stream())
                .filter(s -> s.getScore() > (param.delete ? 10000 : 0))
                .toList();
    }

    private void getPlayers() {
        Set<Long> playerUIDSet = this.scores.stream().map(Match.MatchScore::getUserID)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, MicroUser> playerMap = this.match.getPlayers().stream().collect(Collectors.toMap(MicroUser::getUserID, p -> p, (p1, p2) -> p2));
        this.players = playerUIDSet.stream().map(playerMap::get).toList();
    }

    public class MatchData {
        public boolean teamVs;

        public double averageStar;

        public long firstMapSID = 0L;

        // 玩家数据图和比分图，js 没法传 map
        public transient HashMap<Long, PlayerData> playerDataMap = new LinkedHashMap<>();

        public List<PlayerData> playerDataList = new ArrayList<>();

        public final HashMap<String, Integer> teamPointMap  = new LinkedHashMap<>();

        // 对局次数，比如 3:5 就是 8 局
        public int roundCount;

        // 玩家数量
        public int playerCount;

        // 分数数量
        public int scoreCount;

        // 用于计算 mra
        private double roundAMG = 0d;

        protected double minMQ = 100d;

        private double scalingFactor;

        MatchData(CalculateParam param) {
            this.playerCount = players.size();
            this.roundCount = rounds.size();
            this.scoreCount = rounds.stream().flatMap(r -> Objects.requireNonNullElse(r.getScores(), new ArrayList<Match.MatchScore>()).stream())
                    .filter(s -> s.getScore() > (param.delete ? 10000 : 0))
                    .toList().size();

            initTeamVs();
            initAverageStar();
            initFirstMapSID();

            initPlayerDataMap();

            calculate();
        }

        private void initTeamVs() {
            if (!CollectionUtils.isEmpty(rounds)) {
                teamVs = Objects.equals(rounds.getFirst().getTeamType(), "team-vs");
            } else {
                teamVs = false;
            }
        }

        private void initAverageStar() {
            if (roundCount == 0) {
                averageStar = 0;
            }
            else {
                averageStar = rounds.stream()
                        .filter(r -> r.getBeatMap() != null).map(s -> s.getBeatMap().getStarRating())
                        .reduce(Double::sum).orElse(0d) / roundCount;
            }
        }

        private void initFirstMapSID() {
            if (! CollectionUtils.isEmpty(rounds) && rounds.getFirst().getBeatMap() != null) {
                firstMapSID = rounds.getFirst().getBeatMap().getBeatMapSetID();
            } else {
                firstMapSID = 0L;
            }
        }

        private void initPlayerDataMap() {
            playerDataMap = players.stream().filter(Objects::nonNull).collect(
                    Collectors.toMap(MicroUser::getUserID, PlayerData::new, (a, b) -> b, LinkedHashMap::new)
            );
        }

        public void calculate() {
            //挨个成绩赋予RRA，计算scoreCount
            calculateRRA();

            //挨个成绩赋予RWS，计算胜负
            calculateRWS();

            //挨个用户计算AMG，并记录总AMG，顺便赋予对局的数量（有关联的对局数量）
            //calculateTTS 与 calculateRWS 在这里同时进行

            calculateTotalScore();

            //自己想想，TotalScore是需要遍历第一遍然后算得的一个最终值
            //AMG需要这个最终值。
            //如果同时进行，TotalScore 不完整！！！！！！！！！！！
            calculateAMG();

            //挨个计算MQ,并记录最小的MQ
            calculateMQ();

            //赋予缩放因子
            calculateScalingFactor();

            //根据minMQ计算出ERA，DRA，MRA
            calculateMRA();

            //计算E、D、M的index，排序，并且计算玩家分类 PlayerClassification
            calculateIndex();

            //计算玩家分类
            calculateClass();

            //计算比分
            calculateTeamPoint();

            //最后排序
            calculateSort();
        }

        private void calculateRRA() {

            //每一局
            for (Match.MatchRound round : rounds) {
                List<Match.MatchScore> scoreList = Objects.requireNonNullElse(round.getScores(), new ArrayList<>(0));

                long roundScore = scoreList.stream()
                        .mapToLong(Match.MatchScore::getScore)
                        .reduce(Long::sum).orElse(0L);

                int roundScoreCount = scoreList.stream().filter(s -> s.getScore() > 0).toList().size();
                if (roundScore == 0) continue;

                //每一个分数
                for (Match.MatchScore s : scoreList) {
                    var player = playerDataMap.get(s.getUserID());
                    if (Objects.isNull(player) || s.getScore() == 0) continue;

                    double RRA = 1.0d * s.getScore() * roundScoreCount / roundScore;

                    player.getRRAs().add(RRA);
                    player.getScores().add(s.getScore());

                    if (Objects.isNull(player.getTeam())) {
                        player.setTeam(s.getPlayerStat().team());
                    }
                }
            }
        }

        private void calculateRWS() {

            //每一局
            for (Match.MatchRound round : rounds) {

                String WinningTeam = round.getWinningTeam();
                long WinningTeamScore = round.getWinningTeamScore(); //在单挑的时候给的是玩家的最高分
                if (WinningTeamScore == 0L) continue;

                boolean isTeamVS = Objects.equals(round.getTeamType(), "team-vs");

                //每一个分数
                if (round.getScores() != null) {
                    for (Match.MatchScore score : round.getScores()) {
                        var player = playerDataMap.get(score.getUserID());
                        if (player == null) continue;

                        var team = player.getTeam();
                        double RWS;
                        if (isTeamVS) {
                            if (Objects.equals(WinningTeam, team)) {
                                RWS = 1d * score.getScore() / WinningTeamScore;
                                player.setWin(player.getWin() + 1);
                            } else if (WinningTeam == null) {
                                //平局
                                RWS = 1d * score.getScore() / WinningTeamScore;
                            } else {
                                RWS = 0d;
                                player.setLose(player.getLose() + 1);
                            }
                        } else {
                            if (score.getScore() == WinningTeamScore) {
                                RWS = 1d;
                                player.setWin(player.getWin() + 1);
                            } else {
                                RWS = 0d;
                                player.setLose(player.getLose() + 1);
                            }
                        }

                        player.getRWSs().add(RWS);
                    }
                }
            }
            // 挨个计算放在外面在一个循环进行
        }

        private void calculateTotalScore() {
            playerDataMap.values().forEach(PlayerData::calculateTotalScore);
        }

        private void calculateAMG() {
            playerDataMap.values().forEach(player -> {
                player.calculateRWS();
                player.calculateTMG();
                player.calculateAverageScore();
                player.setARC(rounds.size());
                roundAMG += player.getAMG();
            });
        }


        private void calculateMQ() {
            playerDataMap.values().forEach(player -> {
                player.calculateMQ(roundAMG / playerCount); //除以的是所有玩家数

                minMQ = Math.min(minMQ, player.getMQ());
            });
        }


        private void calculateScalingFactor() {
            if (playerCount <= 2) {
                scalingFactor = 0d;
            } else {
                scalingFactor = 2d / (1d + Math.exp(0.5d - 0.25d * playerCount)) - 1d;
            }
        }

        private void calculateMRA() {
            playerDataMap.values().forEach(player -> {
                player.calculateERA(minMQ, scalingFactor);
                player.calculateDRA(playerCount, scoreCount);
                player.calculateMRA();
            });
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

        private void calculateClass() {
            playerDataMap.values().forEach(PlayerData::calculateClass);
        }

        private void calculateTeamPoint() {
            for (Match.MatchRound r : rounds) {
                String winner = r.getWinningTeam();
                if (winner != null) {
                    teamPointMap.put(winner, teamPointMap.getOrDefault(winner, 0) + 1);
                }
            }
        }

        private void calculateSort() {
            // 根据 MRA 高低排序，重新写图

            playerDataMap = playerDataMap.entrySet().stream()
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

    public static class PlayerData implements Cloneable {
        MicroUser player;

        String team;

        List<Integer> scores = new ArrayList<>();

        List<Double> RWSs = new ArrayList<>();

        //totalScore
        Long total = 0L;

        //标准化的单场个人得分 RRAs，即标准分 = score/TotalScore
        List<Double> RRAs = new ArrayList<>();

        //总得斗力点 TMG，也就是RRAs的和
        Double TMG = 0d;

        //场均标准分
        Double AMG = 0d;

        //AMG/Average(AMG) 场均标准分的相对值
        Double MQ = 0d;

        Double ERA = 0d;
        //(TMG*playerNumber)/参赛人次

        Double DRA = 0d;
        //MRA = 0.7 * ERA + 0.3 * DRA

        Double MRA = 0d;
        //平均每局胜利分配 RWS v3.4添加

        Double RWS = 0d;

        PlayerClass playerClass;

        double ERAIndex;

        double DRAIndex;

        double RWSIndex;

        int ranking = 0;

        //胜负场次
        int win = 0;

        int lose = 0;

        //有关联的所有场次，注意不是参加的场次 AssociatedRoundCount
        int ARC = 0;

        PlayerData(MicroUser player) {
            this.player = player;
        }

        PlayerData() {
            this.player = new MicroUser();
        }

        public MicroUser getPlayer() {
            return player;
        }

        public void setPlayer(MicroUser player) {
            this.player = player;
        }

        public String getTeam() {
            return team;
        }

        public void setTeam(String team) {
            this.team = team;
        }

        public List<Integer> getScores() {
            return scores;
        }

        public void setScores(List<Integer> scores) {
            this.scores = scores;
        }

        public List<Double> getRWSs() {
            return RWSs;
        }

        public void setRWSs(List<Double> RWSs) {
            this.RWSs = RWSs;
        }

        public Long getTotal() {
            return total;
        }

        public void setTotal(Long total) {
            this.total = total;
        }

        public List<Double> getRRAs() {
            return RRAs;
        }

        public void setRRAs(List<Double> RRAs) {
            this.RRAs = RRAs;
        }

        public Double getTMG() {
            return TMG;
        }

        public void setTMG(Double TMG) {
            this.TMG = TMG;
        }

        public Double getAMG() {
            return AMG;
        }

        public void setAMG(Double AMG) {
            this.AMG = AMG;
        }

        public Double getMQ() {
            return MQ;
        }

        public void setMQ(Double MQ) {
            this.MQ = MQ;
        }

        public Double getERA() {
            return ERA;
        }

        public void setERA(Double ERA) {
            this.ERA = ERA;
        }

        public Double getDRA() {
            return DRA;
        }

        public void setDRA(Double DRA) {
            this.DRA = DRA;
        }

        public Double getMRA() {
            return MRA;
        }

        public void setMRA(Double MRA) {
            this.MRA = MRA;
        }

        public Double getRWS() {
            return RWS;
        }

        public void setRWS(Double RWS) {
            this.RWS = RWS;
        }

        public PlayerClass getPlayerClass() {
            return playerClass;
        }

        public void setPlayerClass(PlayerClass playerClass) {
            this.playerClass = playerClass;
        }

        public double getERAIndex() {
            return ERAIndex;
        }

        public void setERAIndex(double ERAIndex) {
            this.ERAIndex = ERAIndex;
        }

        public double getDRAIndex() {
            return DRAIndex;
        }

        public void setDRAIndex(double DRAIndex) {
            this.DRAIndex = DRAIndex;
        }

        public double getRWSIndex() {
            return RWSIndex;
        }

        public void setRWSIndex(double RWSIndex) {
            this.RWSIndex = RWSIndex;
        }

        public int getRanking() {
            return ranking;
        }

        public void setRanking(int ranking) {
            this.ranking = ranking;
        }

        public int getWin() {
            return win;
        }

        public void setWin(int win) {
            this.win = win;
        }

        public int getLose() {
            return lose;
        }

        public void setLose(int lose) {
            this.lose = lose;
        }

        public int getARC() {
            return ARC;
        }

        public void setARC(int ARC) {
            this.ARC = ARC;
        }

        public void calculateTotalScore() {
            for (Integer score : scores) {
                total += score;
            }
        }

        // 注意。Series 中，不需要再次统计 TMG。
        public void calculateTMG() {
            for (Double RRA : RRAs) {
                TMG += RRA;
            }
        }

        public void calculateAverageScore() {
            if (!RRAs.isEmpty()) {
                AMG = TMG / RRAs.size();
            }
        }

        //aAMG是AMG的平均值
        public void calculateMQ(double aAMG) {
            MQ = AMG / aAMG;
        }

        public void calculateERA(double minMQ, double ScalingFactor) {
            ERA = (MQ - minMQ * ScalingFactor) / (1 - minMQ * ScalingFactor);
        }

        public void calculateDRA(int playerCount, int scoreCount) {
            DRA = (TMG / scoreCount) * playerCount;
        }

        public void calculateMRA() {
            MRA = 0.7d * ERA + 0.3d * DRA;
        }

        public void calculateRWS() {
            RWS = RWSs.stream().mapToDouble(Double::doubleValue).average().orElse(0d);
        }

        public void calculateClass() {
            playerClass = new PlayerClass(ERAIndex, DRAIndex, RWSIndex);
        }

        @Override
        public PlayerData clone() {
            try {
                return (PlayerData) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public MatchData getMatchData() {
        return matchData;
    }

    public void setMatchData(MatchData matchData) {
        this.matchData = matchData;
    }

    public void setPlayers(List<MicroUser> players) {
        this.players = players;
    }

    public List<Match.MatchRound> getRounds() {
        return rounds;
    }

    public void setRounds(List<Match.MatchRound> rounds) {
        this.rounds = rounds;
    }

    public List<Match.MatchScore> getScores() {
        return scores;
    }

    public void setScores(List<Match.MatchScore> scores) {
        this.scores = scores;
    }

    public OsuBeatmapApiService getBeatmapApiService() {
        return beatmapApiService;
    }

    public void setBeatmapApiService(OsuBeatmapApiService beatmapApiService) {
        this.beatmapApiService = beatmapApiService;
    }

    /**
     * 合并同一个玩家的多组玩家数据
     * @param p1 数据1
     * @param p2 数据2
     * @return 合并数据
     */
    public static PlayerData merge2PlayerData(PlayerData p1, PlayerData p2) {
        if (! Objects.equals(p1.getPlayer().getUserID(), p2.getPlayer().getUserID())) {
            return p2;
        } else {
            var p = p1.clone();

            p.setTotal(p1.getTotal() + p2.getTotal());
            p.setWin(p1.getWin() + p2.getWin());
            p.setLose(p1.getLose() + p2.getLose());
            p.setTMG(p1.getTMG() + p2.getTMG());
            p.setARC(p1.getARC() + p2.getARC());
            p.getScores().addAll(p2.getScores());
            p.getRWSs().addAll(p2.getRWSs());
            p.getRRAs().addAll(p2.getRRAs());

            return p;
        }
    }
}
