package com.now.nowbot.model.multiplayer;

import com.now.nowbot.model.JsonData.MicroUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MatchData {
    Map<Long, PlayerData> playerDataMap = new HashMap<>();
    List<MicroUser> players;
    List<MatchRound> rounds;
    Map<String, Integer> teamPoint = new HashMap<>();
    boolean isTeamVS = true;

    //对局次数，比如 3:5 就是 8 局
    Integer roundCount = 0;

    //玩家数量，
    Integer playerCount = 0;
    Integer scoreCount = 0;

    Double roundAMG = 0d;
    Double minMQ = 100d;

    public MatchData(Match match, boolean rematch, boolean remove) {
        var cal = new MatchCal(match, rematch, remove);

        this.rounds = cal.getGameRounds();
        this.players = match.getPlayers(); //不准确，应该需要剔除掉某些玩家

        if (!rounds.isEmpty()) {
            this.isTeamVS = Objects.equals(rounds.getFirst().teamType, "team-vs");
        } else {
            this.isTeamVS = false;
        }

        for (MicroUser p: players) {
            playerDataMap.put(p.getId(), new PlayerData(p));
        }
    }

    public MatchData(List<MatchRound> rounds) {
        this.rounds = rounds;
    }

    public MatchData() {}

    public void calculate(){
        this.playerCount = this.players.size();

        //挨个用户计算AMG,并记录总AMG
        for (var player : playerDataMap.values()) {
            player.calculateAMG();
            roundAMG += player.getAMG();
        }

        //挨个计算MQ,并记录最小的MQ
        for (var player : playerDataMap.values()) {
            player.calculateMQ(roundAMG / playerCount);
            if (player.getMQ() < minMQ)
                minMQ = player.getMQ();
        }

    }

}
