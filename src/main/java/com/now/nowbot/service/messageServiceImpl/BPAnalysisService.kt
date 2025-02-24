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
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

@Service("BP_ANALYSIS") class BPAnalysisService(
    private val scoreApiService: OsuScoreApiService,
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
    private val uubaService: UUBAService,
    private val calculateApiService: OsuCalculateApiService
) : MessageService<BAParam>, TencentMessageService<BAParam> {

    data class BAParam(val user: OsuUser, val scores: List<LazerScore>, val isMyself: Boolean)

    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<BAParam>
    ): Boolean {
        val matcher = Instruction.BP_ANALYSIS.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        val isMyself = AtomicBoolean(false)
        val mode = getMode(matcher)
        val user = getUserWithoutRange(event, matcher, mode, isMyself)
        val bpList = scoreApiService.getBestScores(user.userID, mode.data)
        data.value = BAParam(user, bpList, isMyself.get())

        return true
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: BAParam) {
        val image = param.getImage(2, calculateApiService, userApiService, imageService, uubaService)

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
        param.getImage(2, calculateApiService, userApiService, imageService, uubaService)
    )

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BPAnalysisService::class.java)
        private val RANK_ARRAY = arrayOf("XH", "X", "SSH", "SS", "SH", "S", "A", "B", "C", "D", "F")

        @JvmStatic fun parseData(
            user: OsuUser,
            bpList: List<LazerScore>?,
            userApiService: OsuUserApiService,
            version: Int = 1
        ): Map<String, Any> {
            if (bpList == null || bpList.size <= 5) return HashMap.newHashMap(1)

            val bps: List<LazerScore> = ArrayList(bpList)

            val bpSize = bps.size

            // top
            val t6: List<LazerScore> = bps.take(6)
            val b5: List<LazerScore> = bps.takeLast(bpSize - max((bpSize - 5).toDouble(), 0.0).toInt())

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
                @JsonProperty("map_count")
                val mapCount: Int,
                @JsonProperty("pp_count")
                val ppCount: Double,
                val percent: Double
            )

            val beatMapList: MutableList<BeatMap4BA> = ArrayList(bpSize)
            val modsPPMap: MultiValueMap<String, Double> = LinkedMultiValueMap()
            val rankMap: MultiValueMap<String, Double> = LinkedMultiValueMap()

            var modsSum = 0

            for (i in 0 until bpSize) {
                val s = bps[i]
                val b = s.beatMap

                val m = s.mods.filter {
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
                        s.maxCombo,
                        (b.BPM ?: 0.0).toFloat(),
                        b.starRating.toFloat(),
                        s.rank,
                        s.beatMapSet.covers.list,
                        m
                    )
                    beatMapList.add(ba)
                }

                run { // 统计 mods / rank
                    if (m.isNotEmpty()) {
                        m.forEach {
                            modsPPMap.add(it.acronym, s.weight!!.PP)
                        }
                        modsSum += m.size
                    } else {
                        modsSum += 1
                    }

                    if (s.fullCombo) {
                        rankMap.add("FC", s.weight!!.PP)
                    }

                    rankMap.add(s.rank, s.weight!!.PP)
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

            val ppRawList = bps.map { it.PP!! }
            val ppSum = bps.map(LazerScore::getWeightPP).sum()
            val rankList = bps.map { it.rank }
            val lengthList = beatMapList.map { it.length }
            val starList = beatMapList.map { it.star }
            val modsList: List<List<String>> = beatMapList.map {
                it.mods.map { mod -> mod.acronym }
            }
            val timeList = bps.map { 1.0 * it.endedTime.plusHours(8).hour + (it.endedTime.plusHours(8).minute / 60.0) }
            val timeDist = mutableListOf(0, 0, 0, 0, 0, 0, 0, 0)

            for (time in timeList) {
                val position: Int = min(floor(time / 3.0).toInt(), 7)
                timeDist[position]++
            }

            val rankSort = rankList.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }

            data class Mapper(
                @JsonProperty("avatar_url")
                val avatarUrl: String,
                val username: String,
                @JsonProperty("map_count")
                val mapCount: Int,
                @JsonProperty("pp_count")
                val ppCount: Float
            )

            val mapperMap = bps
                .groupingBy { it.beatMap.mapperID }
                .eachCount()

            val mapperSize = mapperMap.size
            val mapperCount =
                mapperMap.entries.sortedByDescending { it.value }.take(8)
                    .associateBy({ it.key }, { it.value })
                    .toMap(LinkedHashMap())

            val mapperInfo = userApiService.getUsers(mapperCount.keys)
            val mapperList =
                bps.filter { mapperCount.containsKey(it.beatMap.mapperID) }
                    .groupingBy { it.beatMap.mapperID }
                    .aggregate<LazerScore, Long, Double>({ _, accumulator, element, _ ->
                        if (accumulator == null) {
                            element.PP ?: 0.0
                        } else {
                            accumulator + (element.PP ?: 0.0)
                        }
                    }).entries
                    .sortedWith(compareByDescending<Map.Entry<Long, Double>> { mapperCount[it.key] }
                        .thenByDescending { it.value })
                    .map {
                        var name = ""
                        var avatar = ""
                        for (node in mapperInfo) {
                            if (it.key == node.userID) {
                                name = node.userName
                                avatar = node.avatarUrl
                                break
                            }
                        }
                        Mapper(avatar, name, mapperCount[it.key] ?: 0, it.value.toFloat())
                    }.toList()

            val bpPPs = bps.map { obj: LazerScore -> obj.PP ?: 0.0 }.toDoubleArray()

            val userPP = user.pp
            val bonusPP = getBonusPP(userPP, bpPPs)

            //bpPP + remainPP (bp100之后的) = rawPP
            val bpPP = bps.sumOf { it.weight!!.PP }
            val rawPP = (userPP - bonusPP)

            val modsAttr: List<Attr>
            run {
                val m = modsSum
                val modsAttrTmp: MutableList<Attr> = ArrayList(modsPPMap.size)
                modsPPMap.forEach { (mod: String, value: MutableList<Double?>) ->
                    val attr = Attr(
                        mod, value.count{ it != null }, value.filterNotNull().sum(), (value.size * 1.0 / m)
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

                    fc = Attr("FC", fcList.size, fcPPSum, (fcPPSum / bpPP) * (bpSize / 100.0))
                }
                rankAttr.add(fc)
                for (rank in RANK_ARRAY) {
                    if (rankMap.containsKey(rank)) {
                        val value = rankMap[rank]
                        var rankPPSum: Double
                        var attr: Attr? = null
                        if (! value.isNullOrEmpty()) {
                            rankPPSum = value.sum()
                            attr = Attr(
                                rank, value.count(), rankPPSum, (rankPPSum / bpPP) * (bpSize / 100.0)
                            )
                        }
                        rankAttr.add(attr)
                    }
                }
            }

            val clientCount =
                listOf(
                    bps.stream().filter { !it.isLazer }.count(), bps.stream().filter { it.isLazer }.count()
                )

            val data = HashMap<String, Any>(18)

            if (version == 1) {
                data["card_A1"] = user
                data["bpTop5"] = t6.slice(0..4)
                data["bpLast5"] = b5
                data["bpLength"] = summary["length"]!!
                data["bpCombo"] = summary["combo"]!!
                data["bpSR"] = summary["star"]!!
                data["bpBpm"] = summary["bpm"]!!
                data["favorite_mappers_count"] = mapperSize
                data["favorite_mappers"] = mapperList
                data["pp_raw_arr"] = ppRawList
                data["rank_arr"] = rankList
                data["rank_elect_arr"] = rankSort
                data["bp_length_arr"] = lengthList
                data["mods_attr"] = modsAttr
                data["rank_attr"] = rankAttr
                data["pp_raw"] = rawPP
                data["pp"] = userPP
                data["game_mode"] = bps.first().mode
            } else {
                data["user"] = user
                data["bests"] = t6
                data["length_attr"] = summary["length"]!!
                data["combo_attr"] = summary["combo"]!!
                data["star_attr"] = summary["star"]!!
                data["bpm_attr"] = summary["bpm"]!!
                data["favorite_mappers"] = mapperList
                data["pp_raw_arr"] = ppRawList
                data["rank_arr"] = rankList
                data["length_arr"] = lengthList
                data["mods_arr"] = modsList
                data["star_arr"] = starList
                data["time_arr"] = timeList
                data["time_dist_arr"] = timeDist

                data["mods_attr"] = modsAttr
                data["rank_attr"] = rankAttr

                data["pp_sum"] = ppSum
                data["client_count"] = clientCount
            }

            return data
        }

        @JvmStatic fun BAParam.getImage(
            version: Int = 1,
            calculateApiService: OsuCalculateApiService,
            userApiService: OsuUserApiService,
            imageService: ImageService,
            uubaService: UUBAService
        ): ByteArray {
            val scores = scores
            val user = user

            if (scores.isEmpty() || scores.size <= 5) {
                if (isMyself) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_NotEnoughBP_Me, user.mode)
                } else {
                    throw GeneralTipsException(GeneralTipsException.Type.G_NotEnoughBP_Player, user.mode)
                }
            }

            // 提取星级变化的谱面 DT/HT 等
            calculateApiService.applyBeatMapChanges(scores)
            calculateApiService.applyStarToScores(scores)

            val data = parseData(
                user, scores, userApiService, version
            )

            return try {
                when (version) {
                    1 -> imageService.getPanel(data, "J")
                    else -> imageService.getPanel(data, "J2")
                }
            } catch (e: WebClientResponseException) {
                log.error("最好成绩分析：复杂面板生成失败", e)
                try {
                    val msg = uubaService.getTextPlus(scores, user.username, user.mode).split("\n".toRegex())
                        .dropLastWhile { it.isEmpty() }.toTypedArray()
                    imageService.getPanelAlpha(*msg)
                } catch (e1: ResourceAccessException) {
                    log.error("最好成绩分析：渲染失败", e1)
                    throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "最好成绩分析")
                } catch (e1: WebClientResponseException) {
                    log.error("最好成绩分析：渲染失败", e1)
                    throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "最好成绩分析")
                } catch (e1: Exception) {
                    log.error("最好成绩分析：UUBA 转换失败", e1)
                    throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "最好成绩分析（文字版）")
                }
            } catch (e: Exception) {
                log.error("最好成绩分析：渲染失败", e)
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "最好成绩分析")
            }
        }
    }
}
