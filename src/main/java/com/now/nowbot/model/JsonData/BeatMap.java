package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeatMap {
    Long id;
    @JsonProperty("beatmapset_id")
    Integer beatmapsetId;
    @JsonProperty("difficulty_rating")
    Float difficultyRating;
    String mode;
    @JsonProperty("mode_int")
    Integer modeInt;
    String status;
    @JsonProperty("total_length")
    Integer totalLength;
    @JsonProperty("hit_length")
    Boolean hitLength;
    @JsonProperty("user_id")
    Integer userId;
    String version;
    @JsonProperty("accuracy")
    Float od;
    Float ar;
    Float cs;
    @JsonProperty("drain")
    Float hp;
    Float bpm;
    @JsonProperty("max_combo")
    Integer maxCombo;
    Boolean convert;
    @JsonProperty("is_scoreable")
    Boolean scoreable;
    @JsonProperty("last_updated")
    String updatedTime;
    Integer passcount;
    Integer playcount;
    String url;
    @JsonProperty("checksum")
    String md5;
    @JsonProperty("count_sliders")
    Integer sliders;
    @JsonProperty("count_spinners")
    Integer spinners;
    @JsonProperty("count_circles")
    Integer circles;
    /**
     * 仅在查询铺面信息的时候有
     */
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getBeatmapsetId() {
        return beatmapsetId;
    }

    public void setBeatmapsetId(Integer beatmapsetId) {
        this.beatmapsetId = beatmapsetId;
    }

    public Float getDifficultyRating() {
        return difficultyRating;
    }

    public void setDifficultyRating(Float difficultyRating) {
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

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Float getOD() {
        return od;
    }

    public void setOD(Float od) {
        this.od = od;
    }

    public Float getAR() {
        return ar;
    }

    public void setAR(Float ar) {
        this.ar = ar;
    }

    public Float getCS() {
        return cs;
    }

    public void setCS(Float cs) {
        this.cs = cs;
    }

    public Float getHP() {
        return hp;
    }

    public void setHP(Float hp) {
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

    public Integer getPasscount() {
        return passcount;
    }

    public void setPasscount(Integer passcount) {
        this.passcount = passcount;
    }

    public Integer getPlaycount() {
        return playcount;
    }

    public void setPlaycount(Integer playcount) {
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
