package com.now.nowbot.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class InfoLogStatistics extends Statistics {
    @JsonProperty("time")
    LocalDateTime logTime;

    public LocalDateTime getLogTime() {
        return logTime;
    }

    public void setLogTime(LocalDateTime logTime) {
        this.logTime = logTime;
    }
}
