package com.now.nowbot.util

import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.GroupDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.isNotDefaultOrNull
import com.now.nowbot.model.enums.OsuMode.Companion.toOsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.service.sbApiService.SBUserApiService
import com.now.nowbot.util.command.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 把 [com.now.nowbot.util.command.CommandExtractor] 剥离后的 [StripContext] 补成一个可直接使用的上下文。
 *
 * 只负责「脚手架」性质的两件事：补模式、补玩家。
 * 具体的业务判断（范围怎么用、模式是否合法）留给组件。
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
     * 取得本次指令最终生效的模式。
     *
     * 优先使用 [StripContext] 里已经提取出的模式；没提取到（或提取出的值认不出来）时，
     * 回退到 [GroupDao.getGroupMode]，并在回退成功时把群模式补写回 [ctx]，
     * 让后面直接读 `ctx[ExtractType.MODE]` 的组件也能看到同一份结果。
     *
     * @param event 可以为空；为空时 [GroupDao.getGroupMode] 会返回 [OsuMode.DEFAULT]
     * @return 最终生效的模式，都没有时为 [OsuMode.DEFAULT]
     */
    fun getMode(ctx: StripContext, event: MessageEvent? = null): OsuMode {
        val extracted = ctx[ExtractType.MODE]
            .filter { it.isNotBlank() }
            .map { it.toOsuMode() }
            .firstOrNull { it.isNotDefault }

        if (extracted.isNotDefaultOrNull()) {
            return extracted
        }

        val groupMode = groupDao.getGroupMode(event)

        if (groupMode.isNotDefaultOrNull()) {
            ctx[ExtractType.MODE] = groupMode.shortName
        }

        return groupMode
    }

    enum class BanchoType {
        OFFICIAL, PPYSB
    }


    /**
     * 解析本次指令指向的玩家，返回 osu! ID。
     *
     * 优先级：
     * 1. `ctx[ExtractType.USER_ID]`（`uid=123`）：直接采用，并且和 at 一样不再把剩余文本
     *    当名字，但仍会尝试从剩余文本里补一个范围；
     * 2. `event.hasAt()`：把被 at 的 QQ 从 [BindDao] 的绑定里换成 osu! ID；
     * 3. 剩余文本是「名字」或「名字 + 范围」，也处理「玩家名和范围重名」的歧义
     *    （例如 `osu happy 2` 里的 `2` 既可能是名的一部分，也可能是范围）；
     * 4. 没有给名字（文本为空，或只剩一个范围）：使用发送者自己的绑定。
     *
     * 名字与范围的歧义规则：
     * - extractor 已经拿到 range 时，剩下的整段文本就是名字；
     * - 没有 range 时，文本整体是范围则把玩家当自己；
     * - 否则尝试把开头 / 结尾的范围 token 拆出去，**只有名字部分真的能查到**才承认它是范围；
     * - 名字拆不出来或查不到，就把整段文本当名字查。
     *
     * 名字查找先走 [BindDao] 的本地缓存，再走 [OsuUserApiService]；
     * 远端异常在这里吞掉并记日志，不让查名字失败影响指令本身。
     *
     * 本方法不会改写 [StripContext.text]，只会按需补写 `RANGE`。
     *
     * @param onNull 最终没有解析到玩家时调用（包含 at 的用户没有绑定、名字查不到、
     *               自己也没有绑定的情况）。默认什么都不做；调用方可以传 `{ throw ... }`
     *               在没解析到时直接抛异常。
     * @return 解析到的 osu! ID；`onNull` 没有抛异常时为 null。
     */
    fun getUserID(
        ctx: StripContext,
        event: MessageEvent,
        onNull: () -> Unit = {},
        type: BanchoType = BanchoType.OFFICIAL
    ): Long? {
        val text = ctx.text.trim()
        val hasRange = ctx.exists(ExtractType.RANGE)

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

            return lookupBindID(event.target, type) ?: notFound(onNull)
        }

        // 2. 没有名字：文本为空，或 extractor 拿走了整段范围
        if (text.isEmpty()) {
            return lookupBindID(event.sender.contactID, type) ?: notFound(onNull)
        }

        // 3. 范围已经明确，剩下的就是名字
        if (hasRange) {
            return lookupUser(text, type) ?: notFound(onNull)
        }

        // 4. 整段就是一个范围，玩家是自己
        if (RANGE_TOKEN_REGEX.matches(text)) {
            addRange(ctx, text)

            return lookupBindID(event.sender.contactID, type) ?: notFound(onNull)
        }

        // 5. 尝试把开头 / 结尾的范围拆出去，名字能查到才认
        val split = splitNameWithRange(text)

        if (split != null) {
            val userID = lookupUser(split.first, type)

            if (userID != null) {
                addRange(ctx, split.second)

                return userID
            }
        }

        return lookupUser(text, type) ?: notFound(onNull)
    }

    /** `uid=123` 是外部直接指定的玩家，值可能是纯数字，也可能是带前缀的原文。 */
    private fun getContextUserID(ctx: StripContext): Long? {
        return ctx[ExtractType.USER_ID].firstNotNullOfOrNull(::parseUserID)
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

    private fun notFound(onNull: () -> Unit): Long? {
        onNull()

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
