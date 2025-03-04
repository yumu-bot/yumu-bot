package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.serviceException.PPMinusException
import com.now.nowbot.util.DataUtil.getBonusPP
import com.now.nowbot.util.DataUtil.splitString
import com.now.nowbot.util.Instruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.regex.Matcher

@Service("TEST_PPM")
class TestPPMService(
        private val userApiService: OsuUserApiService,
        private val scoreApiService: OsuScoreApiService,
) : MessageService<Matcher> {

    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: DataValue<Matcher>,
    ): Boolean {
        val m = Instruction.TEST_PPM.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }

    @CheckPermission(test = true)
    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, matcher: Matcher) {
        val names: List<String>? = splitString(matcher.group("data"))
        val inputMode = OsuMode.getMode(matcher.group("mode"))

        if (names.isNullOrEmpty()) {
            throw PPMinusException(PPMinusException.Type.PM_Test_Empty)
        }

        val sb = StringBuilder()

        for (name in names) {
            if (name.isBlank()) {
                continue
            }

            var user: OsuUser
            var bps: List<LazerScore>?

            try {
                user = userApiService.getPlayerInfo(name, inputMode)

                val mode = if (OsuMode.isDefaultOrNull(inputMode)) {
                    user.currentOsuMode
                } else {
                    inputMode
                }

                bps = scoreApiService.getBestScores(user.userID, mode)
            } catch (e: Exception) {
                sb.append("name=").append(name).append(" not found").append('\n')
                continue
            }

            val ppmData = TestPPMData()
            ppmData.init(user, bps)

            sb.append(user.username)
                    .append(',')
                    .append(user.globalRank ?: 0L)
                    .append(',')
                    .append(user.pp)
                    .append(',')
                    .append(user.accuracy)
                    .append(',')
                    .append(user.levelCurrent)
                    .append(',')
                    .append(user.statistics.maxCombo)
                    .append(',')
                    .append(user.totalHits)
                    .append(',')
                    .append(user.playCount)
                    .append(',')
                    .append(user.playTime)
                    .append(',')
                    .append(ppmData.notfc)
                    .append(',')
                    .append(ppmData.rawpp)
                    .append(',')
                    .append(ppmData.xx)
                    .append(',')
                    .append(ppmData.xs)
                    .append(',')
                    .append(ppmData.xa)
                    .append(',')
                    .append(ppmData.xb)
                    .append(',')
                    .append(ppmData.xc)
                    .append(',')
                    .append(ppmData.xd)
                    .append(',')
                    .append(ppmData.ppv0)
                    .append(',')
                    .append(ppmData.accv0)
                    .append(',')
                    .append(ppmData.lengv0)
                    .append(',')
                    .append(ppmData.pgr0)
                    .append(',')
                    .append(ppmData.ppv45)
                    .append(',')
                    .append(ppmData.accv45)
                    .append(',')
                    .append(ppmData.lengv45)
                    .append(',')
                    .append(ppmData.pgr45)
                    .append(',')
                    .append(ppmData.ppv90)
                    .append(',')
                    .append(ppmData.accv45)
                    .append(',')
                    .append(ppmData.lengv90)
                    .append(',')
                    .append(ppmData.pgr90)
                    .append(',')
                    .append(user.statistics?.pP4K ?: 0.0)
                    .append('\n')
        }

        val file = sb.toString().toByteArray(StandardCharsets.UTF_8)

        // 必须群聊
        event.replyFileInGroup(file, names.first() + "...-testppm.csv")
    }

    internal class TestPPMData {
        var ppv0: Float = 0f
        var ppv45: Float = 0f
        var ppv90: Float = 0f
        var accv0: Float = 0f
        var pgr0: Float = 0f
        var pgr45: Float = 0f
        var pgr90: Float = 0f
        var accv45: Float = 0f
        private var accv90: Float = 0f
        var lengv0: Long = 0
        var lengv45: Long = 0
        var lengv90: Long = 0
        private var bpPP: Double = 0.0
        var rawpp: Double = 0.0
        private var bonus: Double = 0.0
        var xd: Int = 0
        var xc: Int = 0
        var xb: Int = 0
        var xa: Int = 0
        var xs: Int = 0
        var xx: Int = 0
        var notfc: Int = 0

        fun init(user: OsuUser, bps: List<LazerScore>) {
            val bpPPs = DoubleArray(bps.size)
            for (i in bps.indices) {
                val bp = bps[i]
                val bpiPP = bp.weight?.PP ?: 0.0
                val bprPP = bp.PP ?: 0.0

                bpPP += bpiPP
                bpPPs[i] = bprPP

                when (bp.rank) {
                    "XH",
                    "X" -> xx++
                    "SH",
                    "S" -> xs++
                    "A" -> xa++
                    "B" -> xb++
                    "C" -> xc++
                    "D" -> xd++
                }
                if (!bp.fullCombo) notfc++
                if (i < 10) {
                    ppv0 += bp.PP!!.toFloat()
                    accv0 += bp.accuracy.toFloat()
                    lengv0 += bp.beatMap.totalLength.toLong()
                } else if (i in 45..54) {
                    ppv45 += bp.PP!!.toFloat()
                    accv45 += bp.accuracy.toFloat()
                    lengv45 += bp.beatMap.totalLength.toLong()
                } else if (i >= 90) {
                    ppv90 += bp.PP!!.toFloat()
                    accv90 += bp.accuracy.toFloat()
                    lengv90 += bp.beatMap.totalLength.toLong()
                }
            }
            // bonus = bonusPP(allBpPP, user.getStatistics().getPlayCount());
            bonus = getBonusPP(user.pp, bpPPs).toDouble()
            rawpp = user.pp - bonus

            ppv0 /= 10f
            ppv45 /= 10f
            ppv90 /= 10f
            accv0 /= 10f
            accv45 /= 10f
            accv90 /= 10f
            lengv0 /= 10
            lengv45 /= 10
            lengv90 /= 10
            if (bps.size < 90) {
                ppv90 = 0f
                accv90 = 0f
                lengv90 = 0
            }
            if (bps.size < 45) {
                ppv45 = 0f
                accv45 = 0f
                lengv45 = 0
            }
            if (bps.size < 10) {
                ppv0 = 0f
                accv0 = 0f
                lengv0 = 0
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TestPPMService::class.java)
    }
}
