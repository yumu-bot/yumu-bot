package com.now.nowbot.image.markdown

import org.commonmark.node.Image
import org.commonmark.node.Node
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser

internal fun interface MarkdownImageResolver {
    /**
     * 返回 null 表示保留原图（仅限 data image）。
     */
    fun resolve(source: String): String?
}

internal object MarkdownImageRewriter {
    private val PARSER = Parser.builder()
        .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
        .build()

    fun rewrite(markdown: String, resolver: MarkdownImageResolver): String {
        val images = mutableListOf<Image>()
        collect(PARSER.parse(markdown), images)
        if (images.isEmpty()) return markdown

        val replacements = images.mapNotNull { image ->
            val source = image.destination.orEmpty()
            val resolved = resolver.resolve(source) ?: return@mapNotNull null
            val spans = image.sourceSpans
            if (spans.isEmpty()) {
                throw IllegalStateException("无法定位 Markdown 图片源码位置")
            }
            val start = spans.minOf { it.inputIndex }
            val end = spans.maxOf { it.inputIndex + it.length }
            val raw = markdown.substring(start, end)
            replacement(start, end, raw, resolved)
        }.sortedBy { it.start }

        if (replacements.isEmpty()) return markdown
        val accepted = mutableListOf<Replacement>()
        var lastEnd = -1
        for (replacement in replacements) {
            if (replacement.start < lastEnd) continue
            accepted += replacement
            lastEnd = replacement.end
        }

        val builder = StringBuilder(markdown.length)
        var cursor = 0
        for (replacement in accepted) {
            builder.append(markdown.substring(cursor, replacement.start))
            builder.append(replacement.text)
            cursor = replacement.end
        }
        builder.append(markdown.substring(cursor))
        return builder.toString()
    }

    private fun collect(node: Node?, images: MutableList<Image>) {
        var current = node
        while (current != null) {
            if (current is Image) {
                images += current
            } else {
                collect(current.firstChild, images)
            }
            current = current.next
        }
    }

    private fun replacement(start: Int, end: Int, raw: String, url: String): Replacement {
        val altClose = findAltClose(raw)
            ?: return Replacement(start, end, "![]($url)")
        if (altClose + 1 < raw.length && raw[altClose + 1] == '(') {
            val destination = destinationRange(raw, altClose + 2)
            if (destination != null) {
                return Replacement(
                    start + destination.start,
                    start + destination.end,
                    url,
                )
            }
        }
        val alt = raw.substring(2, altClose)
        return Replacement(start, end, "![$alt]($url)")
    }

    private fun findAltClose(raw: String): Int? {
        if (!raw.startsWith("![")) return null
        var depth = 0
        var index = 2
        while (index < raw.length) {
            when (raw[index]) {
                '\\' -> index += 2
                '[' -> {
                    depth++
                    index++
                }
                ']' -> {
                    if (depth == 0) return index
                    depth--
                    index++
                }
                else -> index++
            }
        }
        return null
    }

    private fun destinationRange(raw: String, from: Int): DestinationRange? {
        var index = from
        while (index < raw.length && raw[index].isWhitespace()) index++
        if (index >= raw.length) return null

        if (raw[index] == '<') {
            val start = index
            index++
            while (index < raw.length) {
                when (raw[index]) {
                    '\\' -> index += 2
                    '>' -> return DestinationRange(start, index + 1)
                    else -> index++
                }
            }
            return null
        }

        val start = index
        var depth = 0
        while (index < raw.length) {
            val char = raw[index]
            when {
                char == '\\' -> index += 2
                char == '(' -> {
                    depth++
                    index++
                }
                char == ')' -> {
                    if (depth == 0) break
                    depth--
                    index++
                }
                char.isWhitespace() && depth == 0 -> break
                else -> index++
            }
        }
        if (index <= start) return null
        return DestinationRange(start, index)
    }

    private data class DestinationRange(val start: Int, val end: Int)

    private data class Replacement(val start: Int, val end: Int, val text: String)
}
