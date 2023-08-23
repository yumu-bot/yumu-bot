package com.now.nowbot.model.match;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchEvent {
    Long id;
    OffsetDateTime timestamp;
    @JsonProperty("user_id")
    Integer UID;
    GameInfo game;

    public Long getID() {
        return id;
    }

    public void setID(Long id) {
        this.id = id;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getUserID() {
        return UID;
    }

    public void setUserID(Integer UID) {
        this.UID = UID;
    }

    public GameInfo getGame() {
        return game;
    }

    public void setGame(GameInfo game) {
        this.game = game;
    }
}
