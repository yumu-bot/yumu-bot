package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.CoverType
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.filter.ScoreFilter
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.ScorePRService.ScorePRParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserAndRangeWithBackoff
import com.now.nowbot.util.command.FLAG_RANGE
import com.now.nowbot.util.command.REG_HYPHEN
import com.now.nowbot.util.command.REG_RANGE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher
import kotlin.collections.last

@Service("UU_PR")
class UUPRService(
    private val scoreApiService: OsuScoreApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val userApiService: OsuUserApiService,
) : MessageService<ScorePRParam>, TencentMessageService<ScorePRParam> {

    @Throws(Throwable::class)
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: DataValue<ScorePRParam>
    ): Boolean {
        val matcher = Instruction.UU_PR.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        val isPass =
            if (matcher.group("recent") != null) {
                false
            } else if (matcher.group("pass") != null) {
                true
            } else {
                log.error("最近成绩分类失败：")
                throw IllegalStateException.ClassCast("最近成绩")
            }

        data.value = getParam(event, messageText, matcher, isPass)

        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: ScorePRParam): ServiceCallStatistic? {
        val message = param.getUUMessageChain()

        // 单成绩发送
        try {
            event.reply(message)
        } catch (e: Exception) {
            log.error("最近成绩文字：发送失败", e)
            event.reply("最近成绩文字：发送失败，请重试。")
        }

        val pairs = param.scores.toList().take(5)

        return ServiceCallStatistic.builds(event,
            beatmapIDs = pairs.map { it.second.beatmapID },
            beatmapsetIDs = pairs.map { it.second.beatmapset.beatmapsetID },
            userIDs = listOf(param.user.userID),
            modes = listOf(param.user.currentOsuMode)
        )
    }

    override fun accept(
        event: MessageEvent,
        messageText: String
    ): ScorePRParam? {
        val matcher = Instruction.UU_PR.matcher(messageText)

        if (!matcher.find()) {
            return null
        }

        val isPass =
            if (matcher.group("recent") != null) {
                false
            } else if (matcher.group("pass") != null) {
                true
            } else {
                log.error("腾讯最近成绩分类失败：")
                throw IllegalStateException.ClassCast("最近成绩")
            }

        return getParam(event, messageText, matcher, isPass)
    }

    override fun reply(
        event: MessageEvent,
        param: ScorePRParam
    ): MessageChain? {
        return param.getUUMessageChain()
    }

    private fun getParam(event: MessageEvent, messageText: String, matcher: Matcher, isPass: Boolean): ScorePRParam? {
        val any: String = matcher.group("any") ?: ""

        // 避免指令冲突
        if (any.contains("&sb", ignoreCase = true)) return null

        val isMyself = AtomicBoolean(true) // 处理 range
        val mode = getMode(matcher)

        val id = UserIDUtil.getUserIDWithRange(event, matcher, mode, isMyself)

        id.setZeroToRange100()

        val conditions = DataUtil.paramMatcher(any, ScoreFilter.entries.map { it.regex }, REG_RANGE.toRegex())

        // 如果不加井号，则有时候范围会被匹配到这里来
        val rangeInConditions = conditions.lastOrNull()?.firstOrNull()
        val hasRangeInConditions = (rangeInConditions.isNullOrEmpty().not())
        val hasCondition = conditions.dropLast(1).sumOf { it.size } > 0

        if (hasRangeInConditions.not() && hasCondition.not() && any.isNotBlank()) {
            throw IllegalArgumentException.WrongException.Cabbage()
        }

        val ranges = if (hasRangeInConditions) {
            rangeInConditions
        } else {
            matcher.group(FLAG_RANGE)
        }?.split(REG_HYPHEN.toRegex())

        val user: OsuUser
        val scores: Map<Int, LazerScore>

        // 高效的获取方式
        if (id.data != null) {
            val id2 = if (id.start != null) {
                id
            } else {
                val start = ranges?.firstOrNull()?.toIntOrNull()
                val end = if (ranges?.size == 2) {
                    ranges.last().toIntOrNull()
                } else {
                    null
                }

                CmdRange(id.data!!, start, end)
            }


            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { userApiService.getOsuUser(id2.data!!, mode.data!!) },
                { id2.getRecentsFromUserID(mode.data ?: OsuMode.DEFAULT, false, hasCondition, isPass) }
            )

            user = async.first
            scores = async.second
        } else {
            // 经典的获取方式

            val range = getUserAndRangeWithBackoff(event, matcher, mode, isMyself, messageText, "re")
            range.setZeroToRange100()

            val range2 = if (range.start != null) {
                range
            } else {
                val start = ranges?.firstOrNull()?.toIntOrNull()
                val end = if (ranges?.size == 2) {
                    ranges.last().toIntOrNull()
                } else {
                    null
                }

                CmdRange(range.data!!, start, end)
            }

            user = range2.data!!

            scores = range2.getRecentsFromOsuUser(mode.data ?: OsuMode.DEFAULT, false, hasCondition, isPass)
        }

        val filteredScores = ScoreFilter.filterScores(scores, conditions)

        if (filteredScores.isEmpty()) {
            if (isPass) {
                throw NoSuchElementException.PassedScoreFiltered(user.username, user.currentOsuMode)
            } else {
                throw NoSuchElementException.RecentScoreFiltered(user.username, user.currentOsuMode)
            }
        }

        return ScorePRParam(user, filteredScores, isPass, false)
    }

    private fun CmdRange<OsuUser>.getRecentsFromOsuUser(
        mode: OsuMode,
        isMultiple: Boolean,
        isSearch: Boolean = false,
        isPass: Boolean = false,
    ): Map<Int, LazerScore> {

        val offset: Int
        val limit: Int

        if (isSearch && this.start == null) {
            offset = 0
            limit = 100
        } else if (isMultiple) {
            offset = getOffset(0, true)
            limit = getLimit(20, true)
        } else {
            offset = getOffset(0, false)
            limit = getLimit(1, false)
        }

        val scores = scoreApiService.getScore(data!!.userID, mode, offset, limit, isPass)

        calculateApiService.applyStarToScores(scores)
        calculateApiService.applyBeatMapChanges(scores)

        // 检查查到的数据是否为空
        if (scores.isEmpty()) {
            if (isPass) {
                throw NoSuchElementException.RecentScore(data!!.username, data!!.currentOsuMode)
            } else {
                throw NoSuchElementException.PassedScore(data!!.username, data!!.currentOsuMode)
            }
        }

        return scores.mapIndexed { index, score -> (index + offset + 1) to score }.toMap()
    }


    private fun CmdRange<Long>.getRecentsFromUserID(
        mode: OsuMode,
        isMultiple: Boolean,
        isSearch: Boolean = false,
        isPass: Boolean = false,
    ): Map<Int, LazerScore> {
        val o = this.getOffsetAndLimit(isMultiple, isSearch)

        val offset: Int = o.first
        val limit: Int = o.second

        val scores = scoreApiService.getScore(this.data!!, mode, offset, limit, isPass)

        // 检查查到的数据是否为空
        if (scores.isEmpty()) {
            if (isPass) {
                throw NoSuchElementException.RecentScore(this.data!!.toString(), mode)
            } else {
                throw NoSuchElementException.PassedScore(this.data!!.toString(), mode)
            }
        }

        calculateApiService.applyStarToScores(scores)
        calculateApiService.applyBeatMapChanges(scores)

        return scores.mapIndexed { index, score -> (index + offset + 1) to score }.toMap()
    }


    private fun <T> CmdRange<T>.getOffsetAndLimit(
        isMultiple: Boolean,
        isSearch: Boolean = false,
    ): Pair<Int, Int> {
        val offset: Int
        val limit: Int

        if (isSearch && this.start == null) {
            offset = 0
            limit = 100
        } else if (isMultiple) {
            offset = getOffset(0, true)
            limit = getLimit(20, true)
        } else {
            offset = getOffset(0, false)
            limit = getLimit(1, false)
        }

        return offset to limit
    }

    /*
    @Deprecated("已过时")
    private fun getTextOutput(score: LazerScore, event: MessageEvent) {
        val d = UUScore(score, beatmapApiService, calculateApiService)

        val img = try {
            osuApiWebClient.get()
                .uri(d.url ?: "")
                .retrieve()
                .bodyToMono(ByteArray::class.java)
                .block()
        } catch (_: Exception) {
            try {
                Files.readAllBytes(Path.of(System.getenv("EXPORT_FILE_V3") + "avatar_guest.jpg"))
            } catch (_: IOException) {
                throw NoSuchElementException.Avatar()
            }
        }
        event.reply(img, d.scoreLegacyOutput)
    }

     */

    private fun ScorePRParam.getUUMessageChain(): MessageChain {
        return if (scores.size > 1) {
            val list = scores.toList().take(5)
            val ss = list.map { it.second }

            AsyncMethodExecutor.awaitPairCallableExecute (
                { beatmapApiService.applyBeatmapExtend(ss) },
                { calculateApiService.applyPPToScores(ss) },
            )

            val covers = scoreApiService.getCovers(ss, CoverType.COVER_2X)

            getUUScores(user, list, covers)
        } else {

            val s = scores.toList().take(1).first().second

            val cover = scoreApiService.getCover(s, CoverType.COVER_2X)

            AsyncMethodExecutor.awaitPairCallableExecute (
                { beatmapApiService.applyBeatmapExtend(s) },
                { calculateApiService.applyPPToScore(s) },
            )

            getUUScore(user, s, cover)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(UUPRService::class.java)

        fun getUUScores(user: OsuUser, scores: List<Pair<Int, LazerScore>>, covers: List<ByteArray>): MessageChain {
            /*
             * Muziyami (taiko):
             *
             */

            val sb = MessageChain.MessageChainBuilder()

            sb.addText("${user.username} (${user.currentOsuMode.shortName}):\n\n")

            scores.mapIndexed { i, (rk, s) ->
                val cover = covers.getOrNull(i)

                val image = cover ?: try {
                    Files.readAllBytes(
                        Path.of(NowbotConfig.EXPORT_FILE_PATH).resolve("Banner").resolve("c8.png")
                    )
                } catch (_: IOException) {
                    byteArrayOf()
                }

                val info = getUUScoresInfo(s, rk)

                sb.addImage(image).addText("\n").addText(info)

                if (i != scores.size - 1) {
                    sb.addText("\n\n")
                }
            }

            return sb.build()
        }

        /**
         * 获取多成绩时的单一切片（不包含图片）
         */
        fun getUUScoresInfo(score: LazerScore, rank: Int = 1): String {
            /*
             * [封面]
             * #1 塞壬唱片-MSR - ......已至。 [......Oni.]
             * ★★★☆ 3.86* [2:51]
             * [B] 167x / 620x // 91.45% // 88 PP
             */

            val b = score.beatmap
            val sr = b.starRating

            val starInt = sr.toInt()
            val starHalf = sr % 1.0

            val stars = "★".repeat(starInt) + if (starHalf in 0.5..< 1.0) {
                "☆"
            } else {
                ""
            }

            val rating = String.format("%.2f", sr) + "*"

            val time = "${b.totalLength / 60}:" + String.format("%02d", b.totalLength % 60)

            val pp = String.format("%.2f", score.pp)

            val acc = String.format("%.2f", score.accuracy * 100.0)

            val maxCombo = when {
                (score.mode == OsuMode.CATCH || score.mode == OsuMode.CATCH_RELAX) && b.convert == true
                    -> (b.maxCombo ?: 0) - (b.spinners ?: 0)

                (score.mode == OsuMode.MANIA) ->
                    score.maximumStatistics.perfect + score.maximumStatistics.legacyComboIncrease

                else -> b.maxCombo ?: 0
            }

            val info = """
                #$rank ${score.previewName}
                $stars $rating [$time]
                [${score.rank}] ${score.maxCombo}x / ${maxCombo}x // ${acc}% // $pp PP
            """.trimIndent()


            return info
        }

        /**
         * 这里的 score 需要 applyBeatmapExtend 和 applyPPToScore
         */
        fun getUUScore(user: OsuUser, score: LazerScore, cover: ByteArray?): MessageChain {
            val image = cover ?: try {
                Files.readAllBytes(
                    Path.of(NowbotConfig.EXPORT_FILE_PATH).resolve("Banner").resolve("c8.png")
                )
            } catch (_: IOException) {
                byteArrayOf()
            }


            /*
             * [封面]
             * 塞壬唱片-MSR - ......已至。 [......Oni.]
             * ★★★☆ 3.86* [2:51]
             *
             * Muziyami (taiko): 88 PP
             * [B] 39'4850
             * 167x / 620x // 91.45%
             * 541 / 52 / 27
             *
             * ID: 5232618
             */

            val b = score.beatmap
            val sr = b.starRating

            val starInt = sr.toInt()
            val starHalf = sr % 1.0

            val stars = "★".repeat(starInt) + if (starHalf in 0.5..< 1.0) {
                "☆"
            } else {
                ""
            }

            val rating = String.format("%.2f", sr) + "*"

            val time = "${b.totalLength / 60}:" + String.format("%02d", b.totalLength % 60)

            val pp = String.format("%.2f", score.pp)

            val sc = String.format("%,d", score.score)

            val acc = String.format("%.2f", score.accuracy * 100.0)

            val maxCombo = when {
                (score.mode == OsuMode.CATCH || score.mode == OsuMode.CATCH_RELAX) && b.convert == true
                    -> (b.maxCombo ?: 0) - (b.spinners ?: 0)

                (score.mode == OsuMode.MANIA) ->
                    score.maximumStatistics.perfect + score.maximumStatistics.legacyComboIncrease

                else -> b.maxCombo ?: 0
            }

            val t = score.statistics

            val statistics = if (score.isLazer) {
                when(score.mode) {
                    OsuMode.TAIKO, OsuMode.TAIKO_RELAX
                        -> "${t.great} / ${t.ok} / ${t.miss}"

                    OsuMode.MANIA
                        -> "${t.great} (+${t.perfect} [${getPerfectGreatRate(t.perfect, t.great)}]) / ${t.good} / ${t.ok} / ${t.meh} / ${t.miss}"

                    OsuMode.CATCH, OsuMode.CATCH_RELAX
                        -> "${t.great} / ${t.largeTickHit} / ${t.smallTickHit} / ${t.miss} (-${t.smallTickMiss})"

                    else -> "${t.great} / ${t.ok} / ${t.meh} / ${t.miss}"
                }
            } else {
                when(score.mode) {
                    OsuMode.TAIKO, OsuMode.TAIKO_RELAX
                        -> "${t.great} / ${t.ok} / ${t.miss}"

                    OsuMode.MANIA
                        -> "${t.great} (+${t.perfect} [${getPerfectGreatRate(t.perfect, t.great)}]) / ${t.good} / ${t.ok} / ${t.meh} / ${t.miss}"

                    OsuMode.CATCH, OsuMode.CATCH_RELAX
                        -> "${t.great} / ${t.largeTickHit} / ${t.smallTickHit} / ${t.miss} (-${t.smallTickMiss})"

                    else -> "${t.great} / ${t.ok} / ${t.meh} / ${t.miss}"
                }
            }

            val extendedStatistics = if (score.isLazer) {
                val m = score.maximumStatistics

                when(score.mode) {
                    OsuMode.OSU, OsuMode.OSU_AUTOPILOT, OsuMode.OSU_RELAX
                        -> "[TK: ${t.largeTickHit} / ED: ${t.sliderTailHit} / O+: ${t.largeBonus}, O?: ${t.smallBonus}]"

                    OsuMode.TAIKO, OsuMode.TAIKO_RELAX
                        -> "[O+: ${t.largeBonus} / ==: ${t.smallBonus} / (): ${t.ignoreHit}]"

                    OsuMode.CATCH, OsuMode.CATCH_RELAX
                        -> "[BN: ${t.largeBonus}/${m.largeBonus} ${String.format("%.2f", t.largeBonus.toDouble() / m.largeBonus)}%]"

                    else -> ""
                }
            } else {
                ""
            } + "\n"

            val info = """
                ${score.previewName}
                $stars $rating [$time]
                
                ${user.username} (${score.mode.shortName}): $pp PP
                [${score.rank}] $sc
                
                ${score.maxCombo}x / ${maxCombo}x // ${acc}%
                $statistics
            """.trimIndent()

            return MessageChain(info + "\n" + extendedStatistics + "\nID: ${b.beatmapID}", image)
        }

        private fun getPerfectGreatRate(perfect: Int, great: Int): String {
            if (great == 0) {
                return "Inf.x"
            } else if (perfect == 0) {
                return "0x"
            }

            return when(val r = perfect.toDouble() / great) {
                in 0.9..1.1 -> String.format("%.2f", r) + "x"
                else -> String.format("%.1f", r) + "x"
            }
        }
    }
}