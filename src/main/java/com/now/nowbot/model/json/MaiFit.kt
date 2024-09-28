package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonProperty

// 拟合难度类
class MaiFit {
    // 这是不同歌曲的统计结果，Key 是字符串，对应歌曲 ID（SID）
    @JsonProperty("charts") var charts: Map<String, List<ChartData>> = mapOf()

    // 这是不同难度下的统计结果，Key 是字符串，对应难度（level）
    @JsonProperty("diff_data") var diffData: Map<String, DiffData> = mapOf()

    class ChartData {
        // 统计计数
        @JsonProperty("cnt") var count: Int = 0

        // 定数的实际显示，0.6-0.9 后面会多一个 +，宴会场谱面会多一个 ?
        @JsonProperty("diff") var level: String = ""

        // 拟合难度，也就是水鱼认为它的难度
        @JsonProperty("fit_diff") var fit: Double = 0.0

        // 平均准确率
        @JsonProperty("avg") var achievements: Double = 0.0

        // 平均 DX 分数
        @JsonProperty("avg_dx") var score: Double = 0.0

        // 标准差
        @JsonProperty("std_dev") var standardDeviation: Double = 0.0

        // 难度的评级分布（依次对应 d, c, b, bb, bbb, a, aa, aaa, s, sp, ss, ssp, sss, sssp）
        @JsonProperty("dist") var distribution: List<Double> = listOf()

        // 难度的 Full Combo 分布（依次对应 无、fc、fcp、ap、app）
        @JsonProperty("fc_dist") var fullComboDistribution: List<Double> = listOf()
    }

    class DiffData {
        // 平均准确率
        var achievements: Double = 0.0

        // 难度的评级分布（依次对应 d, c, b, bb, bbb, a, aa, aaa, s, sp, ss, ssp, sss, sssp）
        @JsonProperty("dist") var distribution: List<Double> = listOf()

        // 难度的 Full Combo 分布（依次对应 无、fc、fcp、ap、app）
        @JsonProperty("fc_dist") var fullComboDistribution: List<Double> = listOf()
    }

    fun getChartData(songID: String, index: Int): ChartData {
        val charts = charts[songID] ?: return ChartData()

        return if (index >= charts.size) {
            charts.last()
        } else {
            charts[index]
        }
    }

    fun getDiffData(chartData: ChartData): DiffData {
        return diffData[chartData.level] ?: DiffData()
    }
}
