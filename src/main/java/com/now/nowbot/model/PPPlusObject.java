package com.now.nowbot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PPPlusObject {
    @JsonProperty("UserID")
    private Long uid;
    @JsonProperty("UserName")
    private String name;

    @JsonIgnore
    private LocalDateTime time = LocalDateTime.now();
    @JsonProperty("PerformanceTotal")
    private Double Total;
    @JsonProperty("JumpAimTotal")
    private Double Jump;
    @JsonProperty("FlowAimTotal")
    private Double Flow;
    @JsonProperty("AccuracyTotal")
    private Double Acc;
    @JsonProperty("StaminaTotal")
    private Double Sta;
    @JsonProperty("SpeedTotal")
    private Double Spd;
    @JsonProperty("PrecisionTotal")
    private Double Pre;

    public PPPlusObject() {
    }

    public PPPlusObject(Long uid, LocalDateTime time, Double total, Double jump, Double flow, Double acc, Double sta, Double spd, Double pre) {
        this.uid = uid;
        this.time = time;
        Total = total;
        Jump = jump;
        Flow = flow;
        Acc = acc;
        Sta = sta;
        Spd = spd;
        Pre = pre;
    }


    public Long getUid() {
        return uid;
    }

    public PPPlusObject setUid(Long uid) {
        this.uid = uid;
        return this;
    }

    public String getName() {
        return name;
    }


    public PPPlusObject setName(String name) {
        this.name = name;
        return this;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public PPPlusObject setTime(LocalDateTime time) {
        this.time = time;
        return this;
    }

    public Double getTotal() {
        return Total;
    }

    public PPPlusObject setTotal(Double total) {
        Total = total;
        return this;
    }

    public Double getJump() {
        return Jump;
    }


    public PPPlusObject setJump(Double jump) {
        Jump = jump;
        return this;
    }

    public Double getFlow() {
        return Flow;
    }


    public PPPlusObject setFlow(Double flow) {
        Flow = flow;
        return this;
    }

    public Double getAcc() {
        return Acc;
    }

    public PPPlusObject setAcc(Double acc) {
        Acc = acc;
        return this;
    }

    public Double getSta() {
        return Sta;
    }

    public PPPlusObject setSta(Double sta) {
        Sta = sta;
        return this;
    }

    public Double getSpd() {
        return Spd;
    }


    public PPPlusObject setSpd(Double spd) {
        Spd = spd;
        return this;
    }

    public Double getPre() {
        return Pre;
    }

    public PPPlusObject setPre(Double pre) {
        Pre = pre;
        return this;
    }
}
