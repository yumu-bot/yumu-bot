package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
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
import com.now.nowbot.service.PerformancePlusService
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
    private val performancePlusService: PerformancePlusService,
    private val imageService: ImageService,
) : MessageService<PPPlusParam> {
    data class PPPlusParam(val isUser: Boolean, val me: Any?, val other: Any?)

    override fun isHandle(
        event: MessageEvent, messageText: String, data: DataValue<PPPlusParam>
    ): Boolean {
        val matcher = Instruction.PP_PLUS.matcher(messageText)
        if (!matcher.find()) return false

        if (!Permission.isSuperAdmin(event)) return false

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
            "my" to getUserPerformancePlus(me.userID),
            "isVs" to isVs
        ).apply {
            other?.let {
                put("other", it)
                put("others", getUserPerformancePlus(it.userID))
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
        val performance = performancePlusService.calculateUserPerformance(bests)
        val stats = calculateUserAdvancedStats(performance)

        val plus = PPPlus().apply {
            this.performance = performance
            this.advancedStats = stats
        }

        return plus
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

    private fun calculateLevel(value: Double, array: IntArray?): Double {
        // 1. Guard Clause: 预校验，利用 ? 和 size 判断直接返回
        if (array == null || array.size < 13) return 0.0

        // 2. 核心逻辑：利用 indexOfFirst 代替 for 循环查找第一个不满足条件的索引
        // 如果全部都满足，则说明等级到了最高的 11
        val index = array.indexOfFirst { value < it }
        val lv = if (index == -1) 11 else index - 2

        // 3. 表达式化 when：直接返回计算结果，逻辑更紧凑
        return when (lv) {
            -2 -> 0.25 * value / array[0]

            -1 -> 0.25 + 0.5 * (value - array[0]) / (array[1] - array[0])

            0  -> 0.75 + 0.25 * (value - array[1]) / (array[2] - array[1])

            else -> lv.toDouble()
        }
    }

    companion object {
        // 将静态常量数组提取出来，避免每次调用函数都重新创建对象
        private val JUMP_ARRAY = intArrayOf(1300, 1700, 1975, 2250, 2525, 2800, 3075, 3365, 3800, 4400, 4900, 5900, 6900)
        private val FLOW_ARRAY = intArrayOf(200, 450, 563, 675, 788, 900, 1013, 1225, 1500, 1825, 2245, 3200, 4400)
        private val PRECISION_ARRAY = intArrayOf(200, 400, 463, 525, 588, 650, 713, 825, 950, 1350, 1650, 2300, 3050)
        private val SPEED_ARRAY = intArrayOf(950, 1250, 1363, 1475, 1588, 1700, 1813, 1925, 2200, 2400, 2650, 3100, 3600)
        private val STAMINA_ARRAY = intArrayOf(600, 1000, 1100, 1200, 1300, 1400, 1500, 1625, 1800, 2000, 2200, 2600, 3050)
        private val ACCURACY_ARRAY = intArrayOf(600, 900, 1000, 1100, 1200, 1300, 1400, 1500, 1750, 2100, 2550, 3400, 4400)

        private val log: Logger = LoggerFactory.getLogger(PPPlusService::class.java)
    }

    private fun calculateUserAdvancedStats(performance: PPPlus.Stats?): AdvancedStats? {
        // 1. 使用 ?.let 替代 if (null) return null，保持链式调用
        return performance?.run {
            // 2. 批量计算 Level
            val jump = calculateLevel(jumpAim, JUMP_ARRAY)
            val flow = calculateLevel(flowAim, FLOW_ARRAY)
            val prec = calculateLevel(precision, PRECISION_ARRAY)
            val spd = calculateLevel(speed, SPEED_ARRAY)
            val sta = calculateLevel(stamina, STAMINA_ARRAY)
            val acc = calculateLevel(accuracy, ACCURACY_ARRAY)
            val aimLevel = calculateLevel(aim, JUMP_ARRAY)

            // 3. 常规指数计算 (利用 math 函数简化视觉)
            val generalIndex = (sqrt((getPiCent(jumpAim, 1300, 1700) + 8.0) * (getPiCent(flowAim, 200, 450) + 3.0)) * 10.0
                    + getPiCent(precision, 200, 450)
                    + getPiCent(speed, 950, 1250) * 7.0
                    + getPiCent(stamina, 600, 1000) * 3.0
                    + getPiCent(accuracy, 600, 1200) * 10.0)

            // 4. 进阶指数：通过映射和排序获取第二大值
            // 这里的逻辑是将各个维度与其对应的数组配置“绑定”处理
            val advancedIndex = listOf(
                Triple(jumpAim, jump, JUMP_ARRAY),
                Triple(flowAim, flow, FLOW_ARRAY),
                Triple(precision, prec, PRECISION_ARRAY),
                Triple(speed, spd, SPEED_ARRAY),
                Triple(stamina, sta, STAMINA_ARRAY),
                Triple(accuracy, acc, ACCURACY_ARRAY)
            ).map { (value, lv, arr) ->
                getDetail(value, lv, arr[1], arr.last())
            }.sortedDescending()[1]

            val levels = listOf(jump, flow, acc, sta, spd, prec, aimLevel)

            // 5. 直接构造返回结果
            AdvancedStats(
                levels,
                generalIndex,
                advancedIndex,
                levels.sum(),
                advancedIndex * 6 - 4
            )
        }
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
    
    private fun beforePost(user: OsuUser, plus: PPPlus) {
        if (user.id == 17064371L) {
            plus.performance = PPPlus.maxStats
        }
    }
}
