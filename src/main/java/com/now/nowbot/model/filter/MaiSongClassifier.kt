package com.now.nowbot.model.filter

import com.now.nowbot.model.enums.*
import com.now.nowbot.model.maimai.MaiSong
import com.now.nowbot.model.maimai.MaiSong.MaiChart.MaiNote
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language

/**
 * [MaiSongFilter] 的名字形式分类器。
 *
 * 条目顺序与 [MaiSongFilter] 保持一致（末尾的 RANGE 由
 * [com.now.nowbot.util.command.SymbolHead.RANGE] 处理），因此可以按 ordinal / name 与过滤器条目一一对应。
 *
 * [DIFFICULTY] 与 [DIFFICULTY_NAME] 的前缀相同，靠 [valueRegex] 区分：
 * `d=13+` 命中 [DIFFICULTY]，`d=Master` 落到 [DIFFICULTY_NAME]。
 */
enum class MaiSongClassifier(
    @param:Language("RegExp") override val prefix: Regex,
    override val valueType: FilterValueType,
    @param:Language("RegExp") override val suffix: Regex? = null,
    @param:Language("RegExp") override val valueRegex: Regex? = null,
): CommandClassifier {
    CHARTER("(chart(er)?|mapper|谱师?|c)".toRegex(), FilterValueType.ANY),

    ID("((song\\s*)?id|(歌曲)?编?号|i)".toRegex(), FilterValueType.INTEGER),

    DIFFICULTY(
        "(difficulty|diff|难度?|d)".toRegex(),
        FilterValueType.WORD,
        valueRegex = Regex("^$PATTERN_MAI_DIFFICULTY$"),
    ),

    DIFFICULTY_NAME("(difficulty|diff|难度?|d)".toRegex(), FilterValueType.ANY),

    CABINET(
        "(cabinet|框体?|cab|ca|n)".toRegex(),
        FilterValueType.WORD,
        valueRegex = Regex("^$PATTERN_MAI_CABINET$"),
    ),

    VERSION("(version|版本?|v)".toRegex(), FilterValueType.ANY),

    TITLE("(title|name|song|歌?曲|名|歌?曲名|标题|t)".toRegex(), FilterValueType.ANY),

    ALIASES("(alias|aliases|外号|绰号|别名?|l)".toRegex(), FilterValueType.ANY),

    ARTIST("(artist|singer|art|艺术家|歌手?|歌手名|a)".toRegex(), FilterValueType.ANY),

    CATEGORY("(type|category|genre|类型?|[分种]类|t|g)".toRegex(), FilterValueType.ANY),

    BPM("(bpm|曲?速|b|bm)".toRegex(), FilterValueType.DECIMAL),

    TAP("(tap|点击?|ta|tp)".toRegex(), FilterValueType.DECIMAL),

    HOLD("(hold|长?按|hod|ho|hd)".toRegex(), FilterValueType.DECIMAL),

    SLIDE("(slider?|[滑划]动?|sld|sl|se)".toRegex(), FilterValueType.DECIMAL),

    TOUCH("(touch|触?摸|toh|tch|th|to)".toRegex(), FilterValueType.DECIMAL),

    BREAK("(break|绝?赞|brk|br|bk)".toRegex(), FilterValueType.DECIMAL),

    DX_SCORE("(dx\\s*score|score|(dx)?分数?|dx|ds|o)".toRegex(), FilterValueType.DECIMAL),
    ;

    companion object {
        fun Collection<MaiSong>.classify(
            ctx: StripContext,
            difficulties: List<MaiDifficulty> = emptyList(),
        ): List<MaiSong> {
            return classify(ctx.syntaxes, difficulties)
        }

        fun Collection<MaiSong>.classify(
            syntaxes: Map<Pair<String, Operator>, List<String>>,
            difficulties: List<MaiDifficulty> = emptyList(),
        ): List<MaiSong> {
            val conditions = conditionsByFilter(syntaxes)

            var collect = listOf<Pair<MaiSong, List<MaiDifficulty>>>()
            var hasCondition = false

            entries.forEach { filter ->
                val list = conditions[filter].orEmpty()

                if (list.isNotEmpty()) {
                    hasCondition = true

                    collect = getOrIntersect(collect, filterConditions(this, filter, list))
                }
            }

            // 没有任何筛选条件时，全部歌曲都视为匹配（与 ScoreClassifier 的行为一致）。
            if (!hasCondition) {
                collect = map { it to emptyList() }
            }

            val d = difficulties.toSet()

            return collect.filter {
                d.isEmpty() || d.containsAll(it.second.toSet())
            }.map { r ->
                val highlight = r.second
                    .map { diff -> MaiDifficulty.getIndex(diff) }
                    .toSet()

                r.first.updateHighlight(highlight)

                r.first
            }
        }

        fun <K> Map<K, MaiSong>.classify(
            ctx: StripContext,
            difficulties: List<MaiDifficulty> = emptyList(),
        ): Map<K, MaiSong> {
            return classify(ctx.syntaxes, difficulties)
        }

        fun <K> Map<K, MaiSong>.classify(
            syntaxes: Map<Pair<String, Operator>, List<String>>,
            difficulties: List<MaiDifficulty> = emptyList(),
        ): Map<K, MaiSong> {
            val matched = values.classify(syntaxes, difficulties)

            return entries
                .filter { (_, song) -> matched.any { it === song } }
                .associate { it.key to it.value }
        }

        private fun conditionsByFilter(
            syntaxes: Map<Pair<String, Operator>, List<String>>
        ): Map<MaiSongClassifier, List<Pair<Operator, Condition>>> {
            val result = entries.associateWith { mutableListOf<Pair<Operator, Condition>>() }

            syntaxes.forEach { (key, values) ->
                val filter = entries.firstOrNull { it.name == key.first } ?: return@forEach
                val list = result.getValue(filter)

                values.forEach { value ->
                    list.add(key.second to Condition(value))
                }
            }

            return result
        }

        private fun filterConditions(
            songs: Collection<MaiSong>,
            filter: MaiSongClassifier,
            conditions: List<Pair<Operator, Condition>>,
        ): List<Pair<MaiSong, List<MaiDifficulty>>> {
            var collect = listOf<Pair<MaiSong, List<MaiDifficulty>>>()

            for ((operator, condition) in conditions) {
                val fit = songs.mapNotNull { s ->
                    val r = fitSong(s, operator, filter, condition)

                    if (r.first) s to r.second else null
                }

                collect = getOrUnion(collect, fit)
            }

            // 过滤掉多条件时部分条件不满足的结果
            return collect.filter { it.second.isEmpty() || (it.second.size >= conditions.size) }
        }

        /**
         * 如果返回的 list 为空，则视作匹配所有难度
         */
        private fun fitSong(
            it: MaiSong,
            operator: Operator,
            filter: MaiSongClassifier,
            condition: Condition,
        ): Pair<Boolean, List<MaiDifficulty>> {
            val int = condition.int
            val long = condition.long
            val str = condition.condition
            val decimal = condition.decimal

            val default = listOf<MaiDifficulty>()

            val levelArray = when (it.charts.size) {
                4 -> arrayListOf(0, 1, 2, 3)
                5 -> arrayListOf(0, 1, 2, 3, 4)
                else -> arrayListOf(5)
            }

            fun processNoteType(noteSelector: (MaiNote) -> Int): Pair<Boolean, List<MaiDifficulty>> {
                val diff = it.charts.mapIndexedNotNull { i, chart ->
                    val t = chart.maiNote.total
                    val noteCount = noteSelector(chart.maiNote)

                    val fits = fitCountOrPercent(operator, noteCount, decimal, t)

                    if (fits) {
                        MaiDifficulty.getDifficulty(levelArray.getOrNull(i) ?: -1)
                    } else {
                        null
                    }
                }

                return if (diff.isEmpty()) {
                    false to default
                } else {
                    true to diff
                }
            }

            return when (filter) {
                CHARTER -> {
                    val list = it.charts
                        .mapIndexed { i, chart ->
                            val l = MaiDifficulty.getDifficulty(levelArray.getOrNull(i) ?: -1)

                            l to chart.charter
                        }
                        .filter { pair -> pair.second != "-" }

                    val con = MaiCharter.getCharter(str).sorted().joinToString(" ")

                    val result = list.map { l ->
                        val charter = l.second

                        val cha = MaiCharter.getCharter(charter).sorted().joinToString(" ")

                        fit(operator, cha, con) to l.first
                    }.toSet()

                    val diff = result.filter { it.first }.map { it.second }

                    if (diff.isEmpty()) {
                        false to default
                    } else {
                        true to diff
                    }
                }

                ID -> fit(operator, it.songID % 10000L, long % 10000L) to default

                DIFFICULTY -> {
                    val result = it.star.mapIndexed { i, sr ->
                        val f = MaiScoreFilter.fitRange(operator, sr, str)

                        val l = MaiDifficulty.getDifficulty(levelArray.getOrNull(i) ?: -1)

                        f to l
                    }.filter {
                        it.first
                    }.map {
                        it.second
                    }

                    if (result.isEmpty()) {
                        false to default
                    } else {
                        true to result
                    }
                }

                DIFFICULTY_NAME -> {
                    val con = MaiDifficulty.getIndex(MaiDifficulty.getDifficulty(str))

                    // 如果不是查询宴会场，就不会返回宴会场的数据
                    if (con != 5 && levelArray.contains(5)) return false to default

                    val result = levelArray.map { level ->
                        fit(operator, level, con) to level
                    }.toSet()

                    val diff = result.filter { it.first }.map { MaiDifficulty.getDifficulty(it.second) }

                    if (diff.isEmpty()) {
                        false to default
                    } else {
                        true to diff
                    }
                }

                CABINET -> fit(operator, MaiCabinet.getCabinet(it.type, it.isUtage), MaiCabinet.getCabinet(str)) to default
                VERSION -> fit(operator, MaiVersion.getVersionList(it.info.version), MaiVersion.getVersionList(str)) to default

                TITLE -> fit(operator, it.title, str) to default
                ALIASES -> {
                    val a = it.aliases.orEmpty()

                    a.map { alias ->
                        fit(operator, alias, str)
                    }.contains(true) to default
                }

                ARTIST -> fit(operator, it.info.artist, str) to default

                CATEGORY -> {
                    val con = MaiCategory.getCategory(str)
                    val gen = MaiCategory.getCategory(it.info.genre)

                    fit(operator, gen, con) to default
                }

                BPM -> fit(operator, it.info.bpm, int) to default

                TAP -> processNoteType(MaiNote::tap)
                HOLD -> processNoteType(MaiNote::hold)
                SLIDE -> processNoteType(MaiNote::slide)
                TOUCH -> processNoteType(MaiNote::touch)
                BREAK -> processNoteType(MaiNote::`break`)

                DX_SCORE -> {
                    val result = it.charts.mapIndexed { i, chart ->
                        val f = fit(operator, chart.dxScore, int)

                        val l = MaiDifficulty.getDifficulty(levelArray.getOrNull(i) ?: -1)

                        f to l
                    }

                    val diff = result.filter { it.first }.map { it.second }

                    if (diff.isEmpty()) {
                        false to default
                    } else {
                        true to diff
                    }
                }
            }
        }

        /**
         * 如果 collection 为空，则返回 fit。否则，返回它们之间的交集
         * 应当只在双方都有具体难度约束时才取交集；如果其中一方是“全曲匹配（空集）”，则保留另一方的约束。
         */
        private fun getOrIntersect(
            collection: List<Pair<MaiSong, List<MaiDifficulty>>>,
            fit: List<Pair<MaiSong, List<MaiDifficulty>>>,
        ): List<Pair<MaiSong, List<MaiDifficulty>>> {
            if (collection.isEmpty()) {
                return fit
            }

            val intersection = collection.filter { col ->
                fit.any { f ->
                    f.first.songID == col.first.songID
                }
            }

            val set = fit.associate { it.first.songID to it.second }

            return intersection.map { pair ->
                val new = set[pair.first.songID]
                val old = pair.second

                val resultDiff = when {
                    new == null -> old
                    new.isEmpty() -> old
                    old.isEmpty() -> new
                    else -> old.toSet().intersect(new.toSet()).toList()
                }

                pair.first to resultDiff
            }
        }

        /**
         * 如果 collection 为空，则返回 fit。否则，返回它们之间的并集
         */
        private fun getOrUnion(
            collection: List<Pair<MaiSong, List<MaiDifficulty>>>,
            fit: List<Pair<MaiSong, List<MaiDifficulty>>>,
        ): List<Pair<MaiSong, List<MaiDifficulty>>> {
            if (collection.isEmpty()) {
                return fit
            }

            val collectionMap = collection.associateBy { it.first.songID }
            val fitMap = fit.associateBy { it.first.songID }

            val allSongIDs = (collectionMap.keys + fitMap.keys).toSet()

            return allSongIDs.map { songID ->
                val song = collectionMap[songID]?.first ?: fitMap[songID]!!.first

                val difficulties1 = collectionMap[songID]?.second.orEmpty()
                val difficulties2 = fitMap[songID]?.second.orEmpty()
                val mergedDifficulties = (difficulties1.toSet() + difficulties2.toSet()).toList()

                song to mergedDifficulties
            }.filter { it.second.isNotEmpty() }
        }
    }
}
