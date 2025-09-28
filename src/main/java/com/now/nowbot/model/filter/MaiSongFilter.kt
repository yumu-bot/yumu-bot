package com.now.nowbot.model.filter

import com.now.nowbot.model.enums.*
import com.now.nowbot.model.filter.ScoreFilter.Companion.fit
import com.now.nowbot.model.filter.ScoreFilter.Companion.fitCountOrPercent
import com.now.nowbot.model.maimai.MaiSong
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language

enum class MaiSongFilter(@Language("RegExp") val regex: Regex) {
    CHARTER("(chart(er)?|mapper|谱师?|c)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    ID("((song\\s*)?id|(歌曲)?编?号|i)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    DIFFICULTY("(difficulty|diff|难度?|d)(?<n>$REG_OPERATOR_WITH_SPACE$REG_MAI_DIFFICULTY)".toRegex()),

    DIFFICULTY_NAME("(difficulty|diff|难度?|d)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    CABINET("(cabinet|框体?|cab|ca|n)(?<n>$REG_OPERATOR_WITH_SPACE$REG_MAI_CABINET)".toRegex()),

    VERSION("(version|版本?|v)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    TITLE("(title|name|song|歌?曲|名|歌?曲名|标题|t)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    ALIASES("(alias|aliases|外号|绰号|别名?|l)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    ARTIST("(artist|singer|art|艺术家|歌手?|歌手名|a)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    CATEGORY("(type|category|genre|类型?|[分种]类|t|g)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    BPM("(bpm|曲?速|b|bm)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    TAP("(tap|点击?|ta|tp)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    HOLD("(hold|长?按|hod|ho|hd)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    SLIDE("(slider?|[滑划]动?|sld|sl|se)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    TOUCH("(touch|触?摸|toh|tch|th|to)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    BREAK("(break|绝?赞|brk|br|bk)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    DX_SCORE("(dx\\s*score|score|dx\\s*分数?|分数?|dx|ds|o)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    RANGE(REG_MAI_RANGE.toRegex()),
    ;

    companion object {
        fun filterSongs(songs: List<MaiSong>, conditions: List<List<String>>, difficulties: List<MaiDifficulty>): List<MaiSong> {
            var collect = listOf<Pair<MaiSong, List<MaiDifficulty>>>()

            val el = entries.toList()

            // 最后一个筛选条件无需匹配
            conditions
                .dropLast(1)
                .forEachIndexed { index, strings ->
                    if (strings.isNotEmpty()) {
                        val fit = filterConditions(songs, el[index], strings)

                        collect = getOrIntersect(collect, fit)
                    }
                }

            val d = difficulties.toSet()

            // 在这里设置高亮难度
            return collect.filter {
                if (d.isEmpty()) {
                    true
                } else {
                    d.containsAll(it.second.toSet())
                }
            }.map { r ->
                val highlight = r.second
                    .map { diff -> MaiDifficulty.getIndex(diff) }

                r.first.updateHighlight(highlight)

                r.first
            }
        }

        /**
         * 如果 collection 为空，则返回 fit。否则，返回它们之间的交集
         */
        private fun getOrIntersect(collection: List<Pair<MaiSong, List<MaiDifficulty>>>, fit: List<Pair<MaiSong, List<MaiDifficulty>>>): List<Pair<MaiSong, List<MaiDifficulty>>> {
            if (collection.isEmpty()) {
                return fit
            } else {
                // 根据歌曲名称和难度列表取交集
                val intersection = collection.filter { col ->
                    fit.any { f ->
                        f.first.songID == col.first.songID
                    }
                }

                val set = fit.associate { it.first.songID to it.second }

                return intersection.map { pair ->
                    val new = set[pair.first.songID]

                    if (new != null) {
                        pair.first to new.toSet().intersect(pair.second.toSet()).toList()
                    } else {
                        pair
                    }
                }
            }
        }

        private fun filterConditions(songs: List<MaiSong>, filter: MaiSongFilter, conditions: List<String>): List<Pair<MaiSong, List<MaiDifficulty>>> {
            var collect = listOf<Pair<MaiSong, List<MaiDifficulty>>>()

            for (c in conditions) {
                val operator = Operator.getOperator(c)
                val condition = (c.split(REG_OPERATOR_WITH_SPACE.toRegex()).lastOrNull() ?: "").trim()

                val fit = songs.map { s ->
                    val r = fitSong(s, operator, filter, condition)

                    Triple(r.first, r.second, s)
                }
                    .filter { it.first }
                    .map { it.third to it.second }

                collect = getOrIntersect(collect, fit)
            }

            return collect
        }

        /**
         * 如果返回的 list 为空，则视作匹配所有难度
         */
        private fun fitSong(it: MaiSong, operator: Operator, filter: MaiSongFilter, condition: String): Pair<Boolean, List<MaiDifficulty>> {
            val int = condition.toIntOrNull() ?: -1
            val double = condition.toDoubleOrNull() ?: -1.0
            val default = listOf<MaiDifficulty>()

            val levelArray = when (it.charts.size) {
                4 -> arrayListOf(0, 1, 2, 3)
                5 -> arrayListOf(0, 1, 2, 3, 4)
                else -> arrayListOf(5)
            }

            return when(filter) {
                CHARTER -> {
                    val list = it.charts
                        .mapIndexed { i, chart ->

                            val l = MaiDifficulty.getDifficulty(levelArray.getOrNull(i) ?: -1)

                            l to chart.charter
                        }
                        .filter { pair -> pair.second != "-" }

                    val con = MaiCharter.getCharter(condition).sorted().joinToString(" ")

                    val result = list.map { l ->
                        val charter = l.second

                        val cha = MaiCharter.getCharter(charter).sorted().joinToString(" ")

                        fit(operator, cha, con) to l.first
                    }.toSet()

                    val diff = result.filter { it.first }.map { it.second }

                    return if (diff.isEmpty()) {
                        false to default
                    } else {
                        true to diff
                    }
                }

                ID -> fit(operator, it.songID % 10000L, int.toLong() % 10000L) to default
                DIFFICULTY -> {
                    val result = it.star.mapIndexed{ i, sr ->

                        val f = fit(operator, sr, double, digit = 1, isRound = false, isInteger = true)

                        val l = MaiDifficulty.getDifficulty(levelArray.getOrNull(i) ?: -1)

                        f to l
                    }

                    val diff = result.filter { it.first }.map { it.second }

                    return if (diff.isEmpty()) {
                        false to default
                    } else {
                        true to diff
                    }
                }
                DIFFICULTY_NAME -> {
                    val con = MaiDifficulty.getIndex(MaiDifficulty.getDifficulty(condition))

                    // 如果不是查询宴会场，就不会返回宴会场的数据
                    if (con != 5 && levelArray.contains(5)) return false to default

                    val result = levelArray.map { level ->
                        fit(operator, level, con) to level
                    }.toSet()

                    val diff = result.filter { it.first }.map { MaiDifficulty.getDifficulty(it.second) }

                    return if (diff.isEmpty()) {
                        false to default
                    } else {
                        true to diff
                    }
                }

                CABINET -> fit(operator, MaiCabinet.getCabinet(condition), MaiCabinet.getCabinet(it.type)) to default
                VERSION -> fit(
                    operator,
                    MaiVersion.getVersionList(it.info.version).joinToString(" ") { it.abbreviation },
                    MaiVersion.getVersionList(condition).joinToString(" ") { it.abbreviation }
                ) to default

                TITLE -> fit(operator, it.title, condition) to default
                ALIASES -> {
                    val a = it.aliases ?: listOf()

                    return a.map { alias ->
                        fit(operator, alias, condition)
                    }.toSet().contains(true) to default
                }

                ARTIST -> fit(operator, it.info.artist, condition) to default

                CATEGORY -> {
                    val con = MaiCategory.getCategory(condition)
                    val gen = MaiCategory.getCategory(it.info.genre)

                    val fit = fit(operator, gen, con)

                    fit to default
                }

                BPM -> fit(operator, it.info, int) to default

                TAP -> {
                    val result = it.charts.mapIndexed{ i, chart ->
                        val t = chart.notes.total

                        val f = fitCountOrPercent(operator, chart.notes.tap, condition, t)

                        val l = MaiDifficulty.getDifficulty(levelArray.getOrNull(i) ?: -1)

                        f to l
                    }

                    val diff = result.filter { it.first }.map { it.second }

                    return if (diff.isEmpty()) {
                        false to default
                    } else {
                        true to diff
                    }
                }

                HOLD -> {
                    val result = it.charts.mapIndexed{ i, chart ->
                        val t = chart.notes.total

                        val f = fitCountOrPercent(operator, chart.notes.hold, condition, t)

                        val l = MaiDifficulty.getDifficulty(levelArray.getOrNull(i) ?: -1)

                        f to l
                    }

                    val diff = result.filter { it.first }.map { it.second }

                    return if (diff.isEmpty()) {
                        false to default
                    } else {
                        true to diff
                    }
                }
                SLIDE -> {
                    val result = it.charts.mapIndexed{ i, chart ->
                        val t = chart.notes.total

                        val f = fitCountOrPercent(operator, chart.notes.slide, condition, t)

                        val l = MaiDifficulty.getDifficulty(levelArray.getOrNull(i) ?: -1)

                        f to l
                    }

                    val diff = result.filter { it.first }.map { it.second }

                    return if (diff.isEmpty()) {
                        false to default
                    } else {
                        true to diff
                    }
                }
                TOUCH -> {
                    val result = it.charts.mapIndexed{ i, chart ->
                        val t = chart.notes.total

                        val f = fitCountOrPercent(operator, chart.notes.touch, condition, t)

                        val l = MaiDifficulty.getDifficulty(levelArray.getOrNull(i) ?: -1)

                        f to l
                    }

                    val diff = result.filter { it.first }.map { it.second }

                    return if (diff.isEmpty()) {
                        false to default
                    } else {
                        true to diff
                    }
                }
                BREAK -> {
                    val result = it.charts.mapIndexed{ i, chart ->
                        val t = chart.notes.total

                        val f = fitCountOrPercent(operator, chart.notes.break_, condition, t)

                        val l = MaiDifficulty.getDifficulty(levelArray.getOrNull(i) ?: -1)

                        f to l
                    }

                    val diff = result.filter { it.first }.map { it.second }

                    return if (diff.isEmpty()) {
                        false to default
                    } else {
                        true to diff
                    }
                }
                DX_SCORE -> {
                    val result = it.charts.mapIndexed{ i, chart ->
                        val f = fit(operator, chart.dxScore, int)

                        val l = MaiDifficulty.getDifficulty(levelArray.getOrNull(i) ?: -1)

                        f to l
                    }

                    val diff = result.filter { it.first }.map { it.second }

                    return if (diff.isEmpty()) {
                        false to default
                    } else {
                        true to diff
                    }
                }

                else -> false to default
            }
        }
    }
}