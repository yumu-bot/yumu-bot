package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.List;

// 拟合难度类
public class MaiFit {
    // 这是不同歌曲的统计结果，Key 是字符串，对应歌曲 ID（SID）
    @JsonProperty("charts")
    Map<String, List<ChartData>> charts;

    // 这是不同难度下的统计结果，Key 是字符串，对应难度（level）
    @JsonProperty("diff_data")
    Map<String, DiffData> diffData;

    public static class ChartData {
        // 统计计数
        @JsonProperty("cnt")
        Integer count;

        // 定数的实际显示，0.6-0.9 后面会多一个 +，宴会场谱面会多一个 ?
        @JsonProperty("diff")
        String level;

        // 拟合难度，也就是水鱼认为它的难度
        @JsonProperty("fit_diff")
        Double fit;

        // 平均准确率
        @JsonProperty("avg")
        Double achievements;

        // 平均 DX 分数
        @JsonProperty("avg_dx")
        Double score;

        // 标准差
        @JsonProperty("std_dev")
        Double standardDeviation;

        // 难度的评级分布（依次对应 d, c, b, bb, bbb, a, aa, aaa, s, sp, ss, ssp, sss, sssp）
        @JsonProperty("dist")
        List<Double> distribution;

        // 难度的 Full Combo 分布（依次对应 无、fc、fcp、ap、app）
        @JsonProperty("fc_dist")
        List<Double> fullComboDistribution;

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public Double getFit() {
            return fit;
        }

        public void setFit(Double fit) {
            this.fit = fit;
        }

        public Double getAchievements() {
            return achievements;
        }

        public void setAchievements(Double achievements) {
            this.achievements = achievements;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public Double getStandardDeviation() {
            return standardDeviation;
        }

        public void setStandardDeviation(Double standardDeviation) {
            this.standardDeviation = standardDeviation;
        }

        public List<Double> getDistribution() {
            return distribution;
        }

        public void setDistribution(List<Double> distribution) {
            this.distribution = distribution;
        }

        public List<Double> getFullComboDistribution() {
            return fullComboDistribution;
        }

        public void setFullComboDistribution(List<Double> fullComboDistribution) {
            this.fullComboDistribution = fullComboDistribution;
        }
    }

    public static class DiffData {
        // 平均准确率
        Double achievements;

        // 难度的评级分布（依次对应 d, c, b, bb, bbb, a, aa, aaa, s, sp, ss, ssp, sss, sssp）
        @JsonProperty("dist")
        List<Double> distribution;

        // 难度的 Full Combo 分布（依次对应 无、fc、fcp、ap、app）
        @JsonProperty("fc_dist")
        List<Double> fullComboDistribution;

        public Double getAchievements() {
            return achievements;
        }

        public void setAchievements(Double achievements) {
            this.achievements = achievements;
        }

        public List<Double> getDistribution() {
            return distribution;
        }

        public void setDistribution(List<Double> distribution) {
            this.distribution = distribution;
        }

        public List<Double> getFullComboDistribution() {
            return fullComboDistribution;
        }

        public void setFullComboDistribution(List<Double> fullComboDistribution) {
            this.fullComboDistribution = fullComboDistribution;
        }
    }

    public Map<String, List<ChartData>> getCharts() {
        return charts;
    }

    public void setCharts(Map<String, List<ChartData>> charts) {
        this.charts = charts;
    }

    public Map<String, DiffData> getDiffData() {
        return diffData;
    }

    public void setDiffData(Map<String, DiffData> diffData) {
        this.diffData = diffData;
    }
}
