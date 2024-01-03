package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.model.enums.OsuMode;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
    Integer hitLength;
    @JsonProperty("user_id")
    Long userId;
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

    Integer ranked;
    String url;
    @JsonProperty("checksum")
    String md5;
    @JsonProperty("count_sliders")
    Integer sliders;
    @JsonProperty("count_spinners")
    Integer spinners;
    @JsonProperty("count_circles")
    Integer circles;
    @JsonIgnore
    List<Integer> retryList;
    @JsonIgnore
    List<Integer> failedList;

    /**
     * 仅在查询铺面信息的时候有
     */
    @JsonProperty("beatmapset")
    BeatMapSet beatMapSet;

    @JsonProperty("failtimes")
    public void setFailTimes(JsonNode data) {
        if (data.hasNonNull("fail") && data.get("fail").isArray()) {
            retryList = StreamSupport.stream(data.get("fail").spliterator(), false)
                    .map(jsonNode -> jsonNode.asInt(0))
                    .collect(Collectors.toList());
        }
        if (data.hasNonNull("exit") && data.get("exit").isArray()) {
            failedList = StreamSupport.stream(data.get("exit").spliterator(), false)
                    .map(jsonNode -> jsonNode.asInt(0))
                    .collect(Collectors.toList());
        }
    }

    public Long getId() {
        return id;
    }

    //不能动（因为有数据库存储
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
        if (mode == null) {
            mode = OsuMode.getMode(modeInt).getName();
        }
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

    public boolean isRanked() {
        return (Objects.equals(status, "ranked") || Objects.equals(status, "qualified") || Objects.equals(status, "loved") || Objects.equals(status, "approved"));
    }

    public Integer getTotalLength() {
        return totalLength;
    }

    public void setTotalLength(Integer totalLength) {
        this.totalLength = totalLength;
    }

    public Integer getHitLength() {
        return hitLength;
    }

    public void setHitLength(Integer hitLength) {
        this.hitLength = hitLength;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * getDifficulty
     */
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

    public Float getBPM() {
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

    public Integer getRanked() {
        return ranked;
    }

    public void setRanked(Integer ranked) {
        this.ranked = ranked;
    }

    public List<Integer> getBeatMapRatingList() {return null;}

    public double getBeatMapRating() {
        return 0f;
    }

    public List<Integer> getBeatMapRetryList() {return retryList;}

    public void SetBeatMapRetryList(List<Integer> retryList) {
        this.retryList = retryList;
    }

    public int getBeatMapRetryCount() {
        List<Integer> rl = getBeatMapRetryList();
        if (rl == null || rl.isEmpty()) return 0;
        return rl.stream().reduce(Integer::sum).orElse(0);
    }

    public List<Integer> getBeatMapFailedList() {return failedList;}
    
    public void SetBeatMapFailedList(List<Integer> failedList) {
        this.failedList = failedList;
    }
    
    public int getBeatMapFailedCount() {
        List<Integer> fl = getBeatMapFailedList();

        if (fl == null || fl.isEmpty()) {
            return 0;
        }

        return fl.stream().reduce(Integer::sum).orElse(0);
    }

    @Override
    public String toString() {
        return STR."BeatMap{id=\{id}, beatmapsetId=\{beatmapsetId}, difficultyRating=\{difficultyRating}, mode='\{mode}\{'\''}, modeInt=\{modeInt}, status='\{status}\{'\''}, totalLength=\{totalLength}, hitLength=\{hitLength}, userId=\{userId}, version='\{version}\{'\''}, od=\{od}, ar=\{ar}, cs=\{cs}, hp=\{hp}, bpm=\{bpm}, maxCombo=\{maxCombo}, convert=\{convert}, scoreable=\{scoreable}, updatedTime='\{updatedTime}\{'\''}, passcount=\{passcount}, playcount=\{playcount}, ranked=\{ranked}, url='\{url}\{'\''}, md5='\{md5}\{'\''}, sliders=\{sliders}, spinners=\{spinners}, circles=\{circles}, retryList=\{retryList}, failedList=\{failedList}, beatMapSet=\{beatMapSet}\{'}'}";
    }
}
