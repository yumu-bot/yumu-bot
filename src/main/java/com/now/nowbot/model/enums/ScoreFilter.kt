package com.now.nowbot.model.enums

import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToLong

enum class ScoreFilter(@Language("RegExp") val regex: Regex) {
    MAPPER("(mapper|creator|gd(er)?|host|u)(?<n>$REG_OPERATOR$REG_NAME)".toRegex()),

    BID("(beatmapid|b?id|i)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

    SID("((beatmap)?setid|sid?)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

    TITLE("(title|name|song|t)(?<n>$REG_OPERATOR$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    ARTIST("(artist|f?a)(?<n>$REG_OPERATOR$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    SOURCE("(source|src|s)(?<n>$REG_OPERATOR$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    TAG("(tags?|g)(?<n>$REG_OPERATOR$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    DIFFICULTY("(difficulty|diff|d)(?<n>$REG_OPERATOR$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    STAR("(star|rating|sr|r)(?<n>$REG_OPERATOR$REG_NUMBER_DECIMAL)$REG_STAR$LEVEL_MAYBE".toRegex()),

    AR("(ar|approach)(?<n>$REG_OPERATOR$REG_NUMBER_DECIMAL)".toRegex()),

    CS("(cs|circle|keys?)(?<n>$REG_OPERATOR$REG_NUMBER_DECIMAL)".toRegex()),

    OD("(od|overall)(?<n>$REG_OPERATOR$REG_NUMBER_DECIMAL)".toRegex()),

    HP("(hp|health)(?<n>$REG_OPERATOR$REG_NUMBER_DECIMAL)".toRegex()),

    PERFORMANCE("(performance|pp|p)(?<n>$REG_OPERATOR$REG_NUMBER_DECIMAL)".toRegex()),

    RANK("(rank(ing)?|k)(?<n>$REG_OPERATOR$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    LENGTH("(length|drain|time|l)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE($REG_COLON$REG_NUMBER$LEVEL_MORE)?)".toRegex()),

    BPM("(bpm|b)(?<n>$REG_OPERATOR$REG_NUMBER_DECIMAL)".toRegex()),

    ACCURACY("(accuracy|acc)(?<n>$REG_OPERATOR$REG_NUMBER_DECIMAL)[%％]?".toRegex()),

    COMBO("(combo|cb?)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE[xX]?)".toRegex()),

    PERFECT("(perfect|320|305|pf)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

    GREAT("(great|300|良|gr)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

    GOOD("(good|200|gd)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

    OK("(ok|150|100|(?<!不)可|ba?d)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

    MEH("(meh|p(oo)?r|50)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

    MISS("(m(is)?s|0|x|不可)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

    MOD("(m(od)?s?)(?<n>$REG_OPERATOR$REG_MOD$LEVEL_MORE)".toRegex()),

    RATE("(rate|e|pm)(?<n>$REG_OPERATOR$REG_NUMBER_DECIMAL)".toRegex()),

    CIRCLE("((hit)?circles?|hi?t|click|rice|ci|cr|rc)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

    SLIDER("(slider?s?|sl|longnote|ln)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

    SPINNER("(spin(ner)?s?|rattle|sp)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

    TOTAL("(total|all|ttl|(hit)?objects?|o)(?<n>$REG_OPERATOR$REG_NUMBER$LEVEL_MORE)".toRegex()),

    CONVERT("(convert|cv)(?<n>$REG_OPERATOR$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    CLIENT("(client|z|v|version)(?<n>$REG_OPERATOR$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    RANGE(REG_RANGE.toRegex());
    
    companion object {
        fun filterScores(scores: Map<Int, LazerScore>, conditions: List<List<String>>): Map<Int, LazerScore> {
            val s = scores.toMutableMap()

            // 最后一个筛选条件无需匹配
            conditions.take(ScoreFilter.entries.size - 1).forEachIndexed { index, strings ->
                if (strings.isNotEmpty()) {
                    filterConditions(s, ScoreFilter.entries.toList()[index], strings)
                }
            }

            return s.toMap()
        }

        private fun filterConditions(scores: MutableMap<Int, LazerScore>, filter: ScoreFilter, conditions: List<String>) {
            for (c in conditions) {
                val operator = Operator.getOperator(c)
                val condition = (c.split(REG_OPERATOR.toRegex()).lastOrNull() ?: "").trim()

                scores.entries.removeIf { ScoreFilter.fitScore(it.value, operator, filter, condition).not() }
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
            } else {
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Calculate, "成绩筛选")
            }
        }

        private fun fitScore(it: LazerScore, operator: Operator, filter: ScoreFilter, condition: String): Boolean {
            val long = condition.toLongOrNull() ?: -1L
            val double = condition.toDoubleOrNull() ?: -1.0

            return when (filter) {
                MAPPER ->
                    if (it.beatmap.owners != null) {
                        for (o in it.beatmap.owners!!) {
                            if (fit(operator, o.userName, condition)) return true
                        }
                        false
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
                TAG -> run {
                    if (it.beatmapset.tags == null) return false

                    for (t in it.beatmapset.tags!!.split("\\s+".toRegex())) {
                        if (fit(operator, it.beatmapset.source, condition)) return true
                    }

                    return false
                }
                DIFFICULTY -> fit(operator, it.beatmap.difficultyName, condition)

                STAR -> fit(operator, it.beatmap.starRating, double)

                AR -> fit(operator, it.beatmap.AR?.toDouble() ?: 0.0, double)
                CS -> fit(operator, it.beatmap.CS?.toDouble() ?: 0.0, double)
                OD -> fit(operator, it.beatmap.OD?.toDouble() ?: 0.0, double)
                HP -> fit(operator, it.beatmap.HP?.toDouble() ?: 0.0, double)
                PERFORMANCE -> fit(operator, it.pp, double)
                RANK -> run {
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
                LENGTH -> run {
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

                BPM -> fit(operator, it.beatmap.BPM?.toDouble() ?: 0.0, double, isPlus = true)
                ACCURACY -> run {
                    val acc = when {
                        double > 10000.0 -> throw IllegalArgumentException.WrongException.Henan()
                        double > 100.0 -> double / 10000.0
                        double > 1.0 -> double / 100.0
                        else -> double
                    }

                    fit(operator, it.accuracy, acc, isPlus = true)
                }
                COMBO -> run {
                    val combo = when {
                        double <= 1.0 && double > 0.0 -> it.beatmap.maxCombo?.times(double)?.roundToLong() ?: long
                        else -> long
                    }

                    fit(operator, it.maxCombo.toLong(), combo)
                }
                PERFECT -> fit(operator, it.statistics.perfect.toLong(), long)
                GREAT -> fit(operator, it.statistics.great.toLong(), long)
                GOOD -> fit(operator, it.statistics.good.toLong(), long)
                OK -> fit(operator, it.statistics.ok.toLong(), long)
                MEH -> fit(operator, it.statistics.meh.toLong(), long)
                MISS -> fit(operator, it.statistics.miss.toLong(), long)
                MOD -> run {
                    if (condition.contains("NM", ignoreCase = true)) {
                        when (operator) {
                            Operator.XQ, Operator.EQ -> it.mods.isEmpty() || (it.mods.size == 1 && it.mods.first().acronym == "CL")
                            Operator.NE -> (it.mods.isEmpty() || (it.mods.size == 1 && it.mods.first().acronym == "CL")).not()
                            else -> throw IllegalArgumentException.WrongException.OperatorOnly("==", "=", "!=")
                        }
                    } else if (condition.contains("FM", ignoreCase = true)) {
                        when (operator) {
                            Operator.XQ, Operator.EQ -> it.mods.isNotEmpty() && (it.mods.size == 1 && it.mods.first().acronym == "CL").not()
                            Operator.NE -> it.mods.isEmpty() || (it.mods.size == 1 && it.mods.first().acronym == "CL")
                            else -> throw IllegalArgumentException.WrongException.OperatorOnly("==", "=", "!=")
                        }
                    } else {
                        val mods = LazerMod.getModsList(condition)

                        when (operator) {
                            Operator.XQ -> LazerMod.hasMod(mods, it.mods) && (mods.size == it.mods.size)
                            Operator.EQ -> LazerMod.hasMod(mods, it.mods)
                            Operator.NE -> LazerMod.hasMod(mods, it.mods).not()
                            else -> throw IllegalArgumentException.WrongException.OperatorOnly("==", "=", "!=")
                        }
                    }
                }

                RATE -> run {
                    if (it.mode != OsuMode.MANIA) throw IllegalArgumentException.WrongException.Mode()

                    val rate = min((it.statistics.perfect * 1.0 / it.statistics.great), 100.0)
                    val input = if (double > 0.0) min(double, 100.0) else double

                    fit(operator, rate, input, isPlus = true)
                }

                CIRCLE -> fit(operator, it.beatmap.circles?.toLong() ?: -1L, long)
                SLIDER -> fit(operator, it.beatmap.sliders?.toLong() ?: -1L, long)
                SPINNER -> fit(operator, it.beatmap.spinners?.toLong() ?: -1L, long)
                TOTAL -> fit(operator, (it.beatmap.circles?.toLong() ?: -1L) + (it.beatmap.sliders?.toLong() ?: -1L) + (it.beatmap.spinners?.toLong() ?: -1L), long)

                CONVERT -> when (condition.trim().lowercase()) {
                    "true", "t", "yes", "y" -> it.beatmap.convert == true
                    "false", "f", "no", "not", "n" -> it.beatmap.convert != true
                    else -> it.beatmap.convert != true
                }

                CLIENT -> when (condition.trim().lowercase()) {
                    "lazer", "l", "lz", "lzr" -> it.isLazer
                    "stable", "s", "st", "stb" -> !it.isLazer
                    else -> !it.isLazer
                }

                else -> false
            }
        }
    }
}