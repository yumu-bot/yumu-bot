package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.BPService.BPParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithBackoff
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

@Service("BP") class BPService(
    private val calculateApiService: OsuCalculateApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val scoreApiService: OsuScoreApiService,
    private val imageService: ImageService,
) : MessageService<BPParam>, TencentMessageService<BPParam> {

    data class BPParam(val user: OsuUser?, val scores: List<LazerScore>, val isMyself: Boolean)

    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<BPParam>,
    ): Boolean {
        val matcher = Instruction.BP.matcher(messageText)
        if (!matcher.find()) return false

        val isMyself = AtomicBoolean() // 处理 range
        val mode = getMode(matcher)

        val range = getUserWithBackoff(event, matcher, mode, isMyself, messageText, "bp")

        val any = matcher.group("any")
        val conditions = DataUtil.paramMatcher(any, Filter.entries.map { it.regex })

        // 如果不加井号，则有时候范围会被匹配到这里来
        val rangeInConditions = conditions.lastOrNull()
        val hasRange = (rangeInConditions.isNullOrEmpty().not())
        val hasCondition = conditions.dropLast(1).sumOf { it.size } > 0

        val ranges = if (hasRange) rangeInConditions else matcher.group(FLAG_RANGE)?.split(REG_HYPHEN)

        val range2 = if (range.start != null) {
            range
        } else {
            CmdRange(range.data!!, ranges?.firstOrNull()?.toIntOrNull(), ranges?.lastOrNull()?.toIntOrNull())
        }

        val isMultiple = matcher.group("s").isNullOrBlank().not()
        val scores = range2.getBPScores(mode.data!!, isMultiple, hasCondition)

        val filteredScores = filterScores(scores, conditions)

        if (filteredScores.isEmpty()) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_FilterBP, range.data!!.username)
        }

        data.value = BPParam(range.data, filteredScores, isMyself.get())

        return true
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: BPParam) {
        val image: ByteArray = param.getImage()

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("最好成绩：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "最好成绩")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): BPParam? {
        val matcher1 = OfficialInstruction.BP.matcher(messageText)
        val matcher2 = OfficialInstruction.BPS.matcher(messageText)

        val matcher: Matcher
        val isMultiple: Boolean

        if (matcher1.find()) {
            matcher = matcher1
            isMultiple = false
        } else if (matcher2.find()) {
            matcher = matcher2
            isMultiple = true
        } else {
            return null
        }
        val isMyself = AtomicBoolean() // 处理 range
        val mode = getMode(matcher)

        val range = getUserWithBackoff(event, matcher, mode, isMyself, messageText, "bp")

        val any = matcher.group("any")
        val conditions = DataUtil.paramMatcher(any, Filter.entries.map { it.regex })

        // 如果不加井号，则有时候范围会被匹配到这里来
        val rangeInConditions = conditions.lastOrNull()
        val hasRange = (rangeInConditions.isNullOrEmpty().not())
        val hasCondition = conditions.dropLast(1).sumOf { it.size } > 0

        val ranges = if (hasRange) rangeInConditions else matcher.group(FLAG_RANGE)?.split(REG_HYPHEN)

        val range2 = if (range.start != null) {
            range
        } else {
            CmdRange(range.data!!, ranges?.firstOrNull()?.toIntOrNull(), ranges?.lastOrNull()?.toIntOrNull())
        }

        val scores = range2.getBPScores(mode.data!!, isMultiple, hasCondition)

        val filteredScores = filterScores(scores, conditions)

        if (filteredScores.isEmpty()) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_FilterBP, range.data!!.username)
        }

        return BPParam(range.data, filteredScores, isMyself.get())
    }

    override fun reply(event: MessageEvent, param: BPParam): MessageChain? = QQMsgUtil.getImage(param.getImage())

    enum class Operator(@Language("RegExp") val regex: Regex) {
        // 不等于
        NE("$REG_EXCLAMATION$REG_EQUAL".toRegex()),

        // 完全等于
        XQ("$REG_EQUAL$REG_EQUAL".toRegex()),

        // 等于
        EQ(REG_EQUAL.toRegex()),

        // 大于等于
        GE("$REG_GREATER$REG_EQUAL".toRegex()),

        // 大于
        GT(REG_GREATER.toRegex()),

        // 小于等于
        LE("$REG_LESS$REG_EQUAL".toRegex()),

        // 小于
        LT(REG_LESS.toRegex()),
    }

    enum class Filter(@Language("RegExp") val regex: Regex) {
        MAPPER("(mapper|creator|host|u)(?<n>$REG_OPERATOR$REG_NAME)".toRegex()),

        SCORE_ID("(score|scoreid|s)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

        TITLE("(title|name|song|t)(?<n>$REG_OPERATOR$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

        ARTIST("(artist|f)(?<n>$REG_OPERATOR$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

        STAR("(star|sr|s)(?<n>$REG_OPERATOR$REG_NUMBER_DECIMAL)".toRegex()),

        // 既然都整合了，为什么还要用 index ?
        INDEX("(index|i)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

        AR("(ar|approach)(?<n>$REG_OPERATOR$REG_NUMBER_DECIMAL)".toRegex()),

        CS("(cs|circle|keys?|k)(?<n>$REG_OPERATOR$REG_NUMBER_DECIMAL)".toRegex()),

        OD("(od|difficulty)(?<n>$REG_OPERATOR$REG_NUMBER_DECIMAL)".toRegex()),

        HP("(hp|health)(?<n>$REG_OPERATOR$REG_NUMBER_DECIMAL)".toRegex()),

        PERFORMANCE("(performance|pp|p)(?<n>$REG_OPERATOR$REG_NUMBER_DECIMAL)".toRegex()),

        RANK("(rank(ing)?|r)(?<n>$REG_OPERATOR$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

        LENGTH("(length|drain|time|l)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE($REG_COLON$REG_NUMBER$LEVEL_MORE)?)".toRegex()),

        BPM("(bpm|b)(?<n>$REG_OPERATOR$REG_NUMBER_DECIMAL)".toRegex()),

        ACCURACY("(accuracy|acc|a)(?<n>$REG_OPERATOR$REG_NUMBER_DECIMAL)".toRegex()),

        COMBO("(combo|cb?)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

        PERFECT("(perfect|320|305|pf)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

        GREAT("(great|300|良|gr)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

        GOOD("(good|200|gd)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

        OK("(ok|150|100|可|ba?d)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

        MEH("(meh|p(oo)?r|50)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

        MISS("(m(is)?s|0|x|不可)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

        MOD("(m(od)?s?)(?<n>$REG_OPERATOR$REG_MOD$LEVEL_MORE)".toRegex()),

        RATE("(rate|e|pm)(?<n>$REG_OPERATOR$REG_NUMBER_DECIMAL)".toRegex()),

        CLIENT("(client|z|v|version)(?<n>$REG_OPERATOR$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

        RANGE("(\\d{1,2}$REG_HYPHEN)?(100|\\d{1,2})".toRegex())
    }

    private fun CmdRange<OsuUser>.getBPScores(
        mode: OsuMode,
        isMultiple: Boolean,
        isSearch: Boolean = false,
    ): List<LazerScore> {
        val offset: Int
        val limit: Int

        if (isSearch) {
            offset = 0
            limit = 100
        } else if (isMultiple) {
            offset = getOffset(0, true)
            limit = getLimit(20, true)
        } else {
            offset = getOffset(0, false)
            limit = getLimit(1, false)
        }

        val isDefault = offset == 0 && limit == 1

        val scores = scoreApiService.getBestScores(data!!.userID, mode, offset, limit)

        calculateApiService.applyStarToScores(scores)
        calculateApiService.applyBeatMapChanges(scores)

        val modeStr = if (mode == OsuMode.DEFAULT) {
            scores.firstOrNull()?.mode?.getName() ?: "默认"
        } else {
            mode.getName()
        }

        // 检查查到的数据是否为空
        if (scores.isEmpty()) {
            if (isDefault) {
                throw GeneralTipsException(
                    GeneralTipsException.Type.G_Null_PlayerRecord,
                    modeStr,
                )
            } else {
                throw GeneralTipsException(
                    GeneralTipsException.Type.G_Null_SelectedBP,
                    data!!.username ?: data!!.userID,
                    modeStr,
                )
            }
        }

        return scores
    }

    private fun BPParam.getImage(): ByteArray = try {
        if (scores.size > 1) {
            val ranks = List(scores.size) { it + 1 }
            val scores = this.scores

            imageService.getPanelA4(user, scores, ranks, "BS")
        } else {
            val score: LazerScore = scores.first()

            val e5Param = ScorePRService.getScore4PanelE5(user!!, score, "B", beatmapApiService, calculateApiService)
            imageService.getPanel(e5Param.toMap(), "E5")
        }
    } catch (e: Exception) {
        log.error("最好成绩：渲染失败", e)
        throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "最好成绩")
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BPService::class.java)

        private fun filterScores(scores: List<LazerScore>, conditions: List<List<String>>): List<LazerScore> {
            val s = scores.toMutableList()

            conditions.forEachIndexed { index, strings ->
                if (strings.isNotEmpty()) {
                    filterConditions(s, Filter.entries.toList()[index], strings)
                }
            }

            return s.toList()
        }

        private fun getOperator(string: String): Operator {
            Operator.entries.forEach {
                if (string.contains(it.regex)) return it
            }

            throw GeneralTipsException(GeneralTipsException.Type.G_Wrong_ParamOperator)
        }

        private fun filterConditions(scores: MutableList<LazerScore>, filter: Filter, conditions: List<String>) {
            for (c in conditions) {
                val operator = getOperator(c)
                val condition = c.split("[<>＜＞=＝!！]".toRegex()).lastOrNull() ?: ""

                scores.removeIf {
                    fitScore(it, operator, filter, condition).not()
                }
            }
        }

        private fun fitScore(it: LazerScore, operator: Operator, filter: Filter, condition: String): Boolean {
            val long = condition.toLongOrNull() ?: -1L
            val double = condition.toDoubleOrNull() ?: -1.0

            return when (filter) {
                Filter.MAPPER -> fit(operator, it.beatMapSet.creator, condition)
                Filter.SCORE_ID -> fit(operator, it.scoreID, long)
                Filter.TITLE -> (fit(operator, it.beatMapSet.title, condition)
                        || fit(operator, it.beatMapSet.titleUnicode, condition))
                Filter.ARTIST -> (fit(operator, it.beatMapSet.artist, condition)
                        || fit(operator, it.beatMapSet.artistUnicode, condition))

                Filter.STAR -> fit(operator, it.beatMap.starRating, double)

                Filter.AR -> fit(operator, it.beatMap.AR?.toDouble() ?: 0.0, double)
                Filter.CS -> fit(operator, it.beatMap.CS?.toDouble() ?: 0.0, double)
                Filter.OD -> fit(operator, it.beatMap.OD?.toDouble() ?: 0.0, double)
                Filter.HP -> fit(operator, it.beatMap.HP?.toDouble() ?: 0.0, double)
                Filter.PERFORMANCE -> fit(operator, it.PP ?: 0.0, double)
                Filter.RANK -> run {
                    val rank = when(condition.lowercase()) {
                        "ssh" -> "xh"
                        "ss" -> "x"
                        else -> condition.lowercase()
                    }

                    fit(operator, it.rank, rank)
                }
                Filter.LENGTH -> run {
                    var seconds = 0L
                    if (condition.contains(REG_COLON.toRegex())) {
                        val strs = condition.split(REG_COLON)
                        var parseMinute = true

                        for (s in strs) {
                            if (s.contains(REG_NUMBER_DECIMAL.toRegex())) {
                                seconds += if (parseMinute) {
                                    s.toLong()
                                } else {
                                    s.toLong() * 60L
                                }

                                parseMinute = false
                            }
                        }

                    } else {
                        seconds = condition.toLong()
                    }

                    fit(operator, it.beatMap.totalLength.toLong(), seconds)
                }

                Filter.BPM -> fit(operator, it.beatMap.BPM?.toDouble() ?: 0.0, double, isPlus = true)
                Filter.ACCURACY -> run {
                    val acc = when {
                        double > 10000.0 -> throw GeneralTipsException(GeneralTipsException.Type.G_Exceed_Param)
                        double >= 100.0 -> double / 10000.0
                        double >= 1.0 -> double / 100.0
                        else -> double
                    }

                    fit(operator, it.accuracy, acc, isPlus = true)
                }
                Filter.COMBO -> run {
                    val combo = when {
                        double <= 1.0 && double > 0.0 -> it.beatMap.maxCombo?.times(double)?.roundToLong() ?: long
                        else -> long
                    }

                    fit(operator, it.maxCombo.toLong(), combo)
                }
                Filter.PERFECT -> fit(operator, it.statistics.perfect.toLong(), long)
                Filter.GREAT -> fit(operator,  it.statistics.great.toLong(), long)
                Filter.GOOD -> fit(operator,  it.statistics.good.toLong(), long)
                Filter.OK -> fit(operator,  it.statistics.ok.toLong(), long)
                Filter.MEH -> fit(operator,  it.statistics.meh.toLong(), long)
                Filter.MISS -> fit(operator,  it.statistics.miss.toLong(), long)
                Filter.MOD -> run {
                    val mods = LazerMod.getModsList(condition)

                    when (operator) {
                        Operator.XQ -> LazerMod.hasMod(mods, it.mods) && (mods.size == it.mods.size)
                        Operator.EQ -> LazerMod.hasMod(mods, it.mods)
                        Operator.NE -> LazerMod.hasMod(mods, it.mods).not()
                        else -> throw GeneralTipsException(
                            GeneralTipsException.Type.G_Wrong_ParamOnly, ">, >=, <, <="
                        )
                    }
                }

                Filter.RATE -> fit(operator, (it.statistics.perfect / it.statistics.great), double, isPlus = true)

                Filter.CLIENT -> when (condition.trim().lowercase()) {
                    "lazer", "l", "lz", "lzr" -> it.isLazer
                    "stable", "s", "st", "stb" -> !it.isLazer
                    else -> !it.isLazer
                }

                else -> false
            }
        }

        private fun fit(operator: Operator, compare: Any, to: Any, isPlus: Boolean = false): Boolean {
            return if (compare is Long && to is Long) {
                val c: Long = compare
                val t: Long = to

                when (operator) {
                    Operator.XQ, Operator.EQ -> c == t
                    Operator.NE -> c != t
                    Operator.GT -> c > t
                    Operator.GE -> c >= t
                    Operator.LT -> c < t
                    Operator.LE -> c <= t
                }
            } else if (compare is Double && to is Double) {
                val c: Double = compare
                val t: Double = to

                val d = abs(c - t)

                // 如果输入的特别接近整数，则判断是这个值到这个值 +1 的范围（不包含）
                when (operator) {
                    Operator.XQ -> d < 1e-8
                    Operator.EQ -> if (isPlus && abs(c) - floor(abs(c)) < 1e-4) {
                        c <= t && (c + 1.0) > t
                    } else {
                        d < 1e-4
                    }

                    Operator.NE -> d > 1e-4
                    Operator.GT -> c > t
                    Operator.GE -> c >= t
                    Operator.LT -> c < t
                    Operator.LE -> c <= t
                }
            } else if (compare is String && to is String) {
                val c: String = compare.trim()
                val t: String = to.trim()

                when (operator) {
                    Operator.XQ -> c.equals(t, ignoreCase = true)
                    Operator.EQ -> t.contains(c, ignoreCase = true)
                    Operator.NE -> t.contains(c, ignoreCase = true).not()
                    else -> throw GeneralTipsException(
                        GeneralTipsException.Type.G_Wrong_ParamOnly, ">, >=, <, <="
                    )
                }
            } else {
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Calculate, "最好成绩")
            }
        }
    }
}
