package com.now.nowbot.model.filter

import com.now.nowbot.model.enums.*
import com.now.nowbot.model.maimai.MaiScore
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * [MaiScoreFilter] 的名字形式分类器。
 *
 * 条目顺序与 [MaiScoreFilter] 保持一致（末尾的 RANGE 由
 * [com.now.nowbot.util.command.SymbolHead.RANGE] 处理），因此可以按 ordinal / name 与过滤器条目一一对应。
 *
 * [DIFFICULTY] 与 [DIFFICULTY_NAME] 的前缀相同，靠 [valueRegex] 区分：
 * `d=13+` 命中 [DIFFICULTY]，`d=Master` 落到 [DIFFICULTY_NAME]。
 */
enum class MaiScoreClassifier(
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

    ACHIEVEMENT("(accuracy|达成率?|精[确准][率度]?|准确?[率度]|achieve(ment)?|acc|ach|ac)".toRegex(), FilterValueType.DECIMAL, "[%％]?".toRegex()),

    TAP("(tap|点击?|ta|tp)".toRegex(), FilterValueType.DECIMAL),

    HOLD("(hold|长?按|hod|ho|hd)".toRegex(), FilterValueType.DECIMAL),

    SLIDE("(slider?|[滑划]动?|sld|sl|se)".toRegex(), FilterValueType.DECIMAL),

    TOUCH("(touch|触?摸|toh|tch|th|to)".toRegex(), FilterValueType.DECIMAL),

    BREAK("(break|绝?赞|brk|br|bk)".toRegex(), FilterValueType.DECIMAL),

    DX_SCORE("(dx\\s*score|score|(dx)?分数?|dx|ds|o)".toRegex(), FilterValueType.DECIMAL),

    DX_STAR(
        "(dx\\s*star|star|(dx)?星星?|星|dxsr|dr|sr|s)".toRegex(),
        FilterValueType.WORD,
        valueRegex = Regex("^[0-5]$"),
    ),

    RATING("(rating|评?分|r|ra|rt)".toRegex(), FilterValueType.INTEGER),

    RANK("(rank|评价?|rk|k)".toRegex(), FilterValueType.ANY),

    COMBO("(combo|连击?|cb|x)".toRegex(), FilterValueType.ANY),

    SYNC("(sync|同步?|sy|y)".toRegex(), FilterValueType.ANY),
    ;

    companion object {
        enum class ComboType {
            PASS, FC, FC_PLUS, AP, AP_PLUS;

            companion object {
                fun getCombo(str: String?): ComboType {
                    return when (str?.replace(Regex("\\s*"), "")?.lowercase()) {
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
                    return when (str?.replace(Regex("\\s*"), "")?.lowercase()) {
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

        fun Collection<MaiScore>.classify(ctx: StripContext): List<MaiScore> {
            return classify(ctx.syntaxes)
        }

        fun Collection<MaiScore>.classify(
            syntaxes: Map<Pair<String, Operator>, List<String>>
        ): List<MaiScore> {
            return classifyBy(syntaxes, entries) { score, operator, filter, condition ->
                fitScore(score, operator, filter, condition)
            }
        }

        fun <K> Map<K, MaiScore>.classify(ctx: StripContext): Map<K, MaiScore> {
            return classify(ctx.syntaxes)
        }

        fun <K> Map<K, MaiScore>.classify(
            syntaxes: Map<Pair<String, Operator>, List<String>>
        ): Map<K, MaiScore> {
            return classifyValuesBy(syntaxes, MaiScoreClassifier.entries) { score, operator, filter, condition ->
                fitScore(score, operator, filter, condition)
            }
        }

        private fun fitScore(
            it: MaiScore,
            operator: Operator,
            filter: MaiScoreClassifier,
            condition: Condition,
        ): Boolean {
            val int = condition.int
            val double = condition.double
            val str = condition.condition
            val decimal = condition.decimal

            return when (filter) {
                CHARTER -> fit(operator, MaiCharter.getCharter(it.charter), MaiCharter.getCharter(str))

                ID -> fit(operator, it.songID % 10000L, int.toLong() % 10000L)

                DIFFICULTY -> fitRange(operator, it.star, str)

                DIFFICULTY_NAME -> {
                    val con = MaiDifficulty.getIndex(MaiDifficulty.getDifficulty(str))

                    val dif = if (!it.isUtage) {
                        it.index
                    } else {
                        5
                    }

                    // 如果不是查询宴会场，就不会返回宴会场的数据
                    if (con != 5 && dif == 5) return false

                    fit(operator, dif, con)
                }

                CABINET -> fit(operator, MaiCabinet.getCabinet(it.type, it.isUtage), MaiCabinet.getCabinet(str))
                VERSION -> fit(
                    operator,
                    MaiVersion.getVersionList(it.version),
                    MaiVersion.getVersionList(str),
                )
                TITLE -> fit(operator, it.title, str)
                ALIASES -> {
                    it.aliases?.map { alias ->
                        fit(operator, alias, str)
                    }?.toSet()?.contains(true) ?: false
                }
                ARTIST -> fit(operator, it.artist, str)
                CATEGORY -> fit(operator, MaiCategory.getCategory(it.genre), MaiCategory.getCategory(str))
                BPM -> fit(operator, it.bpm, int)
                ACHIEVEMENT -> {
                    val acc = when {
                        double > 1010000.0 || double <= 0.0 -> throw IllegalArgumentException.WrongException.Henan()
                        double > 10100.0 -> double / 1000000.0
                        double > 101.0 -> double / 100.0
                        double > 1.0 -> double
                        else -> double * 100.0
                    }

                    fit(operator, it.achievements, acc)
                }
                TAP -> fitCountOrPercent(operator, it.notes[0], decimal, it.notes.sum())
                HOLD -> fitCountOrPercent(operator, it.notes[1], decimal, it.notes.sum())
                SLIDE -> fitCountOrPercent(operator, it.notes[2], decimal, it.notes.sum())
                TOUCH -> fitCountOrPercent(operator, it.notes[3], decimal, it.notes.sum())
                BREAK -> fitCountOrPercent(operator, it.notes[4], decimal, it.notes.sum())
                DX_SCORE -> fitCountOrPercent(operator, it.score, decimal, it.notes.sum() * 3)
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

                    val cr = rankArray.indexOf(str.uppercase().replace('P', '+'))

                    if (cr == -1) {
                        throw IllegalArgumentException.WrongException.Rank()
                    }

                    fit(operator, ir, cr)
                }
                COMBO -> {
                    val comboArray = arrayOf(ComboType.PASS, ComboType.FC, ComboType.FC_PLUS, ComboType.AP, ComboType.AP_PLUS)

                    val ic = comboArray.indexOf(ComboType.getCombo(it.combo))
                    val cc = comboArray.indexOf(ComboType.getCombo(str))

                    fit(operator, ic, cc)
                }
                SYNC -> {
                    val syncArray = arrayOf(SyncType.PASS, SyncType.SYNC, SyncType.FS, SyncType.FS_PLUS, SyncType.FDX, SyncType.FDX_PLUS)

                    val ic = syncArray.indexOf(SyncType.getSync(it.sync))
                    val cc = syncArray.indexOf(SyncType.getSync(str))

                    fit(operator, ic, cc)
                }
            }
        }

        /**
         * 公用方法
         * 检测 compare 是否在 to 输入的难度区间内
         */
        fun fitRange(operator: Operator, compare: Double, to: String?): Boolean {
            val intRange = if (to.isNullOrBlank()) {
                10..150
            } else if (to.contains(REGEX_HYPHEN)) {
                val s = to.split(REGEX_HYPHEN).map { it.trim() }

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
                parseLevel(to)
            }

            val t = (compare * 10.0).roundToInt()

            return when (operator) {
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
            if (level.contains(REGEX_PLUS)) {
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
