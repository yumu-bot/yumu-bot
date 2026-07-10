package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.Discussion
import com.now.nowbot.model.osu.Discussion.Companion.toppingUnsolvedDiscussionDetails
import com.now.nowbot.model.osu.DiscussionDetails
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.NominationService.NominationParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuDiscussionApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService

import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.regex.Matcher
import kotlin.math.floor

@Service("NOMINATION") class NominationService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val userApiService: OsuUserApiService,
    private val discussionApiService: OsuDiscussionApiService,
    private val imageService: ImageService,
    private val dao: ServiceCallStatisticsDao,
) : MessageService<NominationParam>, TencentMessageService<NominationParam> {

    data class NominationParam(
        val beatmapset: Beatmapset,
        val discussions: List<DiscussionDetails>,
        val hype: List<DiscussionDetails>,
        val more: Map<String, Any>,
        val users: List<OsuUser>
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "beatmapset" to beatmapset,
                "discussion" to discussions,
                "hype" to hype,
                "more" to more,
                "users" to users,
            )
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<NominationParam>,
    ): Boolean {
        val matcher = Instruction.NOMINATION.matcher(messageText)
        if (!matcher.find()) return false

        val param = getNominationParam(event, matcher)

        data.value = param
        return true
    }

    override fun handleMessage(event: MessageEvent, param: NominationParam): ServiceCallStatistic? {
        val image: ByteArray = imageService.getPanel(param.toMap(), "N")

        try {
            event.replyAsync(image)
        } catch (e: Exception) {
            log.error("提名信息：发送失败", e)
            throw IllegalStateException.Send("提名信息")
        }

        return ServiceCallStatistic.builds(event,
            beatmapIDs = param.beatmapset.beatmaps?.map { it.beatmapID }.orEmpty(),
            beatmapsetIDs = listOf(param.beatmapset.beatmapsetID),
            userIDs = param.users.map { it.userID },
            modes = param.beatmapset.beatmaps?.map { it.mode }?.distinct()
        )
    }

    override fun accept(event: MessageEvent, messageText: String): NominationParam? {
        val matcher = OfficialInstruction.NOMINATION.matcher(messageText)
        if (!matcher.find()) return null

        val param = getNominationParam(event, matcher)

        return param
    }

    override fun reply(event: MessageEvent, param: NominationParam): MessageChain? {
        val image: ByteArray =  imageService.getPanel(param.toMap(), "N")

        return MessageChainBuilder().addImage(image).build()
    }

    private fun getNominationParam(event: MessageEvent,
        matcher: Matcher,
    ): NominationParam {
        val (beatmapset, _) = beatmapApiService.getBeatmapsetAndTopBeatmapFromAnyID(matcher) { dao.getLastBeatmapID(event) }

        val discussion = runCatching {
            discussionApiService.getBeatmapsetDiscussion(beatmapset.beatmapsetID)
        }.onFailure { e ->
            log.error("提名信息：讨论区获取失败", e)
            throw IllegalStateException.Fetch("讨论区")
        }.getOrThrow()

        beatmapset.creatorData?.let {
            try {
                beatmapset.creatorData = userApiService.getOsuUser(it.userID, it.currentOsuMode)
            } catch (_: Exception) {

            }
        }

        beatmapset.beatmaps!!.forEach { beatmapApiService.extendBeatmapTag(it) }

        val diffs = beatmapset.beatmaps!!.associate { it.beatmapID to it.difficultyName }
        discussion.addDifficulty4DiscussionDetails(diffs)

        return getParam(
            beatmapset,
            discussion,
        )
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(NominationService::class.java)

        fun getParam(
            beatmapset: Beatmapset,
            discussion: Discussion,
        ): NominationParam {
            val details: List<DiscussionDetails>
            val discussions: List<DiscussionDetails>
            val hypes: List<DiscussionDetails>
            val more: Map<String, Any>

            // 获取 hypes 和 discussions 列表
            // 这两个list需要合并起来
            run {
                details = (discussion.discussions + discussion.includedDiscussions).distinct()
                hypes = details.filter { i: DiscussionDetails ->
                    i.messageType.isComment()
                }

                val dis = details.filter { i: DiscussionDetails ->
                    i.messageType.isCheck()
                }

                discussions = dis.toppingUnsolvedDiscussionDetails()
            }

            // 这一部分提供额外信息
            run {
                var problemCount = 0
                var suggestCount = 0
                var notSolvedCount = 0
                var hypeCount = 0
                var praiseCount = 0

                for (i in details) {
                    when (i.messageType) {
                        DiscussionDetails.DiscussionType.PROBLEM -> problemCount++
                        DiscussionDetails.DiscussionType.SUGGESTION -> suggestCount++
                        DiscussionDetails.DiscussionType.HYPE -> hypeCount++
                        DiscussionDetails.DiscussionType.PRAISE -> praiseCount++
                        else -> {}
                    }

                    if (i.canBeResolved && !i.resolved) {
                        notSolvedCount++
                    }
                }

                val bs = beatmapset.beatmaps.orEmpty()

                val stars = bs.map { it.starRating }

                val maxStar = stars.max()
                val minStar = stars.min()
                val totalLength = bs.sumOf { it.totalLength }

                val hostCount = bs.count { it.mapperIDs.contains(beatmapset.creatorID) }
                val guestCount = bs.size - hostCount

                val maxSR = floor(maxStar).toString() + if (maxStar - floor(maxStar) >= 0.5) "+" else ""
                val minSR = if (bs.size <= 1) "" else floor(minStar).toString()

                // 其他
                val tags = beatmapset.tags.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                more = mapOf(
                    "host_count" to hostCount,
                    "guest_count" to guestCount,
                    "total_count" to (hostCount + guestCount),
                    "max_star" to maxSR,
                    "min_star" to minSR,
                    "stars" to stars.sortedDescending(),
                    "total_length" to totalLength,
                    "tags" to tags,
                    "problem_count" to problemCount,
                    "suggest_count" to suggestCount,
                    "not_solved_count" to notSolvedCount,
                    "hype_count" to hypeCount,
                    "praise_count" to praiseCount,
                )
            }

            return NominationParam(
                beatmapset, discussions, hypes, more, discussion.users
            )
        }
    }
}
