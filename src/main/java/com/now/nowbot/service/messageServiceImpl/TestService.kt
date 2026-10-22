package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.enums.Operator.Companion.getText
import com.now.nowbot.model.filter.ScoreClassifier
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.restrict.RestrictUtils.isSuperAdmin
import com.now.nowbot.service.MessageService
import com.now.nowbot.util.CommandUtil
import com.now.nowbot.util.command.CommandExtractor
import com.now.nowbot.util.command.ExtractType
import com.now.nowbot.util.command.OsuModeExtractor
import com.now.nowbot.util.command.PATTERN_EXCLAMATION
import com.now.nowbot.util.command.RangeExtractor
import com.now.nowbot.util.command.ScoreClassifierExtractor
import com.now.nowbot.util.command.StripContext
import com.now.nowbot.util.command.StripPipeline
import com.now.nowbot.util.command.UserIDExtractor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 解析器自检。
 *
 * 给超级管理员用的「命令行调试器」：`!yuumu` 后面的整段文本就是待解析素材，
 * 原样交给 [com.now.nowbot.util.command.CommandExtractor] 剥离，再用 [CommandUtil]
 * 补出模式、玩家和范围，最后把每一步的中间结果回显到聊天里。
 *
 * 输出分三块：
 * 1. 剥离结果：剩余文本、`syntaxes`（名字形式的分类器命中）、`extracted`（符号形式的提取结果）；
 * 2. 顺序敏感性：同一组剥离器正序 / 逆序各跑一遍，结果不一致时给出提示；
 * 3. [CommandUtil]：最终生效的 mode、userID、offset / limit，以及它补写回 ctx 的 RANGE。
 *
 * 三块都只是读解析结果，不会真正执行业务指令，所以可以放心用各种脏输入试。
 */
@Service("TEST")
class TestService(
    private val commandUtil: CommandUtil,
) : MessageService<String> {

    val instruction = "yuumu"

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<String>
    ): Boolean {
        if (messageText.contains(instruction) && event.isSuperAdmin()) {
            data.value = messageText.replaceFirst(PATTERN_EXCLAMATION.toRegex(), "")
                .replaceFirst(instruction, "")
            return true
        } else {
            return false
        }
    }

    override fun handleMessage(event: MessageEvent, param: String): ServiceCallStatistic? {
        val message = try {
            inspect(event, param)
        } catch (e: Exception) {
            log.warn("解析器自检异常", e)

            "解析器自检异常：${e.javaClass.simpleName}: ${e.message}"
        }

        event.reply(message)

        return null
    }

    /**
     * 把一次自检的完整过程拼成一段可直接发出去的文本。
     *
     * @param param `!yuumu` 之后、尚未做任何处理的原始素材
     */
    private fun inspect(event: MessageEvent, param: String): String {
        val source = param.trim()

        if (source.isEmpty()) {
            return """
                用法：!yuumu <待解析文本>
                示例：!yuumu pp>=600 acc>98.5% title='My Song' :m #2-5 剩下的名字
            """.trimIndent()
        }

        val forward = parse(source, EXTRACTORS)
        val backward = parse(source, EXTRACTORS.reversed())

        val orderStable = forward.text == backward.text &&
                forward.extracted == backward.extracted &&
                forward.syntaxes == backward.syntaxes

        // CommandUtil 会往 ctx 里补写 MODE / RANGE，单独开一份上下文，避免影响上面的剥离结果
        val utilCtx = parse(source, EXTRACTORS)

        val mode = runCatching { commandUtil.getMode(utilCtx, event) }
            .onFailure { log.debug("CommandUtil.getMode 失败：{}", it.message) }
            .getOrNull()

        val userID = runCatching { commandUtil.getUserID(utilCtx, event) }
            .onFailure { log.debug("CommandUtil.getUserID 失败：{}", it.message) }
            .getOrNull()

        // 别名补写依赖 getUserID 的结果：解析失败时剩余文本不该被当成别名
        val alias = if (userID != null) {
            commandUtil.addResidualAliases(utilCtx)

            utilCtx[Pair(ScoreClassifier.ALIASES.name, Operator.EQ)]
        } else {
            emptyList()
        }

        val range = runCatching { commandUtil.getRange(utilCtx) }
            .onFailure { log.debug("CommandUtil.getRange 失败：{}", it.message) }
            .getOrNull()

        return buildString {
            appendLine("【原始】$source")
            appendLine()
            appendLine("【剥离结果】")
            append(describe(forward))

            if (!orderStable) {
                appendLine("⚠ 顺序敏感：逆序结果不同")
                append(describe(backward))
            }

            appendLine()
            appendLine("【CommandUtil】")
            appendLine("mode     = ${mode?.let { "${it.name}/${it.shortName}" } ?: "null"}")
            appendLine("userID   = ${userID ?: "null"}")
            appendLine("alias    = ${alias.ifEmpty { listOf("(无)") }.joinToString("、")}")
            appendLine("range    = ${range?.let { (offset, limit) -> "offset=$offset, limit=$limit" } ?: "null"}")
            appendLine("ctx.RANGE = ${utilCtx[ExtractType.RANGE]}")
        }
    }

    /** 用同一组剥离器跑一遍，等价于正式指令里的 `StripPipeline(...).apply(...).trim()`。 */
    private fun parse(source: String, extractors: List<CommandExtractor>): StripContext {
        val pipeline = StripPipeline(source)

        extractors.forEach { pipeline.apply(it) }

        return pipeline.trim().context
    }

    /** 把 [StripContext] 里的三类结果摊平成几行文本。 */
    private fun describe(ctx: StripContext): String = buildString {
        appendLine("剩余文本：\"${ctx.text}\"")
        appendLine("剩余片段：${ctx.segments.joinToString(" | ") { "[${it.start},${it.end})\"${it.text}\"" }}")
        appendLine("保底名字：\"${ctx.separatedText}\"")

        appendLine("syntaxes：")
        if (ctx.syntaxes.isEmpty()) {
            appendLine("  (无)")
        } else {
            ctx.syntaxes.forEach { (key, values) ->
                // key = (分类器名, 运算符)，例如 (PERFORMANCE, GE)
                appendLine("  ${key.first}${key.second.getText()}${values.joinToString("、")}")
            }
        }

        appendLine("extracted：")
        if (ctx.extracted.isEmpty()) {
            appendLine("  (无)")
        } else {
            ctx.extracted.forEach { (key, values) ->
                appendLine("  $key = ${values.joinToString("、")}")
            }
        }
    }

    private companion object {
        private val log: Logger = LoggerFactory.getLogger(TestService::class.java)

        /**
         * 和正式指令保持同一批剥离器；这里的顺序只是模拟真实执行，
         * [inspect] 会再跑一遍逆序来验证「顺序不影响结果」。
         *
         * - [UserIDExtractor]：`uid=123`
         * - [ScoreClassifierExtractor]：`pp>=600`、`title='My Song'` 这类名字形式
         * - [OsuModeExtractor]：`:m`
         * - [RangeExtractor]：`#3-5`
         */
        private val EXTRACTORS: List<CommandExtractor> = listOf(
            UserIDExtractor,
            ScoreClassifierExtractor,
            OsuModeExtractor,
            RangeExtractor,
        )
    }
}
