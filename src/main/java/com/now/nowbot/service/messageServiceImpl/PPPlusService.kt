package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.PerformancePlusDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.osu.PPPlus
import com.now.nowbot.model.osu.PPPlus.AdvancedStats
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.PPPlusService.PPPlusParam
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.Instruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.sqrt

@Service("PP_PLUS")
class PPPlusService(
    private val userApiService: OsuUserApiService,
    private val scoreApiService: OsuScoreApiService,
    private val bindDao: BindDao,
    private val imageService: ImageService,
    private val plusDao: PerformancePlusDao
) : MessageService<PPPlusParam> {
    data class PPPlusParam(val isUser: Boolean, val me: Any?, val other: Any?)

    override fun isHandle(
        event: MessageEvent, messageText: String, data: DataValue<PPPlusParam>
    ): Boolean {
        val matcher = Instruction.PP_PLUS.matcher(messageText)
        if (!matcher.find()) return false
        val cmd = matcher.group("function")?.ifBlank { "pp" } ?: "pp"
        val a1 = matcher.group("area1")?.ifBlank { null }
        val a2 = matcher.group("area2")?.ifBlank { null }

        val me = bindDao.getBindFromQQOrNull(event.sender.contactID)

        try {
            when (cmd.lowercase()) {
                "pp", "ppp", "pp+", "p+", "ppplus", "plus" -> { // user 非vs

                    if (event.hasAt()) {
                        setUser(null, null, bindDao.getBindFromQQ(event.target), false, data)
                    } else {
                        setUser(a1, a2, me, false, data)
                    }
                }

                "px", "ppx", "ppv", "ppvs", "pppvs", "ppplusvs", "plusvs" -> { // user vs
                    if (event.hasAt()) {
                        setUser(
                            bindDao.getBindFromQQ(event.target).username, null, me, true, data
                        )
                    } else {
                        setUser(a1, a2, me, true, data)
                    }
                }

                else -> {
                    log.error("PP+ 指令解析失败: {}", cmd)
                    return false
                }
            }
        } catch (e: BindException) {
            throw e
        } catch (e: Exception) {
            log.error("pp+ 请求异常", e)
            throw IllegalStateException.Fetch("PP+")
        }

        return true
    }

    override fun handleMessage(event: MessageEvent, param: PPPlusParam): ServiceCallStatistic? {
        val me = param.me as OsuUser
        val other = param.other as? OsuUser
        val isVs = other != null

        val dataMap = mutableMapOf(
            "isUser" to true,
            "me" to me,
            "my" to getUserPerformancePlus(me.userID).apply {
                this.putSpringStats(me.userID)
            },
            "isVs" to isVs
        ).apply {
            other?.let {
                put("other", it)
                put("others", getUserPerformancePlus(it.userID).apply {
                    this.putSpringStats(me.userID)
                })
            }
        }

        val image = runCatching {
            imageService.getPanel(dataMap, "B3")
        }.getOrElse { e ->
            log.error("PP+ 渲染失败", e)
            throw IllegalStateException.Render("PP+")
        }

        runCatching {
            event.reply(image)
        }.onFailure { e ->
            log.error("PP+ 发送失败", e)
            throw IllegalStateException.Send("PP+")
        }

        // 使用 when 或 if 表达式直接返回，结构更清晰
        return when (other) {
            null -> ServiceCallStatistic.build(
                event,
                userID = me.userID,
                mode = me.currentOsuMode
            )
            else -> ServiceCallStatistic.builds(
                event,
                userIDs = listOf(me.userID, other.userID),
                modes = listOf(me.currentOsuMode)
            )
        }
    }

    // 把数据合并一下。这个才是真传过去的 PP+
    private fun getUserPerformancePlus(uid: Long): PPPlus {
        val bests = scoreApiService.getBestScores(uid, OsuMode.OSU)

        return plusDao.getUserPerformancePlusMax(bests)
            ?: throw IllegalStateException.Fetch("表现分加")

//        val performance = performancePlusAPIService.getUserPerformancePlusStats(bests)
//        val stats = calculateUserAdvancedStats(performance)
//
//        val plus = PPPlus().apply {
//            this.performance = performance
//            this.advancedStats = stats
//        }
//
//        return plus
    }

    private fun setUser(
        name1: String?, name2: String?, me: BindUser?, isVs: Boolean, data: DataValue<PPPlusParam>
    ) {

        val user1: OsuUser
        val user2: OsuUser?

        if (!name2.isNullOrBlank()) {
            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { userApiService.getOsuUser(
                    name1 ?: me?.username ?: throw BindException.NotBindException.YouNotBind(), OsuMode.OSU)
                },
                { userApiService.getOsuUser(name2, OsuMode.OSU) }
            )

            user1 = async.first
            user2 = async.second
        } else if (isVs && !name1.isNullOrBlank() && me != null) {
            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { userApiService.getOsuUser(me.username, OsuMode.OSU) },
                { userApiService.getOsuUser(name1, OsuMode.OSU) }
            )

            user1 = async.first
            user2 = async.second
        } else if (!name1.isNullOrBlank()) {
            user1 = userApiService.getOsuUser(name1, OsuMode.OSU)
            user2 = null
        } else {
            user1 = userApiService.getOsuUser(
                me?.username ?: throw BindException.NotBindException.YouNotBind(),
                OsuMode.OSU)
            user2 = null
        }

        data.value = PPPlusParam(true, user1, user2)
    }



    companion object {

        // 第一个是 25%，第二个是 75%，第三个是LV1
        private val JUMP_ARRAY = intArrayOf(1300, 1700, 1975, 2250, 2525, 2800, 3075, 3365, 3800, 4400, 4900, 5900, 6900)
        private val FLOW_ARRAY = intArrayOf(200, 450, 563, 675, 788, 900, 1013, 1225, 1500, 1825, 2245, 3200, 4400)
        private val PRECISION_ARRAY = intArrayOf(200, 400, 463, 525, 588, 650, 713, 825, 950, 1350, 1650, 2300, 3050)
        private val SPEED_ARRAY = intArrayOf(950, 1250, 1363, 1475, 1588, 1700, 1813, 1925, 2200, 2400, 2650, 3100, 3600)
        private val STAMINA_ARRAY = intArrayOf(600, 1000, 1100, 1200, 1300, 1400, 1500, 1625, 1800, 2000, 2200, 2600, 3050)
        private val ACCURACY_ARRAY = intArrayOf(600, 900, 1000, 1100, 1200, 1300, 1400, 1500, 1750, 2100, 2550, 3400, 4400)

        private val log: Logger = LoggerFactory.getLogger(PPPlusService::class.java)

        fun calculateUserAdvancedStats(performance: PPPlus.Stats?): AdvancedStats? {
            if (performance == null) return null

            val advancedIndex: Double

            val jumpAim = calculateLevel(performance.jumpAim, JUMP_ARRAY)
            val flowAim = calculateLevel(performance.flowAim, FLOW_ARRAY)
            val precision = calculateLevel(performance.precision, PRECISION_ARRAY)
            val speed = calculateLevel(performance.speed, SPEED_ARRAY)
            val stamina = calculateLevel(performance.stamina, STAMINA_ARRAY)
            val accuracy = calculateLevel(performance.accuracy, ACCURACY_ARRAY)
            val aim = calculateLevel(performance.aim, JUMP_ARRAY)

            // 常规指数和进阶指数，进阶指数是以上情况的第二大的值，达标情况的目标是以上第二大值 * 6 - 4，
            val generalIndex =
                (sqrt(
                    (getPiCent(performance.jumpAim, 1300, 1700) + 8.0)
                            * (getPiCent(performance.flowAim, 200, 450) + 3.0)
                ) * 10.0
                        + getPiCent(performance.precision, 200, 450)
                        + getPiCent(performance.speed, 950, 1250) * 7.0
                        + getPiCent(performance.stamina, 600, 1000) * 3.0
                        + getPiCent(performance.accuracy, 600, 1200) * 10.0
                        )


            advancedIndex = mutableListOf(
                getDetail(
                    performance.jumpAim, jumpAim, JUMP_ARRAY[1], JUMP_ARRAY.last()
                ), getDetail(
                    performance.flowAim, flowAim, FLOW_ARRAY[1], FLOW_ARRAY.last()
                ), getDetail(
                    performance.precision, precision, PRECISION_ARRAY[1], PRECISION_ARRAY.last()
                ), getDetail(performance.speed, speed, SPEED_ARRAY[1], SPEED_ARRAY.last()), getDetail(
                    performance.stamina, stamina, STAMINA_ARRAY[1], STAMINA_ARRAY.last()
                ), getDetail(
                    performance.accuracy, accuracy, ACCURACY_ARRAY[1], ACCURACY_ARRAY.last()
                )
            ).sortedDescending()[1] // 第二大

            val index = listOf(jumpAim, flowAim, accuracy, stamina, speed, precision, aim)
            val sum: Double = index.sum()

            return AdvancedStats(index, generalIndex, advancedIndex, sum, advancedIndex * 6 - 4)
        }

        // 化学式进阶指数 获取百分比 * Pi（加权 1）
        private fun getPiCent(value: Double, percent25: Int, percent75: Int): Double {
            return (atan((value * 2.0 - (percent75 + percent25)) / (percent75 - percent25)) / Math.PI + 0.5) * Math.PI
        }

        // 化学式进阶指数 获取详细情况（用于进阶指数求和）
        private fun getDetail(value: Double, level: Double, percent75: Int, percentEX: Int): Double {
            return if (value < percent75) {
                -2.0
            } else if (value > percentEX) {
                floor(value / percentEX * 10.0) + 1.0
            } else {
                level
            }
        }
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
                -2 -> { // 0 - 25
                    return 0.25 * value / array[0]
                }

                -1 -> { // 25 - 75
                    return 0.25 + 0.5 * (value - array[0]) / (array[1] - array[0])
                }

                0 -> { // 75 - 100
                    return 0.75 + 0.25 * (value - array[1]) / (array[2] - array[1])
                }

                else -> {
                    return lv.toDouble()
                }
            }
        }

        private fun PPPlus.putSpringStats(userID: Long) {
            if (userID == 17064371L) {
                this.performance = PPPlus.maxStats
            }
        }
    }


}
