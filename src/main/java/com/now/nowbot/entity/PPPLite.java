package com.now.nowbot.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "osu_pp_plus")
public class PPPLite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    //记录时间
    private LocalDateTime date;

    private Double Total;

    private Double Junp;

    private Double Flow;

    private Double Acc;

    private Double Sta;

    private Double Spd;

    private Double Pre;

    public PPPLite() {
    }

    public PPPLite(Long userId, LocalDateTime date, Double total, Double junp, Double flow, Double acc, Double sta, Double spd, Double pre) {
        this.userId = userId;
        this.date = date;
        Total = total;
        Junp = junp;
        Flow = flow;
        Acc = acc;
        Sta = sta;
        Spd = spd;
        Pre = pre;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public double getTotal() {
        return Total;
    }

    public void setTotal(Double total) {
        Total = total;
    }

    public double getJunp() {
        return Junp;
    }

    public void setJunp(Double junp) {
        Junp = junp;
    }

    public double getFlow() {
        return Flow;
    }

    public void setFlow(Double flow) {
        Flow = flow;
    }

    public double getAcc() {
        return Acc;
    }

    public void setAcc(Double acc) {
        Acc = acc;
    }

    public double getSta() {
        return Sta;
    }

    public void setSta(Double sta) {
        Sta = sta;
    }

    public double getSpd() {
        return Spd;
    }

    public void setSpd(Double spd) {
        Spd = spd;
    }

    public double getPre() {
        return Pre;
    }

    public void setPre(Double pre) {
        Pre = pre;
    }
}
