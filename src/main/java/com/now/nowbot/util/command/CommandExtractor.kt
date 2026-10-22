package com.now.nowbot.util.command

import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.filter.CommandClassifier
import com.now.nowbot.model.filter.FilterValueType
import com.now.nowbot.model.filter.ScoreClassifier

enum class ExtractType {
    QQ,
    QQ_GROUP,
    RANGE,
    MOD,
    MODE,
    USER_ID,
    BEATMAP_ID,
    BEATMAPSET_ID,
    STARRY
}

/**
 * 剩余内容里的一个字符。
 *
 * [index] 是它在原始指令文本（[StripContext.rawText]）里的下标；
 * 剥离过程中只删元素，不会重建字符串，所以这个下标始终有效，可直接用来排序。
 */
data class ResidualChar(val char: Char, val index: Int)

/**
 * 原文里下标连续的一段剩余内容。
 *
 * 被剥掉的参数会在两段之间留下断点，所以 [start] 不一定等于上一段的 [end]。
 * [start] / [end] 是原文坐标；折叠空白时 [text] 会比 `end - start` 短。
 * 片段按 [start] 天然升序，见 [StripContext.segments]。
 */
data class ResidualSegment(val start: Int, val text: String, val end: Int = start + text.length)

class StripContext(
    rawText: String,
    val extracted: MutableMap<String, MutableList<String>> = mutableMapOf(),
    val syntaxes: MutableMap<Pair<String, Operator>, MutableList<String>> = mutableMapOf(),
) {
    /** 剥离前的完整文本，[residual] 里的下标都以它为准。 */
    var rawText: String = rawText
        private set

    /**
     * 还没被剥掉的字符，按原文本顺序保存。
     *
     * 逐次剥离只从这里删除元素，不把剩下的字符重新拼成一个字符串，
     * 因此每段的 [ResidualChar.index] 始终指向原文位置。
     */
    val residual: MutableList<ResidualChar> = rawText.toResidual()

    /**
     * 剩余字符按当前顺序拼成的文本，兼容旧代码。
     *
     * 它不区分字符在原文里是否连续；需要区分时用 [segments] 或 [separatedText]。
     */
    val text: String
        get() = buildString(residual.size) { residual.forEach { append(it.char) } }

    /**
     * 剩余内容按原位置切成的连续片段。
     *
     * `abc<被剥掉的参数>def` 会得到 `abc`、`def` 两段，而不是粘在一起的 `abcdef`；
     * 单纯折叠空白造成的下标缺口不算断点，所以 `my  name` 仍然是一段。
     * 各段按 [ResidualSegment.start] 升序，等价于原文顺序。
     */
    val segments: List<ResidualSegment>
        get() {
            if (residual.isEmpty()) return emptyList()

            val result = ArrayList<ResidualSegment>()
            var start = residual[0].index
            val value = StringBuilder()

            for (i in residual.indices) {
                val current = residual[i]

                if (i > 0) {
                    val previous = residual[i - 1]
                    val gap = rawText.substring(previous.index + 1, current.index)

                    // 只有真的剥掉了非空白内容才算断点；折叠空白留下的缺口仍属同一段
                    if (gap.any { !it.isWhitespace() }) {
                        result.add(ResidualSegment(start, value.toString(), previous.index + 1))
                        start = current.index
                        value.setLength(0)
                    }
                }

                value.append(current.char)
            }

            result.add(ResidualSegment(start, value.toString(), residual.last().index + 1))

            return result
        }

    /**
     * 片段之间补一个空格后再拼起来，避免两段残文直接粘成一个词。
     * 保底解析玩家名时用这个，不要用 [text]。
     */
    val separatedText: String
        get() = segments.joinToString(" ") { it.text }
            .replace(REGEX_SPACE_MORE, " ")
            .trim()

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
        val at = this.text.indexOf(text)
        if (at >= 0) removeResidual(at, at + text.length)
    }

    fun clearFirst(regex: Regex) {
        val match = regex.find(text) ?: return
        removeResidual(match.range.first, match.range.last + 1)
    }

    fun clear(regex: Regex) {
        val ranges = regex.findAll(text).map { it.range }.toList()

        // 倒序删除，前面 match 的下标才不会失效
        for (range in ranges.asReversed()) {
            removeResidual(range.first, range.last + 1)
        }
    }

    fun reset(text: String) {
        rawText = text
        residual.clear()
        residual.addAll(text.toResidual())
    }

    fun collapseWhitespace() {
        if (residual.isEmpty()) return

        val collapsed = ArrayList<ResidualChar>(residual.size)
        var pendingSpace: ResidualChar? = null

        for (rc in residual) {
            if (rc.char.isWhitespace()) {
                // 一段空白只留第一个字符，并统一成空格；开头的空白直接丢掉
                if (pendingSpace == null && collapsed.isNotEmpty()) {
                    pendingSpace = rc.copy(char = ' ')
                }
            } else {
                pendingSpace?.let(collapsed::add)
                pendingSpace = null
                collapsed.add(rc)
            }
        }

        // 结尾的 pendingSpace 不加入，等价于 trim
        residual.clear()
        residual.addAll(collapsed)
    }

    /**
     * 按「当前拼接文本」[text] 的下标区间删除剩余字符。
     *
     * [text] 与 [residual] 一一对应，所以区间可以直接当 [residual] 的下标使用。
     */
    fun removeResidual(start: Int, end: Int) {
        if (start >= end) return
        residual.subList(start.coerceAtLeast(0), end.coerceAtMost(residual.size)).clear()
    }

    /**
     * 已经被消费掉的片段数（例如 [com.now.nowbot.util.CommandUtil.getUserID] 认作玩家名的那一段）。
     *
     * 段本身会从 [residual] 删除；这里只保留计数，让调用方还能知道「删除前是不是多段」。
     */
    var removedSegments: Int = 0
        private set

    /**
     * 按原文坐标删除一整段剩余内容，并累加 [removedSegments]。
     *
     * 与 [removeResidual] 不同，[ResidualSegment.start] / [ResidualSegment.end] 是原文下标，
     * 不受之前删掉了多少字符影响，适合把某一段消费掉之后精确移除它。
     */
    fun removeSegment(segment: ResidualSegment) {
        residual.removeAll { it.index in segment.start until segment.end }
        removedSegments++
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

/** 把一段原文摊成带原下标的字符列表 */
private fun String.toResidual(): MutableList<ResidualChar> =
    mapIndexedTo(ArrayList(length)) { index, char -> ResidualChar(char, index) }

fun interface CommandExtractor {
    fun process(context: StripContext)
}

class StripPipeline(initialText: String) {
    val context = StripContext(rawText = initialText)

    /**
     * 是否至少有一个剥离组件真的匹配到了东西。
     *
     * 判定标准是「[context] 被改动过」：剩余文本变短，或者 extracted / syntaxes 里多出了值。
     * 只做清理、不做提取的组件（例如 [trim]）不会把它置为 true。
     */
    var hasMatch: Boolean = false
        private set

    // 执行一个剥离组件
    fun apply(extractor: CommandExtractor): StripPipeline {
        val textBefore = context.text
        val extractedBefore = context.extracted.valueCount()
        val syntaxesBefore = context.syntaxes.valueCount()

        extractor.process(context)

        if (context.text != textBefore ||
            context.extracted.valueCount() != extractedBefore ||
            context.syntaxes.valueCount() != syntaxesBefore
        ) {
            hasMatch = true
        }
        return this
    }

    /** 所有剥离组件都没匹配到任何东西，即从头到尾没有剥离、也没有提取。 */
    fun matchedNothing(): Boolean = !hasMatch

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

/** context 里一共记录了多少个值，用来判断一次 [StripPipeline.apply] 有没有提取到东西 */
private fun Map<*, List<String>>.valueCount(): Int = values.sumOf { it.size }

operator fun CommandExtractor.plus(next: CommandExtractor): CommandExtractor = CommandExtractor { ctx ->
    this.process(ctx)
    next.process(ctx)
}

/// === 剥离引擎 ===

/**
 * 一个可以被剥离器认出来的「参数头」。
 *
 * 有两种形态，可以出现在同一行的任意位置、任意顺序：
 *
 * - 名字形式：`名字 + 运算符 + 值`，例如 `pp>=600`、`title='My Song'`；
 *   运算符左边的整段连续内容（不含空白）就是参数名，必须完整等于 [namePattern]。
 * - 符号形式：`符号 + 值`，例如 `:m`、`: m`、`#3-5`；符号本身即字段名，
 *   符号与值之间允许若干空格。
 *
 * 值的收尾规则见 [readValue]：
 * - 允许空格的类型（NAME / ANY）一路向后吞，直到运算符、冒号、引号，
 *   或者「下一个参数名 + 运算符」为止；
 * - 其余类型只吃一段不含空格的连续文本。
 * 单/双引号包裹的值优先级最高，引号直接锚定范围。
 *
 * 没被任何参数头认领的内容原样留在 [StripContext.residual] 里。
 */
class HeadSpec(
    val name: String,
    /** 名字形式：整段参数名必须与之完全匹配；为 null 表示不支持名字形式 */
    val namePattern: String? = null,
    /** 符号形式：候选符号集合，同义符号可以给多个（`:` / `：`）；为 null 表示不支持符号形式 */
    val symbols: List<String>? = null,
    val valueType: FilterValueType,
    /** 值的首段校验，同一个参数名有多种含义时用来挑候选 */
    val valueCheck: String? = null,
    /** 值末尾可以被吞掉的后缀，例如 `%`、`x` */
    val suffix: String? = null,
    val emit: (StripContext, Operator, String) -> Unit,
) {
    internal val nameMatcher: Regex? = namePattern?.let(::Regex)
    internal val valueRegex: Regex? = valueCheck?.let(::Regex)

    /** 候选符号按长度降序，长符号优先，避免短符号抢先匹配 */
    internal val symbolCandidates: List<String> = symbols.orEmpty().sortedByDescending(String::length)

    /** 后缀一律贴着值的末尾匹配，避免 `[%％]?` 这类可空正则在开头空匹配把值吃光 */
    internal val suffixRegex: Regex? = suffix?.let { Regex("(?:$it)\$") }

    /** 同一个后缀，但只要求从当前位置开始匹配：用来把字符集之外的后缀（`%`、`x`、`★`）也吞进 token */
    internal val suffixHeadRegex: Regex? = suffix?.let { Regex("(?:$it)") }

    internal fun suffixAt(text: String, at: Int): MatchResult? =
        suffixHeadRegex?.matchAt(text, at)?.takeIf { it.value.isNotEmpty() }

    internal fun accepts(value: String): Boolean {
        if (value.isEmpty()) return false
        if (!valueType.isValidLength(value)) return false
        val check = valueRegex ?: return true
        return check.find(value) != null
    }
}

/**
 * 符号形式的参数头。符号本身就代表字段，不需要参数名和运算符。
 *
 * 一项可以携带多个同义符号，等价于 `PATTERN_COLON` 那种 `[:：]` 的写法。
 */
enum class SymbolHead(
    vararg symbols: String,
    val type: ExtractType,
    val valueType: FilterValueType = FilterValueType.WORD,
) {
    MODE(":", "：", type = ExtractType.MODE),
    RANGE("#", "＃", type = ExtractType.RANGE),
    MOD("+", "＋", type = ExtractType.MOD),
    STAR("#", "＃", type = ExtractType.STARRY),
    ;

    /** 该头认的所有符号，按长度降序，长符号优先 */
    val symbols: List<String> = symbols.sortedByDescending(String::length)
}

private fun SymbolHead.toHeadSpec() = HeadSpec(
    name = type.name,
    symbols = symbols,
    valueType = valueType,
    emit = { ctx, _, value -> ctx[type] = value },
)

private fun CommandClassifier.toHeadSpec() = HeadSpec(
    name = name,
    namePattern = prefix.pattern,
    valueType = valueType,
    valueCheck = valueRegex?.pattern,
    suffix = suffix?.pattern,
    emit = { ctx, operator, value -> ctx[Pair(name, operator)] = value },
)

/**
 * 按 [heads] 一次性扫描整行，把所有认出来的参数从 [StripContext.residual] 里摘掉。
 */
fun StripContext.extractByHeads(heads: List<HeadSpec>) {
    // 先取一份快照：text 是计算属性，循环里反复取会把剩余字符重复拼很多遍
    val text = this.text
    if (heads.isEmpty() || text.isEmpty()) return

    val hits = ArrayList<TokenHit>()
    var i = 0
    while (i < text.length) {
        // 参数头只能从词首开始，避免把单词内部的片段误认成参数名
        if (i > 0 && text[i - 1].isLetterOrDigit()) {
            i++
            continue
        }
        val hit = matchHead(text, i, heads)
        if (hit == null) {
            i++
            continue
        }
        hits.add(hit)
        i = hit.end
    }

    if (hits.isEmpty()) return

    // 倒序删除，前面 token 的下标才不会失效
    for (index in hits.indices.reversed()) {
        val hit = hits[index]
        removeResidual(hit.start, hit.end)
    }
    for (hit in hits) {
        hit.head.emit(this, hit.operator, hit.value)
    }
    collapseWhitespace()
}

inline fun <reified E> StripContext.extractByClassifier()
        where E : Enum<E>, E : CommandClassifier {
    extractByClassifiers(enumValues<E>().toList())
}

fun StripContext.extractByClassifiers(classifiers: Iterable<CommandClassifier>) {
    extractByHeads(classifiers.map { it.toHeadSpec() })
}

fun StripContext.extractBySymbols(heads: Iterable<SymbolHead>) {
    extractByHeads(heads.map { it.toHeadSpec() })
}

fun StripContext.extractBySymbols(vararg heads: SymbolHead) {
    extractBySymbols(heads.toList())
}

/// === 扫描细节 ===

private class ValueSpan(val value: String, val end: Int)

private class TokenHit(
    val head: HeadSpec,
    val operator: Operator,
    val value: String,
    val start: Int,
    val end: Int,
)

/**
 * 运算符的起首字符，用来判断参数名的右边界。
 *
 * 直接从 [Operator.allSymbols] 推导，包含全角 / 别名写法（`＞`、`＝`、`！` 等），
 * 避免这里和运算符表两处维护、漏掉新别名。
 */
private val OPERATOR_START_CHARS: Set<Char> = Operator.allSymbols.mapTo(HashSet<Char>()) { it[0] }

private val NAME_STOP_CHARS = setOf(':', '：', '\'', '"')

/**
 * 所有符号头使用的字符，一律视为值的硬边界。
 *
 * 符号有自己的主人（`#` 就是 RANGE），不该被名字类的值顺手吞掉。
 * 这样「先跑名字类还是先跑符号类」就不再影响结果了。
 */
private val RESERVED_SYMBOLS: Set<Char> =
    SymbolHead.entries.flatMap { head -> head.symbols.flatMap { it.toList() } }.toSet()

// 提取所有 Operator 的符号（含全角 / 别名写法），并按长度降序排列（优先匹配 ==, >=, ＞＝ 等多字符符号）
private val sortedSymbols = Operator.allSymbols

private fun isOperatorStart(c: Char) = c in OPERATOR_START_CHARS

private fun skipSpaces(text: String, from: Int): Int {
    var i = from
    while (i < text.length && text[i].isWhitespace()) i++
    return i
}

private fun symbolAt(text: String, at: Int): String? {
    if (at >= text.length) return null
    for (symbol in sortedSymbols) {
        if (text.startsWith(symbol, at)) return symbol
    }
    return null
}

/** 在 [at] 处匹配 [candidates] 里最长的一个符号；候选需已按长度降序 */
private fun symbolAt(text: String, at: Int, candidates: List<String>): String? {
    if (at >= text.length) return null
    for (symbol in candidates) {
        if (text.startsWith(symbol, at)) return symbol
    }
    return null
}

/** 参数名的右边界：连续的非空白、非运算符、非冒号引号内容 */
private fun nameRunEnd(text: String, from: Int): Int {
    var i = from
    while (i < text.length) {
        val c = text[i]
        if (c.isWhitespace() || isOperatorStart(c) || c in NAME_STOP_CHARS) break
        i++
    }
    return i
}

private fun matchHead(text: String, start: Int, heads: List<HeadSpec>): TokenHit? {
    // 1. 符号形式：符号 + 若干空格 + 值
    for (head in heads) {
        val symbol = symbolAt(text, start, head.symbolCandidates) ?: continue

        val span = readValue(text, start + symbol.length, head, heads) ?: continue
        if (!head.accepts(span.value)) continue
        return TokenHit(head, Operator.EQ, span.value, start, span.end)
    }

    // 2. 名字形式：参数名 + 若干空格 + 运算符 + 值
    val runEnd = nameRunEnd(text, start)
    if (runEnd == start) return null

    val name = text.substring(start, runEnd)
    val opPos = skipSpaces(text, runEnd)
    val operatorSymbol = symbolAt(text, opPos) ?: return null
    val operator = Operator.getOperator(operatorSymbol)

    val valueFrom = opPos + operatorSymbol.length
    for (head in heads) {
        val matcher = head.nameMatcher ?: continue
        if (!matcher.matches(name)) continue

        val span = readValue(text, valueFrom, head, heads) ?: continue
        if (!head.accepts(span.value)) continue
        return TokenHit(head, operator, span.value, start, span.end)
    }
    return null
}

private fun readValue(text: String, from: Int, head: HeadSpec, heads: List<HeadSpec>): ValueSpan? {
    val start = skipSpaces(text, from)
    if (start >= text.length) return null

    // 引号优先级最高，直接锚定范围
    val first = text[start]
    if (first == '\'' || first == '"') {
        val close = text.indexOf(first, start + 1)
        if (close < 0) return null
        val value = stripSuffix(text.substring(start + 1, close), head)
        return if (value.isEmpty()) null else ValueSpan(value, close + 1)
    }

    var end = start
    if (head.valueType.allowsSpace) {
        // 一直向后吞，直到下一个参数头
        while (end < text.length && !isBoundary(text, end, heads)) end++
    } else {
        while (end < text.length && !text[end].isWhitespace() && head.valueType.isValidChar(text[end])) end++
        // 后缀（`%`、`x`、`★`）不在字符集里，单独再吞一次，避免留在剩余文本里
        head.suffixAt(text, end)?.let { end += it.value.length }
    }
    if (end == start) return null

    val value = stripSuffix(text.substring(start, end), head)
    return if (value.isEmpty()) null else ValueSpan(value, end)
}

private fun stripSuffix(raw: String, head: HeadSpec): String {
    val regex = head.suffixRegex ?: return raw.trim()
    val match = regex.find(raw) ?: return raw.trim()
    return raw.substring(0, match.range.first).trim()
}

/** 值是否应该在这里收尾 */
private fun isBoundary(text: String, at: Int, heads: List<HeadSpec>): Boolean {
    val c = text[at]
    if (isOperatorStart(c) || c in NAME_STOP_CHARS || c in RESERVED_SYMBOLS) return true

    // 只有词首才可能开始一个新的参数名
    if (at > 0 && text[at - 1].isLetterOrDigit()) return false
    return startsNamedHead(text, at, heads)
}

/** 这里是否是一个「已知参数名 + 运算符」的开头 */
private fun startsNamedHead(text: String, at: Int, heads: List<HeadSpec>): Boolean {
    val runEnd = nameRunEnd(text, at)
    if (runEnd == at) return false

    val name = text.substring(at, runEnd)
    if (symbolAt(text, skipSpaces(text, runEnd)) == null) return false
    return heads.any { it.nameMatcher?.matches(name) == true }
}

/// === 现成的剥离组件 ===

/**
 * `uid=123` / `userid=123`。只捕获数字部分，方便 [StripContext.extractPattern] 直接存成 ID。
 */
private val userIDGroupRegex = Regex("""u(?:ser)?id\s*${PATTERN_EQUAL}\s*(${PATTERN_NUMBER_MORE})""")

private val qqGroupRegex = Regex("""group\s*${PATTERN_EQUAL}\s*${PATTERN_QQ}""")

private val qqRegex = Regex("""qq\s*${PATTERN_EQUAL}\s*${PATTERN_QQ}""")

private val beatmapIDRegex = Regex("""(?:beatmap|b)\s*(${PATTERN_NUMBER_MORE})""")

private val beatmapsetIDRegex = Regex("""(?:beatmapset|set|s)\s*(${PATTERN_NUMBER_MORE})""")

val UserIDExtractor = CommandExtractor { ctx ->
    ctx.extractPattern(ExtractType.USER_ID, userIDGroupRegex)
}

val BeatmapIDExtractor = CommandExtractor { ctx ->
    ctx.extractPattern(ExtractType.BEATMAP_ID, beatmapIDRegex)
}

val BeatmapsetIDExtractor = CommandExtractor { ctx ->
    ctx.extractPattern(ExtractType.BEATMAPSET_ID, beatmapsetIDRegex)
}

val QQGroupExtractor = CommandExtractor { ctx ->
    ctx.extractPattern(ExtractType.QQ_GROUP, qqGroupRegex)
}

val QQExtractor = CommandExtractor { ctx ->
    ctx.extractPattern(ExtractType.QQ, qqRegex)
}

val OsuModeExtractor = CommandExtractor { ctx ->
    ctx.extractBySymbols(SymbolHead.MODE)
}

/**
 * 把 `+HR` 这类符号形式的 mod 桥接成一条筛选条件。
 *
 * [SymbolHead.MOD] 只把值写进 [StripContext.extracted]，而 [ScoreClassifier] 只看
 * [StripContext.syntaxes]；不桥接的话 `!bp +HR` 会把 `+HR` 从指令里剥掉却完全不筛成绩，
 * 等于这个参数被吞掉了。取 [ScoreClassifier.MOD] 的键、[Operator.EQ]，与手写 `mod=+HR` 等价。
 *
 * 多个 `+HR +DT` 合并成一条条件（`HRDT`），和 [com.now.nowbot.model.filter.fitMod] 一样
 * 要求列出的 mod 全部命中；同键已有 `mod=+HD` 时并列进去，语义不变。
 */
fun StripContext.addModsAsCondition() {
    val mods = this[ExtractType.MOD].filter(String::isNotBlank).distinct()
    if (mods.isEmpty()) return

    val key = ScoreClassifier.MOD.name to Operator.EQ
    val merged = (syntaxes[key].orEmpty() + mods.joinToString("")).joinToString("")

    syntaxes[key] = mutableListOf(merged)
}

val LazerModExtractor = CommandExtractor { ctx ->
    ctx.extractBySymbols(SymbolHead.MOD)
    // `+HR` 既用来算 pp / 查指定 mod 的成绩，也要参与成绩筛选
    ctx.addModsAsCondition()
}

val RangeExtractor = CommandExtractor { ctx ->
    ctx.extractBySymbols(SymbolHead.RANGE)
}

val ScoreClassifierExtractor = CommandExtractor { ctx ->
    ctx.extractByClassifier<ScoreClassifier>()
}

fun main() {
    val samples = listOf(
        "!bp pp>=600 acc>98.5% title='My Song' :m #3-5 residual",
        "pp>=600",
        "pp >= 600",
        ":  m",
        "#3-5",
        "combo>500x sr>6.5★",
        "l=en",
        "l=3:30",
        "title=My Song",
        "title=My Song acc>98",
        "bid=123456 sid=789",
        "mod=+hr",
        "pp>=",
        "replay=true pp>=600",
        "title=Song #3",
        "pp>=600 #3-5 :m",
        // 全角同义符号
        "：m ＃3-5",
        "!bp ：m ＃3-5 pp>=600",
        // 全角运算符（半角/全角混用也要能认）
        "sr＞6.8",
        "sr＞＝6.8 acc＞98",
        "ar＜9",
        "pp！＝600",
        // NAME 限长 3~15；WORD / ANY 不限长
        "title=abcdefghijklmnop",
        "tag=ab",
        "diff=Reform Extra",
        "replay=0 cv=std",
    )

    // 正序 / 逆序各跑一遍，用来确认「顺序不影响结果」
    fun run(sample: String, extractors: List<CommandExtractor>): StripContext {
        val ctx = StripContext(sample)
        extractors.forEach { it.process(ctx) }
        return ctx
    }

    val forward = listOf(ScoreClassifierExtractor, OsuModeExtractor, RangeExtractor)
    val backward = forward.reversed()

    for (sample in samples) {
        val a = run(sample, forward)
        val b = run(sample, backward)

        val parsed = a.syntaxes.entries.joinToString(", ") { "${it.key.first}${it.key.second}${it.value}" }
        val extra = a.extracted.entries.joinToString(", ") { "${it.key}${it.value}" }
        val same = a.text == b.text && a.syntaxes == b.syntaxes && a.extracted == b.extracted

        // 剩余字符必须仍能按 index 指回原文，否则位置信息就失效了
        val positionsOk = a.residual.all { a.rawText[it.index] == it.char }
        val segments = a.segments.joinToString(" | ") { "${it.start}..${it.end}\"${it.text}\"" }

        println(
            "$sample\n  -> [$parsed] [$extra] 剩余=\"${a.text}\"" +
                    " 分段=[$segments] 名字=\"${a.separatedText}\"" +
                    " 位置=${if (positionsOk) "OK" else "BROKEN"}" +
                    if (same) "" else "\n  !! 顺序敏感: 逆序剩余=\"${b.text}\""
        )
    }
}
