package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.config.Permission
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.serviceException.PPMinusException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.DataUtil.getBonusPP
import com.now.nowbot.util.DataUtil.splitString
import com.now.nowbot.util.Instruction
import io.ktor.util.collections.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.regex.Matcher
import kotlin.math.min

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
        if (m.find() && Permission.isGroupAdmin(event)) {
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

        val userMap = ConcurrentMap<String, OsuUser?>(names.size)
        val dataMap = ConcurrentMap<String, TestPPMData?>(names.size)

        event.reply("正在处理玩家数据。注意需要输入玩家名，而并非玩家 ID！")

        names.chunked(20).forEachIndexed { i, chunk ->
            val actions = chunk.map {
                return@map AsyncMethodExecutor.Supplier<Unit> {
                    val user: OsuUser
                    val bps: List<LazerScore>

                    try {
                        // 神奇！如果这里传 ID，会有问题！
                        user = userApiService.getPlayerInfo(it, inputMode)

                        val mode = if (OsuMode.isDefaultOrNull(inputMode)) {
                            user.currentOsuMode
                        } else {
                            inputMode
                        }

                        bps = scoreApiService.getBestScores(user.userID, mode)

                        val ppmData = TestPPMData()

                        ppmData.init(user, bps)

                        userMap.putIfAbsent(it, user)
                        dataMap.putIfAbsent(it, ppmData)
                    } catch (e: Exception) {
                        // sb.append("name=").append(name).append(" not found").append('\n')
                        e.printStackTrace()
                        userMap.putIfAbsent(it, null)
                        dataMap.putIfAbsent(it, null)
                    }
                }
            }

            event.reply("当前进度：${min(20 * i, names.size)} - ${min(20 * (i + 1) - 1, names.size)} / ${names.size}")

            AsyncMethodExecutor.AsyncSupplier(actions)

            val await = 20 * (i + 1) < names.size

            event.reply("处理完成：${min(20 * i, names.size)} - ${min(20 * (i + 1) - 1, names.size)}，${if (await) "等待 1 秒" else "即将发送"}")

            if (await) {
                // 虚拟线程等待，避免 429
                Thread.sleep(1000L)
            }
        }

        val sb = StringBuilder()

        sb.append("name,rank,pp,acc,level,maxcombo,totalhits,pc,pt,notfc,rawpp,ss,s,a,b,c,d,top10pp,top10acc,top10length,top10rate,mid10pp,mid10acc,mid10length,mid10rate,bottom10pp,bottom10acc,bottom10length,bottom10rate,4keypp")
            .append('\n')

        for (name in names) {
            val user = userMap[name]
            val data = dataMap[name]

            if (user == null || data == null) {
                sb.append("name=").append(name).append(" not found").append('\n')
                continue
            }

            sb.append(user.username)
                    .append(',')
                    .append(user.globalRank ?: 0L)
                    .append(',')
                    .append(user.pp ?: 0.0)
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
                    .append(data.notfc)
                    .append(',')
                    .append(data.rawpp)
                    .append(',')
                    .append(data.xx)
                    .append(',')
                    .append(data.xs)
                    .append(',')
                    .append(data.xa)
                    .append(',')
                    .append(data.xb)
                    .append(',')
                    .append(data.xc)
                    .append(',')
                    .append(data.xd)
                    .append(',')
                    .append(data.ppv0)
                    .append(',')
                    .append(data.accv0)
                    .append(',')
                    .append(data.lengv0)
                    .append(',')
                    .append(data.pgr0)
                    .append(',')
                    .append(data.ppv45)
                    .append(',')
                    .append(data.accv45)
                    .append(',')
                    .append(data.lengv45)
                    .append(',')
                    .append(data.pgr45)
                    .append(',')
                    .append(data.ppv90)
                    .append(',')
                    .append(data.accv45)
                    .append(',')
                    .append(data.lengv90)
                    .append(',')
                    .append(data.pgr90)
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
            bonus = getBonusPP(user.pp, bpPPs)
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
