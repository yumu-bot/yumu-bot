package com.now.nowbot.model.multiplayer;

import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.imag.MapAttr;
import com.now.nowbot.model.imag.MapAttrGet;
import com.now.nowbot.service.ImageService;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MatchData {
    MatchStat matchStat;
    boolean isMatchEnd;
    boolean hasCurrentGame;

    Map<Long, PlayerData> playerData = new LinkedHashMap<>();
    List<MicroUser> playerList;
    List<MatchRound> roundList;
    Map<String, Integer> teamPoint = new HashMap<>();
    boolean isTeamVS = true;

    float averageStar = 0f;

    long firstMapSID = 0L;

    //对局次数，比如 3:5 就是 8 局
    int roundCount = 0;

    //玩家数量
    int playerCount = 0;

    //分数数量
    int scoreCount = 0;

    double roundAMG = 0d;
    private double minMQ = 100d;
    private double scalingFactor;

    // easy Mod 倍率
    double easyMultiplier;

    /**
     * @param delete 是否保留低于 1w 的成绩，true 为删除，false 为保留
     * @param rematch 是否去重赛, true 为包含; false 为去重, 去重操作为保留最后一个
     */
    public MatchData(Match match, int skip, int ignore, List<Integer> remove, double easy, boolean delete, boolean rematch) {
        easyMultiplier = easy;
        matchStat = match.getMatchStat();
        isMatchEnd = match.isMatchEnd();
        hasCurrentGame = match.getCurrentGameId() != null;
        var cal = new MatchCal(match, skip, ignore, remove, easy, delete, rematch);

        averageStar = cal.getAverageStar();
        firstMapSID = cal.getFirstMapSID();

        roundList = cal.getRoundList();
        playerList = cal.getPlayerList();
        roundCount = roundList.size();
        playerCount = playerList.size();

        roundList.forEach(r -> scoreCount += r.getScoreInfoList().stream().filter(s -> s.getScore() > 0).toList().size());

        if (!CollectionUtils.isEmpty(roundList)) {
            isTeamVS = Objects.equals(roundList.getFirst().getTeamType(), "team-vs");
        } else {
            isTeamVS = false;
        }

        playerData = cal.getPlayerMap().entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, e -> new PlayerData(e.getValue()), (a, b) -> b, LinkedHashMap::new)
        );
    }

    public MatchData(List<MatchRound> rounds) {
        this.roundList = rounds;
    }

    public MatchData() {}

    public void calculate(){

        //挨个成绩赋予RRA，计算scoreCount
        calculateRRA();

        //挨个成绩赋予RWS，计算胜负
        calculateRWS();

        //挨个用户计算AMG，并记录总AMG，顺便赋予对局的数量（有关联的对局数量）
        //calculateTTS 与 calculateRWS 在这里同时进行

        calculateTTS();

        //自己想想，TotalScore是需要遍历第一遍然后算得的一个最终值
        //AMG需要这个最终值。
        //如果同时进行，TotalScore 不完整！！！！！！！！！！！
        calculateAMG();

        //挨个计算MQ,并记录最小的MQ
        calculateMQ();

        //赋予缩放因子
        calculateSF();

        //根据minMQ计算出ERA，DRA，MRA
        calculateMRA();

        //计算E、D、M的index，排序，并且计算玩家分类 PlayerClassification
        calculateIndex();

        //计算玩家分类
        calculateClass();

        //计算比分
        calculateTeamPoint();
    }

    public void calculateRRA() {
        //每一局
        for (MatchRound round : roundList) {
            List<MatchScore> scoreList = round.getScoreInfoList();

            long roundScore = scoreList.stream()
                    .mapToLong(MatchScore::getScore)
                    .reduce(Long::sum).orElse(0L);

            int roundScoreCount = scoreList.stream().filter(s -> s.getScore() > 0).toList().size();
            if (roundScore == 0) continue;

            //每一个分数
            for (MatchScore s: scoreList) {
                var player = playerData.get(s.getUserId());
                if (Objects.isNull(player) || s.getScore() == 0) continue;

                double RRA = 1.0d * s.getScore() * roundScoreCount / roundScore;

                player.getRRAs().add(RRA);
                player.getScores().add(s.getScore());
                if (Objects.isNull(player.getTeam())) {
                    player.setTeam(s.getTeam());
                }
            }
        }
    }

    public void calculateRWS() {
        //每一局
        for (MatchRound round : roundList) {

            String WinningTeam = round.getWinningTeam();
            int WinningTeamScore = round.getWinningTeamScore(); //在单挑的时候给的是玩家的最高分
            if (WinningTeamScore == 0) continue;

            boolean isTeamVS = Objects.equals(round.getTeamType(), "team-vs");

            //每一个分数
            for (MatchScore score: round.getScoreInfoList()) {
                var player = playerData.get(score.getUserId());
                if (Objects.isNull(player)) continue;

                var team = player.getTeam();
                double RWS;
                if (isTeamVS) {
                    if (Objects.equals(WinningTeam, team)) {
                        RWS = 1.0d * score.getScore() / WinningTeamScore;
                        player.setWin(player.getWin() + 1);
                    } else if (WinningTeam == null) {
                        //平局
                        RWS = 1.0d * score.getScore() / WinningTeamScore;
                    } else {
                        RWS = 0d;
                        player.setLose(player.getLose() + 1);
                    }
                } else {
                    if (score.getScore() == WinningTeamScore) {
                        RWS = 1.0d;
                        player.setWin(player.getWin() + 1);
                    } else {
                        RWS = 0d;
                        player.setLose(player.getLose() + 1);
                    }
                }

                player.getRWSs().add(RWS);
            }
        }
        // 挨个计算放在外面在一个循环进行
    }

    public void calculateTTS() {
        playerData.values().forEach(PlayerData::calculateTTS);
    }

    public void calculateAMG() {
        playerData.values().forEach(player -> {
            player.calculateRWS();
            player.calculateAMG();
            player.setARC(roundList.size());
            roundAMG += player.getAMG();
        });
    }

    public void calculateMQ() {
        playerData.values().forEach(player -> {
            player.calculateMQ(roundAMG / playerCount); //除以的是该玩家所有人数

            if (player.getMQ() < minMQ) {
                minMQ = player.getMQ();
            }
        });
    }

    //缩放因子 Scaling Factor
    public void calculateSF() {
        double scalingFactor = 2D / (1D + Math.exp(0.5D - 0.25D * playerCount)) - 1D;
        if (playerCount <= 2) scalingFactor = 0D;

        this.scalingFactor = scalingFactor;
    }

    public void calculateMRA() {
        playerData.values().forEach(player -> {
            player.calculateERA(minMQ, scalingFactor);
            player.calculateDRA(playerCount, scoreCount);
            player.calculateMRA();
        });
    }

    public void calculateIndex() {
        AtomicInteger ai1 = new AtomicInteger(1);
        AtomicInteger ai2 = new AtomicInteger(1);
        AtomicInteger ai3 = new AtomicInteger(1);
        AtomicInteger ai4 = new AtomicInteger(1);

        var l = new ArrayList<>(playerData.values());
        l.sort(Comparator.comparing(PlayerData::getERA).reversed());
        l.forEach(r -> r.setERAIndex(1D * ai1.getAndIncrement() / playerCount));
        l.sort(Comparator.comparing(PlayerData::getDRA).reversed());
        l.forEach(r -> r.setDRAIndex(1D * ai2.getAndIncrement() / playerCount));
        l.sort(Comparator.comparing(PlayerData::getRWS).reversed());
        l.forEach(r -> r.setRWSIndex(1D * ai3.getAndIncrement() / playerCount));
        l.sort(Comparator.comparing(PlayerData::getMRA).reversed());
        l.forEach(r -> r.setRanking(ai4.getAndIncrement()));

        playerData = playerData.entrySet()
                .stream()
                .sorted(Comparator.<Map.Entry<Long, PlayerData>>comparingDouble(e -> e.getValue().getMRA()).reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
    }

    public void calculateClass() {
        playerData.values().forEach(PlayerData::calculateClass);
    }

    public void calculateTeamPoint() {
        //每一局
        for (MatchRound r : roundList) {
            String team = r.getWinningTeam();
            if (team != null) {
                teamPoint.put(team, teamPoint.getOrDefault(team, 0) + 1);
            }
        }
    }

    /**
     * 根据房间所选 mod 来修正谱面星级与四维, 注意, mode 不能是多个, 也就是所有的 MatchRound 都会仅计算一种 mode
     * 如果需要计算多种 mode 请自行拆分 rounds
     *
     * @param mode         游戏模式
     * @param rounds       轮次
     * @param imageService 用于请求结果
     */
    public void updateBeatmapAttr(OsuMode mode, List<MatchRound> rounds, ImageService imageService) {
        final var getParm = new MapAttrGet(mode);
        // 统计需要修改的轮次
        for (var m : rounds) {
            if (Mod.hasChangeRating(m.getModInt())) {
                getParm.addMap(m.getId().longValue(), m.getBid(), m.getModInt());
            }
        }

        // 修改星级, 四维
        var result = imageService.getMapAttr(getParm);
        for (var m : rounds) {
            MapAttr attr;
            if (Objects.nonNull(attr = result.get(m.getId().longValue()))) {
                var beatmap = m.getBeatmap();
                beatmap.setStarRating(attr.getStars());
                beatmap.setAR(attr.getAr());
                beatmap.setOD(attr.getOd());
                beatmap.setCS(attr.getCs());
                beatmap.setHP(attr.getHp());
            }
        }
    }

    public MatchStat getMatchStat() {
        return matchStat;
    }

    public void setMatchStat(MatchStat matchStat) {
        this.matchStat = matchStat;
    }

    public boolean isMatchEnd() {
        return isMatchEnd;
    }

    public void setMatchEnd(boolean matchEnd) {
        isMatchEnd = matchEnd;
    }

    public boolean isHasCurrentGame() {
        return hasCurrentGame;
    }

    public void setHasCurrentGame(boolean hasCurrentGame) {
        this.hasCurrentGame = hasCurrentGame;
    }

    public List<PlayerData> getPlayerDataList() {
        return new ArrayList<>(playerData.values());
    }

    public List<MicroUser> getPlayerList() {
        return playerList;
    }

    public void setPlayerList(List<MicroUser> playerList) {
        this.playerList = playerList;
    }

    public List<MatchRound> getRoundList() {
        return roundList;
    }

    public void setRoundList(List<MatchRound> roundList) {
        this.roundList = roundList;
    }

    public Map<String, Integer> getTeamPoint() {
        return teamPoint;
    }

    public void setTeamPoint(Map<String, Integer> teamPoint) {
        this.teamPoint = teamPoint;
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

    public long getFirstMapSID() {
        return firstMapSID;
    }

    public void setFirstMapSID(long firstMapSID) {
        this.firstMapSID = firstMapSID;
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

    public Double getRoundAMG() {
        return roundAMG;
    }

    public void setRoundAMG(Double roundAMG) {
        this.roundAMG = roundAMG;
    }
}
