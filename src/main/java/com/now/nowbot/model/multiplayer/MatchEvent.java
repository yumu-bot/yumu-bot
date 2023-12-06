package com.now.nowbot.model.multiplayer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchEvent implements Comparable<MatchEvent> {
    @JsonProperty("detail")
    Detail detail;
    Long id;
    OffsetDateTime timestamp;
    @JsonProperty("user_id")
    Integer userId;

    @JsonProperty("game")
    MatchRound round;

    @Override
    public int compareTo(@NotNull MatchEvent o) {
        return (int) (o.id - id);
    }

    public record Detail(String type, String text) {
    }

    public Detail getDetail() {
        return detail;
    }

    public void setDetail(Detail detail) {
        this.detail = detail;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @Nullable
    public MatchRound getRound() {
        return round;
    }

    public void setRound(@Nullable MatchRound round) {
        this.round = round;
    }
}
