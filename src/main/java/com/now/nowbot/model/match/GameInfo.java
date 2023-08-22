package com.now.nowbot.model.match;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.model.beatmap.BeatmapInfo4Match;
import com.now.nowbot.model.score.MpScoreInfo;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GameInfo {

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
    String[] mods;
    @Nullable
    BeatmapInfo4Match beatmap;
    @JsonProperty("scores")
    List<MpScoreInfo> scoreInfos;

    public String getTeamType() {
        return teamType;
    }

    public void setTeamType(String teamType) {
        this.teamType = teamType;
    }

    public long getBID() {
        return bid;
    }

    public void setBID(long bid) {
        this.bid = bid;
    }

    public Integer getMatchID() {
        return id;
    }

    public void setMatchID(Integer id) {
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

    public String[] getMods() {
        return mods;
    }

    public void setMods(String[] mods) {
        this.mods = mods;
    }

    public BeatmapInfo4Match getBeatmap() {
        return beatmap;
    }

    public void setBeatmap(BeatmapInfo4Match beatmap) {
        this.beatmap = beatmap;
    }

    public List<MpScoreInfo> getScoreInfos() {
        return scoreInfos;
    }

    public void setScoreInfos(List<MpScoreInfo> scoreInfos) {
        this.scoreInfos = scoreInfos;
    }

    public String toString() {
        return new StringJoiner(", ", GameInfo.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("startTime=" + startTime)
                .add("endTime=" + endTime)
                .add("mode='" + mode + "'")
                .add("modInt=" + modInt)
                .add("scoringType='" + scoringType + "'")
                .add("teamType='" + teamType + "'")
                .add("mods=" + Arrays.toString(mods))
                .add("beatmap=" + beatmap)
                .add("scoreInfos=" + scoreInfos)
                .toString();
    }
}
