package com.now.nowbot.model.filter

import com.now.nowbot.util.command.LEVEL_MAYBE
import com.now.nowbot.util.command.PATTERN_DURATION_NUMBER
import com.now.nowbot.util.command.PATTERN_LANGUAGE_VALUE
import com.now.nowbot.util.command.PATTERN_STAR
import org.intellij.lang.annotations.Language

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

    CREATED_TIME("(date|at|creat(ed)?(\\s*(at|time))?|earn(ed)?(\\s*(at|time))?|score\\s*(at|time)|(成绩(创建)?|创建)(时间)?|st|ct|ca|ti)".toRegex(), FilterValueType.TIME);

}