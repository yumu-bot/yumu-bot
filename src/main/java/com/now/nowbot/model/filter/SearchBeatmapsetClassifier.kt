package com.now.nowbot.model.filter

import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.enums.Operator.Companion.getText
import com.now.nowbot.model.enums.OsuGenre
import com.now.nowbot.model.enums.OsuLanguage
import com.now.nowbot.model.enums.OsuMode.Companion.toOsuMode
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.TimeParser
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language

/**
 * [SearchBeatmapsetFilter] 的名字形式分类器。
 *
 * 条目顺序与 [SearchBeatmapsetFilter] 保持一致（末尾的 RANGE 由
 * [com.now.nowbot.util.command.SymbolHead.RANGE] 处理），因此可以按 ordinal / name 与过滤器条目一一对应。
 */
enum class SearchBeatmapsetClassifier(
    @param:Language("RegExp") override val prefix: Regex,
    override val valueType: FilterValueType,
    @param:Language("RegExp") override val suffix: Regex? = null,
    @param:Language("RegExp") override val valueRegex: Regex? = null,
): CommandClassifier {
    CREATOR("(creator|host|c|h|谱师|作者|谱|主)".toRegex(), FilterValueType.NAME),

    BID("((beatmap\\s*)?id|bid|b|(谱面)?编?号)".toRegex(), FilterValueType.INTEGER),

    SID("((beatmap\\s*)?setid|sid|s|(谱面)?集编号)".toRegex(), FilterValueType.INTEGER),

    TITLE("(title|name|song|t|歌?曲名|歌曲|标题)".toRegex(), FilterValueType.ANY),

    ARTIST("(artist|singer|art|f?a|艺术家|曲师?)".toRegex(), FilterValueType.ANY),

    SOURCE("(source|src|from|f|o|sc|来?源)".toRegex(), FilterValueType.ANY),

    ANY("(any(thing)?|y|任[何意]?(字段|文字)?|[字文])".toRegex(), FilterValueType.ANY),

    DIFFICULTY("(difficult(y|ies)|diff|d|难度名?)".toRegex(), FilterValueType.ANY),

    STAR("(stars?|rating|sr|r|星数?)".toRegex(), FilterValueType.DECIMAL, "$PATTERN_STAR$LEVEL_MAYBE".toRegex()),

    AR("(ar|approach\\s*(rate)?)".toRegex(), FilterValueType.DECIMAL),

    CS("(cs|circle\\s*(size)?|keys?|键)".toRegex(), FilterValueType.DECIMAL),

    OD("(od|overall\\s*(difficulty)?)".toRegex(), FilterValueType.DECIMAL),

    HP("(hp|health\\s*(point)?)".toRegex(), FilterValueType.DECIMAL),

    LENGTH(
        "(length|drain|time|长度?|时[长间]|l)".toRegex(),
        FilterValueType.TIME,
        valueRegex = Regex("^(?:$PATTERN_DURATION_NUMBER|[:：])"),
    ),

    BOOLEAN("(general|bool(ean)?|常规?|总览?|布尔值?|值|e)".toRegex(), FilterValueType.ANY),

    MODE(
        "(mode|模式?|m)".toRegex(),
        FilterValueType.ANY,
        valueRegex = Regex("^$PATTERN_MODE$"),
    ),

    CATEGORY("(type|category|status|类型?|[分种]类|状态?|z)".toRegex(), FilterValueType.ANY),

    GENRE("(genre|g|曲?风|风格|流派?)".toRegex(), FilterValueType.ANY),

    LANGUAGE(
        "(languages?|l|la|lang|语[言种]?|文字?)".toRegex(),
        FilterValueType.ANY,
        valueRegex = Regex("^$PATTERN_LANGUAGE_VALUE"),
    ),

    RANK("(rank(ing|s)?|评[价级]?|k)".toRegex(), FilterValueType.ANY),
    ;

    companion object {
        /**
         * 搜索条件最终不是用来筛选本地谱面集，而是拼成 ppy API 的查询参数，
         * 因此这里的 classify 返回的是 `参数名 -> 参数值`。
         */
        fun classify(
            ctx: StripContext,
            overwrite: Map<String, Any> = mapOf(),
            overwritten: Map<String, Any> = mapOf(),
        ): Map<String, Any> {
            return buildQuery(ctx.syntaxes, overwrite, overwritten)
        }

        fun classify(
            syntaxes: Map<Pair<String, Operator>, List<String>>,
            overwrite: Map<String, Any> = mapOf(),
            overwritten: Map<String, Any> = mapOf(),
        ): Map<String, Any> {
            return buildQuery(syntaxes, overwrite, overwritten)
        }

        fun buildQuery(
            ctx: StripContext,
            overwrite: Map<String, Any> = mapOf(),
            overwritten: Map<String, Any> = mapOf(),
        ): Map<String, Any> {
            return buildQuery(ctx.syntaxes, overwrite, overwritten)
        }

        /**
         * 后面的所有键值对，除了 q= 查询字段，其他键均只保留第一次出现的值
         * @param overwrite 需要覆盖的部分。
         * @param overwritten 被覆盖的部分。
         */
        fun buildQuery(
            syntaxes: Map<Pair<String, Operator>, List<String>>,
            overwrite: Map<String, Any> = mapOf(),
            overwritten: Map<String, Any> = mapOf(),
        ): Map<String, Any> {
            val con = entries.flatMap { filter ->
                syntaxes
                    .filter { (key, _) -> key.first == filter.name }
                    .flatMap { (key, values) ->
                        val operator = key.second

                        values.flatMap { value ->
                            fitBeatmapset(operator, filter, Condition(value))
                        }
                    }
            }

            val pairs: List<Pair<String, Any>> = overwrite.toList() + con + overwritten.toList()

            return pairs.mergePairs()
        }

        private fun fitBeatmapset(
            operator: Operator,
            filter: SearchBeatmapsetClassifier,
            condition: Condition,
        ): List<Pair<String, Any>> {
            val text = condition.condition
            val op = operator.getText()

            return when (filter) {
                BOOLEAN -> SearchBooleans.getSearchBooleans(text).map { it.getQuery() }

                else -> {
                    val con: Pair<String, String>? = when (filter) {
                        BOOLEAN -> null
                        CREATOR -> "q" to "creator=\"\"${text}\"\""
                        BID -> "q" to text
                        SID -> "q" to text
                        TITLE -> "q" to "title=\"\"${text}\"\""
                        ARTIST -> "q" to "artist=\"\"${text}\"\""
                        SOURCE -> "q" to "source=\"\"${text}\"\""
                        DIFFICULTY -> "q" to "difficulty=\"\"${text}\"\""
                        STAR -> "q" to "star${op}${text}"
                        AR -> "q" to "ar${op}${text}"
                        CS -> "q" to "cs${op}${text}"
                        OD -> "q" to "od${op}${text}"
                        HP -> "q" to "hp${op}${text}"
                        LENGTH -> "q" to "length${op}${TimeParser.parseDurationSeconds(text)}"
                        ANY -> "q" to text
                        MODE -> "m" to text.toOsuMode().modeValue.toString()
                        CATEGORY -> {
                            val s = DataUtil.getStatus(text)

                            s?.let { "s" to it }
                        }

                        GENRE -> {
                            val g = OsuGenre.getByte(text)?.toString()

                            g?.let { "g" to it }
                        }

                        LANGUAGE -> {
                            val l = OsuLanguage.getByte(text)?.toString()

                            l?.let { "l" to it }
                        }

                        RANK -> {
                            val rankArray = arrayOf("F", "D", "C", "B", "A", "S", "SH", "X", "XH")

                            val cr = rankArray.indexOf(
                                when (text.uppercase()) {
                                    "SSH" -> "XH"
                                    "SS" -> "X"
                                    else -> text.uppercase()
                                }
                            )

                            rankArray.getOrNull(index = cr)?.let { "r" to it }
                        }
                    }

                    listOfNotNull(con)
                }
            }
        }

        /**
         * 合并所有键值对。
         */
        private fun List<Pair<String, Any>>.mergePairs(): Map<String, Any> {
            return this
                .groupBy { it.first }
                .mapValues { (key, pairs) ->
                    when (key) {
                        "q" -> pairs.joinToString(" ") { it.second.toString() }
                        "c", "e" -> pairs.joinToString(",") { it.second.toString() }
                        else -> pairs.first().second
                    }
                }
        }

        internal enum class SearchBooleans {
            RECOMMENDED, CONVERTS, FOLLOWER, SPOTLIGHTS, FEATURED_ARTIST, EXPLICIT, HAS_VIDEO, HAS_STORYBOARD, PLAYED, UNPLAYED;

            fun getQuery(): Pair<String, String> {
                return when (this) {
                    RECOMMENDED -> "c" to "recommended"
                    CONVERTS -> "c" to "converts"
                    FOLLOWER -> "c" to "follows"
                    SPOTLIGHTS -> "c" to "spotlights"
                    FEATURED_ARTIST -> "c" to "featured_artists"
                    EXPLICIT -> "nsfw" to "false"
                    HAS_VIDEO -> "e" to "video"
                    HAS_STORYBOARD -> "e" to "storyboard"
                    PLAYED -> "played" to "played"
                    UNPLAYED -> "played" to "unplayed"
                }
            }

            companion object {
                fun getSearchBooleans(inputs: String): List<SearchBooleans> {
                    return inputs.split(REGEX_SEPARATOR_NO_SPACE)
                        .filter { it.isNotBlank() }
                        .mapNotNull { getSearchBoolean(it) }
                }

                fun getSearchBoolean(input: String): SearchBooleans? {
                    return when (input.dropWhile { it.isWhitespace() }.trim()) {
                        "recommended", "recommend", "r" -> RECOMMENDED
                        "converts", "convert", "cv", "c" -> CONVERTS
                        "follower", "follow", "followmapper", "f", "m" -> FOLLOWER
                        "spotlights", "spotlight", "spot", "light", "s", "l" -> SPOTLIGHTS
                        "featured", "artist", "feature", "featuredartist", "fa", "a" -> FEATURED_ARTIST
                        "explicit", "nsfw", "e", "x", "h" -> EXPLICIT
                        "hasvideo", "video", "v" -> HAS_VIDEO
                        "hasstoryboard", "storyboard", "sb", "b" -> HAS_STORYBOARD
                        "played", "play", "p" -> PLAYED
                        "unplayed", "unplay", "notplayed", "notplay", "n", "u" -> UNPLAYED

                        else -> null
                    }
                }
            }
        }
    }
}
