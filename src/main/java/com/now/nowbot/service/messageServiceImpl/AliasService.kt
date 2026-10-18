package com.now.nowbot.service.messageServiceImpl

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.now.nowbot.controller.TokenBucketRateLimiter
import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.dao.AliasDao
import com.now.nowbot.dao.BeatmapDao
import com.now.nowbot.dao.PendingAlias
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.FileMessage
import com.now.nowbot.restrict.RestrictUtils.isNotSuperAdmin
import com.now.nowbot.restrict.RestrictUtils.isSuperAdmin
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.PassiveMessageHandler
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.TipsRuntimeException
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.AsyncMessageUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.JacksonUtil
import com.now.nowbot.util.StringUtil.formatted
import com.now.nowbot.util.StringUtil.removeSongSuffix
import com.now.nowbot.util.command.FLAG_DATA
import com.now.nowbot.util.command.FLAG_FULL
import com.now.nowbot.util.command.PATTERN_EQUAL
import com.now.nowbot.util.command.PATTERN_HYPHEN
import com.now.nowbot.util.exchangeToBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Matcher
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Service("ALIAS")
class AliasService(
    @field:Qualifier("restClient")
    private val restClient: RestClient,
    private val beatmapApiService: OsuBeatmapApiService,
    private val beatmapDao: BeatmapDao,

    private val aliasDao: AliasDao,
): MessageService<AliasService.AliasParam>, PassiveMessageHandler {

    /**
     * 以 event.subject.contactID 为键的 !am 会话。
     *
     * 用 Caffeine 的 expireAfterWrite 自动清理超时会话，不需要额外定时任务。
     */
    private val manualSessions: Cache<Long, ManualSession> = Caffeine.newBuilder()
        .expireAfterWrite(MANUAL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .maximumSize(MANUAL_SESSION_MAX_SIZE)
        .build()

    /**
     * 每个联系人最近一次 !am 请求的编号。
     *
     * 新 !am 一发起就更新编号并清掉旧会话；谱面查询是异步完成的，
     * 编号能保证先发起但后返回的旧请求不会把已经被替换的会话重新写回去。
     */
    private val manualRequestIDs = ConcurrentHashMap<Long, Long>()

    private val manualRequestSequence = AtomicLong()

    /**
     * 一组「标题 -> 别名」参数。
     *
     * 删除时 alias 可以为 null，表示该标题下的所有别名。
     */
    data class AliasTarget(
        val title: String?,
        val beatmapID: Long?,
        val alias: String?,
    )

    data class AliasParam(
        val targets: List<AliasTarget>,
        val operation: AliasOperation,
        /** 添加/删除时因超过 [ALIAS_BATCH_SIZE] 而被抛弃的条目数量。 */
        val discarded: Int = 0,
        /** 是否跳过二次确认直接执行（:f + 管理员，添加/删除通用）。 */
        val force: Boolean = false,
        /** 第一条被抛弃的条目，用于提示用户抛弃是从哪一条开始的。 */
        val discardedFrom: AliasTarget? = null,
        /** !ai 直接给出的文件地址（命令行测试用，或用户直接贴链接）。 */
        val location: String? = null,
        /** !am 后面直接给出的别名（非纯数字），保存到对应会话的谱面上。 */
        val manualAlias: String? = null,
        /** !aw 后面直接给出的审核编号（支持 1,2,5-10 这样的闭区间）。 */
        val reviewSelection: String? = null,
    )

    enum class AliasOperation {
        ADD, DELETE, REVIEW, EXPORT, IMPORT, MANUAL, MANUAL_ALIAS
    }

    /**
     * 一次 !am 手动补录会话。
     *
     * 以 event.subject.contactID 为键，所以同一个群/私聊里只有一个进行中的会话；
     * 再次发送 !am <BID> 会直接用新谱面覆盖旧会话。
     */
    private data class ManualSession(
        val beatmapID: Long,
        val title: String,
        /** 发起 !am 的用户。直接回复别名时只认这位用户，避免把群友的正常聊天当成别名。 */
        val requester: Long,
        /** 发起 !am 时的权限状态。管理员发起的补录直接生效，普通用户发起的需要审核。 */
        val approved: Boolean,
        /** 会话过期时间，使用 MANUAL_TIMEOUT_SECONDS 计算。 */
        val expireAt: Long,
    )

    private data class DeleteTarget(
        val title: String,
        val alias: String?,
    )

    /**
     * 解析 <标题或BID> = <别名>，多组之间用或换行分隔。
     * 冒号不作为分隔符，它是 :f 开关的语法。
     *
     * 整段参数拆开后，奇数位（第 1、3、5……个）视为标题，偶数位视为别名。
     * 纯数字视为 BID，其余视为标题。
     *
     * 用成对引号包裹的内容不参与拆分，引号本身也不会保留。
     * 例如输入  "A, B" = "C|D"  时会拆成标题 A, B 和别名 C|D。
     *
     * 数量为奇数说明最后一个标题漏写了别名，会提示用户。
     * 删除时允许只输入一个标题（别名留空），此时 [delete] 会先列出该标题下的所有别名，
     * 再让用户用位置（从 1 开始、逗号分隔）挑选要删除的几项，输入 0 删除全部。
     *
     * 添加/删除时一次最多保留 [ALIAS_BATCH_SIZE] 条，超出的部分直接抛弃。
     * 添加时管理员不受该限制，只有非管理员才会走抛弃；
     * 删除时超级管理员输入带 :f 捕获组（如 !ad:f 123=abc）时不受该限制。
     *
     * :f 捕获组表示跳过二次确认直接执行（添加、删除同理），仅管理员生效。
     *
     * @param unlimited 为真时不限制条目数量。
     * @param force 为真时表示跳过二次确认直接执行。
     */
    private fun parseAliasParam(
        matcher: Matcher,
        operation: AliasOperation,
        unlimited: Boolean = false,
        force: Boolean = false,
    ): AliasParam? {
        val tokens = matcher.group("data")
            ?.let { splitTargets(it) }
            .orEmpty()

        if (tokens.isEmpty()) {
            return null
        }

        // 删除：只给一个标题时先留空别名，具体删哪几条交给 delete 让用户挑选
        if (operation == AliasOperation.DELETE && tokens.size == 1) {
            return AliasParam(listOf(parseAliasTarget(tokens.first(), null)), operation, force = force)
        }

        // 标题和别名必须成对，奇数个说明漏写了别名
        if (tokens.size % 2 != 0) {
            throw IllegalArgumentException.WrongException.AliasPair()
        }

        val targets = tokens.chunked(2).map { (title, alias) -> parseAliasTarget(title, alias) }

        // 添加/删除：一次最多只处理 ALIAS_BATCH_SIZE 条，超出的主动抛弃
        if (!unlimited && targets.size > ALIAS_BATCH_SIZE) {
            return AliasParam(
                targets = targets.take(ALIAS_BATCH_SIZE),
                operation = operation,
                discarded = targets.size - ALIAS_BATCH_SIZE,
                force = force,
                // 被抛弃的第一条，提示用户从这里开始
                discardedFrom = targets[ALIAS_BATCH_SIZE],
            )
        }

        return AliasParam(targets, operation, force = force)
    }

    /**
     * 按 [TARGET_SEPARATOR] 把整段参数拆成标题/别名序列。
     *
     * 被成对引号（[QUOTATION]）包裹的内容一律视为普通字符，其中的分隔符不会被拆开，
     * 引号本身只作为保护标记，不会出现在结果里。
     *
     * 引号没有成对出现时，说明用户想保护的范围不明确，提示用户重新输入。
     *
     * @return 去掉首尾空白后的非空片段。
     */
    private fun splitTargets(data: String): List<String> {
        val tokens = mutableListOf<String>()
        val token = StringBuilder()
        var quoted = false

        for (c in data) {
            when {
                c in QUOTATION -> quoted = !quoted
                quoted -> token.append(c)
                isTargetSeparator(c) -> {
                    tokens += token.toString()
                    token.clear()
                }

                else -> token.append(c)
            }
        }

        if (quoted) {
            throw IllegalArgumentException.WrongException.Quotation()
        }

        tokens += token.toString()

        return tokens.map { it.trim() }.filter { it.isNotEmpty() }
    }

    /**
     * 单个字符是否是分隔符。所有分隔符都是单字符，所以可以直接逐个字符判断。
     */
    private fun isTargetSeparator(c: Char): Boolean = TARGET_SEPARATOR.matches(c.toString())

    /**
     * 纯数字视为 BID，其余视为标题。
     */
    private fun parseAliasTarget(title: String, alias: String?): AliasTarget {
        val bid = title.toLongOrNull()

        return if (bid != null) {
            AliasTarget(null, bid, alias)
        } else {
            AliasTarget(title.ifBlank { null }, null, alias)
        }
    }

    /**
     * 取出标准化的标题。没有直接给标题时，通过 BID 查询谱面集标题。
     */private fun getStandardizedTitle(target: AliasTarget): String {
        // 1. 优先检查 target.title。如果存在且不为空白，直接使用（此时不需要 beatmapID）
        val title = target.title?.takeIf { it.isNotBlank() }
            ?: run {
                // 2. 只有当 target.title 不可用时，才需要 beatmapID
                val beatmapID = target.beatmapID ?: throw IllegalArgumentException.WrongException.BeatmapID()

                // 3. 从 Dao 获取并过滤 isBlank，如果为空则请求 Api
                beatmapDao.getBeatmapTitleFromExtend(beatmapID)?.takeIf { it.isNotBlank() }
                    ?: try {
                        beatmapApiService.getBeatmap(beatmapID).beatmapset?.title
                    } catch (_: Exception) {
                        throw NoSuchElementException.Beatmap(beatmapID)
                    }
            }

        // 4. 如果最终获取到的标题为空或者全空白，抛出 TipsException
        if (title.isNullOrBlank()) {
            throw IllegalArgumentException.WrongException.BeatmapID()
        }

        return title.removeSongSuffix().formatted()
    }

    /**
     * 取出标准化的别名。别名缺失或为空时提示用户。
     */
    private fun getStandardizedAlias(target: AliasTarget): String {
        return target.alias
            ?.takeIf { it.isNotBlank() }
            ?.removeSongSuffix()
            ?: throw IllegalArgumentException.WrongException.Alias(target.alias)
    }

    /**
     * 渲染第一条被抛弃的条目，拼成「（从 标题 -> 别名 开始）」。
     *
     * 别名和删除时一样只做 removeSongSuffix + trim，不再做 standardised。
     */
    private fun formatDiscardedFrom(target: AliasTarget?): String {
        if (target == null) return ""

        val title = getStandardizedTitle(target)
        val alias = target.alias
            ?.takeIf { it.isNotBlank() }
            ?.removeSongSuffix()

        val entry = if (alias == null) "$title -> 全部别名" else "$title -> $alias"

        return "（从 $entry 开始）"
    }

    /**
     * 添加别名。
     *
     * 管理员（含超级管理员）添加的别名默认通过审核，普通用户添加的需要等待审核。
     *
     * 多组参数会合并成一条确认消息；回复 OK 才会保存，回复其他内容撤回本次操作；超时同样会默认保存。
     *
     * 管理员输入带 :f 捕获组（如 !aa:f 123=abc）时跳过二次确认，直接保存。
     */
    private fun check(event: MessageEvent, param: AliasParam) {
        // 同一标题下的同一个别名只允许写一次。
        // 用户可能在同一句里重复输入（!aa A=x, A=x），也可能用 BID 和标题两种写法
        // 指向同一个谱面集（!aa 123=x, A=x），解析后都会变成完全相同的「标题 -> 别名」。
        // 这里先去重，保证数据库不会收到两次一样的别名。
        val targets = param.targets.map { target ->
            getStandardizedTitle(target) to getStandardizedAlias(target)
        }.distinct()

        // 添加处甄别是否是管理员，是则默认通过审核
        val approved = event.isSuperAdmin()

        // :f + 管理员：不确认，直接添加
        if (param.force) {
            targets.forEach { (title, alias) ->
                aliasDao.addAliasToTitle(
                    title,
                    alias,
                    setAsPrimary = false,
                    approved = approved,
                    from = event.sender.contactID
                )
            }
            event.replyAsync(if (approved) "保存成功！" else "已提交审核，请等待管理员审核。")
            return
        }

        val preview = buildString {
            appendLine("添加别名：")
            targets.forEach { (title, alias) -> appendLine("$title -> $alias") }

            if (param.discarded > 0) {
                val from = formatDiscardedFrom(param.discardedFrom)
                appendLine("一次最多只能添加 $ALIAS_BATCH_SIZE 条，已主动抛弃后面的 ${param.discarded} 条$from。")
            }

            if (!approved) {
                appendLine("这些别名需要管理员审核之后才会通过。")
            }

            append("回复 OK 确认本次操作，回复其他内容则撤回本次操作。")
        }

        AsyncMessageUtil.doubleCheck(
            event = event,
            keywords = listOf("OK"),
            onCheck = { event.reply(preview) },
            onOverTime = {
                targets.forEach { (title, alias) ->
                    aliasDao.addAliasToTitle(
                        title,
                        alias,
                        setAsPrimary = false,
                        approved = approved,
                        from = event.sender.contactID
                    )
                }
                event.replyAsync(if (approved) "操作超时，已默认保存。" else "操作超时，已提交审核，请等待管理员审核。")
            },
            onFailure = { throw TipsRuntimeException("已撤回操作。") },
            onSuccess = { ev ->
                targets.forEach { (title, alias) ->
                    aliasDao.addAliasToTitle(
                        title,
                        alias,
                        setAsPrimary = false,
                        approved = approved,
                        from = event.sender.contactID
                    )
                }
                ev.replyAsync(if (approved) "保存成功！" else "已提交审核，请等待管理员审核。")
            },
            timeout = 60.seconds
        )
    }

    /**
     * 手动补录别名（!am <BID>）。
     *
     * 按 BID 找到谱面并展示它的 title、titleUnicode、artist，然后把这次补录登记成
     * 一个以 event.subject.contactID 为键、60 秒有效的会话。
     *
     * 之后有两种方式提交别名：
     * - 再次发送「!am 别名」：由 [manualAlias] 处理，不会再请求一次谱面信息；
     * - 直接回复别名（不带 !am 前缀）：由 [handlePassiveMessage] 处理。
     *
     * 别名不能是纯数字：!am 后面的纯数字一律视为 BID，会开启新的补录会话。
     * 回复 OK 或超时视为取消。挂载标题和 [check] 一致：
     * beatmapset.title 经 removeSongSuffix + formatted 标准化。
     * 权限也和 [check] 一致：管理员直接生效，普通用户提交后需要审核。
     */
    private fun manual(event: MessageEvent, param: AliasParam) {
        val target = param.targets.firstOrNull() ?: return
        val beatmapID = target.beatmapID ?: throw IllegalArgumentException.WrongException.BeatmapID()

        val key = event.subject.contactID
        val requestID = manualRequestSequence.incrementAndGet()

        // 新的 !am 一发起，旧的补录会话立即失效。
        // 即使这次查询谱面失败，也不会再回退到上一个未完成的会话。
        manualRequestIDs[key] = requestID
        manualSessions.invalidate(key)

        val beatmapset = runCatching {
            beatmapApiService.getBeatmap(beatmapID).beatmapset
        }.getOrNull() ?: throw NoSuchElementException.Beatmap(beatmapID)

        // 挂载标题和 !aa 保持一致：去掉歌曲后缀再 formatted 标准化
        val title = beatmapset.title.removeSongSuffix().formatted()
        if (title.isBlank()) throw IllegalArgumentException.WrongException.BeatmapID()

        // 管理员可以看到全部别名（含待审核）；普通用户只能看到已审核别名，
        // 并额外提示还有多少条在等待审核。
        val isAdmin = event.isSuperAdmin()
        val aliases = if (isAdmin) {
            aliasDao.getAliasesByTitleIncludingUnapproved(title)
        } else {
            // 按标准流程（和 applyAlias 一致）查询该标题当前已审核的别名
            aliasDao.getAliasesByNormalizedTitles(listOf(title))[title].orEmpty()
        }
        val unapprovedCount = if (isAdmin) 0 else aliasDao.countUnapprovedAliasesByTitle(title)

        val aliasText = if (aliases.isEmpty()) "（空）" else aliases.joinToString("、")
        val unapprovedHint = if (!isAdmin && unapprovedCount > 0) {
            "（有${unapprovedCount}个未通过审核的别名）"
        } else {
            ""
        }

        val preview = buildString {
            appendLine("谱面信息：")
            appendLine("标准化标题：${title}")
            appendLine("标题：${beatmapset.title}")
            appendLine("罗马字标题：${beatmapset.titleUnicode}")
            appendLine("艺术家：${beatmapset.artist}")
            appendLine()
            appendLine("将保存到别名库的标题：$title")
            appendLine("现有别名：$aliasText$unapprovedHint")
            appendLine("发送「!am 别名」或直接回复别名，即可将其保存为该标题的别名。")
            appendLine("「!am 别名」里的别名不能是纯数字（纯数字会被当成 BID）。")
            appendLine("回复 OK 取消本次操作。")
            append("60 秒后不操作同样视为取消。")
        }

        val session = ManualSession(
            beatmapID = beatmapID,
            title = title,
            requester = event.sender.contactID,
            approved = isAdmin,
            expireAt = System.currentTimeMillis() + MANUAL_TIMEOUT_SECONDS * 1_000,
        )

        // 只有仍然是最新一次 !am 请求时，才允许把会话写回去。
        // 先发起但后返回的旧请求不会覆盖后来的会话。
        manualSessions.asMap().compute(key) { _, current ->
            if (manualRequestIDs[key] == requestID) session else current
        }

        // 已经被更新的 !am 替换掉的请求不再发送谱面预览
        if (manualSessions.getIfPresent(key) !== session) return

        event.replyAsync(preview)
    }

    /**
     * 处理「!am 别名」：把别名保存到当前会话对应的谱面上。
     *
     * 这里不会重新请求谱面信息，也不会开启新的补录会话。
     * 没有会话时只提示用户先发送 !am <BID>。
     */
    private fun manualAlias(event: MessageEvent, rawAlias: String?) {
        val session = currentManualSession(event)

        if (session == null) {
            event.replyAsync("当前没有正在进行的 !am 会话，请先发送「!am <BID>」查询谱面。")
            return
        }

        val alias = rawAlias?.trim()?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException.WrongException.Alias(rawAlias)

        // !am 后面的纯数字已经在 isHandle 里当成 BID 了，这里再防一手
        if (alias.toLongOrNull() != null) {
            throw IllegalArgumentException.WrongException.Alias(alias)
        }

        saveManualAlias(event, session, alias)
    }

    /**
     * 不是指令的普通消息：如果当前会话正在等待别名，就把这条消息当成别名。
     *
     * 只接受发起 !am 的用户，避免同群的其他人正常聊天时误保存。
     * 回复 OK 取消；没有会话或不是发起人时返回 false，消息继续按普通聊天处理。
     */
    override fun handlePassiveMessage(event: MessageEvent, messageText: String): Boolean {
        val session = currentManualSession(event) ?: return false

        if (session.requester != event.sender.contactID) return false

        val text = messageText.trim()
        if (text.isEmpty()) return false

        // 和原来的 doubleCancel 保持一致：回复里包含 OK 就取消
        if (text.contains("OK", ignoreCase = true)) {
            manualSessions.asMap().remove(event.subject.contactID, session)
            event.replyAsync("已取消操作。")
            return true
        }

        saveManualAlias(event, session, text)
        return true
    }

    /**
     * 取出当前联系人的 !am 会话。
     *
     * Caffeine 负责自动清理，这里再用 expireAt 显式判断一次，避免临界时间点上拿到刚过期的会话。
     */
    private fun currentManualSession(event: MessageEvent): ManualSession? {
        val key = event.subject.contactID
        val session = manualSessions.getIfPresent(key) ?: return null

        if (session.expireAt <= System.currentTimeMillis()) {
            manualSessions.asMap().remove(key, session)
            return null
        }

        return session
    }

    /**
     * 保存会话对应的别名。
     *
     * 先用 remove(key, session) 抢占会话：并发的「!am 别名」和直接回复里只有一个能成功，
     * 也避免旧会话被新 !am 请求覆盖之后还把别名写到旧谱面上。
     */
    private fun saveManualAlias(event: MessageEvent, session: ManualSession, rawAlias: String) {
        val alias = rawAlias.removeSongSuffix().takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException.WrongException.Alias(rawAlias)

        if (!manualSessions.asMap().remove(event.subject.contactID, session)) {
            event.replyAsync("本次 !am 会话已经结束，请重新发送「!am <BID>」。")
            return
        }

        val approved = session.approved

        aliasDao.addAliasToTitle(
            session.title,
            alias,
            setAsPrimary = false,
            approved = approved,
            from = event.sender.contactID,
        )

        event.replyAsync(
            if (approved) {
                "保存成功！已将「$alias」保存为「${session.title}」的别名。"
            } else {
                "已提交审核！已将「$alias」提交为「${session.title}」的别名，请等待管理员审核。"
            }
        )
    }

    /**
     * 删除别名。只有超级管理员才能调用。
     *
     * 给出明确的「标题 = 别名」时合并成一条确认消息，回复 OK 才删除。
     *
     * 只给一个标题时不会直接删掉全部：先列出该标题下的所有别名，再由用户回复位置挑选，
     * 详细规则见 [deleteOneTitle]。
     *
     * 超级管理员输入带 :f 捕获组（如 !ad:f 123=abc）时跳过二次确认，直接删除选定的所有条目；
     * 只给标题时（!ad:f 123）则跳过挑选，直接删除该标题下的全部别名。
     */
    private fun delete(event: MessageEvent, param: AliasParam) {
        // 和添加同理：去掉重复的「标题 -> 别名」，避免同一条被删两次、也避免结果里重复计数。
        val targets = param.targets.map { target ->
            val alias = target.alias
                ?.takeIf { it.isNotBlank() }
                ?.removeSongSuffix()

            DeleteTarget(getStandardizedTitle(target), alias)
        }.distinct()

        // 只给了标题（没有指定别名）：列出该标题下的别名，让用户自己挑要删哪几条
        if (targets.size == 1 && targets.single().alias == null) {
            deleteOneTitle(event, targets.single().title, param.force)
            return
        }

        // :f + 超级管理员：不确认，直接删除选定的所有条目
        if (param.force) {
            event.replyAsync(deleteTargets(targets))
            return
        }

        val preview = buildString {
            appendLine("删除别名：")
            targets.forEach { target ->
                if (target.alias == null) {
                    appendLine("${target.title} -> 全部别名")
                } else {
                    appendLine("${target.title} -> ${target.alias}")
                }
            }

            if (param.discarded > 0) {
                val from = formatDiscardedFrom(param.discardedFrom)
                appendLine("一次最多只能删除 $ALIAS_BATCH_SIZE 条，已主动抛弃后面的 ${param.discarded} 条$from。")
            }
        }

        AsyncMessageUtil.doubleCheck(
            event = event,
            keywords = listOf("OK"),
            onCheck = { event.reply(preview) },
            onSuccess = { ev -> ev.replyAsync(deleteTargets(targets)) },
            timeout = 60.seconds
        )
    }

    /**
     * 单标题删除：先把该标题下的别名连位置一起列出来，再等用户回复要删除的位置。
     *
     * 回复里只允许出现数字和逗号（半角逗号、中文逗号、顿号、空白）：
     * - 输入 1 开始的自然数：删除对应位置的别名，可以用逗号一次挑多个
     * - 输入 0：删除该标题下的全部别名
     * - 输入数字和逗号之外的内容、空回复：放弃本次操作
     * - 超时：同样视为放弃
     *
     * 超出范围的位置会被无视；挑中的位置全都超出范围时也算放弃。
     *
     * [force] 为真（:f）时不询问，直接删除该标题下的全部别名。
     */
    private fun deleteOneTitle(event: MessageEvent, title: String, force: Boolean) {
        // :f 沿用原来的强制语义：不询问，直接删掉这个标题下的全部别名
        if (force) {
            event.replyAsync(deleteTargets(listOf(DeleteTarget(title, null))))
            return
        }

        // 删除是超级管理员专属操作，这里必须列出全部别名（含待审核），
        // 不能复用面向普通用户的「只返回已审核别名」视图。
        val aliases = aliasDao.getAliasesByTitleIncludingUnapproved(title)

        if (aliases.isEmpty()) {
            event.replyAsync("「$title」下没有别名。")
            return
        }

        AsyncMessageUtil.doubleCheck(
            event = event,
            // 任意回复都交给下面解析，非数字由 parseDeleteSelection 判定为放弃
            keywords = emptyList(),
            onCheck = {
                event.reply(buildString {
                    appendLine("「$title」下的别名：")
                    aliases.forEachIndexed { index, alias -> appendLine("${index + 1}. $alias") }
                    appendLine()
                    append("请回复要删除的别名位置（从 1 开始，可用逗号分隔多个，也可以用 5-8 这样包含头尾的区间），回复 0 删除全部，回复其他内容放弃本次操作。")
                    append("\n60 秒内不回复同样视为放弃。")
                })
            },
            onOverTime = { throw TipsRuntimeException("操作超时，已放弃删除。") },
            onSuccess = { ev ->
                // 和审核一样，编号要从原文里取，textMessage 可能拿不到用户输入的内容
                val selected = parseDeleteSelection(ev.rawMessage, aliases.size)

                when {
                    selected == null -> ev.replyAsync("已放弃操作。")
                    selected.isEmpty() -> ev.replyAsync("没有选中任何别名，已放弃操作。")
                    else -> {
                        val count = aliasDao.deleteAliasesFromTitle(title, selected.map { aliases[it - 1] })
                        ev.replyAsync(if (count > 0) "删除成功！共删除 $count 个别名。" else "没有找到这个别名。")
                    }
                }
            },
            timeout = 60.seconds
        )
    }

    /**
     * 解析「要删除哪些别名」的回复。
     *
     * 编号写法和审核一致（见 [expandSelection]），另外：
     * - 输入 0（和其它位置写在一起也算）：全部
     * - 其余数字按 1 开始的位置理解，超出范围的位置会被无视
     * - 出现无法识别的内容：放弃本次操作
     *
     * @return 要删除的位置（1-based，已去重、升序）；
     * 空列表表示输入合法但位置全都超出范围；返回 null 表示放弃本次操作。
     */
    private fun parseDeleteSelection(raw: String, size: Int): List<Int>? {
        val numbers = expandSelection(raw, size) ?: return null

        if (numbers.isEmpty()) return null

        // 0 表示全部，和别的编号写在一起也按全部处理
        if (numbers.any { it == 0 }) return (1..size).toList()

        return numbers.filter { it in 1..size }.distinct().sorted()
    }

    /**
     * 把「1,2,5-10」这样的编号串展开成具体的编号列表，供 AD 的位置挑选和 AW 的审核共用。
     *
     * 支持：
     * - 单个编号：`1,2,3`，允许负数（负数在审核里表示全部不通过）
     * - 闭区间：`5-10`，头尾都包含；写成 `10-5` 也按 `5-10` 处理
     * - 区间符号和 `PATTERN_HYPHEN` 一致：`-`、`－`、`—`、`~`、`～`、`〜`
     * - 分隔符见 [SELECTION_SEPARATOR]；区间符号两侧的空白会被忽略
     *
     * 区间会截断到 1..[max]（只展示 10 条时 `1-100` 等于 `1-10`，不会真的展开一百个），
     * 单个编号原样保留，越界和 0 的语义交给调用方判断。
     *
     * @return 展开后的编号，保持输入顺序；返回 null 表示输入为空或含有无法识别的内容。
     */
    private fun expandSelection(raw: String, max: Int): List<Int>? {
        val input = raw.trim()

        if (input.isEmpty()) return null

        // 先去掉区间符号两侧的空白，避免「5 - 10」被空白拆成三段
        val normalized = SELECTION_RANGE_SPACE.replace(input) { it.value.trim() }

        val numbers = mutableListOf<Int>()

        for (token in normalized.split(SELECTION_SEPARATOR)) {
            if (token.isEmpty()) continue

            // 单个编号，允许负数
            if (token.matches(SELECTION_NUMBER)) {
                numbers += token.toIntOrNull() ?: return null
                continue
            }

            // 闭区间，头尾都包含
            val range = SELECTION_RANGE.matchEntire(token) ?: return null
            val start = range.groupValues[1].toIntOrNull() ?: return null
            val end = range.groupValues[2].toIntOrNull() ?: return null

            // 写反也按从小到大处理，再截断到本次展示的范围
            val from = maxOf(1, minOf(start, end))
            val to = minOf(max, maxOf(start, end))

            if (from <= to) numbers += from..to
        }

        return numbers
    }

    /**
     * 执行删除，并拼出结果消息。
     */
    private fun deleteTargets(targets: List<DeleteTarget>): String {
        // 单项操作沿用原来的文案
        if (targets.size == 1) {
            val target = targets.first()

            if (target.alias == null) {
                val count = aliasDao.deleteAllAliasesFromTitle(target.title)
                return if (count > 0) "删除成功！共删除 $count 个别名。" else "没有找到这个别名。"
            }

            return if (aliasDao.deleteAliasFromTitle(target.title, target.alias)) "删除成功！" else "没有找到这个别名。"
        }

        var success = 0
        val missing = mutableListOf<String>()

        targets.forEach { target ->
            val deleted = if (target.alias == null) {
                aliasDao.deleteAllAliasesFromTitle(target.title) > 0
            } else {
                aliasDao.deleteAliasFromTitle(target.title, target.alias)
            }

            if (deleted) {
                success++
            } else {
                missing += if (target.alias == null) target.title else "${target.title} -> ${target.alias}"
            }
        }

        return buildString {
            if (success > 0) append("删除成功 $success 项。")
            if (missing.isNotEmpty()) {
                if (success > 0) append('\n')
                append("没有找到：${missing.joinToString("、")}。")
            }
        }
    }

    /**
     * 审核别名。只有管理员才能调用。
     *
     * 一次最多只展示 REVIEW_SIZE 条待审核的别名，剩下的下次审核时再展示。
     *
     * 两种用法：
     * - `!aw`：展示列表后等待管理员回复编号序列；
     * - `!aw 1,2,5-10`：直接按给出的编号处理本次展示的这批别名，不再等回复。
     *
     * 编号规则见 [parseSelection]：0 全部通过、负数全部不通过、
     * 其余编号（含 `5-10` 这种闭区间）对应的通过，没有选中的不通过。
     *
     * 不通过的别名会连同绑定一起删除。
     */
    private fun review(event: MessageEvent, rawSelection: String?) {
        val allPending = aliasDao.getPendingAliases()

        if (allPending.isEmpty()) {
            event.replyAsync("目前没有待审核的别名。")
            return
        }

        // 一次最多只给 REVIEW_SIZE 条，剩下的下次审核时再给
        val pending = allPending.take(REVIEW_SIZE)
        val remaining = allPending.size - pending.size

        // 指令后面直接跟了编号：不用再等回复，按同样的顺序直接处理
        if (!rawSelection.isNullOrBlank()) {
            applySelection(event, pending, remaining, rawSelection)
            return
        }

        AsyncMessageUtil.doubleCheck(
            event = event,
            // 空列表表示任意回复都会进入 onSuccess，由我们自己解析数字序列
            keywords = emptyList(),
            onCheck = {
                event.reply(buildString {
                    if (remaining > 0) {
                        appendLine("待审核的别名（共 ${allPending.size} 条，本次展示前 ${pending.size} 条）：")
                    } else {
                        appendLine("待审核的别名：")
                    }
                    pending.forEachIndexed { index, alias ->
                        appendLine("${index + 1}. ${alias.titles.joinToString(" / ")} -> ${alias.value}（${alias.qq}）")
                    }
                    append("请回复需要通过的编号（用逗号分隔，也可以用 5-10 这样包含头尾的区间），回复 0 本次全部通过，回复负数本次全部不通过，回复其他非数字取消操作。")
                    if (remaining > 0) {
                        append("\n处理完这 ${pending.size} 条后还有 $remaining 条待审核，请再次使用本指令。")
                    }
                })
            },
            onOverTime = { throw TipsRuntimeException("审核超时了。") },
            onSuccess = { ev ->
                // 这里必须用 rawMessage：部分平台 textMessage 是空的，编号只存在于原文里，
                // 用 textMessage 会解析不出任何内容，看起来就像「不识别」。
                applySelection(ev, pending, remaining, ev.rawMessage)
            },
            timeout = 60.seconds
        )
    }

    /**
     * 按编号执行审核：选中的通过，其余的不通过并删除。
     *
     * @param pending 本次展示的这一批待审核别名，[rawSelection] 里的编号就是它的位置。
     * @param remaining 展示范围之外还剩多少条待审核，只用于提示。
     */
    private fun applySelection(
        event: MessageEvent,
        pending: List<PendingAlias>,
        remaining: Int,
        rawSelection: String,
    ) {
        val selected = parseSelection(rawSelection, pending.size)

        if (selected == null) {
            event.replyAsync("已取消审核。回复 0 全部通过，回复负数全部不通过。")
            return
        }

        val approvedIds = selected.map { pending[it - 1].id }
        val rejectedIds = pending.map { it.id }.filterNot { it in approvedIds }

        val (approved, rejected) = aliasDao.reviewAliases(approvedIds, rejectedIds)

        event.replyAsync(buildString {
            append("已通过 $approved 个，不通过并删除 $rejected 个。")
            if (remaining > 0) append("还有 $remaining 条待审核。")
        })
    }

    /**
     * 解析审核编号。
     *
     * 编号写法和 AD 完全一致（见 [expandSelection]），另外：
     * - `0`：本次全部通过
     * - 负数：本次全部不通过
     *
     * 超出本次展示范围的编号会被无视，不会导致其余编号失效。
     *
     * @return 通过审核的编号（1-based，已去重、排序）。
     * 返回空表示全部不通过；返回 null 表示取消操作（输入为空或含有无法识别的内容）。
     */
    private fun parseSelection(raw: String, size: Int): List<Int>? {
        val input = raw.trim()

        // 空回复视为取消操作
        if (input.isEmpty()) return null

        // 全部通过
        if (input == "0") return (1..size).toList()

        val numbers = expandSelection(input, size) ?: return null

        if (numbers.isEmpty()) return null

        // 负数：全部不通过
        if (numbers.all { it < 0 }) return emptyList()

        // 不存在的编号直接无视
        val selected = numbers
            .filter { it in 1..size }
            .distinct()
            .sorted()

        // 回复的编号全都不存在：视为没有选择，取消操作
        if (selected.isEmpty()) return null

        return selected
    }

    /**
     * 导出库里所有「标题 -> 第一个别名」的映射，发给用户一个 JSON 文件。
     *
     * 输出是 JSON 对象：键是标题，值是它排序最靠前的别名（没有别名的标题不会出现在结果里）。
     * 外部 JS 程序可以直接读成 Map / Set 查询：
     * ```js
     * const titleToAlias = new Map(Object.entries(JSON.parse(text)))        // 标题 -> 别名
     * const aliasToTitle = new Map([...titleToAlias].map(([t, a]) => [a, t])) // 别名 -> 标题
     * const titles = new Set(titleToAlias.keys())                            // 标题集合
     * ```
     *
     * 所有用户都能调用；只有超级管理员不受冷却限制，其余用户复用
     * [FetchService.rateLimiter] 的令牌桶，2 分钟一次（独立计数，不占用 !fetch 的次数）。
     */
    private fun export(event: MessageEvent) {
        // 超级管理员既不消耗令牌，也不受冷却限制
        if (event.isNotSuperAdmin()) {
            rateLimiter.checkOrThrow(EXPORT_RATE_KEY)
        }

        val mapping = aliasDao.getTitlePrimaryAliases()
        val file = JacksonUtil.objectToJsonPretty(mapping).toByteArray(Charsets.UTF_8)

        event.replyFileInGroupAsync(file, EXPORT_FILE_NAME)
    }

    /**
     * 导入 !ao 导出的「标题 -> 别名」JSON 映射，仅超级管理员可用。
     *
     * 先提示用户发送文件（也可以回复一条包含文件的消息），再把文件内容按 JSON 解析。
     * 读取失败、JSON 解析失败都会直接回复用户，不会写入数据库。
     *
     * 标题和 !aa 一样做 removeSongSuffix + formatted 标准化；
     * 已存在的绑定会跳过，新绑定追加在该标题的别名列表末尾，并直接通过审核。
     */
    private fun importAliases(event: MessageEvent, param: AliasParam) {
        // 直接在指令后面给了链接或路径（命令行测试、或用户不想上传文件）
        val location = param.location

        if (!location.isNullOrBlank()) {
            importText(event, readImportText(location))
            return
        }

        AsyncMessageUtil.doubleCheck(
            event = event,
            // 任意回复都当作「文件消息」继续处理，具体有没有文件由 importFile 判断
            keywords = emptyList(),
            onCheck = {
                event.reply("""
                    请发送要导入的 JSON 文件（格式与 !ao 导出的结果一致），
                    或回复一条包含该文件的消息。
                    也可以直接回复文件的下载链接或本地路径。
                """.trimIndent())
            },
            onOverTime = { throw TipsRuntimeException("等待文件超时，已取消导入。") },
            onSuccess = { ev -> importFile(ev) },
            timeout = 60.seconds,
        )
    }

    private fun importFile(event: MessageEvent) {
        val file = findImportFile(event)

        if (file != null) {
            importText(event, readImportText(event, file))
            return
        }

        // 命令行测试时没法发文件，允许用户直接回复链接或本地路径
        val location = textLocation(event) ?: throw IllegalArgumentException.WrongException.AliasImportFile()
        importText(event, readImportText(location))
    }

    private fun textLocation(event: MessageEvent): String? {
        val text = normalizeLocation(event.textMessage)
        if (text.isEmpty()) return null

        return when {
            // 远程链接、file:// URL 直接交给 readImportText
            isHttpLocation(text) || text.startsWith("file:", true) -> text

            // 本地路径：即使文件不存在也交给 readImportText，好给出「找不到文件」的提示
            looksLikeLocalPath(text) -> resolveImportPath(text).toAbsolutePath().toString()

            // 只写了不带斜杠的文件名时，去 !ao 的 debug 目录里找一下
            !text.contains('/') && !text.contains('\\') ->
                resolveImportPath(text)
                    .takeIf { Files.isRegularFile(it) }
                    ?.toAbsolutePath()
                    ?.toString()

            else -> null
        }
    }

    private fun isHttpLocation(text: String): Boolean {
        return text.startsWith("http://", true) || text.startsWith("https://", true)
    }

    private fun looksLikeLocalPath(text: String): Boolean {
        return text.startsWith("/") ||
                text.startsWith("~") ||
                text.startsWith("./") ||
                text.startsWith("../") ||
                text.contains('/') ||
                text.contains('\\')
    }

    /**
     * 处理从终端拖拽出来的路径：去掉外层引号，并把 \ 还原成空格。
     */
    private fun normalizeLocation(raw: String): String {
        var value = raw.trim()

        if (value.length >= 2 &&
            ((value.startsWith('"') && value.endsWith('"')) ||
                    (value.startsWith('\'') && value.endsWith('\'')))
        ) {
            value = value.substring(1, value.length - 1).trim()
        }

        return value.replace("\\ ", " ")
    }

    /**
     * 解析本地路径：
     * - 支持 ~ ~/`
     * - 只给文件名（例如 aliasDB.json）时，去 !ao 导出的 debug 目录里找
     */
    private fun resolveImportPath(location: String): Path {
        val value = normalizeLocation(location)
        val expanded = when {
            value == "~" -> System.getProperty("user.home")
            value.startsWith("~/") -> System.getProperty("user.home") + value.substring(1)
            else -> value
        }

        val path = Path.of(expanded)
        if (Files.isRegularFile(path)) return path

        if (!expanded.contains('/') && !expanded.contains('\\')) {
            val debugPath = Path.of(NowbotConfig.RUN_PATH, "debug", expanded)
            if (Files.isRegularFile(debugPath)) return debugPath
        }

        return path
    }

    private fun importText(event: MessageEvent, text: String) {
        val rawMapping = parseImportJson(text)

        // 标题标准化，并丢掉空标题/空别名；同一标题只保留一条
        val mapping = LinkedHashMap<String, String>(rawMapping.size)
        rawMapping.forEach { (rawTitle, rawAlias) ->
            val title = rawTitle.removeSongSuffix().formatted()
            val alias = rawAlias.removeSongSuffix()

            if (title.isNotBlank() && alias.isNotBlank()) {
                mapping[title] = alias
            }
        }

        if (mapping.isEmpty()) {
            event.replyAsync("文件里没有可以导入的别名。")
            return
        }

        val result = aliasDao.importAliases(
            mapping = mapping,
            approved = true,
            from = event.sender.contactID,
        )

        event.replyAsync(buildString {
            append("导入完成！新增 ${result.imported} 条")
            if (result.skipped > 0) append("，跳过已存在 ${result.skipped} 条")
            append("。")
        })
    }

    /**
     * 当前消息带文件就用当前消息的；否则如果它在回复一条消息，就去原消息里找文件。
     */
    private fun findImportFile(event: MessageEvent): FileMessage? {
        event.file?.let { return it }

        return event.repliedMessage
            ?.messageList
            ?.filterIsInstance<FileMessage>()
            ?.firstOrNull()
    }

    /**
     * 读取文件的文本内容。支持 http(s) 链接和本地路径（含 file:// URL）。
     */
    private fun readImportText(event: MessageEvent, file: FileMessage): String {
        val location = file.url?.takeIf { it.isNotBlank() }
            ?: event.bot?.getFileUrl(file, (event.subject as? Group)?.contactID)

        if (location.isNullOrBlank()) {
            log.warn("别名导入：文件没有可用的下载地址，name=${file.name}, fileID=${file.fileID}")
            throw IllegalStateException.ReadFile("别名导入")
        }

        return readImportText(location)
    }

    private fun readImportText(location: String): String {
        return try {
            when {
                isHttpLocation(location) ->
                    restClient.get().uri(location).exchangeToBody<ByteArray>()
                        .toString(StandardCharsets.UTF_8)

                location.startsWith("file:", true) ->
                    Files.readString(Path.of(URI(location)), StandardCharsets.UTF_8)

                // 普通本地路径（绝对路径、~/、相对路径都走这里）
                else -> Files.readString(resolveImportPath(location), StandardCharsets.UTF_8)
            }.removePrefix("\uFEFF")
        } catch (e: NoSuchFileException) {
            log.warn("别名导入：文件不存在，location=$location")
            throw IllegalArgumentException.WrongException.AliasImportNotFound(e.file ?: location)
        } catch (e: Exception) {
            log.error("别名导入：读取文件失败，location=$location", e)
            throw IllegalStateException.ReadFile("别名导入")
        }
    }

    /**
     * 把文件内容解析成「标题 -> 别名」，格式与 !ao 导出一致。
     */
    private fun parseImportJson(text: String): Map<String, String> {
        val mapping = try {
            JacksonUtil.parseObject<Map<String, String>>(text)
        } catch (e: Exception) {
            log.warn("别名导入：JSON 解析失败", e)
            null
        } ?: throw IllegalArgumentException.WrongException.AliasImportJson()

        return mapping
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<AliasParam>
    ): Boolean {
        val export = Instruction.ALIAS_EXPORT.matcher(messageText)
        if (export.find()) {
            // 所有用户都能导出，冷却只在 handleMessage 里对非超级管理员生效
            data.value = AliasParam(emptyList(), AliasOperation.EXPORT)
            return true
        }

        val importInstruction = Instruction.ALIAS_IMPORT.matcher(messageText)
        if (importInstruction.find()) {
            // 导入会直接写入数据库并通过审核，只允许超级管理员
            if (event.isNotSuperAdmin()) return false

            // 指令后面可以跟链接或本地路径，不跟则等待用户发送文件
            val location = importInstruction.group(FLAG_DATA)
                ?.let { normalizeLocation(it) }
                ?.takeIf { it.isNotBlank() }

            data.value = AliasParam(emptyList(), AliasOperation.IMPORT, location = location)
            return true
        }

        val add = Instruction.ALIAS_ADD.matcher(messageText)
        if (add.find()) {
            // 管理员添加时不抛弃超出的条目，只有非管理员才会走抛弃
            val unlimited = event.isSuperAdmin()
            // :f 让管理员跳过二次确认，直接添加
            val force = add.group(FLAG_FULL) != null && event.isSuperAdmin()

            data.value = parseAliasParam(add, AliasOperation.ADD, unlimited = unlimited, force = force) ?: return false
            return true
        }

        val delete = Instruction.ALIAS_DELETE.matcher(messageText)
        if (delete.find()) {
            // 删除需要超级管理员，否则不回应
            if (event.isNotSuperAdmin()) return false

            // :f 捕获组表示不限制删除数量，并跳过二次确认直接删除
            val force = delete.group(FLAG_FULL) != null

            data.value = parseAliasParam(
                delete,
                AliasOperation.DELETE,
                unlimited = force,
                force = force,
            ) ?: return false
            return true
        }

        val manual = Instruction.ALIAS_MANUAL.matcher(messageText)
        if (manual.find()) {
            // !am 后面可以跟 BID，也可以跟别名：
            // 纯数字视为 BID，开启一个新的补录会话；其余内容视为别名，保存到当前会话。
            val input = manual.group(FLAG_DATA)?.trim()?.takeIf { it.isNotBlank() } ?: return false

            val bid = input.toLongOrNull()
            data.value = if (bid != null) {
                AliasParam(listOf(AliasTarget(null, bid, null)), AliasOperation.MANUAL)
            } else {
                AliasParam(emptyList(), AliasOperation.MANUAL_ALIAS, manualAlias = input)
            }
            return true
        }

        val review = Instruction.ALIAS_REVIEW.matcher(messageText)
        if (review.find()) {
            // 审核需要管理员，否则不回应
            if (event.isNotSuperAdmin()) return false

            // 指令后面可以直接跟编号，例如 !aw 1,2,5-10
            val selection = review.group(FLAG_DATA)
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            data.value = AliasParam(emptyList(), AliasOperation.REVIEW, reviewSelection = selection)
            return true
        }

        return false
    }

    override fun handleMessage(
        event: MessageEvent,
        param: AliasParam
    ): ServiceCallStatistic? {
        when (param.operation) {
            AliasOperation.ADD -> check(event, param)
            AliasOperation.DELETE -> delete(event, param)
            AliasOperation.REVIEW -> review(event, param.reviewSelection)
            AliasOperation.EXPORT -> export(event)
            AliasOperation.IMPORT -> importAliases(event, param)
            AliasOperation.MANUAL -> manual(event, param)
            AliasOperation.MANUAL_ALIAS -> manualAlias(event, param.manualAlias)
        }

        return ServiceCallStatistic.building(event)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AliasService::class.java)

        private val rateLimiter = TokenBucketRateLimiter(1, 2.minutes)

        /**
         * 添加/删除别名时一次最多处理的条目数量。超出的会被主动抛弃。
         */
        private const val ALIAS_BATCH_SIZE = 20

        /**
         * 审核时一次最多展示/处理的待审核别名数量。
         */
        private const val REVIEW_SIZE = 10

        /**
         * !am 手动补录会话的有效期（秒）。
         */
        private const val MANUAL_TIMEOUT_SECONDS = 60L

        /**
         * !am 会话缓存的容量上限。
         */
        private const val MANUAL_SESSION_MAX_SIZE = 10_000L

        /**
         * 导出指令发送的文件名。
         */
        private const val EXPORT_FILE_NAME = "aliasDB.json"

        /**
         * 导出指令在 [FetchService.rateLimiter] 里的令牌桶名。
         *
         * 用独立的名字，既复用 FetchService 的 2 分钟令牌桶，又不和 !fetch 抢次数。
         * 这个名字会直接进冷却提示（「服务 xxx 已经超过限制」），所以用中文。
         */
        private const val EXPORT_RATE_KEY = "别名导出"

        /**
         * 参数分隔符：,、，、;、&、|、反引号、、、PATTERN_EQUAL（=、＝）以及换行符。
         *
         * 冒号（:、：）**不在**此列：它是 :f 开关的语法，标题/别名里的冒号不应该被拆开。
         *
         * 整段参数按它拆开后，奇数位是标题，偶数位是别名。
         * 被 [QUOTATION] 包裹的内容不参与拆分。
         *
         * 换行必须配合 Instruction 里 data 捕获组的 PATTERN_ANYTHING 才能生效，
         * 若捕获组退回 [^\n]+，换行就进不到这里，这条分支会变成死代码。
         *
         * 所有分隔符都是单个字符，[isTargetSeparator] 依赖这一点。
         */
        private val TARGET_SEPARATOR = Regex("[,，;|&`、]|$PATTERN_EQUAL|[\\r\\n]")

        /**
         * 成对引号：英文双引号、中文双引号（开引号和闭引号都算）、全角双引号。
         *
         * 它们只作为保护标记，本身不会进入标题或别名。
         */
        private val QUOTATION = setOf('"', '“', '”', '＂')

        /**
         * 编号之间的分隔符：空白、半角逗号、中文逗号、顿号、竖线、冒号、反引号。
         *
         * AD 的位置挑选和 AW 的审核共用这一套。
         */
        private val SELECTION_SEPARATOR = Regex("[\\s,，、|:：`]+")

        /**
         * 区间符号两侧的空白。先去掉它们，`5 - 10` 才不会被空白拆成三段。
         */
        private val SELECTION_RANGE_SPACE = Regex("\\s*$PATTERN_HYPHEN\\s*")

        /**
         * 单个编号，允许负数（负数在审核里表示本次全部不通过）。
         */
        private val SELECTION_NUMBER = Regex("-?\\d+")

        /**
         * 闭区间：`5-10`、`5~10` 等，头尾都包含。区间符号和 [PATTERN_HYPHEN] 一致。
         */
        private val SELECTION_RANGE = Regex("(\\d+)\\s*$PATTERN_HYPHEN\\s*(\\d+)")
    }
}
