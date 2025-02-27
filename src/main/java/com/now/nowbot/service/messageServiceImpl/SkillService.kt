package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.beatmapParse.OsuFile
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.model.mapminus.PPMinus4
import com.now.nowbot.model.mapminus.PPMinus4Mania
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.CmdObject
import com.now.nowbot.util.CmdUtil
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

@Service("SKILL") class SkillService(
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService
) : MessageService<SkillService.SkillParam> {
    data class SkillParam(val user: OsuUser, val mode: OsuMode, val isMyself: Boolean = true)

    data class SkillScore(val score: LazerScore, val skill: List<Float>)

    override fun isHandle(
        event: MessageEvent, messageText: String, data: MessageService.DataValue<SkillParam>
    ): Boolean {
        val matcher = Instruction.SKILL.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        val isMyself = AtomicBoolean(true)
        val mode = OsuMode.MANIA // CmdUtil.getMode(matcher)
        val user = CmdUtil.getUserWithoutRange(event, matcher, CmdObject(mode), isMyself) //mode

        data.value = SkillParam(user, mode, isMyself.get())
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: SkillParam) {
        val bests = try {
            scoreApiService.getBestScores(param.user, param.mode)
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_Score)
        }

        calculateApiService.applyBeatMapChanges(bests)
        calculateApiService.applyStarToScores(bests)

        val fileMap = getOsuFileMap(bests, beatmapApiService)
        val skillMap = getSkill(fileMap)

        val body = getBody(param.user, bests, skillMap)

        val image = try {
            imageService.getPanel(body, "K")
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_RenderTooMany, "技巧分析")
        }

        try {
            event.reply(image)
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "技巧分析")
        }
    }

    private fun getOsuFileMap(bests: List<LazerScore>?, beatmapApiService: OsuBeatmapApiService): Map<Long, OsuFile?> {
        if (bests.isNullOrEmpty()) return mapOf()

        val files = ConcurrentHashMap<Long, OsuFile?>(bests.size)

        val actions = bests.map {
            return@map AsyncMethodExecutor.Supplier<Unit> {
                try {
                    files[it.beatMapID] = OsuFile.getInstance(beatmapApiService.getBeatMapFileString(it.beatMapID))
                } catch (_: Exception) {

                }
            }
        }

        AsyncMethodExecutor.AsyncSupplier(actions)

        return files
    }

    private fun getSkill(fileMap: Map<Long, OsuFile?>): Map<Long, PPMinus4?> {
        val skillMap = ConcurrentHashMap<Long, PPMinus4?>(fileMap.size)

        val actions = fileMap.map {
            return@map AsyncMethodExecutor.Supplier<Unit> {
                val file = it.value

                skillMap[it.key] = if (file?.mode == OsuMode.MANIA) {
                    PPMinus4Mania(file.mania)
                } else {
                    null
                }
            }
        }

        AsyncMethodExecutor.AsyncSupplier(actions)

        return skillMap
    }

    private fun getBody(user: OsuUser, bests: List<LazerScore>, skillMap: Map<Long, PPMinus4?>): Map<String, Any> {
        val sum = MutableList(8) {0.0}

        for (i in bests.indices) {
            val percent: Double = (0.95).pow(i)
            val b = bests[i]

            val skills = skillMap[b.beatMapID]?.values ?: listOf()

            for (j in 0..< min(sum.size, skills.size)) {
                sum[j] += (skills[j] * nerfByAccuracy(b) * percent)
            }
        }

        val skill = sum.map { it / DIVISOR }.toList()

        val scores: List<SkillScore> = bests
            .map { SkillScore(it, skillMap[it.beatMapID]?.values ?: listOf()) }

        val total = run {
            var star = 0.0
            val powers = listOf(0.8, 0.8, 0.8, 0.4, 0.6, 1.2)

            for (i in 0 ..< 6) {
                star += powers[i] * skill[i]
            }

            star / 3.0
        }

        return mapOf(
            "user" to user,
            "skill" to skill,
            "scores" to scores,
            "total" to total,
            "size" to skill.size
        )
    }

    companion object {
        // 用于求和并归一化
        private val DIVISOR = (1 - (0.95).pow(100)) / 0.05 // 19.88158941559331949

        private fun nerfByAccuracy(score: LazerScore): Double {
            return when(score.mode) {
                OsuMode.MANIA -> when(score.accuracy) {
                    in 0.0..< 0.85 -> 0.0
                    in 0.85..< 1.0 -> sqrt((score.accuracy - 0.8) / 0.2)

                    else -> 1.0
                }

                else -> when(score.accuracy) {
                    in 0.0..< 0.85 -> 0.0
                    in 0.85..< 1.0 -> sqrt((score.accuracy - 0.8) / 0.2)

                    else -> 1.0
                }
            }
        }
    }
}