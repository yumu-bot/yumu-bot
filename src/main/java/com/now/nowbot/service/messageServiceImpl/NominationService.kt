package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.Discussion
import com.now.nowbot.model.osu.DiscussionDetails
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuDiscussionApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.command.FLAG_SID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*
import java.util.regex.Matcher
import kotlin.math.floor

@Service("NOMINATION") class NominationService(
    private val osuBeatmapApiService: OsuBeatmapApiService,
    private val osuUserApiService: OsuUserApiService,
    private val osuDiscussionApiService: OsuDiscussionApiService,
    private val imageService: ImageService,
) : MessageService<Matcher>, TencentMessageService<Matcher> {

    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<Matcher>,
    ): Boolean {
        val matcher = Instruction.NOMINATION.matcher(messageText)
        if (!matcher.find()) return false

        data.value = matcher
        return true
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: Matcher) {
        val image: ByteArray = getNominationImage(
            param,
            osuBeatmapApiService,
            osuDiscussionApiService,
            osuUserApiService,
            imageService,
        )

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("提名信息：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "提名信息")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): Matcher? {
        val matcher = OfficialInstruction.NOMINATION.matcher(messageText)
        if (!matcher.find()) return null

        return matcher
    }

    override fun reply(event: MessageEvent, param: Matcher): MessageChain? {
        val image: ByteArray = getNominationImage(
            param,
            osuBeatmapApiService,
            osuDiscussionApiService,
            osuUserApiService,
            imageService,
        )

        return MessageChainBuilder().addImage(image).build()
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(NominationService::class.java)

        private fun getNominationImage(
            matcher: Matcher,
            osuBeatmapApiService: OsuBeatmapApiService,
            osuDiscussionApiService: OsuDiscussionApiService,
            osuUserApiService: OsuUserApiService,
            imageService: ImageService,
        ): ByteArray {
            val sidStr: String? = matcher.group(FLAG_SID)
            val mode = matcher.group("mode")
            val isSID = !(mode != null && (mode == "b" || mode == "bid"))

            val sid = sidStr?.toLongOrNull()
                ?: if (isSID) {
                    throw IllegalArgumentException.WrongException.BeatmapsetID()
                } else {
                    throw IllegalArgumentException.WrongException.BeatmapID()
                }

            val data = parseData(
                sid,
                isSID,
                osuBeatmapApiService,
                osuDiscussionApiService,
                osuUserApiService,
            )

            return imageService.getPanel(data, "N")
        }

        @JvmStatic @Throws(GeneralTipsException::class) fun parseData(
            sid: Long,
            isSID: Boolean,
            beatmapApiService: OsuBeatmapApiService,
            discussionApiService: OsuDiscussionApiService,
            userApiService: OsuUserApiService,
        ): Map<String, Any> {
            var id = sid
            var s: Beatmapset
            val d: Discussion
            val details: List<DiscussionDetails>
            val discussions: List<DiscussionDetails>
            val hypes: List<DiscussionDetails>
            val more: Map<String, Any>

            if (isSID) {
                try {
                    s = beatmapApiService.getBeatMapSet(id)
                } catch (e: WebClientResponseException.NotFound) {
                    try {
                        val b = beatmapApiService.getBeatMapFromDataBase(id)
                        id = b.beatmapsetID
                        s = beatmapApiService.getBeatMapSet(id)
                    } catch (e1: WebClientResponseException.NotFound) {
                        throw GeneralTipsException(GeneralTipsException.Type.G_Null_Map)
                    } catch (e1: Exception) {
                        log.error("提名信息：谱面获取失败", e1)
                        throw GeneralTipsException(GeneralTipsException.Type.G_Fetch_BeatMap)
                    }
                } catch (e: WebClientResponseException.BadGateway) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI)
                } catch (e: WebClientResponseException.ServiceUnavailable) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI)
                } catch (e: Exception) {
                    log.error("提名信息：谱面获取失败", e)
                    throw GeneralTipsException(GeneralTipsException.Type.G_Fetch_BeatMap)
                }
            } else {
                try {
                    val b = beatmapApiService.getBeatMapFromDataBase(id)
                    id = b.beatmapsetID
                    s = beatmapApiService.getBeatMapSet(id)
                } catch (e: WebClientResponseException.NotFound) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Null_Map)
                } catch (e: WebClientResponseException.BadGateway) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI)
                } catch (e: WebClientResponseException.ServiceUnavailable) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_ppyAPI)
                } catch (e: Exception) {
                    log.error("提名信息：谱面获取失败", e)
                    throw GeneralTipsException(GeneralTipsException.Type.G_Fetch_BeatMap)
                }
            }

            if (s.creatorData != null) {
                s.creatorData!!.parseFull(userApiService)
            }

            try {
                d = discussionApiService.getBeatMapSetDiscussion(id)
            } catch (e: Exception) {
                log.error("提名信息：讨论区获取失败", e)
                throw GeneralTipsException(GeneralTipsException.Type.G_Fetch_Discussion)
            }

            if (!s.beatmaps.isNullOrEmpty()) {
                // 插入标签
                s.beatmaps!!.forEach { beatmapApiService.extendBeatMapTag(it) }

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

                val hostCount = bs.count { it.mapperID == s.creatorID }
                val guestCount = bs.size - hostCount

                val maxSR = floor(maxStar).toString() + if (maxStar - floor(maxStar) >= 0.5) "+" else ""
                val minSR = if (bs.size <= 1) "" else floor(minStar).toString()

                // 其他
                val tags = (s.tags ?: "").split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

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

            val n = mapOf(
                "beatmapset" to s,
                "discussion" to discussions,
                "hype" to hypes,
                "more" to more,
                "users" to d.users,
            )

            return n
        }
    }
}
