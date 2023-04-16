package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
    @JsonProperty("fail")
    List<Integer> retryList;
    @JsonProperty("exit")
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
            failedList  = StreamSupport.stream(data.get("exit").spliterator(), false)
                    .map(jsonNode -> jsonNode.asInt(0))
                    .collect(Collectors.toList());
        }
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

    public Integer getHitLength() {
        return hitLength;
    }

    public void setHitLength(Integer hitLength) {
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

    public List<Integer> getBeatMapRatingList() {return null;}

    public double getBeatMapRating() {
        List<Integer> rl = getBeatMapRatingList();
        if (null == rl) return 0D;
        double r = 0;
        double sum = 0;
        int i;

        for (i = 0; i < rl.size(); i++) {
            sum = sum + rl.get(i);
        }

        for (int j = 0; j <= 10; j++) {
            r = r + j / 10D * rl.get(j) / sum;
        }
        return r;
    }

    public List<Integer> getBeatMapRetryList() {return retryList;}

    public void SetBeatMapRetryList(List<Integer> retryList) {
        this.retryList = retryList;
    }

    public int getBeatMapRetryCount() {
        List<Integer> fl = getBeatMapRetryList();
        if (null == fl) return 0;
        int sum = 0;

        for (int i = 0; i < fl.size(); i++) {
            sum = sum + fl.get(i);
        }
        return sum;
    }

    public List<Integer> getBeatMapFailedList() {return failedList;}
    
    public void SetBeatMapFailedList(List<Integer> failedList) {
        this.failedList = failedList;
    }
    
    public int getBeatMapFailedCount() {
        List<Integer> fl = getBeatMapFailedList();

        if (null == fl) {
            return 0;
        }

        int sum = 0;
        for (int i = 0; i < fl.size(); i++) {
            sum = sum + fl.get(i);
        }
        return sum;
    }
    

    public String toString() {
        final StringBuilder sb = new StringBuilder("BeatMap{");
        sb.append("id=").append(id);
        sb.append(", beatmapsetId=").append(beatmapsetId);
        sb.append(", difficultyRating=").append(difficultyRating);
        sb.append(", mode='").append(mode).append('\'');
        sb.append(", modeInt=").append(modeInt);
        sb.append(", status='").append(status).append('\'');
        sb.append(", totalLength=").append(totalLength);
        sb.append(", hitLength=").append(hitLength);
        sb.append(", userId=").append(userId);
        sb.append(", version='").append(version).append('\'');
        sb.append(", od=").append(od);
        sb.append(", ar=").append(ar);
        sb.append(", cs=").append(cs);
        sb.append(", hp=").append(hp);
        sb.append(", bpm=").append(bpm);
        sb.append(", maxCombo=").append(maxCombo);
        sb.append(", convert=").append(convert);
        sb.append(", scoreable=").append(scoreable);
        sb.append(", updatedTime='").append(updatedTime).append('\'');
        sb.append(", passcount=").append(passcount);
        sb.append(", playcount=").append(playcount);
        sb.append(", url='").append(url).append('\'');
        sb.append(", md5='").append(md5).append('\'');
        sb.append(", sliders=").append(sliders);
        sb.append(", spinners=").append(spinners);
        sb.append(", circles=").append(circles);
        sb.append(", beatMapSet=").append(beatMapSet);
        sb.append('}');
        return sb.toString();
    }
}
