package com.now.nowbot.model.enums

import com.now.nowbot.model.enums.ScoreFilter.Companion.fit
import com.now.nowbot.model.maimai.MaiScore
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language
import kotlin.math.roundToInt

enum class MaiScoreFilter(@Language("RegExp") val regex: Regex) {
    CHARTER("(chart(er)?|mapper|谱师?|c)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    ID("(id|编?号|i)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER$LEVEL_MORE)".toRegex()),

    DIFFICULTY("(difficulty|diff|难度?|d)(?<n>$REG_OPERATOR_WITH_SPACE$REG_MAI_DIFFICULTY)".toRegex()),

    DIFFICULTY_NAME("(difficulty|diff|难度?|d)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    CABINET("(cabinet|框体?|n)(?<n>$REG_OPERATOR_WITH_SPACE$REG_MAI_CABINET)".toRegex()),

    VERSION("(version|版本?|v)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    TITLE("(title|name|song|曲|名|曲名|标题|t)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    ALIASES("(alias|aliases|外号|绰号|别名?|l)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    ARTIST("(artist|singer|art|艺术家|歌手?|a)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    CATEGORY("(type|category|种类?|t)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    BPM("(bpm|b)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    ACHIEVEMENT("(accuracy|达成率?|achieve(ment)?|acc|ach)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)[%％]?".toRegex()),

    TAP("(tap|ta|tp)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER$LEVEL_MORE)".toRegex()),

    HOLD("(hold|hod|ho|hd)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER$LEVEL_MORE)".toRegex()),

    SLIDE("(slider?|sld|sl|se)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER$LEVEL_MORE)".toRegex()),

    TOUCH("(touch|toh|tch|th|to)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER$LEVEL_MORE)".toRegex()),

    BREAK("(break|brk|br|bk)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER$LEVEL_MORE)".toRegex()),

    DX_SCORE("(dx\\s*score|score|dx分|分|dx|x|s)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER$LEVEL_MORE)".toRegex()),

    DX_STAR("(dx\\s*star|star|dx星|星|sr|r)(?<n>$REG_OPERATOR_WITH_SPACE[0-5])".toRegex()),

    RATING("(rating|评分|ra|rt|t)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER$LEVEL_MORE)".toRegex()),

    RANK("(rank|评价|rk|k)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER$LEVEL_MORE)".toRegex()),

    COMBO("(combo|连击|cb)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    SYNC("(sync|同步|sy)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    RANGE(REG_MAI_RANGE.toRegex()),
    ;

    companion object {
        enum class ComboType {
            PASS, FC, FC_PLUS, AP, AP_PLUS;

            companion object {
                fun getCombo(str: String?): ComboType {
                    return when(str?.dropWhile { it == ' ' }?.lowercase()) {
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
                    return when(str?.dropWhile { it == ' ' }?.lowercase()) {
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

            // 最后一个筛选条件无需匹配
            conditions.take(MaiScoreFilter.entries.size - 1).forEachIndexed { index, strings ->
                if (strings.isNotEmpty()) {
                    filterConditions(s, MaiScoreFilter.entries.toList()[index], strings)
                }
            }

            return s.toList()
        }

        private fun filterConditions(scores: MutableList<MaiScore>, filter: MaiScoreFilter, conditions: List<String>) {
            for (c in conditions) {
                val operator = Operator.getOperator(c)
                val condition = (c.split(REG_OPERATOR_WITH_SPACE.toRegex()).lastOrNull() ?: "").trim()

                scores.removeIf { MaiScoreFilter.fitScore(it, operator, filter, condition).not() }
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

                DIFFICULTY -> fit(operator, it.star, double)

                DIFFICULTY_NAME -> fit(operator, MaiDifficulty.getDifficulty(it.difficulty).name, MaiDifficulty.getDifficulty(condition).name)

                CABINET -> fit(operator, MaiCabinet.getCabinet(condition).name, MaiCabinet.getCabinet(it.type).name)
                VERSION -> fit(operator,
                    MaiVersion.getVersionList(it.version).joinToString(" ") { it.abbreviation },
                    MaiVersion.getVersionList(condition).joinToString(" ") { it.abbreviation }
                )
                TITLE -> fit(operator, it.title, condition)
                ALIASES -> run {
                    it.aliases?.map { alias ->
                        fit(operator, alias, condition)
                    }?.contains(true) ?: false
                }
                ARTIST -> fit(operator, it.artist, condition)
                CATEGORY -> fit(operator, MaiCategory.getCategory(it.genre).english, MaiCategory.getCategory(condition).english)
                BPM -> fit(operator, it.bpm, int)
                ACHIEVEMENT -> fit(operator, (it.achievements * 10000).roundToInt(), (double * 10000).roundToInt(), isPlus = true)
                TAP -> fit(operator, it.notes[0], int)
                HOLD -> fit(operator, it.notes[1], int)
                SLIDE -> fit(operator, it.notes[2], int)
                TOUCH -> fit(operator, it.notes[3], int)
                BREAK -> fit(operator, it.notes[4], int)
                DX_SCORE -> fit(operator, it.score, int)
                DX_STAR -> run {
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
                RANK -> run {
                    val rankArray = arrayOf("F", "D", "C", "B", "BB", "BBB", "A", "AA", "AAA", "S", "S+", "SS", "SS+", "SSS", "SSS+")

                    val cr = rankArray.indexOf(condition.uppercase().replace('P', '+'))

                    val ir = rankArray.indexOf(it.rank.uppercase().replace('P', '+'))

                    if (cr == -1) {
                        throw IllegalArgumentException.WrongException.Rank()
                    }

                    fit(operator, ir, cr)
                }
                COMBO -> fit(operator, ComboType.getCombo(condition).name, ComboType.getCombo(it.combo).name)
                SYNC -> fit(operator, SyncType.getSync(condition).name, SyncType.getSync(it.sync).name)
                else -> false
            }
        }
    }
}