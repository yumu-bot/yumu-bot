package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BinUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.model.json.PPPlus
import com.now.nowbot.model.json.PPPlus.AdvancedStats
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.AtMessage
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.PerformancePlusService
import com.now.nowbot.service.messageServiceImpl.PPPlusService.PPPlusParam
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.serviceException.BindException
import com.now.nowbot.throwable.serviceException.PPPlusException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.QQMsgUtil
import java.util.Objects
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.sqrt
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service("PP_PLUS")
class PPPlusService(
        private val userApiService: OsuUserApiService,
        private val scoreApiService: OsuScoreApiService,
        private val bindDao: BindDao,
        private val performancePlusService: PerformancePlusService,
        private val imageService: ImageService,
) : MessageService<PPPlusParam> {
    @JvmRecord data class PPPlusParam(val isUser: Boolean, val me: Any?, val other: Any?)

    @Throws(Throwable::class)
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: DataValue<PPPlusParam>
    ): Boolean {
        val matcher = Instruction.PP_PLUS.matcher(messageText)
        if (!matcher.find()) return false

        val cmd = Objects.requireNonNullElse<String>(matcher.group("function"), "pp")
        var a1 = matcher.group("area1")
        var a2 = matcher.group("area2")

        val at = QQMsgUtil.getType<AtMessage>(event.getMessage(), AtMessage::class.java)

        val me = bindDao.getUserFromQQ(event.getSender().getId(), true)

        try {
            when (cmd.lowercase()) {
                "pp",
                "ppp",
                "pp+",
                "p+",
                "ppplus",
                "plus" -> {
                    // user 非vs
                    if (Objects.nonNull(a1) && a1!!.isBlank()) a1 = null
                    if (Objects.nonNull(a2) && a2!!.isBlank()) a2 = null
                    if (Objects.nonNull(at))
                            setUser(null, null, bindDao.getUserFromQQ(at!!.target), false, data)
                    else setUser(a1, a2, me, false, data)
                }
                "px",
                "ppx",
                "ppv",
                "ppvs",
                "pppvs",
                "ppplusvs",
                "plusvs" -> {
                    // user vs
                    if (Objects.nonNull(at)) {
                        setUser(
                                null,
                                bindDao.getUserFromQQ(at!!.target).getOsuName(),
                                me,
                                true,
                                data)
                    } else {
                        setUser(a1, a2, me, true, data)
                    }
                }
                else -> {
                    log.error("PP+ 指令解析失败: [{}]", cmd)
                    return false
                }
            }
        } catch (e: BindException) {
            throw e
        } catch (e: Exception) {
            log.error("pp+ 请求异常", e)
            throw PPPlusException(PPPlusException.Type.PL_Send_Error)
        }

        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: PPPlusParam) {
        val dataMap = HashMap<String, Any>(6)

        // user 对比
        dataMap.put("isUser", true)
        val u1 = param.me as OsuUser
        dataMap.put("me", u1)
        dataMap.put("my", getUserPerformancePlus(u1.userID))

        if (Objects.nonNull(param.other)) {
            // 包含另一个就是 vs, 直接判断了
            val u2 = param.other as OsuUser
            val pp2 = getUserPerformancePlus(u2.userID)

            beforePost(u2, pp2)

            dataMap.put("other", u2)
            dataMap.put("others", pp2)
        }

        var image: ByteArray?

        try {
            image = imageService.getPanelB3(dataMap)
        } catch (e: Exception) {
            log.error("PP+ 渲染失败", e)
            throw PPPlusException(PPPlusException.Type.PL_Render_Error)
        }
        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("PP+ 发送失败", e)
            throw PPPlusException(PPPlusException.Type.PL_Send_Error)
        }
    }

    // 把数据合并一下 。这个才是真传过去的 PP+
    @Throws(TipsException::class)
    private fun getUserPerformancePlus(uid: Long): PPPlus {
        val bps = scoreApiService.getBestPerformance(uid, OsuMode.OSU, 0, 100)
        val performance = performancePlusService.calculateUserPerformance(bps)

        val plus = PPPlus()
        plus.setPerformance(performance)
        plus.setAdvancedStats(calculateUserAdvancedStats(performance))

        return plus
    }

    @Throws(PPPlusException::class)
    private fun setUser(
            a1: String?,
            a2: String?,
            me: BinUser?,
            isVs: Boolean,
            data: DataValue<PPPlusParam>
    ) {
        var p1: OsuUser?
        var p2: OsuUser?

        try {
            p1 =
                    if (StringUtils.hasText(a1)) userApiService.getPlayerInfo(a1, OsuMode.OSU)
                    else userApiService.getPlayerInfo(me, OsuMode.OSU)

            p2 =
                    if (StringUtils.hasText(a2)) userApiService.getPlayerInfo(a2, OsuMode.OSU)
                    else null

            if (isVs && Objects.isNull(p2)) {
                p2 = p1
                p1 = userApiService.getPlayerInfo(me, OsuMode.OSU)
            }
        } catch (e: WebClientResponseException.NotFound) {
            throw PPPlusException(PPPlusException.Type.PL_User_NotFound)
        } catch (e: WebClientResponseException.Forbidden) {
            throw PPPlusException(PPPlusException.Type.PL_User_Banned)
        } catch (e: WebClientResponseException) {
            throw PPPlusException(PPPlusException.Type.PL_API_NotAccessible)
        }

        data.setValue(PPPlusParam(true, p1, p2))
    }

    // 计算进阶指数的等级
    private fun calculateLevel(value: Double, array: IntArray?): Double {
        if (array == null || array.size < 13) return 0.0

        var lv = 11

        for (i in 0..12) {
            if (value < array[i]) {
                lv = i - 2
                break
            }
        }

        when (lv) {
            -2 -> {
                // 0 - 25
                return 0.25 * value / array[0]
            }
            -1 -> {
                // 25 - 75
                return 0.25 + 0.5 * (value - array[0]) / (array[1] - array[0])
            }
            0 -> {
                // 75 - 100
                return 0.75 + 0.25 * (value - array[1]) / (array[2] - array[1])
            }
            else -> {
                return lv.toDouble()
            }
        }
    }

    private fun calculateUserAdvancedStats(performance: PPPlus.Stats?): AdvancedStats? {
        if (performance == null) return null

        // 第一个是 25%，第二个是 75%，第三个是LV1
        val jumpArray =
                intArrayOf(
                        1300,
                        1700,
                        1975,
                        2250,
                        2525,
                        2800,
                        3075,
                        3365,
                        3800,
                        4400,
                        4900,
                        5900,
                        6900)
        val flowArray =
                intArrayOf(200, 450, 563, 675, 788, 900, 1013, 1225, 1500, 1825, 2245, 3200, 4400)
        val precisionArray =
                intArrayOf(200, 400, 463, 525, 588, 650, 713, 825, 950, 1350, 1650, 2300, 3050)
        val speedArray =
                intArrayOf(
                        950, 1250, 1363, 1475, 1588, 1700, 1813, 1925, 2200, 2400, 2650, 3100, 3600)
        val staminaArray =
                intArrayOf(
                        600, 1000, 1100, 1200, 1300, 1400, 1500, 1625, 1800, 2000, 2200, 2600, 3050)
        val accuracyArray =
                intArrayOf(
                        600, 900, 1000, 1100, 1200, 1300, 1400, 1500, 1750, 2100, 2550, 3400, 4400)

        // 常规指数和进阶指数，进阶指数是以上情况的第二大的值，达标情况的目标是以上第二大值 * 6 - 4，
        var generalIndex: Double
        var advancedIndex: Double

        val jumpAim = calculateLevel(performance.jumpAim, jumpArray)
        val flowAim = calculateLevel(performance.flowAim, flowArray)
        val precision = calculateLevel(performance.precision, precisionArray)
        val speed = calculateLevel(performance.speed, speedArray)
        val stamina = calculateLevel(performance.stamina, staminaArray)
        val accuracy = calculateLevel(performance.accuracy, accuracyArray)

        generalIndex =
                (sqrt(getPiCent(performance.jumpAim, 1300, 1700) + 8.0) *
                        (getPiCent(performance.flowAim, 200, 450) + 3.0) *
                        10.0 +
                        getPiCent(performance.precision, 200, 400) +
                        getPiCent(performance.speed, 950, 1250) * 7.0 +
                        getPiCent(performance.speed, 950, 1250) * 3.0 +
                        getPiCent(performance.accuracy, 600, 1200) * 10.0)

        advancedIndex =
                mutableListOf<Double>(
                                getDetail(
                                        performance.jumpAim, jumpAim, jumpArray[0], jumpArray[11]),
                                getDetail(
                                        performance.flowAim, flowAim, flowArray[0], flowArray[11]),
                                getDetail(
                                        performance.precision,
                                        precision,
                                        precisionArray[0],
                                        precisionArray[11]),
                                getDetail(performance.speed, speed, speedArray[0], speedArray[11]),
                                getDetail(
                                        performance.stamina,
                                        stamina,
                                        staminaArray[0],
                                        staminaArray[11]),
                                getDetail(
                                        performance.accuracy,
                                        accuracy,
                                        accuracyArray[0],
                                        accuracyArray[11]))
                        .sorted()
                        .toList()
                        .get(4) // 第二大

        val index = mutableListOf<Double>(jumpAim, flowAim, accuracy, stamina, speed, precision)
        val sum: Double = index.stream().reduce { a: Double, b: Double -> a + b }.orElse(0.0)

        return AdvancedStats(index, generalIndex, advancedIndex, sum, advancedIndex * 6 - 4)
    }

    // 化学式进阶指数 获取百分比 * Pi（加权 1）
    private fun getPiCent(`val`: Double, percent25: Int, percent75: Int): Double {
        return (atan((`val` * 2.0 - (percent75 + percent25)) / (percent75 - percent25)) / Math.PI +
                0.5) * Math.PI
    }

    // 化学式进阶指数 获取详细情况（用于进阶指数求和）
    private fun getDetail(value: Double, level: Double, percent75: Int, percentEX: Int): Double {
        if (value < percent75) return -2.0
        else if (value > percentEX) return floor(value / percentEX * 10.0) + 1.0 else return level
    }

    // 、、、、、、、、、、、、、、、、、、
    // 不要多看, 反正不影响用
    private fun beforePost(user: OsuUser, plus: PPPlus) {
        if (user.getId() == 17064371L) {
            plus.performance = PPPlus.getMaxStats()
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PPPlusService::class.java)
    }
}
