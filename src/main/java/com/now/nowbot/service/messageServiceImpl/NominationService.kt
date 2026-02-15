package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.Discussion
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

import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.command.FLAG_SID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.regex.Matcher
import kotlin.math.floor

@Service("NOMINATION") class NominationService(
    private val osuBeatmapApiService: OsuBeatmapApiService,
    private val osuUserApiService: OsuUserApiService,
    private val osuDiscussionApiService: OsuDiscussionApiService,
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
            event.reply(image)
        } catch (e: Exception) {
            log.error("提名信息：发送失败", e)
            throw IllegalStateException.Send("提名信息")
        }

        return ServiceCallStatistic.builds(event,
            beatmapIDs = param.beatmapset.beatmaps?.map { it.beatmapID },
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
        val idStr: String? = matcher.group(FLAG_SID)
        val mode = matcher.group("mode")
        var isSID = !(mode != null && (mode == "b" || mode == "bid"))

        val id = idStr?.toLongOrNull()
            ?: run {
                val last = dao.getLastBeatmapID(
                    groupID = event.subject.contactID,
                    name = null,
                    from = LocalDateTime.now().minusHours(24L)
                )

                if (last != null) {
                    isSID = false
                }

                last ?: throw IllegalArgumentException.WrongException.Audio()
            }

        return getParam(
            id,
            isSID,
            osuBeatmapApiService,
            osuDiscussionApiService,
            osuUserApiService,
        )
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(NominationService::class.java)

        fun getParam(
            id: Long,
            assumeSID: Boolean,
            beatmapApiService: OsuBeatmapApiService,
            discussionApiService: OsuDiscussionApiService,
            userApiService: OsuUserApiService,
        ): NominationParam {
            var id = id
            var s: Beatmapset
            val d: Discussion
            val details: List<DiscussionDetails>
            val discussions: List<DiscussionDetails>
            val hypes: List<DiscussionDetails>
            val more: Map<String, Any>

            if (assumeSID) {
                try {
                    s = beatmapApiService.getBeatmapset(id)
                } catch (_: NetworkException.BeatmapException.NotFound) {
                    val b = beatmapApiService.getBeatmapFromDatabase(id)
                    id = b.beatmapsetID
                    s = beatmapApiService.getBeatmapset(id)
                }
            } else {
                val b = beatmapApiService.getBeatmapFromDatabase(id)
                id = b.beatmapsetID
                s = beatmapApiService.getBeatmapset(id)
            }

            s.creatorData?.let {
                try {
                    s.creatorData = userApiService.getOsuUser(it.userID, it.currentOsuMode)
                } catch (_: Exception) {

                }
            }

            try {
                d = discussionApiService.getBeatmapsetDiscussion(id)
            } catch (e: Exception) {
                log.error("提名信息：讨论区获取失败", e)
                throw IllegalStateException.Fetch("讨论区")
            }

            if (!s.beatmaps.isNullOrEmpty()) {
                // 插入标签
                s.beatmaps!!.forEach { beatmapApiService.extendBeatmapTag(it) }

                // 插入难度名
                val diffs = s.beatmaps!!.associate { it.beatmapID to it.difficultyName }

                d.addDifficulty4DiscussionDetails(diffs)
            }

            // 获取 hypes 和 discussions 列表
            // 这两个list需要合并起来
            run {
                details = (d.discussions + d.includedDiscussions).distinct()
                hypes = details.filter { i: DiscussionDetails ->
                    val t = i.messageType
                    t == "hype" || t == "praise"
                }

                val dis = details.filter { i: DiscussionDetails ->
                    val t = i.messageType
                    t == "problem" || t == "suggestion"
                }
                discussions = Discussion.toppingUnsolvedDiscussionDetails(dis)
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
                        "problem" -> problemCount++
                        "suggestion" -> suggestCount++
                        "hype" -> hypeCount++
                        "praise" -> praiseCount++
                        else -> {}
                    }

                    if (i.canBeResolved && !i.resolved) {
                        notSolvedCount++
                    }
                }

                val bs = s.beatmaps ?: listOf()

                val stars = bs.map { it.starRating }

                val maxStar = stars.max()
                val minStar = stars.min()
                val totalLength = bs.sumOf { it.totalLength }

                val hostCount = bs.count { it.mapperIDs.contains(s.creatorID) }
                val guestCount = bs.size - hostCount

                val maxSR = floor(maxStar).toString() + if (maxStar - floor(maxStar) >= 0.5) "+" else ""
                val minSR = if (bs.size <= 1) "" else floor(minStar).toString()

                // 其他
                val tags = s.tags.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

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
                s, discussions, hypes, more, d.users
            )
        }
    }
}
