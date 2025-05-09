package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.UUScore
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.ScorePRService.ScorePRParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.impl.ScoreApiImpl
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserAndRangeWithBackoff
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

// Multiple Score也合并进来了
@Service("SCORE_PR")
class ScorePRService(
    private val osuApiWebClient: WebClient,
    private val imageService: ImageService,
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
) : MessageService<ScorePRParam>, TencentMessageService<ScorePRParam> {

    data class ScorePRParam(
        val user: OsuUser?,
        val offset: Int,
        val limit: Int,
        val isRecent: Boolean,
        val mode: OsuMode?
    )

    data class PanelE5Param(
        val user: OsuUser,
        val score: LazerScore,
        val position: Int?,
        val density: IntArray,
        val progress: Double,
        val original: Map<String, Any>,
        val attributes: Any,
        val panel: String,
        val health: Map<Int, Double>?
    ) {
        fun toMap(): Map<String, Any> {

            val out = mutableMapOf<String, Any>()

            out["user"] = user
            out["score"] = score
            out["density"] = density
            out["progress"] = progress
            out["original"] = original
            out["attributes"] = attributes
            out["panel"] = panel

            if (position != null) {
                out["position"] = position
            }

            if (health != null) {
                out["health"] = mapOf(
                    "time" to health.map { it.key },
                    "percent" to health.map { it.value }
                )
            }

            return out
        }
    }

    @Throws(Throwable::class)
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: DataValue<ScorePRParam>
    ): Boolean {
        val matcher = Instruction.SCORE_PR.matcher(messageText)
        if (!matcher.find()) return false

        val isMulti = (matcher.group("s").isNullOrBlank().not() || matcher.group("es").isNullOrBlank().not())

        val offset: Int
        val limit: Int

        val isRecent =
                if (matcher.group("recent") != null) {
                    true
                } else if (matcher.group("pass") != null) {
                    false
                } else {
                    log.error("成绩分类失败：")
                    throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "成绩")
                }

        val isMyself = AtomicBoolean()
        val mode = getMode(matcher)

        val range = getUserAndRangeWithBackoff(event, matcher, mode, isMyself, messageText, "recent")

        range.setZeroToRange100()

        if (range.data == null) {
            throw GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Me)
        }
        if (isMulti) {
            offset = range.getOffset(0, true)
            limit = range.getLimit(20, true)
        } else {
            offset = range.getOffset(0, false)
            limit = range.getLimit(1, false)
        }

        data.value = ScorePRParam(range.data, offset, limit, isRecent, mode.data)
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: ScorePRParam) {
        getMessageChain(param, event)?.let { event.reply(it) }
    }

    override fun accept(event: MessageEvent, messageText: String): ScorePRParam? {
        var matcher: Matcher
        val isRecent: Boolean
        val isMultiInput: Boolean
        when {
            OfficialInstruction.SCORE_PASS.matcher(messageText)
                .apply { matcher = this }.find() -> {
                isRecent = false
                isMultiInput = false
            }

            OfficialInstruction.SCORE_PASSES.matcher(messageText)
                    .apply { matcher = this }.find() -> {
                isRecent = false
                isMultiInput = true
            }

            OfficialInstruction.SCORE_RECENT.matcher(messageText)
                    .apply { matcher = this }.find() -> {
                isRecent = true
                isMultiInput = false
            }

            OfficialInstruction.SCORE_RECENTS.matcher(messageText)
                    .apply { matcher = this }.find() -> {
                isRecent = true
                isMultiInput = true
            }

            else -> return null
        }

        val offset: Int
        val limit: Int

        val isMyself = AtomicBoolean()
        val mode = getMode(matcher)
        val range = getUserAndRangeWithBackoff(event, matcher, mode, isMyself, messageText, "recent")

        if (range.data == null) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_Param)
        }

        if (isMultiInput) {
            offset = range.getOffset(0, true)
            limit = range.getLimit(20, true)
        } else {
            offset = range.getOffset(0, false)
            limit = range.getLimit(1, false)
        }

        return ScorePRParam(range.data, offset, limit, isRecent, mode.data)
    }

    @Throws(Throwable::class)
    override fun reply(event: MessageEvent, param: ScorePRParam): MessageChain? {
        return getMessageChain(param)
    }

    @Throws(GeneralTipsException::class)
    private fun getTextOutput(score: LazerScore): MessageChain {
        val d = UUScore(score, calculateApiService)

        val imgBytes = osuApiWebClient.get()
            .uri(d.url ?: "")
            .retrieve()
            .bodyToMono(ByteArray::class.java)
            .block()


        return QQMsgUtil.getTextAndImage(d.scoreLegacyOutput, imgBytes)
    }

    private fun getMessageChain(param: ScorePRParam, event: MessageEvent? = null): MessageChain? {
        val offset = param.offset
        val limit = param.limit
        val isRecent = param.isRecent

        val user = param.user

        val scoreMap: Map<Int, LazerScore>

        try {
            scoreMap = scoreApiService.getScore(user!!.userID, param.mode, offset, limit, !isRecent)
                .mapIndexed { index, score -> (index + offset + 1) to score }.toMap()
        } catch (e: WebClientResponseException) {
            // 退避 !recent
            if (event != null &&
                    event.rawMessage.lowercase(Locale.getDefault()).contains("recent")) {
                log.info("recent 退避成功")
                return null
            }

            throw when (e) {
                is WebClientResponseException.Unauthorized ->
                        GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Me)
                is WebClientResponseException.Forbidden ->
                        GeneralTipsException(
                                GeneralTipsException.Type.G_Banned_Player, user!!.username)
                is WebClientResponseException.NotFound ->
                        GeneralTipsException(
                                GeneralTipsException.Type.G_Null_RecentScore,
                                user!!.username,
                                user.currentOsuMode.fullName)

                else -> e
            }
        } catch (e: Exception) {
            log.error("成绩：列表获取失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "成绩")
        }

        if (scoreMap.isEmpty()) {
            throw GeneralTipsException(
                GeneralTipsException.Type.G_Null_RecentScore,
                user.username,
                user.currentOsuMode.fullName)
        }

        // 成绩发送
        val image: ByteArray

        if (scoreMap.size > 1) {
            val ranks = scoreMap.map{it.key}.toList()
            val scores = scoreMap.map{it.value}.toList()

            scoreApiService.asyncDownloadBackground(scores)
            scoreApiService.asyncDownloadBackground(scores, ScoreApiImpl.CoverType.LIST)

            calculateApiService.applyPPToScores(scores)
            calculateApiService.applyBeatMapChanges(scores)
            calculateApiService.applyStarToScores(scores)

            try {
                val body = mapOf(
                    "user" to user,
                    "score" to scores,
                    "rank" to ranks,
                    "panel" to if (isRecent) "RS" else "PS"
                )

                image = imageService.getPanel(body, "A5")
                return QQMsgUtil.getImage(image)
            } catch (e: Exception) {
                log.error("成绩发送失败：", e)
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "成绩")
            }
        } else {
            // 单成绩发送
            val score = scoreMap.map { it.value }.first()

            scoreApiService.asyncDownloadBackground(score)
            scoreApiService.asyncDownloadBackground(score, ScoreApiImpl.CoverType.LIST)

            val e5Param = getScore4PanelE5(user, score, (if (isRecent) "R" else "P"), beatmapApiService, calculateApiService)
            try {
                /*
                val st = OffsetDateTime.of(2025, 4, 1, 0, 0, 0, 0, ZoneOffset.ofHours(8))
                val ed = OffsetDateTime.of(2025, 4, 2, 0, 0, 0, 0, ZoneOffset.ofHours(8))

                image = if (OffsetDateTime.now().isAfter(st) && OffsetDateTime.now().isBefore(ed)) {
                    imageService.getPanel(e5Param.toMap(), "Eta" +
                            (floor((System.currentTimeMillis() % 1000) / 1000.0 * 4) + 1).toInt())
                } else {
                }

                 */

                image = imageService.getPanel(e5Param.toMap(), "E5")
                return QQMsgUtil.getImage(image)
            } catch (e: Exception) {
                log.error("成绩：绘图出错, 成绩信息:\n {}",
                    JacksonUtil.objectToJsonPretty(e5Param.score), e)
                return getTextOutput(e5Param.score)
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ScorePRService::class.java)

        @JvmStatic
        @Throws(Exception::class)
        fun getScore4PanelE5(
            user: OsuUser,
            score: LazerScore,
            panel: String,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService
        ): PanelE5Param {
            beatmapApiService.applyBeatMapExtend(score)
            return getScore4PanelE5AfterExtended(user, score, position = null, panel, beatmapApiService, calculateApiService)
        }

        @JvmStatic
        @Throws(Exception::class)
        fun getScore4PanelE5(
            user: OsuUser,
            score: LazerScore,
            beatMap: BeatMap,
            position: Int? = null,
            panel: String,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService,
        ): PanelE5Param {
            beatmapApiService.applyBeatMapExtend(score, beatMap)
            return getScore4PanelE5AfterExtended(user, score, position, panel, beatmapApiService, calculateApiService)
        }

        @JvmStatic
        @Throws(Exception::class)
        fun getScore4PanelE5AfterExtended(
            user: OsuUser,
            score: LazerScore,
            position: Int? = null,
            panel: String,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService,
        ): PanelE5Param {
            val beatmap = score.beatMap
            val original = DataUtil.getOriginal(beatmap)

            calculateApiService.applyPPToScore(score)
            calculateApiService.applyBeatMapChanges(score)
            calculateApiService.applyStarToScore(score)

            val attributes = calculateApiService.getScoreStatisticsWithFullAndPerfectPP(score)

            val density = beatmapApiService.getBeatmapObjectGrouping26(beatmap)
            val progress = beatmapApiService.getPlayPercentage(score)

            return PanelE5Param(user, score, position, density, progress, original, attributes, panel, null)
        }
    }
}
