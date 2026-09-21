package com.now.nowbot.model.filter

import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.enums.OsuGenre
import com.now.nowbot.model.enums.OsuLanguage
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerMod.Companion.toLazerModAcronyms
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.StringUtil.standardised
import com.now.nowbot.util.TimeParser
import com.now.nowbot.util.command.*

import org.intellij.lang.annotations.Language
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

enum class ScoreFilter(@param:Language("RegExp") val regex: Regex) {
    CREATOR("(creator|host|c|谱师|作者|谱|主)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NAME)".toRegex()),

    GUEST("((gd(er)?|guest\\s*diff(er)?)|mapper|guest|g?u|客串?(谱师)?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NAME)".toRegex()),

    BID("((beatmap\\s*)?id|bid|b|(谱面)?编?号)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    SID("((beatmap\\s*)?setid|sid|s|(谱面)?集编号)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    TITLE("(title|name|song|t|歌?曲名|歌曲|标题)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    ARTIST("(artist|singer|art|f?a|艺术家|曲师?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    SOURCE("(source|src|from|f|o|sc|se|来?源)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    TAG("(tags?|ta|tg|w|标签?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    ANY("(any(thing)?|y|任[何意]?(字段|文字)?|[字文])(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    GENRE("(genre|g|曲?风|风格|流派?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    LANGUAGE("(languages?|l|la|曲?风|风格|流派?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    DIFFICULTY("(difficult(y|ies)|diff|d|难度名?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    STAR("(stars?|rating|sr|r|星数?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)$PATTERN_STAR$LEVEL_MAYBE".toRegex()),

    SCORE("(scores?|sc|ss|e|分数?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    REPLAY("(replay|re?p|journal|j|回放|录像|记录)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    AR("(ar|approach\\s*(rate)?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    CS("(cs|circle\\s*(size)?|keys?|键)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    OD("(od|overall\\s*(difficulty)?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    HP("(hp|health\\s*(point)?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    PERFORMANCE("(performance|表现分?|pp|p)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    RANK("(rank(ing|s)?|评[价级]?|k)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    LENGTH("(length|drain|long|duration|长度|时?长|lh|h)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_TIME)".toRegex()),

    BPM("(bpm|曲速|速度|bm)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    ACCURACY("(accuracy|精[确准][率度]?|准确?[率度]|acc?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)[%％]?".toRegex()),

    COMBO("(combo|连击|cb?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL[xX]?)".toRegex()),

    PERFECT("(perfect|320|305|彩|完美|pf)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    GREAT("(great|300|大果?|fruits?|fr|良|黄|gr|很好)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    MISSED_FRUIT("(miss(ed)?\\s*fruits?|漏大果?|mf)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    MISSED_DROP("(miss(ed)?\\s*drop|漏中果?|mp)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    MISSED_DROPLET("(miss(ed)?\\s*droplet|漏小?果?|md)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    GOOD("(good|200|绿|gd|良好)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    OK("(ok|150|100|中果?|large\\s*drop(let)?|ld|(?<!不)可|蓝|ba?d|可以)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    MEH("(me?h|小果?|drop(let)?|sd|p(oo)?r|灰|50|一般)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    MISS("(m(is)?s|0|x|不可|红|失误|漏击)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    MOD("((m(od)?s?)|模组?)(?<n>($PATTERN_OPERATOR_WITH_SPACE|$PATTERN_PLUS)$PATTERN_MOD$LEVEL_MORE)".toRegex()),

    RATE("(rate|彩[率比]|黄彩比?|q|pm)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    CIRCLE("((hit)?circles?|hi?t|click|rice|ci|cr|rc|圆圈?|米)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    SLIDER("(slider?s?|sl|long(note)?|lns?|[滑长]?条|长键|面)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    SPINNER("(spin(ner)?s?|rattle|sp|转盘|[转盘])(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    TOTAL("(notes?|total|all|ttl|(hit)?objects?|tt|n|(物件|音符)数?|总数?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    CONVERT("(convert|cv|转谱?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    CLIENT("(client|z|v|version|版本?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    CREATED_TIME("(date|(created|score)?\\s*(at|time)|creat(ed)?\\s*(at|time)?|(成绩(创建)?|创建)?(时间)?|st|ct|ca|ti)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_TIME)".toRegex()),

    RANGE(PATTERN_RANGE.toRegex());
    
    companion object {
        val regexes: List<Regex> by lazy { entries.map { it.regex } }

        fun filterScores(scores: List<LazerScore>, conditions: List<List<String>>): List<LazerScore> {
            val predicates = parsePredicates(conditions)
            if (predicates.isEmpty()) return scores

            return scores.filter { score ->
                predicates.all { predicate -> predicate(score) }
            }
        }

        fun <K> filterScores(scores: Map<K, LazerScore>, conditions: List<List<String>>): Map<K, LazerScore> {
            val predicates = parsePredicates(conditions)
            if (predicates.isEmpty()) return scores

            return scores.filterValues { score ->
                predicates.all { predicate -> predicate(score) }
            }
        }

        /**
         * 将多维 conditions 解析为一组可复用的谓词函数 (Score -> Boolean)
         */
        private fun parsePredicates(conditions: List<List<String>>): List<(LazerScore) -> Boolean> {
            val predicates = mutableListOf<(LazerScore) -> Boolean>()

            // 使用 subList 避免 dropLast 带来的内存拷贝
            val validConditions = if (conditions.isNotEmpty()) conditions.subList(0, conditions.size - 1) else emptyList()

            for ((index, strings) in validConditions.withIndex()) {
                if (strings.isEmpty()) continue

                val filter = entries.getOrNull(index) ?: continue
                for (c in strings) {
                    val operator = Operator.getOperator(c)
                    val conditionStr = c.split(REGEX_OPERATOR_WITH_SPACE).lastOrNull().orEmpty()
                    val condition = Condition(conditionStr)

                    predicates.add { score -> fitScore(score, operator, filter, condition) }
                }
            }
            return predicates
        }

        /**
         * @param compare 被比较的数据
         * @param to 输入的数据，这里认为你已经在外面 standardized 了，否则开销是 O(N^2)
         */
        fun fit(
            operator: Operator,
            compare: Any?,
            to: Any?,
        ): Boolean {
            return !(compare == null || to == null) && when (compare) {
                is Number if to is Number -> {
                    if (isIntegral(compare) && isIntegral(to)) {
                        compareLongs(operator, compare.toLong(), to.toLong())
                    } else if (compare is BigDecimal && to is BigDecimal) {
                        compareDecimals(operator, compare, to)
                    } else if (to is BigDecimal) {
                        compareDecimals(operator, compare.toString().toBigDecimal(), to)
                    } else {
                        compareDecimals(operator, BigDecimal(compare.toDouble()), BigDecimal(to.toDouble()))
                    }
                }

                is String if to is String -> {
                    val cs = compare.standardised()

                    when (operator) {
                        Operator.XQ -> to.equals(cs, ignoreCase = true)
                        Operator.EQ -> cs.contains(to, ignoreCase = true)
                        Operator.NE -> !cs.contains(to, ignoreCase = true)
                        Operator.GT -> to.contains(cs, ignoreCase = true) && to.length > cs.length
                        Operator.GE -> to.contains(cs, ignoreCase = true) && to.length >= cs.length
                        Operator.LT -> cs.contains(to, ignoreCase = true) && to.length < cs.length
                        Operator.LE -> cs.contains(to, ignoreCase = true) && to.length <= cs.length
                    }
                }

                is Boolean if to is Boolean -> {
                    when (operator) {
                        Operator.XQ, Operator.EQ -> compare == to
                        Operator.NE -> compare != to
                        else -> throw IllegalArgumentException.WrongException.OperatorOnly("==", "=", "!=")
                    }
                }

                is Enum<*> if to is Enum<*> -> {
                    compareLongs(operator, compare.ordinal.toLong(), to.ordinal.toLong())
                }

                is List<*> if to is List<*> -> {
                    val cs = compare.filterNotNull().toSet()
                    val ts = to.filterNotNull().toSet()

                    when (operator) {
                        Operator.XQ -> cs == ts
                        Operator.EQ -> cs.containsAll(ts)
                        Operator.NE -> !cs.containsAll(ts)
                        Operator.GT -> ts.containsAll(cs) && ts.size > cs.size
                        Operator.GE -> ts.containsAll(cs) && ts.size >= cs.size
                        Operator.LT -> cs.containsAll(ts) && cs.size < ts.size
                        Operator.LE -> cs.containsAll(ts) && cs.size <= ts.size
                    }
                }

                is Comparable<*> if compare::class == to::class -> {
                    @Suppress("UNCHECKED_CAST")
                    val c = compare as Comparable<Any>
                    val cmp = c.compareTo(to)
                    when (operator) {
                        Operator.XQ, Operator.EQ -> cmp == 0
                        Operator.NE -> cmp != 0
                        Operator.GT -> cmp > 0
                        Operator.GE -> cmp >= 0
                        Operator.LT -> cmp < 0
                        Operator.LE -> cmp <= 0
                    }
                }

                else -> false
            }
        }

        private fun isIntegral(n: Number): Boolean =
            n is Long || n is Int || n is Short || n is Byte

        // 辅助方法：Long 集中比较
        private fun compareLongs(operator: Operator, c: Long, t: Long): Boolean {
            return when (operator) {
                Operator.XQ, Operator.EQ -> c == t
                Operator.NE -> c != t
                Operator.GT -> c > t
                Operator.GE -> c >= t
                Operator.LT -> c < t
                Operator.LE -> c <= t
            }
        }

        private val epsilon = BigDecimal("0.0001")

        private fun compareDecimals(
            operator: Operator,
            compare: BigDecimal,
            to: BigDecimal
        ): Boolean {
            // 获取目标值的保留小数位数（避免 scale 为负数的情况，例如 1E2）
            val dig = to.scale().coerceIn(0, 6)

            // 1. 消除 Double 转换带来的尾数噪声（预平滑处理）
            // Double 有效精度约为 15-17 位，这里保留 6 位小数并用 HALF_UP 规整
            // 5.999999999999999 -> 6.0000000000
            // 6.999999999999999 -> 7.0000000000
            val cleanedCompare = compare.setScale(6, RoundingMode.HALF_UP)

            // 2. 将规整后的值按目标 scale (dig) 进行 FLOOR 截断处理
            val normCompare = cleanedCompare.setScale(dig, RoundingMode.FLOOR)
            val normTo = to.setScale(dig, RoundingMode.FLOOR)

            return when (operator) {
                // EQ：匹配 [to, to + 10^-dig) 区间
                // 示例 (dig=0): to=6 时，[6.0, 7.0) 范围均返回 true
                // 示例 (dig=2): to=6.00 时，[6.00, 6.01) 范围均返回 true
                Operator.EQ -> normCompare.compareTo(normTo) == 0
                Operator.NE -> normCompare.compareTo(normTo) != 0

                Operator.LE -> compare <= to
                Operator.GT -> compare > to
                Operator.GE -> compare >= to
                Operator.LT -> compare < to

                Operator.XQ -> (compare - to).abs() <= epsilon
            }
        }

        private fun fitScore(it: LazerScore, operator: Operator, filter: ScoreFilter, condition: Condition): Boolean {
            val long = condition.long
            val double = condition.double
            val str = condition.condition
            val decimal = condition.decimal

            return when (filter) {
                CREATOR -> fit(operator, it.beatmapset.creator, str)

                GUEST -> if (! it.beatmap.owners.isNullOrEmpty()) {
                    if (long > 0L) {
                        val ids = it.beatmap.owners!!.map { fit(operator, it.userID, long) }.toSet()
                        val names = it.beatmap.owners!!.map { fit(operator, it.username, str) }.toSet()

                        ids.contains(element = true) || names.contains(element = true)
                    } else {
                        val names = it.beatmap.owners!!.map { fit(operator, it.username, str) }.toSet()

                        names.contains(element = true)
                    }
                } else {
                    fit(operator, it.beatmapset.creator, str)
                }

                BID -> fit(operator, it.beatmapID, long)
                SID -> fit(operator, it.beatmapset.beatmapsetID, long)
                TITLE -> (fit(operator, it.beatmapset.title, str)
                        || fit(operator, it.beatmapset.titleUnicode, str))
                ARTIST -> (fit(operator, it.beatmapset.artist, str)
                        || fit(operator, it.beatmapset.artistUnicode, str))
                SOURCE -> fit(operator, it.beatmapset.source, str)
                TAG -> fitTags(operator, it.beatmapset.tags, str)

                ANY -> {
                    fitTags(operator, it.beatmapset.tags, str)
                            || fit(operator, it.beatmapset.title, str)
                            || fit(operator, it.beatmapset.titleUnicode, str)
                            || fit(operator, it.beatmapset.artist, str)
                            || fit(operator, it.beatmapset.artistUnicode, str)
                            || fit(operator, it.beatmapset.source, str)
                }

                GENRE -> fit(operator, it.beatmapset.genreID.toInt(), OsuGenre.getByte(str)?.toInt() ?: return false)
                LANGUAGE -> fit(operator, it.beatmapset.languageID.toInt(), OsuLanguage.getByte(str)?.toInt() ?: return false)

                DIFFICULTY -> fit(operator, it.beatmap.difficultyName, str)

                STAR -> fit(operator, it.beatmap.starRating.toBigDecimal(), decimal)

                SCORE -> fit(operator, it.score, long)

                REPLAY -> fit(operator, it.replay, !(str == "false" || str == "f") || long > 0)

                AR -> fit(operator, it.beatmap.ar?.toBigDecimal(), decimal)
                CS -> fit(operator, it.beatmap.cs?.toBigDecimal(), decimal)
                OD -> fit(operator, it.beatmap.od?.toBigDecimal(), decimal)
                HP -> fit(operator, it.beatmap.hp?.toBigDecimal(), decimal)
                PERFORMANCE -> fit(operator, it.pp.roundToLong(), long)
                RANK -> {
                    val rankArray = arrayOf("F", "D", "C", "B", "A", "S", "SH", "X", "XH")

                    val cr = rankArray.indexOf(
                        when(str.uppercase()) {
                            "SSH" -> "XH"
                            "SS" -> "X"
                            else -> str.uppercase()
                        }
                    )

                    val ir = rankArray.indexOf(it.rank.uppercase())

                    if (cr == -1) {
                        throw IllegalArgumentException.WrongException.Rank()
                    }

                    fit(operator, ir.toLong(), cr.toLong())
                }

                LENGTH -> {
                    val seconds = str.filter { it.isDigit() }.toLongOrNull() ?: return false

                    fit(operator, it.beatmap.totalLength.toLong(), seconds)
                }

                BPM -> fit(operator, it.beatmap.bpm.toBigDecimal(), decimal)
                ACCURACY -> {
                    val acc = when {
                        double > 10000.0 || double <= 0.0 -> throw IllegalArgumentException.WrongException.Henan()
                        double > 100.0 -> double / 10000.0
                        double > 1.0 -> double / 100.0
                        else -> double
                    } // 0-1

                    fit(operator, it.accuracy, acc)
                }

                COMBO -> fitCountOrPercent(operator, it.maxCombo, decimal, it.beatmap.maxCombo)

                PERFECT -> it.mode == OsuMode.MANIA && fitCountOrPercent(operator, it.statistics.perfect, decimal, it.maximumStatistics.perfect)
                GREAT -> fitCountOrPercent(operator, it.statistics.great, decimal, it.maximumStatistics.great)
                GOOD -> it.mode == OsuMode.MANIA && fitCountOrPercent(operator, it.statistics.good, decimal, it.maximumStatistics.good)

                OK -> if (it.mode != OsuMode.CATCH && it.mode != OsuMode.CATCH_RELAX) {
                    fitCountOrPercent(operator, it.statistics.ok, decimal, it.maximumStatistics.ok)
                } else {
                    fitCountOrPercent(operator, it.statistics.ok, decimal, it.maximumStatistics.largeTickHit)
                }

                MEH -> if (it.mode != OsuMode.CATCH && it.mode != OsuMode.CATCH_RELAX) {
                    fitCountOrPercent(operator, it.statistics.meh, decimal, it.maximumStatistics.meh)
                } else {
                    fitCountOrPercent(operator, it.statistics.meh, decimal, it.maximumStatistics.smallTickHit)
                }

                MISS -> fitCountOrPercent(operator, it.statistics.miss, decimal, it.maximumStatistics.miss)

                MISSED_FRUIT -> {
                    val compare = if (it.isLazer) {
                        it.statistics.miss
                    } else {
                        it.statistics.miss - it.statistics.largeTickMiss
                    }

                    (it.mode == OsuMode.CATCH || it.mode == OsuMode.CATCH_RELAX) && fitCountOrPercent(operator, compare, decimal, it.maximumStatistics.great)
                }

                MISSED_DROP -> (it.mode == OsuMode.CATCH || it.mode == OsuMode.CATCH_RELAX) && it.maximumStatistics.largeTickHit > 0 &&
                        fitCountOrPercent(operator, it.statistics.largeTickMiss, decimal, it.maximumStatistics.largeTickHit)

                MISSED_DROPLET -> (it.mode == OsuMode.CATCH || it.mode == OsuMode.CATCH_RELAX) && it.maximumStatistics.smallTickHit > 0 &&
                        fitCountOrPercent(operator, it.statistics.smallTickMiss, decimal, it.maximumStatistics.smallTickHit)

                MOD -> fitMod(operator, str, it.mods)

                RATE -> {
                    if (it.mode != OsuMode.MANIA) throw IllegalArgumentException.WrongException.Mode()

                    val rate = min((it.statistics.perfect * 1.0 / it.statistics.great), 100.0)
                    val input = if (double > 0.0) min(double, 100.0) else double

                    fit(operator, rate, input)
                }

                CIRCLE -> fitCountOrPercent(operator, it.beatmap.circles, decimal, it.beatmap.totalNotes)
                SLIDER -> fitCountOrPercent(operator, it.beatmap.sliders, decimal, it.beatmap.totalNotes)
                SPINNER -> fitCountOrPercent(operator, it.beatmap.spinners, decimal, it.beatmap.totalNotes)

                TOTAL -> {
                    val total = it.beatmap.totalNotes

                    total != 0 && fit(operator, total, long)
                }

                CONVERT -> when (str.trim().lowercase()) {
                    "true", "t", "yes", "y" -> it.beatmap.convert == true
                    "false", "f", "no", "not", "n" -> it.beatmap.convert == false
                    else -> it.beatmap.convert == false
                }

                CLIENT -> when (str.trim().lowercase()) {
                    "lazer", "l", "lz", "lzr" -> it.isLazer
                    "stable", "s", "st", "stb" -> !it.isLazer
                    else -> !it.isLazer
                }

                CREATED_TIME -> fitTime(operator, it.endedTime.atZoneSameInstant(ZoneOffset.UTC).toEpochSecond(), str)

                else -> false
            }
        }

        fun fitTags(operator: Operator, tags: String, to: String): Boolean {
            if (tags.isBlank() || to.isBlank()) return false

            val tagList = tags.replace('_', ' ')
                .split(REGEX_SPACE_MORE)
                .filter { it.isNotEmpty() }
            val toList = to.split(REGEX_SPACE_MORE)
                .filter { it.isNotEmpty() }

            return when {
                toList.size == 1 -> {
                    val target = toList.first()
                    tagList.any { tag -> fit(operator, tag, target) }
                }

                else -> {
                    fit(operator, tagList, toList)
                }
            }
        }

        /**
         * 公用方法
         * 在 to 含有小数点时，按 compare 占 total 的百分比来处理。在其他情况时，按 compare 整数来处理。
         */
        fun fitCountOrPercent(operator: Operator, compare: Number?, to: BigDecimal, total: Number?): Boolean {
            if (compare == null) return false

            val cleanedTo = to.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros()

            // 2. 判定是否含有有效小数：只需看剔除尾随 0 后的 scale 是否 > 0
            val hasDecimal = cleanedTo.scale() > 0

            // 3. 判断是否满足百分比模式条件：含有有效小数 且 在 [0.0, 1.0] 之间 且 不是 XQ
            val isPercentMode = hasDecimal
                    && cleanedTo >= BigDecimal.ZERO
                    && cleanedTo <= BigDecimal.ONE
                    && operator != Operator.XQ

            return if (isPercentMode) {
                val totalBd = BigDecimal(total.toString())
                if (totalBd.abs() < epsilon) {
                    false
                } else {
                    val compareBd = BigDecimal(compare.toString())

                    val ratio = compareBd.divide(totalBd, MathContext.DECIMAL64)
                        .setScale(6, RoundingMode.HALF_UP)
                        .stripTrailingZeros()

                    // 传入高精度的 fit / compareDecimals 函数进行比对
                    fit(operator, ratio, cleanedTo)
                }
            } else {
                // 整数模式：直接转为 Long 比较
                fit(operator, compare.toLong(), cleanedTo.toLong())
            }
        }

        fun fitTime(operator: Operator, compare: Long?, to: String): Boolean {
            val time = TimeParser.process(to) // 返回的是 ZonedDateTime
            val timeInstant = time.toInstant() // 转换为 UTC 瞬时点

            val nowInstant = Instant.now() // 获取当前 UTC 瞬时点

            if (nowInstant.isBefore(timeInstant)) {
                throw UnsupportedOperationException.InvalidFuture()
            }
            val isRelative = TimeParser.isRelativeTime(to)

            val toSec = timeInstant.toEpochMilli() / 1000L

            val sec = compare ?: 0L

            return when (operator) {
                Operator.GT -> {
                    if (isRelative) sec < toSec else sec > toSec
                }
                Operator.GE -> {
                    if (isRelative) sec <= toSec else sec >= toSec
                }
                Operator.LT -> {
                    if (isRelative) sec > toSec else sec < toSec
                }
                Operator.LE -> {
                    if (isRelative) sec >= toSec else sec <= toSec
                }

                else -> {
                    // 1. 将毫秒值重新解析为当前时区的日期时间
                    val zonedDateTime = Instant.ofEpochMilli(toSec)
                        .atZone(ZoneId.systemDefault())

                    // 2. 强制对齐到 0 点 (例如 15:30:25 -> 00:00:00)
                    val startOfDay = zonedDateTime.toLocalDate().atStartOfDay(ZoneId.systemDefault())
                    val startSec = startOfDay.toInstant().toEpochMilli() / 1000L

                    when(operator) {
                        Operator.EQ -> sec in startSec until (startSec + 1.days.inWholeSeconds)
                        Operator.XQ -> sec in startSec until (startSec + 1.hours.inWholeSeconds)
                        Operator.NE -> sec !in startSec until (startSec + 1.days.inWholeSeconds)
                    }
                }
            }
        }

        fun fitMod(operator: Operator, compare: String, to: List<LazerMod>): Boolean {
            val com = compare.toLazerModAcronyms().toSet()
            val too = to
                .map { it.acronym }.filter { it != LazerMod.Classic.type }.toSet()

            return if (compare.isEmpty() || compare.contains(LazerMod.NoMod.type, ignoreCase = true)) {
                when (operator) {
                    Operator.XQ, Operator.EQ -> too.isEmpty()
                    Operator.NE, Operator.GE, Operator.GT -> too.isNotEmpty()
                    else -> false
                }
            } else if (compare.contains(LazerMod.FreeMod.type, ignoreCase = true)) {
                when (operator) {
                    Operator.XQ, Operator.EQ -> too.isNotEmpty()
                    Operator.NE, Operator.LE, Operator.LT -> too.isEmpty()
                    else -> false
                }
            } else {
                val ins = com.intersect(too)

                when (operator) {
                    Operator.XQ -> com == too
                    Operator.EQ,
                    Operator.GE -> ins.size == com.size
                    Operator.GT -> ins.size == com.size && com.size < too.size
                    Operator.LE -> ins.size == too.size
                    Operator.LT -> ins.size == too.size && com.size > too.size
                    Operator.NE -> ins.isEmpty()
                }
            }
        }

        // private val log = LoggerFactory.getLogger(ScoreFilter::class.java)
    }
}