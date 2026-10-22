package com.now.nowbot.model.filter

import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.enums.OsuGenre
import com.now.nowbot.model.enums.OsuLanguage
import com.now.nowbot.model.enums.OsuMode.Companion.toOsuMode
import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.TimeParser
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language
import java.time.ZoneOffset

/**
 * [BeatmapsetFilter] 的名字形式分类器。
 *
 * 开头的 [HOST]、[MAPPER]、[GUEST] 与 [BeatmapsetFilter] 开头的同名条目同义（前缀与
 * [ScoreClassifier] 的同名条目合并）：
 * [HOST] 只匹配谱面集作者，[MAPPER] 匹配谱面谱师，[GUEST] 额外排除作者。
 *
 * 其余条目顺序与 [BeatmapsetFilter] 保持一致（末尾的 RANGE 由
 * [com.now.nowbot.util.command.SymbolHead.RANGE] 处理）。
 */
enum class BeatmapsetClassifier(
    @param:Language("RegExp") override val prefix: Regex,
    override val valueType: FilterValueType,
    @param:Language("RegExp") override val suffix: Regex? = null,
    @param:Language("RegExp") override val valueRegex: Regex? = null,
): CommandClassifier {
    // H
    HOST("(host|h|作者|主)".toRegex(), FilterValueType.NAME),

    // C
    MAPPER("(creator|mapper|map|charter|c|谱师?)".toRegex(), FilterValueType.NAME),

    GUEST("((gd(er)?|guest\\s*diff(er)?)|guest|gu|客串(谱师)?|客)".toRegex(), FilterValueType.NAME),

    BID("((beatmap\\s*)?id|bid|b|(谱面)?编?号)".toRegex(), FilterValueType.INTEGER),

    SID("((beatmap\\s*)?setid|sid|s|(谱面)?集编号)".toRegex(), FilterValueType.INTEGER),

    TITLE("(title|name|song|t|歌?曲名|歌曲|标题)".toRegex(), FilterValueType.ANY),

    ARTIST("(artist|singer|art|f?a|艺术家|曲师?)".toRegex(), FilterValueType.ANY),

    SOURCE("(source|src|from|f|o|sc|se|来?源)".toRegex(), FilterValueType.ANY),

    TAG("(tags?|ta|tg|w|标签?)".toRegex(), FilterValueType.ANY),

    ANY("(any(thing)?|y|任[何意]?(字段|文字)?|[字文])".toRegex(), FilterValueType.ANY),

    GENRE("(genre|g|曲?风|风格|流派?)".toRegex(), FilterValueType.ANY),

    LANGUAGE(
        "(languages?|l|la|lang|语[言种]?|文字?)".toRegex(),
        FilterValueType.ANY,
        valueRegex = Regex("^$PATTERN_LANGUAGE_VALUE"),
    ),

    DIFFICULTY("(difficult(y|ies)|diff|d|难度名?)".toRegex(), FilterValueType.ANY),

    STAR("(stars?|rating|sr|r|星数?)".toRegex(), FilterValueType.DECIMAL, "$PATTERN_STAR$LEVEL_MAYBE".toRegex()),

    MODE(
        "(mode|模式?|m)".toRegex(),
        FilterValueType.ANY,
        valueRegex = Regex("^$PATTERN_MODE$"),
    ),

    CATEGORY("(type|category|status|类型?|[分种]类|状态?|z|$PATTERN_STAR)".toRegex(), FilterValueType.ANY),

    AR("(ar|approach\\s*(rate)?)".toRegex(), FilterValueType.DECIMAL),

    CS("(cs|circle\\s*(size)?|keys?|键)".toRegex(), FilterValueType.DECIMAL),

    OD("(od|overall\\s*(difficulty)?)".toRegex(), FilterValueType.DECIMAL),

    HP("(hp|health\\s*(point)?)".toRegex(), FilterValueType.DECIMAL),

    LENGTH(
        "(length|drain|time|长度?|时[长间]|l)".toRegex(),
        FilterValueType.TIME,
        valueRegex = Regex("^(?:$PATTERN_DURATION_NUMBER|[:：])"),
    ),

    BPM("(bpm|曲速|速度|bm)".toRegex(), FilterValueType.DECIMAL),

    CIRCLE("((hit)?circles?|hi?t|click|rice|ci|cr|rc|圆圈?|米)".toRegex(), FilterValueType.DECIMAL),

    SLIDER("(slider?s?|sl|long(note)?|lns?|[滑长]?条|长键|面)".toRegex(), FilterValueType.DECIMAL),

    SPINNER("(spin(ner)?s?|rattle|sp|转盘|[转盘])".toRegex(), FilterValueType.DECIMAL),

    TOTAL("(notes?|total|all|ttl|(hit)?objects?|tt|n|(物件|音符)数?|总数?)".toRegex(), FilterValueType.INTEGER),

    CREATED_TIME("(at|(creat(ed)?|earn(ed)?)(\\s*(at|time))?|创建(时间)?|ct|ca)".toRegex(), FilterValueType.TIME),

    RANKED_TIME("(at|rank(ed)?(\\s*(at|time))?|上架(时间)?|rt|ra)".toRegex(), FilterValueType.TIME),

    PLAY_COUNT("(play\\s*counts?|游玩(次数)?|pc)".toRegex(), FilterValueType.INTEGER),

    FAVOURITE("(favou?rites?|favou?r|fav|fv|收藏(次数)?|收|藏)".toRegex(), FilterValueType.INTEGER),

    ALIASES("(alias(es)?|al|别名|外号|昵称)".toRegex(), FilterValueType.ANY),
    ;

    companion object {
        /**
         * 使用已经解析好的 [ctx] 筛选谱面集。
         *
         * 同一谱面集下的任意一张谱面满足条件，即视为该谱面集满足条件。
         */
        fun Collection<Beatmapset>.classify(ctx: StripContext): List<Beatmapset> {
            return classify(ctx.syntaxes)
        }

        fun Collection<Beatmapset>.classify(
            syntaxes: Map<Pair<String, Operator>, List<String>>
        ): List<Beatmapset> {
            return classifyBy(syntaxes, entries) { beatmapset, operator, filter, condition ->
                fitBeatmapset(beatmapset, operator, filter, condition)
            }
        }

        /**
         * 与 [Collection.classify] 相同，但保留键值对。
         */
        fun <K> Map<K, Beatmapset>.classify(ctx: StripContext): Map<K, Beatmapset> {
            return classify(ctx.syntaxes)
        }

        fun <K> Map<K, Beatmapset>.classify(
            syntaxes: Map<Pair<String, Operator>, List<String>>
        ): Map<K, Beatmapset> {
            return classifyValuesBy(syntaxes, BeatmapsetClassifier.entries) { beatmapset, operator, filter, condition ->
                fitBeatmapset(beatmapset, operator, filter, condition)
            }
        }

        private fun fitBeatmapset(
            s: Beatmapset,
            operator: Operator,
            filter: BeatmapsetClassifier,
            condition: Condition,
        ): Boolean {
            val long = condition.long
            val decimal = condition.decimal
            val str = condition.condition

            val bs = s.beatmaps.orEmpty()

            return when (filter) {
                HOST -> fit(operator, s.creator, str)

                MAPPER -> if (bs.isEmpty()) {
                    fit(operator, s.creator, str)
                } else {
                    bs.any { beatmap ->
                        beatmap.mappers.any { mapper -> fitMapper(operator, mapper, long, str) }
                    }
                }

                GUEST -> if (bs.isEmpty()) {
                    false
                } else {
                    val isHost = (s.creatorID == long) || fit(operator, s.creator, str)

                    !isHost && bs.any { beatmap ->
                        beatmap.mappers.any { mapper -> fitMapper(operator, mapper, long, str) }
                    }
                }

                BID -> bs.map {
                    fit(operator, it.beatmapID, long)
                }.contains(true)
                SID -> fit(operator, s.beatmapsetID, long)
                TITLE -> fitAlias(operator, s.aliases, condition.string)
                        || fit(operator, s.title, str)
                        || fit(operator, s.titleUnicode, str)
                ARTIST -> fit(operator, s.artist, str)
                        || fit(operator, s.artistUnicode, str)
                SOURCE -> fit(operator, s.source, str)
                TAG -> fitTags(operator, s.tags, str)

                ANY -> {
                    fitAlias(operator, s.aliases, condition.string)
                            || fitTags(operator, s.tags, str)
                            || fit(operator, s.title, str)
                            || fit(operator, s.titleUnicode, str)
                            || fit(operator, s.artist, str)
                            || fit(operator, s.artistUnicode, str)
                            || fit(operator, s.source, str)
                }
                GENRE -> fit(operator, s.genreID.toInt(), OsuGenre.getByte(str)?.toInt() ?: return false)
                LANGUAGE -> fit(operator, s.languageID.toInt(), OsuLanguage.getByte(str)?.toInt() ?: return false)
                DIFFICULTY -> bs.map {
                    fit(operator, it.difficultyName, str)
                }.contains(true)
                STAR -> bs.map {
                    fit(operator, it.starRating.toBigDecimal(), decimal)
                }.contains(true)
                MODE -> bs.map {
                    fit(operator, it.modeInt!!, str.toOsuMode().modeValue)
                }.contains(true)
                CATEGORY -> bs.map {
                    fit(operator, it.ranked, DataUtil.getStatusByte(str) ?: return false)
                }.contains(true)
                AR -> bs.map {
                    fit(operator, it.ar?.toBigDecimal(), decimal)
                }.contains(true)
                CS -> bs.map {
                    fit(operator, it.cs?.toBigDecimal(), decimal)
                }.contains(true)
                OD -> bs.map {
                    fit(operator, it.od?.toBigDecimal(), decimal)
                }.contains(true)
                HP -> bs.map {
                    fit(operator, it.hp?.toBigDecimal(), decimal)
                }.contains(true)
                LENGTH -> {
                    val seconds = TimeParser.parseDurationSeconds(str)

                    bs.map {
                        fit(operator, it.totalLength.toLong(), seconds)
                    }.contains(true)
                }
                BPM -> bs.map {
                    fit(operator, it.bpm, decimal)
                }.contains(true)
                CIRCLE -> bs.map {
                    fit(operator, it.circles!!.toLong(), long)
                }.contains(true)
                SLIDER -> bs.map {
                    fit(operator, it.sliders!!.toLong(), long)
                }.contains(true)
                SPINNER -> bs.map {
                    fit(operator, it.spinners!!.toLong(), long)
                }.contains(true)
                TOTAL -> {
                    bs.map {
                        val total = it.totalNotes

                        total != 0 && fit(operator, total, long)
                    }.contains(true)
                }
                CREATED_TIME -> fitTime(operator, s.submittedDate.atZoneSameInstant(ZoneOffset.UTC).toEpochSecond(), str)
                RANKED_TIME -> fitTime(operator, s.rankedDate?.atZoneSameInstant(ZoneOffset.UTC)?.toEpochSecond() ?: return false, str)
                PLAY_COUNT -> fit(operator, s.playCount, long)
                FAVOURITE -> fit(operator, s.favouriteCount, long)
                ALIASES -> fitAlias(operator, s.aliases, condition.string)
            }
        }
    }
}
