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

    public String getWinningTeam() {
        if (!Objects.equals(teamType, "team-vs")) return "none";
        int redTeamScore = 0;
        int blueTeamScore = 0;

        for (MatchScore s : scoreInfoList) {
            String team = s.getMatchPlayerStat().getTeam();

            switch (team) {
                case "red" -> redTeamScore += s.getScore();
                case "blue" -> blueTeamScore += s.getScore();
            }
        }

        if (redTeamScore > blueTeamScore) return "red";
        else if (redTeamScore < blueTeamScore) return "blue";
        else return null; //平局
    }

    public Integer getWinningTeamScore() {
        String WinningTeam = getWinningTeam();

        return scoreInfoList.stream().filter(s -> Objects.equals(s.getMatchPlayerStat().getTeam(), WinningTeam))
                .mapToInt(ms -> ms.score).reduce(Integer::sum).orElse(0);
    }
}
