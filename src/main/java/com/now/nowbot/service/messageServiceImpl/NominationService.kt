package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.BeatMapSet
import com.now.nowbot.model.json.Discussion
import com.now.nowbot.model.json.DiscussionDetails
import com.now.nowbot.model.json.DiscussionDetails.MessageType.*
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
import com.now.nowbot.throwable.serviceException.NominationException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.command.FLAG_SID
import java.util.*
import java.util.regex.Matcher
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.math.floor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service("NOMINATION")
class NominationService(
        private val osuBeatmapApiService: OsuBeatmapApiService,
        private val osuUserApiService: OsuUserApiService,
        private val osuDiscussionApiService: OsuDiscussionApiService,
        private val imageService: ImageService,
) : MessageService<Matcher>, TencentMessageService<Matcher> {

    @Throws(Throwable::class)
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: DataValue<Matcher>,
    ): Boolean {
        val matcher = Instruction.NOMINATION.matcher(messageText)
        if (!matcher.find()) return false

        data.value = matcher
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, matcher: Matcher) {
        val image: ByteArray =
                getNominationImage(
                        matcher,
                        osuBeatmapApiService,
                        osuDiscussionApiService,
                        osuUserApiService,
                        imageService,
                )

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("提名信息：发送失败", e)
            throw NominationException(NominationException.Type.N_Send_Error)
        }
    }

    override fun accept(event: MessageEvent, messageText: String): Matcher? {
        val matcher = OfficialInstruction.NOMINATION.matcher(messageText)
        if (!matcher.find()) return null

        return matcher
    }

    override fun reply(event: MessageEvent, param: Matcher): MessageChain? {
        val image: ByteArray =
                getNominationImage(
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
            val sid: Long
            val mode = matcher.group("mode")
            val isSID = !(mode != null && (mode == "b" || mode == "bid"))

            try {
                sid = matcher.group(FLAG_SID).toLong()
            } catch (e: NumberFormatException) {
                throw NominationException(NominationException.Type.N_Instructions)
            }

            val data =
                    parseData(
                            sid,
                            isSID,
                            osuBeatmapApiService,
                            osuDiscussionApiService,
                            osuUserApiService,
                    )

            return try {
                imageService.getPanel(data, "N")
            } catch (e: Exception) {
                log.error("提名信息：渲染失败", e)
                throw NominationException(NominationException.Type.N_Render_Failed)
            }
        }

        @JvmStatic
        @Throws(NominationException::class)
        fun parseData(
                sid: Long,
                isSID: Boolean,
                beatmapApiService: OsuBeatmapApiService,
                discussionApiService: OsuDiscussionApiService,
                userApiService: OsuUserApiService?,
        ): Map<String, Any> {
            var id = sid
            var s: BeatMapSet
            val d: Discussion
            val details: List<DiscussionDetails>
            val discussions: List<DiscussionDetails>
            val hypes: List<DiscussionDetails>
            val more: MutableMap<String, Any> = HashMap()

            if (isSID) {
                try {
                    s = beatmapApiService.getBeatMapSet(id)
                } catch (e: WebClientResponseException.NotFound) {
                    try {
                        val b = beatmapApiService.getBeatMapFromDataBase(id)
                        id = b.beatMapSetID
                        s = beatmapApiService.getBeatMapSet(id)
                    } catch (e1: WebClientResponseException.NotFound) {
                        throw NominationException(NominationException.Type.N_Map_NotFound)
                    } catch (e1: HttpClientErrorException.NotFound) {
                        throw NominationException(NominationException.Type.N_Map_NotFound)
                    } catch (e1: Exception) {
                        log.error("提名信息：谱面获取失败", e1)
                        throw NominationException(NominationException.Type.N_Map_FetchFailed)
                    }
                } catch (e: HttpClientErrorException.NotFound) {
                    try {
                        val b = beatmapApiService.getBeatMapFromDataBase(id)
                        id = b.beatMapSetID
                        s = beatmapApiService.getBeatMapSet(id)
                    } catch (e1: WebClientResponseException.NotFound) {
                        throw NominationException(NominationException.Type.N_Map_NotFound)
                    } catch (e1: HttpClientErrorException.NotFound) {
                        throw NominationException(NominationException.Type.N_Map_NotFound)
                    } catch (e1: Exception) {
                        log.error("提名信息：谱面获取失败", e1)
                        throw NominationException(NominationException.Type.N_Map_FetchFailed)
                    }
                } catch (e: WebClientResponseException.BadGateway) {
                    throw NominationException(NominationException.Type.N_API_Unavailable)
                } catch (e: WebClientResponseException.ServiceUnavailable) {
                    throw NominationException(NominationException.Type.N_API_Unavailable)
                } catch (e: Exception) {
                    log.error("提名信息：谱面获取失败", e)
                    throw NominationException(NominationException.Type.N_Map_FetchFailed)
                }
            } else {
                try {
                    val b = beatmapApiService.getBeatMapFromDataBase(id)
                    id = b.beatMapSetID
                    s = beatmapApiService.getBeatMapSet(id)
                } catch (e: WebClientResponseException.NotFound) {
                    throw NominationException(NominationException.Type.N_Map_NotFound)
                } catch (e: HttpClientErrorException.NotFound) {
                    throw NominationException(NominationException.Type.N_Map_NotFound)
                } catch (e: WebClientResponseException.BadGateway) {
                    throw NominationException(NominationException.Type.N_API_Unavailable)
                } catch (e: WebClientResponseException.ServiceUnavailable) {
                    throw NominationException(NominationException.Type.N_API_Unavailable)
                } catch (e: Exception) {
                    log.error("提名信息：谱面获取失败", e)
                    throw NominationException(NominationException.Type.N_Map_FetchFailed)
                }
            }

            if (s.creatorData != null) {
                s.creatorData!!.parseFull(userApiService)
            }

            try {
                d = discussionApiService.getBeatMapSetDiscussion(id)
            } catch (e: Exception) {
                log.error("提名信息：讨论区获取失败", e)
                throw NominationException(NominationException.Type.N_Discussion_FetchFailed)
            }

            // 插入难度名
            if (! s.beatMaps.isNullOrEmpty()) {
                val diffs =
                        s.beatMaps!!
                                .stream()
                                .collect(
                                        Collectors.toMap(
                                                BeatMap::beatMapID,
                                                BeatMap::difficultyName,
                                        )
                                )

                d.addDifficulty4DiscussionDetails(diffs)
            }

            // 获取 hypes 和 discussions 列表
            run {
                // 这两个list需要合并起来
                details =
                        Stream.of(d.discussions, d.includedDiscussions)
                                .filter { obj: List<DiscussionDetails>? -> Objects.nonNull(obj) }
                                .flatMap { obj: List<DiscussionDetails>? -> obj!!.stream() }
                                .distinct()
                                .toList()

                hypes =
                        details.stream()
                                .filter { i: DiscussionDetails ->
                                    val t = i.messageType
                                    t == hype || t == praise
                                }
                                .toList()

                val dis =
                        details.stream()
                                .filter { i: DiscussionDetails ->
                                    val t = i.messageType
                                    t == problem || t == suggestion
                                }
                                .toList()
                discussions = Discussion.toppingUnsolvedDiscussionDetails(dis)
            }
            // 这一部分提供额外信息
            run {
                var hostCount = 0
                var guestCount = 0
                var problemCount = 0
                var suggestCount = 0
                var notSolvedCount = 0
                var hypeCount = 0
                var praiseCount = 0
                var maxSR = ""
                var minSR = ""
                var totalLength = 0
                val SRList: MutableList<Double> = mutableListOf()

                for (i in details) {
                    when (i.messageType) {
                        problem -> problemCount++
                        suggestion -> suggestCount++
                        hype -> hypeCount++
                        praise -> praiseCount++
                        else -> {}
                    }

                    if (i.canBeResolved && !i.resolved) {
                        notSolvedCount++
                    }
                }

                val bs = s.beatMaps

                // 初始化星数
                var maxStarRating = 0.0
                var minStarRating = 0.0

                if (! bs.isNullOrEmpty()) {
                    val f: BeatMap = bs.first()
                    totalLength = f.totalLength
                    maxStarRating = f.starRating
                    minStarRating = maxStarRating
                }

                if (! bs.isNullOrEmpty()) {
                    for (b in bs) {
                        if (s.creatorID == b.mapperID) {
                            hostCount++
                        } else {
                            guestCount++
                        }

                        if (b.starRating > maxStarRating) maxStarRating = b.starRating
                        if (b.starRating < minStarRating) minStarRating = b.starRating

                        SRList.add(b.starRating)
                    }

                    val maxStarRatingInt = floor(maxStarRating).toInt()
                    val minStarRatingInt = floor(minStarRating).toInt()

                    maxSR = maxStarRatingInt.toString()
                    minSR = minStarRatingInt.toString()

                    if (maxStarRating - maxStarRatingInt >= 0.5) maxSR += '+'

                    // if (minStarRating - minStarRatingInt >= 0.5) minSR += '+';

                    // 单难度
                    if (bs.size <= 1) minSR = ""
                }

                // 其他
                val tags =
                        (s.tags ?: "")
                                .split(" ".toRegex())
                                .dropLastWhile { it.isEmpty() }
                                .toTypedArray()

                more["hostCount"] = hostCount
                more["guestCount"] = guestCount
                more["totalCount"] = hostCount + guestCount
                more["maxSR"] = maxSR
                more["minSR"] = minSR
                more["SRList"] =
                        SRList.stream()
                                .sorted(
                                        Comparator.comparingDouble { obj: Double -> obj }.reversed()
                                )
                                .toList()
                more["totalLength"] = totalLength
                more["tags"] = tags
                more["problemCount"] = problemCount
                more["suggestCount"] = suggestCount
                more["notSolvedCount"] = notSolvedCount
                more["hypeCount"] = hypeCount
                more.put("praiseCount", praiseCount)
            }

            val n = HashMap<String, Any>()
            n["beatmapset"] = s
            n["discussion"] = discussions
            n["hype"] = hypes
            n["more"] = more
            n["users"] = d.users

            return n
        }
    }
}
