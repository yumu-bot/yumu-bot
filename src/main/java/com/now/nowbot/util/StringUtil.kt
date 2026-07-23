package com.now.nowbot.util

import com.now.nowbot.model.enums.GreekChar.Companion.appendRomanizedGreekTo
import com.now.nowbot.model.enums.JaChar.Companion.appendRomanizedJapaneseTo
import com.now.nowbot.util.command.REGEX_OPERATOR
import com.now.nowbot.util.command.REGEX_RANGE
import com.now.nowbot.util.command.REGEX_SEPARATOR

object StringUtil {

    /**
     * 缩短字符 220924
     *
     * @param maxWidth 最大宽度
     * @return 返回已缩短的字符
     */
    fun String.shorten(maxWidth: Int = 100): String {
        val str = this

        if (str.isEmpty() || maxWidth <= 0) return ""

        // 先计算完整字符串的宽度，如果本身就不超宽，直接返回
        val totalWidth = str.getWidth()
        if (totalWidth <= maxWidth) return str

        // 二分查找合适的截断位置
        var left = 0
        var right = str.length

        while (left < right) {
            val mid = (left + right + 1) / 2  // 偏向右侧，尽量保留更多字符
            val subStr = str.substring(0, mid)
            val width = subStr.getWidth() + 3  // +3 是 "..." 的宽度（假设每个点占1个单位）

            if (width <= maxWidth) {
                left = mid  // 可以保留更多字符
            } else {
                right = mid - 1  // 需要减少字符
            }
        }

        // left 是最大可保留字符数
        return if (left == str.length) {
            str
        } else {
            str.substring(0, left) + "..."
        }
    }

    /**
     * 计算字符串的显示宽度
     * 假设：中文字符占2个单位，英文字符占1个单位
     * 实际项目中可能需要使用 Paint.measureText() 等更精确的方法
     */
    private fun String.getWidth(): Int {
        var width = 0
        for (ch in this) {
            width += if (ch.code in 0x4E00..0x9FFF || ch.code in 0x3000..0x303F) {
                2  // 中文等宽字符
            } else {
                1  // 英文、数字等
            }
        }
        return width
    }

    /**
     * 升级版匹配器（重构版）
     *
     * @param assignmentRegex 用于定位匹配的正则，一般就是运算符号（A=B的`=`），如果有其他定位方式，也需要写在这里
     * @param endRegex 用于匹配部分特殊尾巴的正则，
     * 举例：!mf v=spl 13，如果 endPattern 设为 null 而不是 PATTERN_RANGE，此时 13 会被拼接到 spl 内。
     */
    fun String?.asConditions(
        regexes: List<Regex>,
        assignmentRegex: Regex = REGEX_OPERATOR,
        endRegex: Regex? = REGEX_RANGE,
        separatorRegex: Regex = REGEX_SEPARATOR
    ): List<List<String>> {
        val input = this

        if (input.isNullOrBlank()) return emptyList()

        val trimmedInput = input.trim()

        val sanitizeAssignmentRegex = "\\s*(${assignmentRegex.pattern})\\s*".toRegex()
        val splitRegex = "\\s+(?=\\w+${assignmentRegex.pattern})".toRegex()

        // 2. 格式化 input 并切分成 Key-Value 单元
        val words = trimmedInput
            .replace(sanitizeAssignmentRegex, "$1") // 保留原始分隔符，去除两侧多余空格
            .split(splitRegex)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val lastWord = trimmedInput.split(separatorRegex).lastOrNull().orEmpty()

        val combinedStrs = mutableListOf<String>()

        if (endRegex != null && lastWord.matches(endRegex)) {
            val lastWordInList = words.lastOrNull().orEmpty()

            if (words.size >= 2) {
                combinedStrs.addAll(words.dropLast(1))
            }

            if (lastWordInList.isNotEmpty() && !lastWordInList.matches(endRegex)) {
                val trimTailRegex = "(.*$separatorRegex\\S*)\\s*${endRegex.pattern}".toRegex()
                val cleaned = lastWordInList.replace(trimTailRegex) { it.groupValues[1].trim() }
                combinedStrs.add(cleaned)
            }

            combinedStrs.add(lastWord)
        } else {
            combinedStrs.addAll(words)
        }

        val resultSet = List(regexes.size) { mutableSetOf<String>() }

        for (combined in combinedStrs) {
            regexes.forEachIndexed { index, regex ->
                if (regex.matches(combined)) {
                    resultSet[index].add(combined) // Set.add() 会自动忽略已存在的元素
                }
            }
        }

        return resultSet.map { it.toList() }
    }

    private val ENTITIES = mapOf(
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&apos;" to "'",
        "&nbsp;" to " "
    )

    // 单次遍历或全局正则，支持十进制 (&#123;) 和十六进制 (&#x1A;)
    private val NUMERIC_ENTITY_REGEX = Regex("&#(x?[0-9a-fA-F]+);")

    fun String.unescapeHTML(): String {
        // 1. 先替换常用命名实体
        var result = this
        ENTITIES.forEach { (entity, replacement) ->
            result = result.replace(entity, replacement)
        }

        // 2. 动态匹配所有数值实体 (&#39; / &#x27; 等)
        return NUMERIC_ENTITY_REGEX.replace(result) { matchResult ->
            val codeStr = matchResult.groupValues[1]
            try {
                val codePoint = if (codeStr.startsWith("x", ignoreCase = true)) {
                    codeStr.substring(1).toInt(16)
                } else {
                    codeStr.toInt(10)
                }
                String(Character.toChars(codePoint))
            } catch (_: Exception) {
                matchResult.value // 解析失败时保留原样
            }
        }
    }

    fun String.asHalfWidth(): String {
        val sb = StringBuilder(this.length)

        for (c in this) {
            if (c.code in 0xFF01..0xFF5E) {
                sb.append((c.code - 0xFEE0).toChar())
            } else {
                sb.append(c)
            }
        }

        return sb.toString()
    }

    /**
     * 就是之前的 getStringSimilarity 方法
     *
     * 获取两个字符串的相似度。
     * 新版获取方法参考了 string-similarity-js
     *
     * @param stringLength 需要分割的字符串宽度。默认为 2
     * @param caseSensitive 大小写敏感。默认不敏感
     * @param standardised 是否标准化字符串。默认标准化。
     * @return 0-1 之间的相似度
     */
    fun String?.compareSimilarity(
        to: String?,
        stringLength: Int = 2,
        caseSensitive: Boolean = false,
        standardised: Boolean = true,
    ): Double {
        val compare = this

        if (compare.isNullOrEmpty() || to.isNullOrEmpty()) return 0.0

        val cs = compare
            .let { if (standardised) it.standardised() else it }
            .let { if (caseSensitive) it else it.lowercase() }

        val ts = to
            .let { if (standardised) it.standardised() else it }
            .let { if (caseSensitive) it else it.lowercase() }

        if (cs.length < stringLength || ts.length < stringLength) {
            return if (stringLength > 1) {
                // 增强短字符串下的辨识性，此时只看包含单字符的比例
                compare.compareSimilarity(to, 1, caseSensitive, standardised)
            } else {
                0.0
            }
        }

        val map = mutableMapOf<String, Int>()

        for (i in 0 ..< cs.length - (stringLength - 1)) {
            val cb = cs.substring(i, i + stringLength)

            val v = if (map.contains(cb)) {
                map[cb]?.plus(1)
            } else {
                1
            }

            map[cb] = v ?: 0
        }

        var match = 0

        for (j in 0 ..< ts.length - (stringLength - 1)) {
            val tb = ts.substring(j, j + stringLength)

            val count = map[tb] ?: 0

            if (count > 0) {
                map[tb] = count - 1
                match ++
            }
        }

        return (match * 2.0) / (cs.length + ts.length - ((stringLength - 1.0) * 2.0))
    }

    /**
     * 就是之前的 getStandardisedString 方法
     */
    fun String?.standardised(): String {
        if (this.isNullOrEmpty()) return ""

        val sb = StringBuilder(this.length * 2)
        this.appendRomanizedJapaneseTo(sb)

        val sb2 = StringBuilder(sb.length * 2)
        sb.appendRomanizedGreekTo(sb2)

        return StandardizedCharSequence.of(sb2).toString()
    }

    class StandardizedCharSequence private constructor(
        private val source: CharSequence,
        private val indexMap: IntArray
    ) : CharSequence {

        override val length: Int get() = indexMap.size

        override fun get(index: Int): Char {
            val originalIndex = indexMap[index]
            val originalChar = source[originalIndex]

            // 1. 统一转小写
            val lowerChar = originalChar.lowercaseChar()

            // 2. 尝试全角转半角 (Unicode 范围 0xFF01..0xFF5E 刚好对应半角 ASCII)
            val charCode = lowerChar.code
            val halfWidthChar = if (charCode in 0xFF01..0xFF5E) {
                (charCode - 0xFEE0).toChar()
            } else {
                lowerChar
            }

            // 3. 特殊字符映射表查表转换
            val finalCode = halfWidthChar.code
            return if (finalCode < 65536) CHAR_LOOKUP_TABLE[finalCode] else halfWidthChar
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            val subMap = indexMap.copyOfRange(startIndex, endIndex)
            return StandardizedCharSequence(source, subMap)
        }

        override fun toString(): String {
            val len = length
            val sb = StringBuilder(len)
            for (i in 0 until len) {
                sb.append(get(i))
            }
            return sb.toString()
        }

        companion object {
            /**
             * O(1) 索引字符查找表 (全量 65536 字符映射)
             * 所有非基本 ASCII 的字符均已编码为标准的 \uXXXX 格式
             */
            private val CHAR_LOOKUP_TABLE = CharArray(65536) { it.toChar() }.apply {
                this['\uFF0D'.code] = '-' // －
                this['\u2014'.code] = '-' // —
                this['\u007E'.code] = '-' // ~
                this['\uFF5E'.code] = '-' // ～
                this['\u301C'.code] = '-' // 〜

                // PATTERN_COLON -> ':'
                this['\uFF1A'.code] = ':' // ：
                this['\uFF1B'.code] = ';'

                // PATTERN_PLUS -> '+'
                this['\uFF0B'.code] = '+' // ＋

                // PATTERN_HASH -> '#'
                this['\uFF03'.code] = '#' // ＃

                // PATTERN_STAR -> '*'
                this['\u2042'.code] = '*' // ⁂
                this['\u2605'.code] = '*' // ★
                this['\u2606'.code] = '*' // ☆
                this['\u22C6'.code] = '*' // ⋆
                this['\u269D'.code] = '*'
                this['\u2721'.code] = '*'
                this['\u2726'.code] = '*'
                this['\u2727'.code] = '*'
                this['\u2729'.code] = '*'
                this['\u272A'.code] = '*' // ✪
                this['\u272B'.code] = '*'
                this['\u272C'.code] = '*'
                this['\u272D'.code] = '*'
                this['\u272E'.code] = '*'
                this['\u272F'.code] = '*'
                this['\u2730'.code] = '*'
                this['\u2734'.code] = '*'
                this['\u2735'.code] = '*'
                this['\u2736'.code] = '*'
                this['\u2737'.code] = '*'
                this['\u2738'.code] = '*'
                this['\u2739'.code] = '*'
                this['\u2742'.code] = '*'
                this['\u2BE8'.code] = '*'
                this['\u2BE9'.code] = '*'
                this['\u2BEA'.code] = '*'
                this['\u2BEB'.code] = '*'
                this['\uFF0A'.code] = '*' // ＊


                // PATTERN_EXCLAMATION -> '!'
                this['\u00A1'.code] = '!' // ¡
                this['\u203C'.code] = '!' // !!
                this['\uFE15'.code] = '!' // ︕
                this['\uFE57'.code] = '!' // ﹗
                this['\uFF01'.code] = '!' // ！

                // PATTERN_QUESTION -> '?'
                this['\u00BF'.code] = '?' // ¿
                this['\u061F'.code] = '?' // ؟
                this['\uFF1F'.code] = '?' // ？
                this['\u2047'.code] = '?'
                this['\u2048'.code] = '?'
                this['\u2049'.code] = '?'
                this['\u2E2E'.code] = '?' // ⸮
                this['\uFE16'.code] = '?'
                this['\uFE56'.code] = '?'
                this['\uFF1F'.code] = '?'

                // PATTERN_QUOTATION -> '"'
                this['\u201C'.code] = '"' // “
                this['\u201D'.code] = '"' // ”
                this['\u201E'.code] = '"'
                this['\u201F'.code] = '"' // ‟
                this['\u00AB'.code] = '"' // «
                this['\u00BB'.code] = '"' // »
                this['\u300A'.code] = '"' // 《
                this['\u300B'.code] = '"' // 》
                this['\uFF02'.code] = '"' // ＂
                this['\u2E42'.code] = '"'
                this['\u301D'.code] = '"'
                this['\u301E'.code] = '"'
                this['\u301F'.code] = '"'

                // PATTERN_QUOTATION (')
                this['\u2018'.code] = '\'' // ‘
                this['\u2019'.code] = '\'' // ’
                this['\u201B'.code] = '\'' // ‛

                this['\u2039'.code] = '<'
                this['\u276E'.code] = '<'
                this['\u203A'.code] = '>'
                this['\u276F'.code] = '>'

                // PATTERN_FULL_STOP -> '.'
                this['\u0589'.code] = '.' // ։
                this['\u0701'.code] = '.' // ܂
                this['\u06D4'.code] = '.' // ۔
                this['\u3002'.code] = '.' // 。
                this['\uFE52'.code] = '.' // ﹒
                this['\uFF0E'.code] = '.' // ．
                this['\uFF61'.code] = '.' // ｡
                this['\uFE12'.code] = '.' // ︒
                this['\u30FB'.code] = '.' // ・
                this['\u3001'.code] = '.' // 、

                // PATTERN_LEFT_BRACKET -> '['
                this['\u007B'.code] = '[' // {
                this['\u300E'.code] = '[' // 『
                this['\u3010'.code] = '[' // 【
                this['\uFF3B'.code] = '[' // ［
                this['\uFF62'.code] = '[' // ｢

                // PATTERN_RIGHT_BRACKET -> ']'
                this['\u007D'.code] = ']' // }
                this['\u300F'.code] = ']' // 』
                this['\u3011'.code] = ']' // 】
                this['\uFF3D'.code] = ']' // ］
                this['\uFF63'.code] = ']' // ｣
            }

            private fun isSpace(ch: Char): Boolean {
                return ch <= ' ' || Character.isWhitespace(ch)
            }

            /**
             * 工厂方法：负责构建索引映射并返回包装对象
             */
            @JvmStatic
            fun of(source: CharSequence?): CharSequence {
                if (source.isNullOrEmpty()) return ""

                val temp = IntArray( source.length)
                var count = 0

                // 一次遍历，筛选掉空格，构建索引表
                for ((i, element) in source.withIndex()) {
                    if (!isSpace(element)) {
                        temp[count++] = i
                    }
                }

                if (count == 0) return ""

                return StandardizedCharSequence(source, temp.copyOf(count))
            }
        }
    }
}