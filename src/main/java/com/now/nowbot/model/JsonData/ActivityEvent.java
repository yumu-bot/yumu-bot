package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.model.enums.OsuMode;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ActivityEvent {
    public record Beatmap(String title, String url) {
    }

    public record BeatmapSet(String title, String url) {
    }


    public record User(String name, String url, String previousUsername) {
    }

    public enum EventType {
        achievement("achievement", "user"),
        beatmapPlaycount("beatmap", "count"),
        beatmapsetApprove("approval", "beatmapset", "user"),
        beatmapsetDelete("beatmapset"),
        beatmapsetRevive("beatmapset", "user"),
        beatmapsetUpdate("beatmapset", "user"),
        beatmapsetUpload("beatmapset", "user"),

        rank("scoreRank", "rank", "mode", "beatmap", "user"),
        rankLost("mode", "beatmap", "user"),
        userSupportAgain("user"),
        userSupportFirst("user"),
        userSupportGift("user"),
        usernameChange("user"),
        ;
        final String[] field;

        EventType(String... f) {
            field = f;
        }
    }

    @JsonProperty("created_at")
//    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    OffsetDateTime createdAt;
    @JsonProperty("id")
    Long id;

    @JsonIgnore
    EventType type;

    @JsonProperty("type")
    void setType(String type) {
        this.type = EventType.valueOf(type);
    }
    /*****************************下面属性根据不同 type 进行变动, 只包含 enum 中括号内的属性************************************/
    @JsonProperty("count")
    Integer count;
    @JsonProperty("approval")
    Integer approval;
    @JsonProperty("scoreRank")
    String scoreRank;
    @JsonProperty("rank")
    Integer rank;
    @JsonProperty("mode")
    String mode;

    @JsonProperty("achievement")
    String achievement;

    @JsonProperty("user")
    User user;
    @JsonProperty("beatmap")
    Beatmap beatmap;
    @JsonProperty("beatmapset")
    BeatmapSet beatmapSet;

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getApproval() {
        return approval;
    }

    public void setApproval(Integer approval) {
        this.approval = approval;
    }

    public String getScoreRank() {
        return scoreRank;
    }

    public void setScoreRank(String scoreRank) {
        this.scoreRank = scoreRank;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public String getAchievement() {
        return achievement;
    }

    public void setAchievement(String achievement) {
        this.achievement = achievement;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Beatmap getBeatmap() {
        return beatmap;
    }

    public void setBeatmap(Beatmap beatmap) {
        this.beatmap = beatmap;
    }

    public BeatmapSet getBeatmapSet() {
        return beatmapSet;
    }

    public void setBeatmapSet(BeatmapSet beatmapSet) {
        this.beatmapSet = beatmapSet;
    }

    public EventType getType() {
        return type;
    }

    public OsuMode getMode() {
        return OsuMode.getMode(mode);
    }

    public boolean isTypeMapping() {
        return (type == EventType.beatmapsetApprove ||
                type == EventType.beatmapsetDelete ||
                type == EventType.beatmapsetRevive ||
                type == EventType.beatmapsetUpdate ||
                type == EventType.beatmapsetUpload
        );
    }

    @Override
    public String toString() {
        return STR."ActivityEvent{createdAt=\{createdAt}, id=\{id}, count=\{count}, approval=\{approval}, scoreRank='\{scoreRank}\{'\''}, rank=\{rank}, mode='\{mode}\{'\''}, achievement='\{achievement}\{'\''}, type=\{type}, user=\{user}, beatmap=\{beatmap}, beatmapSet=\{beatmapSet}\{'}'}";
    }
}
