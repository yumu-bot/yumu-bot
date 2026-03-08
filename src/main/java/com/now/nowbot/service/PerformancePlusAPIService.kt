package com.now.nowbot.service

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.PPPlus
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.DataUtil.findCauseOfType
import com.now.nowbot.util.FastPower095
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.SocketException

@Component
class PerformancePlusAPIService(
    private val beatmapApiService: OsuBeatmapApiService,
    @Qualifier("rlient") private val restClient: RestClient
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class PlusRetrieveData(
        @JsonIgnore
        val beatmapID: Long = 0,

        @JsonIgnore
        val mods: List<LazerMod>? = null,

        val perfects: Int? = null,
        val greats: Int? = null,
        val goods: Int? = null,
        val oks: Int? = null,
        val mehs: Int? = null,
        val misses: Int? = null,

        val combo: Int? = null,
    ) {
        @get:JsonProperty("beatmapId")
        val beatmapIDStr: String
            get() = beatmapID.toString()

        @get:JsonProperty("mods")
        val cleanMods: List<LazerModLite>?
            get() = mods?.map { LazerModLite(it) }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class LazerModLite(
        @JsonIgnore
        val mod: LazerMod
    ) {
        @get:JsonProperty("acronym")
        val acronym: String = mod.acronym

        @get:JsonProperty("settings")
        val settings: Any? = mod.settings
    }

    fun getMapPerformancePlus(beatmapID: Long, mods: List<LazerMod>?, retryCount: Int = 3): PPPlus? {
        val has = refreshFile(listOf(beatmapID)).toSet()

        if (has.isEmpty()) return null

        val requests = listOf(
            PlusRetrieveData(beatmapID, mods),
        )

        runCatching { return post(requests).firstOrNull() }.onFailure {
            log.warn("表现分加：批量计算失败，启动排雷程序...")

            beatmapApiService.deleteBeatmapFileFromDirectory(beatmapID)

            val has = beatmapApiService.downloadBeatmapFile(listOf(beatmapID)).toSet()

            if (has.isEmpty()) {
                log.warn("表现分加：有无法下载的文件：${beatmapID}。")
                return null
            }

            return getMapPerformancePlus(beatmapID, mods, retryCount - 1)
        }

        throw NetworkException.ComponentException.RequestTimeout()
    }

    fun getMapPerformancePlus(beatmapIDs: Collection<Long>, mods: List<List<LazerMod>>): List<PPPlus> {
        val has = refreshFile(beatmapIDs).toSet()

        val mos: List<List<LazerMod>> = beatmapIDs.mapIndexed { index, id ->
            if (has.contains(id)) {
                mods.getOrNull(index)
            } else {
                null
            }
        }.mapNotNull { it }

        val requests = has.zip(mos)
            .map { (a, b) -> PlusRetrieveData(a, b)}

        return post(requests)
    }

    // 第一个是当前acc，第二个是满，如果算不出来，就是空的
    fun getScorePerformancePlus(score: LazerScore, retryCount: Int = 3): List<PPPlus> {
        val has = refreshFile(listOf(score.beatmapID)).toSet()

        if (has.isEmpty()) return emptyList()

        val retrieve = PlusRetrieveData(
            beatmapID = score.beatmapID,
            mods = score.mods,
            perfects = score.statistics.perfect,
            greats = score.statistics.great,
            goods = score.statistics.good,
            oks = score.statistics.ok,
            mehs = score.statistics.meh,
            misses = score.statistics.miss,
            combo = score.maxCombo
        )

        val retrieveMax = PlusRetrieveData(
            beatmapID = score.beatmapID,
            mods = score.mods,
        )

        runCatching {
            return post(listOf(retrieve, retrieveMax))
        }.onFailure { e ->
            if (e.findCauseOfType<SocketException>() != null) {
                log.info("表现分加：服务未启动，排雷取消。")
                throw IllegalStateException.NotInProgress("PP+")
            }

            log.warn("表现分加：批量计算失败，启动排雷程序...")

            beatmapApiService.deleteBeatmapFileFromDirectory(score.beatmapID)

            val has = beatmapApiService.downloadBeatmapFile(listOf(score.beatmapID)).toSet()

            if (has.isEmpty()) {
                log.warn("表现分加：有无法下载的文件：${score.beatmapID}，已经跳过")
            }

            return getScorePerformancePlus(score, retryCount - 1)
        }

        throw NetworkException.ComponentException.RequestTimeout()
    }

    fun getScoresPerformancePlus(scores: List<LazerScore>, retryCount: Int = 3): List<PPPlus> {
        runCatching {
            // 尝试正常的批量请求
            return getScoresPerformancePlusRaw(scores)
        }.onFailure { e ->
            if (e.findCauseOfType<SocketException>() != null) {
                log.info("表现分加：服务未启动，二分排雷取消。")
                throw IllegalStateException.NotInProgress("PP+")
            }

            if (retryCount <= 0) {
                log.info("表现分加：重试失败。")
                throw e
            }

            log.warn("表现分加：批量计算失败，启动二分排雷程序...")

            // 进入排雷模式，寻找坏 ID
            val badBIDs = findBadBeatmapIDs(scores)

            if (badBIDs.isNotEmpty()) {
                log.warn("表现分加：发现损坏的文件 ID: ${badBIDs.joinToString(", ")}，正在清理并尝试修复...")

                badBIDs.forEach { beatmapApiService.deleteBeatmapFileFromDirectory(it) }

                val has = beatmapApiService.downloadBeatmapFile(badBIDs).toSet()

                val disabled = badBIDs.subtract(has)

                if (disabled.isNotEmpty()) {
                    log.warn("表现分加：有无法下载的文件：${disabled.joinToString(", ")}，已经跳过")
                }

                // 3. 递归重试整个批量请求
                return getScoresPerformancePlus(scores.filterNot { it.beatmapID in disabled }, retryCount - 1)
            }
        }

        throw NetworkException.ComponentException.RequestTimeout()
    }

    private fun getScoresPerformancePlusRaw(scores: List<LazerScore>): List<PPPlus> {
        val has = refreshFile(scores.map { it.beatmapID }).toSet()

        val ss: List<LazerScore> = scores.mapIndexed { index, score ->
            if (has.contains(score.beatmapID)) {
                scores.getOrNull(index)
            } else {
                null
            }
        }.mapNotNull { it }

        val retrieves = ss.map { score ->
            PlusRetrieveData(
                beatmapID = score.beatmapID,
                mods = score.mods,
                perfects = score.statistics.perfect,
                greats = score.statistics.great,
                goods = score.statistics.good,
                oks = score.statistics.ok,
                mehs = score.statistics.meh,
                misses = score.statistics.miss,
                combo = score.maxCombo
            )
        }

        return post(retrieves)
    }

    fun getUserPerformancePlusStats(bests: List<LazerScore>): PPPlus.Stats {
        val results = getScoresPerformancePlus(bests)

        return collect(results.map { it.performance })
    }

    fun findBadBeatmapIDs(scores: List<LazerScore>): List<Long> {
        val results = mutableListOf<Long>()

        fun check(subList: List<LazerScore>) {
            if (subList.isEmpty()) return

            try {
                // 调用原始请求方法（确保它在 400/500 时抛出异常）
                getScoresPerformancePlusRaw(subList)
            } catch (_: Exception) {
                // 如果单条也报错，说明这个就是坏的
                if (subList.size == 1) {
                    results.add(subList[0].beatmapID)
                } else {
                    // 继续二分拆解
                    val mid = subList.size / 2
                    check(subList.subList(0, mid))
                    check(subList.subList(mid, subList.size))
                }
            }
        }

        check(scores)
        return results
    }

    private fun refreshFile(beatmapIDs: Collection<Long>): List<Long> {
        val all = beatmapIDs.toSet()

        val exists = all.filter { beatmapApiService.hasBeatmapFileFromDirectory(it) }.toSet()
        val notExists = beatmapApiService.downloadBeatmapFile(all.subtract(exists))

        val hasFile = exists + notExists

        return all.filter { hasFile.contains(it) }
    }


    private fun post(body: List<PlusRetrieveData>): List<PPPlus> {
        return restClient.post()
            .uri { u -> u
                .scheme(API_SCHEME)
                .host(API_HOST)
                .port(API_PORT)
                .replacePath("/api/batch/calculation")
                .build()
            }
            .body(body)
            .retrieve()
            .body(object : ParameterizedTypeReference<List<PPPlus>>() {})!!
    }

    companion object {
        private const val API_SCHEME = "http"
        private const val API_HOST = "localhost"
        private const val API_PORT = "5000"

        private val log: Logger = LoggerFactory.getLogger(PerformancePlusAPIService::class.java)

        fun collect(stats: List<PPPlus.Stats?>): PPPlus.Stats {
            fun sortSum(selector: (PPPlus.Stats) -> Double): Double {
                return (stats)
                    .mapNotNull { plus -> plus?.let(selector) }
                    .sortedDescending()
                    .mapIndexed { index, value ->
                        value * FastPower095.pow(index)
                    }
                    .sum()
            }

            // 每一个维度都会触发一次独立的 map + sort
            return PPPlus.Stats(
                aim = sortSum { it.aim },
                jumpAim = sortSum { it.jumpAim },
                flowAim = sortSum { it.flowAim },
                precision = sortSum { it.precision },
                speed = sortSum { it.speed },
                stamina = sortSum { it.stamina },
                accuracy = sortSum { it.accuracy },
                total = sortSum { it.total }
            )
        }
    }
}