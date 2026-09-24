package com.now.nowbot.model.filter

import com.now.nowbot.util.command.LEVEL_MAYBE
import com.now.nowbot.util.command.PATTERN_STAR
import org.intellij.lang.annotations.Language

enum class ScoreClassifier(
    @param:Language("RegExp") override val prefix: Regex,
    override val valueType: FilterValueType,
    @param:Language("RegExp") override val suffix: Regex? = null,
): CommandClassifier {
    CREATOR("(creator|host|c|谱师|作者|谱|主)".toRegex(), FilterValueType.NAME),

    GUEST("((gd(er)?|guest\\s*diff(er)?)|mapper|guest|g?u|客串?(谱师)?)".toRegex(), FilterValueType.NAME),

    BID("((beatmap\\s*)?id|bid|b|(谱面)?编?号)".toRegex(), FilterValueType.INTEGER),

    SID("((beatmap\\s*)?setid|sid|s|(谱面)?集编号)".toRegex(), FilterValueType.INTEGER),

    TITLE("(title|name|song|t|歌?曲名|歌曲|标题)".toRegex(), FilterValueType.NAME),

    ARTIST("(artist|singer|art|f?a|艺术家|曲师?)".toRegex(), FilterValueType.NAME),

    SOURCE("(source|src|from|f|o|sc|se|来?源)".toRegex(), FilterValueType.NAME),

    TAG("(tags?|ta|tg|w|标签?)".toRegex(), FilterValueType.NAME),

    ANY("(any(thing)?|y|任[何意]?(字段|文字)?|[字文])".toRegex(), FilterValueType.NAME),

    GENRE("(genre|g|曲?风|风格|流派?)".toRegex(), FilterValueType.NAME),

    LANGUAGE("(languages?|l|la|曲?风|风格|流派?)".toRegex(), FilterValueType.NAME),

    DIFFICULTY("(difficult(y|ies)|diff|d|难度名?)".toRegex(), FilterValueType.NAME),

    STAR("(stars?|rating|sr|r|星数?)".toRegex(), FilterValueType.DECIMAL, "$PATTERN_STAR$LEVEL_MAYBE".toRegex()),

    SCORE("(scores?|sc|ss|e|分数?)".toRegex(), FilterValueType.DECIMAL),

    REPLAY("(replay|re?p|journal|j|回放|录像|记录)".toRegex(), FilterValueType.NAME),

    AR("(ar|approach\\s*(rate)?)".toRegex(), FilterValueType.DECIMAL),

    CS("(cs|circle\\s*(size)?|keys?|键)".toRegex(), FilterValueType.DECIMAL),

    OD("(od|overall\\s*(difficulty)?)".toRegex(), FilterValueType.DECIMAL),

    HP("(hp|health\\s*(point)?)".toRegex(), FilterValueType.DECIMAL),

    PERFORMANCE("(performance|表现分?|pp|p)".toRegex(), FilterValueType.DECIMAL),

    RANK("(rank(ing|s)?|评[价级]?|k)".toRegex(), FilterValueType.NAME),

    LENGTH("(length|drain|long|duration|长度|时?长|lh|h)".toRegex(), FilterValueType.TIME),

    BPM("(bpm|曲速|速度|bm)".toRegex(), FilterValueType.DECIMAL),

    ACCURACY("(accuracy|精[确准][率度]?|准确?[率度]|acc?)".toRegex(), FilterValueType.DECIMAL, "[%％]?".toRegex()),

    COMBO("(combo|连击|cb?)".toRegex(), FilterValueType.DECIMAL, "[xX]?".toRegex()),

    PERFECT("(perfect|320|305|彩|完美|pf)".toRegex(), FilterValueType.DECIMAL),

    GREAT("(great|300|大果?|fruits?|fr|良|黄|gr|很好)".toRegex(), FilterValueType.DECIMAL),

    MISSED_FRUIT("(miss(ed)?\\s*fruits?|漏大果?|mf)".toRegex(), FilterValueType.DECIMAL),

    MISSED_DROP("(miss(ed)?\\s*drop|漏中果?|mp)".toRegex(), FilterValueType.DECIMAL),

    MISSED_DROPLET("(miss(ed)?\\s*droplet|漏小?果?|md)".toRegex(), FilterValueType.DECIMAL),

    GOOD("(good|200|绿|gd|良好)".toRegex(), FilterValueType.DECIMAL),

    OK("(ok|150|100|中果?|large\\s*drop(let)?|ld|(?<!不)可|蓝|ba?d|可以)".toRegex(), FilterValueType.DECIMAL),

    MEH("(me?h|小果?|drop(let)?|sd|p(oo)?r|灰|50|一般)".toRegex(), FilterValueType.DECIMAL),

    MISS("(m(is)?s|0|x|不可|红|失误|漏击)".toRegex(), FilterValueType.DECIMAL),

    MOD("((m(od)?s?)|模组?)".toRegex(), FilterValueType.MOD),

    RATE("(rate|彩[率比]|黄彩比?|q|pm)".toRegex(), FilterValueType.DECIMAL),

    CIRCLE("((hit)?circles?|hi?t|click|rice|ci|cr|rc|圆圈?|米)".toRegex(), FilterValueType.DECIMAL),

    SLIDER("(slider?s?|sl|long(note)?|lns?|[滑长]?条|长键|面)".toRegex(), FilterValueType.DECIMAL),

    SPINNER("(spin(ner)?s?|rattle|sp|转盘|[转盘])".toRegex(), FilterValueType.DECIMAL),

    TOTAL("(notes?|total|all|ttl|(hit)?objects?|tt|n|(物件|音符)数?|总数?)".toRegex(), FilterValueType.INTEGER),

    CONVERT("(convert|cv|转谱?)".toRegex(), FilterValueType.NAME),

    CLIENT("(client|z|v|version|版本?)".toRegex(), FilterValueType.NAME),

    CREATED_TIME("(date|(created|score)?\\s*(at|time)|creat(ed)?\\s*(at|time)?|(成绩(创建)?|创建)?(时间)?|st|ct|ca|ti)".toRegex(), FilterValueType.TIME);

}