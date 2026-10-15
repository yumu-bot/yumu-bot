package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.AliasDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.restrict.RestrictUtils.isSuperAdmin
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.TipsRuntimeException
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.AsyncMessageUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.StringUtil.formatted
import com.now.nowbot.util.StringUtil.removeSongSuffix
import com.now.nowbot.util.StringUtil.standardised
import com.now.nowbot.util.command.PATTERN_EQUAL
import org.springframework.stereotype.Service
import java.util.regex.Matcher
import kotlin.time.Duration.Companion.seconds

@Service("ALIAS")
class AliasService(
    private val beatmapApiService: OsuBeatmapApiService,

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
        val operation: AliasOperation
    )

    enum class AliasOperation {
        ADD, DELETE, REVIEW
    }

    private data class DeleteTarget(
        val title: String,
        val alias: String?,
    )

    /**
     * 解析 `<标题或BID> = <别名>`，多组之间用 `,`、`;`、`:`、`&` 或 `=` 分隔。
     *
     * 整段参数拆开后，奇数位（第 1、3、5……个）视为标题，偶数位视为别名。
     * 纯数字视为 BID，其余视为标题。
     *
     * 数量为奇数说明最后一个标题漏写了别名，会提示用户。
     * 删除时允许只输入一个标题，表示删除该标题下的所有别名。
     */
    private fun parseAliasParam(matcher: Matcher, operation: AliasOperation): AliasParam? {
        val tokens = matcher.group("data")
            ?.split(TARGET_SEPARATOR)
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        if (tokens.isEmpty()) {
            return null
        }

        // 删除：只给一个标题时，删除该标题下的所有别名
        if (operation == AliasOperation.DELETE && tokens.size == 1) {
            return AliasParam(listOf(parseAliasTarget(tokens.first(), null)), operation)
        }

        // 标题和别名必须成对，奇数个说明漏写了别名
        if (tokens.size % 2 != 0) {
            throw IllegalArgumentException.WrongException.AliasPair()
        }

        val targets = tokens.chunked(2).map { (title, alias) -> parseAliasTarget(title, alias) }

        return AliasParam(targets, operation)
    }

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
     */
    private fun getStandardizedTitle(target: AliasTarget): String {
        val title = target.title
            ?: beatmapApiService.getBeatmap(
                target.beatmapID ?: throw IllegalArgumentException.WrongException.BeatmapID()
            ).beatmapset!!.title

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
     * 添加别名。
     *
     * 管理员（含超级管理员）添加的别名默认通过审核，普通用户添加的需要等待审核。
     *
     * 多组参数会合并成一条确认消息；回复 OK 才会保存，回复其他内容撤回本次操作；超时同样会默认保存。
     */
    private fun check(event: MessageEvent, param: AliasParam) {
        val targets = param.targets.map { target ->
            getStandardizedTitle(target) to getStandardizedAlias(target)
        }

        // 添加处甄别是否是管理员，是则默认通过审核
        val approved = event.isSuperAdmin()

        val preview = buildString {
            appendLine("添加别名：")
            targets.forEach { (title, alias) -> appendLine("$title -> $alias") }

            if (!approved) {
                appendLine("这些别名需要管理员审核之后才会通过。")
            }

            append("回复 OK 确认本次操作，回复其他内容则撤回本次操作。")
        }

        AsyncMessageUtil.doubleCheck(
            event = event,
            keyword = "OK",
            onCheck = { event.reply(preview) },
            onOverTime = {
                targets.forEach { (title, alias) ->
                    aliasDao.addAliasToTitle(
                        title,
                        alias,
                        setAsPrimary = false,
                        approved = approved,
                    )
                }
                event.reply(if (approved) "操作超时，已默认保存。" else "操作超时，已提交审核，请等待管理员审核。")
            },
            onWrong = { throw TipsRuntimeException("已撤回操作。") },
            onSuccess = { ev ->
                targets.forEach { (title, alias) ->
                    aliasDao.addAliasToTitle(
                        title,
                        alias,
                        setAsPrimary = false,
                        approved = approved,
                    )
                }
                ev.reply(if (approved) "保存成功！" else "已提交审核，请等待管理员审核。")
            },
            timeout = 30.seconds
        )
    }

    /**
     * 删除别名。只有管理员才能调用。
     *
     * 多组参数会合并成一条确认消息；alias 为 null 时表示删除该标题下的所有别名。
     */
    private fun delete(event: MessageEvent, param: AliasParam) {
        val targets = param.targets.map { target ->
            val alias = target.alias
                ?.takeIf { it.isNotBlank() }
                ?.removeSongSuffix()
                ?.standardised()

            DeleteTarget(getStandardizedTitle(target), alias)
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
        }

        AsyncMessageUtil.doubleCheck(
            event = event,
            keyword = "OK",
            onCheck = { event.reply(preview) },
            onSuccess = { ev -> ev.reply(deleteTargets(targets)) },
            timeout = 30.seconds
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
            // 空关键字，任意回复都会进入 onSuccess，由我们自己解析数字序列
            keyword = "",
            onCheck = {
                event.reply(buildString {
                    if (remaining > 0) {
                        appendLine("待审核的别名（共 ${allPending.size} 条，本次展示前 ${pending.size} 条）：")
                    } else {
                        appendLine("待审核的别名：")
                    }
                    pending.forEachIndexed { index, alias ->
                        appendLine("${index + 1}. ${alias.titles.joinToString(" / ")} -> ${alias.value}")
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

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<AliasParam>
    ): Boolean {
        val add = Instruction.ALIAS_ADD.matcher(messageText)
        if (add.find()) {
            data.value = parseAliasParam(add, AliasOperation.ADD) ?: return false
            return true
        }

        val delete = Instruction.ALIAS_DELETE.matcher(messageText)
        if (delete.find()) {
            // 删除需要管理员，否则不回应
            if (!event.isSuperAdmin()) return false

            data.value = parseAliasParam(delete, AliasOperation.DELETE) ?: return false
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
        }

        return ServiceCallStatistic.building(event)
    }

    companion object {
        /**
         * 审核时一次最多展示/处理的待审核别名数量。
         */
        private const val REVIEW_SIZE = 10

        /**
         * 参数分隔符：`,`、`;`、`:`、`&` 或 PATTERN_EQUAL。
         *
         * 整段参数按它拆开后，奇数位是标题，偶数位是别名。
         */
        private val TARGET_SEPARATOR = Regex(",|;|:|&|$PATTERN_EQUAL")
    }
}
