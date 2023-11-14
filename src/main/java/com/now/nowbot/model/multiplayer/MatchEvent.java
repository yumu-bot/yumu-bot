package com.now.nowbot.model.multiplayer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;
import java.util.Map;

public class MatchEvent {
    Long id;
    OffsetDateTime timestamp;
    @JsonProperty("user_id")
    Integer userId;
    @Nullable
    @JsonProperty("game")
    MatchRound round;

    public record Detail(String type, String text) {}

    @JsonIgnore
    Detail detail;
    @JsonProperty("detail")
    void setDetail(Map<String, String> detail) {
        if (detail != null) this.detail = new Detail(detail.get("type"), detail.get("text"));
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
