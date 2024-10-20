package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BinUser
import com.now.nowbot.model.enums.OsuMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.service.UserParam
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.UUBAService.BPHeadTailParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.serviceException.BPAnalysisException
import com.now.nowbot.throwable.serviceException.BindException
import com.now.nowbot.util.CmdUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.command.FLAG_MODE
import org.apache.logging.log4j.util.Strings
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.regex.Matcher

@Service("UU_BA")
class UUBAService(
        private val userApiService: OsuUserApiService,
        private val scoreApiService: OsuScoreApiService,
        private val beatmapApiService: OsuBeatmapApiService,
        private val bindDao: BindDao,
        private val imageService: ImageService,
) : MessageService<BPHeadTailParam>, TencentMessageService<BPHeadTailParam> {

    // bpht 的全称大概是 BP Head / Tail
    data class BPHeadTailParam(val user: UserParam, val info: Boolean)

    @Throws(BPAnalysisException::class)
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: DataValue<BPHeadTailParam>
    ): Boolean {
        // 旧功能指引
        val matcher2 = Instruction.DEPRECATED_BPHT.matcher(messageText)
        if (matcher2.find()) {
            throw BPAnalysisException(BPAnalysisException.Type.BA_Instruction_Deprecated)
        }

        val matcher3 = Instruction.DEPRECATED_UUBA_I.matcher(messageText)
        if (matcher3.find()) {
            throw BPAnalysisException(BPAnalysisException.Type.BA_I_Deprecated)
        }

        val matcher = Instruction.UU_BA.matcher(messageText)
        if (!matcher.find()) return false

        val info = true
        val mode: OsuMode = OsuMode.getMode(matcher.group(FLAG_MODE))

        if (event.isAt) {
            data.value = BPHeadTailParam(UserParam(event.target, null, mode, true), info)
            return true
        }
        val name = matcher.group("name")
        if (Objects.nonNull(name) && Strings.isNotBlank(name)) {
            data.value = BPHeadTailParam(UserParam(null, name, mode, false), info)
            return true
        }
        data.value = BPHeadTailParam(UserParam(event.sender.id, null, mode, false), info)
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: BPHeadTailParam) {
        val from = event.subject
        var bu: BinUser

        // 是否为绑定用户
        if (Objects.nonNull(param.user.qq)) {
            try {
                bu = bindDao.getUserFromQQ(param.user.qq)
            } catch (e: BindException) {
                if (!param.user.at) {
                    throw BPAnalysisException(BPAnalysisException.Type.BA_Me_TokenExpired)
                } else {
                    throw BPAnalysisException(BPAnalysisException.Type.BA_Player_TokenExpired)
                }
            }
        } else {
            // 查询其他人 [data]
            val name = param.user.name
            var id: Long = 0
            try {
                id = userApiService.getOsuId(name)
                bu = bindDao.getUserFromOsuid(id)
            } catch (e: BindException) {
                // 构建只有 data + id 的对象, binUser == null
                bu = BinUser()
                bu.osuID = id
                bu.osuName = name
            } catch (e: Exception) {
                throw BPAnalysisException(BPAnalysisException.Type.BA_Player_NotFound)
            }
        }

        val bps: List<LazerScore>?

        val mode = OsuMode.getMode(param.user.mode, bu.osuMode)

        try {
            bps = scoreApiService.getBestScores(bu, mode)
        } catch (e: HttpClientErrorException.BadRequest) {
            // 请求失败 超时/断网
            if (param.user.qq == event.sender.id) {
                throw BPAnalysisException(BPAnalysisException.Type.BA_Me_TokenExpired)
            } else {
                throw BPAnalysisException(BPAnalysisException.Type.BA_Player_TokenExpired)
            }
        } catch (e: WebClientResponseException.BadRequest) {
            if (param.user.qq == event.sender.id) {
                throw BPAnalysisException(BPAnalysisException.Type.BA_Me_TokenExpired)
            } else {
                throw BPAnalysisException(BPAnalysisException.Type.BA_Player_TokenExpired)
            }
        } catch (e: HttpClientErrorException.Unauthorized) {
            // 未绑定
            throw BPAnalysisException(BPAnalysisException.Type.BA_Me_TokenExpired)
        } catch (e: WebClientResponseException.Unauthorized) {
            throw BPAnalysisException(BPAnalysisException.Type.BA_Me_TokenExpired)
        }

        if (bps.size <= 10) {
            if (!param.user.at && Objects.isNull(param.user.name)) {
                throw BPAnalysisException(
                        BPAnalysisException.Type.BA_Me_NotEnoughBP, mode!!.getName())
            } else {
                throw BPAnalysisException(
                        BPAnalysisException.Type.BA_Player_NotEnoughBP, mode!!.getName())
            }
        }

        beatmapApiService.applySRAndPP(bps)
        val lines =
                if (OsuMode.isDefaultOrNull(mode)) {
                    getTextPlus(bps, bu.osuName, "")
                } else {
                    getTextPlus(bps, bu.osuName, mode.getName())
                }

        try {
            val panelParam =
                    lines.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val image = imageService.getPanelAlpha(*panelParam)
            from.sendImage(image)
        } catch (e: Exception) {
            throw BPAnalysisException(BPAnalysisException.Type.BA_Send_UUError)
        }
    }

    override fun accept(event: MessageEvent, messageText: String): BPHeadTailParam? {
        var matcher: Matcher
        when {
            OfficialInstruction.UU_BA.matcher(messageText).apply { matcher = this }.find() -> {}

            else -> return null
        }
        val mode = CmdUtil.getMode(matcher)
        val user = CmdUtil.getUserWithOutRange(event, matcher, mode)

        return BPHeadTailParam(UserParam(user.userID, user.username, mode.data, false), info = true)
    }

    override fun reply(event: MessageEvent, param: BPHeadTailParam): MessageChain? {
        val mode = param.user.mode
        val bu = BinUser()
        with(bu) {
            osuID = param.user.qq
            osuName = param.user.name
            osuMode = mode
        }
        val bps = scoreApiService.getBestScores(bu, mode, 0, 100)
        beatmapApiService.applySRAndPP(bps)
        val modeStr = mode?.getName() ?: ""
        val lines = getTextPlus(bps, bu.osuName, modeStr)
        return MessageChain(lines)
    }

    @Deprecated("这个标准获取基本上没人喜欢用了，只有进阶版有人用")
    fun getText(bps: List<LazerScore>, name: String?, mode: String?): String {
        val sb = StringBuffer().append(name).append(": ").append(' ').append(mode).append('\n')
        var allPP = 0.0
        var sSum = 0
        var xSum = 0
        var fcSum = 0
        val modTreeMap = TreeMap<String, AtomicInteger>() // 各个mod的数量

        for (i in bps.indices) {
            val bp = bps[i]
            // 显示前五跟后五的数据
            if (i < 5 || i >= bps.size - 5) {
                sb.append("#")
                        .append(i + 1)
                        .append(' ')
                        .append(String.format("%.2f", bp.PP))
                        .append(' ')
                        .append(String.format("%.2f", 100 * bp.accuracy))
                        .append('%')
                        .append(' ')
                        .append(bp.rank)
                if (bp.mods.isNotEmpty()) {
                    sb.append(" +")
                    for (m in bp.mods) {
                        sb.append(m).append(' ')
                    }
                }
                sb.append('\n')
            } else if (i == 5) {
                sb.append("...").append('\n')
            }
            allPP += bp.PP!!  // 统计总数
            if (bp.mods.isNotEmpty()) {
                for (j in bp.mods.indices) {
                    val mod = bp.mods[j].acronym
                    if (!modTreeMap.containsKey(mod)) modTreeMap[mod] = AtomicInteger()
                    else modTreeMap[mod]!!.incrementAndGet()
                }
            }
            if (bp.rank.contains("S")) sSum++
            if (bp.rank.contains("X")) {
                sSum++
                xSum++
            }
            if (bp.fullCombo) fcSum++
        }
        sb.append("——————————").append('\n')
        sb.append("模组数量: \n")

        val c = AtomicInteger()

        modTreeMap.forEach { (mod: String, sum) ->
            c.getAndIncrement()
            sb.append(mod).append(' ').append(sum.get()).append("x; ")
            if (c.get() == 2) {
                c.set(0)
                sb.append('\n')
            }
        }

        sb.append("\nS+ 评级: ").append(sSum)
        if (xSum != 0) sb.append("\n     其中 SS: ").append(xSum)

        sb.append('\n')
                .append("完美 FC: ")
                .append(fcSum)
                .append('\n')
                .append("平均: ")
                .append(String.format("%.2f", allPP / bps.size))
                .append("PP")
                .append('\n')
                .append("差值: ")
                .append(String.format("%.2f", bps.first().PP!! - bps.last().PP!!))
                .append("PP")

        return sb.toString()
    }

    fun getTextPlus(bps: List<LazerScore>, name: String?, mode: String?): String {
        if (bps.isEmpty()) return ""
        val sb = StringBuffer().append(name).append(": ").append(' ').append(mode).append('\n')

        val BP1: LazerScore = bps.first()
        val BP1BPM = BP1.beatMap.bpm
        val BP1Length = BP1.beatMap.totalLength.toFloat()

        var star: Float
        var maxStar = BP1.beatMap.starRating
        var minStar = maxStar
        var maxBPM = BP1BPM
        var minBPM = maxBPM
        var maxCombo = BP1.maxCombo
        var minCombo = maxCombo
        var maxLength = BP1Length
        var minLength = maxLength

        var maxComboBP = 0
        var minComboBP = 0
        var maxLengthBP = 0
        var minLengthBP = 0
        var maxStarBP = 0
        var minStarBP = 0

        var avgLength = 0.0
        var avgCombo = 0
        var avgStar = 0.0

        var maxTTHPPBP = 0
        var maxTTHPP = 0.0
        var nowPP = 0.0

        val modSum = TreeMap<String, ModData>() // 各个mod的数量

        val mapperSum = TreeMap<Long, FavoriteMapperData>()
        val decimalFormat = DecimalFormat("0.00") // acc格式

        for (i in bps.indices) {
            val bp = bps[i]
            val b = bp.beatMap
            val length = b.totalLength.toFloat()
            val bpm = b.bpm
            bp.mods.forEach(
                    Consumer { r: OsuMod ->
                        if (modSum.containsKey(r.acronym)) {
                            modSum[r.acronym]!!.add(bp.weight?.PP ?: 0.0)
                        } else {
                            modSum[r.acronym] = ModData(bp.weight?.PP ?: 0.0)
                        }
                    })

            avgLength += length
            star = bp.beatMap.starRating
            avgStar += star

            if (bpm < minBPM) {
                minBPM = bpm
            } else if (bpm >= maxBPM) {
                maxBPM = bpm
            }

            if (star < minStar) {
                minStarBP = i
                minStar = star
            } else if (star > maxStar) {
                maxStarBP = i
                maxStar = star
            }

            if (length < minLength) {
                minLengthBP = i
                minLength = length
            } else if (length > maxLength) {
                maxLengthBP = i
                maxLength = length
            }

            if (bp.maxCombo < minCombo) {
                minComboBP = i
                minCombo = bp.maxCombo
            } else if (bp.maxCombo > maxCombo) {
                maxComboBP = i
                maxCombo = bp.maxCombo
            }
            avgCombo += bp.maxCombo

            val tthToPp = (bp.PP!!) / (b.sliders + b.spinners + b.circles)
            if (maxTTHPP < tthToPp) {
                maxTTHPPBP = i
                maxTTHPP = tthToPp
            }

            if (mapperSum.containsKey(b.mapperID)) {
                mapperSum[b.mapperID]!!.add(bp.PP!!)
            } else {
                mapperSum[b.mapperID] = FavoriteMapperData(bp.PP!!, b.mapperID)
            }
            nowPP += bp.weight!!.PP
        }
        avgCombo /= bps.size
        avgLength /= bps.size.toFloat()
        avgStar /= bps.size.toFloat()

        sb.append("平均时间: ").append(getTimeStr(avgLength.toInt())).append('\n')
        sb.append("时间最长: BP")
                .append(maxLengthBP + 1)
                .append(' ')
                .append(getTimeStr(maxLength.toInt()))
                .append('\n')
        sb.append("时间最短: BP")
                .append(minLengthBP + 1)
                .append(' ')
                .append(getTimeStr(minLength.toInt()))
                .append('\n')
        sb.append("——————————").append('\n')

        sb.append("平均连击: ").append(avgCombo).append('x').append('\n')
        sb.append("连击最大: BP")
                .append(maxComboBP + 1)
                .append(' ')
                .append(maxCombo)
                .append('x')
                .append('\n')
        sb.append("连击最小: BP")
                .append(minComboBP + 1)
                .append(' ')
                .append(minCombo)
                .append('x')
                .append('\n')
        sb.append("——————————").append('\n')

        sb.append("平均星数: ").append(String.format("%.2f", avgStar)).append('*').append('\n')
        sb.append("星数最高: BP")
                .append(maxStarBP + 1)
                .append(' ')
                .append(String.format("%.2f", maxStar))
                .append('*')
                .append('\n')
        sb.append("星数最低: BP")
                .append(minStarBP + 1)
                .append(' ')
                .append(String.format("%.2f", minStar))
                .append('*')
                .append('\n')
        sb.append("——————————").append('\n')

        sb.append("PP/TTH 比例最大: BP")
                .append(maxTTHPPBP + 1)
                .append("，为")
                .append(decimalFormat.format(maxTTHPP))
                .append('倍')
                .append('\n')

        sb.append("BPM 区间: ")
                .append(String.format("%.0.0", minBPM))
                .append('-')
                .append(String.format("%.0.0", maxBPM))
                .append('\n')
        sb.append("——————————").append('\n')

        sb.append("谱师: \n")
        val mappers =
                mapperSum.values
                        .stream()
                        .sorted { o1: FavoriteMapperData, o2: FavoriteMapperData ->
                            if (o1.size != o2.size) return@sorted 2 * (o2.size - o1.size)
                            o2.allPP.compareTo(o1.allPP)
                        }
                        .limit(9)
                        .toList()
        val mappersId = mappers.stream().map { u: FavoriteMapperData -> u.uid }.toList()
        val mappersInfo = userApiService.getUsers(mappersId)
        val mapperIdToInfo = HashMap<Long, String>()
        for (node in mappersInfo) {
            mapperIdToInfo[node.userID] = node.userName
        }
        mappers.forEach(
                Consumer { mapper: FavoriteMapperData ->
                    try {
                        sb.append(mapperIdToInfo[mapper.uid])
                                .append(": ")
                                .append(mapper.size)
                                .append("x ")
                                .append(decimalFormat.format(mapper.allPP))
                                .append("PP")
                                .append('\n')
                    } catch (e: Exception) {
                        sb.append("UID: ")
                                .append(mapper.uid)
                                .append(": ")
                                .append(mapper.size)
                                .append("x ")
                                .append(decimalFormat.format(mapper.allPP))
                                .append("PP")
                                .append('\n')
                    }
                })
        sb.append("——————————").append('\n')
        sb.append("模组数量: \n")
        val finalAllPP = nowPP
        modSum.forEach { (mod: String, sum: ModData) ->
            sb.append(mod)
                    .append(": ")
                    .append(sum.size)
                    .append("x ")
                    .append(decimalFormat.format(sum.allPP))
                    .append("PP ")
                    .append('(')
                    .append(decimalFormat.format((100 * sum.allPP / finalAllPP)))
                    .append('%')
                    .append(')')
                    .append('\n')
        }
        return sb.toString()
    }

    internal class FavoriteMapperData(var allPP: Double, var uid: Long) {
        var size: Int = 1

        fun add(pp: Double) {
            allPP += pp
            size++
        }
    }

    internal class ModData(var allPP: Double) {
        var size: Int = 1

        fun add(pp: Double) {
            allPP += pp
            size++
        }
    }

    fun getTimeStr(l: Int): String {
        return if (l < 60) {
            "${l}秒"
        } else {
            "${l / 60}分${l % 60}秒"
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(UUBAService::class.java)
    }
}
