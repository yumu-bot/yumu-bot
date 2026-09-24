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

enum class FilterValueType {
    ANY, DECIMAL, INTEGER, NAME, TIME, MOD;

    private val nameChars = setOf('_', '-', '[', ']', '(', ')')
    private val timeChars: Set<Char> = setOf(
        ':', '：', '-', '/',
        '年','月','日','天','分','钟','秒','小','时',
    )

    fun isValidChar(c: Char): Boolean {
        return c.isWhitespace() || when (this) {
            INTEGER -> c.isDigit()
            DECIMAL -> c.isDigit() || c == '.' || c == ','
            TIME -> c.isLetterOrDigit() || c in timeChars
            MOD -> c.isLetterOrDigit() || c == '+'
            NAME -> c.isLetterOrDigit() || c in nameChars
            ANY -> true
        }
    }
}

interface CommandClassifier {
    val name: String
    val prefix: Regex
    val valueType: FilterValueType
    val suffix: Regex?
}