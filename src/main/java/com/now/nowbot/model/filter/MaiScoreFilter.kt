package com.now.nowbot.model.filter

import com.now.nowbot.model.enums.*
import com.now.nowbot.model.filter.ScoreFilter.Companion.fit
import com.now.nowbot.model.filter.ScoreFilter.Companion.fitCountOrPercent
import com.now.nowbot.model.maimai.MaiScore
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class MaiScoreFilter(@Language("RegExp") val regex: Regex) {
    CHARTER("(chart(er)?|mapper|谱师?|c)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    ID("(id|编?号|i)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    DIFFICULTY("(difficulty|diff|难度?|d)(?<n>$REG_OPERATOR_WITH_SPACE$REG_MAI_DIFFICULTY)".toRegex()),

    DIFFICULTY_NAME("(difficulty|diff|难度?|d)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    CABINET("(cabinet|框体?|cab|ca|n)(?<n>$REG_OPERATOR_WITH_SPACE$REG_MAI_CABINET)".toRegex()),

    VERSION("(version|版本?|v)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    TITLE("(title|name|song|曲|名|曲名|标题|t)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    ALIASES("(alias|aliases|外号|绰号|别名?|l)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    ARTIST("(artist|singer|art|艺术家|歌手?|a)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    CATEGORY("(type|category|genre|类型?|种类?|t|g)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    BPM("(bpm|b|bm)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    ACHIEVEMENT("(accuracy|达成率?|精[确准][率度]?|准确?[率度]|achieve(ment)?|acc|ach|ac)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)[%％]?".toRegex()),

    TAP("(tap|ta|tp)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    HOLD("(hold|hod|ho|hd)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    SLIDE("(slider?|sld|sl|se)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    TOUCH("(touch|toh|tch|th|to)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    BREAK("(break|brk|br|bk)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    DX_SCORE("(dx\\s*score|score|dx分|分|dx|ds|o)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    DX_STAR("(dx\\s*star|star|dx星|星|dxsr|dr|sr|s)(?<n>$REG_OPERATOR_WITH_SPACE[0-5])".toRegex()),

    RATING("(rating|评分|r|ra|rt)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    RANK("(rank|评价|rk|k)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    COMBO("(combo|连击|cb|x)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    SYNC("(sync|同步|sy|y)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    RANGE(REG_MAI_RANGE.toRegex()),
    ;

    companion object {
        enum class ComboType {
            PASS, FC, FC_PLUS, AP, AP_PLUS;

            companion object {
                fun getCombo(str: String?): ComboType {
                    return when(str?.replace(Regex("\\s*"), "")?.lowercase()) {
                        "fullcomboplus", "fullcombo+", "fc+", "fcp" -> FC_PLUS
                        "fullcombo", "fc" -> FC
                        "allperfectplus", "allperfect+", "perfect+", "ap+", "app", "pf+", "pfp" -> AP_PLUS
                        "allperfect", "ap", "pf" -> AP

                        else -> PASS
                    }
                }
            }
        }

        enum class SyncType {
            PASS, SYNC, FS, FS_PLUS, FDX, FDX_PLUS;

            companion object {
                fun getSync(str: String?): SyncType {
                    return when(str?.replace(Regex("\\s*"), "")?.lowercase()) {
                        "fullsyncplus", "fullsync+", "fs+", "fsp" -> FS_PLUS
                        "fullsync", "fs" -> FS
                        "fullsyncdxplus", "fullsyncdx+", "fulldx+", "fsd+", "fsdp", "fdx+", "fdxp" -> FDX_PLUS
                        "fullsyncdx", "fsd", "fdx" -> FDX

                        "sync", "s" -> SYNC

                        else -> PASS
                    }
                }
            }
        }

        fun filterScores(scores: List<MaiScore>, conditions: List<List<String>>): List<MaiScore> {
            val s = scores.toMutableList()
            val el = entries.toList()

            // 最后一个筛选条件无需匹配
            conditions
                .dropLast(1)
                .forEachIndexed { index, strings ->
                if (strings.isNotEmpty()) {
                    filterConditions(s, el[index], strings)
                }
            }

            return s.toList()
        }

        private fun filterConditions(scores: MutableList<MaiScore>, filter: MaiScoreFilter, conditions: List<String>) {
            for (c in conditions) {
                val operator = Operator.getOperator(c)
                val condition = (c.split(REG_OPERATOR_WITH_SPACE.toRegex()).lastOrNull() ?: "").trim()

                scores.removeIf { fitScore(it, operator, filter, condition).not() }
            }
        }

        private fun fitScore(it: MaiScore, operator: Operator, filter: MaiScoreFilter, condition: String): Boolean {
            val int = condition.toIntOrNull() ?: -1
            val double = condition.toDoubleOrNull() ?: -1.0

            return when (filter) {
                CHARTER -> fit(operator,
                    MaiCharter.getCharter(it.charter).sorted().joinToString(" "),
                    MaiCharter.getCharter(condition).sorted().joinToString(" "))

                ID -> fit(operator, it.songID % 10000L, int.toLong() % 10000L)

                DIFFICULTY -> fitRange(operator, condition, it.star)

                DIFFICULTY_NAME -> {
                    val con = MaiDifficulty.getIndex(MaiDifficulty.getDifficulty(condition))

                    val dif = if (!it.isUtage) {
                        it.index
                    } else {
                        5
                    }

                    // 如果不是查询宴会场，就不会返回宴会场的数据
                    if (con != 5 && dif == 5) return false

                    fit(operator, dif, con)
                }

                CABINET -> fit(operator, MaiCabinet.getCabinet(condition), MaiCabinet.getCabinet(it.type))
                VERSION -> fit(operator,
                    MaiVersion.getVersionList(it.version).joinToString(" ") { it.abbreviation },
                    MaiVersion.getVersionList(condition).joinToString(" ") { it.abbreviation }
                )
                TITLE -> fit(operator, it.title, condition)
                ALIASES -> {
                    it.aliases?.map { alias ->
                        fit(operator, alias, condition)
                    }?.toSet()?.contains(true) ?: false
                }
                ARTIST -> fit(operator, it.artist, condition)
                CATEGORY -> fit(operator, MaiCategory.getCategory(it.genre), MaiCategory.getCategory(condition))
                BPM -> fit(operator, it.bpm, int)
                ACHIEVEMENT -> {
                    val acc = when {
                        double > 1010000.0 || double <= 0.0 -> throw IllegalArgumentException.WrongException.Henan()
                        double > 10100.0 -> double / 1000000.0
                        double > 101.0 -> double / 100.0
                        double > 1.0 -> double
                        else -> double * 100.0
                    }

                    fit(operator, it.achievements, acc, digit = 4, isRound = true, isInteger = true)
                }
                TAP -> fitCountOrPercent(operator, it.notes[0], condition, it.notes.sum())
                HOLD -> fitCountOrPercent(operator, it.notes[1], condition, it.notes.sum())
                SLIDE -> fitCountOrPercent(operator, it.notes[2], condition, it.notes.sum())
                TOUCH -> fitCountOrPercent(operator, it.notes[3], condition, it.notes.sum())
                BREAK -> fitCountOrPercent(operator, it.notes[4], condition, it.notes.sum())
                DX_SCORE -> fitCountOrPercent(operator, it.score, condition, it.notes.sum() * 3)
                DX_STAR -> {
                    if (it.max == 0) return false

                    val div = it.score * 1.0 / it.max

                    val level = if (div >= 0.97) {
                        5
                    } else if (div >= 0.95) {
                        4
                    } else if (div >= 0.93) {
                        3
                    } else if (div >= 0.9) {
                        2
                    } else if (div >= 0.85) {
                        1
                    } else {
                        0
                    }

                    fit(operator, level, int)
                }
                RATING -> fit(operator, it.rating, int)
                RANK -> {
                    val rankArray = arrayOf("F", "D", "C", "B", "BB", "BBB", "A", "AA", "AAA", "S", "S+", "SS", "SS+", "SSS", "SSS+")

                    val ir = rankArray.indexOf(it.rank.uppercase().replace('P', '+'))

                    val cr = rankArray.indexOf(condition.uppercase().replace('P', '+'))

                    if (cr == -1) {
                        throw IllegalArgumentException.WrongException.Rank()
                    }

                    fit(operator, ir, cr)
                }
                COMBO -> {
                    val comboArray = arrayOf(ComboType.PASS, ComboType.FC, ComboType.FC_PLUS, ComboType.AP, ComboType.AP_PLUS)

                    val ic = comboArray.indexOf(ComboType.getCombo(it.combo))
                    val cc = comboArray.indexOf(ComboType.getCombo(condition))

                    fit(operator, ic, cc)
                }
                SYNC -> {
                    val syncArray = arrayOf(SyncType.PASS, SyncType.SYNC, SyncType.FS, SyncType.FS_PLUS, SyncType.FDX, SyncType.FDX_PLUS)

                    val ic = syncArray.indexOf(SyncType.getSync(it.sync))
                    val cc = syncArray.indexOf(SyncType.getSync(condition))

                    fit(operator, ic, cc)
                }
                else -> false
            }
        }

        /**
         * 公用方法
         * 检测一个难度是否在输入的难度区间内
         */
        fun fitRange(operator: Operator, compare: String?, to: Double): Boolean {

            val intRange = if (compare.isNullOrBlank()) {
                10..150
            } else if (compare.contains(REG_HYPHEN.toRegex())) {
                val s = compare.split(REG_HYPHEN.toRegex()).map { it.trim() }

                if (s.size == 2) {
                    val f = parseLevel(s.first(), isAccurate = true)
                    val l = parseLevel(s.last(), isAccurate = true)

                    val min = min(min(f.first, f.last), min(l.first, l.last))
                    val max = max(max(f.first, f.last), max(l.first, l.last))

                    IntRange(min, max)
                } else {
                    parseLevel(s.first(), isAccurate = false)
                }
            } else {
                parseLevel(compare)
            }

            val t = (to * 10.0).roundToInt()

            return when(operator) {
                Operator.XQ -> t == intRange.first
                Operator.EQ -> t in intRange

                Operator.NE -> t !in intRange
                Operator.GE -> t >= intRange.last
                Operator.GT -> t > intRange.last
                Operator.LE -> t <= intRange.first
                Operator.LT -> t < intRange.first
            }
        }

        /**
         * 返回等级 x 10
         * @param isAccurate 如果为真，则 13 会匹配成 13.0。否则只会匹配成 13.0-13.5。
         */
        private fun parseLevel(level: String, isAccurate: Boolean = false): IntRange {
            if (level.contains(REG_PLUS.toRegex())) {
                val i10 = level.dropLastWhile { it == '?' || it == '？' }.dropLastWhile { it == '+' || it == '＋' }.toDouble() * 10.0

                return IntRange(i10.toInt() + 6, i10.toInt() + 9)
            } else if (level.contains('.') || isAccurate) {
                // 精确定级
                val i10 = level.dropLastWhile { it == '?' || it == '？' }.toDouble() * 10.0

                return i10.toInt()..i10.toInt()
            } else {
                // 模糊定级
                val i10 = level.dropLastWhile { it == '?' || it == '？' }.toDouble() * 10.0

                return i10.toInt()..(i10.toInt() + 5)
            }
        }
    }
}