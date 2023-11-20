package com.now.nowbot.model.multiplayer;

import com.now.nowbot.model.JsonData.MicroUser;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MatchData {
    MatchStat matchStat;
    boolean isMatchEnd;
    boolean hasCurrentGame;

    List<PlayerData> playerDataList = new ArrayList<>();
    List<MicroUser> players;
    List<MatchRound> rounds;
    Map<String, Integer> teamPoint = new HashMap<>();
    boolean isTeamVS = true;

    float averageStar = 0f;

    long firstMapSID = 0L;

    //对局次数，比如 3:5 就是 8 局
    Integer roundCount = 0;

    //玩家数量
    Integer playerCount = 0;

    //分数数量
    Integer scoreCount = 0;

    Double roundAMG = 0d;
    private Double minMQ = 100d;
    private double scalingFactor;

    /**
     * @param remove 是否删除低于 1w 的成绩，true 为删除，false 为保留
     * @param rematch 是否包含重赛, true 为包含; false 为去重, 去重操作为保留最后一个
     */
    public MatchData(Match match, int skip, int skipEnd, boolean remove, boolean rematch) {
        matchStat = match.getMatchStat();
        isMatchEnd = match.isMatchEnd();
        hasCurrentGame = match.getCurrentGameId() != null;
        var cal = new MatchCal(match, skip, skipEnd, remove, rematch);

        averageStar = cal.getAverageStar();
        firstMapSID = cal.getFirstMapSID();

        rounds = cal.getRoundList();
        players = cal.getPlayers();
        roundCount = rounds.size();
        playerCount = players.size();

        for (MatchRound r : rounds) {
            scoreCount += r.getScoreInfoList().size();
        }

        if (!CollectionUtils.isEmpty(rounds)) {
            isTeamVS = Objects.equals(rounds.getFirst().getTeamType(), "team-vs");
        } else {
            isTeamVS = false;
        }

        for (MicroUser p: players) {
            playerDataList.add(new PlayerData(p));
        }
    }

    public MatchData(List<MatchRound> rounds) {
        this.rounds = rounds;
    }

    public MatchData() {}

    public void calculate(){

        //挨个成绩赋予RRA，计算scoreCount
        calculateRRA();

        //挨个成绩赋予RWS，计算胜负
        calculateRWS();

        //挨个用户计算AMG,并记录总AMG
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
        for (MatchRound round : rounds) {
            List<MatchScore> scoreList = round.getScoreInfoList();

            int roundScore = scoreList.stream().mapToInt(MatchScore::getScore).reduce(Integer::sum).orElse(0);
            int roundScoreCount = scoreList.size();

            //每一个分数
            for (MatchScore score: scoreList) {
                for (var player : playerDataList) {
                    var id = player.getPlayer().getId();

                    if (Objects.equals(id, score.getUserId()) && roundScore > 0) {
                        double RRA = 1.0d * score.getScore() * roundScoreCount / roundScore;

                        player.getRRAs().add(RRA);
                        player.getScores().add(score.getScore());
                        if (player.getTeam() == null) {
                            player.setTeam(score.getTeam());
                        }
                        break;
                    }
                }
            }
        }

        for (var player : playerDataList) {
            player.calculateTTS();
        }
    }

    public void calculateRWS() {
        //每一局
        for (MatchRound round : rounds) {

            String WinningTeam = round.getWinningTeam();
            Integer WinningTeamScore = round.getWinningTeamScore();

            //每一个分数
            for (MatchScore score: round.getScoreInfoList()) {

                for (var player : playerDataList) {
                    var id = player.getPlayer().getId();
                    var team = player.getTeam();

                    if (Objects.equals(id, score.getUserId()) && WinningTeamScore > 0) {
                        double RWS;

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

                        player.getRWSs().add(RWS);
                        break;
                    }
                }
            }
        }

        for (var player : playerDataList) {
            player.calculateRWS(roundCount);
        }
    }

    public void calculateAMG() {
        for (var player : playerDataList) {
            player.calculateAMG();

            roundAMG += player.getAMG();
        }
    }

    public void calculateMQ() {
        for (var player : playerDataList) {
            player.calculateMQ(roundAMG / playerCount); //除以的是该玩家所有人数

            if (player.getMQ() < minMQ) {
                minMQ = player.getMQ();
            }
        }
    }

    //缩放因子 Scaling Factor
    public void calculateSF() {
        double scalingFactor = 2D / (1D + Math.exp(0.5D - 0.25D * playerCount)) - 1D;
        if (playerCount <= 2) scalingFactor = 0D;

        this.scalingFactor = scalingFactor;
    }

    public void calculateMRA() {
        for (var player : playerDataList) {
            player.calculateERA(minMQ, scalingFactor);
            player.calculateDRA(playerCount, scoreCount);
            player.calculateMRA();
        }
    }

    public void calculateIndex() {
        AtomicInteger ai1 = new AtomicInteger(1);
        AtomicInteger ai2 = new AtomicInteger(1);
        AtomicInteger ai3 = new AtomicInteger(1);
        AtomicInteger ai4 = new AtomicInteger(1);

        playerDataList = playerDataList.stream()
                .sorted(Comparator.comparing(PlayerData::getERA).reversed())
                .peek(r -> r.setERAIndex(1D * ai1.getAndIncrement() / playerCount))
                .sorted(Comparator.comparing(PlayerData::getDRA).reversed())
                .peek(r -> r.setDRAIndex(1D * ai2.getAndIncrement() / playerCount))
                .sorted(Comparator.comparing(PlayerData::getRWS).reversed())
                .peek(r -> r.setRWSIndex(1D * ai3.getAndIncrement() / playerCount))
                .sorted(Comparator.comparing(PlayerData::getMRA).reversed())
                .peek(r -> r.setRanking(ai4.getAndIncrement()))
                .collect(Collectors.toList());
    }

    public void calculateClass() {
        for (var player : playerDataList) {
            player.calculateClass();
        }
    }

    public void calculateTeamPoint() {
        //每一局
        for (MatchRound r : rounds) {
            String team = r.getWinningTeam();
            if (team != null) {
                teamPoint.put(team, teamPoint.getOrDefault(team, 0) + 1);
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
        return playerDataList;
    }

    public void setPlayerDataList(List<PlayerData> playerDataList) {
        this.playerDataList = playerDataList;
    }

    public List<MicroUser> getPlayers() {
        return players;
    }

    public void setPlayers(List<MicroUser> players) {
        this.players = players;
    }

    public List<MatchRound> getRounds() {
        return rounds;
    }

    public void setRounds(List<MatchRound> rounds) {
        this.rounds = rounds;
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
