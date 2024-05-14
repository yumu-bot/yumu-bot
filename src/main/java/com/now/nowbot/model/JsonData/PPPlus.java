package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
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

    public record AdvancedStats(
            List<Double> index,
            Double general,
            Double advanced,
            Double sum,
            Double approval
    ) {}

    Double accuracy;
    Integer combo;
    Stats difficulty;
    Stats performance;
    Stats skill;
    AdvancedStats advancedStats;

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

    public Stats getSkill() {
        if (skill == null && difficulty != null) {
            skill = new Stats(
                    calculateSkillValue(difficulty.aim),
                    calculateSkillValue(difficulty.jumpAim),
                    calculateSkillValue(difficulty.flowAim),
                    calculateSkillValue(difficulty.precision),
                    calculateSkillValue(difficulty.speed),
                    calculateSkillValue(difficulty.stamina),
                    calculateSkillValue(difficulty.accuracy),
                    calculateSkillValue(difficulty.total)
            );
            return skill;
        } else return null;
    }

    public void setSkill(Stats skill) {
        this.skill = skill;
    }

    public AdvancedStats getAdvancedStats() {
        return advancedStats;
    }

    public void setAdvancedStats(AdvancedStats advancedStats) {
        this.advancedStats = advancedStats;
    }


    @Override
    public String toString() {
        return STR."PPPlus{accuracy=\{accuracy}, combo=\{combo}, difficulty=\{difficulty}, performance=\{performance}, skill=\{skill}\{'}'}";
    }
}
