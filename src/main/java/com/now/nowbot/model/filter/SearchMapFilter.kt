package com.now.nowbot.model.filter

import com.now.nowbot.util.command.LEVEL_MAYBE
import com.now.nowbot.util.command.LEVEL_MORE
import com.now.nowbot.util.command.REG_ANYTHING_BUT_NO_SPACE
import com.now.nowbot.util.command.REG_COLON
import com.now.nowbot.util.command.REG_NAME
import com.now.nowbot.util.command.REG_NUMBER_DECIMAL
import com.now.nowbot.util.command.REG_NUMBER_MORE
import com.now.nowbot.util.command.REG_OPERATOR_WITH_SPACE
import com.now.nowbot.util.command.REG_RANGE
import com.now.nowbot.util.command.REG_STAR
import com.now.nowbot.util.command.REG_TIME
import org.intellij.lang.annotations.Language

// 当然，这个类是用来给 ppy 的 api 发送查询请求的
enum class SearchMapFilter(@param:Language("RegExp") val regex: Regex) {
    CREATOR("(creator|host|c|h|谱师|作者|谱|主)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NAME)".toRegex()),

    GUEST("((gder|guest\\s*diff(er)?)|mapper|guest|g?u|客串?(谱师)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NAME)".toRegex()),

    BID("((beatmap\\s*)?id|bid|b|(谱面)?编?号)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    SID("((beatmap\\s*)?setid|sid|s|(谱面)?集编号)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    TITLE("(title|name|song|t|歌?曲名|歌曲|标题)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    ARTIST("(artist|singer|art|f?a|艺术家|曲师?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    SOURCE("(source|src|from|f|o|sc|来?源)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    TAG("(tags?|g|标签?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    DIFFICULTY("(difficulty|diff|d|难度名?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$LEVEL_MORE)".toRegex()),

    STAR("(star|rating|sr|r|星数?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)$REG_STAR$LEVEL_MAYBE".toRegex()),

    AR("(ar|approach\\s*(rate)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    CS("(cs|circle\\s*(size)?|keys?|键)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    OD("(od|overall\\s*(difficulty)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    HP("(hp|health\\s*(point)?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    LENGTH("(length|drain|time|长度|l)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE($REG_COLON$REG_NUMBER_MORE)?)".toRegex()),

    CIRCLE("((hit)?circles?|hi?t|click|rice|ci|cr|rc|圆圈?|米)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    SLIDER("(slider?s?|sl|long(note)?|lns?|[滑长]?条|长键|面)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    SPINNER("(spin(ner)?s?|rattle|sp|转盘|[转盘])(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_DECIMAL)".toRegex()),

    TOTAL("(total|all|ttl|(hit)?objects?|tt|物件数?|总数?)(?<n>$REG_OPERATOR_WITH_SPACE$REG_NUMBER_MORE)".toRegex()),

    UPDATED_TIME("((updated?)?\\s*time|更(新(时间)?)?|ti|ut)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$REG_TIME)".toRegex()),

    RANKED_TIME("((rank(ed)?)?\\s*time|上(架(时间)?)?|rt)(?<n>$REG_OPERATOR_WITH_SPACE$REG_ANYTHING_BUT_NO_SPACE$REG_TIME)".toRegex()),

    RANGE(REG_RANGE.toRegex());

    fun buildQuery(input: String?): Map<String, Any?> {
        return mapOf()
    }
}