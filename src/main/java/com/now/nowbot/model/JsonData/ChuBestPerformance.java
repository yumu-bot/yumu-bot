package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ChuBestPerformance {
    // 在游戏里的名字
    @JsonProperty("nickname")
    String name;

    // best30 + recent10
    // 这就是 BP
    @JsonProperty("records")
    Records records;

    public record Records(
            @JsonProperty("b30") List<ChuScore> best30,
            @JsonProperty("r10") List<ChuScore> recent10
    ) {}

    // 在查分器里的名字
    @JsonProperty("username")
    String probername;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Records getRecords() {
        return records;
    }

    public void setRecords(Records records) {
        this.records = records;
    }

    public String getProbername() {
        return probername;
    }

    public void setProbername(String probername) {
        this.probername = probername;
    }
}
