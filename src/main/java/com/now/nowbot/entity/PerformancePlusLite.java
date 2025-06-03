package com.now.nowbot.entity;

import com.now.nowbot.model.osu.PPPlus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "performance_plus")
public class PerformancePlusLite {
    public static final short SCORE = 0;
    public static final short MAP   = 1;
    @Id
    @Column(name = "id", nullable = false)
    private             Long  id;

    /**
     * 暂定 0: score pp+, 1: beatmap pp+
     */
    @Column(name = "type", nullable = false)
    private short type = 0;

    private Double aim;

    @Column(name = "jump")
    private Double jumpAim;

    @Column(name = "flow")
    private Double flowAim;

    private Double precision;

    private Double speed;

    private Double stamina;

    private Double accuracy;

    private Double total;

    public PerformancePlusLite() {
    }

    public PerformancePlusLite(Long id, PPPlus.Stats score, short type) {
        this.id = id;
        this.aim = score.aim();
        this.jumpAim = score.jumpAim();
        this.flowAim = score.flowAim();
        this.precision = score.precision();
        this.speed = score.speed();
        this.stamina = score.stamina();
        this.accuracy = score.accuracy();
        this.total = score.total();
        this.type = type;
    }

    public PPPlus.Stats toStats() {
        return new PPPlus.Stats(aim, jumpAim, flowAim, precision, speed, stamina, accuracy, total);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getAim() {
        return aim;
    }

    public void setAim(Double aim) {
        this.aim = aim;
    }

    public Double getJumpAim() {
        return jumpAim;
    }

    public void setJumpAim(Double jumpAim) {
        this.jumpAim = jumpAim;
    }

    public Double getFlowAim() {
        return flowAim;
    }

    public void setFlowAim(Double flowAim) {
        this.flowAim = flowAim;
    }

    public Double getPrecision() {
        return precision;
    }

    public void setPrecision(Double precision) {
        this.precision = precision;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public Double getStamina() {
        return stamina;
    }

    public void setStamina(Double stamina) {
        this.stamina = stamina;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }
}