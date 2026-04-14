package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.beatmapParse.OsuFile
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.skill.Skill6
import com.now.nowbot.model.skill.getDanFromBests
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
import com.now.nowbot.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.regex.Matcher
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

    data class SkillScore(
        val score: LazerScore,
        val skill: List<Double>,

        @get:JsonProperty("skill_sum")
        val skillSum: Double
    )

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

        return if (param.other != null) {
            ServiceCallStatistic.builds(event,
                userIDs = listOf(param.me.userID, param.other.userID),
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

        val id1 = ids.first
        val id2 = ids.second

        if (id1 != null && id2 != null) {
            // 双人模式 (无论 isVs 是真或假，双人逻辑完全一致)
            mode = inputMode.data!!

            val async = AsyncMethodExecutor.awaitQuad(
                { userApiService.getOsuUser(id1, mode) },
                { scoreApiService.getBestScoresSerial(id1, mode) },
                { userApiService.getOsuUser(id2, mode) },
                { scoreApiService.getBestScoresSerial(id2, mode) }
            )

            me = async.first.first
            myBests = async.first.second
            other = async.second.first
            otherBests = async.second.second

        } else if (!isVs && id1 != null) {
            // 单人模式 (仅当不是 isVs 且只提供了第一个 id 时)
            mode = inputMode.data!!

            val async = AsyncMethodExecutor.awaitPair(
                { userApiService.getOsuUser(id1, mode) },
                { scoreApiService.getBestScores(id1, mode) }
            )

            me = async.first
            myBests = async.second
            other = null
            otherBests = null

        } else {
            // 缺东西，走常规路线 (合并两种情况，直接传入 isVs 变量)
            val users = InstructionUtil.get2User(event, matcher, inputMode, isVs)

            me = users.first()
            other = users.getOrNull(1)

            mode = OsuMode.getMode(inputMode.data!!, me.currentOsuMode)

            myBests = scoreApiService.getBestScores(me.userID, mode)
            otherBests = other?.let { scoreApiService.getBestScores(it.userID, mode) }
        }

        return SkillParam(isVs, me, myBests, other, otherBests, mode)
    }

    private fun SkillParam.getImage(): ByteArray {
        val hasOthers = other != null

        val my: Map<Long, Skill6?>
        val others: Map<Long, Skill6?>?

        if (hasOthers) {
            val async = AsyncMethodExecutor.awaitPair(
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

    private fun getSkillMap(bests: List<LazerScore>?): Map<Long, Skill6?> {
        if (bests.isNullOrEmpty()) return mapOf()

        val scoreMap = bests.associateBy { it.beatmapID }

        val ids = beatmapApiService.downloadBeatmapFile(scoreMap.keys)

        val fileMap = ids.associateWith { id -> beatmapApiService.getBeatmapFileString(id)}

        // 4. 统一解析 Skill (CPU密集型操作)
        // 注意：这里不需要用 !!，没下载到的图直接标记为 null 即可
        return bests.associate { score ->
            val fileString = fileMap[score.beatmapID]
            val skill = if (fileString != null) {
                try {
                    Skill6(
                        OsuFile(fileString),
                        OsuMode.MANIA,
                        LazerMod.getModSpeedForStarCalculate(score.mods).toDouble()
                    )
                } catch (e: Exception) {
                    log.error("解析谱面 ${score.beatmapID} 异常", e)
                    null
                }
            } else {
                null // 下载失败或本地没有的图
            }

            score.beatmapID to skill
        }
    }

    private fun getBody(
        user: OsuUser,
        bests: List<LazerScore>,
        skillMap: Map<Long, Skill6?>,
        isMyself: Boolean = false,
        isShowScores: Boolean = true,
    ): Map<String, Any> {
        val weightedMap = bests.mapNotNull { b ->
            val skills = skillMap[b.beatmapID]?.skills

            skills?.let {
                val nerf = nerfByAccuracy(b)

                b.beatmapID to skills.map { it * nerf }
            }
        }.toMap()

        val weightedSkills = weightedMap.values

        val skills = SkillUtil.collectScoreSkills(weightedSkills.take(100))

        val scores: List<SkillScore> = if (isShowScores) {
            val s10 = bests.take(10)

            // scoreApiService.asyncDownloadBackgroundFromScores(s10, CoverType.LIST)

            BeatmapUtil.applyBeatmapChanges(s10)
            calculateApiService.applyStarToScores(s10)

            s10.map {
                val skills = skillMap[it.beatmapID]?.skills ?: List(6) { 0.0 }

                val scoreRating = SkillUtil.getMapSkillRating(skills)

                SkillScore(it, skills, scoreRating) }
        } else {
            listOf()
        }

        val userRating = SkillUtil.getMapSkillRating(skills)

        val dan = getDanFromBests(weightedMap, bests)

        return if (isMyself) mapOf(
            "user" to user,
            "skill" to skills,
            "abbreviates" to Skill6.getAbbr(user.currentOsuMode),
            "scores" to scores,
            "total" to userRating,
            "dan" to dan,
        ) else mapOf(
            "vs_user" to user,
            "vs_skill" to skills,
            "vs_abbreviates" to Skill6.getAbbr(user.currentOsuMode),
            "vs_scores" to scores,
            "vs_total" to userRating,
            "vs_dan" to dan,
        )
    }

    companion object {

        private val log: Logger = LoggerFactory.getLogger(SkillService::class.java)

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
