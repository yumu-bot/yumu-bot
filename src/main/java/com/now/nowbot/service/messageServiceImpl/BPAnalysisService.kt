package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.*
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.BPAnalysisService.BAParam
import com.now.nowbot.service.messageServiceImpl.BPAnalysisService.Companion.BeatmapAnalysis.Companion.toBeatmapAnalysis
import com.now.nowbot.service.messageServiceImpl.UUBAService.Companion.getText
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsRuntimeException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.*
import com.now.nowbot.util.InstructionUtil
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("BP_ANALYSIS") class BPAnalysisService(
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
    private val calculateApiService: OsuCalculateApiService,
    private val bindDao: BindDao
) : MessageService<BAParam>, TencentMessageService<BAParam> {

    data class BAParam(val user: OsuUser, val bests: List<LazerScore>, val isMyself: Boolean, val mappers: List<MicroUser>, val version: Int) {
        fun toMap(version: Int = this.version): Map<String, Any> {
            if (bests.size <= 5) {
                throw NoSuchElementException.PlayerBestScore(user.username, user.currentOsuMode)
            }

            val rankMap = mutableMapOf<String, MutableList<Double>>()
            val modMap = mutableMapOf<String, MutableList<Double>>()
            val fullMapperMap = mutableMapOf<NanoUser, MutableList<Double>>()

            var modCount = 0

            bests.forEachIndexed { i, s ->
                s.ranking = i + 1

                val b = s.beatmap
                val m = s.mods
                val weighted = s.getWeightedPP(i)

                if (m.isNotEmpty()) {
                    m.forEach {
                        modMap.getOrPut(it.acronym) { mutableListOf() }.add(weighted)
                    }
                    modCount += m.size
                } else {
                    modCount ++
                }


                if (s.fullCombo) {
                    rankMap.getOrPut("FC") { mutableListOf() }.add(weighted)
                }

                rankMap.getOrPut(s.rank) { mutableListOf() }.add(weighted)

                b.mappers.forEach { m ->
                    fullMapperMap.getOrPut(m) { mutableListOf() }.add(s.pp)
                }
            }

            val mapperMap: Map<NanoUser, List<Double>> = fullMapperMap.entries
                .groupBy { it.key.userID }
                .mapValues { (_, sameIDs) ->
                    val bestEntry = sameIDs.maxByOrNull { it.value.size }!!
                    val resolvedUser = bestEntry.key
                    val mergedList = sameIDs.flatMap { it.value }

                    resolvedUser to mergedList
                }
                .values
                .toMap()

            val summary = mutableMapOf<String, List<BeatmapAnalysis>>()

            fun <T : Comparable<T>> sortCount(name: String, sortedBy: (LazerScore) -> T) {
                val ss = bests.sortedByDescending(sortedBy)

                // 3. 使用 listOf() 结合 Kotlin 集合的快捷扩展函数
                summary[name] = listOf(
                    ss.first().toBeatmapAnalysis(),
                    ss[ss.size / 2].toBeatmapAnalysis(),
                    ss.last().toBeatmapAnalysis()
                )
            }

            sortCount("length") { it.beatmap.totalLength }
            sortCount("combo") { it.maxCombo }
            sortCount("star") { it.beatmap.starRating }
            sortCount("bpm") { it.beatmap.bpm }

            val ppRaw = bests.map { it.pp }
            val ppSum = bests.mapIndexed { index, score ->  score.getWeightedPP(index) }.sum()
            val ranks = bests.map { it.rank }
            val length = bests.map { it.beatmap.totalLength }
            val star = bests.map { it.beatmap.starRating }
            val mods = bests.map { it.mods.map { mod -> mod.acronym } }

            val zoneOffset = ZoneOffset.ofHours(8)

            val times: List<Double> = bests.map {
                it.endedTime.withOffsetSameInstant(zoneOffset).run {
                    hour + (minute / 60.0)
                }
            }

            val dist: MutableList<Int> = MutableList(8) { 0 }.apply {
                times.forEach { time ->
                    // 使用 coerceAtMost(7) 替代 min(..., 7)，语义更自然
                    val position = (time / 3.0).toInt().coerceAtMost(7)
                    this[position]++
                }
            }


            val rankSort = ranks.groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .map { it.key }


            val mappers =  mapperMap.map { (ms, ss) ->
                Mapper(
                    avatarUrl = "https://a.ppy.sh/${ms.userID}",
                    username = ms.username,
                    mapCount = ss.size,
                    ppCount = ss.sum(),
                )
            }.sortedByDescending { it.ppCount }

            val userPP = user.pp
            val bestPP = DataUtil.getBestsPP(bests)

            val modsAttribute: List<Attribute> = modMap.map { (mod, value) ->
                Attribute(
                    index = mod,
                    mapCount = value.size,
                    ppCount = value.sum(),
                    percent = value.size * 1.0 / modCount.coerceAtLeast(1)
                )
            }.sortedByDescending { it.ppCount }

            val rankAttribute: List<Attribute?> = buildList {
                // 1. 处理 "FC"
                val fcList = rankMap.remove("FC")
                if (fcList.isNullOrEmpty()) {
                    add(Attribute("FC", 0, 0.0, 0.0))
                } else {
                    val fcPPSum = fcList.sum()
                    add(Attribute("FC", fcList.size, fcPPSum, fcPPSum / bestPP))
                }

                for (rank in RANK_ARRAY) {
                    val value = rankMap[rank] ?: continue

                    if (value.isEmpty()) {
                        add(null) // 如果列表为空，添加 null
                    } else {
                        val rankPPSum = value.sum()
                        add(Attribute(rank, value.size, rankPPSum, rankPPSum / bestPP))
                    }
                }
            }

            val clientCount = bests.fold(intArrayOf(0, 0)) { counts, item ->
                if (item.isLazer) counts[1]++ else counts[0]++
                counts
            }

            val map: Map<String, Any> = when(version) {
                1 -> mapOf(
                    "card_A1" to user,
                    "bpTop5" to bests.take(5),
                    "bpLast5" to bests.drop(5).takeLast(5),
                    "bpLength" to summary["length"]!!,
                    "bpCombo" to summary["combo"]!!,
                    "bpSR" to summary["star"]!!,
                    "bpBpm" to summary["bpm"]!!,
                    "favorite_mappers_count" to mappers.size,
                    "favorite_mappers" to mappers,
                    "pp_raw_arr" to ppRaw,
                    "rank_arr" to ranks,
                    "rank_elect_arr" to rankSort,
                    "bp_length_arr" to length,
                    "mods_attr" to modsAttribute,
                    "rank_attr" to rankAttribute,
                    "pp_raw" to bestPP,
                    "pp" to userPP,
                    "game_mode" to user.currentOsuMode,
                )

                else ->
                    mapOf(
                        "user" to user,
                        "bests" to bests.take(6),
                        "length_attr" to summary["length"]!!,
                        "combo_attr" to summary["combo"]!!,
                        "star_attr" to summary["star"]!!,
                        "bpm_attr" to summary["bpm"]!!,
                        "favorite_mappers" to mappers,
                        "pp_raw_arr" to ppRaw,
                        "rank_arr" to ranks,
                        "length_arr" to length,
                        "mods_arr" to mods,
                        "star_arr" to star,
                        "time_arr" to times,
                        "time_dist_arr" to dist,

                        "mods_attr" to modsAttribute,
                        "rank_attr" to rankAttribute,

                        "pp_sum" to ppSum,
                        "client_count" to clientCount,
                    )
            }

            return map
        }
    }

    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent, messageText: String, data: DataValue<BAParam>
    ): Boolean {
        val matcher = Instruction.BP_ANALYSIS.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        data.value = getParam(event, matcher)

        return true
    }

    override fun handleMessage(event: MessageEvent, param: BAParam): ServiceCallStatistic? {
        val message = param.getMessageChain()

        try {
            event.replyAsync(message)
        } catch (e: Exception) {
            log.error("最好成绩分析：发送失败", e)
            throw IllegalStateException.Send("最好成绩分析")
        }

        return ServiceCallStatistic.build(event, userID = param.user.userID, mode = param.user.currentOsuMode)
    }

    override fun accept(event: MessageEvent, messageText: String): BAParam? {
        val matcher = OfficialInstruction.BP_ANALYSIS.matcher(messageText)

        if (!matcher.find()) {
            return null
        }

        return getParam(event, matcher)
    }

    override fun reply(event: MessageEvent, param: BAParam): MessageChain? = param.getMessageChain()

    private fun getParam(event: MessageEvent, matcher: Matcher): BAParam {
        val isMyself = AtomicBoolean(false)
        val mode = InstructionUtil.getMode(matcher)
        val id = UserIDUtil.getUserIDWithoutRange(event, matcher, mode, isMyself)

        // 1. 使用 if 表达式并结合解构声明，集中处理数据的获取逻辑
        val (user, bests) = if (id != null) {
            val m = OsuMode.getMode(mode.data,
                bindDao.getBindModeFromID(id),
                bindDao.getGroupModeConfig(event)
            )

            if (m.isNotDefault()) {
                AsyncMethodExecutor.awaitPair(
                    { userApiService.getOsuUser(id, m) },
                    { scoreApiService.getBestScores(id, m) }
                )
            } else {
                val fetchedUser = userApiService.getOsuUser(id)
                val fetchedBests = scoreApiService.getBestScores(id, fetchedUser.currentOsuMode)
                fetchedUser to fetchedBests
            }
        } else {
            val fetchedUser = InstructionUtil.getUserWithoutRange(event, matcher, mode, isMyself)
            val fetchedBests = scoreApiService.getBestScores(fetchedUser.userID, fetchedUser.currentOsuMode)
            fetchedUser to fetchedBests
        }

        AsyncMethodExecutor.awaitTriple(
            { BeatmapUtil.applyBeatmapChanges(bests) },
            { calculateApiService.applyStarToScores(bests) },
            { beatmapApiService.applyBeatmapExtend(bests) }
        )

        return BAParam(user, bests, isMyself.get(), emptyList(), 2)
    }

    private fun BAParam.getMessageChain(): MessageChain {
        return try {
            val name = when(version) {
                1 -> "J"
                else -> "J2"
            }

            MessageChain(imageService.getPanel(this.toMap(), name))
        } catch (e0: TipsRuntimeException) {
            throw e0
        } catch (e: Exception) {
            log.error("最好成绩分析：复杂面板生成失败", e)
            try {
                val msg = this.getText()
                    .split("\n".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                MessageChain(imageService.getPanelAlpha(*msg))
            } catch (e01: TipsRuntimeException) {
                throw e01
            } catch (e1: Exception) {
                log.error("最好成绩分析：文字版转换失败", e1)
                MessageChain(e1)
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BPAnalysisService::class.java)
        private val RANK_ARRAY = arrayOf("XH", "X", "SSH", "SS", "SH", "S", "A", "B", "C", "D", "F")

        data class Mapper(
            @field:JsonProperty("avatar_url") val avatarUrl: String,
            @field:JsonProperty("username") val username: String,
            @field:JsonProperty("map_count") val mapCount: Int,
            @field:JsonProperty("pp_count") val ppCount: Double
        )

        data class BeatmapAnalysis(
            val ranking: Int,
            val length: Int,
            val combo: Int,
            val bpm: Float,
            val star: Double,
            val rank: String,
            val cover: String,
            val mods: List<LazerMod>,
        ) {
            companion object {
                fun LazerScore.toBeatmapAnalysis(): BeatmapAnalysis {
                    val b = this.beatmap

                    return BeatmapAnalysis(
                        this.ranking ?: 1,
                        b.totalLength,
                        this.maxCombo,
                        b.bpm,
                        b.starRating,
                        this.rank,
                        this.beatmapset.covers.list,
                        this.mods
                    )
                }
            }
        }

        data class Attribute(
            val index: String,
            @param:JsonProperty("map_count") val mapCount: Int,
            @param:JsonProperty("pp_count") val ppCount: Double,
            val percent: Double
        )
    }
}
