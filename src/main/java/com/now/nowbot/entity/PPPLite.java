package com.now.nowbot.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

//@Entity
//@Table(name = "pp_plus")
public class PPPLite {
    @Id
    @GeneratedValue
    Long id;

    Long userId;
    //记录时间
    private LocalDateTime date;

    private double Total;

    private double Junp;

    private double Flow;

    private double Acc;

    private double Sta;

    private double Spd;

    private double Pre;

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

    public void setTotal(double total) {
        Total = total;
    }

    public double getJunp() {
        return Junp;
    }

    public void setJunp(double junp) {
        Junp = junp;
    }

    public double getFlow() {
        return Flow;
    }

    public void setFlow(double flow) {
        Flow = flow;
    }

    public double getAcc() {
        return Acc;
    }

    public void setAcc(double acc) {
        Acc = acc;
    }

    public double getSta() {
        return Sta;
    }

    public void setSta(double sta) {
        Sta = sta;
    }

    public double getSpd() {
        return Spd;
    }

    public void setSpd(double spd) {
        Spd = spd;
    }

    public double getPre() {
        return Pre;
    }

    public void setPre(double pre) {
        Pre = pre;
    }
}
