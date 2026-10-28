package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.filter.ScoreClassifier
import com.now.nowbot.model.filter.ScoreClassifier.Companion.classify
import com.now.nowbot.model.filter.hasCondition
import com.now.nowbot.model.osu.*
import com.now.nowbot.model.osu.Covers.Companion.CoverType
import com.now.nowbot.model.osu.LazerScore.Companion.putRank
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.KeyboardMessage
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.NewbieRestrictService
import com.now.nowbot.service.messageServiceImpl.ScoreService.ScoreParam
import com.now.nowbot.service.messageServiceImpl.UUPRService.Companion.getUUScore
import com.now.nowbot.service.messageServiceImpl.UUPRService.Companion.getUUScores
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.*
import com.now.nowbot.util.command.BeatmapIDExtractor
import com.now.nowbot.util.command.BeatmapsetIDExtractor
import com.now.nowbot.util.command.FLAG_ANY
import com.now.nowbot.util.command.LazerModExtractor
import com.now.nowbot.util.command.OsuModeExtractor
import com.now.nowbot.util.command.QQExtractor
import com.now.nowbot.util.command.RangeExtractor
import com.now.nowbot.util.command.ScoreClassifierExtractor
import com.now.nowbot.util.command.StripPipeline
import com.now.nowbot.util.command.UserIDExtractor
import com.yumu.qq.message.Keyboard
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.regex.Matcher
import kotlin.time.Duration.Companion.seconds

@Service("SCORE") class ScoreService(
    private val scoreApiService: OsuScoreApiService,
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,
    private val commandUtil: CommandUtil,
    private val scoreDao: ScoreDao,
    private val restrict: NewbieRestrictService,
) : MessageService<ScoreParam>, TencentMessageService<ScoreParam> {

    data class ScoreParam(
        val user: OsuUser,
        val map: Beatmap,
        val scores: List<LazerScore>,
        val mode: OsuMode,
        val mods: List<LazerMod>,
        val isMultiple: Boolean,
        val isShow: Boolean = false,
        val isCompact: Boolean = false,
        val isRecordCallStatistics: Boolean = true,
    )

    internal data class ScoreData(
        val user: OsuUser,
        val map: Beatmap,
        val scores: List<LazerScore>,
        val mode: OsuMode,
        val isMultipleScore: Boolean = false,
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<ScoreParam>,
    ): Boolean {
        val m3 = Instruction.SCORE_SHOW.matcher(messageText)
        val m2 = Instruction.SCORES.matcher(messageText)
        val m = Instruction.SCORE.matcher(messageText)

        val isMultipleScore: Boolean
        val isShow: Boolean

        val matcher: Matcher

        if (m3.find()) {
            matcher = m3
            isMultipleScore = false
            isShow = true
        } else if (m2.find()) {
            matcher = m2
            isMultipleScore = true
            isShow = false
        } else if (m.find()) {
            matcher = m
            isMultipleScore = false
            isShow = false
        } else {
            return false
        }

        val param = getParam(event, matcher, isMultipleScore, isShow, isCompact = false)

        restrict.checkAsync(event, param.scores)

        data.value = param

        return true
    }

    override fun handleMessage(event: MessageEvent, param: ScoreParam): ServiceCallStatistic? {
        val message = param.getMessageChain()

        try {
            event.replyAsync(message)
        } catch (e: Exception) {
            log.error("谱面成绩：发送失败", e)
            throw IllegalStateException.Send("谱面成绩")
        }

        val scores = param.scores

        return if (param.isRecordCallStatistics) {
            ServiceCallStatistic.builds(
                event,
                beatmapIDs = scores.map { it.beatmapID }.distinct(),
                userIDs = listOf(param.user.userID),
                modes = listOf(param.user.mode),
            )
        } else {
            // 部分情况下不记录 bids
            ServiceCallStatistic.builds(
                event,
                userIDs = listOf(param.user.userID),
                modes = listOf(param.user.mode),
            )
        }
    }

    override fun accept(event: MessageEvent, messageText: String): ScoreParam? {
        val m3 = OfficialInstruction.SCORE_SHOW.matcher(messageText)
        val m2 = OfficialInstruction.SCORES.matcher(messageText)
        val m = OfficialInstruction.SCORE.matcher(messageText)

        val isMultipleScore: Boolean
        val isShow: Boolean

        val matcher: Matcher

        if (m3.find()) {
            matcher = m3
            isMultipleScore = false
            isShow = true
        } else if (m2.find()) {
            matcher = m2
            isMultipleScore = true
            isShow = false
        } else if (m.find()) {
            matcher = m
            isMultipleScore = false
            isShow = false
        } else {
            return null
        }
        return getParam(event, matcher, isMultipleScore, isShow, isCompact = true)
    }

    override fun reply(event: MessageEvent, param: ScoreParam): MessageChain? {
        if (ContextUtil.isTestUser) {
            log.warn(
                "ContextUtil.getContext(ContextUtil.IS_TENCENT_MESSAGE, Boolean::class.java): ${
                    ContextUtil.getContext(
                        ContextUtil.IS_TENCENT_MESSAGE,
                        Boolean::class.java
                    ) == true
                }"
            )
        }
        if (ContextUtil.getContext(ContextUtil.IS_TENCENT_MESSAGE, Boolean::class.java) == true) {
            val result = param.getMessageChain()

            val buttonGroup = listOf(
                listOf(Keyboard.Button("我也要查", "/s"))
            )
            val kb = KeyboardMessage(Keyboard(keyboards = buttonGroup))
            result.addMessage(kb)
            if (ContextUtil.isTestUser) {
                log.warn("keyboard: $kb")
            }
            return result
        }
        return param.getMessageChain()
    }

    private fun getParam(event: MessageEvent, matcher: Matcher, isMultiple: Boolean, isShow: Boolean, isCompact: Boolean): ScoreParam {
        val any: String = matcher.group(FLAG_ANY) ?: ""

        val pipeline = StripPipeline(any)
            .apply(OsuModeExtractor)
            .apply(LazerModExtractor)
            .apply(BeatmapIDExtractor)
            .apply(BeatmapsetIDExtractor)
            .apply(UserIDExtractor)
            .apply(QQExtractor)
            .apply(ScoreClassifierExtractor)
            .apply(RangeExtractor)
            .trim()

        val context = pipeline.context

        val mods = commandUtil.getMods(context)

        val (beatmap, selectedScore) = commandUtil.getBeatmap(
            context,
            event
        )

        commandUtil.addResidualAliases(context)

        // 只有 ScoreClassifier 真的会用来筛成绩的参数才算条件；模式、玩家名、范围不算；`+HR` 会被 LazerModExtractor 桥接成 mod 条件，算。
        val hasCondition = context.hasCondition(ScoreClassifier.entries)

        val defaultLimit = when {
            hasCondition -> 100
            isMultiple -> 20
            else -> 1
        }

        val (offset, limit) = commandUtil.getRange(
            context,
            defaultOffset = 0,
            defaultLimit = defaultLimit,
            multiple = false, // score 不展示前几条，而是指定某一条
            max = 100,
        )

        var isRecordCallStatistics = true

        val data = if (selectedScore != null) {
            // SCORE_ID 模式
            val mode = selectedScore.mode
            val user = userApiService.getOsuUser(selectedScore.userID, mode)

            beatmapApiService.applyBeatmapExtend(selectedScore, beatmap)
            calculateApiService.applyPPToScore(selectedScore)

            val scores = listOf(selectedScore)

            ScoreData(user, beatmap, scores, mode)
        } else {
            val userID = commandUtil.getUserID(
                context,
                event,
                onNull = { isMyself ->
                    if (isMyself) throw BindException.NotBindException.YouNotBind()
                    else throw BindException.NotBindException.UserNotBind()
                },
            ) ?: throw NoSuchElementException.Player()

            val mode = commandUtil.getMode(context, event, userID)
            val user = userApiService.getOsuUser(userID, mode)

            // 有榜时优先取在线分数
            val onlineScores = if (beatmap.hasLeaderBoard) {
                scoreApiService.getBeatmapScores(beatmap.beatmapID, userID, mode)
            } else {
                emptyList()
            }

            if (onlineScores.isNotEmpty()) {

                val ss = if (isMultiple) {
                    onlineScores
                } else {
                    listOf(onlineScores.maxBy { it.pp })
                }

                beatmapApiService.applyBeatmapExtendForSameScore(ss, beatmap)

                ScoreData(user, beatmap, ss, mode)
            } else {
                // 无榜 / 在线无数据：回落到本地
                val local = scoreDao.getBeatmapScores(userID, beatmap, mode)

                if (local.isNotEmpty()) {
                    local.applyFromLocal(beatmap)
                    ScoreData(user, beatmap, local, mode)
                } else if (beatmap.hasLeaderBoard) {
                    // 这个难度上没有成绩，去谱面集里的其他难度找
                    val fallback = findBestScoreInBeatmapset(event, user, beatmap, mode, mods)
                        ?: throw NoSuchElementException.AnyBeatmapScore(beatmap.previewName)

                    // 成绩落在别的难度上时返回的 bid 已经不是用户问的这张谱面
                    isRecordCallStatistics = fallback.map.beatmapID == beatmap.beatmapID

                    fallback
                } else {
                    throw NoSuchElementException.DatabaseBeatmapScore(beatmap.previewName)
                }
            }
        }

        if (data.scores.isEmpty()) {
            throw NoSuchElementException.BeatmapScore(data.map.previewName)
        }

        val scores = data.scores.drop(offset).take(limit)

        if (scores.isEmpty()) {
            throw NoSuchElementException.BeatmapScoreRange(data.map.previewName)
        }

        beatmapApiService.applyAlias(scores)
        BeatmapUtil.applyBeatmapChanges(scores)
        calculateApiService.applyStarToScores(scores)
        calculateApiService.applyPPToScores(scores)

        val scoresByRank = scores.putRank(offset)

        val classified = scoresByRank.classify(context)

        if (classified.isEmpty()) {
            val name = data.map.previewName
            if (hasCondition) {
                throw NoSuchElementException.BeatmapScoreFiltered(name)
            } else {
                throw NoSuchElementException.BeatmapScore(name)
            }
        }

        return ScoreParam(data.user, data.map, classified, data.mode, mods,
            isMultiple, isShow, isCompact, isRecordCallStatistics)
    }

    private fun List<LazerScore>.applyFromLocal(beatmap: Beatmap) = apply {
        beatmapApiService.applyBeatmapExtendForSameScore(this, beatmap)
        beatmapApiService.applyVersion(this)
    }

    /**
     * 在 [map] 所属的谱面集里找玩家成绩最好（`pp × 星数`）的一个难度。
     *
     * `S` / `s` 在用户指到的这个难度上没有成绩时用它兜底：成绩可能在同一谱面集的其他难度上。
     * 返回的 [ScoreData.map] 可能不是 [map]，调用方必须据此判断要不要把 bid 记进调用统计。
     *
     * @return 找不到时返回 null，由调用方决定抛什么异常。
     */
    private fun findBestScoreInBeatmapset(
        event: MessageEvent,
        user: OsuUser,
        map: Beatmap,
        mode: OsuMode,
        mods: List<LazerMod>,
    ): ScoreData? {
        event.replyAndRecallAsync(
            listOf(
                listOf("没有获取到您在这个难度上的成绩。", "您的成绩可能在其他难度内。"),
                listOf("正在查询此谱面中，其他难度上相比最好的成绩。", "正在搜寻所有难度。"),
                listOf("这可能需要一段时间。", "请稍候...", "一会儿就好。"),
            ).joinToString("\n") { it.random() }
        )

        val passed = beatmapApiService.getBeatmapPassed(
            user.userID,
            listOf(map.beatmapsetID),
            mode,
            excludeConverts = !map.mode.isConvertAble(mode),
            isLegacy = null,
            noDiffReductionMods = false,
        )

        if (passed.isEmpty()) return null

        if (passed.size >= 16) {
            event.replyAndRecallAsync(
                listOf(
                    listOf("检测到您在此谱面留有大量不同难度的成绩。", "这张谱面难度好多啊，你肯定刷了很久。"),
                    listOf("\n"),
                    listOf("因此，", "所以，", "为了避免鸿儒 ppy 老冯，"),
                    listOf("只会尝试在星数较高的 16 个成绩中查询。", "星数太低的成绩不会纳入考虑。"),
                ).joinToString("") { it.random() }
            )
        }

        val candidates = passed.sortedByDescending { it.starRating }.take(16)

        val best = fetchScoresAtDifficulties(candidates, user.userID, mode, mods)
            .maxWithOrNull(
                compareBy<LazerScore> { it.pp * it.beatmap.starRating }
                    .thenBy { it.beatmap.starRating }
            ) ?: return null

        // better 的 beatmap 可能和上面的不一样，并且没有 max combo
        beatmapApiService.applyBeatmapExtend(best)

        return ScoreData(user, best.beatmap, listOf(best), mode)
    }

    /**
     * 按 4 个一批查询每个难度上的最好成绩：批内并发，批间串行；
     * 一批全空时退避 2 秒，避免被 ppy 限流。
     */
    private fun fetchScoresAtDifficulties(
        beatmaps: List<Beatmap>,
        userID: Long,
        mode: OsuMode,
        mods: List<LazerMod>,
    ): List<LazerScore> = runBlocking(Dispatchers.IO) {
        val scores = mutableListOf<LazerScore>()
        var checked = 0

        for (batch in beatmaps.chunked(4)) {
            val found = batch.map { beatmap ->
                async(Dispatchers.IO) {
                    runCatching { getBestScoreOnBeatmap(beatmap.beatmapID, userID, mode, mods) }.getOrNull()
                }
            }.awaitAll().filterNotNull()

            checked += batch.size
            scores += found

            if (found.isEmpty() && checked < beatmaps.size) delay(2.seconds)
        }

        scores
    }

    /**
     * 没指定 mod 时走带 legacy 重试的三参版本，老玩家的 stable 成绩也能查到；
     * 指定了 mod 就按 mod 筛。
     */
    private fun getBestScoreOnBeatmap(
        beatmapID: Long,
        userID: Long,
        mode: OsuMode,
        mods: List<LazerMod>,
    ): LazerScore? {
        val score = if (mods.isEmpty()) {
            scoreApiService.getBeatmapScore(beatmapID, userID, mode)
        } else {
            scoreApiService.getBeatmapScore(beatmapID, userID, mode, mods)
        }

        return score?.score
    }

    private fun ScoreParam.getMessageChain(): MessageChain {
        return try {
            if (scores.size > 1 && isMultiple) {

                val body = mapOf(
                    "user" to user,

                    "rank" to (1..(scores.size)).toList(),
                    "scores" to scores,
                    "panel" to "SS",
                    "compact" to ((isCompact && scores.size >= 10) || scores.size > 100)
                )

                MessageChain(imageService.getPanel(body, "A5"))
            } else {
                val score = scores.first()

                val e5Param = ScorePRService.getE5ParamForFilteredScore(user, null, score, "S", beatmapApiService, calculateApiService)

                MessageChain(imageService.getPanel(e5Param.toMap(), if (isShow) "E10" else "E5"))
            }
        } catch (e: Exception) {
            log.error(e.message)
            return getUUMessageChain()
        }
    }

    private fun ScoreParam.getUUMessageChain(): MessageChain {
        return if (scores.size > 1) {
            val ss = scores.take(5)

            beatmapApiService.applyBeatmapExtendForSameScore(ss, map)

            val covers = scoreApiService.getCovers(ss, CoverType.COVER)

            val pairs = ss.mapIndexed { i, it -> i + 1 to it }

            getUUScores(user, pairs, covers)
        } else {
            val s = scores.first()

            val cover = scoreApiService.getCover(s, CoverType.COVER)

            beatmapApiService.applyBeatmapExtend(s, map)

            getUUScore(user, s, cover)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ScoreService::class.java)
    }
}
