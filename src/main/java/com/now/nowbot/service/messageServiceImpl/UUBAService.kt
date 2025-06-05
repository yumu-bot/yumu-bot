package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.UUBAService.BPHeadTailParam
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.util.CmdUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.command.FLAG_MODE
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Matcher
import kotlin.math.max

@Service("UU_BA")
class UUBAService(
    private val userApiService: OsuUserApiService,
    private val scoreApiService: OsuScoreApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val bindDao: BindDao,
    private val imageService: ImageService,
) : MessageService<BPHeadTailParam>, TencentMessageService<BPHeadTailParam> {

    data class UserParam(val qq: Long?, val name: String?, val mode: OsuMode, val at: Boolean )

    // bpht 的全称大概是 BP Head / Tail
    data class BPHeadTailParam(val user: UserParam, val info: Boolean)

    @Throws(Throwable::class)
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: DataValue<BPHeadTailParam>
    ): Boolean {
        // 旧功能指引

        /*
        val matcher2 = Instruction.DEPRECATED_BPHT.matcher(messageText)
        if (matcher2.find()) {
            throw GeneralTipsException("bpht 已移至 ua。\n您也可以使用 !ba 来体验丰富版本。\n猫猫 Bot 的 bpht 需要输入 !get bpht。"
            )
        }

        val matcher3 = Instruction.DEPRECATED_UUBA_I.matcher(messageText)
        if (matcher3.find()) {
            throw GeneralTipsException("uuba-i 已移至 ua。\n您也可以使用 !ba 来体验丰富版本。")
        }

         */

        val matcher = Instruction.UU_BA.matcher(messageText)
        if (!matcher.find()) return false

        val info = true
        val mode: OsuMode = OsuMode.getMode(matcher.group(FLAG_MODE))

        if (event.isAt) {
            data.value = BPHeadTailParam(UserParam(event.target, null, mode, true), info)
            return true
        }
        val name = matcher.group("name")
        if (name.isNullOrBlank().not()) {
            data.value = BPHeadTailParam(UserParam(null, name, mode, false), info)
            return true
        }
        data.value = BPHeadTailParam(UserParam(event.sender.id, null, mode, false), info)
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: BPHeadTailParam) {
        var bu: BindUser

        // 是否为绑定用户
        if (param.user.qq != null) {
            try {
                bu = bindDao.getBindFromQQ(param.user.qq)
            } catch (e: BindException) {
                if (!param.user.at) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Me)
                } else {
                    throw GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Player)
                }
            }
        } else {
            // 查询其他人 [data]
            val name = param.user.name!!
            var id: Long = 0
            try {
                id = userApiService.getOsuID(name)
                bu = bindDao.getBindUserFromOsuID(id)
            } catch (e: BindException) {
                // 构建只有 data + id 的对象, bindUser == null
                bu = BindUser()
                bu.userID = id
                bu.username = name
            } catch (e: Exception) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Player, name)
            }
        }

        val bests: List<LazerScore>?

        val mode = OsuMode.getMode(param.user.mode, bu.mode, bindDao.getGroupModeConfig(event))

        try {
            bests = scoreApiService.getBestScores(bu, mode)
        } catch (e: WebClientResponseException.BadRequest) {
            // 请求失败 超时/断网
            if (param.user.qq == event.sender.id) {
                throw GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Me)
            } else {
                throw GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Player)
            }
        } catch (e: WebClientResponseException.Unauthorized) {
            // 未绑定
            throw GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Me)
        }

        if (bests.size <= 10) {
            if (!param.user.at && param.user.name.isNullOrBlank()) {
                throw GeneralTipsException(GeneralTipsException.Type.G_NotEnoughBP_Me, mode.fullName)
            } else {
                throw GeneralTipsException(GeneralTipsException.Type.G_NotEnoughBP_Player, mode.fullName)
            }
        }

        calculateApiService.applyBeatMapChanges(bests)
        calculateApiService.applyStarToScores(bests)
        val lines =
                if (OsuMode.isDefaultOrNull(mode)) {
                    getTextPlus(bests, bu.username, "", userApiService)
                } else {
                    getTextPlus(bests, bu.username, mode.fullName, userApiService)
                }

        try {
            val panelParam =
                    lines.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val image = imageService.getPanelAlpha(*panelParam)
            event.reply(image)
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "最好成绩分析（文字版）")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): BPHeadTailParam? {
        val matcher: Matcher
        when {
            OfficialInstruction.UU_BA.matcher(messageText).apply { matcher = this }.find() -> {}

            else -> return null
        }
        val mode = CmdUtil.getMode(matcher)
        val user = CmdUtil.getUserWithoutRange(event, matcher, mode)

        return BPHeadTailParam(UserParam(user.userID, user.username, mode.data!!, false), info = true)
    }

    override fun reply(event: MessageEvent, param: BPHeadTailParam): MessageChain? {
        val mode = param.user.mode
        val bu = BindUser().apply {
            param.user.qq?.let { userID = it }
            param.user.name?.let { username = it }
            this.mode = mode
        }

        val bests = scoreApiService.getBestScores(bu, mode)

        calculateApiService.applyBeatMapChanges(bests)
        calculateApiService.applyStarToScores(bests)

        val modeStr = mode.fullName
        val lines = getTextPlus(bests, bu.username, modeStr, userApiService)
        return MessageChain(lines)
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

    companion object {
        private fun getTimeStr(l: Int): String {
            return if (l < 60) {
                "${l}秒"
            } else {
                "${l / 60}分${l % 60}秒"
            }
        }

        @Deprecated("这个标准获取基本上没人喜欢用了，只有进阶版有人用")
        fun getText(bests: List<LazerScore>, name: String?, mode: String?): String {
            val sb = StringBuffer().append(name).append(": ").append(' ').append(mode).append('\n')
            var allPP = 0.0
            var sSum = 0
            var xSum = 0
            var fcSum = 0
            val modTreeMap = TreeMap<String, AtomicInteger>() // 各个mod的数量

            for (i in bests.indices) {
                val bp = bests[i]
                // 显示前五跟后五的数据
                if (i < 5 || i >= bests.size - 5) {
                    sb.append("#")
                        .append(i + 1)
                        .append(' ')
                        .append(String.format("%.2f", bp.pp))
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
                allPP += bp.pp  // 统计总数
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
                .append(String.format("%.2f", allPP / bests.size))
                .append("PP")
                .append('\n')
                .append("差值: ")
                .append(String.format("%.2f", bests.first().pp - bests.last().pp))
                .append("PP")

            return sb.toString()
        }

        fun getTextPlus(bests: List<LazerScore>, name: String?, mode: String?, userApiService: OsuUserApiService): String {
            if (bests.isEmpty()) return ""
            val sb = StringBuffer().append(name).append(": ").append(' ').append(mode).append('\n')

            val best: LazerScore = bests.first()
            val bestBPM = best.beatmap.BPM!!
            val bestLength = best.beatmap.totalLength.toFloat()

            var star: Double
            var maxStar = best.beatmap.starRating
            var minStar = maxStar
            var maxBPM = bestBPM
            var minBPM = maxBPM
            var maxCombo = best.maxCombo
            var minCombo = maxCombo
            var maxLength = bestLength
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


            bests.forEachIndexed { i, score ->
                val b = score.beatmap
                val length = b.totalLength.toFloat()
                val bpm = b.BPM!!

                score.mods.forEach {
                    if (modSum.containsKey(it.acronym)) {
                        modSum[it.acronym]!!.add(score.weight?.pp ?: 0.0)
                    } else {
                        modSum[it.acronym] = ModData(score.weight?.pp ?: 0.0)
                    }
                }

                avgLength += length
                star = score.beatmap.starRating
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

                if (score.maxCombo < minCombo) {
                    minComboBP = i
                    minCombo = score.maxCombo
                } else if (score.maxCombo > maxCombo) {
                    maxComboBP = i
                    maxCombo = score.maxCombo
                }
                avgCombo += score.maxCombo

                val tthToPp = (score.pp) / max((b.sliders!! + b.spinners!! + b.circles!!), 1)
                if (maxTTHPP < tthToPp) {
                    maxTTHPPBP = i
                    maxTTHPP = tthToPp
                }

                if (mapperSum.containsKey(b.mapperID)) {
                    mapperSum[b.mapperID]!!.add(score.pp)
                } else {
                    mapperSum[b.mapperID] = FavoriteMapperData(score.pp, b.mapperID)
                }
                nowPP += score.weight!!.pp
            }
            avgCombo /= bests.size
            avgLength /= bests.size.toFloat()
            avgStar /= bests.size.toFloat()

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
                .append(String.format("%.0f", minBPM))
                .append('-')
                .append(String.format("%.0f", maxBPM))
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
            val mappersId = mappers.map { u: FavoriteMapperData -> u.uid }
            val mappersInfo = userApiService.getUsers(mappersId)
            val mapperIdToInfo = HashMap<Long, String>()
            for (node in mappersInfo) {
                mapperIdToInfo[node.userID] = node.userName
            }
            mappers.forEach {
                try {
                    sb.append(mapperIdToInfo[it.uid])
                        .append(": ")
                        .append(it.size)
                        .append("x ")
                        .append(decimalFormat.format(it.allPP))
                        .append("PP")
                        .append('\n')
                } catch (e: Exception) {
                    sb.append("UID: ")
                        .append(it.uid)
                        .append(": ")
                        .append(it.size)
                        .append("x ")
                        .append(decimalFormat.format(it.allPP))
                        .append("PP")
                        .append('\n')
                }
            }
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
    }
}
