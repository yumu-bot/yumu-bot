package com.now.nowbot.util

import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.GroupDao
import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.isNotDefaultOrNull
import com.now.nowbot.model.enums.OsuMode.Companion.toOsuMode
import com.now.nowbot.model.filter.ScoreClassifier
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.service.sbApiService.SBUserApiService
import com.now.nowbot.util.command.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 把 [com.now.nowbot.util.command.CommandExtractor] 剥离后的 [StripContext] 补成一个可直接使用的上下文。
 *
 * 只负责「脚手架」性质的三件事：补模式、补玩家、补范围。
 * 具体的业务判断（模式是否合法、越界怎么处理）留给组件。
 */
@Component
class CommandUtil(
    private val bindDao: BindDao,
    private val groupDao: GroupDao,
    private val userApiService: OsuUserApiService,
    private val sbUserApiService: SBUserApiService,
) {
    private val log = LoggerFactory.getLogger(CommandUtil::class.java)

    /**
     * 取得本次指令最终生效的模式，优先级从高到低：
     *
     * 1. 指令里显式指定的模式（`ctx[ExtractType.MODE]`，如 `m=c`）；
     * 2. 成绩条件反推（[inferModeFromSyntaxes]）：`mf>=1`、`彩率>=0.5` 这类只有某个模式
     *    才有的条件，命中时采用；
     * 3. 群组设定（[GroupDao.getGroupMode]）；
     * 4. 玩家的绑定模式（[BindDao.getBindModeFromID]），需要 [userID]；
     * 5. 都没有时为 [OsuMode.DEFAULT]。
     *
     * 第 1、2 步同时命中且互不相容（既不是同一模式，也不能互相转谱）时，
     * 抛出 [IllegalArgumentException.WrongException.ModeConflict]，提示用户统一模式。
     *
     * 第 3、4 步命中时会把模式补写回 [ctx]，让后面直接读 `ctx[ExtractType.MODE]`
     * 的组件也能看到同一份结果。
     *
     * @param event 可以为空；为空时 [GroupDao.getGroupMode] 会返回 [OsuMode.DEFAULT]
     * @param userID 解析出的玩家 osu! ID；为空时跳过第 4 步
     * @return 最终生效的模式
     */
    fun getMode(
        ctx: StripContext,
        event: MessageEvent? = null,
        userID: Long? = null,
    ): OsuMode {
        // 1. 指令里显式指定的模式
        val extracted = ctx[ExtractType.MODE]
            .filter { it.isNotBlank() }
            .map { it.toOsuMode() }
            .firstOrNull { it.isNotDefault }

        // 2. 条件本身指明了模式
        val inferred = inferModeFromSyntaxes(ctx.syntaxes)

        if (extracted.isNotDefaultOrNull() && inferred != null) {
            if (!extracted.isConvertAble(inferred)) {
                throw IllegalArgumentException.WrongException.ModeConflict(extracted, inferred)
            }

            return extracted
        }

        if (extracted.isNotDefaultOrNull()) return extracted
        if (inferred != null) return inferred

        // 3. 群组设定
        val groupMode = groupDao.getGroupMode(event)

        if (groupMode.isNotDefaultOrNull()) {
            ctx[ExtractType.MODE] = groupMode.shortName
            return groupMode
        }

        // 4. 玩家绑定
        val bindMode = userID?.let { bindDao.getBindModeFromID(it) }

        if (bindMode.isNotDefaultOrNull()) {
            ctx[ExtractType.MODE] = bindMode.shortName
            return bindMode
        }

        return OsuMode.DEFAULT
    }

    enum class BanchoType {
        OFFICIAL, PPYSB
    }

    /**
     * 能反推出模式的成绩条件，按优先级排列。
     *
     * 漏大果 / 漏中果 / 漏小果只有 osu!catch 才有，
     * 彩率 / 黄彩比只有 osu!mania 才有。
     */
    private val SCORE_MODE_HINTS: List<Pair<ScoreClassifier, OsuMode>> = listOf(
        ScoreClassifier.MISSED_FRUIT to OsuMode.CATCH,
        ScoreClassifier.MISSED_DROP to OsuMode.CATCH,
        ScoreClassifier.MISSED_DROPLET to OsuMode.CATCH,
        ScoreClassifier.RATE to OsuMode.MANIA,
    )

    /**
     * 从成绩筛选条件里反推模式，没有提示时返回 null。
     *
     * 漏大果 / 漏中果 / 漏小果只有 osu!catch 才有，
     * 彩率 / 黄彩比只有 osu!mania 才有。
     * 多个提示同时出现时按 [SCORE_MODE_HINTS] 的顺序取第一个，保持原来的 if / else-if 语义。
     *
     * @param syntaxes 解析结果，键为 `(分类器名, 运算符)`，通常直接来自 [StripContext.syntaxes]
     */
    private fun inferModeFromSyntaxes(
        syntaxes: Map<Pair<String, Operator>, List<String>>,
    ): OsuMode? {
        if (syntaxes.isEmpty()) return null

        val names = HashSet<String>(syntaxes.size)
        for ((name, _) in syntaxes.keys) names.add(name)

        return SCORE_MODE_HINTS.firstOrNull { (classifier, _) -> classifier.name in names }?.second
    }


    /**
     * 解析本次指令指向的玩家，返回 osu! ID。
     *
     * 优先级：
     * 1. `ctx[ExtractType.USER_ID]`（`uid=123`）：直接采用，并且和 at 一样不再把剩余文本
     *    当名字，但仍会尝试从剩余文本里补一个范围；
     * 2. `event.hasAt()`：把被 at 的 QQ 从 [BindDao] 的绑定里换成 osu! ID；
     * 3. `ctx[ExtractType.QQ]`（`qq=123456`）：同样从 [BindDao] 的绑定里换成 osu! ID；
     * 4. 剩余文本按 [StripContext.segments] 拆成候选名字逐段查找：每段先 trim，只有符合
     *    [REGEX_OSU_USERNAME] 的段才参与查询，长段优先；不像名字的段直接跳过（不联网）；
     *    同时处理「玩家名和范围重名」的歧义（例如 `osu happy 2` 里的 `2` 既可能是名的一部分，
     *    也可能是范围）；
     * 5. 没有给名字（文本为空，或只剩一个范围）：使用发送者自己的绑定；
     *    有名字但所有候选段都不符合 [REGEX_OSU_USERNAME] 时按查不到处理，不再拼成一整段去查。
     *
     * 名字与范围的歧义规则：
     * - extractor 已经拿到 range 时，剩下的候选名字逐段尝试；
     * - 没有 range 时，文本整体是范围则把玩家当自己；
     * - 否则逐段尝试把开头 / 结尾的范围 token 拆出去，**只有名字部分真的能查到**才承认它是范围；
     * - 名字拆不出来或查不到，就把候选名字从长到短依次查一遍。
     *
     * 名字查找先走 [BindDao] 的本地缓存，再走 [OsuUserApiService]；
     * 远端异常在这里吞掉并记日志，不让查名字失败影响指令本身。
     *
     * 本方法不会重建 [StripContext.text]；命中玩家名的那个残余片段会从
     * [StripContext.residual] 移除（避免后续 [addResidualAliases] 把玩家名误当别名），
     * 并按需补写 `RANGE`。
     *
     * @param onNull 最终没有解析到玩家时调用（包含 at 的用户没有绑定、名字查不到、
     *               自己也没有绑定的情况）。默认什么都不做；调用方可以传 `{ throw ... }`
     *               在没解析到时直接抛异常。
     * @return 解析到的 osu! ID；`onNull` 没有抛异常时为 null。
     */
    fun getUserID(
        ctx: StripContext,
        event: MessageEvent,
        onNull: (Boolean) -> Unit = {},
        type: BanchoType = BanchoType.OFFICIAL
    ): Long? {
        // 用分离后的文本，避免被剥掉的部分把两段残文粘成一个查不到的名字
        val text = ctx.separatedText
        val hasRange = ctx.exists(ExtractType.RANGE)
        
        val isMyself = event.targets.contains(event.sender.contactID) || 
                (ctx[ExtractType.QQ].map { it.toLongOrNull() ?: -1L }.contains(event.sender.contactID))

        // 0. uid= 直接给了 osu! ID，和 at 一样跳过名字解析
        getContextUserID(ctx)?.let { userID ->
            if (!hasRange) {
                extractRangeToken(ctx, text)
            }

            return userID
        }

        // 1. at 了人，剩下的东西只可能混着范围，不可能是玩家名
        if (event.hasAt()) {
            if (!hasRange) {
                extractRangeToken(ctx, text)
            }

            return lookupBindID(event.target, type) ?: notFound(onNull, isMyself)
        }

        // 1.5 qq= 指定了玩家，和 at / uid= 一样不再把剩余文本当名字
        getContextQQ(ctx)?.let { qq ->
            if (!hasRange) {
                extractRangeToken(ctx, text)
            }

            return lookupBindID(qq, type) ?: notFound(onNull, isMyself)
        }

        // 2. 没有名字：文本为空，或 extractor 拿走了整段范围
        if (text.isEmpty()) {
            return lookupBindID(event.sender.contactID, type) ?: notFound(onNull, isMyself)
        }

        // 残余可能被剥成多段：逐段 trim 后必须像一个 osu! 用户名才参与查询，长段优先。
        // 全部不合格时 nameCandidates 为空，后面的查询会直接失败，不会把垃圾文本当名字发出去。
        val nameCandidates = ctx.segments
            .map { it to it.text.trim() }
            .filter { (_, name) -> name.isNotEmpty() && REGEX_OSU_USERNAME.matches(name) }
            .distinctBy { (_, name) -> name }
            .sortedByDescending { (_, name) -> name.length }

        // 3. 范围已经明确，剩下的候选名字从长到短依次尝试
        if (hasRange) {
            return lookupFirstUser(ctx, nameCandidates, type) ?: notFound(onNull, isMyself)
        }

        // 4. 整段就是一个范围，玩家是自己
        if (RANGE_TOKEN_REGEX.matches(text)) {
            addRange(ctx, text)

            return lookupBindID(event.sender.contactID, type) ?: notFound(onNull, isMyself)
        }

        // 5. 逐段尝试把开头 / 结尾的范围拆出去，名字能查到才认。
        // 不能拿整段 separatedText 去拆：`yanmaizhou329 1-5` 和别名可能被剥成两段，
        // 整段拆不出来，但第一段自己是可以拆出「名字 + 范围」的。
        for (segment in ctx.segments.sortedByDescending { it.text.trim().length }) {
            val split = splitNameWithRange(segment.text.trim()) ?: continue
            val splitName = split.first.trim()

            // 拆出来的名字也必须像用户名，否则不浪费一次查询
            if (!REGEX_OSU_USERNAME.matches(splitName)) continue

            val userID = lookupUser(splitName, type) ?: continue

            addRange(ctx, split.second)
            ctx.removeSegment(segment)

            return userID
        }

        // 5.5 范围自己单独成了一段时，逐段拆不出来，但仍可能从整段 separatedText 拆出
        // 「名字 + 范围」；命中后只移除名字所在的片段，单独的 range 片段留给 RANGE 记录。
        val wholeSplit = splitNameWithRange(text)

        if (wholeSplit != null) {
            val splitName = wholeSplit.first.trim()

            if (REGEX_OSU_USERNAME.matches(splitName)) {
                val userID = lookupUser(splitName, type)

                if (userID != null) {
                    addRange(ctx, wholeSplit.second)
                    removeSegmentsContainedIn(ctx, splitName)

                    return userID
                }
            }
        }

        return lookupFirstUser(ctx, nameCandidates, type) ?: notFound(onNull, isMyself)
    }

    /**
     * 把 [getUserID] 之后仍然剩下的多段文本当作谱面别名条件写回 [ctx]。
     *
     * 只在剩余内容原本有至少两段时生效：单段残文更可能是没解析出来的玩家名，应该留给调用方报错，
     * 不能悄悄变成别名去筛成绩。已经被 [getUserID] 消费掉的玩家名段也计入这个段数，
     * 所以「玩家名 + 一个别名」在名字被移除后仍然会保留别名。每段会先 trim，
     * 然后只要满足下面任意一条即可：
     * - 至少 1 个中文字符；
     * - 至少 2 个英文字母；
     * - 数字既不算中文也不算英文（不参与计数）。
     *
     * 合格的段写入 `ScoreClassifier.ALIASES` 的 `=` 条件，键为
     * `Pair(ScoreClassifier.ALIASES.name, Operator.EQ)`；同一个键下多个值在
     * [ScoreClassifier] 里取并集，所以多段别名是“命中任意一个即可”。
     *
     * 必须在本轮 [getUserID] 结束后调用：先把残文里的玩家名 / 范围解析掉，剩下的才可能是别名。
     *
     * @return 实际新增的别名数量
     */
    fun addResidualAliases(ctx: StripContext): Int {
        val segments = ctx.segments

        // 玩家名段已经被 getUserID 删除；把它加回来才能判断删除前是否多段。
        if (segments.size + ctx.removedSegments < 2) return 0

        val key = Pair(ScoreClassifier.ALIASES.name, Operator.EQ)
        var added = 0

        for (segment in segments) {
            val alias = segment.text.trim()

            if (!isAliasCondition(alias)) continue
            if (alias in ctx[key]) continue

            ctx[key] = alias
            added++
        }

        return added
    }

    /**
     * 别名条件的最低要求：至少 1 个中文字符，或至少 2 个英文字母；数字不计入。
     */
    private fun isAliasCondition(text: String): Boolean {
        if (text.isEmpty()) return false

        var chinese = 0
        var english = 0

        for (c in text) {
            when {
                Character.UnicodeScript.of(c.code) == Character.UnicodeScript.HAN -> chinese++
                c in 'a'..'z' || c in 'A'..'Z' -> english++
            }
        }

        return chinese >= 1 || english >= 2
    }

    /**
     * 把本次指令里的范围换算成列表接口直接能用的 offset / limit。
     *
     * 只读 [StripContext] 里已经提取好的 `RANGE`（`3`、`3-5`、`#3-5`、`＃3-5` 都行），
     * 不会再去 [StripContext.separatedText] 里补抠；需要补抠时先调用 [getUserID]。
     * 同一个 ctx 里有多段范围时取最后一段，和 [extractRangeToken] 的取值方式保持一致。
     *
     * 换算规则：
     * - 没有范围、或范围里连起点都认不出来：返回 `defaultOffset to defaultLimit`；
     * - 只有一个 `0`：语义是「全部取走」，返回 `0 to max`；
     * - `3-5`：`2 to 3`；
     * - `5-3`：`2 to 3`，反着写同样当作范围，和 [com.now.nowbot.util.DataUtil.parseRange2Offset] 一致；
     * - 只有一个非 0 数字时由 [multiple] 决定它的含义：
     *   - `false`（默认）：`30` 是「第 30 个」，返回 `29 to 1`；
     *   - `true`：`30` 是「取 30 条」，返回 `defaultOffset to 30`；
     * - 起点小于 1 时按 1 处理，offset 不会是负数。
     *
     * @param defaultOffset 没有范围时的 offset，也是 [multiple] 为 true 时取条数的起点，默认 0
     * @param defaultLimit 没有范围时的 limit，默认 1
     * @param multiple 只有一个数字时，是否把它当成「条数」而不是「第几个」
     * @param max 单写一个 `0` 时要取走的条数，默认 200
     */
    fun getRange(
        ctx: StripContext,
        defaultOffset: Int = 0,
        defaultLimit: Int = 1,
        multiple: Boolean = false,
        max: Int = 200,
    ): Pair<Int, Int> {
        val range = parseRange(ctx[ExtractType.RANGE].lastOrNull())
            ?: return defaultOffset to defaultLimit

        val start = range.first
        val end = range.second?.takeIf { it > 0 }

        // 只有一个 0：意思是「全部取走」
        if (end == null && start <= 0) return 0 to max

        // 单数字：multiple 决定它是「第 N 个」还是「从默认起点取 N 条」
        if (end == null) {
            val count = start.coerceAtLeast(1)

            return if (multiple) defaultOffset to count else count - 1 to 1
        }

        val from = start.coerceAtLeast(1)

        return if (end <= from) {
            end - 1 to from - end + 1
        } else {
            from - 1 to end - from + 1
        }
    }

    /**
     * 把范围 token 拆成起止数字，规则和 [InstructionUtil] 里的 parseRange 一致：
     * 去掉 `#` / `＃`，按横线（含全角、波浪号）切分，只认能转成整数的部分。
     *
     * `3-5` 是 `3 to 5`，`30` 是 `30 to null`，`-5` 也按 `5 to null` 处理。
     *
     * @return 起点和终点（终点可能没有）；连起点都认不出来时返回 null
     */
    private fun parseRange(text: String?): Pair<Int, Int?>? {
        if (text.isNullOrBlank()) return null

        val tokens = text
            .removePrefix(CHAR_HASH.toString())
            .removePrefix(CHAR_HASH_FULL.toString())
            .trim()
            .split(REGEX_HYPHEN)
            .dropWhile { it.isEmpty() }
            .map { it.trim().toIntOrNull() }

        val start = tokens.getOrNull(0) ?: return null

        return start to tokens.getOrNull(1)
    }

    /** `uid=123` 是外部直接指定的玩家，值可能是纯数字，也可能是带前缀的原文。 */
    private fun getContextUserID(ctx: StripContext): Long? {
        return ctx[ExtractType.USER_ID].firstNotNullOfOrNull(::parseUserID)
    }

    /** `qq=123456` 里存的是 QQ 号，需要再换成绑定的 osu! ID。 */
    private fun getContextQQ(ctx: StripContext): Long? {
        return ctx[ExtractType.QQ].firstNotNullOfOrNull(::parseUserID)
    }

    private fun parseUserID(value: String): Long? {
        val raw = value.trim()

        raw.toLongOrNull()?.takeIf { it > 0 }?.let { return it }

        raw.substringAfter('=', raw).trim().toLongOrNull()?.takeIf { it > 0 }?.let { return it }

        return USER_ID_REGEX.find(raw)?.value?.toLongOrNull()?.takeIf { it > 0 }
    }

    /** 把 QQ 换成绑定里的 osu! ID。没有绑定、或数据库查询失败都返回 null。 */
    private fun lookupBindID(qq: Long, type: BanchoType = BanchoType.OFFICIAL): Long? {
        if (qq == 0L) return null

        return try {
            if (type == BanchoType.PPYSB) {
                bindDao.getSBBindFromQQOrNull(qq)?.userID?.takeIf { it > 0 }
            } else {
                bindDao.getBindFromQQOrNull(qq)?.userID?.takeIf { it > 0 }
            }
        } catch (e: Exception) {
            log.debug("查询 QQ {} 的绑定失败：{}", qq, e.message)

            null
        }
    }

    private fun notFound(onNull: (Boolean) -> Unit, boolean: Boolean): Long? {
        onNull(boolean)

        return null
    }

    /**
     * 从文本里找出一个范围 token 补进 [ctx]。
     *
     * 有 at 时范围一般写在名字的位置上，所以不做「必须在首尾」的限制，
     * 而是从后往前找第一个长得像范围的 token。
     */
    private fun extractRangeToken(ctx: StripContext, text: String) {
        if (text.isEmpty()) return

        val range = text.split(REGEX_SPACE_MORE)
            .lastOrNull { RANGE_TOKEN_REGEX.matches(it) }
            ?: return

        addRange(ctx, range)
    }

    /**
     * 把文本切成「名字 + 首尾范围」。
     *
     * 结尾优先于开头：`osu happy 2` 会先得到 `osu happy` + `2`，
     * 而不是在 `osu` 上误判（`osu` 本身不匹配范围）。
     *
     * @return 名字和范围；文本没有首尾范围 token 时返回 null
     */
    private fun splitNameWithRange(text: String): Pair<String, String>? {
        val tokens = text.split(REGEX_SPACE_MORE).filter { it.isNotEmpty() }

        if (tokens.size < 2) return null

        val tail = tokens.last()

        if (RANGE_TOKEN_REGEX.matches(tail)) {
            return tokens.dropLast(1).joinToString(" ") to tail
        }

        val head = tokens.first()

        if (RANGE_TOKEN_REGEX.matches(head)) {
            return tokens.drop(1).joinToString(" ") to head
        }

        return null
    }

    /** 删除 trimmed 后整段都包含在 [name] 里的残余片段（用于整段拆出名字后清理 residual）。 */
    private fun removeSegmentsContainedIn(ctx: StripContext, name: String) {
        for (segment in ctx.segments) {
            val value = segment.text.trim()

            if (value.isNotEmpty() && name.contains(value)) {
                ctx.removeSegment(segment)
            }
        }
    }

    /**
     * 按顺序尝试候选名字，命中即返回；列表为空时直接返回 null。
     *
     * 命中后会立刻把对应的残余片段从 [ctx] 移除：该段已经被当作玩家名消费掉，
     * 如果继续留在 residual 里，后面的 [addResidualAliases] 会把它误当成别名。
     */
    private fun lookupFirstUser(
        ctx: StripContext,
        candidates: List<Pair<ResidualSegment, String>>,
        type: BanchoType,
    ): Long? {
        for ((segment, name) in candidates) {
            lookupUser(name, type)?.let {
                ctx.removeSegment(segment)

                return it
            }
        }

        return null
    }

    /** 本地缓存 -> 远端 API。查不到、或远端报错都返回 null。 */
    private fun lookupUser(name: String, type: BanchoType = BanchoType.OFFICIAL): Long? {
        if (name.isBlank()) return null

        try {
            if (type == BanchoType.PPYSB) {
                bindDao.getSBUserID(name)?.let { return it }
            } else {
                bindDao.getOsuID(name)?.let { return it }
            }
        } catch (e: Exception) {
            log.debug("本地查询玩家 {} 失败：{}", name, e.message)
        }

        return try {
            if (type == BanchoType.PPYSB) {
                sbUserApiService.getUserID(name)
            } else {
                userApiService.getOsuID(name)
            }
        } catch (e: Exception) {
            log.debug("远程查询玩家 {} 失败：{}", name, e.message)

            null
        }
    }

    private fun addRange(ctx: StripContext, range: String) {
        ctx[ExtractType.RANGE] = range.removePrefix(CHAR_HASH.toString())
            .removePrefix(CHAR_HASH_FULL.toString())
            .trim()
    }

    private companion object {
        /**
         * 一个完整的范围 token，可以是 `2`、`3-5`、`#3-5`，也可以带全角横线。
         *
         * 这里直接复用 [PATTERN_RANGE]，和旧指令里的范围语义保持一致（最大 200）。
         */
        private val RANGE_TOKEN_REGEX = Regex("^[#＃]?$PATTERN_RANGE$")

        /** 从 `uid=123` 这类原文里兜底抠出数字。 */
        private val USER_ID_REGEX = Regex("\\d+")
    }
}
