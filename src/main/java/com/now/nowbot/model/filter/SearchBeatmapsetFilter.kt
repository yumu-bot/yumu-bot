package com.now.nowbot.model.filter

import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.enums.Operator.Companion.getText
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language
import kotlin.text.split

// 当然，这个类是用来给 ppy 的 api 发送查询请求的
enum class SearchBeatmapsetFilter(@param:Language("RegExp") val regex: Regex) {
    CREATOR("(creator|host|c|h|谱师|作者|谱|主)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NAME)".toRegex()),

    GUEST("((gd(er)?|guest\\s*diff(er)?)|mapper|guest|g?u|客串?(谱师)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NAME)".toRegex()),

    BID("((beatmap\\s*)?id|bid|b|(谱面)?编?号)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    SID("((beatmap\\s*)?setid|sid|s|(谱面)?集编号)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    TITLE("(title|name|song|t|歌?曲名|歌曲|标题)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING$LEVEL_MORE)".toRegex()),

    ARTIST("(artist|singer|art|f?a|艺术家|曲师?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    SOURCE("(source|src|from|f|o|sc|来?源)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    ANY("(any(thing)?|y|任[何意]?(字段|文字)?|[字文])(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    DIFFICULTY("(difficulty|diff|d|难度名?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    STAR("(star|rating|sr|r|星数?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)$REG_STAR$LEVEL_MAYBE".toRegex()),

    AR("(ar|approach\\s*(rate)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    CS("(cs|circle\\s*(size)?|keys?|键)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    OD("(od|overall\\s*(difficulty)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    HP("(hp|health\\s*(point)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    LENGTH("(length|drain|time|长度|l)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE($REG_COLON$REG_NUMBER_MORE)?)".toRegex()),

    GENERAL("(general|bool(ean)?|常规?|总览?|布尔值?|值|e)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    MODE("(mode|模式?|m)(?<n>$REG_OPERATOR_WITH_SPACE$REG_MODE)".toRegex()),

    CATEGORY("(type|category|status|类型?|[分种]类|状态?|v|z)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    GENRE("(genre|g|曲?风|风格|流派?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    LANGUAGE("(languages?|l|曲?风|风格|流派?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    RANK("(rank(ing)?|评[价级]?|k)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_MORE)".toRegex()),

    RANGE(REG_RANGE.toRegex());

    companion object {

        /**
         * 后面的所有键值对，除了 q= 查询字段，其他键均只保留第一次出现的值
         * @param overwrite 需要覆盖的部分。
         * @param overwritten 被覆盖的部分。
         */
        fun buildQuery(conditions: List<List<String>>, overwrite: Map<String, Any> = mapOf(), overwritten: Map<String, Any> = mapOf()): Map<String, Any> {
            val el = SearchBeatmapsetFilter.entries.toList()

            // 最后一个筛选条件无需匹配
            val con = conditions
                .dropLast(1)
                .mapIndexed { index, strings ->
                    if (strings.isNotEmpty()) {
                        filterConditions(el[index], strings)
                    } else {
                        listOf()
                    }
                }.flatten()

            val pairs: List<Pair<String, Any>> = overwrite.toList() + con + overwritten.toList()

            return pairs.mergePairs()
        }

        private fun filterConditions(filter: SearchBeatmapsetFilter, conditions: List<String>): List<Pair<String, Any>> {
            val maps = conditions.map { c ->
                val operator = Operator.getOperator(c)
                val condition = Condition((c.split(REG_OPERATOR_WITH_SPACE.toRegex()).lastOrNull() ?: "").trim())

                fitBeatmapset(operator, filter, condition)
            }.flatten()

            return maps
        }

        private fun fitBeatmapset(operator: Operator, filter: SearchBeatmapsetFilter, condition: Condition): List<Pair<String, Any>> {
            val text = condition.condition
            val op = operator.getText()

            return when (filter) {
                GENERAL -> General.getGenerals(text).map { it.getQuery() }

                else -> {
                    val con: Pair<String, String>? = when(filter) {
                        CREATOR -> "q" to "creator=\"${text}\""
                        GUEST -> "q" to "creator=\"${text}\""
                        BID -> "q" to text
                        SID -> "q" to text
                        TITLE -> "q" to "title=\"${text}\""
                        ARTIST -> "q" to "artist=\"${text}\""
                        SOURCE -> "q" to "source=\"${text}\""
                        DIFFICULTY -> "q" to "difficulty=\"${text}\""
                        STAR -> "q" to "star${op}\"${text}\""
                        AR -> "q" to "ar${op}\"${text}\""
                        CS -> "q" to "cs${op}\"${text}\""
                        OD -> "q" to "od${op}\"${text}\""
                        HP -> "q" to "hp${op}\"${text}\""
                        LENGTH -> "q" to "length${op}\"${text}\""
                        ANY -> "q" to text
                        MODE -> "m" to OsuMode.getMode(text).modeValue.toString()
                        CATEGORY -> {
                            val s = DataUtil.getStatus(text)

                            s?.let { "s" to it }
                        }
                        GENRE -> {
                            val g = DataUtil.getGenre(text)?.toString()

                            g?.let { "g" to it }
                        }
                        LANGUAGE -> {
                            val l = DataUtil.getLanguage(text)?.toString()

                            l?.let { "l" to it }
                        }
                        RANK -> {
                            val rankArray = arrayOf("F", "D", "C", "B", "A", "S", "SH", "X", "XH")

                            val cr = rankArray.indexOf(
                                when(text.uppercase()) {
                                    "SSH" -> "XH"
                                    "SS" -> "X"
                                    else -> text.uppercase()
                                }
                            )

                            rankArray.getOrNull(index = cr)?.let { "r" to it }
                        }

                        else -> null
                    }

                    listOfNotNull(con)
                }
            }
        }

        /**
         * 合并所有键值对。
         * ai 生成。
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

        internal enum class General {
            RECOMMENDED, CONVERTS, FOLLOWER, SPOTLIGHTS, FEATURED_ARTIST, EXPLICIT, HAS_VIDEO, HAS_STORYBOARD, PLAYED, UNPLAYED;

            fun getQuery(): Pair<String, Any> {
                return when(this) {
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
                fun getGenerals(inputs: String): List<General> {
                    return inputs.split(REG_SEPERATOR_NO_SPACE.toRegex())
                        .dropWhile { it.isEmpty() }
                        .mapNotNull { getGeneral(it) }
                }

                fun getGeneral(input: String): General? {
                    return when(input.dropWhile { it.isWhitespace() }.trim()) {
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