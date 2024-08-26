package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.JsonData.OsuUser
import com.now.nowbot.model.JsonData.Score
import com.now.nowbot.model.ScoreLegacy
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.MessageServiceImpl.ScorePRService.ScorePRParam
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.OsuApiService.OsuScoreApiService
import com.now.nowbot.service.OsuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.ServiceException.ScoreException
import com.now.nowbot.throwable.TipsException
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
import kotlin.math.max

//Multiple Score也合并进来了
@Service("SCORE_PR")
class ScorePRService(
    var template: RestTemplate? = null,
    var userApiService: OsuUserApiService? = null,
    var scoreApiService: OsuScoreApiService? = null,
    var beatmapApiService: OsuBeatmapApiService? = null,
    var bindDao: BindDao? = null,
    var imageService: ImageService? = null

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
    data class SingleScoreParam(
        @JvmField val user: OsuUser?,
        @JvmField val score: Score,
        @JvmField val density: IntArray,
        @JvmField val progress: Double,
        @JvmField val original: Map<String, Any>,
        @JvmField val attributes: Map<String, Any>
    )

    @Throws(Throwable::class)
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<ScorePRParam>): Boolean {
        val matcher = Instruction.SCORE_PR.matcher(messageText)
        if (!matcher.find()) return false

        val s = matcher.group("s")
        val es = matcher.group("es")

        var offset : Int
        var limit : Int

        val isRecent = if (matcher.group("recent") != null) {
            true
        } else if (matcher.group("pass") != null) {
            false
        } else {
            log.error("成绩分类失败：")
            throw ScoreException(ScoreException.Type.SCORE_Send_Error)
        }

        val isMyself = AtomicBoolean()
        val mode = getMode(matcher)
        val range = getUserAndRangeWithBackoff(event, matcher, mode, isMyself, messageText, "recent")

        if (Objects.isNull(range.data)) {
            throw ScoreException(ScoreException.Type.SCORE_Me_TokenExpired)
        }

        offset = range.getValue(0, true)
        limit = range.getValue(1, false)
        offset = max(0.0, (offset - 1).toDouble()).toInt()
        limit = max(1.0, (limit - offset).toDouble()).toInt()
        if ((Objects.nonNull(s) || Objects.nonNull(es)) && range.allNull()) {
            limit = 20
        }

        val isMultipleScore = limit > 1

        data.setValue(ScorePRParam(range.data, offset, limit, isRecent, isMultipleScore, mode.data))
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: ScorePRParam) {
        val from = event.subject
        getMessageChain(param, event)?.let { from.sendMessage(it) }
    }

    override fun accept(event: MessageEvent, messageText: String): ScorePRParam? {
        var matcher: Matcher
        val isRecent: Boolean
        val isMulti: Boolean
        when {
            OfficialInstruction.SCORE_PASS
                .matcher(messageText)
                .apply { matcher = this }
                .find() -> {
                isRecent = false
                isMulti = false
            }

            OfficialInstruction.SCORE_PASSES
                .matcher(messageText)
                .apply { matcher = this }
                .find() -> {
                isRecent = false
                isMulti = true
            }

            OfficialInstruction.SCORE_RECENT
                .matcher(messageText)
                .apply { matcher = this }
                .find() -> {
                isRecent = true
                isMulti = false
            }

            OfficialInstruction.SCORE_RECENTS
                .matcher(messageText)
                .apply { matcher = this }
                .find() -> {
                isRecent = true
                isMulti = true
            }

            else -> return null
        }

        var offset: Int
        var limit: Int


        val isMyself = AtomicBoolean()
        val mode = getMode(matcher)
        val range = getUserAndRangeWithBackoff(event, matcher, mode, isMyself, messageText, "recent")

        if (Objects.isNull(range.data)) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_Param)
        }

        offset = range.getValue(0, true)
        limit = range.getValue(1, false)
        offset = max(0.0, (offset - 1).toDouble()).toInt()
        limit = max(1.0, (limit - offset).toDouble()).toInt()
        if (isMulti && range.allNull()) {
            limit = 20
        }

        val isMultipleScore = limit > 1

        return ScorePRParam(range.data, offset, limit, isRecent, isMultipleScore, mode.data)
    }

    @Throws(Throwable::class)
    override fun reply(event: MessageEvent, data: ScorePRParam): MessageChain? {
        return getMessageChain(data)
    }

    @Throws(ScoreException::class)
    private fun getTextOutput(score: Score): MessageChain {
        val d = ScoreLegacy.getInstance(score, beatmapApiService)

        val httpEntity = HttpEntity.EMPTY as HttpEntity<Array<Byte>>
        val imgBytes = template!!.exchange(
            d.url, HttpMethod.GET, httpEntity,
            ByteArray::class.java
        ).body

        return QQMsgUtil.getTextAndImage(d.scoreLegacyOutput, imgBytes)
    }

    private fun getMessageChain(param: ScorePRParam, event: MessageEvent? = null): MessageChain? {

        val offset = param.offset
        var limit = param.limit
        val isRecent = param.isRecent
        val isMultipleScore = param.isMultipleScore

        val user = param.user

        val scoreList: List<Score?>

        try {
            scoreList = scoreApiService!!.getRecent(user!!.userID, param.mode, offset, limit, !isRecent)
        } catch (e: WebClientResponseException) {
            // 退避 !recent
            if (event != null && event.rawMessage.lowercase(Locale.getDefault()).contains("recent")) {
                log.info("recent 退避成功")
                return null
            }
            throw when (e) {
                is WebClientResponseException.Unauthorized -> ScoreException(ScoreException.Type.SCORE_Me_TokenExpired)
                is WebClientResponseException.Forbidden -> ScoreException(ScoreException.Type.SCORE_Player_Banned)
                is WebClientResponseException.NotFound -> ScoreException(
                    ScoreException.Type.SCORE_Player_NoScore,
                    user!!.username
                )

                else -> ScoreException(ScoreException.Type.SCORE_Send_Error)
            }
        } catch (e: Exception) {
            log.error("成绩：列表获取失败", e)
            throw ScoreException(ScoreException.Type.SCORE_Score_FetchFailed)
        }

        if (CollectionUtils.isEmpty(scoreList)) {
            throw ScoreException(ScoreException.Type.SCORE_Recent_NotFound, user.username)
        }

        //成绩发送
        val image: ByteArray

        if (isMultipleScore) {
            val scoreSize = scoreList.size

            //M太大
            if (scoreSize < offset + limit) limit = scoreSize - offset
            if (limit <= 0) throw ScoreException(ScoreException.Type.SCORE_Score_OutOfRange)

            val scores: List<Score?> = scoreList.subList(offset, offset + limit)
            beatmapApiService!!.applySRAndPP(scoreList)

            try {
                // 处理新人群 ps 超星问题
                if (event?.subject?.id == 595985887L) ContextUtil.setContext("isNewbie", true)
                image = imageService!!.getPanelA5(user, scores)
                return QQMsgUtil.getImage(image)
            } catch (e: Exception) {
                log.error("成绩发送失败：", e)
                throw ScoreException(ScoreException.Type.SCORE_Render_Error)
            } finally {
                ContextUtil.remove()
            }
        } else {
            //单成绩发送
            val score: Score = scoreList.first()
            val e5Param = getScore4PanelE5(user, score, beatmapApiService!!)
            try {
                image = imageService!!.getPanelE5(e5Param)
                return QQMsgUtil.getImage(image)
            } catch (e: Exception) {
                log.error("成绩：绘图出错, 成绩信息:\n {}", JacksonUtil.objectToJsonPretty(e5Param.score), e)
                return getTextOutput(e5Param.score)
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ScorePRService::class.java)

        @JvmStatic
        @Throws(Exception::class)
        fun getScore4PanelE5(user: OsuUser?, score: Score, beatmapApiService: OsuBeatmapApiService): SingleScoreParam {
            val b = score.beatMap

            beatmapApiService.applyBeatMapExtend(score)

            val original = HashMap<String, Any>(6)
            original["cs"] = b.cs
            original["ar"] = b.ar
            original["od"] = b.od
            original["hp"] = b.hp
            original["bpm"] = b.bpm
            original["drain"] = b.hitLength
            original["total"] = b.totalLength

            beatmapApiService.applySRAndPP(score)

            val attributes = beatmapApiService.getStatistics(score)
            attributes["full_pp"] = beatmapApiService.getFcPP(score).pp
            attributes["perfect_pp"] = beatmapApiService.getMaxPP(score).pp

            val density = beatmapApiService.getBeatmapObjectGrouping26(b)
            val progress = beatmapApiService.getPlayPercentage(score)

            return SingleScoreParam(user, score, density, progress, original, attributes)
        }
    }
}
