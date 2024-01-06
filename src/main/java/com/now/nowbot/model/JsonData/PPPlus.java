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
    private Double total;
    @JsonProperty("JumpAimTotal")
    private Double jump;
    @JsonProperty("FlowAimTotal")
    private Double flow;
    @JsonProperty("AccuracyTotal")
    private Double acc;
    @JsonProperty("StaminaTotal")
    private Double sta;
    @JsonProperty("SpeedTotal")
    private Double spd;
    @JsonProperty("PrecisionTotal")
    private Double pre;

    public PPPlus() {
    }

    public PPPlus(Long uid, LocalDateTime time, Double total, Double jump, Double flow, Double acc, Double sta, Double spd, Double pre) {
        this.uid = uid;
        this.time = time;
        this.total = total;
        this.jump = jump;
        this.flow = flow;
        this.acc = acc;
        this.sta = sta;
        this.spd = spd;
        this.pre = pre;
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
        return total;
    }

    public PPPlus setTotal(Double total) {
        this.total = total;
        return this;
    }

    public Double getJump() {
        return jump;
    }


    public PPPlus setJump(Double jump) {
        this.jump = jump;
        return this;
    }

    public Double getFlow() {
        return flow;
    }


    public PPPlus setFlow(Double flow) {
        this.flow = flow;
        return this;
    }

    public Double getAcc() {
        return acc;
    }

    public PPPlus setAcc(Double acc) {
        this.acc = acc;
        return this;
    }

    public Double getSta() {
        return sta;
    }

    public PPPlus setSta(Double sta) {
        this.sta = sta;
        return this;
    }

    public Double getSpd() {
        return spd;
    }


    public PPPlus setSpd(Double spd) {
        this.spd = spd;
        return this;
    }

    public Double getPre() {
        return pre;
    }

    public PPPlus setPre(Double pre) {
        this.pre = pre;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PPPlus{");
        sb.append("uid=").append(uid);
        sb.append(", name='").append(name).append('\'');
        sb.append(", time=").append(time);
        sb.append(", Total=").append(total);
        sb.append(", Jump=").append(jump);
        sb.append(", Flow=").append(flow);
        sb.append(", Acc=").append(acc);
        sb.append(", Sta=").append(sta);
        sb.append(", Spd=").append(spd);
        sb.append(", Pre=").append(pre);
        sb.append('}');
        return sb.toString();
    }
}
