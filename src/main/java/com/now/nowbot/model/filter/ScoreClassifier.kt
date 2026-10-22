package com.now.nowbot.model.filter

import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.enums.OsuGenre
import com.now.nowbot.model.enums.OsuLanguage
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.TimeParser
import com.now.nowbot.util.command.LEVEL_MAYBE
import com.now.nowbot.util.command.PATTERN_DURATION_NUMBER
import com.now.nowbot.util.command.PATTERN_LANGUAGE_VALUE
import com.now.nowbot.util.command.PATTERN_STAR
import com.now.nowbot.util.command.StripContext
import org.intellij.lang.annotations.Language
import java.time.ZoneOffset
import kotlin.math.min
import kotlin.math.roundToLong

enum class ScoreClassifier(
    @param:Language("RegExp") override val prefix: Regex,
    override val valueType: FilterValueType,
    @param:Language("RegExp") override val suffix: Regex? = null,
    @param:Language("RegExp") override val valueRegex: Regex? = null,
): CommandClassifier {
    // C H
    CREATOR("(creator|host|c|h|谱师|作者|谱|主)".toRegex(), FilterValueType.NAME),

    // U
    GUEST("((gd(er)?|guest\\s*diff(er)?)|mapper|guest|g?u|客串?(谱师)?)".toRegex(), FilterValueType.NAME),

    // B
    BID("((beatmap\\s*)?id|bid|b|(谱面)?编?号)".toRegex(), FilterValueType.INTEGER),

    // S
    SID("((beatmap\\s*)?setid|sid|s|(谱面)?集编号)".toRegex(), FilterValueType.INTEGER),

    // T
    TITLE("(title|name|song|t|歌?曲名|歌曲|标题)".toRegex(), FilterValueType.ANY),

    // A
    ARTIST("(artist|singer|art|f?a|艺术家|曲师?)".toRegex(), FilterValueType.ANY),

    // F O
    SOURCE("(source|src|from|f|o|sc|se|来?源)".toRegex(), FilterValueType.ANY),

    // W
    TAG("(tags?|ta|tg|w|标签?)".toRegex(), FilterValueType.ANY),

    // Y
    ANY("(any(thing)?|y|任[何意]?(字段|文字)?|[字文])".toRegex(), FilterValueType.ANY),

    // G
    GENRE("(genre|g|曲?风|风格|流派?)".toRegex(), FilterValueType.ANY),

    // L
    LANGUAGE(
        "(languages?|l|la|lang|语[言种]?|文字?)".toRegex(),
        FilterValueType.ANY,
        valueRegex = Regex("^$PATTERN_LANGUAGE_VALUE"),
    ),

    // D
    DIFFICULTY("(difficult(y|ies)|diff|d|难度名?)".toRegex(), FilterValueType.ANY),

    // R
    STAR("(stars?|rating|sr|r|星数?)".toRegex(), FilterValueType.DECIMAL, "$PATTERN_STAR$LEVEL_MAYBE".toRegex()),

    // E
    SCORE("(scores?|sc|ss|e|分数?)".toRegex(), FilterValueType.DECIMAL),

    // J
    REPLAY("(replay|re?p|journal|j|回放|录像|记录)".toRegex(), FilterValueType.WORD),

    AR("(ar|approach\\s*(rate)?)".toRegex(), FilterValueType.DECIMAL),

    CS("(cs|circle\\s*(size)?|keys?|键)".toRegex(), FilterValueType.DECIMAL),

    OD("(od|overall\\s*(difficulty)?)".toRegex(), FilterValueType.DECIMAL),

    HP("(hp|health\\s*(point)?)".toRegex(), FilterValueType.DECIMAL),

    // P
    PERFORMANCE("(performance|表现分?|pp|p)".toRegex(), FilterValueType.DECIMAL),

    // K
    RANK("(rank(ing|s)?|评[价级]?|k)".toRegex(), FilterValueType.ANY),

    // L
    LENGTH(
        "(length|drain|time|长度?|时[长间]|l)".toRegex(),
        FilterValueType.TIME,
        valueRegex = Regex("^(?:$PATTERN_DURATION_NUMBER|[:：])"),
    ),

    BPM("(bpm|曲速|速度|bm)".toRegex(), FilterValueType.DECIMAL),

    ACCURACY("(accuracy|精[确准][率度]?|准确?[率度]|acc?)".toRegex(), FilterValueType.DECIMAL, "[%％]?".toRegex()),

    COMBO("(combo|连击|cb)".toRegex(), FilterValueType.DECIMAL, "[xX]?".toRegex()),

    PERFECT("(perfect|320|305|彩|完美|pf)".toRegex(), FilterValueType.DECIMAL),

    GREAT("(great|300|大果?|fruits?|fr|良|黄|gr|很好)".toRegex(), FilterValueType.DECIMAL),

    MISSED_FRUIT("(miss(ed)?\\s*fruits?|漏大果?|mf)".toRegex(), FilterValueType.DECIMAL),

    MISSED_DROP("(miss(ed)?\\s*drop|漏中果?|md)".toRegex(), FilterValueType.DECIMAL),

    MISSED_DROPLET("(miss(ed)?\\s*droplet|漏小?果?|ml)".toRegex(), FilterValueType.DECIMAL),

    GOOD("(good|200|绿|gd|良好)".toRegex(), FilterValueType.DECIMAL),

    OK("(ok|150|100|中果?|large\\s*drop(let)?|ld|(?<!不)可|蓝|ba?d|可以)".toRegex(), FilterValueType.DECIMAL),

    MEH("(me?h|小果?|drop(let)?|sd|p(oo)?r|灰|50|一般)".toRegex(), FilterValueType.DECIMAL),

    // X
    MISS("(m(is)?s|0|x|不可|红|失误|漏击)".toRegex(), FilterValueType.DECIMAL),

    // M
    MOD("((m(od)?s?)|模组?)".toRegex(), FilterValueType.MOD),

    // Q
    RATE("(rate|彩[率比]|黄彩比?|q|pm)".toRegex(), FilterValueType.DECIMAL),

    CIRCLE("((hit)?circles?|hi?t|click|rice|ci|cr|rc|圆圈?|米)".toRegex(), FilterValueType.DECIMAL),

    SLIDER("(slider?s?|sl|long(note)?|lns?|[滑长]?条|长键|面)".toRegex(), FilterValueType.DECIMAL),

    SPINNER("(spin(ner)?s?|rattle|sp|转盘|[转盘])".toRegex(), FilterValueType.DECIMAL),

    // N
    TOTAL("(notes?|total|all|ttl|(hit)?objects?|tt|n|(物件|音符)数?|总数?)".toRegex(), FilterValueType.INTEGER),

    CONVERT("(convert|cv|转谱?)".toRegex(), FilterValueType.WORD),

    // V
    CLIENT("(client|v|version|版本?)".toRegex(), FilterValueType.ANY),

    // Z
    CATEGORY("(type|category|status|类型?|[分种]类|状态?|z|$PATTERN_STAR)".toRegex(), FilterValueType.ANY),

    CREATED_TIME("(date|at|creat(ed)?(\\s*(at|time))?|earn(ed)?(\\s*(at|time))?|score\\s*(at|time)|(成绩(创建)?|创建)(时间)?|st|ct|ca|ti)".toRegex(), FilterValueType.TIME),

    ALIASES("(alias(es)?|别名|外号|昵称)".toRegex(), FilterValueType.ANY);

    companion object {
        /**
         * 使用已经解析好的 [ctx] 筛选成绩。
         *
         * [StripContext.syntaxes] 里保存的是 `(分类器名, 运算符) -> 值列表`，
         * 分类器名对应 [ScoreClassifier.name]，值在提取阶段已经去掉了后缀（如 `%`、`x`、`★`）。
         * 同一个键下的多个值：范围型参数（DECIMAL/INTEGER/TIME）在 `>`、`>=`、`<`、`<=`、`!=`
         * 下取交集，在 `=`、`==` 下取并集；非范围型参数始终取并集。
         * 不同键之间始终取交集。没有任何成绩条件时返回原列表。
         */
        fun Collection<LazerScore>.classify(ctx: StripContext): List<LazerScore> {
            return classify(ctx.syntaxes)
        }

        /**
         * @param syntaxes 解析结果，键为 `(分类器名, 运算符)`，值为该条件的所有取值
         */
        fun Collection<LazerScore>.classify(
            syntaxes: Map<Pair<String, Operator>, List<String>>
        ): List<LazerScore> {
            val predicates = parsePredicates(syntaxes)
            if (predicates.isEmpty()) return toList()

            return filter { score ->
                predicates.all { predicate -> predicate(score) }
            }
        }

        /**
         * 与 [Collection.classify] 相同，但保留键值对：只按成绩筛值，键（名次、行号等）原样带出。
         *
         * 用于「先固定位置、再筛选成绩」的场景，避免筛选后从结果反推位置而丢掉原集合的信息。
         */
        fun <K> Map<K, LazerScore>.classify(ctx: StripContext): Map<K, LazerScore> {
            return classify(ctx.syntaxes)
        }

        /**
         * 按值筛选 [LazerScore]，保留原键与遍历顺序。
         *
         * 先复制成 [LinkedHashMap] 再筛值，因此返回的 map 可以继续增删；没有成绩条件时返回原 map 的副本。
         */
        fun <K> Map<K, LazerScore>.classify(
            syntaxes: Map<Pair<String, Operator>, List<String>>
        ): Map<K, LazerScore> {
            val filtered: MutableMap<K, LazerScore> = LinkedHashMap(this)
            val predicates = parsePredicates(syntaxes)

            if (predicates.isNotEmpty()) {
                filtered.values.removeIf { score ->
                    !predicates.all { predicate -> predicate(score) }
                }
            }

            return filtered
        }

        /**
         * 将 [StripContext.syntaxes] 解析为一组可复用的谓词函数 (Score -> Boolean)。
         *
         * 同一个键（同一分类器 + 同一运算符）下的多个值：
         * - 范围型（[FilterValueType.DECIMAL]、[FilterValueType.INTEGER]、[FilterValueType.TIME]）
         *   且运算符为 `>`、`>=`、`<`、`<=`、`!=` 时取交集，
         *   例如 `pp>=600 pp>=700` 等价于 `pp>=700`；
         * - 范围型但运算符为 `=`、`==` 时取并集，例如 `bid=1 bid=2` 表示两个谱面都可以；
         * - 非范围型始终取并集，例如 `title=A title=B`、`mod=HD mod=HR`。
         *
         * 不同键各生成一个谓词，最终在 [classify] 中取交集。
         */
        private fun parsePredicates(
            syntaxes: Map<Pair<String, Operator>, List<String>>
        ): List<(LazerScore) -> Boolean> {
            val predicates = mutableListOf<(LazerScore) -> Boolean>()

            for ((key, values) in syntaxes) {
                val (name, operator) = key
                val filter = entries.firstOrNull { it.name == name } ?: continue
                val conditions = values.map(::Condition)

                if (conditions.isEmpty()) continue

                val intersection = filter.valueType.isRangeType &&
                    operator != Operator.EQ && operator != Operator.XQ

                predicates.add { score ->
                    if (intersection) {
                        conditions.all { condition -> fitScore(score, operator, filter, condition) }
                    } else {
                        conditions.any { condition -> fitScore(score, operator, filter, condition) }
                    }
                }
            }

            return predicates
        }

        private fun fitScore(it: LazerScore, operator: Operator, filter: ScoreClassifier, condition: Condition): Boolean {
            val long = condition.long
            val double = condition.double
            val str = condition.condition
            val decimal = condition.decimal
            val boolean = condition.boolean

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
                TITLE -> (fitAlias(operator, it.beatmapset.aliases, condition.string)
                        || fit(operator, it.beatmapset.title, str)
                        || fit(operator, it.beatmapset.titleUnicode, str))
                ARTIST -> (fit(operator, it.beatmapset.artist, str)
                        || fit(operator, it.beatmapset.artistUnicode, str))
                SOURCE -> fit(operator, it.beatmapset.source, str)
                TAG -> fitTags(operator, it.beatmapset.tags, str)

                ANY -> {
                    fitAlias(operator, it.beatmapset.aliases, condition.string)
                            || fitTags(operator, it.beatmapset.tags, str)
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
                    val seconds = TimeParser.parseDurationSeconds(str)

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

                OK -> if (it.mode.safeModeValue != 2.toByte()) {
                    fitCountOrPercent(operator, it.statistics.ok, decimal, it.maximumStatistics.ok)
                } else {
                    fitCountOrPercent(operator, it.statistics.ok, decimal, it.maximumStatistics.largeTickHit)
                }

                MEH -> if (it.mode.safeModeValue != 2.toByte()) {
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

                    (it.mode.safeModeValue == 2.toByte()) && fitCountOrPercent(operator, compare, decimal, it.maximumStatistics.great)
                }

                MISSED_DROP -> (it.mode.safeModeValue == 2.toByte()) && it.maximumStatistics.largeTickHit > 0 &&
                        fitCountOrPercent(operator, it.statistics.largeTickMiss, decimal, it.maximumStatistics.largeTickHit)

                MISSED_DROPLET -> (it.mode.safeModeValue == 2.toByte()) && it.maximumStatistics.smallTickHit > 0 &&
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

                CONVERT -> it.beatmap.convert == boolean

                CLIENT -> when (str.lowercase()) {
                    "lazer", "l", "lz", "lzr" -> it.isLazer
                    "stable", "s", "st", "stb" -> !it.isLazer
                    else -> !it.isLazer
                }

                CATEGORY -> fit(operator, DataUtil.getStatus(it.beatmap.status), DataUtil.getStatus(str))

                CREATED_TIME -> fitTime(operator, it.endedTime.atZoneSameInstant(ZoneOffset.UTC).toEpochSecond(), str)
                ALIASES -> fitAlias(operator, it.beatmapset.aliases, condition.string)
            }
        }
    }
}