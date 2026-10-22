package com.now.nowbot.model.filter

import com.now.nowbot.entity.IDUser
import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.NanoUser
import com.now.nowbot.model.osu.LazerMod.Companion.toLazerModAcronyms
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.StringUtil.formatted
import com.now.nowbot.util.StringUtil.removeParticle
import com.now.nowbot.util.StringUtil.standardised
import com.now.nowbot.util.TimeParser
import com.now.nowbot.util.command.REGEX_OPERATOR_WITH_SPACE
import com.now.nowbot.util.command.REGEX_SPACE_MORE
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

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

    /**
     * 是否为范围型值。
     *
     * DECIMAL、INTEGER、TIME 这类值重复出现时通常是在收紧范围
     * （如 `pp>=600 pp>=700`），配合范围/排除运算符时取交集；
     * 如果是等值运算符（`=`、`==`），则仍然是在列举可接受的值，取并集。
     * 文本、模组、排名等非范围型值始终取并集。
     */
    val isRangeType: Boolean
        get() = this == DECIMAL || this == INTEGER || this == TIME

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
        get() = null

    /**
     * 可选的 value 首段校验。
     *
     * 同一个 prefix 可能对应多个分类器（例如 ScoreClassifier 的 `l` 既可能是
     * LANGUAGE 也可能是 LENGTH），提取器会在选中候选前用这个正则检查 value。
     * 为 null 时不限制。
     */
    val valueRegex: Regex?
        get() = null
}

/**
 * [CommandClassifier] 的通用筛选逻辑。
 *
 * 与 [ScoreClassifier] 的 `classify` 保持同一套语义：
 * - [StripContext.syntaxes] 的键为 `(分类器名, 运算符)`，值为该条件的所有取值；
 * - 同一个键下的多个值：范围型参数（DECIMAL/INTEGER/TIME）在 `>`、`>=`、`<`、`<=`、`!=`
 *   下取交集，在 `=`、`==` 下取并集；非范围型参数始终取并集；
 * - 不同键之间始终取交集。
 *
 * 各分类器只需要提供 `fit(item, operator, classifier, condition)` 即可复用这套规则。
 */
internal fun <T, C : CommandClassifier> Collection<T>.classifyBy(
    syntaxes: Map<Pair<String, Operator>, List<String>>,
    classifiers: Collection<C>,
    fitter: (T, Operator, C, Condition) -> Boolean,
): List<T> {
    val predicates = parseClassifyPredicates(syntaxes, classifiers, fitter)
    if (predicates.isEmpty()) return toList()

    return filter { item ->
        predicates.all { predicate -> predicate(item) }
    }
}

/**
 * 与 [classifyBy] 相同，但保留键值对：只按值筛选，键（名次、行号等）原样带出。
 */
internal fun <K, T, C : CommandClassifier> Map<K, T>.classifyValuesBy(
    syntaxes: Map<Pair<String, Operator>, List<String>>,
    classifiers: Collection<C>,
    fitter: (T, Operator, C, Condition) -> Boolean,
): Map<K, T> {
    val filtered: MutableMap<K, T> = LinkedHashMap(this)
    val predicates = parseClassifyPredicates(syntaxes, classifiers, fitter)

    if (predicates.isNotEmpty()) {
        filtered.values.removeIf { item ->
            !predicates.all { predicate -> predicate(item) }
        }
    }

    return filtered
}

private fun <T, C : CommandClassifier> parseClassifyPredicates(
    syntaxes: Map<Pair<String, Operator>, List<String>>,
    classifiers: Collection<C>,
    fitter: (T, Operator, C, Condition) -> Boolean,
): List<(T) -> Boolean> {
    val predicates = mutableListOf<(T) -> Boolean>()

    for ((key, values) in syntaxes) {
        val (name, operator) = key
        val classifier = classifiers.firstOrNull { it.name == name } ?: continue
        val conditions = values.map(::Condition)

        if (conditions.isEmpty()) continue

        val intersection = classifier.valueType.isRangeType &&
            operator != Operator.EQ && operator != Operator.XQ

        predicates.add { item ->
            if (intersection) {
                conditions.all { condition -> fitter(item, operator, classifier, condition) }
            } else {
                conditions.any { condition -> fitter(item, operator, classifier, condition) }
            }
        }
    }

    return predicates
}

/**
 * @param compare 被比较的数据
 * @param to 输入的数据，这里认为你已经在外面 standardized 了，否则开销是 O(N^2)
 */
fun fit(
    operator: Operator,
    compare: Any?,
    to: Any?,
): Boolean {
    return !(compare == null || to == null) && when (compare) {
        is Number if to is Number -> {
            if (isIntegral(compare) && isIntegral(to)) {
                compareLongs(operator, compare.toLong(), to.toLong())
            } else if (compare is BigDecimal && to is BigDecimal) {
                compareDecimals(operator, compare, to)
            } else if (to is BigDecimal) {
                compareDecimals(operator, compare.toString().toBigDecimal(), to)
            } else {
                compareDecimals(operator, BigDecimal(compare.toDouble()), BigDecimal(to.toDouble()))
            }
        }

        is String if to is String -> {
            val cs = compare.standardised()

            when (operator) {
                Operator.XQ -> to.equals(cs, ignoreCase = true)
                Operator.EQ -> cs.contains(to, ignoreCase = true)
                Operator.NE -> !cs.contains(to, ignoreCase = true)
                Operator.GT -> to.contains(cs, ignoreCase = true) && to.length > cs.length
                Operator.GE -> to.contains(cs, ignoreCase = true) && to.length >= cs.length
                Operator.LT -> cs.contains(to, ignoreCase = true) && to.length < cs.length
                Operator.LE -> cs.contains(to, ignoreCase = true) && to.length <= cs.length
            }
        }

        is Boolean if to is Boolean -> {
            when (operator) {
                Operator.XQ, Operator.EQ -> compare == to
                Operator.NE -> compare != to
                else -> throw IllegalArgumentException.WrongException.OperatorOnly("==", "=", "!=")
            }
        }

        is Enum<*> if to is Enum<*> -> {
            compareLongs(operator, compare.ordinal.toLong(), to.ordinal.toLong())
        }

        is List<*> if to is List<*> -> {
            val cs = compare.filterNotNull().toSet()
            val ts = to.filterNotNull().toSet()

            when (operator) {
                Operator.XQ -> cs == ts
                Operator.EQ -> cs.containsAll(ts)
                Operator.NE -> !cs.containsAll(ts)
                Operator.GT -> ts.containsAll(cs) && ts.size > cs.size
                Operator.GE -> ts.containsAll(cs) && ts.size >= cs.size
                Operator.LT -> cs.containsAll(ts) && cs.size < ts.size
                Operator.LE -> cs.containsAll(ts) && cs.size <= ts.size
            }
        }

        is Comparable<*> if compare::class == to::class -> {
            @Suppress("UNCHECKED_CAST")
            val c = compare as Comparable<Any>
            val cmp = c.compareTo(to)
            when (operator) {
                Operator.XQ, Operator.EQ -> cmp == 0
                Operator.NE -> cmp != 0
                Operator.GT -> cmp > 0
                Operator.GE -> cmp >= 0
                Operator.LT -> cmp < 0
                Operator.LE -> cmp <= 0
            }
        }

        else -> false
    }
}

private fun isIntegral(n: Number): Boolean =
    n is Long || n is Int || n is Short || n is Byte

// 辅助方法：Long 集中比较
private fun compareLongs(operator: Operator, c: Long, t: Long): Boolean {
    return when (operator) {
        Operator.XQ, Operator.EQ -> c == t
        Operator.NE -> c != t
        Operator.GT -> c > t
        Operator.GE -> c >= t
        Operator.LT -> c < t
        Operator.LE -> c <= t
    }
}

private val epsilon = BigDecimal("0.0001")

private fun compareDecimals(
    operator: Operator,
    compare: BigDecimal,
    to: BigDecimal
): Boolean {
    val dig = to.stripTrailingZeros().scale().coerceIn(0, 6)

    return when (operator) {
        Operator.EQ, Operator.NE -> {
            // 1. 加上 epsilon 进行尾数纠偏，再 FLOOR 截断到 dig 位
            // 8.9999996 + 0.000001 -> 9.0000006 -> FLOOR(0) -> 9.0
            val correctedCompare = compare + epsilon
            val normCompare = correctedCompare.setScale(dig, RoundingMode.FLOOR)
            val normTo = to.setScale(dig, RoundingMode.FLOOR)

            val isEqual = normCompare.compareTo(normTo) == 0
            if (operator == Operator.EQ) isEqual else !isEqual
        }

        Operator.XQ -> {
            // 2. 多放大 1 位精度，同样先加 epsilon 纠偏再截断
            val extendedDig = (dig + 1).coerceAtMost(6)

            val correctedCompare = compare + epsilon
            val normCompare = correctedCompare.setScale(extendedDig, RoundingMode.FLOOR)
            val normTo = to.setScale(extendedDig, RoundingMode.FLOOR)

            normCompare.compareTo(normTo) == 0
        }

        Operator.LE -> compare <= to + epsilon
        Operator.LT -> compare < to - epsilon
        Operator.GE -> compare >= to - epsilon
        Operator.GT -> compare > to + epsilon
    }
}

fun fitTags(operator: Operator, tags: String, to: String): Boolean {
    if (tags.isBlank() || to.isBlank()) return false

    val tagList = tags.replace('_', ' ')
        .split(REGEX_SPACE_MORE)
        .filter { it.isNotEmpty() }
    val toList = to.split(REGEX_SPACE_MORE)
        .filter { it.isNotEmpty() }

    return when {
        toList.size == 1 -> {
            val target = toList.first()
            tagList.any { tag -> fit(operator, tag, target) }
        }

        else -> {
            fit(operator, tagList, toList)
        }
    }
}

/**
 * 别名查询：任意一条别名命中条件即可。
 *
 * @param aliases 谱面集上挂的别名列表；为 null 或空时视为不匹配
 */
fun fitAlias(operator: Operator, aliases: List<String>?, string: String): Boolean {
    if (aliases.isNullOrEmpty()) return false

    val to = string.formatted().removeParticle()

    return aliases.any { alias -> fit(operator, alias.formatted().removeParticle(), to) }
}

/**
 * 谱师匹配：[long] 是 UID，[str] 是用户名。
 *
 * 只有输入了 UID 时才比较 UID，用户名始终参与比较，因此 `c=名字` 和 `c=123456` 都能命中。
 */
internal fun fitMapper(operator: Operator, mapper: IDUser, long: Long, str: String): Boolean {
    return (long > 0L && fit(operator, mapper.userID, long)) || fit(operator, mapper.username, str)
}

/**
 * 公用方法
 * 在 to 含有小数点时，按 compare 占 total 的百分比来处理。在其他情况时，按 compare 整数来处理。
 */
fun fitCountOrPercent(operator: Operator, compare: Number?, to: BigDecimal, total: Number?): Boolean {
    if (compare == null) return false

    val cleanedTo = to.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros()

    // 2. 判定是否含有有效小数：只需看剔除尾随 0 后的 scale 是否 > 0
    val hasDecimal = cleanedTo.scale() > 0

    // 3. 判断是否满足百分比模式条件：含有有效小数 且 在 [0.0, 1.0] 之间 且 不是 XQ
    val isPercentMode = hasDecimal
            && cleanedTo >= BigDecimal.ZERO
            && cleanedTo <= BigDecimal.ONE
            && operator != Operator.XQ

    return if (isPercentMode) {
        val totalBd = BigDecimal(total.toString())
        if (totalBd.abs() < epsilon) {
            false
        } else {
            val compareBd = BigDecimal(compare.toString())

            val ratio = compareBd.divide(totalBd, MathContext.DECIMAL64)
                .setScale(6, RoundingMode.HALF_UP)
                .stripTrailingZeros()

            // 传入高精度的 fit / compareDecimals 函数进行比对
            fit(operator, ratio, cleanedTo)
        }
    } else {
        // 整数模式：直接转为 Long 比较
        fit(operator, compare.toLong(), cleanedTo.toLong())
    }
}

fun fitTime(operator: Operator, compare: Long?, to: String): Boolean {
    val time = TimeParser.process(to) // 返回的是 ZonedDateTime
    val timeInstant = time.toInstant() // 转换为 UTC 瞬时点

    val nowInstant = Instant.now() // 获取当前 UTC 瞬时点

    if (nowInstant.isBefore(timeInstant)) {
        throw UnsupportedOperationException.InvalidFuture()
    }
    val isRelative = TimeParser.isRelativeTime(to)

    val toSec = timeInstant.toEpochMilli() / 1000L

    val sec = compare ?: 0L

    return when (operator) {
        Operator.GT -> {
            if (isRelative) sec < toSec else sec > toSec
        }
        Operator.GE -> {
            if (isRelative) sec <= toSec else sec >= toSec
        }
        Operator.LT -> {
            if (isRelative) sec > toSec else sec < toSec
        }
        Operator.LE -> {
            if (isRelative) sec >= toSec else sec <= toSec
        }

        else -> {
            // 1. 将毫秒值重新解析为当前时区的日期时间
            val zonedDateTime = Instant.ofEpochMilli(toSec)
                .atZone(ZoneId.systemDefault())

            // 2. 强制对齐到 0 点 (例如 15:30:25 -> 00:00:00)
            val startOfDay = zonedDateTime.toLocalDate().atStartOfDay(ZoneId.systemDefault())
            val startSec = startOfDay.toInstant().toEpochMilli() / 1000L

            when(operator) {
                Operator.EQ -> sec in startSec until (startSec + 1.days.inWholeSeconds)
                Operator.XQ -> sec in startSec until (startSec + 1.hours.inWholeSeconds)
                Operator.NE -> sec !in startSec until (startSec + 1.days.inWholeSeconds)
            }
        }
    }
}

fun fitMod(operator: Operator, compare: String, to: List<LazerMod>): Boolean {
    val com = compare.toLazerModAcronyms().toSet()
    val too = to
        .map { it.acronym }.filter { it != LazerMod.Classic.type }.toSet()

    return if (compare.isEmpty() || compare.contains(LazerMod.NoMod.type, ignoreCase = true)) {
        when (operator) {
            Operator.XQ, Operator.EQ -> too.isEmpty()
            Operator.NE, Operator.GE, Operator.GT -> too.isNotEmpty()
            else -> false
        }
    } else if (compare.contains(LazerMod.FreeMod.type, ignoreCase = true)) {
        when (operator) {
            Operator.XQ, Operator.EQ -> too.isNotEmpty()
            Operator.NE, Operator.LE, Operator.LT -> too.isEmpty()
            else -> false
        }
    } else {
        val ins = com.intersect(too)

        when (operator) {
            Operator.XQ -> com == too
            Operator.EQ,
            Operator.GE -> ins.size == com.size
            Operator.GT -> ins.size == com.size && com.size < too.size
            Operator.LE -> ins.size == too.size
            Operator.LT -> ins.size == too.size && com.size > too.size
            Operator.NE -> ins.isEmpty()
        }
    }
}