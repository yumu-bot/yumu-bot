package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService

import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.DataUtil.splitString
import com.now.nowbot.util.Instruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import kotlin.math.max
import kotlin.math.round
import kotlin.math.roundToInt

@Service("TEST_FIX") class TestFixPPService(
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val scoreApiService: OsuScoreApiService,
    private val calculateApiService: OsuCalculateApiService
) : MessageService<TestFixPPService.TestFixPPParam> {

    data class TestFixPPParam(val names: List<String>, val mode: OsuMode)

    data class TestFixPPData(val user: OsuUser?, val bests: List<LazerScore>)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<TestFixPPParam>
    ): Boolean {
        val matcher = Instruction.TEST_FIX.matcher(messageText)
        if (matcher.find()) {
            val names: List<String>? = splitString(matcher.group("data"))

            if (names.isNullOrEmpty()) {
                throw IllegalArgumentException.WrongException.PlayerName()
            }

            val mode = OsuMode.getMode(matcher.group("mode"))

            if (Permission.isCommonUser(event)) {
                throw PermissionException.DeniedException.BelowGroupAdministrator()
            }

            if (names.isEmpty()) {
                throw throw IllegalStateException.Fetch("玩家名")
            }

            data.value = TestFixPPParam(names, mode)
            return true
        } else return false
    }

    override fun HandleMessage(event: MessageEvent, param: TestFixPPParam) {
        var mode = param.mode

        val sb = StringBuilder()

        var time = System.currentTimeMillis()

        val isOsuID = param.names.first().matches("\\d+".toRegex())

        event.reply("TF：正在按${if (isOsuID) " ID " else "玩家名"}的形式处理数据。")

        val ids = if (isOsuID) {
            param.names.mapNotNull { it.toLongOrNull() }
        } else {
            val actions = param.names.map {
                return@map AsyncMethodExecutor.Supplier<Pair<String, Long>?> {
                    return@Supplier try {
                        it to userApiService.getOsuID(it)
                    } catch (e: Exception) {
                        log.error("TF：获取玩家 {} 编号失败", it)
                        it to -1L
                    }
                }
            }

            val result = AsyncMethodExecutor.awaitSupplierExecute(actions)
                .filterNotNull()
                .filter { it.second > 0L }
                .toMap()

            param.names.mapNotNull { result[it] }
        }

        // 获取第一个玩家，来设定默认游戏模式
        if (mode == OsuMode.DEFAULT) {
            val firstUser = userApiService.getOsuUser(ids.first(), mode)
            mode = firstUser.currentOsuMode
        }

        val actions = ids.map {
            return@map AsyncMethodExecutor.Supplier<Pair<Long, TestFixPPData>?> {
                val user: OsuUser = try {
                    userApiService.getOsuUser(it, mode)
                } catch (e: Exception) {
                    log.error("TP：获取玩家 $it 信息失败")
                    return@Supplier it to TestFixPPData(OsuUser(it), listOf())
                }

                val bests: List<LazerScore> = try {
                    scoreApiService.getBestScores(user.userID, mode, 0, 200)
                } catch (e: Exception) {
                    log.error("TP：获取玩家 $it 最好成绩失败")
                    return@Supplier it to TestFixPPData(user, listOf())
                }

                it to TestFixPPData(user, bests)
            }
        }

        val data = AsyncMethodExecutor.awaitSupplierExecute(actions).filterNotNull().toMap()

        log.info("TP：获取玩家信息和最好成绩成功，耗时：${(System.currentTimeMillis() - time) / 1000} 秒")
        time = System.currentTimeMillis()

        val actions2 = data.values.flatMap { it.bests }.map {
            return@map AsyncMethodExecutor.Runnable {
                beatmapApiService.applyBeatMapExtendFromDataBase(it)
            }
        }

        AsyncMethodExecutor.awaitRunnableExecute(actions2, Duration.ofMinutes(max(5, ids.size / 5).toLong()))

        log.info("TP：玩家最好成绩添加谱面成功，耗时：${(System.currentTimeMillis() - time) / 1000} 秒")
        time = System.currentTimeMillis()

        ids.map { id ->
            val d = data[id]!!

            if (d.user == null) {
                sb.append("0, ")
            } else {
                val bests = d.bests

                for (s in bests) {
                    beatmapApiService.applyBeatMapExtendFromDataBase(s)

                    val max = s.beatmap.maxCombo ?: 1
                    val combo = s.maxCombo

                    val miss = s.statistics.miss

                    // 断连击，mania 模式不参与此项筛选
                    val isChoke = (miss == 0) && (combo < round(max * 0.98f)) && (s.mode != OsuMode.MANIA)

                    // 含有 <1% 的失误
                    val has1pMiss = (miss > 0) && ((1f * miss / max) <= 0.01f)

                    // 并列关系，miss 不一定 choke（断尾不会计入 choke），choke 不一定 miss（断滑条
                    if (isChoke || has1pMiss) {
                        s.pp = calculateApiService.getScoreFullComboPP(s).pp
                    }
                }

                val bpPP = bests.sumOf { it.weight?.pp ?: 0.0 }
                val fixed = bests.sortedByDescending{ it.pp }

                var weight = 1.0 / 0.95

                for (f in fixed) {
                    weight *= 0.95
                    f.weight = LazerScore.Weight(weight, (f.pp) * weight)
                }

                val fixedPP = fixed.sumOf { it.weight?.pp ?: 0.0 }
                val playerPP = d.user.pp

                val resultPP = playerPP - bpPP + fixedPP
                sb.append(resultPP.roundToInt()).append(", ")
            }
        }

        log.info("TP：修补最好成绩成功，耗时：${(System.currentTimeMillis() - time) / 1000} 秒")

        event.reply(sb.toString().removeSuffix(", "))


        /*

        for (name in names) {
            if (name.isBlank()) {
                continue
            }

            val user: OsuUser
            val bests: List<LazerScore>
            val playerPP: Double

            try {
                user = userApiService.getOsuUser(name, mode)
                playerPP = user.pp

                if (mode == OsuMode.DEFAULT) {
                    mode = user.currentOsuMode
                }

                bests = scoreApiService.getBestScores(user.userID, mode, 0, 100) +
                        scoreApiService.getBestScores(user.userID, mode, 100, 100)
            } catch (e: Exception) {
                sb.append("name=").append(name).append(" not found").append('\n')
                continue
            }

            if (bests.isEmpty()) {
                sb.append("name=").append(name).append(" bp is empty").append('\n')
            }

            for (s in bests) {
                beatmapApiService.applyBeatMapExtendFromDataBase(s)

                val max = s.beatmap.maxCombo ?: 1
                val combo = s.maxCombo

                val miss = s.statistics.miss

                // 断连击，mania 模式不参与此项筛选
                val isChoke = (miss == 0) && (combo < round(max * 0.98f)) && (s.mode != OsuMode.MANIA)

                // 含有 <1% 的失误
                val has1pMiss = (miss > 0) && ((1f * miss / max) <= 0.01f)

                // 并列关系，miss 不一定 choke（断尾不会计入 choke），choke 不一定 miss（断滑条
                if (isChoke || has1pMiss) {
                    s.PP = calculateApiService.getScoreFullComboPP(s).pp
                }
            }

            val bpPP = bests.sumOf { it.weight?.PP ?: 0.0 }
            val fixed = bests.sortedByDescending{ it.PP ?: 0.0 }

            var weight = 1.0 / 0.95

            for (f in fixed) {
                weight *= 0.95
                f.weight = LazerScore.Weight(weight, (f.PP ?: 0.0) * weight)
            }

            val fixedPP = fixed.sumOf { it.weight?.PP ?: 0.0 }

            val resultPP = playerPP - bpPP + fixedPP
            sb.append(resultPP.roundToInt()).append(", ")
        }

        event.reply(sb.toString().removeSuffix(", "))

         */
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TestFixPPService::class.java)
    }
}
