package com.now.nowbot.model.filter

import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore

import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.*

enum class ScoreFilter(@param:Language("RegExp") val regex: Regex) {
    CREATOR("(creator|host|c|h|谱师|作者|谱|主)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NAME)".toRegex()),

    GUEST("((gder|guest\\s*diff(er)?)|mapper|guest|g?u|客串?(谱师)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NAME)".toRegex()),

    BID("((beatmap\\s*)?id|bid|b|(谱面)?编?号)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    SID("((beatmap\\s*)?setid|sid|s|(谱面)?集编号)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    TITLE("(title|name|song|t|歌?曲名|歌曲|标题)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    ARTIST("(artist|singer|art|f?a|艺术家|曲师?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    SOURCE("(source|src|from|f|o|sc|来?源)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    TAG("(tags?|g|标签?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    DIFFICULTY("(difficulty|diff|d|难度名?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    STAR("(star|rating|sr|r|星数?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)$REG_STAR$LEVEL_MAYBE".toRegex()),

    AR("(ar|approach\\s*(rate)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    CS("(cs|circle\\s*(size)?|keys?|键)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    OD("(od|overall\\s*(difficulty)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    HP("(hp|health\\s*(point)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    PERFORMANCE("(performance|表现分?|pp|p)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    RANK("(rank(ing)?|评[价级]|k)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    LENGTH("(length|drain|time|长度|l)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE($REG_COLON$REG_NUMBER_MORE)?)".toRegex()),

    BPM("(bpm|曲速|速度|bm)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    ACCURACY("(accuracy|精[确准][率度]?|准确?[率度]|acc?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)[%％]?".toRegex()),

    COMBO("(combo|连击|cb?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL[xX]?)".toRegex()),

    PERFECT("(perfect|320|305|彩|完美|pf)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    GREAT("(great|300|大果?|fruits?|fr|良|黄|gr|很好)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    MISSED_DROPLET("(miss(ed)?\\s*drop(let)?|漏小?果?|md)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    GOOD("(good|200|绿|gd|良好)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    OK("(ok|150|100|中果?|large\\s*drop(let)?|ld|(?<!不)可|蓝|ba?d|可以)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    MEH("(me?h|小果?|drop(let)?|sd|p(oo)?r|灰|50|一般)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    MISS("(m(is)?s|0|x|不可|红|失误|漏击)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    MOD("((m(od)?s?)|模组?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_MOD$LEVEL_MORE)".toRegex()),

    RATE("(rate|彩[率比]|黄彩比?|e|pm)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    CIRCLE("((hit)?circles?|hi?t|click|rice|ci|cr|rc|圆圈?|米)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    SLIDER("(slider?s?|sl|long(note)?|lns?|[滑长]?条|长键|面)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    SPINNER("(spin(ner)?s?|rattle|sp|转盘|[转盘])(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    TOTAL("(total|all|ttl|(hit)?objects?|tt|物件数?|总数?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    CONVERT("(convert|cv|转谱?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    CLIENT("(client|z|v|version|版本?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    TIME("(time|时间?|ti)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$REG_TIME)".toRegex()),

    RANGE(REG_RANGE.toRegex());
    
    companion object {
        fun filterScores(scores: Map<Int, LazerScore>, conditions: List<List<String>>): Map<Int, LazerScore> {
            val s = scores.toMutableMap()
            val el = entries.toList()

            // 最后一个筛选条件无需匹配
            conditions
                .dropLast(1)
                .forEachIndexed { index, strings ->
                if (strings.isNotEmpty()) {
                    filterConditions(s, el[index], strings)
                }
            }

            return s.toMap()
        }

        private fun filterConditions(scores: MutableMap<Int, LazerScore>, filter: ScoreFilter, conditions: List<String>) {
            for (c in conditions) {
                val operator = Operator.getOperator(c)
                val condition = (c.split(REG_OPERATOR_WITH_SPACE.toRegex()).lastOrNull() ?: "").trim()

                scores.entries.removeIf { fitScore(it.value, operator, filter, condition).not() }
            }
        }

        /**
         * @param compare 谱面数据
         * @param to 输入的数据
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
            to: Any,
            digit: Int = 0,
            isRound: Boolean = true,
            isInteger: Boolean = true,
        ): Boolean {
            if (compare == null) return false

            return when {
                (compare is Long && to is Long) -> {
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
                }

                (compare is Int && to is Int) -> return fit(operator, compare.toLong(), to.toLong(), digit, isRound, isInteger)

                (compare is Double && to is Double) -> {
                    val c: Double = abs(compare)
                    val t: Double = abs(to)

                    val dig = if (isInteger) {
                        var temp = 0

                        for (i in 0..digit) {
                            val sc = 10.0.pow(i)
                            val tt = t * sc

                            if (tt in floor(tt) ..< floor(tt) + 0.1) {
                                temp = i
                                break
                            }
                        }

                        temp
                    } else {
                        digit
                    }

                    val scale = 10.0.pow(dig)

                    val rc = if (isRound && digit == dig) {
                        round(c * scale) / scale
                    } else {
                        floor(c * scale) / scale
                    }

                    when (operator) {
                        Operator.XQ -> abs(c - t) <= 1e-4
                        Operator.EQ -> abs(rc - t) <= 1e-4
                        Operator.NE -> abs(rc - t) > 1e-4
                        Operator.GT -> c > t
                        Operator.GE -> c >= t
                        Operator.LT -> c < t
                        Operator.LE -> c <= t
                    }
                }

                (compare is String && to is String) -> {
                    val c: String = DataUtil.getStandardisedString(compare.trim())
                    val t: String = DataUtil.getStandardisedString(to.trim())

                    when (operator) {
                        Operator.XQ -> t.equals(c, ignoreCase = true)
                        Operator.EQ -> c.contains(t, ignoreCase = true)
                        Operator.NE -> c.contains(t, ignoreCase = true).not()
                        Operator.GT -> t.contains(c, ignoreCase = true) && t.length > c.length
                        Operator.GE -> t.contains(c, ignoreCase = true) && t.length >= c.length
                        Operator.LT -> c.contains(t, ignoreCase = true) && t.length < c.length
                        Operator.LE -> c.contains(t, ignoreCase = true) && t.length <= c.length
                    }
                }

                (compare is Boolean && to is Boolean) -> {
                    when(operator) {
                        Operator.XQ, Operator.EQ -> compare == to
                        Operator.NE -> compare != to

                        else -> throw IllegalArgumentException.WrongException.OperatorOnly("==", "=", "!=")
                    }
                }

                (compare is Enum<*> && to is Enum<*>) -> {
                    fit(operator, compare.ordinal.toLong(), to.ordinal.toLong(), digit, isRound)
                }

                (compare is List<*> && to is List<*>) -> {
                    val c: Set<Any?> = compare.toSet()
                    val t: Set<Any?> = to.toSet()

                    when (operator) {
                        Operator.XQ -> c.contains(t) && t.size == c.size
                        Operator.EQ -> c.contains(t)
                        Operator.NE -> c.contains(t).not()
                        Operator.GT -> t.contains(c) && t.size > c.size
                        Operator.GE -> t.contains(c) && t.size >= c.size
                        Operator.LT -> c.contains(t) && t.size < c.size
                        Operator.LE -> c.contains(t) && t.size <= c.size
                    }
                }

                else -> fit(operator, compare.toString(), to.toString(), digit, isRound)
            }
        }

        private fun fitScore(it: LazerScore, operator: Operator, filter: ScoreFilter, condition: String): Boolean {
            val long = condition.toLongOrNull() ?: -1L
            val double = condition.toDoubleOrNull() ?: -1.0

            // 一般这个数据都很大。如果输入很小的数，会自动给你乘 1k
            val longPlus = if (long in 1..< 100) {
                long * 1000L
            } else {
                long
            }

            return when (filter) {
                CREATOR -> fit(operator, it.beatmapset.creator, condition)

                GUEST -> if (! it.beatmap.owners.isNullOrEmpty()) {
                    if (long > 0L) {
                        val ids = it.beatmap.owners!!.map { fit(operator, it.userID, long) }.toSet()
                        val names = it.beatmap.owners!!.map { fit(operator, it.username, condition) }.toSet()

                        ids.contains(element = true) || names.contains(element = true)
                    } else {
                        val names = it.beatmap.owners!!.map { fit(operator, it.username, condition) }.toSet()

                        names.contains(element = true)
                    }
                } else {
                    fit(operator, it.beatmapset.creator, condition)
                }

                BID -> fit(operator, it.beatmapID, long)
                SID -> fit(operator, it.beatmapset.beatmapsetID, long)
                TITLE -> (fit(operator, it.beatmapset.title, condition)
                        || fit(operator, it.beatmapset.titleUnicode, condition))
                ARTIST -> (fit(operator, it.beatmapset.artist, condition)
                        || fit(operator, it.beatmapset.artistUnicode, condition))
                SOURCE -> fit(operator, it.beatmapset.source, condition)
                TAG ->
                    if (it.beatmapset.tags.isNullOrBlank()) {
                        false
                    } else {
                        val ts = it.beatmapset.tags!!.split("\\s+".toRegex()).map { fit(operator, it, condition) }.toSet()
                        return ts.contains(element = true)
                    }

                DIFFICULTY -> fit(operator, it.beatmap.difficultyName, condition)

                STAR -> fit(operator, it.beatmap.starRating, double, digit = 2, isRound = false, isInteger = true)

                AR -> fit(operator, it.beatmap.AR?.toDouble() ?: 0.0, double, digit = 2, isRound = true, isInteger = true)
                CS -> fit(operator, it.beatmap.CS?.toDouble() ?: 0.0, double, digit = 2, isRound = true, isInteger = true)
                OD -> fit(operator, it.beatmap.OD?.toDouble() ?: 0.0, double, digit = 2, isRound = true, isInteger = true)
                HP -> fit(operator, it.beatmap.HP?.toDouble() ?: 0.0, double, digit = 2, isRound = true, isInteger = true)
                PERFORMANCE -> fit(operator, it.pp.roundToLong(), longPlus) //fit(operator, it.pp, double, digit = 0, isRound = true, isInteger = true)
                RANK -> {
                    val rankArray = arrayOf("F", "D", "C", "B", "A", "S", "SH", "X", "XH")

                    val cr = rankArray.indexOf(
                        when(condition.uppercase()) {
                            "SSH" -> "XH"
                            "SS" -> "X"
                            else -> condition.uppercase()
                        }
                    )

                    val ir = rankArray.indexOf(it.rank.uppercase())

                    if (cr == -1) {
                        throw IllegalArgumentException.WrongException.Rank()
                    }

                    fit(operator, ir.toLong(), cr.toLong())
                }

                LENGTH -> {
                    var seconds = 0L
                    if (condition.contains(REG_COLON.toRegex())) {
                        val strs = condition.split(REG_COLON.toRegex())
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

                    fit(operator, it.beatmap.totalLength.toLong(), seconds)
                }

                BPM -> fit(operator, it.beatmap.BPM?.toDouble() ?: 0.0, double, digit = 2, isRound = true, isInteger = true)
                ACCURACY -> {
                    val acc = when {
                        double > 10000.0 || double <= 0.0 -> throw IllegalArgumentException.WrongException.Henan()
                        double > 100.0 -> double / 10000.0
                        double > 1.0 -> double / 100.0
                        else -> double
                    } // 0-1

                    fit(operator, it.accuracy, acc, digit = 2, isRound = true, isInteger = true)
                }

                COMBO -> fitCountOrPercent(operator, it.maxCombo, condition, it.beatmap.maxCombo)

                PERFECT -> if (it.mode == OsuMode.MANIA) {
                    fitCountOrPercent(operator, it.statistics.perfect, condition, it.maximumStatistics.perfect)
                } else {
                    false
                }
                GREAT -> fitCountOrPercent(operator, it.statistics.great, condition, it.maximumStatistics.great)
                GOOD -> if (it.mode == OsuMode.MANIA) {
                    fitCountOrPercent(operator, it.statistics.good, condition, it.maximumStatistics.good)
                } else {
                    false
                }

                OK -> if (it.mode != OsuMode.CATCH && it.mode != OsuMode.CATCH_RELAX) {
                    fitCountOrPercent(operator, it.statistics.ok, condition, it.maximumStatistics.ok)
                } else {
                    fitCountOrPercent(operator, it.statistics.ok, condition, it.maximumStatistics.largeTickHit)
                }

                MEH -> if (it.mode != OsuMode.CATCH && it.mode != OsuMode.CATCH_RELAX) {
                    fitCountOrPercent(operator, it.statistics.meh, condition, it.maximumStatistics.meh)
                } else {
                    fitCountOrPercent(operator, it.statistics.meh, condition, it.maximumStatistics.smallTickHit)
                }

                MISS -> fitCountOrPercent(operator, it.statistics.miss, condition, it.maximumStatistics.miss)

                MISSED_DROPLET -> if (it.mode == OsuMode.CATCH || it.mode == OsuMode.CATCH_RELAX) {
                    fitCountOrPercent(operator, it.statistics.smallTickMiss, condition, it.maximumStatistics.smallTickHit)
                } else {
                    false
                }

                MOD -> fitMod(operator, condition, it.mods)

                RATE -> {
                    if (it.mode != OsuMode.MANIA) throw IllegalArgumentException.WrongException.Mode()

                    val rate = min((it.statistics.perfect * 1.0 / it.statistics.great), 100.0)
                    val input = if (double > 0.0) min(double, 100.0) else double

                    fit(operator, rate, input, digit = 2, isRound = true, isInteger = true)
                }

                CIRCLE -> fitCountOrPercent(operator, it.beatmap.circles, condition, it.beatmap.totalNotes)
                SLIDER -> fitCountOrPercent(operator, it.beatmap.sliders, condition, it.beatmap.totalNotes)
                SPINNER -> fitCountOrPercent(operator, it.beatmap.spinners, condition, it.beatmap.totalNotes)

                TOTAL -> {
                    val total = it.beatmap.totalNotes

                    if (total == 0) {
                        false
                    } else {
                        fit(operator, total, long)
                    }
                }

                CONVERT -> when (condition.trim().lowercase()) {
                    "true", "t", "yes", "y" -> it.beatmap.convert == true
                    "false", "f", "no", "not", "n" -> it.beatmap.convert == false
                    else -> it.beatmap.convert == false
                }

                CLIENT -> when (condition.trim().lowercase()) {
                    "lazer", "l", "lz", "lzr" -> it.isLazer
                    "stable", "s", "st", "stb" -> !it.isLazer
                    else -> !it.isLazer
                }

                TIME -> fitTime(operator, it.endedTime.toEpochSecond(), condition)

                else -> false
            }
        }

        /**
         * 公用方法
         * 在 to 含有小数点时，按 compare 占 total 的百分比来处理。在其他情况时，按 compare 整数来处理。
         */
        fun fitCountOrPercent(operator: Operator, compare: Number?, to: String, total: Number?): Boolean {
            if (compare == null) return false

            val c = compare.toDouble()
            val t = to.toDoubleOrNull() ?: 0.0
            val l = total?.toDouble() ?: 0.0

            val hasDecimal = to.contains('.')

            return if (hasDecimal && t in 0.0..1.0 && operator !== Operator.XQ) {
                if (l == 0.0) {
                    false
                } else {
                    fit(operator, c / l, t, digit = 2, isRound = true, isInteger = false)
                }
            } else {
                fit(operator, compare.toInt(), t.toInt())
            }
        }

        /**
         * 时间筛选器升级
         */
        fun fitTime(operator: Operator, compare: Long?, to: String): Boolean {
            if (compare == null) return false

            val t = DataUtil.parseTime(to)

            // 年月
            val period = t.first

            // 日时分秒
            val duration = t.second

            // = ==
            val isWithInMode = operator == Operator.EQ || operator == Operator.XQ || operator == Operator.NE

            // <= >=
            val isAbsoluteMode = (operator == Operator.GE || operator == Operator.LE)

            val too: Long
            val now = LocalDateTime.now()

            // 区域日期模式下，最小的单位差
            // 如果是 ==，则会精确到秒。如果是 =，则只会精确到天。
            var delta = 0L

            if (isAbsoluteMode) {
                // 绝对日期模式，构建一个目标时间

                val years = when(period.years) {
                    0 -> {
                        delta = ChronoUnit.SECONDS.between(now.minusYears(1), now)
                        now.year
                    }
                    else -> period.years % 100 + (now.year / 1000) * 1000
                }

                val months = when(period.months) {
                    0 -> {
                        delta = ChronoUnit.SECONDS.between(now.minusMonths(1), now)
                        now.month.value
                    }
                    else -> period.months
                }

                val totalSeconds = duration.inWholeSeconds

                val dd = (totalSeconds / (24 * 3600)).toInt()
                val hh = ((totalSeconds % (24 * 3600)) / 3600).toInt()
                val mm = ((totalSeconds % 3600) / 60).toInt()
                val ss = (totalSeconds % 60).toInt()

                val days = when(dd) {
                    0 -> {
                        delta = 24 * 60 * 60
                        now.dayOfMonth
                    }
                    else -> min(YearMonth.from(now).lengthOfMonth(), dd)
                }

                val hours = when(hh) {
                    0 -> {
                        delta = 24 * 60 * 60
                        now.dayOfMonth
                    }
                    else -> min(hh, 24)
                }

                val minutes = when(mm) {
                    0 -> {
                        delta = 24 * 60 * 60
                        now.minute
                    }

                    else -> min(mm, 60)
                }

                val seconds = when(ss) {
                    0 -> {
                        delta = 24 * 60 * 60
                        now.second
                    }
                    else -> min(ss, 60)
                }

                too = OffsetDateTime.of(years, months, days, hours, minutes, seconds, 0, ZoneOffset.ofHours(8)).toEpochSecond()
            } else {
                // 移动日期模式，从现在减去这段时间
                delta = 24 * 60 * 60

                too = now
                    .minusYears(period.years.toLong())
                    .minusMonths(period.months.toLong())
                    .minusSeconds(duration.inWholeSeconds)
                    .toEpochSecond(ZoneOffset.ofHours(8))
            }

            return if (isWithInMode) {
                // 区域日期模式，从目标时间到目标时间 + 输入的最小单位的时间

                if (operator != Operator.NE) {
                    fit(Operator.GE, compare, too) && fit(Operator.LE, compare, too + delta)
                } else {
                    !(fit(Operator.GE, compare, too) && fit(Operator.LE, compare, too + delta))
                }
            } else {
                // 注意，要在这里取反：因为 >2d，其实是指 2 天前并且更早的结果
                fit(operator, -compare, -too)
            }
        }

        fun fitMod(operator: Operator, compare: String, to: List<LazerMod>): Boolean {
            val com = LazerMod.getModsList(compare)
                .map { it.acronym }.toSet()
            val too = to
                .map { it.acronym }.filter { it != "CL" }.toSet()

            return if (compare.isEmpty() || compare.contains("NM", ignoreCase = true)) {
                when (operator) {
                    Operator.XQ, Operator.EQ -> too.isEmpty()
                    Operator.NE, Operator.GE, Operator.GT -> too.isNotEmpty()
                    else -> false
                }
            } else if (compare.contains("FM", ignoreCase = true)) {
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

        fun getBoolean(string: String): Boolean {
            return when(string.trim()) {
                "真", "是", "正确", "对", "t", "true", "y", "yes", "" -> true
                else -> false
            }
        }
    }
}