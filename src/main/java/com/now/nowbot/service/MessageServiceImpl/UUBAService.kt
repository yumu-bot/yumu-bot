package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BinUser
import com.now.nowbot.model.JsonData.Score
import com.now.nowbot.model.Service.UserParam
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.AtMessage
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.MessageServiceImpl.UUBAService.BPHeadTailParam
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.OsuApiService.OsuScoreApiService
import com.now.nowbot.service.OsuApiService.OsuUserApiService
import com.now.nowbot.throwable.ServiceException.BPAnalysisException
import com.now.nowbot.throwable.ServiceException.BindException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.CmdUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import com.now.nowbot.util.command.FLAG_MODE
import org.apache.logging.log4j.util.Strings
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
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

    //bpht 的全称大概是 BP Head / Tail
    data class BPHeadTailParam(val user: UserParam, val info: Boolean)

    @Throws(BPAnalysisException::class)
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<BPHeadTailParam>): Boolean {
        //旧功能指引
        val matcher2 = Instruction.DEPRECATED_BPHT.matcher(messageText)
        if (matcher2.find() && Strings.isNotBlank(matcher2.group("bpht"))) {
            throw BPAnalysisException(BPAnalysisException.Type.BA_Instruction_Deprecated)
        }

        val matcher = Instruction.UU_BA.matcher(messageText)
        if (!matcher.find()) return false
        val info = Strings.isNotBlank(matcher.group("info"))
        val mode: OsuMode = OsuMode.getMode(matcher.group(FLAG_MODE))
        val at = QQMsgUtil.getType(event.message, AtMessage::class.java)

        if (Objects.nonNull(at)) {
            data.value = BPHeadTailParam(
                UserParam(at!!.target, null, mode, true), info
            )
            return true
        }
        val name = matcher.group("name")
        if (Objects.nonNull(name) && Strings.isNotBlank(name)) {
            data.setValue(
                BPHeadTailParam(
                    UserParam(null, name, mode, false), info
                )
            )
            return true
        }
        data.value = BPHeadTailParam(
            UserParam(event.sender.id, null, mode, false), info
        )
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
            //查询其他人 [data]
            val name = param.user.name
            var id: Long = 0
            try {
                id = userApiService.getOsuId(name)
                bu = bindDao.getUserFromOsuid(id)
            } catch (e: BindException) {
                //构建只有 data + id 的对象, binUser == null
                bu = BinUser()
                bu.osuID = id
                bu.osuName = name
            } catch (e: Exception) {
                throw BPAnalysisException(BPAnalysisException.Type.BA_Player_NotFound)
            }
        }

        val bps: List<Score>?

        val mode = OsuMode.getMode(param.user.mode, bu.osuMode)

        try {
            bps = scoreApiService.getBestPerformance(bu, mode, 0, 100)
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

        if (bps == null || bps.size <= 10) {
            if (!param.user.at && Objects.isNull(param.user.name)) {
                throw BPAnalysisException(BPAnalysisException.Type.BA_Me_NotEnoughBP, mode!!.getName())
            } else {
                throw BPAnalysisException(BPAnalysisException.Type.BA_Player_NotEnoughBP, mode!!.getName())
            }
        }

        beatmapApiService.applySRAndPP(bps)
        val lines = if (param.info) {
            if (mode == null || mode == OsuMode.DEFAULT) {
                getAllMsgI(bps, bu.osuName, "")
            } else {
                getAllMsgI(bps, bu.osuName, mode.getName())
            }
        } else {
            if (mode == null || mode == OsuMode.DEFAULT) {
                getAllMsg(bps, bu.osuName, "")
            } else {
                getAllMsg(bps, bu.osuName, mode.getName())
            }
        }

        try {
            val image = imageService.getPanelAlpha(*lines)
            from.sendImage(image)
        } catch (e: Exception) {
            throw BPAnalysisException(BPAnalysisException.Type.BA_Send_UUError)
        }
    }

    override fun accept(event: MessageEvent, messageText: String): BPHeadTailParam? {
        val info: Boolean
        var matcher: Matcher
        when {
            OfficialInstruction.UU_BA
                .matcher(messageText)
                .apply { matcher = this }
                .find() -> {
                info = false
            }

            OfficialInstruction.UU_BAI
                .matcher(messageText)
                .apply { matcher = this }
                .find() -> {
                info = true
            }

            else -> return null
        }
        val mode = CmdUtil.getMode(matcher)
        val isMyself = AtomicBoolean(false)
        val user = CmdUtil.getUserWithOutRange(event, matcher, mode, isMyself)
            ?: throw if (isMyself.get()) TipsException("我还不知道你是谁哦")
            else BPAnalysisException(BPAnalysisException.Type.BA_Player_NotFound)

        return BPHeadTailParam(
            UserParam(user.userID, user.username, mode.data, false), info
        )
    }

    override fun reply(event: MessageEvent, data: BPHeadTailParam): MessageChain? {
        val mode = data.user.mode
        val bu = BinUser()
        with(bu) {
            osuID = data.user.qq
            osuName = data.user.name
            osuMode = mode
        }
        val bps = scoreApiService.getBestPerformance(bu, mode, 0, 100)
        beatmapApiService.applySRAndPP(bps)
        val modeStr = mode?.getName() ?: ""
        val lines = if (data.info) {
            getAllMsgI(bps, bu.osuName, modeStr)
        } else {
            getAllMsg(bps, bu.osuName, modeStr)
        }
        return QQMsgUtil.getImage(imageService.getPanelAlpha(*lines))
    }

    fun getAllMsg(bps: List<Score>, name: String?, mode: String?): Array<String?> {
        val sb = StringBuffer().append(name).append(": ").append(' ').append(mode).append('\n')
        var allPP = 0.0
        var sSum = 0
        var xSum = 0
        var fcSum = 0
        val modTreeMap = TreeMap<String, AtomicInteger>() //各个mod的数量

        for (i in bps.indices) {
            val bp = bps[i]
            //显示前五跟后五的数据
            if (i < 5 || i >= bps.size - 5) {
                sb.append("#")
                    .append(i + 1)
                    .append(' ')
                    .append(String.format("%.2f", bp.pp))
                    .append(' ')
                    .append(String.format("%.2f", 100 * bp.accuracy))
                    .append('%')
                    .append(' ')
                    .append(bp.rank)
                if (!bp.mods.isEmpty()) {
                    sb.append(" +")
                    for (m in bp.mods) {
                        sb.append(m).append(' ')
                    }
                }
                sb.append('\n')
            } else if (i == 5) {
                sb.append("...").append('\n')
            }
            allPP += bp.pp.toDouble() //统计总数
            if (!bp.mods.isEmpty()) {
                for (j in bp.mods.indices) {
                    val mod = bp.mods[j]
                    if (!modTreeMap.containsKey(mod)) modTreeMap[mod] = AtomicInteger()
                    else modTreeMap[mod]!!.incrementAndGet()
                }
            }
            if (bp.rank.contains("S")) sSum++
            if (bp.rank.contains("X")) {
                sSum++
                xSum++
            }
            if (bp.isPerfect) fcSum++
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

        sb.append('\n').append("完美 FC: ").append(fcSum).append('\n')
            .append("平均: ").append(String.format("%.2f", allPP / bps.size)).append("PP").append('\n')
            .append("差值: ").append(String.format("%.2f", bps.first().getPP() - bps.last().getPP())).append("PP")

        return sb.toString().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    }

    fun getAllMsgI(bps: List<Score>, name: String?, mode: String?): Array<String?> {
        if (bps.isEmpty()) return arrayOfNulls(0)
        val sb = StringBuffer().append(name).append(": ").append(' ').append(mode).append('\n')

        val BP1: Score = bps.first()
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

        var avgLength = 0f
        var avgCombo = 0
        var avgStar = 0f

        var maxTTHPPBP = 0
        var maxTTHPP = 0f
        var nowPP = 0f

        val modSum = TreeMap<String, modData>() //各个mod的数量

        val mapperSum = TreeMap<Long, mapperData>()
        val decimalFormat = DecimalFormat("0.00") //acc格式

        for (i in bps.indices) {
            val bp = bps[i]
            val b = bp.beatMap
            val length = b.totalLength.toFloat()
            val bpm = b.bpm
            bp.mods.forEach(Consumer { r: String ->
                if (modSum.containsKey(r)) {
                    modSum[r]!!.add(Optional.ofNullable(bp.weightedPP).orElse(0f))
                } else {
                    modSum[r] = modData(Optional.ofNullable(bp.weightedPP).orElse(0f))
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

            val tthToPp = (bp.pp) / (b.sliders + b.spinners + b.circles)
            if (maxTTHPP < tthToPp) {
                maxTTHPPBP = i
                maxTTHPP = tthToPp
            }

            if (mapperSum.containsKey(b.mapperID)) {
                mapperSum[b.mapperID]!!.add(bp.pp)
            } else {
                mapperSum[b.mapperID] = mapperData(bp.pp, b.mapperID)
            }
            nowPP += bp.weightedPP
        }
        avgCombo /= bps.size
        avgLength /= bps.size.toFloat()
        avgStar /= bps.size.toFloat()

        sb.append("平均时间: ").append(getTimeStr(avgLength.toInt())).append('\n')
        sb.append("时间最长: BP").append(maxLengthBP + 1).append(' ').append(getTimeStr(maxLength.toInt())).append('\n')
        sb.append("时间最短: BP").append(minLengthBP + 1).append(' ').append(getTimeStr(minLength.toInt())).append('\n')
        sb.append("——————————").append('\n')

        sb.append("平均连击: ").append(avgCombo).append('x').append('\n')
        sb.append("连击最大: BP").append(maxComboBP + 1).append(' ').append(maxCombo).append('x').append('\n')
        sb.append("连击最小: BP").append(minComboBP + 1).append(' ').append(minCombo).append('x').append('\n')
        sb.append("——————————").append('\n')

        sb.append("平均星数: ").append(String.format("%.2f", avgStar)).append('*').append('\n')
        sb.append("星数最高: BP").append(maxStarBP + 1).append(' ').append(String.format("%.2f", maxStar)).append('*')
            .append('\n')
        sb.append("星数最低: BP").append(minStarBP + 1).append(' ').append(String.format("%.2f", minStar)).append('*')
            .append('\n')
        sb.append("——————————").append('\n')

        sb.append("PP/TTH 比例最大: BP").append(maxTTHPPBP + 1)
            .append("，为").append(decimalFormat.format(maxTTHPP.toDouble())).append('倍').append('\n')

        sb.append("BPM 区间: ").append(String.format("%.0f", minBPM)).append('-').append(String.format("%.0f", maxBPM))
            .append('\n')
        sb.append("——————————").append('\n')

        sb.append("谱师: \n")
        val mappers = mapperSum.values.stream()
            .sorted { o1: mapperData, o2: mapperData ->
                if (o1.size != o2.size) return@sorted 2 * (o2.size - o1.size)
                java.lang.Float.compare(o2.allPP, o1.allPP)
            }
            .limit(9).toList()
        val mappersId = mappers.stream().map { u: mapperData -> u.uid }.toList()
        val mappersInfo = userApiService.getUsers(mappersId)
        val mapperIdToInfo = HashMap<Long, String>()
        for (node in mappersInfo) {
            mapperIdToInfo[node.userID] = node.userName
        }
        mappers.forEach(Consumer { mapper: mapperData ->
            try {
                sb.append(mapperIdToInfo[mapper.uid]).append(": ").append(mapper.size).append("x ")
                    .append(decimalFormat.format(mapper.allPP.toDouble())).append("PP").append('\n')
            } catch (e: Exception) {
                sb.append("UID: ").append(mapper.uid).append(": ").append(mapper.size).append("x ")
                    .append(decimalFormat.format(mapper.allPP.toDouble())).append("PP").append('\n')
            }
        })
        sb.append("——————————").append('\n')
        sb.append("模组数量: \n")
        val finalAllPP = nowPP
        modSum.forEach { (mod: String, sum: modData) ->
            sb.append(mod).append(": ").append(sum.size).append("x ")
                .append(decimalFormat.format(sum.allPP.toDouble())).append("PP ")
                .append('(').append(decimalFormat.format((100 * sum.allPP / finalAllPP).toDouble())).append('%')
                .append(')')
                .append('\n')
        }
        return sb.toString().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    }

    internal class mapperData(var allPP: Float, var uid: Long) {
        var size: Int = 1

        fun add(pp: Float) {
            allPP += pp
            size++
        }
    }

    internal class modData(var allPP: Float) {
        var size: Int = 1

        fun add(pp: Float) {
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


