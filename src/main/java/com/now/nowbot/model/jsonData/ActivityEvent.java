package com.now.nowbot.model.jsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ActivityEvent {
    public record Achievement(
            @JsonProperty("icon_url") String icon_url,
            @JsonProperty("id") Integer AID,
            String name, String grouping, String ordering, String slug, String description, String mode, String instructions) {
    }

    public record EventBeatMap(String title, String url) {
    }

    public record EventBeatMapSet(String title, String url) {
    }

    public record EventUser(String name, String url, String previousUsername) {
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

    //@JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    @JsonProperty("created_at")
    OffsetDateTime createdAt;

    @JsonProperty("id")
    Long EID;

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
    String approval;

    @JsonProperty("scoreRank")
    String scoreRank;

    @JsonProperty("rank")
    Integer rank;

    @JsonProperty("mode")
    String mode;

    @JsonProperty("achievement")
    Achievement achievement;

    @JsonProperty("user")
    EventUser user;

    @JsonProperty("beatmap")
    EventBeatMap beatmap;

    @JsonProperty("beatmapset")
    EventBeatMapSet beatmapSet;

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getEID() {
        return EID;
    }

    public void setEID(Long EID) {
        this.EID = EID;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getApproval() {
        return approval;
    }

    public void setApproval(String approval) {
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

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Achievement getAchievement() {
        return achievement;
    }

    public void setAchievement(Achievement achievement) {
        this.achievement = achievement;
    }

    public EventUser getUser() {
        return user;
    }

    public void setUser(EventUser user) {
        this.user = user;
    }

    public EventBeatMap getBeatmap() {
        return beatmap;
    }

    public void setBeatmap(EventBeatMap beatmap) {
        this.beatmap = beatmap;
    }

    public EventBeatMapSet getBeatmapSet() {
        return beatmapSet;
    }

    public void setBeatmapSet(EventBeatMapSet beatmapSet) {
        this.beatmapSet = beatmapSet;
    }

    public boolean isMapping() {
        return Objects.equals(type, EventType.beatmapsetApprove) || Objects.equals(type, EventType.beatmapsetDelete) || Objects.equals(type, EventType.beatmapsetRevive) || Objects.equals(type, EventType.beatmapsetUpdate) || Objects.equals(type, EventType.beatmapsetUpload);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ActivityEvent e) {
            return Objects.equals(this.getType(), e.getType()) && Objects.equals(this.getBeatmapSet().url(), e.getBeatmapSet().url());
        }
        return false;
    }

    @Override
    public String toString() {
        return STR."ActivityEvent{createdAt=\{createdAt}, EID=\{EID}, type=\{type}, count=\{count}, approval=\{approval}, scoreRank='\{scoreRank}\{'\''}, rank=\{rank}, mode='\{mode}\{'\''}, achievement=\{achievement}, user=\{user}, beatmap=\{beatmap}, beatmapSet=\{beatmapSet}\{'}'}";
    }
}
