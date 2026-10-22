package com.now.nowbot.model.filter

import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.enums.OsuGenre
import com.now.nowbot.model.enums.OsuLanguage
import com.now.nowbot.model.enums.OsuMode.Companion.toOsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.TimeParser
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language

/**
 * [MostPlayedBeatmapFilter] 的名字形式分类器。
 *
 * 条目顺序与 [MostPlayedBeatmapFilter] 保持一致（末尾的 RANGE 由
 * [com.now.nowbot.util.command.SymbolHead.RANGE] 处理），因此可以按 ordinal / name 与过滤器条目一一对应。
 */
enum class MostPlayedBeatmapClassifier(
    @param:Language("RegExp") override val prefix: Regex,
    override val valueType: FilterValueType,
    @param:Language("RegExp") override val suffix: Regex? = null,
    @param:Language("RegExp") override val valueRegex: Regex? = null,
): CommandClassifier {
    CREATOR("(creator|host|c|h|谱师|作者|谱|主)".toRegex(), FilterValueType.NAME),

    GUEST("((gd(er)?|guest\\s*diff(er)?)|mapper|guest|g?u|客串?(谱师)?)".toRegex(), FilterValueType.NAME),

    BID("((beatmap\\s*)?id|bid|b|(谱面)?编?号)".toRegex(), FilterValueType.INTEGER),

    SID("((beatmap\\s*)?setid|sid|s|(谱面)?集编号)".toRegex(), FilterValueType.INTEGER),

    TITLE("(title|name|song|t|歌?曲名|歌曲|标题)".toRegex(), FilterValueType.ANY),

    ARTIST("(artist|singer|art|f?a|艺术家|曲师?)".toRegex(), FilterValueType.ANY),

    SOURCE("(source|src|from|f|o|sc|来?源)".toRegex(), FilterValueType.ANY),

    ANY("(any(thing)?|y|任[何意]?(字段|文字)?|[字文])".toRegex(), FilterValueType.ANY),

    DIFFICULTY("(difficult(y|ies)|diff|d|难度名?)".toRegex(), FilterValueType.ANY),

    STAR("(stars?|rating|sr|r|星数?)".toRegex(), FilterValueType.DECIMAL, "$PATTERN_STAR$LEVEL_MAYBE".toRegex()),

    LENGTH(
        "(length|drain|time|长度?|时[长间]|l)".toRegex(),
        FilterValueType.TIME,
        valueRegex = Regex("^(?:$PATTERN_DURATION_NUMBER|[:：])"),
    ),

    MODE(
        "(mode|模式?|m)".toRegex(),
        FilterValueType.ANY,
        valueRegex = Regex("^$PATTERN_MODE$"),
    ),

    CATEGORY("(type|category|status|类型?|[分种]类|状态?|z|$PATTERN_STAR)".toRegex(), FilterValueType.ANY),

    GENRE("(genre|g|曲?风|风格|流派?)".toRegex(), FilterValueType.ANY),

    LANGUAGE(
        "(languages?|l|la|lang|语[言种]?|文字?)".toRegex(),
        FilterValueType.ANY,
        valueRegex = Regex("^$PATTERN_LANGUAGE_VALUE"),
    ),

    COUNT("(counts?|n|游玩(次数)?|游|玩|次数)".toRegex(), FilterValueType.INTEGER),
    ;

    companion object {
        fun Collection<Beatmap>.classify(ctx: StripContext): List<Beatmap> {
            return classify(ctx.syntaxes)
        }

        fun Collection<Beatmap>.classify(
            syntaxes: Map<Pair<String, Operator>, List<String>>
        ): List<Beatmap> {
            return classifyBy(syntaxes, entries) { beatmap, operator, filter, condition ->
                fitMostPlayedBeatmap(beatmap, operator, filter, condition)
            }
        }

        fun <K> Map<K, Beatmap>.classify(ctx: StripContext): Map<K, Beatmap> {
            return classify(ctx.syntaxes)
        }

        fun <K> Map<K, Beatmap>.classify(
            syntaxes: Map<Pair<String, Operator>, List<String>>
        ): Map<K, Beatmap> {
            return classifyValuesBy(syntaxes, MostPlayedBeatmapClassifier.entries) { beatmap, operator, filter, condition ->
                fitMostPlayedBeatmap(beatmap, operator, filter, condition)
            }
        }

        private fun fitMostPlayedBeatmap(
            b: Beatmap,
            operator: Operator,
            filter: MostPlayedBeatmapClassifier,
            condition: Condition,
        ): Boolean {
            val long = condition.long
            val decimal = condition.decimal
            val str = condition.condition

            val s = b.beatmapset!!

            return when (filter) {
                CREATOR -> fit(operator, s.creator, str)
                GUEST -> {
                    if (long <= 0) throw RuntimeException("抱歉，本功能暂时只能输入第一位客串谱师的编号 (UID)！")

                    fit(operator, b.mapperID, long)
                }

                BID -> fit(operator, b.beatmapID, long)
                SID -> fit(operator, s.beatmapsetID, long)
                TITLE -> (fit(operator, s.title, str) || fit(operator, s.titleUnicode, str))
                ARTIST -> (fit(operator, s.artist, str) || fit(operator, s.artistUnicode, str))
                SOURCE -> fit(operator, s.source, str)
                ANY -> fit(operator, s.title, str)
                        || fit(operator, s.titleUnicode, str)
                        || fit(operator, s.artist, str)
                        || fit(operator, s.artistUnicode, str)
                        || fit(operator, s.source, str)
                DIFFICULTY -> fit(operator, b.difficultyName, str)
                STAR -> fit(operator, b.starRating.toBigDecimal(), decimal)
                LENGTH -> {
                    val seconds = TimeParser.parseDurationSeconds(str)

                    fit(operator, b.totalLength.toLong(), seconds)
                }
                MODE -> fit(operator, b.mode.modeValue.toInt(), str.toOsuMode().modeValue.toInt())
                CATEGORY -> fit(operator, DataUtil.getStatus(b.status), DataUtil.getStatus(str))
                GENRE -> fit(operator, s.genreID.toInt(), OsuGenre.getByte(str)?.toInt())
                LANGUAGE -> fit(operator, s.languageID.toInt(), OsuLanguage.getByte(str)?.toInt())
                COUNT -> fit(operator, b.currentPlayCount?.toLong(), long)
            }
        }
    }
}
