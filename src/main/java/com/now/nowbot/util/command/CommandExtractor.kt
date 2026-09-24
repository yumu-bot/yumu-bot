package com.now.nowbot.util.command

import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.filter.BeatmapsetFilter
import com.now.nowbot.model.filter.CommandClassifier
import com.now.nowbot.model.filter.ScoreClassifier
import com.now.nowbot.model.filter.ScoreFilter

enum class ExtractType {
    QQ,
    QQ_GROUP,
    RANGE,
    MODE,
    USER_ID,
    FLAG
}

data class StripContext(
    var text: String,
    val extracted: MutableMap<String, MutableList<String>> = mutableMapOf(),
    val syntaxes: MutableMap<Pair<String, Operator>, MutableList<String>> = mutableMapOf(),
) {
    fun exists(key: String): Boolean = extracted[key].isNullOrEmpty().not()

    // 快速获取某类提取结果
    operator fun get(key: String): List<String> = extracted[key] ?: emptyList()

    /**
     * 注意，这个 set 实质上是 add
     */
    operator fun set(key: String, value: String) {
        extracted.getOrPut(key) { mutableListOf() }.add(value)
    }

    fun exists(type: ExtractType): Boolean = exists(type.name)

    // 使用 Enum 作为 Key 获取提取结果
    operator fun get(type: ExtractType): List<String> = get(type.name)

    /**
     * 使用 Enum 作为 Key
     * 注意，这个 set 实质上是 add
     */
    operator fun set(type: ExtractType, value: String) = set(type.name, value)

    // 使用 Syntax 作为 Key 获取提取结果
    operator fun get(pair: Pair<String, Operator>): List<String> = syntaxes[pair] ?: emptyList()

    /**
     * 使用 Syntax 作为 Key
     * 注意，这个 set 实质上是 add
     */
    operator fun set(pair: Pair<String, Operator>, value: String) {
        syntaxes.getOrPut(pair) { mutableListOf() }.add(value)
    }

    fun clearFirst(text: String) {
        this.text = this.text.replaceFirst(text, "")
    }

    fun clearFirst(regex: Regex) {
        this.text = this.text.replaceFirst(regex, "")
    }

    fun clear(regex: Regex) {
        this.text = this.text.replace(regex, "")
    }

    fun reset(text: String) {
        this.text = text
    }

    fun collapseWhitespace() {
        this.text = this.text.replace(REGEX_SPACE_MORE, " ").trim()
    }

    fun extractPattern(
        type: ExtractType,
        regex: Regex,
        groupIndex: Int = 1
    ) {
        regex.findAll(this.text).forEach { match ->
            val value = match.groupValues.getOrNull(groupIndex) ?: match.value
            this[type] = value
        }
        this.clear(regex)
        this.collapseWhitespace()
    }

    fun extractPattern(
        type: ExtractType,
        regex: Regex,
        group: String
    ) {
        regex.findAll(this.text).forEach { match ->
            val value = match.groups[group]?.value ?: match.value
            this[type] = value
        }
        this.clear(regex)
        this.collapseWhitespace()
    }
}

fun interface CommandExtractor {
    fun process(context: StripContext)
}

class StripPipeline(initialText: String) {
    val context = StripContext(text = initialText)

    // 执行一个剥离组件
    fun apply(extractor: CommandExtractor): StripPipeline {
        extractor.process(context)
        return this
    }

    // 组合扩展：通用正则剥离组件
    fun stripPattern(
        tagKey: String,
        regex: Regex,
        captureGroup: Int = 1
    ): StripPipeline {
        return apply { ctx ->
            regex.findAll(ctx.text).forEach { match ->
                val value = match.groupValues.getOrElse(captureGroup) { match.value }
                ctx[tagKey] = value
            }

            ctx.clear(regex)
        }
    }

    // 清理多余空格
    fun trim(): StripPipeline {
        context.collapseWhitespace()
        return this
    }
}

// 4. DSL 入口
fun stripPipeline(input: String, block: StripPipeline.() -> Unit): StripContext {
    val pipeline = StripPipeline(input)
    pipeline.block()
    return pipeline.context
}

operator fun CommandExtractor.plus(next: CommandExtractor): CommandExtractor = CommandExtractor { ctx ->
    this.process(ctx)
    next.process(ctx)
}

val userIDGroupRegex = Regex("""u(?:ser)?id\s*${PATTERN_EQUAL}\s*${PATTERN_UID}""")

val qqGroupRegex = Regex("""group\s*${PATTERN_EQUAL}\s*${PATTERN_QQ}""")

val qqRegex = Regex("""qq\s*${PATTERN_EQUAL}\s*${PATTERN_QQ}""")

val modeRegex = Regex("""${PATTERN_COLON}\s*${PATTERN_MODE}""")

val rangeRegex = Regex("""${PATTERN_HASH}\s*(${PATTERN_NUMBER_1_100}\s*${PATTERN_HYPHEN}?\s*${PATTERN_NUMBER_1_100}?)""")

/// ===

val UserIDExtractor = CommandExtractor { ctx ->
    ctx.extractPattern(ExtractType.USER_ID, userIDGroupRegex)
}

val QQGroupExtractor = CommandExtractor { ctx ->
    ctx.extractPattern(ExtractType.QQ_GROUP, qqGroupRegex)
}

val QQExtractor = CommandExtractor { ctx ->
    ctx.extractPattern(ExtractType.QQ, qqRegex)
}

val OsuModeExtractor = CommandExtractor { ctx ->
    ctx.extractPattern(ExtractType.MODE, modeRegex)
}

val RangeExtractor = CommandExtractor { ctx ->
    ctx.extractPattern(ExtractType.RANGE, rangeRegex)
}

val ScoreClassifierExtractor = CommandExtractor { ctx ->
    ctx.extractByClassifier<ScoreClassifier>()
}

@Deprecated("use ScoreClassifierExtractor instead.")
val ScoreFilterExtractor = CommandExtractor { ctx ->
    ctx.extractByEnumFilters(
        filters = ScoreFilter.entries.dropLast(1),
        getRegex = { it.regex },
        loop = true
    )
}

val BeatmapsetExtractor = CommandExtractor { ctx ->
    ctx.extractByEnumFilters(
        filters = BeatmapsetFilter.entries.dropLast(1),
        getRegex = { it.regex },
        loop = true
    )
}

fun <T : Enum<T>> StripContext.extractByEnumFilters(
    filters: Iterable<T>,
    getRegex: (T) -> Regex,
    loop: Boolean = true
) {
    extractByFilters(
        filters = filters,
        getRegex = getRegex,
        getName = { it.name },
        loop = loop
    )
}

/**
 * 通用单次/循环剥离函数
 *
 * @param filters 需要匹配的 Filter 列表（例如 ScoreFilter.entries.dropLast(1)）
 * @param getRegex 获取 Filter 对应的 Regex 的 lambda
 * @param getName 获取 Filter 对应的 名称（Key） 的 lambda
 * @param loop 是否开启循环剥离（直至无新匹配为止）
 */
fun <T> StripContext.extractByFilters(
    filters: Iterable<T>,
    getRegex: (T) -> Regex,
    getName: (T) -> String,
    loop: Boolean = true
) {
    var matchedInThisRound: Boolean

    do {
        matchedInThisRound = false

        for (filter in filters) {
            val regex = getRegex(filter)
            val matchResult = regex.find(this.text)

            if (matchResult != null) {
                // 1. 优先获取组 n 的内容
                val rawNValue = try {
                    matchResult.groups["n"]?.value ?: matchResult.value
                } catch (_: IllegalArgumentException) {
                    matchResult.value
                }

                // 2. 从 rawNValue 中提取操作符与纯数值
                val rawTrimmed = rawNValue.trim()
                val opMatch = Regex(PATTERN_OPERATOR).find(rawTrimmed)
                val opStr = opMatch?.value?.trim() ?: ""
                val cleanValue = rawTrimmed.removePrefix(opStr).trim()

                // 3. 解析成 Operator 枚举
                val operator = Operator.getOperator(opStr)

                // 4. 存入 syntaxes 容器：Key 为 Pair(filterName, operator)
                val filterName = getName(filter)
                this[Pair(filterName, operator)] = cleanValue

                // 5. 剥离匹配文本并标记发生过匹配
                this.clearFirst(matchResult.value)
                matchedInThisRound = true
            }
        }
    } while (loop && matchedInThisRound)

    // 整理多余空格
    this.collapseWhitespace()
}

inline fun <reified E> StripContext.extractByClassifier()
        where E : Enum<E>, E : CommandClassifier {
    extractByClassifiers(enumValues<E>().toList())
}

// 提取所有 Operator 的符号，并按长度降序排列（优先匹配 ==, >=, <=, != 等）
private val sortedSymbols = Operator.entries
    .flatMap { it.symbols }
    .sortedByDescending { it.length }

fun StripContext.extractByClassifiers(classifiers: Iterable<CommandClassifier>) {
    var matchedInThisRound: Boolean

    do {
        matchedInThisRound = false

        // 1. 优先寻找字符串里含有任意 operator 的位置（包含全角归一化字符处理）
        var earliestOpIndex = -1
        var matchedSymbol = ""

        for (symbol in sortedSymbols) {
            val idx = this.text.indexOf(symbol)
            if (idx != -1 && (earliestOpIndex == -1 || idx < earliestOpIndex)) {
                earliestOpIndex = idx
                matchedSymbol = symbol
            }
        }

        // 找不到任何 operator 则直接结束
        if (earliestOpIndex == -1 || matchedSymbol.isEmpty()) break

        // 解析出具体的 Operator 枚举（利用你自定义的 getOperator 归一化解析）
        val operator = Operator.getOperator(matchedSymbol)

        val leftText = this.text.substring(0, earliestOpIndex)
        val rightText = this.text.substring(earliestOpIndex + matchedSymbol.length)

        // 2. 往左寻找分类器中的 prefix（跳过空白字符，紧贴运算符左侧的非空内容）
        var bestClassifier: CommandClassifier? = null
        var bestPrefixStart = -1

        // 先把 leftText 末尾的空白字符 trimEnd 掉，拿到紧贴 operator 的最右侧非空文本
        val trimmedLeftText = leftText.trimEnd()

        if (trimmedLeftText.isNotEmpty()) {
            for (classifier in classifiers) {
                // 寻找在 trimmedLeftText 结尾处能否匹配上 prefix
                val matches = classifier.prefix.findAll(trimmedLeftText).toList()
                if (matches.isNotEmpty()) {
                    val lastMatch = matches.last()

                    // 核心条件：匹配项必须刚好在 trimmedLeftText 的末尾结束
                    if (lastMatch.range.last == trimmedLeftText.lastIndex) {
                        if (lastMatch.range.first > bestPrefixStart) {
                            bestPrefixStart = lastMatch.range.first
                            bestClassifier = classifier
                        }
                    }
                }
            }
        }

        if (bestClassifier != null) {
            // 预先搜寻右侧文本中下一个 prefix 的起始位置，作为 value 的截断边界
            var nextPrefixIndex = rightText.length
            for (c in classifiers) {
                c.prefix.find(rightText)?.let {
                    if (it.range.first < nextPrefixIndex) {
                        nextPrefixIndex = it.range.first
                    }
                }
            }

            // 3. 往右根据 valueType 逐个字符寻找特定字符
            var valueEndIndex = 0
            val rawValueBuilder = StringBuilder()

            for (i in rightText.indices) {
                // 碰到了下一个 prefix
                if (i >= nextPrefixIndex) break

                val char = rightText[i]

                if (!bestClassifier.valueType.isValidChar(char)) {
                    break
                }

                rawValueBuilder.append(char)
                valueEndIndex = i + 1
            }

            var cleanValue = rawValueBuilder.toString().trim()

            // 4. 处理 suffix（如果结尾有 suffix，也要从 value 里扣除）
            bestClassifier.suffix?.let { suffixRegex ->
                val suffixMatch = suffixRegex.find(cleanValue)
                if (suffixMatch != null) {
                    cleanValue = cleanValue.substring(0, suffixMatch.range.first).trim()
                }
            }

            // 存入 syntaxes 容器，Key 为 Pair(classifier.name, operator)
            this[Pair(bestClassifier.name, operator)] = cleanValue

            // 5. 从原字符串中删去匹配到的字符（Prefix + Operator + Value/Suffix），避免二次污染
            val fullMatchEnd = earliestOpIndex + matchedSymbol.length + valueEndIndex
            this.text = this.text.removeRange(bestPrefixStart, fullMatchEnd).trim()

            matchedInThisRound = true
        } else {
            // 如果找到了 operator，但向左没有找到任何对应的 classifier prefix，
            // 移除当前运算符避免无限死循环
            this.text = this.text.removeRange(earliestOpIndex, earliestOpIndex + matchedSymbol.length).trim()
        }

    } while (matchedInThisRound)

    this.collapseWhitespace()
}

fun main() {
    println(rangeRegex.pattern)
//    val ctx = StripContext("pp>=600 acc>98.5% bid=123456 residual")
//
//    // 执行提取
//    ScoreFilterExtractor.process(ctx)
//
//    println("剩余文本: \"${ctx.text}\"") // 输出: "residual"
//
//    // 查询 syntaxes 中的结果
//    val ppGte = ctx[Pair(ScoreFilter.PERFORMANCE.name, Operator.GE)]
//    val accGt = ctx[Pair(ScoreFilter.ACCURACY.name, Operator.GT)]
//    val bidEq = ctx[Pair(ScoreFilter.BID.name, Operator.EQ)]
//
//    println("PP >= : $ppGte")  // 输出: [600]
//    println("ACC > : $accGt")  // 输出: [98.5]
//    println("BID = : $bidEq")  // 输出: [123456]
}