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
import java.util.regex.Matcher
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

        val param = getParam(event, m, m2)

        data.value = param ?: return false
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: SkillParam) {
        val image = param.getImage()

        try {
            event.reply(image)
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "技巧分析")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): SkillParam? {
        val m = OfficialInstruction.SKILL.matcher(messageText)
        val m2 = OfficialInstruction.SKILL_VS.matcher(messageText)

        return getParam(event, m, m2)
    }

    override fun reply(event: MessageEvent, param: SkillParam): MessageChain? {
        return QQMsgUtil.getImage(param.getImage())
    }

    private fun getParam(event: MessageEvent, m: Matcher, m2: Matcher): SkillParam? {
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

    private fun SkillParam.getImage(): ByteArray {
        val vs: Map<String, Any>?
        val me: Map<String, Any>

        if (this.vs != null) {
            val bests = try {
                scoreApiService.getBestScores(this.user, this.mode)
            } catch (e: Exception) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_BP, this.user.username)
            }

            val skillMap = getSkillMap(bests, beatmapApiService)

            me = getBody(this.user, bests, skillMap, isVS = false, isShowScores = false)

            val vsBests = try {
                scoreApiService.getBestScores(this.vs, this.mode)
            } catch (e: Exception) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_BP, this.vs.username)
            }

            val vsSkillMap = getSkillMap(vsBests, beatmapApiService)

            vs = getBody(this.vs, vsBests, vsSkillMap, isVS = true, isShowScores = false)
        } else {
            val bests = try {
                scoreApiService.getBestScores(this.user, this.mode)
            } catch (e: Exception) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_BP, this.user.username)
            }

            val skillMap = getSkillMap(bests, beatmapApiService)

            me = getBody(this.user, bests, skillMap, isVS = false, isShowScores = true)
            vs = null
        }

        val image = try {
            if (vs != null) {
                imageService.getPanel(vs + me + mapOf("panel" to "KV"), "K")
            } else {
                imageService.getPanel(me + mapOf("panel" to "K"), "K")
            }
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_RenderTooMany, "技巧分析")
        }

        return image
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
        isVS: Boolean = false,
        isShowScores: Boolean = true,
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

        val scores: List<SkillScore> = if (isShowScores) {
            val s10 = bests.take(10)

            calculateApiService.applyBeatMapChanges(s10)
            calculateApiService.applyStarToScores(s10)

            s10.map { SkillScore(it, skillMap[it.beatMapID]?.values ?: listOf()) }
        } else {
            listOf()
        }

        val kSort = skill.take(6).sortedDescending()
        val total = kSort[0] * 0.5f + kSort[1] * 0.3f + kSort[2] * 0.2f + kSort[3] * 0.1f + kSort[4] * 0.05f

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
