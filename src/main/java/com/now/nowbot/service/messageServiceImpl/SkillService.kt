package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.beatmapParse.OsuFile
import com.now.nowbot.model.osu.Covers.Companion.CoverType
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.skill.Skill
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.*
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.regex.Matcher
import kotlin.math.pow
import kotlin.math.sqrt

@Service("SKILL") class SkillService(
    private val scoreApiService: OsuScoreApiService,
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService
) : MessageService<SkillService.SkillParam>, TencentMessageService<SkillService.SkillParam> {
    data class SkillParam(
        val isVs: Boolean,
        val me: OsuUser,
        val myBests: List<LazerScore>,
        val other: OsuUser?,
        val otherBests: List<LazerScore>?,
        val mode: OsuMode
    )

    data class SkillScore(val score: LazerScore, val skill: List<Float>)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<SkillParam>
    ): Boolean {
        val m = Instruction.SKILL.matcher(messageText)
        val m2 = Instruction.SKILL_VS.matcher(messageText)

        val matcher: Matcher
        val isVs: Boolean

        if (m.find()) {
            matcher = m
            isVs = false
        } else if (m2.find()) {
            matcher = m2
            isVs = true
        } else return false

        data.value = getParam(event, matcher, isVs)
        return true
    }

    override fun handleMessage(event: MessageEvent, param: SkillParam): ServiceCallStatistic? {
        val image = param.getImage()

        try {
            event.reply(image)
        } catch (_: Exception) {
            throw IllegalStateException.Send("技巧分析")
        }

        return if (param.isVs) {
            ServiceCallStatistic.builds(event,
                userIDs = listOf(param.me.userID, param.other?.userID ?: throw NoSuchElementException.Player()),
                modes = listOf(param.mode)
            )
        } else {
            ServiceCallStatistic.build(event,
                userID = param.me.userID,
                mode = param.mode
            )
        }
    }

    override fun accept(event: MessageEvent, messageText: String): SkillParam? {
        val m = OfficialInstruction.SKILL.matcher(messageText)
        val m2 = OfficialInstruction.SKILL_VS.matcher(messageText)

        val matcher: Matcher
        val isVs: Boolean

        if (m.find()) {
            matcher = m
            isVs = false
        } else if (m2.find()) {
            matcher = m2
            isVs = true
        } else return null

        return getParam(event, matcher, isVs)
    }

    override fun reply(event: MessageEvent, param: SkillParam): MessageChain? {
        return MessageChain(param.getImage())
    }

    private fun getParam(event: MessageEvent, matcher: Matcher, isVs: Boolean = false): SkillParam {
        val inputMode = InstructionObject(OsuMode.MANIA)// getMode(matcher)

        val ids = UserIDUtil.get2UserID(event, matcher, inputMode, isVs)

        val me: OsuUser
        val other: OsuUser?
        val myBests: List<LazerScore>
        val otherBests: List<LazerScore>?
        val mode: OsuMode

        if (isVs) {
            if (ids.first != null && ids.second != null) {
                // 双人模式

                mode = inputMode.data!!

                val async = AsyncMethodExecutor.awaitQuadCallableExecute(
                    { userApiService.getOsuUser(ids.first!!, mode) },
                    { scoreApiService.getBestScores(ids.first!!, mode, 0, 100) },
                    { userApiService.getOsuUser(ids.second!!, mode) },
                    { scoreApiService.getBestScores(ids.second!!, mode, 0, 100) },
                )

                me = async.first.first
                other = async.second.first

                myBests = async.first.second
                otherBests = async.second.second
            } else {
                // 缺东西，走常规路线
                val users = InstructionUtil.get2User(event, matcher, inputMode, true)

                mode = OsuMode.getMode(inputMode.data!!, users.first().currentOsuMode)

                me = users.first()
                other = if (users.size == 2) users.last() else null

                myBests = scoreApiService.getBestScores(me.userID, mode, 0, 100)
                otherBests = if (other != null) scoreApiService.getBestScores(other.userID, mode, 0, 100) else null
            }
        } else {
            if (ids.first != null && ids.second != null) {
                // 双人模式

                mode = inputMode.data!!

                val async = AsyncMethodExecutor.awaitQuadCallableExecute(
                    { userApiService.getOsuUser(ids.first!!, mode) },
                    { scoreApiService.getBestScores(ids.first!!, mode, 0, 100) },
                    { userApiService.getOsuUser(ids.second!!, mode) },
                    { scoreApiService.getBestScores(ids.second!!, mode, 0, 100) },
                )

                me = async.first.first
                other = async.second.first

                myBests = async.first.second
                otherBests = async.second.second

            } else if (ids.first != null) {
                // 单人模式

                mode = inputMode.data!!

                val async = AsyncMethodExecutor.awaitPairCallableExecute(
                    { userApiService.getOsuUser(ids.first!!, mode) },
                    { scoreApiService.getBestScores(ids.first!!, mode, 0, 100) },
                )

                me = async.first
                other = null

                myBests = async.second
                otherBests = null

            } else {
                // 缺东西，走常规路线

                val users = InstructionUtil.get2User(event, matcher, inputMode, false)

                mode = OsuMode.getMode(inputMode.data!!, users.first().currentOsuMode)

                me = users.first()
                other = if (users.size == 2) users.last() else null

                myBests = scoreApiService.getBestScores(me.userID, mode, 0, 100)
                otherBests = if (other != null) scoreApiService.getBestScores(other.userID, mode, 0, 100) else null
            }
        }

        return SkillParam(isVs, me, myBests, other, otherBests, mode)
    }

    private fun SkillParam.getImage(): ByteArray {
        val hasOthers = other != null

        val my: Map<Long, Skill?>
        val others: Map<Long, Skill?>?

        if (hasOthers) {
            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { getSkillMap(myBests) },
                { getSkillMap(otherBests) }
            )

            my = async.first
            others = async.second
        } else {
            my = getSkillMap(myBests)
            others = null
        }

        val myBody = getBody(me, myBests, my, isMyself = true, !hasOthers)
        val othersBody = if (hasOthers) {
            getBody(other, otherBests!!, others!!, isMyself = false, isShowScores = false)
        } else null

        val image =
            if (hasOthers) {
                imageService.getPanel(myBody + othersBody!! + mapOf("panel" to "KV"), "K")
            } else {
                imageService.getPanel(myBody + mapOf("panel" to "K"), "K")
            }

        return image
    }

    private fun getSkillMap(bests: List<LazerScore>?): Map<Long, Skill?> {
        if (bests.isNullOrEmpty()) return mapOf()

        val actions = bests.map {
            Callable {
                it to beatmapApiService.getBeatmapFileString(it.beatmapID)
            }
        }

        val files: List<Pair<LazerScore, String?>> = AsyncMethodExecutor.awaitCallableExecute(actions)

        val actions2 = files.map {
            val id = it.first.beatmapID

            Callable {
                try {
                    val file = OsuFile.getInstance(it.second)

                    id to Skill.getInstance(
                        file,
                        OsuMode.MANIA,
                        LazerMod.getModSpeedForStarCalculate(it.first.mods).toDouble()
                    )
                } catch (_: Exception) {
                    id to null
                }
            }
        }

        val result = AsyncMethodExecutor.awaitCallableExecute(actions2).toMap()

        return bests.associate { it.beatmapID to result[it.beatmapID] }
    }

    private fun getBody(
        user: OsuUser,
        bests: List<LazerScore>,
        skillMap: Map<Long, Skill?>,
        isMyself: Boolean = false,
        isShowScores: Boolean = true,
    ): Map<String, Any> {
        val skills = List(8) { mutableListOf<Double>() }

        bests.forEach {
            val values = skillMap[it.beatmapID]?.values ?: listOf()

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

            scoreApiService.asyncDownloadBackgroundFromScores(s10, CoverType.LIST)

            calculateApiService.applyBeatmapChanges(s10)
            calculateApiService.applyStarToScores(s10)

            s10.map { SkillScore(it, skillMap[it.beatmapID]?.values ?: listOf()) }
        } else {
            listOf()
        }

        val kSort = skill.take(6).sortedDescending()
        val total = kSort[0] * 0.5f + kSort[1] * 0.3f + kSort[2] * 0.2f + kSort[3] * 0.1f + kSort[4] * 0.05f

        return if (isMyself) mapOf(
            "user" to user,
            "skill" to skill,
            "scores" to scores,
            "total" to total,
        ) else mapOf(
            "vs_user" to user,
            "vs_skill" to skill,
            "vs_scores" to scores,
            "vs_total" to total,
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
