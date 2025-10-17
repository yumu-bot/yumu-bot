package com.now.nowbot.model.filter

import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.enums.Operator.*
import com.now.nowbot.util.command.LEVEL_MAYBE
import com.now.nowbot.util.command.LEVEL_MORE
import com.now.nowbot.util.command.REG_ANYTHING_BUT_NO_SPACE
import com.now.nowbot.util.command.REG_COLON
import com.now.nowbot.util.command.REG_NAME
import com.now.nowbot.util.command.REG_NUMBER_DECIMAL
import com.now.nowbot.util.command.REG_NUMBER_MORE
import com.now.nowbot.util.command.REG_OPERATOR_WITH_SPACE
import com.now.nowbot.util.command.REG_RANGE
import com.now.nowbot.util.command.REG_SEPERATOR_NO_SPACE
import com.now.nowbot.util.command.REG_STAR
import org.intellij.lang.annotations.Language
import kotlin.text.split

// 当然，这个类是用来给 ppy 的 api 发送查询请求的
enum class SearchMapFilter(@param:Language("RegExp") val regex: Regex) {
    CREATOR("(creator|host|c|h|谱师|作者|谱|主)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NAME)".toRegex()),

    GUEST("((gder|guest\\s*diff(er)?)|mapper|guest|g?u|客串?(谱师)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NAME)".toRegex()),

    BID("((beatmap\\s*)?id|bid|b|(谱面)?编?号)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    SID("((beatmap\\s*)?setid|sid|s|(谱面)?集编号)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    TITLE("(title|name|song|t|歌?曲名|歌曲|标题)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    ARTIST("(artist|singer|art|f?a|艺术家|曲师?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    SOURCE("(source|src|from|f|o|sc|来?源)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    // TAG("(tags?|g|标签?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    DIFFICULTY("(difficulty|diff|d|难度名?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    STAR("(star|rating|sr|r|星数?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)$REG_STAR$LEVEL_MAYBE".toRegex()),

    AR("(ar|approach\\s*(rate)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    CS("(cs|circle\\s*(size)?|keys?|键)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    OD("(od|overall\\s*(difficulty)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    HP("(hp|health\\s*(point)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    LENGTH("(length|drain|time|长度|l)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE($REG_COLON$REG_NUMBER_MORE)?)".toRegex()),

    GENERAL("(general|常规?|总览?|布尔值?|值|g)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),



    /*

    CIRCLE("((hit)?circles?|hi?t|click|rice|ci|cr|rc|圆圈?|米)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    SLIDER("(slider?s?|sl|long(note)?|lns?|[滑长]?条|长键|面)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    SPINNER("(spin(ner)?s?|rattle|sp|转盘|[转盘])(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    TOTAL("(total|all|ttl|(hit)?objects?|tt|物件数?|总数?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    UPDATED_TIME("((updated?)?\\s*time|更(新(时间)?)?|ti|ut)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$REG_TIME)".toRegex()),

    RANKED_TIME("((rank(ed)?)?\\s*time|上(架(时间)?)?|rt)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$REG_TIME)".toRegex()),

     */

    RANGE(REG_RANGE.toRegex());

    /**
     * @param overwrite 需要覆盖的部分。后面的所有键值对，除了 q= 查询字段，其他键均只保留第一次出现的值
     */
    fun buildQuery(conditions: List<List<String>>, overwrite: Map<String, Any> = mapOf()): Map<String, Any> {
        val el = SearchMapFilter.entries.toList()

        // 最后一个筛选条件无需匹配
        val pairs: List<Pair<String, Any>> = overwrite.toList() + (conditions
            .dropLast(1)
            .mapIndexed { index, strings ->
                if (strings.isNotEmpty()) {
                    filterConditions(el[index], strings)
                } else {
                    listOf()
                }
            }.flatten())

        return pairs.mergePairs()
    }

    private fun filterConditions(filter: SearchMapFilter, conditions: List<String>): List<Pair<String, Any>> {
        val maps = conditions.map { c ->
            val operator = Operator.getOperator(c)
            val condition = (c.split(REG_OPERATOR_WITH_SPACE.toRegex()).lastOrNull() ?: "").trim()

            fitMap(operator, filter, condition)
        }.flatten()

        return maps
    }

    private fun fitMap(operator: Operator, filter: SearchMapFilter, condition: String): List<Pair<String, Any>> {
        val text = condition.trim()
        val op = operator.getText()

        return when (filter) {
            CREATOR -> listOf("q" to "creator=${text}")
            GUEST -> listOf("q" to "creator=${text}")
            BID -> listOf("q" to text)
            SID -> listOf("q" to text)
            TITLE -> listOf("q" to "title=\"${text}\"")
            ARTIST -> listOf("q" to "artist=\"${text}\"")
            SOURCE -> listOf("q" to "source=\"${text}\"")
            DIFFICULTY -> listOf("q" to "difficulty=\"${text}\"")
            STAR -> listOf("q" to "star${op}\"${text}\"")
            AR -> listOf("q" to "ar${op}\"${text}\"")
            CS -> listOf("q" to "cs${op}\"${text}\"")
            OD -> listOf("q" to "od${op}\"${text}\"")
            HP -> listOf("q" to "hp${op}\"${text}\"")
            LENGTH -> listOf("q" to "length${op}\"${text}\"")
            GENERAL -> General.getGenerals(text).map { it.getQuery() }
            RANGE -> listOf()
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

    private fun Operator.getText(): String {
        return when (this) {
            XQ,
            EQ -> "="
            NE -> "!="
            GE -> ">="
            GT -> ">"
            LE -> "<="
            LT -> "<"
        }
    }

    companion object {
        internal enum class General {
            RECOMMENDED, CONVERTS, FOLLOWER, SPOTLIGHTS, FEATURED_ARTIST, EXPLICIT, HAS_VIDEO, HAS_STORYBOARD, PLAYED, UNPLAYED;

            fun getQuery(): Pair<String, Any> {
                return when(this) {
                    RECOMMENDED -> "c" to "recommended"
                    CONVERTS -> "c" to "converts"
                    FOLLOWER -> "c" to "converts"
                    SPOTLIGHTS -> "c" to "spotlights"
                    FEATURED_ARTIST -> "c" to "featured_artists"
                    EXPLICIT -> "nsfw" to "true"
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
                        "follower", "follow", "followmapper", "f" -> FOLLOWER
                        "spotlights", "spotlight", "spot", "light", "s" -> SPOTLIGHTS
                        "featured", "artist", "featuredartist", "fa", "a" -> FEATURED_ARTIST
                        "explicit", "nsfw", "e", "x", "h" -> EXPLICIT
                        "hasvideo", "video", "v" -> HAS_VIDEO
                        "hasstoryboard", "storyboard", "sb", "b" -> HAS_STORYBOARD
                        "played", "play", "p" -> PLAYED
                        "unplayed", "unplay", "notplayed", "notplay", "n" -> UNPLAYED

                        else -> null
                    }
                }
            }
        }


    }

}