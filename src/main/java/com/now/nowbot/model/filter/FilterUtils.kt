package com.now.nowbot.model.filter

import com.now.nowbot.util.command.REGEX_OPERATOR_WITH_SPACE

/**
 * 根据 Enum 的 ordinal 获取对应的匹配结果列表
 */
fun <E : Enum<E>> List<List<String>>.getMatches(filter: E): List<String> {
    return this.getOrNull(filter.ordinal).orEmpty().mapNotNull { it.split(REGEX_OPERATOR_WITH_SPACE).lastOrNull() }
}

/**
 * 判断指定 Enum 是否匹配到了至少一条记录
 */
fun <E : Enum<E>> List<List<String>>.anyMatches(filter: E): Boolean {
    return this.getMatches(filter).isNotEmpty()
}

/**
 * 快捷获取指定 Enum 的第一个匹配项（日常最常用）
 */
fun <E : Enum<E>> List<List<String>>.getFirstMatch(filter: E): String? {
    return this.getMatches(filter).firstOrNull()
}

/**
 * 参数的「值」长什么样。
 *
 * [allowsSpace] 决定剥离器用什么方式给值收尾：
 * - 允许空格的值（NAME / ANY）会一直向后吞，直到撞上运算符、冒号、引号，
 *   或者「下一个参数名 + 运算符」。歌曲名这类含空格的值靠这个才能完整保留。
 * - 不允许空格的值只吃一段连续的、符合字符集的文本。
 *
 * [minLength] / [maxLength] 按字符数限制值的长度，剥离器会在摘掉后缀之后再校验，
 * 长度不合法的参数视为没认出来，原样留在剩余文本里。
 */
enum class FilterValueType(
    val allowsSpace: Boolean,
    val minLength: Int = 0,
    val maxLength: Int = Int.MAX_VALUE,
) {
    ANY(true),
    /** 曲名、谱师这类文本值，长度限定 3~15 个字符 */
    NAME(true, minLength = 3, maxLength = 15),
    /** 不含空格的一段任意文本，不限长度：`:m` 的 `m`、`#3-5` 的 `3-5`、`replay=0` 的 `0` */
    WORD(false),
    DECIMAL(false),
    INTEGER(false),
    TIME(false),
    MOD(false);

    /** 值长度是否落在该类型的允许区间内 */
    fun isValidLength(value: CharSequence): Boolean = value.length in minLength..maxLength

    private val timeChars: Set<Char> = setOf(
        ':', '：', '-', '/',
        '年','月','日','天','分','钟','秒','小','时',
    )

    /**
     * 仅对 [allowsSpace] 为 false 的类型生效；空格本身不参与判断，
     * 由剥离器在遇到空白时直接收尾。
     */
    fun isValidChar(c: Char): Boolean = when (this) {
        INTEGER -> c.isDigit()
        DECIMAL -> c.isDigit() || c == '.' || c == ','
        TIME -> c.isLetterOrDigit() || c in timeChars
        MOD -> c.isLetterOrDigit() || c == '+'
        ANY, NAME, WORD -> true
    }
}

interface CommandClassifier {
    val name: String
    val prefix: Regex
    val valueType: FilterValueType
    val suffix: Regex?

    /**
     * 可选的 value 首段校验。
     *
     * 同一个 prefix 可能对应多个分类器（例如 ScoreClassifier 的 `l` 既可能是
     * LANGUAGE 也可能是 LENGTH），提取器会在选中候选前用这个正则检查 value。
     * 为 null 时不限制。
     */
    val valueRegex: Regex? get() = null
}