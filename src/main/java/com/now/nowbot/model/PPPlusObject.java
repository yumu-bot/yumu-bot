package com.now.nowbot.model;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
/* todo 这个转成OsuGet能直接获取的对象,就不用JSONNode了 */
public class PPPlusObject {
    @JsonProperty("UserID")
    private Long uid;
    @JsonProperty("UserName")
    private String name;

    @JacksonInject()
    private LocalDateTime time = LocalDateTime.now();
    @JsonProperty("PerformanceTotal")
    private Double Total;
    @JsonProperty("JumpAimTotal")
    private Double Junp;
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

    public PPPlusObject(Long uid, LocalDateTime time, Double total, Double junp, Double flow, Double acc, Double sta, Double spd, Double pre) {
        this.uid = uid;
        this.time = time;
        Total = total;
        Junp = junp;
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

    public Double getJunp() {
        return Junp;
    }


    public PPPlusObject setJunp(Double junp) {
        Junp = junp;
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
