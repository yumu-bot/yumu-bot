package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BeatMap {
    @JsonProperty("id")
    Integer id;
    @JsonProperty("beatmapset_id")
    Integer beatmapsetId;
    @JsonProperty("difficulty_rating")
    Integer difficultyRating;
    @JsonProperty("mode")
    String mode;
    @JsonProperty("mode_int")
    Integer modeInt;
    @JsonProperty("status")
    String status;
    @JsonProperty("total_length")
    Integer totalLength;
    @JsonProperty("hit_length")
    Boolean hitLength;
    @JsonProperty("user_id")
    String userId;
    @JsonProperty("version")
    String version;
    @JsonProperty("accuracy")
    Float od;
    @JsonProperty("ar")
    Float ar;
    @JsonProperty("cs")
    Float cs;
    @JsonProperty("drain")
    Float hp;
    @JsonProperty("bpm")
    Float bpm;
    @JsonProperty("max_combo")
    Integer maxCombo;
    @JsonProperty("convert")
    Boolean convert;
    @JsonProperty("is_scoreable")
    Boolean scoreable;
    @JsonProperty("last_updated")
    String updatedTime;
    @JsonProperty("passcount")
    String passcount;
    @JsonProperty("playcount")
    String playcount;
    @JsonProperty("url")
    String url;
    @JsonProperty("checksum")
    String md5;
    @JsonProperty("count_sliders")
    Integer sliders;
    @JsonProperty("count_spinners")
    Integer spinners;
    @JsonProperty("count_circles")
    Integer circles;
    @JsonProperty("beatmapset")
    BeatMapSet beatMapSet;

    @Override
    public String toString() {
        return "BeatMap{" +
                "id=" + id +
                ", beatmapsetId=" + beatmapsetId +
                ", difficultyRating=" + difficultyRating +
                ", mode='" + mode + '\'' +
                ", modeInt=" + modeInt +
                ", status='" + status + '\'' +
                ", totalLength=" + totalLength +
                ", hitLength=" + hitLength +
                ", userId='" + userId + '\'' +
                ", version='" + version + '\'' +
                ", od=" + od +
                ", ar=" + ar +
                ", cs=" + cs +
                ", hp=" + hp +
                ", bpm=" + bpm +
                ", maxCombo=" + maxCombo +
                ", convert=" + convert +
                ", scoreable=" + scoreable +
                ", updatedTime='" + updatedTime + '\'' +
                ", passcount='" + passcount + '\'' +
                ", playcount='" + playcount + '\'' +
                ", url='" + url + '\'' +
                ", md5='" + md5 + '\'' +
                ", sliders=" + sliders +
                ", spinners=" + spinners +
                ", circles=" + circles +
                ", \nbeatMapSet=" + beatMapSet +
                '}';
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getBeatmapsetId() {
        return beatmapsetId;
    }

    public void setBeatmapsetId(Integer beatmapsetId) {
        this.beatmapsetId = beatmapsetId;
    }

    public Integer getDifficultyRating() {
        return difficultyRating;
    }

    public void setDifficultyRating(Integer difficultyRating) {
        this.difficultyRating = difficultyRating;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Integer getModeInt() {
        return modeInt;
    }

    public void setModeInt(Integer modeInt) {
        this.modeInt = modeInt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalLength() {
        return totalLength;
    }

    public void setTotalLength(Integer totalLength) {
        this.totalLength = totalLength;
    }

    public Boolean getHitLength() {
        return hitLength;
    }

    public void setHitLength(Boolean hitLength) {
        this.hitLength = hitLength;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Float getOd() {
        return od;
    }

    public void setOd(Float od) {
        this.od = od;
    }

    public Float getAr() {
        return ar;
    }

    public void setAr(Float ar) {
        this.ar = ar;
    }

    public Float getCs() {
        return cs;
    }

    public void setCs(Float cs) {
        this.cs = cs;
    }

    public Float getHp() {
        return hp;
    }

    public void setHp(Float hp) {
        this.hp = hp;
    }

    public Float getBpm() {
        return bpm;
    }

    public void setBpm(Float bpm) {
        this.bpm = bpm;
    }

    public Integer getMaxCombo() {
        return maxCombo;
    }

    public void setMaxCombo(Integer maxCombo) {
        this.maxCombo = maxCombo;
    }

    public Boolean getConvert() {
        return convert;
    }

    public void setConvert(Boolean convert) {
        this.convert = convert;
    }

    public Boolean getScoreable() {
        return scoreable;
    }

    public void setScoreable(Boolean scoreable) {
        this.scoreable = scoreable;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getPasscount() {
        return passcount;
    }

    public void setPasscount(String passcount) {
        this.passcount = passcount;
    }

    public String getPlaycount() {
        return playcount;
    }

    public void setPlaycount(String playcount) {
        this.playcount = playcount;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public Integer getSliders() {
        return sliders;
    }

    public void setSliders(Integer sliders) {
        this.sliders = sliders;
    }

    public Integer getSpinners() {
        return spinners;
    }

    public void setSpinners(Integer spinners) {
        this.spinners = spinners;
    }

    public Integer getCircles() {
        return circles;
    }

    public void setCircles(Integer circles) {
        this.circles = circles;
    }

    public BeatMapSet getBeatMapSet() {
        return beatMapSet;
    }

    public void setBeatMapSet(BeatMapSet beatMapSet) {
        this.beatMapSet = beatMapSet;
    }
}
