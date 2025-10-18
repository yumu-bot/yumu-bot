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
import org.springframework.web.util.UriBuilder
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

@Service("PP_PLUS_SEV") class PerformancePlusService(
    config: FileConfig,
    private val performancePlusLiteRepository: PerformancePlusLiteRepository,
    private val webClient: WebClient,
    private val beatmapApiService: OsuBeatmapApiService
) {
    private val osuPath: Path = Path.of(config.osuFilePath)

    fun getMapPerformancePlus(beatmapId: Long, modList: List<LazerMod?>?): PPPlus? {
        checkFile(beatmapId)
        val mods: Optional<String>
        if (modList.isNullOrEmpty()) {
            mods = Optional.empty()
        } else {
            val s = JacksonUtil.toJson(modList)
            mods = Optional.of(URLEncoder.encode(s, StandardCharsets.UTF_8))
        }
        val p = performancePlusLiteRepository.findBeatMapPPPById(beatmapId)
        if (p.isPresent) {
            val result = PPPlus()
            result.difficulty = p.get().toStats()
            return result
        }
        return webClient.get().uri { u: UriBuilder ->
                u.scheme(API_SCHEME).host(API_HOST).port(API_PORT).path("/api/calculation")
                    .queryParam("BeatmapId", beatmapId).queryParamIfPresent("Mods", mods).build()
            }.retrieve().bodyToMono(PPPlus::class.java).block()
    }

    fun clearCache(beatmapId: String?) {
        val p = ProcessBuilder("pkill", "-9", "-f", "Difficulty.PerformancePlus")
        Thread.startVirtualThread {
            try {
                p.start()
            } catch (e: IOException) {
                log.error("", e)
            }
        }
    }

    @Throws(TipsException::class) fun calculateUserPerformance(bps: List<LazerScore>): PPPlus.Stats {
        val ppPlus = getScorePerformancePlus(bps)

        var aim = 0.0
        var jumpAim = 0.0
        var flowAim = 0.0
        var precision = 0.0
        var speed = 0.0
        var stamina = 0.0
        var accuracy = 0.0
        var total = 0.0

        val callables: MutableList<Callable<String>> = ArrayList(7)
        val ppPlusMap: MutableMap<String, List<Double>> = ConcurrentHashMap(7)


        // 逐个排序
        callables.add(createCallable("aim", ppPlusMap, ppPlus, PPPlus.Stats::aim))
        callables.add(createCallable("jumpAim", ppPlusMap, ppPlus, PPPlus.Stats::jumpAim))
        callables.add(createCallable("flowAim", ppPlusMap, ppPlus, PPPlus.Stats::flowAim))
        callables.add(createCallable("precision", ppPlusMap, ppPlus, PPPlus.Stats::precision))
        callables.add(createCallable("speed", ppPlusMap, ppPlus, PPPlus.Stats::speed))
        callables.add(createCallable("stamina", ppPlusMap, ppPlus, PPPlus.Stats::stamina))
        callables.add(createCallable("accuracy", ppPlusMap, ppPlus, PPPlus.Stats::accuracy))
        callables.add(createCallable("total", ppPlusMap, ppPlus, PPPlus.Stats::total))

        AsyncMethodExecutor.awaitCallableExecute(callables)

        // 计算加权和
        var weight = 1.0 / 0.95

        for (n in ppPlus.indices) {
            weight *= 0.95

            aim += ppPlusMap["aim"]!![n] * weight
            jumpAim += ppPlusMap["jumpAim"]!![n] * weight
            flowAim += ppPlusMap["flowAim"]!![n] * weight
            precision += ppPlusMap["precision"]!![n] * weight
            speed += ppPlusMap["speed"]!![n] * weight
            stamina += ppPlusMap["stamina"]!![n] * weight
            accuracy += ppPlusMap["accuracy"]!![n] * weight
            total += ppPlusMap["total"]!![n] * weight
        }

        return PPPlus.Stats(aim, jumpAim, flowAim, precision, speed, stamina, accuracy, total)
    }

    @Throws(TipsException::class) fun getScorePerformancePlus(scores: Iterable<LazerScore>): List<PPPlus> {
        val scoreIDs = scores.map { it.scoreID }
        val ppPlusList = performancePlusLiteRepository.findScorePPP(scoreIDs)
        val ppPlusMap = ppPlusList.associateBy { it.id }

        // 挑选出没有记录的 score
        val body: MutableList<ScorePerformancePlus> = ArrayList()
        val postDataID = LinkedList<Long>()
        val allScoreIDs = LinkedList<Long>()
        for (score in scores) {
            allScoreIDs.add(score.scoreID)
            if (ppPlusMap.containsKey(score.scoreID)) {
                continue
            }
            postDataID.add(score.scoreID)
            checkFile(score.beatmap.beatmapID)
            val combo = score.maxCombo
            val misses = score.statistics.miss
            val meh = score.statistics.meh
            val oks = score.statistics.ok
            body.add(
                ScorePerformancePlus(
                    score.beatmap.beatmapID.toString() + "", score.mods, combo, misses, meh, oks
                )
            )
        }

        if (body.isEmpty()) {
            return allScoreIDs.mapNotNull { key: Long -> ppPlusMap[key] }.map {
                val p = PPPlus()
                p.performance = it.toStats()
                p
            }
        }
        val result: List<PPPlus>?

        try {
            result = getScorePerformancePlus(body)
        } catch (e: WebClientResponseException) {
            val n = findErrorBid(body)
            getMapPerformancePlus(n.toLong(), listOf<LazerMod>())
            beatmapApiService.refreshBeatmapFileFromDirectory(n.toLong())
            Thread.startVirtualThread { this.clearCache(n) }
            throw IllegalStateException.Fetch("PP+：谱面编号 $n")
        }

        var i = 0
        val data = ArrayList<PerformancePlusLite>(postDataID.size)
        for (scoreId in postDataID) {
            data.add(
                PerformancePlusLite(
                    scoreId, result!![i].performance, PerformancePlusLite.SCORE
                )
            )
            i++
        }

        performancePlusLiteRepository.saveAll(data)

        val allPPPlus = ArrayList<PPPlus>(allScoreIDs.size)
        i = 0
        for (id in allScoreIDs) {
            if (postDataID.contains(id)) {
                allPPPlus.add(result!![i])
                i++
            } else {
                val lite = ppPlusMap[id]
                val ppp = PPPlus()
                ppp.performance = lite!!.toStats()
                allPPPlus.add(ppp)
            }
        }
        return allPPPlus
    }

    // beatmapId 居然要 String ??? [https://difficalcy.syrin.me/api-reference/difficalcy-osu/#post-apibatchcalculation](啥玩意)
    @JvmRecord internal data class ScorePerformancePlus(
        @field:JsonProperty("beatmapId") @param:JsonProperty(
            "beatmapId"
        ) val beatmapId: String, val mods: List<LazerMod>, val combo: Int, val misses: Int, val mehs: Int, val oks: Int
    )

    private fun getScorePerformancePlus(body: List<ScorePerformancePlus>): List<PPPlus>? {
        return webClient.post().uri { u: UriBuilder ->
                u.scheme(API_SCHEME).host(API_HOST).port(API_PORT).path("/api/batch/calculation").build()
            }.contentType(MediaType.APPLICATION_JSON).bodyValue(JacksonUtil.toJson(body)).retrieve()
            .bodyToMono(JsonNode::class.java).map { node: JsonNode? ->
                JacksonUtil.parseObjectList(
                    node, PPPlus::class.java
                )
            }.block()
    }

    private fun checkFile(beatmapId: Long) {
        val beatmapFiles = osuPath.resolve("$beatmapId.osu")
        if (!Files.isRegularFile(beatmapFiles)) {
            try {
                val fileStr = beatmapApiService.getBeatmapFileString(beatmapId) ?: throw RuntimeException()

                Files.writeString(beatmapFiles, fileStr)
            } catch (e: Throwable) {
                log.error("下载谱面文件失败", e)
                throw RuntimeException("下载谱面文件失败")
            }
        }
    }

    private fun findErrorBid(x: List<ScorePerformancePlus>): String {
        if (x.size > 2) {
            val mid = x.size / 2
            val left = x.subList(0, mid)
            val right = x.subList(mid, x.size)
            return if (testScorePerformancePlus(left)) {
                findErrorBid(left)
            } else {
                findErrorBid(right)
            }
        } else if (x.size == 2) {
            return if (testScorePerformancePlus(x.subList(0, 1))) {
                x.first().beatmapId
            } else {
                x.last().beatmapId
            }
        } else {
            return x.first().beatmapId
        }
    }

    private fun testScorePerformancePlus(allPPP: List<ScorePerformancePlus>): Boolean {
        try {
            getScorePerformancePlus(allPPP)
        } catch (e: Exception) {
            return true
        }
        return false
    }

    private fun createCallable(
        key: String, ppPlusMap: MutableMap<String, List<Double>>, list: List<PPPlus>, function: (PPPlus.Stats) -> Double
    ): Callable<String> {
        return Callable {
            ppPlusMap[key] = list
                .mapNotNull { it.performance }
                .map { function(it) }
                .sortedDescending()

            key
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PerformancePlusService::class.java)
        private var API_SCHEME = "http" // 不用改了
        private var API_HOST = "localhost"
        private var API_PORT = "46880"

        @JvmStatic fun runDevelopment() {
            API_SCHEME = "https"
            API_HOST = "ppp.365246692.xyz"
            API_PORT = "443"
        }
    }
}
