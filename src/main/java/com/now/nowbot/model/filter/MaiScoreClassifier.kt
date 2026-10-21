package com.now.nowbot.model.filter

import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language

/**
 * [MaiScoreFilter] 的名字形式分类器。
 *
 * 条目顺序与 [MaiScoreFilter] 保持一致（末尾的 RANGE 由
 * [com.now.nowbot.util.command.SymbolHead.RANGE] 处理），因此可以按 ordinal / name 与过滤器条目一一对应。
 *
 * [DIFFICULTY] 与 [DIFFICULTY_NAME] 的前缀相同，靠 [valueRegex] 区分：
 * `d=13+` 命中 [DIFFICULTY]，`d=Master` 落到 [DIFFICULTY_NAME]。
 */
enum class MaiScoreClassifier(
    @param:Language("RegExp") override val prefix: Regex,
    override val valueType: FilterValueType,
    @param:Language("RegExp") override val suffix: Regex? = null,
    @param:Language("RegExp") override val valueRegex: Regex? = null,
): CommandClassifier {
    CHARTER("(chart(er)?|mapper|谱师?|c)".toRegex(), FilterValueType.ANY),

    ID("((song\\s*)?id|(歌曲)?编?号|i)".toRegex(), FilterValueType.INTEGER),

    DIFFICULTY(
        "(difficulty|diff|难度?|d)".toRegex(),
        FilterValueType.WORD,
        valueRegex = Regex("^$PATTERN_MAI_DIFFICULTY$"),
    ),

    DIFFICULTY_NAME("(difficulty|diff|难度?|d)".toRegex(), FilterValueType.ANY),

    CABINET(
        "(cabinet|框体?|cab|ca|n)".toRegex(),
        FilterValueType.WORD,
        valueRegex = Regex("^$PATTERN_MAI_CABINET$"),
    ),

    VERSION("(version|版本?|v)".toRegex(), FilterValueType.ANY),

    TITLE("(title|name|song|歌?曲|名|歌?曲名|标题|t)".toRegex(), FilterValueType.ANY),

    ALIASES("(alias|aliases|外号|绰号|别名?|l)".toRegex(), FilterValueType.ANY),

    ARTIST("(artist|singer|art|艺术家|歌手?|歌手名|a)".toRegex(), FilterValueType.ANY),

    CATEGORY("(type|category|genre|类型?|[分种]类|t|g)".toRegex(), FilterValueType.ANY),

    BPM("(bpm|曲?速|b|bm)".toRegex(), FilterValueType.DECIMAL),

    ACHIEVEMENT("(accuracy|达成率?|精[确准][率度]?|准确?[率度]|achieve(ment)?|acc|ach|ac)".toRegex(), FilterValueType.DECIMAL, "[%％]?".toRegex()),

    TAP("(tap|点击?|ta|tp)".toRegex(), FilterValueType.DECIMAL),

    HOLD("(hold|长?按|hod|ho|hd)".toRegex(), FilterValueType.DECIMAL),

    SLIDE("(slider?|[滑划]动?|sld|sl|se)".toRegex(), FilterValueType.DECIMAL),

    TOUCH("(touch|触?摸|toh|tch|th|to)".toRegex(), FilterValueType.DECIMAL),

    BREAK("(break|绝?赞|brk|br|bk)".toRegex(), FilterValueType.DECIMAL),

    DX_SCORE("(dx\\s*score|score|(dx)?分数?|dx|ds|o)".toRegex(), FilterValueType.DECIMAL),

    DX_STAR(
        "(dx\\s*star|star|(dx)?星星?|星|dxsr|dr|sr|s)".toRegex(),
        FilterValueType.WORD,
        valueRegex = Regex("^[0-5]$"),
    ),

    RATING("(rating|评?分|r|ra|rt)".toRegex(), FilterValueType.INTEGER),

    RANK("(rank|评价?|rk|k)".toRegex(), FilterValueType.ANY),

    COMBO("(combo|连击?|cb|x)".toRegex(), FilterValueType.ANY),

    SYNC("(sync|同步?|sy|y)".toRegex(), FilterValueType.ANY),
}
