package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.controller.TokenBucketRateLimiter
import com.now.nowbot.dao.AliasDao
import com.now.nowbot.dao.BeatmapDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.FileMessage
import com.now.nowbot.restrict.RestrictUtils.isSuperAdmin
import com.now.nowbot.service.MessageService
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
import com.now.nowbot.util.StringUtil.standardised
import com.now.nowbot.util.command.FLAG_BID
import com.now.nowbot.util.command.FLAG_FULL
import com.now.nowbot.util.command.PATTERN_EQUAL
import com.now.nowbot.util.exchangeToBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
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
): MessageService<AliasService.AliasParam> {

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
    )

    enum class AliasOperation {
        ADD, DELETE, REVIEW, EXPORT, IMPORT, MANUAL
    }

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
     * 删除时允许只输入一个标题，表示删除该标题下的所有别名。
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

        // 删除：只给一个标题时，删除该标题下的所有别名
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
     * @param standardiseAlias 删除时别名需要和预览一致地做标准化。
     */
    private fun formatDiscardedFrom(target: AliasTarget?, standardiseAlias: Boolean): String {
        if (target == null) return ""

        val title = getStandardizedTitle(target)
        val alias = target.alias
            ?.takeIf { it.isNotBlank() }
            ?.removeSongSuffix()
            ?.let { if (standardiseAlias) it.standardised() else it }

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
            event.reply(if (approved) "保存成功！" else "已提交审核，请等待管理员审核。")
            return
        }

        val preview = buildString {
            appendLine("添加别名：")
            targets.forEach { (title, alias) -> appendLine("$title -> $alias") }

            if (param.discarded > 0) {
                val from = formatDiscardedFrom(param.discardedFrom, standardiseAlias = false)
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
                event.reply(if (approved) "操作超时，已默认保存。" else "操作超时，已提交审核，请等待管理员审核。")
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
                ev.reply(if (approved) "保存成功！" else "已提交审核，请等待管理员审核。")
            },
            timeout = 60.seconds
        )
    }

    /**
     * 手动补录别名（!am <BID>）。
     *
     * 先按 BID 找到谱面并展示它的 title、titleUnicode、artist，
     * 再用一问一答让用户决定要保存的别名：
     * - 回复 OK：取消操作
     * - 超时：取消操作
     * - 回复其他任意内容：把回复内容去掉歌曲后缀后保存为该曲目的别名
     *
     * 挂载标题和 [check] 一致：beatmapset.title 经 removeSongSuffix + formatted 标准化。
     * 权限也和 [check] 一致：管理员直接生效，普通用户提交后需要审核。
     */
    private fun manual(event: MessageEvent, param: AliasParam) {
        val target = param.targets.firstOrNull() ?: return
        val beatmapID = target.beatmapID ?: throw IllegalArgumentException.WrongException.BeatmapID()

        val beatmapset = runCatching {
            beatmapApiService.getBeatmap(beatmapID).beatmapset
        }.getOrNull() ?: throw NoSuchElementException.Beatmap(beatmapID)

        // 挂载标题和 !aa 保持一致：去掉歌曲后缀再 formatted 标准化
        val title = beatmapset.title.removeSongSuffix().formatted()
        if (title.isBlank()) throw IllegalArgumentException.WrongException.BeatmapID()

        val approved = event.isSuperAdmin()

        // 按标准流程（和 applyAlias 一致）查询该标题当前的别名，没有就是空列表
        val aliases = aliasDao.getAliasesByNormalizedTitles(listOf(title))[title].orEmpty()

        val preview = buildString {
            appendLine("谱面信息：")
            appendLine("标准化标题：${title}")
            appendLine("标题：${beatmapset.title}")
            appendLine("罗马字标题：${beatmapset.titleUnicode}")
            appendLine("艺术家：${beatmapset.artist}")
            appendLine()
            appendLine("将保存到别名库的标题：$title")
            appendLine("现有别名：${if (aliases.isEmpty()) "（空）" else aliases.joinToString("、")}")
            appendLine("回复 OK 取消本次操作，回复其他内容则将其作为该标题的别名保存。")
            append("60 秒后不操作同样视为取消。")
        }

        AsyncMessageUtil.doubleCancel(
            event = event,
            keywords = listOf("OK"),
            onCheck = { event.reply(preview) },
            // 回复 OK、超时都视为取消（默认行为就是取消，这里显式写出来更清楚）
            onCancel = { throw TipsRuntimeException("已取消操作。") },
            onInput = { ev ->
                val alias = ev.textMessage.removeSongSuffix()
                    .takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException.WrongException.Alias(ev.textMessage)

                aliasDao.addAliasToTitle(
                    title,
                    alias,
                    setAsPrimary = false,
                    approved = approved,
                    from = event.sender.contactID,
                )

                event.reply(
                    if (approved) {
                        "保存成功！已将「$alias」保存为「$title」的别名。"
                    } else {
                        "已提交审核！已将「$alias」提交为「$title」的别名，请等待管理员审核。"
                    }
                )
            },
            timeout = 60.seconds,
        )
    }

    /**
     * 删除别名。只有管理员才能调用。
     *
     * 多组参数会合并成一条确认消息；alias 为 null 时表示删除该标题下的所有别名。
     *
     * 超级管理员输入带 :f 捕获组（如 !ad:f 123=abc）时跳过二次确认，直接删除选定的所有条目。
     */
    private fun delete(event: MessageEvent, param: AliasParam) {
        // 和添加同理：去掉重复的「标题 -> 别名」，避免同一条被删两次、也避免结果里重复计数。
        val targets = param.targets.map { target ->
            val alias = target.alias
                ?.takeIf { it.isNotBlank() }
                ?.removeSongSuffix()
                ?.standardised()

            DeleteTarget(getStandardizedTitle(target), alias)
        }.distinct()

        // :f + 超级管理员：不确认，直接删除选定的所有条目
        if (param.force) {
            event.reply(deleteTargets(targets))
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
                val from = formatDiscardedFrom(param.discardedFrom, standardiseAlias = true)
                appendLine("一次最多只能删除 $ALIAS_BATCH_SIZE 条，已主动抛弃后面的 ${param.discarded} 条$from。")
            }
        }

        AsyncMessageUtil.doubleCheck(
            event = event,
            keywords = listOf("OK"),
            onCheck = { event.reply(preview) },
            onSuccess = { ev -> ev.reply(deleteTargets(targets)) },
            timeout = 60.seconds
        )
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
     * 然后等待管理员回复编号序列：
     * - 输入 0：本次展示的全部通过
     * - 输入负数：本次展示的全部不通过
     * - 输入其他非数字：取消操作
     * - 输入数字和逗号：对应的通过，其余的不通过（不存在的编号无视）
     *
     * 不通过的别名会连同绑定一起删除。
     */
    private fun review(event: MessageEvent) {
        val allPending = aliasDao.getPendingAliases()

        if (allPending.isEmpty()) {
            event.reply("目前没有待审核的别名。")
            return
        }

        // 一次最多只给 REVIEW_SIZE 条，剩下的下次审核时再给
        val pending = allPending.take(REVIEW_SIZE)
        val remaining = allPending.size - pending.size

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
                    append("请回复需要通过的编号（用逗号分隔），回复 0 本次全部通过，回复负数本次全部不通过，回复其他非数字取消操作。")
                    if (remaining > 0) {
                        append("\n处理完这 ${pending.size} 条后还有 $remaining 条待审核，请再次使用本指令。")
                    }
                })
            },
            onOverTime = { throw TipsRuntimeException("审核超时了。") },
            onSuccess = { ev ->
                val selected = parseSelection(ev.textMessage, pending.size)

                if (selected == null) {
                    ev.reply("已取消操作。")
                } else {
                    val approvedIds = selected.map { pending[it - 1].id }
                    val rejectedIds = pending.map { it.id }.filterNot { it in approvedIds }

                    val (approved, rejected) = aliasDao.reviewAliases(approvedIds, rejectedIds)
                    ev.reply(buildString {
                        append("已通过 $approved 个，不通过并删除 $rejected 个。")
                        if (remaining > 0) append("还有 $remaining 条待审核。")
                    })
                }
            },
            timeout = 60.seconds
        )
    }

    /**
     * 解析审核回复。
     *
     * 不存在的编号会被无视，不会导致其余编号失效。
     *
     * @return 通过审核的编号（1-based，已去重、排序）。
     * 返回空表示全部不通过；返回 null 表示取消操作（输入非数字，或编号全都不存在）。
     */
    private fun parseSelection(text: String, size: Int): List<Int>? {
        val input = text.trim()

        // 全部通过
        if (input == "0") return (1..size).toList()

        // 只接受「数字 + 分隔符」的输入，其余（含空回复）视为取消操作
        if (!input.matches(Regex("[\\d\\s,，、|:：`-]+"))) return null

        val numbers = input.split(Regex("[\\s,，、|:：`]+"))
            .filter { it.isNotEmpty() }
            .map { it.toIntOrNull() ?: return null }

        // 空回复视为取消操作
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
        if (!event.isSuperAdmin()) {
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
    private fun importAliases(event: MessageEvent) {
        AsyncMessageUtil.doubleCheck(
            event = event,
            // 任意回复都当作「文件消息」继续处理，具体有没有文件由 importFile 判断
            keywords = emptyList(),
            onCheck = {
                event.reply("""
                    请发送要导入的 JSON 文件（格式与 !ao 导出的结果一致），
                    或回复一条包含该文件的消息。
                """.trimIndent())
            },
            onOverTime = { throw TipsRuntimeException("等待文件超时，已取消导入。") },
            onSuccess = { ev -> importFile(ev) },
            timeout = 60.seconds,
        )
    }

    private fun importFile(event: MessageEvent) {
        val file = findImportFile(event) ?: throw IllegalArgumentException.WrongException.AliasImportFile()
        val text = readImportText(event, file)
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
            event.reply("文件里没有可以导入的别名。")
            return
        }

        val result = aliasDao.importAliases(
            mapping = mapping,
            approved = true,
            from = event.sender.contactID,
        )

        event.reply(buildString {
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

        return try {
            when {
                location.startsWith("http://", true) || location.startsWith("https://", true) ->
                    restClient.get().uri(location).exchangeToBody<ByteArray>()
                        .toString(StandardCharsets.UTF_8)

                location.startsWith("file:", true) ->
                    Files.readString(Path.of(URI(location)), StandardCharsets.UTF_8)

                else -> Files.readString(Path.of(location), StandardCharsets.UTF_8)
            }.removePrefix("\uFEFF")
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
            if (!event.isSuperAdmin()) return false

            data.value = AliasParam(emptyList(), AliasOperation.IMPORT)
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
            // 删除需要管理员，否则不回应
            if (!event.isSuperAdmin()) return false

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
            // 必须给出 BID，没给就当没匹配上，交给其他服务
            val bid = manual.group(FLAG_BID)?.toLongOrNull() ?: return false

            data.value = AliasParam(listOf(AliasTarget(null, bid, null)), AliasOperation.MANUAL)
            return true
        }

        val review = Instruction.ALIAS_REVIEW.matcher(messageText)
        if (review.find()) {
            // 审核需要管理员，否则不回应
            if (!event.isSuperAdmin()) return false

            data.value = AliasParam(emptyList(), AliasOperation.REVIEW)
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
            AliasOperation.REVIEW -> review(event)
            AliasOperation.EXPORT -> export(event)
            AliasOperation.IMPORT -> importAliases(event)
            AliasOperation.MANUAL -> manual(event, param)
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
    }
}
