package com.now.nowbot.model;

import java.time.LocalDateTime;

public class PPPlusObject {
    private Long uid;
    private String name;
    private LocalDateTime time;
    private Double Total;
    private Double Junp;
    private Double Flow;
    private Double Acc;
    private Double Sta;
    private Double Spd;
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

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public Double getTotal() {
        return Total;
    }

    public void setTotal(Double total) {
        Total = total;
    }

    public Double getJunp() {
        return Junp;
    }

    public void setJunp(Double junp) {
        Junp = junp;
    }

    public Double getFlow() {
        return Flow;
    }

    public void setFlow(Double flow) {
        Flow = flow;
    }

    public Double getAcc() {
        return Acc;
    }

    public void setAcc(Double acc) {
        Acc = acc;
    }

    public Double getSta() {
        return Sta;
    }

    public void setSta(Double sta) {
        Sta = sta;
    }

    public Double getSpd() {
        return Spd;
    }

    public void setSpd(Double spd) {
        Spd = spd;
    }

    public Double getPre() {
        return Pre;
    }

    public void setPre(Double pre) {
        Pre = pre;
    }
}
