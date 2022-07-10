package com.now.nowbot.model.score;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Statistics;
import com.now.nowbot.model.beatmap.BeatMapSet;
import com.now.nowbot.model.beatmap.BeatmapInfo;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.beatmap.Mod;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Score {
    Long id;
    @JsonProperty("best_id")
    Long bestId;
    Double accuracy;
    @JsonProperty("created_at")
    String time;

    @JsonProperty("max_combo")
    Integer maxCombo;
    @JsonIgnore
    OsuMode mode;
    @JsonProperty("mode")
    void setMode(String mode){
        this.mode = OsuMode.getMode(mode);
    }

    @JsonIgnore
    List<Mod> mods;
    @JsonProperty("mods")
    void setMods(List<String> mods){
        this.mods = new ArrayList<Mod>();
        for (var s : mods){
            this.mods.add(Mod.fromStr(s));
        }
    }

    Boolean passed;
    Boolean perfect;
    Float pp;
    String rank;
    Boolean replay;
    Integer score;

    Statistics statistics;

    BeatMap beatmap;
    BeatMapSet beatmapset;

    OsuUser user;

    @Override
    public String toString() {
        return "Score{" +
                "id=" + id +
                ", bestId=" + bestId +
                ", accuracy=" + accuracy +
                ", time='" + time + '\'' +
                ", maxCombo=" + maxCombo +
                ", mode=" + mode +
                ", mods=" + mods +
                ", passed=" + passed +
                ", perfect=" + perfect +
                ", pp=" + pp +
                ", rank='" + rank + '\'' +
                ", replay=" + replay +
                ", score=" + score +
                ", statistics=" + statistics +
                ", beatmap=" + beatmap +
                ", beatmapset=" + beatmapset +
                ", user=" + user +
                '}';
    }

    public Long getId() {
        return id;
    }

    public Long getBestId() {
        return bestId;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public String getTime() {
        return time;
    }

    public Integer getMaxCombo() {
        return maxCombo;
    }

    public OsuMode getMode() {
        return mode;
    }

    public List<Mod> getMods() {
        return mods;
    }

    public Boolean getPassed() {
        return passed;
    }

    public Boolean getPerfect() {
        return perfect;
    }

    public Float getPp() {
        return pp;
    }

    public String getRank() {
        return rank;
    }

    public Boolean getReplay() {
        return replay;
    }

    public Integer getScore() {
        return score;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public BeatMap getBeatmap() {
        return beatmap;
    }

    public BeatMapSet getBeatmapset() {
        return beatmapset;
    }

    public OsuUser getUser() {
        return user;
    }
}
