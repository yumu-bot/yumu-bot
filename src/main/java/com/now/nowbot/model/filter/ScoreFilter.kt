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

    SCORE("(scores?|sc|ss|e|分数?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)$PATTERN_STAR$LEVEL_MAYBE".toRegex()),

    REPLAY("(replay|rep|rp|回放|录像|记录)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)$PATTERN_STAR$LEVEL_MAYBE".toRegex()),

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

    TOTAL("(total|all|ttl|(hit)?objects?|tt|物件数?|总数?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    CONVERT("(convert|cv|转谱?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    CLIENT("(client|z|v|version|版本?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    CREATED_TIME("(date|(created|score)?\\s*(at|time)|creat(ed)?\\s*(at|time)?|(成绩(创建)?|创建)?(时间)?|st|ct|ca|ti)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_TIME)".toRegex()),

    RANGE(PATTERN_RANGE.toRegex());
    
    companion object {
        val regexes: List<Regex> by lazy { entries.map { it.regex } }

        fun filterScores(scores: Map<Int, LazerScore>, conditions: List<List<String>>): Map<Int, LazerScore> {
            val s = scores.toMutableMap()

            // 最后一个筛选条件无需匹配
            conditions
                .dropLast(1)
                .forEachIndexed { index, strings ->
                if (strings.isNotEmpty()) {
                    filterConditions(s, entries[index], strings)
                }
            }

            return s.toMap()
        }

        private fun filterConditions(scores: MutableMap<Int, LazerScore>, filter: ScoreFilter, conditions: List<String>) {
            for (c in conditions) {
                val operator = Operator.getOperator(c)
                val condition = Condition((c.split(REGEX_OPERATOR_WITH_SPACE).lastOrNull() ?: ""))

                scores.entries.removeIf { fitScore(it.value, operator, filter, condition).not() }
            }
        }

        /**
         * @param compare 被比较的数据
         * @param to 输入的数据，这里认为你已经在外面 standardized 了，否则开销是 O(N^2)
         * @param digit 需要比较的位数。比如星数，这里就需要输入 2，这样假设条件是 star=7.27，会返回 7.27 ..< 7.28 的谱面。
         * @param isRound 如果为真，则会按照四舍五入的方式处理 compare（比如表现分）。否则按照向下取整的方式处理 compare（比如星数或者准确率）。
         * @param isInteger 如果为真，则会在 to 接近某位时，digit 按当前位数处理。
         * 如果当前位数小于 digit，则 isRound 会设定为假（默认 floor）
         * 此设置只会影响 EQ 运算符。假如 star=7.1，此时会返回 7.10 ..< 7.20 的谱面。XQ 不受影响。
         * 如果您需要比较 0-1 之间的数据，这个最好设为假。
         */
        fun fit(
            operator: Operator,
            compare: Any?,
            to: Any?,
            digit: Int = 0,
            isRound: Boolean = true,
            isInteger: Boolean = true,
        ): Boolean {
            return !(compare == null || to == null) && when (compare) {
                is Number if to is Number -> {
                    if (isIntegral(compare) && isIntegral(to)) {
                        compareLongs(operator, compare.toLong(), to.toLong())
                    } else {
                        compareDoubles(operator, compare.toDouble(), to.toDouble(), digit, isRound, isInteger)
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

        // 辅助方法：Double 集中比较（修正了取 abs() 的 Bug，保留符号）
        private fun compareDoubles(
            operator: Operator,
            compare: Double,
            to: Double,
            digit: Int,
            isRound: Boolean,
            isInteger: Boolean
        ): Boolean {

            // 1. 确定保留的小数位数 dig
            val dig = if (isInteger) {
                // 将 to 格式化为最多 digit 位小数的字符串，保留末尾的 0（如 7.0 就是 "7.0"）
                val formatted = String.format("%.${digit}f", to).trimEnd('0')
                val dotIndex = formatted.indexOf('.')

                val actualDigits = if (dotIndex >= 0) {
                    formatted.substring(dotIndex + 1).length
                } else {
                    0
                }

                // 如果玩家输入的是 7.0，actualDigits 为 1；如果输入的是 7.27，actualDigits 为 2
                actualDigits.coerceAtMost(digit)
            } else {
                digit
            }

            val scale = 10.0.pow(dig)
            val rc = if (isRound && digit == dig) {
                round(compare * scale) / scale
            } else {
                floor(compare * scale) / scale
            }

            val eps = 1e-9
            val diff = rc - to

            return when (operator) {
                Operator.XQ -> abs(compare - to) <= eps
                Operator.EQ -> abs(diff) <= eps
                Operator.NE -> abs(diff) > eps
                Operator.GT -> diff > eps
                Operator.GE -> diff >= -eps
                Operator.LT -> diff < -eps
                Operator.LE -> diff <= eps
            }
        }

        private fun fitScore(it: LazerScore, operator: Operator, filter: ScoreFilter, condition: Condition): Boolean {
            val long = condition.long
            val double = condition.double
            val str = condition.condition
            val dec = condition.hasDecimal

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

                STAR -> fit(operator, it.beatmap.starRating, double, digit = 2, isRound = false, isInteger = true)

                SCORE -> fit(operator, it.score, long)

                REPLAY -> fit(operator, it.replay, !(str == "false" || str == "f") || long > 0)

                AR -> fit(operator, it.beatmap.ar?.toDouble() ?: 0.0, double, digit = 2, isRound = true, isInteger = true)
                CS -> fit(operator, it.beatmap.cs?.toDouble() ?: 0.0, double, digit = 2, isRound = true, isInteger = true)
                OD -> fit(operator, it.beatmap.od?.toDouble() ?: 0.0, double, digit = 2, isRound = true, isInteger = true)
                HP -> fit(operator, it.beatmap.hp?.toDouble() ?: 0.0, double, digit = 2, isRound = true, isInteger = true)
                PERFORMANCE -> fit(operator, it.pp.roundToLong(), long) //fit(operator, it.pp, double, digit = 0, isRound = true, isInteger = true)
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

                BPM -> fit(operator, it.beatmap.bpm.toDouble(), double, digit = 2, isRound = true, isInteger = true)
                ACCURACY -> {
                    val acc = when {
                        double > 10000.0 || double <= 0.0 -> throw IllegalArgumentException.WrongException.Henan()
                        double > 100.0 -> double / 10000.0
                        double > 1.0 -> double / 100.0
                        else -> double
                    } // 0-1

                    fit(operator, it.accuracy, acc, digit = 2, isRound = true, isInteger = true)
                }

                COMBO -> fitCountOrPercent(operator, it.maxCombo, double, it.beatmap.maxCombo, dec)

                PERFECT -> it.mode == OsuMode.MANIA && fitCountOrPercent(operator, it.statistics.perfect, double, it.maximumStatistics.perfect, dec)
                GREAT -> fitCountOrPercent(operator, it.statistics.great, double, it.maximumStatistics.great, dec)
                GOOD -> it.mode == OsuMode.MANIA && fitCountOrPercent(operator, it.statistics.good, double, it.maximumStatistics.good, dec)

                OK -> if (it.mode != OsuMode.CATCH && it.mode != OsuMode.CATCH_RELAX) {
                    fitCountOrPercent(operator, it.statistics.ok, double, it.maximumStatistics.ok, dec)
                } else {
                    fitCountOrPercent(operator, it.statistics.ok, double, it.maximumStatistics.largeTickHit, dec)
                }

                MEH -> if (it.mode != OsuMode.CATCH && it.mode != OsuMode.CATCH_RELAX) {
                    fitCountOrPercent(operator, it.statistics.meh, double, it.maximumStatistics.meh, dec)
                } else {
                    fitCountOrPercent(operator, it.statistics.meh, double, it.maximumStatistics.smallTickHit, dec)
                }

                MISS -> fitCountOrPercent(operator, it.statistics.miss, double, it.maximumStatistics.miss, dec)

                MISSED_FRUIT -> (it.mode == OsuMode.CATCH || it.mode == OsuMode.CATCH_RELAX) && fitCountOrPercent(operator, it.statistics.miss - it.statistics.largeTickMiss, double, it.maximumStatistics.great, dec)

                MISSED_DROP -> (it.mode == OsuMode.CATCH || it.mode == OsuMode.CATCH_RELAX) && it.maximumStatistics.largeTickHit > 0 &&
                        fitCountOrPercent(operator, it.statistics.largeTickMiss, double, it.maximumStatistics.largeTickHit, dec)

                MISSED_DROPLET -> (it.mode == OsuMode.CATCH || it.mode == OsuMode.CATCH_RELAX) && it.maximumStatistics.smallTickHit > 0 &&
                        fitCountOrPercent(operator, it.statistics.smallTickMiss, double, it.maximumStatistics.smallTickHit, dec)

                MOD -> fitMod(operator, str, it.mods)

                RATE -> {
                    if (it.mode != OsuMode.MANIA) throw IllegalArgumentException.WrongException.Mode()

                    val rate = min((it.statistics.perfect * 1.0 / it.statistics.great), 100.0)
                    val input = if (double > 0.0) min(double, 100.0) else double

                    fit(operator, rate, input, digit = 2, isRound = true, isInteger = true)
                }

                CIRCLE -> fitCountOrPercent(operator, it.beatmap.circles, double, it.beatmap.totalNotes, dec)
                SLIDER -> fitCountOrPercent(operator, it.beatmap.sliders, double, it.beatmap.totalNotes, dec)
                SPINNER -> fitCountOrPercent(operator, it.beatmap.spinners, double, it.beatmap.totalNotes, dec)

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
            if (tags.isBlank()) return false

            var start = 0
            val length = tags.length

            while (start < length) {
                // 跳过前面的连续空格
                while (start < length && tags[start].isWhitespace()) {
                    start++
                }
                if (start >= length) break

                // 查找当前 tag 的结束位置
                var end = start
                var hasUnderscore = false
                while (end < length && !tags[end].isWhitespace()) {
                    if (tags[end] == '_') hasUnderscore = true
                    end++
                }

                // 提取并清洗 tag
                val rawTag = tags.substring(start, end)
                val tag = if (hasUnderscore) rawTag.replace("_", "") else rawTag

                if (fit(operator, tag, to)) {
                    return true
                }

                start = end + 1
            }

            return false
        }

        /**
         * 公用方法
         * 在 to 含有小数点时，按 compare 占 total 的百分比来处理。在其他情况时，按 compare 整数来处理。
         */
        fun fitCountOrPercent(operator: Operator, compare: Number?, to: Number, total: Number?, hasDecimal: Boolean): Boolean {
            if (compare == null) return false

            val c = compare.toDouble()
            val t = to.toDouble()
            val l = total?.toDouble() ?: 0.0

            return if (hasDecimal && t in 0.0..1.0 && operator !== Operator.XQ) {
                l != 0.0 && fit(operator, c / l, t, digit = 2, isRound = true, isInteger = false)
            } else {
                fit(operator, compare.toLong(), t.toLong())
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