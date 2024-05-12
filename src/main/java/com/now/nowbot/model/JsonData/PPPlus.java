package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class PPPlus {

    public record Stats(
            Double aim,
            @JsonProperty("jumpAim")
            Double jumpAim,
            @JsonProperty("flowAim")
            Double flowAim,
            Double precision,
            Double speed,
            Double stamina,
            Double accuracy,
            Double total
    ) {}

    Double accuracy;
    Integer combo;
    Stats difficulty;
    Stats performance;
    Stats skill = calculateSkill();

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Integer getCombo() {
        return combo;
    }

    public void setCombo(Integer combo) {
        this.combo = combo;
    }

    public Stats getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Stats difficulty) {
        this.difficulty = difficulty;
    }

    public Stats getPerformance() {
        return performance;
    }

    public void setPerformance(Stats performance) {
        this.performance = performance;
    }

    // 计算出 legacy PP+ 显示的接近 1000 的值
    public static double calculateSkillValue(double difficultyValue) {
        return Math.pow(difficultyValue, 3d) * 3.9d;
    }

    public Stats calculateSkill() {
        if (Objects.nonNull(difficulty)) {
            return new Stats(
                    calculateSkillValue(difficulty.aim),
                    calculateSkillValue(difficulty.jumpAim),
                    calculateSkillValue(difficulty.flowAim),
                    calculateSkillValue(difficulty.precision),
                    calculateSkillValue(difficulty.speed),
                    calculateSkillValue(difficulty.stamina),
                    calculateSkillValue(difficulty.accuracy),
                    calculateSkillValue(difficulty.total)
            );
        }
        return null;
    }

    public Stats getSkill() {
        return skill;
    }

    public void setSkill(Stats skill) {
        this.skill = skill;
    }

    @Override
    public String toString() {
        return STR."PPPlus{accuracy=\{accuracy}, combo=\{combo}, difficulty=\{difficulty}, performance=\{performance}, skill=\{skill}\{'}'}";
    }
}
