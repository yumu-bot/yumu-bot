package com.now.nowbot.model.multiplayer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.model.JsonData.BeatMap;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchRound {


    @JsonProperty("beatmap_id")
    Long bid;
    Integer id;
    @JsonProperty("start_time")
    OffsetDateTime startTime;
    @JsonProperty("end_time")
    OffsetDateTime endTime;
    String mode;
    @JsonProperty("mod_int")
    Integer modInt;
    @JsonProperty("scoring_type")
    String scoringType;
    @JsonProperty("team_type")
    String teamType;
    List<String> mods;
    BeatMap beatmap;
    @JsonProperty("scores")
    List<MatchScore> scoreInfoList;

    int redTeamScore = 0;
    int blueTeamScore = 0;
    int totalTeamScore = 0;

    public Long getBid() {
        return bid;
    }

    public void setBid(Long bid) {
        this.bid = bid;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(OffsetDateTime endTime) {
        this.endTime = endTime;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Integer getModInt() {
        return modInt;
    }

    public void setModInt(Integer modInt) {
        this.modInt = modInt;
    }

    public String getScoringType() {
        return scoringType;
    }

    public void setScoringType(String scoringType) {
        this.scoringType = scoringType;
    }

    public String getTeamType() {
        return teamType;
    }

    public void setTeamType(String teamType) {
        this.teamType = teamType;
    }

    public List<String> getMods() {
        return mods;
    }

    public void setMods(List<String> mods) {
        this.mods = mods;
    }

    public BeatMap getBeatmap() {
        return beatmap;
    }

    public void setBeatmap(BeatMap beatmap) {
        this.beatmap = beatmap;
    }

    public List<MatchScore> getScoreInfoList() {
        return scoreInfoList;
    }

    public void setScoreInfoList(List<MatchScore> scoreInfoList) {
        this.scoreInfoList = scoreInfoList;
    }

    public int getRedTeamScore() {
        initTeamScore();
        return redTeamScore;
    }

    public void setRedTeamScore(int redTeamScore) {
        this.redTeamScore = redTeamScore;
    }

    public int getBlueTeamScore() {
        initTeamScore();
        return blueTeamScore;
    }

    public void setBlueTeamScore(int blueTeamScore) {
        this.blueTeamScore = blueTeamScore;
    }

    public int getTotalTeamScore() {
        return totalTeamScore;
    }

    public void setTotalTeamScore(int totalTeamScore) {
        this.totalTeamScore = totalTeamScore;
    }

    private void initTeamScore() {
        if (totalTeamScore == 0 && redTeamScore == 0 && blueTeamScore == 0) {
            if (Objects.equals(teamType, "team-vs")) {
                for (MatchScore s : scoreInfoList) {
                    String team = s.getTeam();
                    switch (team) {
                        case "red" -> redTeamScore += s.getScore();
                        case "blue" -> blueTeamScore += s.getScore();
                    }
                }
                totalTeamScore = redTeamScore + blueTeamScore;

            } else {
                for (MatchScore s : scoreInfoList) {
                    totalTeamScore += s.getScore();
                }
            }
        }
    }

    //在单挑的时候给的是玩家的最高分
    public String getWinningTeam() {
        if (!Objects.equals(teamType, "team-vs")) return "none";
        initTeamScore();

        if (redTeamScore > blueTeamScore) return "red";
        else if (redTeamScore < blueTeamScore) return "blue";
        else return null; //平局
    }

    public Integer getWinningTeamScore() {
        if (Objects.equals(teamType, "team-vs")) {
            initTeamScore();
            return Math.max(redTeamScore, blueTeamScore);
        } else {
            return this.getScoreInfoList().stream().mapToInt(MatchScore::getScore).reduce(Integer::max).orElse(0);
        }
    }
}
