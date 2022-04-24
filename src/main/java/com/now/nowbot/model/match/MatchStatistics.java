package com.now.nowbot.model.match;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchStatistics {
    Map<Integer, UserMatchData> users;
    List<GameRound> gameRounds = new ArrayList<>();
    Map<String, Integer> teamPoint = new HashMap<>();

    Integer scoreNum;
    Double totalAMG = 0d;
    Double minMQ = 100d;
    boolean teamVs = true;


    public void calculate() {
        //挨个用户计算AMG,并记录总AMG
        for (var user : users.values()) {
            user.calculateAMG();
            totalAMG += user.getAMG();
        }

        //挨个计算MQ,并记录最小的MQ
        for (var user : users.values()) {
            user.calculateMQ(totalAMG / users.size());
            if (user.getMQ() < minMQ)
                minMQ = user.getMQ();
        }

        //挨个计算ERA、DRA、MRA
        for (var user : users.values()) {
            user.calculateERA(minMQ);
            user.calculateDRA(users.size(), scoreNum);
            user.calculateMRA();
        }

        //如果人数<=2或者没换过人, 则ERA==MQ,并重新计算最终的MRA
        //不用放缩ERA的模型了，直接用MQ模型，这样能解决掉某些极端情况的问题
        /*
        if (users.size() <= 2 || users.size() * gameRounds.size() == scoreNum) {
            for (var user : users.values()) {
                user.setERA(user.getMQ());
                user.calculateMRA();
            }
        }
         */

        //计算比分
        for (var round : gameRounds) {
            String team = round.getWinningTeam();
            teamPoint.put(team, teamPoint.getOrDefault(team, 0) + 1);
            for (Integer id : round.getUserScores().keySet()) {
                var user = users.get(id);
                if (user.getTeam().equals(team)) {
                    user.setWins(user.getWins() + 1);
                } else user.setLost(user.getLost() + 1);
            }
        }

    }

    public Map<String, Integer> getTeamPoint() {
        return teamPoint;
    }

    public void setTeamPoint(Map<String, Integer> teamPoint) {
        this.teamPoint = teamPoint;
    }

    public Integer getScoreNum() {
        return scoreNum;
    }

    public void setScoreNum(Integer scoreNum) {
        this.scoreNum = scoreNum;
    }

    public Double getTotalAMG() {
        return totalAMG;
    }

    public void setTotalAMG(Double totalAMG) {
        this.totalAMG = totalAMG;
    }

    public Double getMinMQ() {
        return minMQ;
    }

    public void setMinMQ(Double minMQ) {
        this.minMQ = minMQ;
    }

    public Map<Integer, UserMatchData> getUsers() {
        return users;
    }

    public void setUsers(Map<Integer, UserMatchData> users) {
        this.users = users;
    }

    public List<GameRound> getGameRounds() {
        return gameRounds;
    }

    public void setGameRounds(List<GameRound> gameRounds) {
        this.gameRounds = gameRounds;
    }

    public boolean isTeamVs() {
        return teamVs;
    }

    public void setTeamVs(boolean teamVs) {
        this.teamVs = teamVs;
    }
}
