package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.beatmapParse.OsuFile
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.model.mapminus.PPMinus4
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.*
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow
import kotlin.math.sqrt

@Service("SKILL") class SkillService(
    private val scoreApiService: OsuScoreApiService,
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val bindDao: BindDao,
    private val imageService: ImageService
) : MessageService<SkillService.SkillParam>, TencentMessageService<SkillService.SkillParam> {
    data class SkillParam(val user: OsuUser, val vs: OsuUser?, val mode: OsuMode)

    data class SkillScore(val score: LazerScore, val skill: List<Float>)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<SkillParam>
    ): Boolean {
        val m = Instruction.SKILL.matcher(messageText)
        val m2 = Instruction.SKILL_VS.matcher(messageText)

        val isVs: Boolean = if (m.find()) {
            false
        } else if (m2.find()) {
            true
        } else return false

        val mode = OsuMode.MANIA

        val me = try {
            bindDao.getBindFromQQ(event.sender.id)
        } catch (e: Exception) {
            null
        }

        val user: OsuUser
        val vs: OsuUser?

        if (isVs) {
            if (m2.group("vs").isNullOrBlank().not()) {
                user = CmdUtil.getUserWithoutRange(event, m2, CmdObject(mode))
                vs = CmdUtil.getOsuUser(m2.group("vs"), mode)
            } else {
                val maybe = CmdUtil.getUserWithoutRange(event, m2, CmdObject(mode))
                if (me == null || maybe.id == me.osuID) {
                    user = maybe
                    vs = null
                } else {
                    user = userApiService.getPlayerInfo(me, mode)
                    vs = maybe
                }
            }
        } else {
            vs = if (m.group("vs").isNullOrBlank().not()) {
                CmdUtil.getOsuUser(m.group("vs"), mode)
            } else {
                null
            }
            user = CmdUtil.getUserWithoutRange(event, m, CmdObject(mode))
        }

        data.value = SkillParam(user, vs, mode)
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: SkillParam) {
        val vs = if (param.vs != null) {
            val bests = try {
                scoreApiService.getBestScores(param.vs, param.mode)
            } catch (e: Exception) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_BP, param.vs.username)
            }

            calculateApiService.applyBeatMapChanges(bests)
            calculateApiService.applyStarToScores(bests)

            val skillMap = getSkillMap(bests, beatmapApiService)

            getBody(param.vs, bests, skillMap, true)
        } else null

        val bests = try {
            scoreApiService.getBestScores(param.user, param.mode)
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_BP, param.user.username)
        }

        calculateApiService.applyBeatMapChanges(bests)
        calculateApiService.applyStarToScores(bests)

        val skillMap = getSkillMap(bests, beatmapApiService)

        val body = getBody(param.user, bests, skillMap)

        val image = try {
            if (vs != null) {
                imageService.getPanel(vs + body + mapOf("panel" to "KV"), "K")
            } else {
                imageService.getPanel(body + mapOf("panel" to "K"), "K")
            }
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_RenderTooMany, "技巧分析")
        }

        try {
            event.reply(image)
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "技巧分析")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): SkillParam? {
        val m = OfficialInstruction.SKILL.matcher(messageText)
        val m2 = OfficialInstruction.SKILL_VS.matcher(messageText)

        val isVs: Boolean = if (m.find()) {
            false
        } else if (m2.find()) {
            true
        } else return null

        val mode = OsuMode.MANIA

        val me = try {
            bindDao.getBindFromQQ(event.sender.id)
        } catch (e: Exception) {
            null
        }

        val user: OsuUser
        val vs: OsuUser?
        if (isVs) {
            if (m2.group("vs").isNullOrBlank().not()) {
                user = CmdUtil.getUserWithoutRange(event, m2, CmdObject(mode))
                vs = CmdUtil.getOsuUser(m2.group("vs"), mode)
            } else {
                val maybe = CmdUtil.getUserWithoutRange(event, m2, CmdObject(mode))
                if (me == null || maybe.id == me.osuID) {
                    user = maybe
                    vs = null
                } else {
                    user = userApiService.getPlayerInfo(me, mode)
                    vs = maybe
                }
            }
        } else {
            vs = if (m.group("vs").isNullOrBlank().not()) {
                CmdUtil.getOsuUser(m.group("vs"), mode)
            } else {
                null
            }
            user = CmdUtil.getUserWithoutRange(event, m, CmdObject(mode))
        }

        return SkillParam(user, vs, mode)
    }

    override fun reply(event: MessageEvent, param: SkillParam): MessageChain? {
        val vs = if (param.vs != null) {
            val bests = try {
                scoreApiService.getBestScores(param.vs, param.mode)
            } catch (e: Exception) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_BP, param.vs.username)
            }

            calculateApiService.applyBeatMapChanges(bests)
            calculateApiService.applyStarToScores(bests)

            val skillMap = getSkillMap(bests, beatmapApiService)

            getBody(param.vs, bests, skillMap, true)
        } else null

        val bests = try {
            scoreApiService.getBestScores(param.user, param.mode)
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_BP, param.user.username)
        }

        calculateApiService.applyBeatMapChanges(bests)
        calculateApiService.applyStarToScores(bests)

        val skillMap = getSkillMap(bests, beatmapApiService)

        val body = getBody(param.user, bests, skillMap)

        val image = try {
            if (vs != null) {
                imageService.getPanel(vs + body, "KV")
            } else {
                imageService.getPanel(body, "K")
            }
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_RenderTooMany, "技巧分析")
        }

        return QQMsgUtil.getImage(image)
    }

    private fun getSkillMap(bests: List<LazerScore>?, beatmapApiService: OsuBeatmapApiService): Map<Long, PPMinus4?> {
        if (bests.isNullOrEmpty()) return mapOf()

        val skillMap = ConcurrentHashMap<Long, PPMinus4?>(bests.size)

        val actions = bests.map {
            return@map AsyncMethodExecutor.Supplier<Unit> {
                try {
                    val file = OsuFile.getInstance(beatmapApiService.getBeatMapFileString(it.beatMapID))

                    skillMap[it.beatMapID] = PPMinus4.getInstance(
                        file,
                        OsuMode.MANIA,
                        LazerMod.getModSpeedForStarCalculate(it.mods).toDouble()
                    )
                } catch (_: Exception) {

                }
            }
        }

        AsyncMethodExecutor.AsyncSupplier(actions)

        return skillMap
    }

    private fun getBody(
        user: OsuUser,
        bests: List<LazerScore>,
        skillMap: Map<Long, PPMinus4?>,
        isVS: Boolean = false
    ): Map<String, Any> {
        val skills = List(8) { mutableListOf<Double>() }

        bests.forEach {
            val values = skillMap[it.beatMapID]?.values ?: listOf()

            for (i in values.indices) {
                skills[i].add(values[i] * nerfByAccuracy(it))
            }
        }

        val skill = skills.map {
            it.sortedDescending().mapIndexed { i, v ->
                val percent: Double = (0.95).pow(i)
                v * percent
            }.sum() / DIVISOR
        }

        val scores: List<SkillScore> = bests.map { SkillScore(it, skillMap[it.beatMapID]?.values ?: listOf()) }

        val k = skill.take(6).sortedDescending()
        val total = k[0] * 0.5f + k[1] * 0.3f + k[2] * 0.2f + k[3] * 0.1f + k[4] * 0.05f

        /*

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

            star / STAR_DIVISOR
        }

         */

        return if (isVS) mapOf(
            "vs_user" to user,
            "vs_skill" to skill,
            "vs_scores" to scores,
            "vs_total" to total,
        ) else mapOf(
            "user" to user,
            "skill" to skill,
            "scores" to scores,
            "total" to total,
        )
    }

    companion object { // 用于控制最后的星数
        // private const val STAR_DIVISOR = 3.6

        // 用于求和并归一化
        private const val DIVISOR = 16.0 // (1 - (0.95).pow(100)) / 0.05 // 19.88158941559331949

        private fun nerfByAccuracy(score: LazerScore): Double {
            return when (score.mode) {
                OsuMode.MANIA -> when (score.accuracy) {
                    in 0.0..<0.85 -> 0.0
                    in 0.85..<1.0 -> sqrt((score.accuracy - 0.8) / 0.2)

                    else -> 1.0
                }

                else -> when (score.accuracy) {
                    in 0.0..<0.85 -> 0.0
                    in 0.85..<1.0 -> sqrt((score.accuracy - 0.8) / 0.2)

                    else -> 1.0
                }
            }
        }
    }
}
