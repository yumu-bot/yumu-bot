package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.ValueMod
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.BPAnalysisService.BAParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithoutRange
import com.now.nowbot.util.DataUtil.getBonusPP
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

@Service("BP_ANALYSIS") class BPAnalysisService(
    private val scoreApiService: OsuScoreApiService,
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
    private val calculateApiService: OsuCalculateApiService,
    private val beatmapApiService: OsuBeatmapApiService
) : MessageService<BAParam>, TencentMessageService<BAParam> {

    data class BAParam(val user: OsuUser, val scores: List<LazerScore>, val isMyself: Boolean)

    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent, messageText: String, data: DataValue<BAParam>
    ): Boolean {
        val matcher = Instruction.BP_ANALYSIS.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        val isMyself = AtomicBoolean(false)
        val mode = getMode(matcher)
        val user = getUserWithoutRange(event, matcher, mode, isMyself)
        val bests = scoreApiService.getBestScores(user.userID, mode.data) + scoreApiService.getBestScores(
            user.userID,
            mode.data,
            100,
            100
        )

        data.value = BAParam(user, bests, isMyself.get())

        return true
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: BAParam) {
        val image = param.getImage(
            2,
            calculateApiService,
            userApiService,
            imageService,
            scoreApiService,
            beatmapApiService
        )

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("最好成绩分析：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "最好成绩分析")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): BAParam? {
        val matcher = OfficialInstruction.BP_ANALYSIS.matcher(messageText)

        if (!matcher.find()) {
            return null
        }

        val isMyself = AtomicBoolean(false)
        val mode = getMode(matcher)
        val user = getUserWithoutRange(event, matcher, mode, isMyself)
        val bpList = scoreApiService.getBestScores(user.userID, mode.data)

        return BAParam(user, bpList, isMyself.get())
    }

    override fun reply(event: MessageEvent, param: BAParam): MessageChain? = QQMsgUtil.getImage(
        param.getImage(
            2,
            calculateApiService,
            userApiService,
            imageService,
            scoreApiService,
            beatmapApiService
        )
    )

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BPAnalysisService::class.java)
        private val RANK_ARRAY = arrayOf("XH", "X", "SSH", "SS", "SH", "S", "A", "B", "C", "D", "F")

        @JvmStatic fun parseData(
            user: OsuUser, bpList: List<LazerScore>?, userApiService: OsuUserApiService, version: Int = 1
        ): Map<String, Any> {
            if (bpList == null || bpList.size <= 5) return HashMap.newHashMap(1)

            val bests: List<LazerScore> = ArrayList(bpList)

            val bpSize = bests.size

            // top
            val t6: List<LazerScore> = bests.take(6)
            val b5: List<LazerScore> = bests.takeLast(bpSize - max((bpSize - 5).toDouble(), 0.0).toInt())

            data class BeatMap4BA(
                val ranking: Int,
                val length: Int,
                val combo: Int,
                val bpm: Float,
                val star: Float,
                val rank: String,
                val cover: String,
                val mods: List<LazerMod>
            )

            data class Attr(
                val index: String,
                @JsonProperty("map_count") val mapCount: Int,
                @JsonProperty("pp_count") val ppCount: Double,
                val percent: Double
            )

            val beatMapList: MutableList<BeatMap4BA> = ArrayList(bpSize)
            val modsPPMap: MultiValueMap<String, Double> = LinkedMultiValueMap()
            val rankMap: MultiValueMap<String, Double> = LinkedMultiValueMap()

            var modsSum = 0

            bests.forEachIndexed { i, best ->
                val b = best.beatMap

                val m = best.mods.filter {
                    if (it is ValueMod) {
                        it.value != 0
                    } else {
                        true
                    }
                }

                run {
                    val ba = BeatMap4BA(
                        i + 1,
                        b.totalLength,
                        best.maxCombo,
                        (b.BPM ?: 0.0).toFloat(),
                        b.starRating.toFloat(),
                        best.rank,
                        best.beatMapSet.covers.list ?: "",
                        m
                    )
                    beatMapList.add(ba)
                }

                run { // 统计 mods / rank
                    if (m.isNotEmpty()) {
                        m.forEach {
                            modsPPMap.add(it.acronym, best.weight!!.PP)
                        }
                        modsSum += m.size
                    } else {
                        modsSum += 1
                    }

                    if (best.fullCombo) {
                        rankMap.add("FC", best.weight!!.PP)
                    }

                    rankMap.add(best.rank, best.weight!!.PP)
                }
            }

            // 0 length; 1 combo; 2 star; 3 bpm
            val summary: HashMap<String, List<BeatMap4BA>> = HashMap(4)

            fun <T : Comparable<T>> sortCount(name: String, sortedBy: (BeatMap4BA) -> T) {
                val sortList: List<BeatMap4BA> = beatMapList.sortedByDescending(sortedBy)
                val stat: ArrayList<BeatMap4BA> = ArrayList(3)
                stat.add(sortList.first())
                stat.add(sortList[bpSize / 2])
                stat.add(sortList[bpSize - 1])
                summary[name] = stat
            }

            sortCount("length") { it.length }
            sortCount("combo") { it.combo }
            sortCount("star") { it.star }
            sortCount("bpm") { it.bpm }

            val ppRawList = bests.map { it.PP!! }
            val ppSum = bests.sumOf { it.weight?.PP ?: 0.0 }
            val rankList = bests.map { it.rank }
            val lengthList = beatMapList.map { it.length }
            val starList = beatMapList.map { it.star }
            val modsList: List<List<String>> = beatMapList.map {
                it.mods.map { mod -> mod.acronym }
            }
            val timeList =
                bests.map { 1.0 * it.endedTime.plusHours(8).hour + (it.endedTime.plusHours(8).minute / 60.0) }
            val timeDist = MutableList(8) { _ -> 0 }

            for (time in timeList) {
                val position: Int = min(floor(time / 3.0).toInt(), 7)
                timeDist[position]++
            }

            val rankSort = rankList.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }

            data class Mapper(
                @JsonProperty("avatar_url") val avatarUrl: String,
                val username: String,
                @JsonProperty("map_count") val mapCount: Int,
                @JsonProperty("pp_count") val ppCount: Float
            )

            val mapperMap = bests.groupingBy { it.beatMap.mapperID }.eachCount()

            val mapperSize = mapperMap.size
            val mapperCount =
                mapperMap.entries.sortedByDescending { it.value }.take(8).associateBy({ it.key }, { it.value })
                    .toMap(LinkedHashMap())

            val mapperInfo = userApiService.getUsers(mapperCount.keys)
            val mapperList =
                bests.filter { mapperCount.containsKey(it.beatMap.mapperID) }.groupingBy { it.beatMap.mapperID }
                    .aggregate<LazerScore, Long, Double> { _, accumulator, element, _ ->
                        if (accumulator == null) {
                            element.PP ?: 0.0
                        } else {
                            accumulator + (element.PP ?: 0.0)
                        }
                    }.entries.sortedByDescending { it.value }.map {
                        var name = ""
                        var avatar = ""
                        for (node in mapperInfo) {
                            if (it.key == node.userID) {
                                name = node.userName
                                avatar = node.avatarUrl!!
                                break
                            }
                        }
                        Mapper(avatar, name, mapperCount[it.key] ?: 0, it.value.toFloat())
                    }.toList()

            val userPP = user.pp
            val bonusPP = getBonusPP(userPP, bests.map { it.PP ?: 0.0 }.toDoubleArray())

            //bpPP + remainPP (bp100之后的) = rawPP
            val bpPP = bests.sumOf { it.weight!!.PP }
            val rawPP = (userPP - bonusPP)

            val modsAttr: List<Attr>
            run {
                val modsAttrTmp: MutableList<Attr> = ArrayList(modsPPMap.size)
                modsPPMap.forEach { (mod: String, value: MutableList<Double?>) ->
                    val attr = Attr(
                        mod, value.filterNotNull().count(), value.filterNotNull().sum(), (value.size * 1.0 / modsSum)
                    )
                    modsAttrTmp.add(attr)
                }
                modsAttr = modsAttrTmp.sortedByDescending { it.ppCount }
            }

            val rankAttr: MutableList<Attr?> = ArrayList(rankMap.size)
            run {
                val fcList = rankMap.remove("FC")
                val fc: Attr
                if (fcList.isNullOrEmpty()) {
                    fc = Attr("FC", 0, 0.0, 0.0)
                } else {
                    val fcPPSum = fcList.sum()

                    fc = Attr("FC", fcList.size, fcPPSum, (fcPPSum / bpPP))
                }
                rankAttr.add(fc)
                for (rank in RANK_ARRAY) {
                    if (rankMap.containsKey(rank)) {
                        val value = rankMap[rank]
                        var rankPPSum: Double
                        var attr: Attr? = null
                        if (!value.isNullOrEmpty()) {
                            rankPPSum = value.sum()
                            attr = Attr(
                                rank, value.count(), rankPPSum, (rankPPSum / bpPP)
                            )
                        }
                        rankAttr.add(attr)
                    }
                }
            }

            val clientCount = listOf(bests.count { !it.isLazer }, bests.count { it.isLazer })

            val data: Map<String, Any> = when(version) {
                1 -> mapOf(
                    "card_A1" to user,
                    "bpTop5" to t6.dropLast(1),
                    "bpLast5" to b5,
                    "bpLength" to summary["length"]!!,
                    "bpCombo" to summary["combo"]!!,
                    "bpSR" to summary["star"]!!,
                    "bpBpm" to summary["bpm"]!!,
                    "favorite_mappers_count" to mapperSize,
                    "favorite_mappers" to mapperList,
                    "pp_raw_arr" to ppRawList,
                    "rank_arr" to rankList,
                    "rank_elect_arr" to rankSort,
                    "bp_length_arr" to lengthList,
                    "mods_attr" to modsAttr,
                    "rank_attr" to rankAttr,
                    "pp_raw" to rawPP,
                    "pp" to userPP,
                    "game_mode" to bests.first().mode,
                )

                else ->
                    mapOf(
                        "user" to user,
                        "bests" to t6,
                        "length_attr" to summary["length"]!!,
                        "combo_attr" to summary["combo"]!!,
                        "star_attr" to summary["star"]!!,
                        "bpm_attr" to summary["bpm"]!!,
                        "favorite_mappers" to mapperList,
                        "pp_raw_arr" to ppRawList,
                        "rank_arr" to rankList,
                        "length_arr" to lengthList,
                        "mods_arr" to modsList,
                        "star_arr" to starList,
                        "time_arr" to timeList,
                        "time_dist_arr" to timeDist,

                        "mods_attr" to modsAttr,
                        "rank_attr" to rankAttr,

                        "pp_sum" to ppSum,
                        "client_count" to clientCount,
                    )
            }

            return data
        }

        @JvmStatic fun BAParam.getImage(
            version: Int = 1,
            calculateApiService: OsuCalculateApiService,
            userApiService: OsuUserApiService,
            imageService: ImageService,
            scoreApiService: OsuScoreApiService,
            beatmapApiService: OsuBeatmapApiService
        ): ByteArray {
            val scores = scores
            val user = user

            if (scores.isEmpty() || scores.size <= 5) {
                if (isMyself) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_NotEnoughBP_Me, user.currentOsuMode)
                } else {
                    throw GeneralTipsException(GeneralTipsException.Type.G_NotEnoughBP_Player, user.currentOsuMode)
                }
            }

            // 提取星级变化的谱面 DT/HT 等
            calculateApiService.applyBeatMapChanges(scores)
            calculateApiService.applyStarToScores(scores)

            beatmapApiService.applyBeatMapExtend(scores.take(6))
            scoreApiService.asyncDownloadBackground(scores.take(6))

            val data = parseData(
                user, scores, userApiService, version
            )

            return try {
                when (version) {
                    1 -> imageService.getPanel(data, "J")
                    else -> imageService.getPanel(data, "J2")
                }
            } catch (e: Exception) {
                log.error("最好成绩分析：复杂面板生成失败", e)
                try {
                    val msg = UUBAService.getTextPlus(scores, user.username, user.mode, userApiService)
                        .split("\n".toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    imageService.getPanelAlpha(*msg)
                } catch (e1: Exception) {
                    log.error("最好成绩分析：文字版转换失败", e1)
                    throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "最好成绩分析（文字版）")
                }
            }
        }
    }
}
