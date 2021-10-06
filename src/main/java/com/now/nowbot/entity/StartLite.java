package com.now.nowbot.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "qq_start")
public class StartLite {
    @Id
    private Integer qq;

    private Double start;
    //上次刷新时间
    private LocalDateTime lastTime;

    public int getQq() {
        return qq;
    }

    public void setQq(Integer qq) {
        this.qq = qq;
    }

    public double getStart() {
        return start;
    }

    public void setStart(Double start) {
        this.start = start;
    }

    public LocalDateTime getLastTime() {
        return lastTime;
    }

    public void setLastTime(LocalDateTime lastTime) {
        this.lastTime = lastTime;
    }
}
