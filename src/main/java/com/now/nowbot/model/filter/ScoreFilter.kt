package com.now.nowbot.model.filter

import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.enums.OsuGenre
import com.now.nowbot.model.enums.OsuLanguage
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.TimeParser
import com.now.nowbot.util.command.*

import org.intellij.lang.annotations.Language
import java.time.ZoneOffset
import kotlin.math.*

enum class ScoreFilter(@param:Language("RegExp") val regex: Regex) {
    CREATOR("(creator|host|c|h|谱师|作者|谱|主)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NAME)".toRegex()),

    GUEST("((gd(er)?|guest\\s*diff(er)?)|mapper|guest|g?u|客串?(谱师)?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NAME)".toRegex()),

    BID("((beatmap\\s*)?id|bid|b|(谱面)?编?号)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    SID("((beatmap\\s*)?setid|sid|s|(谱面)?集编号)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_MORE)".toRegex()),

    TITLE("(title|name|song|t|歌?曲名|歌曲|标题)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    ARTIST("(artist|singer|art|f?a|艺术家|曲师?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    SOURCE("(source|src|from|f|o|sc|se|来?源)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    TAG("(tags?|ta|tg|w|标签?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    ANY("(any(thing)?|y|任[何意]?(字段|文字)?|[字文])(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    GENRE("(genre|g|曲?风|风格|流派?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    LANGUAGE("(languages?|l|la|lang|语[言种]?|文字?)(?<n>$PATTERN_OPERATOR_WITH_SPACE(?=$PATTERN_LANGUAGE_VALUE)$PATTERN_ANYTHING_MORE)".toRegex()),

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

    LENGTH("(length|drain|time|长度?|时[长间]|l)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_DURATION)".toRegex()),

    BPM("(bpm|曲速|速度|bm)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)".toRegex()),

    ACCURACY("(accuracy|精[确准][率度]?|准确?[率度]|acc?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL)[%％]?".toRegex()),

    COMBO("(combo|连击|cb)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_NUMBER_DECIMAL[xX]?)".toRegex()),

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

    CLIENT("(client|v|version|版本?)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    CATEGORY("(type|category|status|类型?|[分种]类|状态?|z|$PATTERN_STAR)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_ANYTHING_MORE)".toRegex()),

    CREATED_TIME("(date|at|creat(ed)?(\\s*(at|time))?|earn(ed)?(\\s*(at|time))?|score\\s*(at|time)|(成绩(创建)?|创建)(时间)?|st|ct|ca|ti)(?<n>$PATTERN_OPERATOR_WITH_SPACE$PATTERN_TIME)".toRegex()),

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

                CATEGORY -> fit(operator, DataUtil.getStatus(it.beatmap.status), DataUtil.getStatus(str))

                CREATED_TIME -> fitTime(operator, it.endedTime.atZoneSameInstant(ZoneOffset.UTC).toEpochSecond(), str)

                else -> false
            }
        }

        // private val log = LoggerFactory.getLogger(ScoreFilter::class.java)
    }
}