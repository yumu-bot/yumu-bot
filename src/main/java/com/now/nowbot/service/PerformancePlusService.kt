package com.now.nowbot.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.config.FileConfig
import com.now.nowbot.entity.PerformancePlusLite
import com.now.nowbot.mapper.PerformancePlusLiteRepository
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.PPPlus
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

@Service("PP_PLUS_SEV")
class PerformancePlusService(
    config: FileConfig,
    private val performancePlusLiteRepository: PerformancePlusLiteRepository,
    private val webClient: WebClient,
    private val beatmapApiService: OsuBeatmapApiService
) {
    private val osuPath: Path = Path.of(config.osuFilePath)

    fun getMapPerformancePlus(beatmapID: Long, modList: List<LazerMod?>?): PPPlus? {
        checkFile(beatmapID)

        val mods = modList?.takeIf { it.isNotEmpty() }?.let {
            URLEncoder.encode(JacksonUtil.toJson(it), StandardCharsets.UTF_8)
        }

        // 优先从本地库查找
        performancePlusLiteRepository.findBeatmapPPPlusByBeatmapID(beatmapID)?.let {
            return PPPlus().apply { difficulty = it.toStats() }
        }

        return webClient.get()
            .uri { u ->
                u.scheme(API_SCHEME).host(API_HOST).port(API_PORT).path("/api/calculation")
                    .queryParam("BeatmapId", beatmapID)
                    .apply { if (mods != null) queryParam("Mods", mods) }
                    .build()
            }
            .retrieve()
            .bodyToMono(PPPlus::class.java)
            .block()
    }

    fun clearCache(beatmapId: String?) {
        Thread.startVirtualThread {
            runCatching {
                ProcessBuilder("pkill", "-9", "-f", "Difficulty.PerformancePlus").start()
            }.onFailure { log.error("清理缓存进程失败", it) }
        }
    }

    @Throws(TipsException::class)
    fun calculateUserPerformance(bps: List<LazerScore>): PPPlus.Stats {
        val ppPlusList = getScorePerformancePlus(bps)
        val ppPlusMap = ConcurrentHashMap<String, List<Double>>()

        // 定义需要计算的所有属性引用
        val statsProps = listOf(
            "aim" to PPPlus.Stats::aim,
            "jumpAim" to PPPlus.Stats::jumpAim,
            "flowAim" to PPPlus.Stats::flowAim,
            "precision" to PPPlus.Stats::precision,
            "speed" to PPPlus.Stats::speed,
            "stamina" to PPPlus.Stats::stamina,
            "accuracy" to PPPlus.Stats::accuracy,
            "total" to PPPlus.Stats::total
        )

        // 并行计算每个维度的排序列表
        val callables = statsProps.map { (key, prop) ->
            createCallable(key, ppPlusMap, ppPlusList, prop)
        }
        AsyncMethodExecutor.awaitCallableExecute(callables)

        // 利用 pow 计算加权和：sum(value * 0.95^index)
        fun calculateWeightedSum(key: String): Double {
            return ppPlusMap[key]?.asSequence()?.mapIndexed { index, value ->
                value * 0.95.pow(index)
            }?.sum() ?: 0.0
        }

        return PPPlus.Stats(
            calculateWeightedSum("aim"),
            calculateWeightedSum("jumpAim"),
            calculateWeightedSum("flowAim"),
            calculateWeightedSum("precision"),
            calculateWeightedSum("speed"),
            calculateWeightedSum("stamina"),
            calculateWeightedSum("accuracy"),
            calculateWeightedSum("total")
        )
    }

    @Throws(TipsException::class)
    fun getScorePerformancePlus(scores: Iterable<LazerScore>): List<PPPlus> {
        val allScoreIDs = scores.map { it.scoreID }
        val cachedMap = performancePlusLiteRepository.findScorePPPlus(allScoreIDs).associateBy { it.id }

        // 筛选出未缓存的部分
        val (cachedScores, needFetchScores) = scores.partition { cachedMap.containsKey(it.scoreID) }

        if (needFetchScores.isEmpty()) {
            return allScoreIDs.map { id ->
                PPPlus().apply { performance = cachedMap[id]?.toStats() }
            }
        }

        val body = needFetchScores.map { score ->
            checkFile(score.beatmap.beatmapID)
            ScorePerformancePlus(
                beatmapId = score.beatmap.beatmapID.toString(),
                mods = score.mods,
                combo = score.maxCombo,
                misses = score.statistics.miss,
                mehs = score.statistics.meh,
                oks = score.statistics.ok
            )
        }

        val fetchedResults = try {
            getScorePerformancePlusFromApi(body)
        } catch (e: WebClientResponseException) {
            val errorBid = findErrorBid(body)
            // 容错处理
            getMapPerformancePlus(errorBid.toLong(), emptyList())
            beatmapApiService.refreshBeatmapFileFromDirectory(errorBid.toLong())
            clearCache(errorBid)
            throw IllegalStateException.Fetch("PP+：谱面编号 $errorBid")
        } ?: emptyList()

        // 保存新获取的数据
        val newData = needFetchScores.zip(fetchedResults).map { (score, result) ->
            PerformancePlusLite(score.scoreID, result.performance, PerformancePlusLite.SCORE)
        }
        performancePlusLiteRepository.saveAll(newData)

        // 合并结果并保持原始顺序
        val fetchedMap = needFetchScores.map { it.scoreID }.zip(fetchedResults).toMap()

        return allScoreIDs.map { id ->
            fetchedMap[id] ?: PPPlus().apply { performance = cachedMap[id]?.toStats() }
        }
    }

    @JvmRecord
    internal data class ScorePerformancePlus(
        @field:JsonProperty("beatmapId") val beatmapId: String,
        val mods: List<LazerMod>,
        val combo: Int,
        val misses: Int,
        val mehs: Int,
        val oks: Int
    )

    private fun getScorePerformancePlusFromApi(body: List<ScorePerformancePlus>): List<PPPlus>? {
        return webClient.post().uri { u ->
            u.scheme(API_SCHEME).host(API_HOST).port(API_PORT).path("/api/batch/calculation").build()
        }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(JacksonUtil.toJson(body))
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { JacksonUtil.parseObjectList(it, PPPlus::class.java) }
            .block()
    }

    private fun checkFile(beatmapId: Long) {
        val beatmapFile = osuPath.resolve("$beatmapId.osu")
        if (Files.isRegularFile(beatmapFile)) return

        runCatching {
            val fileStr = beatmapApiService.getBeatmapFileString(beatmapId) ?: throw RuntimeException()
            Files.writeString(beatmapFile, fileStr)
        }.onFailure {
            log.error("下载谱面文件失败: $beatmapId", it)
            throw RuntimeException("下载谱面文件失败")
        }
    }

    private fun findErrorBid(list: List<ScorePerformancePlus>): String {
        if (list.size <= 1) return list.first().beatmapId

        val mid = list.size / 2
        val left = list.subList(0, mid)
        val right = list.subList(mid, list.size)

        // 二分法查找导致 API 报错的具体谱面
        return if (testApiError(left)) findErrorBid(left) else findErrorBid(right)
    }

    private fun testApiError(allPPP: List<ScorePerformancePlus>): Boolean = runCatching {
        getScorePerformancePlusFromApi(allPPP)
    }.isFailure

    private fun createCallable(
        key: String,
        ppPlusMap: MutableMap<String, List<Double>>,
        list: List<PPPlus>,
        extractor: (PPPlus.Stats) -> Double
    ) = java.util.concurrent.Callable {
        ppPlusMap[key] = list.mapNotNull { it.performance }.map(extractor).sortedDescending()
        key
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PerformancePlusService::class.java)
        private var API_SCHEME = "http"
        private var API_HOST = "localhost"
        private var API_PORT = "5000"

        fun runDevelopment() {
            API_SCHEME = "https"
            API_HOST = "ppp.365246692.xyz"
            API_PORT = "443"
        }
    }
}