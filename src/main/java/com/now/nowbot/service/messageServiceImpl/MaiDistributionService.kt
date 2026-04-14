package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.dao.MaiDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.maimai.MaiBestScore
import com.now.nowbot.model.maimai.MaiFit.ChartData
import com.now.nowbot.model.maimai.MaiScore
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService

import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.MaimaiUtil
import com.now.nowbot.util.command.FLAG_NAME
import com.now.nowbot.util.command.FLAG_QQ_ID
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

@Service("MAI_DIST") class MaiDistributionService(
    private val maimaiApiService: MaimaiApiService,
    private val imageService: ImageService,
    private val maiDao: MaiDao,
) : MessageService<MaiDistributionService.MaiDistParam> {

    data class MaiDistParam(val qq: Long?, val name: String?, val isMySelf: Boolean = false)

    data class MaiDistScore(
        @field:JsonProperty("score") val score: MaiScore,
        @field:JsonProperty("chart") val chart: ChartData,
        @field:JsonProperty("rating") val rating: Int
    )

    data class PanelMDParam(
        @field:JsonProperty("user") val user: MaiBestScore.User,
        @field:JsonProperty("scores_latest") val deluxe: List<MaiDistScore>,
        @field:JsonProperty("scores") val standard: List<MaiDistScore>,
        @field:JsonProperty("rating") val rating: Int,
        @field:JsonProperty("count") val count: Int,
        @field:JsonProperty("size") val size: Int,
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

        val qq = if (event.hasAt()) {
            event.target
        } else if (qqStr.isNotBlank()) {
            qqStr.toLong()
        } else {
            event.sender.contactID
        }

        if (nameStr.isNotBlank()) {
            data.value = MaiDistParam(null, nameStr, false)
        } else if (qq == event.sender.contactID) {
            data.value = MaiDistParam(qq, null, true)
        } else {
            data.value = MaiDistParam(qq, null, false)
        }

        return true
    }

    override fun handleMessage(event: MessageEvent, param: MaiDistParam): ServiceCallStatistic? {
        val best = MaiBestScoreService.getBestScores(param.qq, param.name, maimaiApiService)

        if (best.charts.standard.isEmpty() && best.charts.deluxe.isEmpty()) {
            throw NoSuchElementException.BestScore(best.name)
        }

        maimaiApiService.insert(best.charts)

        val deluxe = getMaiFitChartData(best.charts.deluxe, maimaiApiService)
            .sortedByDescending { it.rating }
        val standard = getMaiFitChartData(best.charts.standard, maimaiApiService)
            .sortedByDescending { it.rating }

        val rating =
            standard.sumOf { it.rating } + deluxe.sumOf { it.rating }
        val count =
            standard.filter { it.chart.fit > 0.0 }
                .count { it.score.star < it.chart.fit } +
                    deluxe.filter { it.chart.fit > 0.0 }
                        .count { it.score.star < it.chart.fit }

        val size = standard.count {
            it.chart.fit > 0.0
        } + deluxe.count {
            it.chart.fit > 0.0
        }

        val body = PanelMDParam(best.getUser(maiDao), deluxe, standard, rating, count, size)

        val image = imageService.getPanel(body.toMap(), "MD")

        try {
            event.reply(image)
        } catch (_: Exception) {
            throw IllegalStateException.Send("舞萌拟合")
        }

        return ServiceCallStatistic.building(event) {
            setParam(mapOf(
                "mais" to (body.standard.map { it.score.songID } + body.deluxe.map { it.score.songID }).toSet()
            ))
        }
    }

    companion object {

        // 打包
        private fun getMaiFitChartData(scores: List<MaiScore>, maimaiApiService: MaimaiApiService): List<MaiDistScore> {
            val chartData = getChartData(scores, maimaiApiService)

            return scores.map {
                val chart = chartData[it.independentID] ?: ChartData()
                val rating = getDistsRating(it, chart)

                return@map MaiDistScore(it, chart, rating)
            }
        }

        // 多线程获取歌曲的的 ChartData
        private fun getChartData(scores: List<MaiScore>, maimaiApiService: MaimaiApiService): Map<Long, ChartData> {
            ConcurrentHashMap<Long, ChartData>()

            val actions = scores.map {
                Callable {
                    it.independentID to
                            (maimaiApiService.getMaimaiChartData(it.songID).getOrNull(it.index) ?: ChartData())
                    }
                }

            return AsyncMethodExecutor.awaitList(actions).toMap()
        }

        // 计算对应成绩的对应 DX 评分
        private fun getDistsRating(score: MaiScore, chart: ChartData): Int {
            val star = if (chart.fit > 0.0) {
                chart.fit
            } else {
                score.star
            }

            val achievements = score.achievements

            return MaimaiUtil.getRating(star, achievements)
        }
    }
}
