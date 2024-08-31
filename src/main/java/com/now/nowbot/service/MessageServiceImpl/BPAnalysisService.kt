package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.model.JsonData.OsuUser
import com.now.nowbot.model.JsonData.Score
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.MessageServiceImpl.BPAnalysisService.BAParam
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.OsuApiService.OsuScoreApiService
import com.now.nowbot.service.OsuApiService.OsuUserApiService
import com.now.nowbot.throwable.ServiceException.BPAnalysisException
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithOutRange
import com.now.nowbot.util.DataUtil.getBonusPP
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.*
import kotlin.collections.LinkedHashMap
import kotlin.math.max

@Service("BP_ANALYSIS")
class BPAnalysisService(
    private val scoreApiService: OsuScoreApiService,
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
    private val uubaService: UUBAService,
    private val beatmapApiService: OsuBeatmapApiService,
) : MessageService<BAParam>, TencentMessageService<BAParam> {

    data class BAParam(val user: OsuUser, val bpList: List<Score>, val isMyself: Boolean)


    @Throws(Throwable::class)
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<BAParam>): Boolean {
        val matcher = Instruction.BP_ANALYSIS.matcher(messageText)

        if (!matcher.find()) return false

        val isMyself = AtomicBoolean()

        val mode = getMode(matcher)

        val user = getUserWithOutRange(event, matcher, mode, isMyself)?: return false

        val bpList = scoreApiService.getBestPerformance(user.userID, mode.data, 0, 100)

        data.value = BAParam(user, bpList, isMyself.get())

        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: BAParam) {
        val from = event.subject

        val image = param.getImage()

        try {
            from.sendImage(image)
        } catch (e: Exception) {
            log.error("最好成绩分析：发送失败", e)
            throw BPAnalysisException(BPAnalysisException.Type.BA_Send_Error)
        }
    }

    override fun accept(event: MessageEvent, messageText: String): BAParam? {
        val matcher = OfficialInstruction.BP_ANALYSIS.matcher(messageText)

        if (!matcher.find()) return null

        val isMyself = AtomicBoolean()

        val mode = getMode(matcher)

        val user = getUserWithOutRange(event, matcher, mode, isMyself)?: return null

        val bpList = scoreApiService.getBestPerformance(user.userID, mode.data, 0, 100)

       return BAParam(user, bpList, isMyself.get())
    }

    override fun reply(event: MessageEvent, param: BAParam) = QQMsgUtil.getImage(param.getImage())

    private fun BAParam.getImage(): ByteArray {
        val bpList = bpList
        val user = user

        if (CollectionUtils.isEmpty(bpList) || bpList.size <= 5) {
            if (isMyself) {
                throw BPAnalysisException(BPAnalysisException.Type.BA_Me_NotEnoughBP, user.mode)
            } else {
                throw BPAnalysisException(BPAnalysisException.Type.BA_Player_NotEnoughBP, user.mode)
            }
        }

        // 提取星级变化的谱面 DT/HT 等
        beatmapApiService.applySRAndPP(bpList)

        val data = parseData(
            user, bpList,
            userApiService
        )

        return try {
            imageService.getPanelJ(data)
        } catch (e: HttpServerErrorException.InternalServerError) {
            log.error("最好成绩分析：复杂面板生成失败", e)
            try {
                val msg = uubaService.getAllMsg(bpList, user.username, user.mode)
                imageService.getPanelAlpha(*msg)
            } catch (e1: ResourceAccessException) {
                log.error("最好成绩分析：渲染失败", e1)
                throw BPAnalysisException(BPAnalysisException.Type.BA_Render_Error)
            } catch (e1: HttpServerErrorException.InternalServerError) {
                log.error("最好成绩分析：渲染失败", e1)
                throw BPAnalysisException(BPAnalysisException.Type.BA_Render_Error)
            } catch (e1: Exception) {
                log.error("最好成绩分析：UUBA 转换失败", e1)
                throw BPAnalysisException(BPAnalysisException.Type.BA_Send_UUError)
            }
        } catch (e: Exception) {
            log.error("最好成绩分析：渲染失败", e)
            throw BPAnalysisException(BPAnalysisException.Type.BA_Render_Error)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BPAnalysisService::class.java)
        private val RANK_ARRAY = arrayOf("XH", "X", "SSH", "SS", "SH", "S", "A", "B", "C", "D", "F")
        fun parseData(user: OsuUser, bpList: List<Score>?, userApiService: OsuUserApiService): Map<String, Any> {
            if (bpList == null || bpList.size <= 5) return HashMap.newHashMap(1)

            val bps: List<Score> = ArrayList(bpList)

            val bpSize = bps.size

            // top
            val t5: List<Score?> = bps.subList(0, 5)
            val b5: List<Score?> = bps.subList(max((bpSize - 5).toDouble(), 0.0).toInt(), bpSize)

            data class BeatMap4BA(
                val ranking: Int,
                val length: Int,
                val combo: Int,
                val bpm: Float,
                val star: Float,
                val rank: String,
                val cover: String,
                val mods: Array<String>
            )

            data class Attr(
                val index: String,
                val map_count: Int,
                val pp_count: Float,
                val percent: Float
            )

            val beatMapList: MutableList<BeatMap4BA> = ArrayList(bpSize)
            val modsPPMap: MultiValueMap<String, Float> = LinkedMultiValueMap()
            val rankMap: MultiValueMap<String, Float> = LinkedMultiValueMap()

            var modsSum = 0

            for (i in 0 until bpSize) {
                val s = bps[i]
                val b = s.beatMap

                run {
                    val m = BeatMap4BA(
                        i + 1,
                        b.totalLength,
                        s.maxCombo,
                        b.bpm,
                        b.starRating,
                        s.rank,
                        s.beatMapSet.covers.list,
                        s.mods.toTypedArray<String>()
                    )
                    beatMapList.add(m)
                }

                run {
                    // 统计 mods / rank
                    if (!CollectionUtils.isEmpty(s.mods)) {
                        s.mods.forEach {
                            modsPPMap.add(it, s.weightedPP)
                        }
                        modsSum += s.mods.size
                    } else {
                        modsSum += 1
                    }
                    if (s.isPerfect) {
                        rankMap.add("FC", s.weightedPP)
                    }
                    rankMap.add(s.rank, s.weightedPP)
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

            val ppRawList = bps.map { it.pp!! }
            val rankCount = bps.map { it.rank!! }


            val rankSort = rankCount
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .map { it.key }

            data class Mapper(val avatar_url: String, val username: String, val map_count: Int, val pp_count: Float)

            val mapperMap = bps
                .groupingBy { it.beatMap.mapperID }
                .eachCount()

            val mapperSize = mapperMap.size
            val mapperCount = mapperMap
                .entries
                .sortedByDescending { it.value }
                .take(8)
                .associateBy({ it.key }, { it.value })
                .toMap(LinkedHashMap())

            val mapperInfo = userApiService.getUsers(mapperCount.keys)
            val mapperList = bps
                .filter { mapperCount.containsKey(it.beatMap.mapperID) }
                .groupingBy { it.beatMap.mapperID }
                .aggregate<Score, Long, Double>({ _, accumulator, element, _ ->
                    if (accumulator == null) {
                        element.pp.toDouble()
                    } else {
                        accumulator + element.pp.toDouble()
                    }
                })
                .entries.sortedWith(
                    compareByDescending<Map.Entry<Long, Double>> { mapperCount[it.key] }
                        .thenByDescending { it.value }
                )
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
                }
                .toList()

            val bpPPs = bps.map { obj: Score -> obj.pp.toDouble() }.toDoubleArray()

            val userPP = user.pp
            val bonusPP = getBonusPP(userPP, bpPPs)

            //bpPP + remainPP (bp100之后的) = rawPP
            val bpPP = bps.map { it.weightedPP?.toDouble() ?: 0.0 }.sum().toFloat()
            val rawPP = (userPP - bonusPP).toFloat()

            var modsAttr: List<Attr>
            run {
                val m = modsSum
                val modsAttrTmp: MutableList<Attr> = ArrayList(modsPPMap.size)
                modsPPMap.forEach { (mod: String, value: MutableList<Float?>) ->
                    val attr = Attr(
                        mod,
                        value.count { it != null },
                        value.filterNotNull().sum(),
                        (value.size / m.toFloat())
                    )
                    modsAttrTmp.add(attr)
                }
                modsAttr = modsAttrTmp.sortedByDescending { it.pp_count }
            }

            val rankAttr: MutableList<Attr?> = ArrayList(rankMap.size)
            run {
                val fcList = rankMap?.remove("FC")
                val fc: Attr
                if (fcList.isNullOrEmpty()) {
                    fc = Attr("FC", 0, 0f, 0f)
                } else {
                    val ppSum = fcList.reduceOrNull { acc, fl -> acc + fl } ?: 0f

                    fc = Attr("FC", fcList.size, ppSum, (ppSum / bpPP))
                }
                rankAttr.add(fc)
                for (rank in RANK_ARRAY) {
                    if (rankMap.containsKey(rank)) {
                        val value = rankMap[rank]
                        var ppSum: Float
                        var attr: Attr? = null
                        if (Objects.nonNull(value) && value!!.isNotEmpty()) {
                            ppSum = value.filterNotNull().reduceOrNull{ acc, fl -> acc + fl } ?: 0f
                            attr = Attr(
                                rank,
                                value.count { it != null },
                                ppSum, (ppSum / bpPP)
                            )
                        }
                        rankAttr.add(attr)
                    }
                }
            }

            val data = HashMap<String, Any>(18)

            data["card_A1"] = user
            data["bpTop5"] = t5
            data["bpLast5"] = b5
            data["bpLength"] = summary["length"]!!
            data["bpCombo"] = summary["combo"]!!
            data["bpSR"] = summary["star"]!!
            data["bpBpm"] = summary["bpm"]!!
            data["favorite_mappers_count"] = mapperSize
            data["favorite_mappers"] = mapperList
            data["pp_raw_arr"] = ppRawList
            data["rank_arr"] = rankCount
            data["rank_elect_arr"] = rankSort
            data["bp_length_arr"] = beatMapList.map { it.length }
            data["mods_attr"] = modsAttr
            data["rank_attr"] = rankAttr
            data["pp_raw"] = rawPP
            data["pp"] = userPP
            data["game_mode"] = bps.first().mode

            return data
        }
    }
}
