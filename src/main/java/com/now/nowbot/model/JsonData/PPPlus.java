package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PPPlus {
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

    public PPPlus() {
    }

    public PPPlus(Long uid, LocalDateTime time, Double total, Double jump, Double flow, Double acc, Double sta, Double spd, Double pre) {
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

    public PPPlus setUid(Long uid) {
        this.uid = uid;
        return this;
    }

    public String getName() {
        return name;
    }


    public PPPlus setName(String name) {
        this.name = name;
        return this;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public PPPlus setTime(LocalDateTime time) {
        this.time = time;
        return this;
    }

    public Double getTotal() {
        return Total;
    }

    public PPPlus setTotal(Double total) {
        Total = total;
        return this;
    }

    public Double getJump() {
        return Jump;
    }


    public PPPlus setJump(Double jump) {
        Jump = jump;
        return this;
    }

    public Double getFlow() {
        return Flow;
    }


    public PPPlus setFlow(Double flow) {
        Flow = flow;
        return this;
    }

    public Double getAcc() {
        return Acc;
    }

    public PPPlus setAcc(Double acc) {
        Acc = acc;
        return this;
    }

    public Double getSta() {
        return Sta;
    }

    public PPPlus setSta(Double sta) {
        Sta = sta;
        return this;
    }

    public Double getSpd() {
        return Spd;
    }


    public PPPlus setSpd(Double spd) {
        Spd = spd;
        return this;
    }

    public Double getPre() {
        return Pre;
    }

    public PPPlus setPre(Double pre) {
        Pre = pre;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PPPlus{");
        sb.append("uid=").append(uid);
        sb.append(", name='").append(name).append('\'');
        sb.append(", time=").append(time);
        sb.append(", Total=").append(Total);
        sb.append(", Jump=").append(Jump);
        sb.append(", Flow=").append(Flow);
        sb.append(", Acc=").append(Acc);
        sb.append(", Sta=").append(Sta);
        sb.append(", Spd=").append(Spd);
        sb.append(", Pre=").append(Pre);
        sb.append('}');
        return sb.toString();
    }
}
