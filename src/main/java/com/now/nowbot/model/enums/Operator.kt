package com.now.nowbot.model.enums

enum class Operator(val symbols: List<String>) {
    // 不等于
    NE(listOf("!=", "<>", "≠")),

    // 完全等于
    XQ(listOf("==", "≌")),

    // 大于等于
    GE(listOf(">=", "≥")),

    // 大于
    GT(listOf(">")),

    // 小于等于
    LE(listOf("<=", "≤")),

    // 小于
    LT(listOf("<")),

    // （约）等于
    EQ(listOf("=", "≈"))

    ;

    companion object {
        private val SYMBOL_MAP: List<Pair<String, Operator>> = entries
            .flatMap { op -> op.symbols.map { symbol -> symbol to op } }
            .sortedByDescending { it.first.length }

        private val ALIAS_REPLACEMENTS = mapOf(
            '！' to '!',
            '＝' to '=',
            '＞' to '>',
            '＜' to '<'
        )

        /** 半角字符 → 对应的全角别名，用于把运算符展开成所有可识别的写法 */
        private val ALIAS_VARIANTS: Map<Char, List<Char>> =
            ALIAS_REPLACEMENTS.entries.groupBy({ it.value }, { it.key })

        /**
         * 所有可识别的运算符写法，包含全角 / 别名变体。
         *
         * 例如 `>=` 会展开成 `>=`、`>＝`、`＞=`、`＞＝` 四种写法。
         * 剥离器直接用这份表在原文里匹配，不能先做字符替换：改写文本会让剩余字符的下标失效。
         */
        val allSymbols: List<String> = entries
            .flatMap { it.symbols }
            .flatMap { expandAliases(it) }
            .distinct()
            .sortedByDescending(String::length)

        private fun expandAliases(symbol: String): List<String> {
            var variants = listOf("")

            // 逐字符展开：运算符最长只有两个字符，所有组合最多 4 种
            for (ch in symbol) {
                val forms = (ALIAS_VARIANTS[ch].orEmpty() + ch).distinct()
                variants = variants.flatMap { prefix -> forms.map { prefix + it } }
            }

            return variants
        }

        /**
         * 查表法：找到字符串中匹配到的运算符
         */
        fun getOperator(string: String): Operator {
            // 1. 先进行字符归一化
            val normalizedInput = normalize(string)

            // 2. 匹配归一化后的运算符
            for ((symbol, op) in SYMBOL_MAP) {
                if (normalizedInput.contains(symbol)) {
                    return op
                }
            }
            return EQ
        }

        /**
         * 将输入字符串中的别名/全角字符统一替换为标准字符
         */
        private fun normalize(input: String): String {
            // 快速判断：如果输入不含任何别名字符，直接返回原字符串，零开销
            if (input.none { it in ALIAS_REPLACEMENTS }) {
                return input
            }

            // 存在别名字符时才构建新字符串
            val sb = StringBuilder(input.length)
            for (ch in input) {
                sb.append(ALIAS_REPLACEMENTS[ch] ?: ch)
            }
            return sb.toString()
        }

        fun Operator.getText(): String {
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
    }
}