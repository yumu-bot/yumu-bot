package com.now.nowbot.model.match;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameRound {
    Map<String, Long> teamScores = new HashMap<>();
    Map<Integer, Integer> userScores = new HashMap<>();

    public Long getTotalScore(){
        Long totalScore = 0L;
        for(var scores: teamScores.values()){
            totalScore += scores;
        }
        return totalScore;
    }

    public String getWinningTeam(){
        Long maxScore = 0L;
        String winningTeam = null;
        for(var teamScore: teamScores.entrySet()){
            if(teamScore.getValue()>maxScore){
                winningTeam = teamScore.getKey();
                maxScore = teamScore.getValue();
            }
        }
        return winningTeam;
    }

    public Map<String, Long> getTeamScores() {
        return teamScores;
    }

    public void setTeamScores(Map<String, Long> teamScores) {
        this.teamScores = teamScores;
    }

    public Map<Integer, Integer> getUserScores() {
        return userScores;
    }

    public void setUserScores(Map<Integer, Integer> userScores) {
        this.userScores = userScores;
    }
}
