package com.now.nowbot.controller

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import com.now.nowbot.aop.OpenResource
import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.dao.PPMinusDao
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.OsuMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.getMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.BeatmapDifficultyAttributes
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.model.mappool.old.MapPoolDto
import com.now.nowbot.model.multiplayer.Match
import com.now.nowbot.model.multiplayer.MatchRating
import com.now.nowbot.model.ppminus.PPMinus
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.messageServiceImpl.*
import com.now.nowbot.service.osuApiService.*
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.serviceException.DiceException
import com.now.nowbot.throwable.serviceException.MRAException
import com.now.nowbot.throwable.serviceException.MapPoolException
import com.now.nowbot.util.DataUtil.parseRange2Limit
import com.now.nowbot.util.DataUtil.parseRange2Offset
import com.now.nowbot.util.QQMsgUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.*
import org.springframework.lang.NonNull
import org.springframework.lang.Nullable
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.math.max
import kotlin.math.min

@RestController @ResponseBody
@CrossOrigin("http://localhost:5173", "https://siyuyuko.github.io", "https://a.yasunaori.be")
@RequestMapping(value = ["/pub"], method = [RequestMethod.GET]) class BotWebApi(
    private val userApiService: OsuUserApiService,
    private val matchApiService: OsuMatchApiService,
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,
    private val discussionApiService: OsuDiscussionApiService,
    private val infoDao: OsuUserInfoDao,
    private val ppMinusDao: PPMinusDao
) {

    /**
     * SN 图片接口 (SAN)
     * 私密，仅消防栓使用
     *
     * @param name     玩家名称
     * @param playMode 模式，可为空
     * @return image PPM 图片
     */
    @GetMapping(value = ["san"]) @OpenResource(name = "sn", desp = "查询玩家的 SAN") fun getSan(
        @OpenResource(
            name = "name", desp = "第一个玩家的名称", required = true
        ) @RequestParam(value = "name") name: String,
        @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") playMode: String?
    ): ResponseEntity<ByteArray> {
        val mode = getMode(playMode)

        var info: OsuUser?
        val bplist: List<LazerScore>
        var ppm: PPMinus?

        try {
            info = userApiService.getOsuUser(name.trim(), mode)
            bplist = scoreApiService.getBestScores(info.userID, mode)
            ppm = PPMinus.getInstance(mode, info, bplist)
        } catch (e: Exception) {
            info = null
            ppm = null
        }

        val data = imageService.getPanelGamma(info, mode, ppm)
        return ResponseEntity(
            data, getImageHeader(
                "${name.trim()}-sn.jpg", data.size
            ), HttpStatus.OK
        )
    }

    /**
     * PM 图片接口 (PPM)
     *
     * @param name     玩家名称
     * @param name2    第二个玩家名称，不为空会转到 PV 接口
     * @param playMode 模式，可为空
     * @return image PPM 图片
     */
    @GetMapping(value = ["ppm"]) @OpenResource(name = "pm", desp = "查询玩家的 PPM") fun getPPMinus(
        @OpenResource(name = "name", desp = "第一个玩家的名称", required = true) @RequestParam(
            value = "name", required = true
        ) name: String,
        @OpenResource(name = "name2", desp = "第二个玩家的名称") @Nullable @RequestParam("name2") name2: String? = null,
        @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") playMode: String? = "osu",
        @RequestParam(value = "u1", required = false) u1: String? = null
    ): ResponseEntity<ByteArray> {
        if (name2.isNullOrBlank().not()) {
            return getPPMinusVS(name, name2!!, playMode)
        }
        if (u1.isNullOrBlank().not()) {
            return getPPMinus(u1!!, playMode = playMode)
        }

        val mode = getMode(playMode)

        val me = userApiService.getOsuUser(name.trim(), mode)

        val ppm4 = PPMinusService.getPPMinus4(me, scoreApiService, ppMinusDao)

        try {
            val data = imageService.getPanel(PPMinusService.getPPM4Body(me, null, ppm4, null, mode), "B1")
            return ResponseEntity(
                data, getImageHeader(
                    "${name.trim()}-pm.jpg", data.size
                ), HttpStatus.OK
            )
        } catch (e: Exception) {
            throw RuntimeException(GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "PPM4").message)
        }
    }

    /**
     * PV 图片接口 (PV)
     *
     * @param name     玩家名称
     * @param name2    第二个玩家名称，必填
     * @param playMode 模式，可为空
     * @return image PPM 图片
     */
    @GetMapping(value = ["ppm/vs"]) @OpenResource(name = "pv", desp = "比较玩家的 PPM") fun getPPMinusVS(
        @OpenResource(name = "name", desp = "第一个玩家的名称", required = true) @RequestParam("name") name: String,
        @OpenResource(name = "name2", desp = "第二个玩家的名称", required = true) @RequestParam("name2") name2: String,
        @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") playMode: String?
    ): ResponseEntity<ByteArray> {
        var mode = getMode(playMode)

        val user1 = userApiService.getOsuUser(name.trim(), mode)
        val user2 = userApiService.getOsuUser(name2.trim(), mode)

        mode = getMode(playMode, user1.currentOsuMode)

        val ppm1 = PPMinusService.getPPMinus4(user1, scoreApiService, ppMinusDao)
        val ppm2 = PPMinusService.getPPMinus4(user2, scoreApiService, ppMinusDao)

        try {
            val data = imageService.getPanel(PPMinusService.getPPM4Body(user1, user2, ppm1, ppm2, mode), "B1")
            return ResponseEntity(
                data, getImageHeader(
                    "${name.trim()} vs ${name2.trim()}-pv.jpg", data.size
                ), HttpStatus.OK
            )
        } catch (e: Exception) {
            throw RuntimeException(GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "PPM4").message)
        }
    }

    /**
     * 比赛结果图片接口 (MN)
     *
     * @param matchID 比赛编号
     * @param k       跳过开头对局数量，跳过热手
     * @param d       忽略结尾对局数量，忽略 TB 表演赛
     * @param f       是否删除低于 1w 分的成绩，不传默认删除
     * @param r       是否保留重复对局，不传默认保留
     * @return image 比赛结果图片
     */
    @GetMapping(value = ["match/now"]) @OpenResource(name = "mn", desp = "查询比赛结果")
    @Throws(RuntimeException::class) fun getMatchNow(
        @OpenResource(name = "matchid", desp = "比赛编号", required = true) @RequestParam("id") matchID: Int,
        @OpenResource(name = "easy-multiplier", desp = "Easy 模组倍率") @Nullable e: Double?,
        @OpenResource(name = "skip", desp = "跳过开头") @Nullable k: Int?,
        @OpenResource(name = "ignore", desp = "忽略结尾") @Nullable d: Int?,
        @OpenResource(name = "delete-low", desp = "删除低分成绩") @Nullable f: Boolean?,
        @OpenResource(name = "keep-rematch", desp = "保留重复对局") @Nullable r: Boolean?
    ): ResponseEntity<ByteArray> {
        val image: ByteArray
        val match: Match

        try {
            match = matchApiService.getMatch(matchID.toLong(), 10)
        } catch (ex: WebClientResponseException) {
            throw RuntimeException(GeneralTipsException.Type.G_Null_MatchID.message)
        }

        try {
            val data = MatchNowService.calculate(
                MuRatingService.MuRatingPanelParam(
                    match, MatchRating.RatingParam(k ?: 0, d ?: 0, null, e ?: 1.0, f ?: true, r ?: true), false
                ), beatmapApiService, calculateApiService
            )
            image = imageService.getPanel(data, "F")
        } catch (err: Exception) {
            log.error("比赛结果：API 异常", err)
            throw RuntimeException(
                GeneralTipsException(
                    GeneralTipsException.Type.G_Malfunction_Render, "比赛结果"
                ).message
            )
        }

        return ResponseEntity(
            image, getImageHeader(
                "${matchID}-match.jpg", image.size
            ), HttpStatus.OK
        )
    }

    /**
     * 比赛评分图片接口 (RA)
     *
     * @param matchID 比赛编号
     * @param k       跳过开头对局数量，跳过热手
     * @param d       忽略结尾对局数量，忽略 TB 表演赛
     * @param f       是否删除低于 1w 分的成绩，不传默认删除
     * @param r       是否保留重复对局，不传默认保留
     * @return image 评分图片
     */
    @GetMapping(value = ["match/rating"]) @OpenResource(name = "ra", desp = "查询比赛评分") fun getRating(
        @OpenResource(name = "matchid", desp = "比赛编号", required = true) @RequestParam("id") matchID: Int,
        @OpenResource(name = "easy-multiplier", desp = "Easy 模组倍率") @Nullable e: Double?,
        @OpenResource(name = "skip", desp = "跳过开头") @Nullable k: Int?,
        @OpenResource(name = "ignore", desp = "忽略结尾") @Nullable d: Int?,
        @OpenResource(name = "delete-low", desp = "删除低分成绩") @Nullable f: Boolean?,
        @OpenResource(name = "keep-rematch", desp = "保留重复对局") @Nullable r: Boolean?
    ): ResponseEntity<ByteArray> {
        val image: ByteArray
        val match: Match

        try {
            match = matchApiService.getMatch(matchID.toLong(), 10)
        } catch (ex: WebClientResponseException) {
            throw RuntimeException(GeneralTipsException.Type.G_Null_MatchID.message)
        }

        try {
            val c = MuRatingService.calculate(
                MuRatingService.MuRatingPanelParam(
                    match, MatchRating.RatingParam(k ?: 0, d ?: 0, null, e ?: 1.0, f ?: true, r ?: true), false
                ), beatmapApiService, calculateApiService
            )
            image = imageService.getPanel(c, "C")
        } catch (err: Exception) {
            log.error("比赛评分：API 异常", err)
            throw RuntimeException(MRAException.Type.RATING_Send_MRAFailed.message)
        }

        return ResponseEntity(
            image, getImageHeader(
                "${matchID}-mra.jpg", image.size
            ), HttpStatus.OK
        )
    }

    /**
     * 生成图池接口 (GP)
     *
     * @param name    玩家名称
     * @param dataMap 需要传进来的图池请求体。结构是 {"HD": [114514, 1919810]} 组成的 Map
     * @return image 生成图池图片
     */
    @PostMapping(value = ["match/getpool"]) @Throws(RuntimeException::class) fun getPool(
        @RequestParam("name") @Nullable name: String?,
        @RequestParam("mode") @Nullable modeStr: String?,
        @RequestBody dataMap: Map<String, List<Long>>
    ): ResponseEntity<ByteArray> {
        val mode = getMode(modeStr)
        val mapPool = MapPoolDto(name, mode, dataMap, beatmapApiService, calculateApiService)
        if (mapPool.modPools.isEmpty()) throw RuntimeException(MapPoolException.Type.GP_Map_Empty.message)

        val body = mapOf(
            "pool" to mapPool, "mode" to mode.shortName
        )

        val image = imageService.getPanel(body, "H")
        return ResponseEntity(
            image, getImageHeader(
                "${mapPool.name}-pool.jpg", image.size
            ), HttpStatus.OK
        )
    }

    enum class ScoreType {
        TodayBP, BP, Pass, Recent, PassCard, RecentCard,
    }

    /**
     * 多组成绩接口（当然单成绩也行，我把接口改了）
     *
     * @param name     玩家名称
     * @param playMode 模式,可为空
     * @param type     scoreType
     * @param start    !bp 45-55 或 !bp 45 里的 45
     * @param end      !bp 45-55 里的 55
     * @return image 成绩图片
     */
    fun getScore(
        @RequestParam("name") name: String,
        @Nullable @RequestParam("mode") playMode: String?,
        @Nullable @RequestParam("type") type: ScoreType?,
        @Nullable @RequestParam("start") start: Int?,
        @Nullable @RequestParam("end") end: Int?
    ): ResponseEntity<ByteArray> {
        val mode = getMode(playMode)

        val osuUser = userApiService.getOsuUser(name.trim(), mode)
        val scores: List<LazerScore>

        val offset = parseRange2Offset(start, end)
        val limit = parseRange2Limit(start, end)

        val isMultipleScore = (limit > 1)

        //渲染面板
        val data: ByteArray
        val suffix: String

        when (type) {
            ScoreType.BP -> {
                scores = scoreApiService.getBestScores(osuUser.userID, mode, offset, limit)

                val ranks = ArrayList<Int>()
                for (i in offset..(offset + limit)) ranks.add(i + 1)

                if (isMultipleScore) {
                    calculateApiService.applyBeatMapChanges(scores)
                    calculateApiService.applyStarToScores(scores)

                    data = imageService.getPanel(
                        mapOf(
                            "user" to osuUser, "scores" to scores, "rank" to ranks, "panel" to "BS"
                        ), "A4"
                    )
                    suffix = "-bps.jpg"
                } else {
                    try {
                        val e5Param = ScorePRService.getE5Param(
                            osuUser, scores.first(), "B", beatmapApiService, calculateApiService
                        )
                        data = imageService.getPanel(e5Param.toMap(), "E5")
                    } catch (e: Exception) {
                        throw RuntimeException(
                            GeneralTipsException(
                                GeneralTipsException.Type.G_Malfunction_Render, "最好成绩"
                            )
                        )
                    }
                    suffix = "-bp.jpg"
                }
            }

            ScoreType.Pass -> {
                scores = scoreApiService.getPassedScore(osuUser.userID, mode, offset, limit)

                calculateApiService.applyBeatMapChanges(scores)
                calculateApiService.applyStarToScores(scores)

                val ranks = ((offset + 1)..scores.size).toList()

                if (isMultipleScore) {
                    data = imageService.getPanel(
                        mapOf(
                            "user" to osuUser, "score" to scores, "rank" to ranks, "panel" to "PS"
                        ), "A5"
                    )

                    suffix = "-passes.jpg"
                } else {
                    try {
                        val e5Param = ScorePRService.getE5Param(
                            osuUser, scores.first(), "P", beatmapApiService, calculateApiService
                        )
                        data = imageService.getPanel(e5Param.toMap(), "E5")
                    } catch (e: Exception) {
                        throw RuntimeException(
                            GeneralTipsException(
                                GeneralTipsException.Type.G_Malfunction_Render, "通过成绩"
                            )
                        )
                    }
                    suffix = "-pass.jpg"
                }
            }

            ScoreType.Recent -> {
                scores = scoreApiService.getPassedScore(osuUser.userID, mode, offset, limit)

                calculateApiService.applyBeatMapChanges(scores)
                calculateApiService.applyStarToScores(scores)

                if (isMultipleScore) {
                    val ranks = ((offset + 1)..scores.size).toList()

                    data = imageService.getPanel(
                        mapOf(
                            "user" to osuUser, "score" to scores, "rank" to ranks, "panel" to "RS"
                        ), "A5"
                    )

                    suffix = "-recents.jpg"
                } else {
                    try {
                        val e5Param = ScorePRService.getE5Param(
                            osuUser, scores.first(), "R", beatmapApiService, calculateApiService
                        )
                        data = imageService.getPanel(e5Param.toMap(), "E5")
                    } catch (e: Exception) {
                        throw RuntimeException(
                            GeneralTipsException(
                                GeneralTipsException.Type.G_Malfunction_Render, "最近成绩"
                            )
                        )
                    }
                    suffix = "-recent.jpg"
                }
            }

            ScoreType.PassCard -> {
                scores = scoreApiService.getScore(osuUser.userID, mode, offset, 1, true)
                val score: LazerScore = scores.first()

                calculateApiService.applyBeatMapChanges(scores)
                calculateApiService.applyStarToScores(scores)

                data = imageService.getPanelGamma(score)
                suffix = "-pass_card.jpg"
            }

            ScoreType.RecentCard -> {
                scores = scoreApiService.getScore(osuUser.userID, mode, offset, 1, false)
                val score: LazerScore = scores.first()

                calculateApiService.applyBeatMapChanges(scores)
                calculateApiService.applyStarToScores(scores)

                data = imageService.getPanelGamma(score)
                suffix = "-recent_card.jpg"
            }

            else -> { // 时间计算
                val day = max(min((start ?: 1), 999), 1)
                val dayBefore = OffsetDateTime.now().minusDays(day.toLong())

                val bests =
                    scoreApiService.getBestScores(osuUser.userID, mode).mapIndexed { i, it -> i + 1 to it }.toMap()
                        .filter { dayBefore.isBefore(it.value.endedTime) }

                //scoreList = BPList.stream().filter(s -> dayBefore.isBefore(s.getCreateTime())).toList();

                val ranks = bests.map { it.key }
                scores = bests.map { it.value }

                calculateApiService.applyBeatMapChanges(scores)
                calculateApiService.applyStarToScores(scores)

                data = imageService.getPanel(
                    mapOf(
                        "user" to osuUser, "scores" to scores, "rank" to ranks, "panel" to "T"
                    ), "A4"
                )
                suffix = "-todaybp.jpg"
            }
        }

        return ResponseEntity(data, getImageHeader(name + suffix, data.size), HttpStatus.OK)
    }

    /**
     * 今日最好成绩接口 (T)
     *
     * @param name     玩家名称
     * @param playMode 模式，可为空
     * @param day      天数，不传默认一天内
     * @return image 今日最好成绩图片
     */
    @GetMapping(value = ["bp/today"]) @OpenResource(name = "t", desp = "查询今日最好成绩") fun getTodayBP(
        @OpenResource(name = "name", desp = "玩家名称", required = true) @RequestParam("name") name: String,
        @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") playMode: String?,
        @OpenResource(name = "day", desp = "天数") @Nullable @RequestParam("day") day: Int?
    ): ResponseEntity<ByteArray> {
        return getScore(name, playMode, ScoreType.TodayBP, day, null)
    }

    @GetMapping(value = ["bp/scores"]) @OpenResource(name = "bs", desp = "查询多个最好成绩") fun getBPScores(
        @OpenResource(name = "name", desp = "玩家名称", required = true) @RequestParam("name") name: String,
        @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") playMode: String?,
        @OpenResource(name = "start", desp = "开始位置") @Nullable @RequestParam("start") start: Int?,
        @OpenResource(name = "end", desp = "结束位置") @Nullable @RequestParam("end") end: Int?
    ): ResponseEntity<ByteArray> {
        return getScore(name, playMode, ScoreType.BP, start, end)
    }

    /**
     * 多个最近通过成绩接口 (PS) 不计入 Failed 成绩
     *
     * @param name     玩家名称
     * @param playMode 模式，可为空
     * @param start    开始位置，不传默认 1
     * @param end      结束位置，不传默认等于开始位置
     * @return image 多个最近通过成绩图片
     */
    @GetMapping(value = ["score/passes"]) @OpenResource(name = "ps", desp = "查询多个最近通过成绩") fun getPassedScores(
        @OpenResource(name = "name", desp = "玩家名称", required = true) @RequestParam("name") name: String,
        @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") playMode: String?,
        @OpenResource(name = "start", desp = "开始位置") @Nullable @RequestParam("start") start: Int?,
        @OpenResource(name = "end", desp = "结束位置") @Nullable @RequestParam("end") end: Int?
    ): ResponseEntity<ByteArray> {
        return getScore(name, playMode, ScoreType.Pass, start, end)
    }

    /**
     * 多个最近成绩接口 (RS) 计入 Failed 成绩
     *
     * @param name     玩家名称
     * @param playMode 模式，可为空
     * @param start    开始位置，不传默认 1
     * @param end      结束位置，不传默认等于开始位置
     * @return image 多个最近成绩图片
     */
    @GetMapping(value = ["score/recents"]) @OpenResource(name = "rs", desp = "查询多个最近成绩") fun getRecentScores(
        @OpenResource(name = "name", desp = "玩家名称", required = true) @RequestParam("name") name: String,
        @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") playMode: String?,
        @OpenResource(name = "start", desp = "开始位置") @Nullable @RequestParam("start") start: Int?,
        @OpenResource(name = "end", desp = "结束位置") @Nullable @RequestParam("end") end: Int?
    ): ResponseEntity<ByteArray> {
        return getScore(name, playMode, ScoreType.Recent, start, end)
    }

    /**
     * 最好成绩接口 (B)
     *
     * @param name     玩家名称
     * @param playMode 模式，可为空
     * @param start    开始位置，不传默认 1
     * @return image 最好成绩图片
     */
    @GetMapping(value = ["bp"]) @OpenResource(name = "b", desp = "查询最好成绩") fun getBPScore(
        @OpenResource(name = "name", desp = "玩家名称", required = true) @RequestParam("name") name: String,
        @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") playMode: String?,
        @OpenResource(name = "start", desp = "位置") @Nullable @RequestParam("start") start: Int?
    ): ResponseEntity<ByteArray> {
        return getScore(name, playMode, ScoreType.BP, start, null)
    }

    /**
     * 最近通过成绩接口 (P) 不计入 Failed 成绩
     *
     * @param name     玩家名称
     * @param playMode 模式，可为空
     * @param start    开始位置，不传默认 1
     * @return image 最近通过成绩图片
     */
    @GetMapping(value = ["score/pass"]) @OpenResource(name = "p", desp = "查询最近通过成绩") fun getPassedScore(
        @OpenResource(name = "name", desp = "玩家名称", required = true) @RequestParam("name") name: String,
        @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") playMode: String?,
        @OpenResource(name = "start", desp = "位置") @Nullable @RequestParam("start") start: Int?
    ): ResponseEntity<ByteArray> {
        return getScore(name, playMode, ScoreType.Pass, start, null)
    }

    /**
     * 最近成绩接口 (R) 计入 Failed 成绩
     *
     * @param name     玩家名称
     * @param playMode 模式，可为空
     * @param start    开始位置，不传默认 1
     * @return image 最近成绩图片
     */
    @GetMapping(value = ["score/recent"]) @OpenResource(name = "r", desp = "查询最近成绩") fun getRecentScore(
        @OpenResource(name = "name", desp = "玩家名称", required = true) @RequestParam("name") name: String,
        @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") playMode: String?,
        @OpenResource(name = "start", desp = "位置") @Nullable @RequestParam("start") start: Int?
    ): ResponseEntity<ByteArray> {
        return getScore(name, playMode, ScoreType.Recent, start, null)
    }

    /**
     * 谱面成绩接口 (S)
     *
     * @param name     玩家名称
     * @param bid      谱面编号
     * @param playMode 模式，可为空
     * @param mods     模组字符串，可为空
     * @return image 谱面成绩图片
     */
    @GetMapping(value = ["score"]) @OpenResource(name = "s", desp = "查询玩家谱面成绩") fun getBeatMapScore(
        @OpenResource(name = "name", desp = "玩家名称", required = true) @RequestParam("name") name: String,
        @OpenResource(name = "bid", desp = "谱面编号", required = true) @RequestParam("bid") bid: Long,
        @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") playMode: String?,
        @OpenResource(name = "mods", desp = "模组") @Nullable @RequestParam("mods") mods: String?
    ): ResponseEntity<ByteArray> {
        val osuUser: OsuUser

        val mode = getMode(playMode)
        val uid: Long
        val modInt = if (mods.isNullOrBlank()) {
            LazerMod.getModsValue(mods)
        } else {
            0
        }

        val scoreList: List<LazerScore>
        var score: LazerScore? = null

        try {
            osuUser = userApiService.getOsuUser(name)
            uid = osuUser.userID
        } catch (e: WebClientResponseException.NotFound) {
            throw RuntimeException(GeneralTipsException(GeneralTipsException.Type.G_Null_Player, name))
        }

        if (mods.isNullOrBlank()) {
            score = scoreApiService.getBeatMapScore(bid, uid, mode)!!.score
        } else {
            try {
                scoreList = scoreApiService.getBeatMapScores(bid, uid, mode)
                for (s in scoreList) {
                    if (LazerMod.getModsValue(s.mods) == modInt) {
                        score = s
                        break
                    }
                }
            } catch (e: WebClientResponseException.NotFound) {
                throw RuntimeException(GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "成绩列表"))
            }
        }

        if (score == null) {
            throw RuntimeException(GeneralTipsException(GeneralTipsException.Type.G_Null_Score, bid.toString()))
        }

        val image: ByteArray

        try {
            val e5Param = ScorePRService.getE5Param(
                osuUser, score, "S", beatmapApiService, calculateApiService
            )
            image = imageService.getPanel(e5Param.toMap(), "E5")
        } catch (e: Exception) {
            throw RuntimeException(GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "成绩列表"))
        }
        return ResponseEntity(
            image, getImageHeader(
                "${name}@${bid}-score.jpg", image.size
            ), HttpStatus.OK
        )
    }

    /**
     * 最好成绩分析接口 (BA)
     *
     * @param name     玩家名称
     * @param playMode 模式，可为空
     * @return image 最好成绩分析图片
     */
    @GetMapping(value = ["bp/analysis"]) @OpenResource(name = "ba", desp = "最好成绩分析")
    @Throws(RuntimeException::class) fun getBPAnalysis(
        @OpenResource(name = "name", desp = "玩家名称", required = true) @NonNull @RequestParam("name") name: String,
        @OpenResource(name = "mode", desp = "游戏模式") @Nullable @RequestParam("mode") playMode: String?
    ): ResponseEntity<ByteArray> {
        val scores: List<LazerScore>
        val osuUser: OsuUser

        try {
            val mode = getMode(playMode)
            val uid = userApiService.getOsuID(name.trim())
            osuUser = userApiService.getOsuUser(uid, mode)
            if (mode != OsuMode.DEFAULT) osuUser.currentOsuMode = mode
            scores = scoreApiService.getBestScores(uid, mode)
        } catch (e: Exception) {
            throw RuntimeException(GeneralTipsException.Type.G_Fetch_List.message)
        }

        val data: Map<String, Any>
        try {
            data = BPAnalysisService.parseData(osuUser, scores, userApiService, 2)
        } catch (e: Exception) {
            throw RuntimeException(GeneralTipsException.Type.G_Fetch_BeatMapAttr.message)
        }

        val image = imageService.getPanel(data, "J2")
        return ResponseEntity(
            image, getImageHeader(
                "${name}-ba.jpg", image.size
            ), HttpStatus.OK
        )
    }

    /**
     * 扔骰子接口 (D)
     *
     * @param range   范围，支持 0 ~ 2147483647
     * @param compare 需要比较的文本，也可以把范围输入到这里来
     * @return String 扔骰子结果
     */
    @GetMapping(value = ["dice"]) @OpenResource(name = "d", desp = "扔骰子") @Throws(RuntimeException::class)
    fun getDice(
        @OpenResource(name = "range", desp = "范围") @RequestParam("range") @Nullable range: Int?,
        @OpenResource(name = "compare", desp = "需要比较的文本") @RequestParam("compare") @Nullable compare: String?
    ): ResponseEntity<ByteArray> {
        var message: String

        try {
            if (range == null) {
                if (compare.isNullOrBlank()) {
                    message = String.format("%.0f", DiceService.getRandom(100))
                } else {
                    val isOnlyNumbers = compare.matches("^[0-9.]+$".toRegex())

                    if (isOnlyNumbers) {
                        try {
                            val r = DiceService.getRandom(compare.toInt())

                            message = if (r <= 1) {
                                String.format("%.2f", r)
                            } else {
                                String.format("%.0f", r)
                            }
                        } catch (e: NumberFormatException) {
                            message = DiceService.compare(compare)
                        }
                    } else {
                        message = DiceService.compare(compare)
                    }
                }
            } else {
                val r = DiceService.getRandom(range)

                message = if (r <= 1) {
                    String.format("%.2f", r)
                } else {
                    String.format("%.0f", r)
                }
            }

            return ResponseEntity(message.toByteArray(StandardCharsets.UTF_8), HttpStatus.OK)
        } catch (e: DiceException) {
            return ResponseEntity(
                String.format("%.0f", DiceService.getRandom(100)).toByteArray(StandardCharsets.UTF_8), HttpStatus.OK
            )
        } catch (e: Exception) {
            log.error("扔骰子：API 异常", e)
            throw RuntimeException("扔骰子：API 异常")
        }
    }

    /***
     * 获取谱面信息 (M)
     * @param bid 谱面编号
     * @param modeStr 谱面模式
     * @param accuracy acc, 0-1的浮点数，或1-100，或101-10000
     * @param combo 最大连击
     * @param miss 失误数量
     * @param modStr 模组的字符串, 比如 HDHR 等
     * @return 谱面信息图片
     * @throws RuntimeException API 出错
     */
    @GetMapping(value = ["map"]) @OpenResource(name = "m", desp = "获取谱面信息") @Throws(RuntimeException::class)
    fun getMapInfo(
        @OpenResource(name = "bid", desp = "谱面编号") @RequestParam("bid") bid: Long,
        @OpenResource(name = "mode", desp = "游戏模式") @RequestParam("mode") @Nullable modeStr: String?,
        @OpenResource(
            name = "accuracy", desp = "准确率，允许输入 0-10000"
        ) @RequestParam("accuracy") @Nullable accuracy: Double?,
        @OpenResource(name = "combo", desp = "连击数") @RequestParam("combo") @Nullable combo: Int?,
        @OpenResource(name = "miss", desp = "失误数") @RequestParam("miss") @Nullable miss: Int?,
        @OpenResource(
            name = "mods", desp = "模组，允许按成对的双字母输入"
        ) @RequestParam("mods") @Nullable modStr: String?
    ): ResponseEntity<ByteArray> {

        try {
            val mode = getMode(modeStr, OsuMode.OSU)
            val beatMap = beatmapApiService.getBeatMap(bid)
            val mods = LazerMod.getModsList(modStr ?: "")

            val expected = MapStatisticsService.Expected(
                mode, accuracy ?: 1.0, combo ?: 0, miss ?: 0, mods, false
            )

            val image = MapStatisticsService.getPanelE6Image(
                null, beatMap, expected, beatmapApiService, calculateApiService, imageService
            )

            return ResponseEntity(
                image, getImageHeader(
                    "${bid}-mapinfo.jpg", image.size
                ), HttpStatus.OK
            )
        } catch (e: Exception) {
            log.error("谱面信息：API 异常", e)
            throw RuntimeException("谱面信息：API 异常")
        }
    }

    /**
     * 获取玩家信息 (I)
     *
     * @param uid     玩家编号
     * @param name    玩家名称
     * @param modeStr 游戏模式
     * @return 玩家信息图片
     */
    @GetMapping(value = ["info"]) @OpenResource(name = "i", desp = "获取玩家信息") fun getPlayerInfo(
        @OpenResource(name = "uid", desp = "玩家编号") @RequestParam("uid") @Nullable uid: Long?,
        @OpenResource(name = "name", desp = "玩家名称") @RequestParam("name") @Nullable name: String?,
        @OpenResource(name = "mode", desp = "游戏模式") @RequestParam("mode") @Nullable modeStr: String?,
        @OpenResource(name = "day", desp = "回溯天数") @RequestParam("day") @Nullable day: Int = 1
    ): ResponseEntity<ByteArray> {
        val user = getPlayerInfoJson(uid, name, modeStr)
        val mode = getMode(modeStr, user.currentOsuMode)

        val bests = scoreApiService.getBestScores(user)
        val historyUser = infoDao.getLastFrom(
            user.userID, mode, LocalDate.now().minusDays(day.toLong())
        ).map { OsuUserInfoDao.fromArchive(it) }.orElse(null)

        val param = InfoService.PanelDParam(user, historyUser, bests, user.currentOsuMode)

        val image = imageService.getPanel(param.toMap(), "D")

        return ResponseEntity(
            image, getImageHeader(
                "${user.userID}-info.jpg", image.size
            ), HttpStatus.OK
        )
    }

    /**
     * 获取谱师信息 (IM)
     *
     * @param uid  玩家编号
     * @param name 玩家名称
     * @return 谱师信息图片
     */
    @GetMapping(value = ["info/mapper"]) @OpenResource(name = "im", desp = "获取谱师信息") fun getMapperInfo(
        @OpenResource(name = "uid", desp = "玩家编号") @RequestParam("uid") @Nullable uid: Long?,
        @OpenResource(name = "name", desp = "玩家名称") @RequestParam("name") @Nullable name: String?
    ): ResponseEntity<ByteArray> {
        val osuUser = getPlayerInfoJson(uid, name, null)
        val data = IMapperService.getIMapperV1(
            osuUser, userApiService, beatmapApiService
        )
        val image = imageService.getPanel(data, "M")

        return ResponseEntity(
            image, getImageHeader(
                "${osuUser.userID}-mapper.jpg", image.size
            ), HttpStatus.OK
        )
    }

    /**
     * 获取提名信息 (N)
     *
     * @param sid 谱面集编号
     * @param bid 谱面编号
     * @return 提名信息图片
     */
    @GetMapping(value = ["map/nomination"]) @OpenResource(name = "n", desp = "获取提名信息")
    @Throws(RuntimeException::class) fun getNomination(
        @OpenResource(name = "sid", desp = "谱面集编号") @RequestParam("sid") @Nullable sid: Int?,
        @OpenResource(name = "bid", desp = "谱面编号") @RequestParam("bid") @Nullable bid: Int?
    ): ResponseEntity<ByteArray> {
        val data: Map<String, Any>

        try {
            data = if (sid == null) {
                if (bid == null) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Null_Map)
                } else {
                    NominationService.parseData(
                        bid.toLong(), false, beatmapApiService, discussionApiService, userApiService
                    )
                }
            } else {
                NominationService.parseData(
                    sid.toLong(), true, beatmapApiService, discussionApiService, userApiService
                )
            }
        } catch (e: GeneralTipsException) {
            throw RuntimeException(e.message)
        }

        try {
            val image = imageService.getPanel(data, "N")

            return ResponseEntity(
                image, getImageHeader(
                    "${sid ?: bid ?: 0}-nomination.jpg", image.size
                ), HttpStatus.OK
            )
        } catch (e: Exception) {
            log.error("提名信息：API 异常", e)
            throw RuntimeException("提名信息：API 异常")
        }
    }

    /**
     * 获取比赛结果文件 (CA)
     *
     * @return 比赛结果文件
     */
    @GetMapping(value = ["match/rating/csv"]) @OpenResource(name = "ca", desp = "获取比赛结果")
    @Throws(RuntimeException::class) fun getCsvRating(
        @OpenResource(name = "match", desp = "比赛编号（逗号分隔）") @RequestParam("match") @Nullable matchIDs: String?
    ): ResponseEntity<ByteArray> {
        try {
            val sb = StringBuilder()
            val ids = CsvMatchService.parseDataString(matchIDs)

            if (ids == null) {
                throw RuntimeException("请输入对局！")
            } else if (ids.size == 1) {
                CsvMatchService.parseCRA(
                    sb, ids.first(), matchApiService, beatmapApiService, calculateApiService
                )
            } else {
                CsvMatchService.parseCRAs(
                    sb, ids, matchApiService, beatmapApiService, calculateApiService
                )
            }

            val data = sb.toString().toByteArray()

            return ResponseEntity<ByteArray>(
                data, getByteHeader(ids.first().toString() + "-csvrating.csv", data.size), HttpStatus.OK
            )
        } catch (e: Exception) {
            log.error("比赛结果：API 异常", e)
            throw RuntimeException("比赛结果：API 异常")
        }
    }


    //======================  以下是跨域调用 osu!API 的部分，可获取到各种原始 JSON  ===================================
    /**
     * 获取玩家信息的 OsuUser JSON，即原 uui
     *
     * @param uid     玩家编号
     * @param name    玩家名称
     * @param modeStr 玩家模组
     * @return OsuUser JSON
     */
    @GetMapping(value = ["info/json"]) fun getPlayerInfoJson(
        @RequestParam("uid") @Nullable uid: Long?,
        @RequestParam("name") @Nullable name: String?,
        @RequestParam("mode") @Nullable modeStr: String?
    ): OsuUser {
        val mode = getMode(modeStr)
        return if (uid != null) {
            userApiService.getOsuUser(uid, mode)
        } else if (name.isNullOrBlank().not()) {
            userApiService.getOsuUser(name!!, mode)
        } else {
            userApiService.getOsuUser(17064371L, mode)
        }
    }

    /**
     * 获取谱面信息的 BeatMap JSON
     *
     * @param bid 谱面编号
     * @return BeatMap JSON
     */
    @GetMapping(value = ["map/json"]) fun getBeatMapInfoJson(
        @RequestParam("bid") @Nullable bid: Long?
    ): BeatMap {
        return if (bid != null) {
            beatmapApiService.getBeatMap(bid)
        } else {
            BeatMap()
        }
    }

    /**
     * 获取谱面的附加信息 Attr JSON
     *
     * @param bid  谱面编号
     * @param mods 模组字符串，通过逗号分隔
     * @param mode 游戏模式，默认为谱面自己的
     * @return BeatMap JSON
     */
    @GetMapping(value = ["attr/json"]) fun getDifficultyAttributes(
        @RequestParam("bid") @Nullable bid: Long?,
        @RequestParam("mods") @Nullable mods: String?,
        @RequestParam("mode") @Nullable mode: String?
    ): BeatmapDifficultyAttributes {
        return if (bid != null) {
            beatmapApiService.getAttributes(
                bid, getMode(mode), OsuMod.getModsValue(mods)
            )
        } else {
            BeatmapDifficultyAttributes()
        }
    }

    /**
     * 获取 BP 信息的 JSON
     *
     * @param uid     玩家编号
     * @param name    玩家名称
     * @param modeStr 玩家模组
     * @param start   !bp 45-55 或 !bp 45 里的 45
     * @param end     !bp 45-55 里的 55
     * @return OsuUser JSON
     */
    @GetMapping(value = ["bp/json"]) fun getBPJson(
        @RequestParam("uid") @Nullable uid: Long?,
        @RequestParam("name") @Nullable name: String?,
        @RequestParam("mode") @Nullable modeStr: String?,
        @RequestParam("start") @Nullable start: Int?,
        @RequestParam("end") @Nullable end: Int?
    ): List<LazerScore> {
        val mode = getMode(modeStr)

        val offset = parseRange2Offset(start, end)
        val limit = parseRange2Limit(start, end)

        if (uid != null) {
            return scoreApiService.getBestScores(uid, mode, offset, limit)
        } else if (name.isNullOrBlank().not()) {
            val user = userApiService.getOsuUser(name!!, mode)
            return scoreApiService.getBestScores(user, offset, limit)
        } else {
            return scoreApiService.getBestScores(7003013L, OsuMode.DEFAULT, offset, limit)
        }
    }

    /**
     * 获取成绩信息的 JSON
     *
     * @param uid      玩家编号
     * @param name     玩家名称
     * @param modeStr  游戏模式
     * @param start    !bp 45-55 或 !bp 45 里的 45
     * @param end      !bp 45-55 里的 55
     * @param isPass 是否通过，默认通过（真）
     * @return List<Score> JSON
    </Score> */
    @GetMapping(value = ["score/json"]) fun getScoreJson(
        @RequestParam("uid") @Nullable uid: Long?,
        @RequestParam("name") @Nullable name: String?,
        @RequestParam("mode") @Nullable modeStr: String?,
        @RequestParam("start") @Nullable start: Int?,
        @RequestParam("end") @Nullable end: Int?,
        @RequestParam("isPassed") @Nullable isPass: Boolean? = true
    ): List<LazerScore> {
        val mode = getMode(modeStr)

        val offset = parseRange2Offset(start, end)
        val limit = parseRange2Limit(start, end)

        if (uid != null) {
            return scoreApiService.getScore(uid, mode, offset, limit, isPass)
        } else if (name.isNullOrBlank().not()) {
            val user = userApiService.getOsuUser(name!!, mode)
            return scoreApiService.getScore(user.userID, mode, offset, limit, isPass)
        } else {
            return scoreApiService.getScore(7003013L, OsuMode.DEFAULT, offset, limit, isPass)
        }
    }

    /**
     * 登录, 向 bot 发送 !login 获得验证码, 验证码不区分大小写, 1分钟过期
     *
     * @param code 验证码
     */
    @GetMapping(value = ["login"]) fun doLogin(@RequestParam("code") @NonNull code: String): OsuUser {
        val u = LoginService.LOGIN_USER_MAP.getOrDefault(code.uppercase(Locale.getDefault()), null)
        if (u != null) {
            LoginService.LOGIN_USER_MAP.remove(code.uppercase(Locale.getDefault()))
            return userApiService.getOsuUser(u.uid)
        }
        throw RuntimeException("已过期或者不存在")
    }

    /**
     * 用于使用 go-cqhttp 发送文件的下载接口
     *
     * @param key file key
     * @return file
     */
    @GetMapping("file/{key}") fun downloadFile(@PathVariable("key") key: String?): ResponseEntity<ByteArray> {
        if (key.isNullOrBlank()) throw RuntimeException("链接不存在")
        val data = QQMsgUtil.getFileData(key) ?: throw RuntimeException("文件不存在")

        val headers = HttpHeaders()
        headers.contentDisposition = ContentDisposition.inline().filename(data.name).build()
        headers.contentType = MediaType.APPLICATION_OCTET_STREAM
        headers.contentLength = data.bytes.capacity().toLong()
        return ResponseEntity(data.bytes.array(), headers, HttpStatus.OK)
    }

    @GetMapping("log-level") fun setLoggerLever(
        @RequestParam("l") level: String?, @RequestParam("package") @Nullable packageName: String?
    ): String {
        val l = Level.toLevel(level, Level.INFO)
        if (packageName == null) {
            (LoggerFactory.getILoggerFactory() as LoggerContext).getLogger("com.now.nowbot").level = l
        } else {
            (LoggerFactory.getILoggerFactory() as LoggerContext).getLogger(packageName).level = l
        }
        log.trace("trace")
        log.debug("debug")
        log.info("info")
        log.warn("warn")
        log.error("error")

        return "ok - " + l.levelStr
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BotWebApi::class.java)

        private fun getImageHeader(name: String, length: Int): HttpHeaders {
            return getImageHeader(name, length.toLong())
        }

        private fun getByteHeader(name: String, length: Int): HttpHeaders {
            return getByteHeader(name, length.toLong())
        }

        private fun getImageHeader(name: String, length: Long): HttpHeaders {
            val headers = HttpHeaders()
            headers.contentDisposition = ContentDisposition.inline().filename(name).build()
            headers.contentLength = length
            headers.contentType = MediaType.IMAGE_JPEG
            return headers
        }

        private fun getByteHeader(name: String, length: Long): HttpHeaders {
            val headers = HttpHeaders()
            headers.contentDisposition = ContentDisposition.inline().filename(name).build()
            headers.contentLength = length
            headers.contentType = MediaType.APPLICATION_OCTET_STREAM
            return headers
        } /*

    @Resource
    Over6KUserService over6KUserService;

    @GetMapping("alumni")
    public Object getAlumni(@RequestParam(name = "start", defaultValue = "0") int start,
                            @RequestParam(name = "size", defaultValue = "30") int size) {
        try {
            return over6KUserService.getResultJson(start, size);
        } catch (IOException e) {
            log.error("alumni 文件异常", e);
            return "[]";
        }
    }

     */
    }
}

