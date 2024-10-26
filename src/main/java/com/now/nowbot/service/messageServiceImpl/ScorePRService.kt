package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.UUScore
import com.now.nowbot.model.enums.OsuMode
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
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserAndRangeWithBackoff
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

// Multiple Score也合并进来了
@Service("SCORE_PR")
class ScorePRService(
        private val template: RestTemplate,
        private val imageService: ImageService,
        private val scoreApiService: OsuScoreApiService,
        private val beatmapApiService: OsuBeatmapApiService,
) : MessageService<ScorePRParam>, TencentMessageService<ScorePRParam> {

    @JvmRecord
    data class ScorePRParam(
            val user: OsuUser?,
            val offset: Int,
            val limit: Int,
            val isRecent: Boolean,
            val isMultipleScore: Boolean,
            val mode: OsuMode?
    )

    @JvmRecord
    data class PanelE5Param(
            @JvmField val user: OsuUser,
            @JvmField val score: LazerScore,
            @JvmField val density: IntArray,
            @JvmField val progress: Double,
            @JvmField val original: Map<String, Any>,
            @JvmField val attributes: Map<String, Any>,
            @JvmField val panel: String
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

        val s = matcher.group("s")
        val es = matcher.group("es")

        val isMulti = (Objects.nonNull(s) || Objects.nonNull(es))

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
        val range =
                getUserAndRangeWithBackoff(event, matcher, mode, isMyself, messageText, "recent")

        if (Objects.isNull(range.data)) {
            throw GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Me)
        }

        if (isMulti) {
            offset = range.getOffset(0, true)
            limit = range.getLimit(20, true)
        } else {
            offset = range.getOffset(0, false)
            limit = range.getLimit(1, false)
        }

        val isMultipleScore = limit > 1

        data.value = ScorePRParam(range.data, offset, limit, isRecent, isMultipleScore, mode.data)
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: ScorePRParam) {
        getMessageChain(param, event)?.let { event.reply(it) }
    }

    override fun accept(event: MessageEvent, messageText: String): ScorePRParam? {
        var matcher: Matcher
        val isRecent: Boolean
        val isMulti: Boolean
        when {
            OfficialInstruction.SCORE_PASS.matcher(messageText).apply { matcher = this }.find() -> {
                isRecent = false
                isMulti = false
            }

            OfficialInstruction.SCORE_PASSES.matcher(messageText)
                    .apply { matcher = this }
                    .find() -> {
                isRecent = false
                isMulti = true
            }

            OfficialInstruction.SCORE_RECENT.matcher(messageText)
                    .apply { matcher = this }
                    .find() -> {
                isRecent = true
                isMulti = false
            }

            OfficialInstruction.SCORE_RECENTS.matcher(messageText)
                    .apply { matcher = this }
                    .find() -> {
                isRecent = true
                isMulti = true
            }

            else -> return null
        }

        val offset: Int
        val limit: Int

        val isMyself = AtomicBoolean()
        val mode = getMode(matcher)
        val range =
                getUserAndRangeWithBackoff(event, matcher, mode, isMyself, messageText, "recent")

        if (Objects.isNull(range.data)) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_Param)
        }

        if (isMulti) {
            offset = range.getOffset(0, true)
            limit = range.getLimit(20, true)
        } else {
            offset = range.getOffset(0, false)
            limit = range.getLimit(1, false)
        }

        val isMultipleScore = limit > 1

        return ScorePRParam(range.data, offset, limit, isRecent, isMultipleScore, mode.data)
    }

    @Throws(Throwable::class)
    override fun reply(event: MessageEvent, param: ScorePRParam): MessageChain? {
        return getMessageChain(param)
    }

    @Throws(GeneralTipsException::class)
    private fun getTextOutput(score: LazerScore): MessageChain {
        val d = UUScore.getInstance(score, beatmapApiService)

        @Suppress("UNCHECKED_CAST")
        val httpEntity = HttpEntity.EMPTY as HttpEntity<ByteArray>
        val imgBytes =
                template.exchange(d.url ?: "", HttpMethod.GET, httpEntity, ByteArray::class.java).body

        return QQMsgUtil.getTextAndImage(d.scoreLegacyOutput, imgBytes)
    }

    private fun getMessageChain(param: ScorePRParam, event: MessageEvent? = null): MessageChain? {
        var offset = param.offset
        var limit = param.limit
        val isRecent = param.isRecent
        val isMultipleScore = param.isMultipleScore

        val user = param.user

        val scoreList: List<LazerScore?>

        try {
            scoreList =
                    scoreApiService.getScore(user!!.userID, param.mode, offset, limit, !isRecent)
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
                                user.currentOsuMode.getName())

                else -> e
            }
        } catch (e: Exception) {
            log.error("成绩：列表获取失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "成绩")
        }

        if (CollectionUtils.isEmpty(scoreList)) {
            throw GeneralTipsException(
                GeneralTipsException.Type.G_Null_RecentScore,
                user.username,
                user.currentOsuMode.getName())
        }

        // 成绩发送
        val image: ByteArray

        if (isMultipleScore) {
            val scoreSize = scoreList.size

            // M太大
            if (scoreSize < offset + limit) {
                if (scoreSize - offset < 1) {
                    limit = scoreSize
                    offset = 0
                } else {
                    limit = scoreSize - offset
                }
            }

            val scores: List<LazerScore?> = scoreList.subList(offset, offset + limit)
            beatmapApiService.applySRAndPP(scoreList)

            try {
                // 处理新人群 ps 超星问题
                if (event?.subject?.id == 595985887L) {
                    ContextUtil.setContext("isNewbie", true)
                }
                image = imageService.getPanelA5(user, scores, if (isRecent) "RS" else "PS")
                return QQMsgUtil.getImage(image)
            } catch (e: Exception) {
                log.error("成绩发送失败：", e)
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "成绩")
            } finally {
                ContextUtil.remove()
            }
        } else {
            // 单成绩发送
            val score: LazerScore = scoreList.first()
            val e5Param = getScore4PanelE5(user, score, (if (isRecent) "R" else "P"), beatmapApiService)
            try {
                image = imageService.getPanel(e5Param.toMap(), "E5")
                return QQMsgUtil.getImage(image)
            } catch (e: Exception) {
                log.error(
                        "成绩：绘图出错, 成绩信息:\n {}",
                        JacksonUtil.objectToJsonPretty(e5Param.score),
                        e)
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
            beatmapApiService: OsuBeatmapApiService
        ): PanelE5Param {
            beatmapApiService.applyBeatMapExtend(score)

            val beatmap = score.beatMap

            val original = DataUtil.getOriginal(beatmap)

            beatmapApiService.applySRAndPP(score)

            val attributes = beatmapApiService.getStatistics(score)
            attributes["full_pp"] = beatmapApiService.getFcPP(score).pp
            attributes["perfect_pp"] = beatmapApiService.getMaxPP(score).pp

            val density = beatmapApiService.getBeatmapObjectGrouping26(beatmap)
            val progress = beatmapApiService.getPlayPercentage(score)

            return PanelE5Param(user, score, density, progress, original, attributes, panel)
        }
    }
}
