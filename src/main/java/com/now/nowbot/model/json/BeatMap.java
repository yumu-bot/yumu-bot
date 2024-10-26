package com.now.nowbot.model.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.model.enums.OsuMode;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.NonNull;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeatMap implements Cloneable {
    // 等同于 BeatMapExtended
    // 原有属性

    @JsonProperty("beatmapset_id")
    Integer setID;

    @JsonProperty("difficulty_rating")
    Float starRating;

    Long id; //这个不能改

    String mode;

    String status;

    @JsonProperty("total_length")
    Integer totalLength;

    @JsonProperty("user_id")
    Long mapperID;

    @JsonProperty("version")
    String difficultyName;

    @JsonProperty("beatmapset")
    BeatMapSet beatMapSet;

    @JsonProperty("checksum")
    String md5;

    //retry == fail, fail == exit
    @JsonProperty("failtimes")
    public void setFailTimes(JsonNode data) {
        retryList = getList(data, "fail");
        failList = getList(data, "exit");
    }

    private List<Integer> getList(@NotNull JsonNode data, String fieldName) {
        List<Integer> list = new ArrayList<>();
        if (data.hasNonNull(fieldName) && data.get(fieldName).isArray()) {
            list = StreamSupport.stream(data.get(fieldName).spliterator(), false)
                    .map(n -> n.asInt(0))
                    .toList();
        }
        return list;
    }

    List<Integer> retryList;

    List<Integer> failList;

    @JsonProperty("max_combo")
    Integer maxCombo;

    // Extended
    @JsonProperty("accuracy")
    Float OD;

    @JsonProperty("ar")
    Float AR;

    @JsonProperty("bpm")
    Float BPM;

    Boolean convert;

    @JsonProperty("count_circles")
    Integer circles;

    @JsonProperty("count_sliders")
    Integer sliders;

    @JsonProperty("count_spinners")
    Integer spinners;

    @JsonProperty("cs")
    Float CS;

    @JsonProperty("deleted_at")
    OffsetDateTime deletedAt;

    @JsonProperty("drain")
    Float HP;

    @JsonProperty("hit_length")
    Integer hitLength;

    @JsonProperty("is_scoreable")
    Boolean scoreAble;

    @JsonProperty("last_updated")
    String lastUpdated;

    @JsonProperty("mode_int")
    Integer modeInt;

    @JsonProperty("passcount")
    Integer passCount;

    @JsonProperty("playcount")
    Integer playCount;

    Integer ranked;

    String url;

    //自己算
    Integer retry;

    //自己算
    Integer fail;

    //自己算
    Boolean hasLeaderBoard;

    //自己取
    public String getPreviewName() {
        if (beatMapSet != null) {
            return beatMapSet.artist + " - " + beatMapSet.title + " (" + beatMapSet.creator + ") [" + difficultyName + "]";
        } else if (id != null){
            return id.toString();
        } else {
            return "";
        }
    }

    //不能动（因为有数据库存储
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBeatMapID() {
        return id;
    }

    //不能动（因为有数据库存储
    public void setBeatMapID(Long id) {
        this.id = id;
    }

    public Integer getSetID() {
        return setID;
    }

    public void setSetID(Integer setID) {
        this.setID = setID;
    }

    public Float getStarRating() {
        return starRating;
    }

    public void setStarRating(Float starRating) {
        this.starRating = starRating;
    }

    public String getMode() {
        if (Objects.isNull(mode)) {
            mode = OsuMode.getMode(modeInt).getName();
        }
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
        this.modeInt = (int) OsuMode.getMode(mode).getModeValue();
    }

    public OsuMode getOsuMode() {
        return OsuMode.getMode(getMode());
    }

    public void setOsuMode(OsuMode mode) {
        this.mode = mode.getName();
        this.modeInt = (int) mode.getModeValue();
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

    public Long getMapperID() {
        return mapperID;
    }

    public void setMapperID(Long mapperID) {
        this.mapperID = mapperID;
    }

    public String getDifficultyName() {
        return difficultyName;
    }

    public void setDifficultyName(String difficultyName) {
        this.difficultyName = difficultyName;
    }

    @Nullable
    public BeatMapSet getBeatMapSet() {
        return beatMapSet;
    }

    public void setBeatMapSet(@Nullable BeatMapSet beatMapSet) {
        this.beatMapSet = beatMapSet;
    }

    @Nullable
    public String getMd5() {
        return md5;
    }

    public void setMd5(@Nullable String md5) {
        this.md5 = md5;
    }

    public List<Integer> getRetryList() {
        return retryList;
    }

    public void setRetryList(List<Integer> retryList) {
        this.retryList = retryList;
    }

    public List<Integer> getFailList() {
        return failList;
    }

    public void setFailList(List<Integer> failList) {
        this.failList = failList;
    }

    public Integer getMaxCombo() {
        return maxCombo;
    }

    public void setMaxCombo(Integer maxCombo) {
        this.maxCombo = maxCombo;
    }

    public Float getOD() {
        return OD;
    }

    public void setOD(Float OD) {
        this.OD = OD;
    }

    public Float getAR() {
        return AR;
    }

    public void setAR(Float AR) {
        this.AR = AR;
    }

    public Float getBPM() {
        return BPM;
    }

    public void setBPM(Float BPM) {
        this.BPM = BPM;
    }

    public Boolean getConvert() {
        return convert;
    }

    public void setConvert(Boolean convert) {
        this.convert = convert;
    }

    public Integer getCircles() {
        return circles;
    }

    public void setCircles(Integer circles) {
        this.circles = circles;
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

    public Float getCS() {
        return CS;
    }

    public void setCS(Float CS) {
        this.CS = CS;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Float getHP() {
        return HP;
    }

    public void setHP(Float HP) {
        this.HP = HP;
    }

    public Integer getHitLength() {
        return hitLength;
    }

    public void setHitLength(Integer hitLength) {
        this.hitLength = hitLength;
    }

    public Boolean getScoreAble() {
        return scoreAble;
    }

    public void setScoreAble(Boolean scoreAble) {
        this.scoreAble = scoreAble;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Integer getModeInt() {
        if (Objects.isNull(modeInt)) {
            modeInt = (int) OsuMode.getMode(mode).getModeValue();
        }
        return modeInt;
    }

    public void setModeInt(Integer modeInt) {
        this.modeInt = modeInt;
        this.mode = OsuMode.getMode(modeInt).getName();
    }

    public Integer getPassCount() {
        return passCount;
    }

    public void setPassCount(Integer passCount) {
        this.passCount = passCount;
    }

    public Integer getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Integer playCount) {
        this.playCount = playCount;
    }

    public Integer getRanked() {
        return ranked;
    }

    public void setRanked(Integer ranked) {
        this.ranked = ranked;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getRetry() {
        if (Objects.nonNull(retryList) && !retryList.isEmpty()) {
            return retryList.stream().reduce(Integer::sum).orElse(0);
        }
        return 0;
    }

    public void setRetry(Integer retry) {
        this.retry = retry;
    }

    public Integer getFail() {
        if (Objects.nonNull(failList) && !failList.isEmpty()) {
            return failList.stream().reduce(Integer::sum).orElse(0);
        }
        return 0;
    }

    public void setFail(Integer fail) {
        this.fail = fail;
    }

    public boolean hasLeaderBoard() {
        if (Objects.nonNull(status)) {
            hasLeaderBoard = (Objects.equals(status, "ranked") || Objects.equals(status, "qualified") || Objects.equals(status, "loved") || Objects.equals(status, "approved"));
        } else {
            switch (ranked) {
                case 1, 2, 3, 4 -> hasLeaderBoard = true;
                case null, default -> hasLeaderBoard = false;
            }
        }
        return hasLeaderBoard;
    }

    public void setHasLeaderBoard(Boolean hasLeaderBoard) {
        this.hasLeaderBoard = hasLeaderBoard;
    }

    public BeatMap() {}

    public BeatMap(long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return STR."BeatMap{setID=\{setID}, starRating=\{starRating}, id=\{id}, mode='\{mode}\{'\''}, status='\{status}\{'\''}, totalLength=\{totalLength}, mapperID=\{mapperID}, difficultyName='\{difficultyName}\{'\''}, beatMapSet=\{beatMapSet}, md5='\{md5}\{'\''}, retryList=\{retryList}, failList=\{failList}, maxCombo=\{maxCombo}, OD=\{OD}, AR=\{AR}, BPM=\{BPM}, convert=\{convert}, circles=\{circles}, sliders=\{sliders}, spinners=\{spinners}, CS=\{CS}, deletedAt=\{deletedAt}, HP=\{HP}, hitLength=\{hitLength}, scoreAble=\{scoreAble}, lastUpdated='\{lastUpdated}\{'\''}, modeInt=\{modeInt}, passCount=\{passCount}, playCount=\{playCount}, ranked=\{ranked}, url='\{url}\{'\''}, retry=\{retry}, fail=\{fail}, hasLeaderBoard=\{hasLeaderBoard}\{'}'}";
    }

    @Override
    public BeatMap clone() {
        try {
            return (BeatMap) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    // 如果当前实例是 beatMapLite，返回以这个实例为主，以 b 为辅的新 beatMap
    @NonNull
    public static BeatMap extend(@Nullable BeatMap lite, BeatMap extended) {
        if (extended == null) {
            return Objects.requireNonNullElseGet(lite, BeatMap::new);
        } else if (lite == null || lite.getCS() == null){
            lite = extended;
            return lite;
        }

        extended.setStarRating(lite.getStarRating());
        extended.setCS(lite.getCS());
        extended.setAR(lite.getAR());
        extended.setOD(lite.getOD());
        extended.setHP(lite.getHP());
        extended.setTotalLength(lite.getTotalLength());
        extended.setHitLength(lite.getHitLength());
        extended.setBPM(lite.getBPM());

        lite = extended;
        return lite;
    }
}
