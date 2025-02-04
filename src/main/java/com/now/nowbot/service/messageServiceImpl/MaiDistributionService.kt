package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.json.MaiBestScore
import com.now.nowbot.model.json.MaiFit.ChartData
import com.now.nowbot.model.json.MaiScore
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_NAME
import com.now.nowbot.util.command.FLAG_QQ_ID
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor
import kotlin.math.min

@Service("MAI_DIST") class MaiDistributionService(
    private val maimaiApiService: MaimaiApiService, private val imageService: ImageService
) : MessageService<MaiDistributionService.MaiDistParam> {

    data class MaiDistParam(val qq: Long?, val name: String?, val isMySelf: Boolean = false)

    data class MaiDistScore(
        @JsonProperty("score") val score: MaiScore, @JsonProperty("chart") val chart: ChartData, @JsonProperty("rating") val rating: Int
    )

    data class PanelMDParam(
        @JsonProperty("user") val user: MaiBestScore.User,
        @JsonProperty("scores_latest") val deluxe: List<MaiDistScore>,
        @JsonProperty("scores") val standard: List<MaiDistScore>,
        @JsonProperty("rating") val rating: Int,
        @JsonProperty("count") val count: Int,
        @JsonProperty("size") val size: Int,
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "user" to user,
                "scores_latest" to deluxe,
                "scores" to standard,
                "rating" to rating,
                "count" to count,
                "size" to size
            )
        }
    }

    override fun isHandle(
        event: MessageEvent, messageText: String, data: MessageService.DataValue<MaiDistParam>
    ): Boolean {
        val matcher = Instruction.MAI_DIST.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        val nameStr = (matcher.group(FLAG_NAME) ?: "").trim()
        val qqStr = (matcher.group(FLAG_QQ_ID) ?: "").trim()

        val qq = if (event.isAt) {
            event.target
        } else if (StringUtils.hasText(qqStr)) {
            qqStr.toLong()
        } else {
            event.sender.id
        }

        if (StringUtils.hasText(nameStr)) {
            data.value = MaiDistParam(null, nameStr, false)
        } else if (qq == event.sender.id) {
            data.value = MaiDistParam(qq, null, true)
        } else {
            data.value = MaiDistParam(qq, null, false)
        }

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: MaiDistParam) {
        val best = MaiBestScoreService.getBestScores(param.qq, param.name, param.isMySelf, maimaiApiService)

        if (best.charts.standard.isEmpty() && best.charts.deluxe.isEmpty()) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Exceed_Score)
        }

        maimaiApiService.insertPosition(best.charts.deluxe, false)
        maimaiApiService.insertPosition(best.charts.standard, true)

        maimaiApiService.insertMaimaiAliasForScore(best.charts.deluxe)
        maimaiApiService.insertMaimaiAliasForScore(best.charts.standard)

        maimaiApiService.insertSongData(best.charts.deluxe)
        maimaiApiService.insertSongData(best.charts.standard)

        val deluxe =
            getMaiFitChartData(best.charts.deluxe, maimaiApiService).sortedByDescending { it.rating }
        val standard =
            getMaiFitChartData(best.charts.standard, maimaiApiService).sortedByDescending { it.rating }

        val rating =
            standard.sumOf { it.rating } + deluxe.sumOf { it.rating }
        val count =
            standard
                .filter { it.chart.fit > 0.0 }
                .count { it.score.star < it.chart.fit } + deluxe
                .filter { it.chart.fit > 0.0 }
                .count { it.score.star < it.chart.fit }

        val size = standard.count {
            it.chart.fit > 0.0
        } + deluxe.count {
            it.chart.fit > 0.0
        }

        val image = try {
            val body = PanelMDParam(best.getUser(), deluxe, standard, rating, count, size)

            imageService.getPanel(body.toMap(), "MD")
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "舞萌拟合")
        }

        try {
            event.reply(image)
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "舞萌拟合")
        }
    }

    companion object {

        // 打包
        private fun getMaiFitChartData(scores: List<MaiScore>, maimaiApiService: MaimaiApiService): List<MaiDistScore> {
            val chartData = getChartData(scores, maimaiApiService)

            return scores.map {
                val chart = chartData[it.songID * 10 + it.index] ?: ChartData()
                val rating = getDistsRating(it, chart)

                return@map MaiDistScore(it, chart, rating)
            }
        }

        // 多线程获取歌曲的的 ChartData
        private fun getChartData(scores: List<MaiScore>, maimaiApiService: MaimaiApiService): Map<Long, ChartData> {
            val charts = ConcurrentHashMap<Long, ChartData>()

            val actions = scores.map {
                return@map AsyncMethodExecutor.Supplier<Unit> {
                    charts[it.songID * 10 + it.index] =
                        maimaiApiService.getMaimaiChartData(it.songID).getOrNull(it.index) ?: ChartData()
                    }
                }

            AsyncMethodExecutor.AsyncSupplier(actions)

            return charts.toMap()
        }

        // 计算对应成绩的对应 DX 评分
        private fun getDistsRating(score: MaiScore , chart: ChartData): Int {
            val star = if (chart.fit > 0.0) {
                chart.fit
            } else {
                score.star
            }

            val achievements = score.achievements

            return getRating(star, achievements)
        }

        // star 就是定数 (ds)
        private fun getRating(star: Double, achievements: Double): Int {

            // 评级系数，100.5 以上就是 22.4
            val accLevel = when {
                achievements >= 100.5 -> 22.4
                achievements in 100.0.rangeUntil(100.5) -> 21.6
                achievements in 99.5.rangeUntil(100.0) -> 21.1
                achievements in 99.0.rangeUntil(99.5) -> 20.8
                achievements in 98.0.rangeUntil(99.0) -> 20.3
                achievements in 97.0.rangeUntil(98.0) -> 20.0
                achievements in 94.0.rangeUntil(97.0) -> 16.8
                achievements in 90.0.rangeUntil(94.0) -> 15.2
                achievements in 80.0.rangeUntil(90.0) -> 13.6
                achievements in 75.0.rangeUntil(80.0) -> 12.0
                achievements in 70.0.rangeUntil(75.0) -> 11.2
                achievements in 60.0.rangeUntil(70.0) -> 9.6
                achievements in 50.0.rangeUntil(60.0) -> 8.0
                achievements in 40.0.rangeUntil(50.0) -> 6.4
                achievements in 30.0.rangeUntil(40.0) -> 4.8
                achievements in 20.0.rangeUntil(30.0) -> 3.2
                achievements in 10.0.rangeUntil(20.0) -> 1.6
                else -> 0.0
            }

            return floor(star * min(achievements, 100.5) / 100.0 * accLevel).toInt()
        }
    }
}
