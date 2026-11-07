package com.now.nowbot.image.render

import kotlin.math.ceil

object SvgDocument {
    data class SvgSize(val width: Int, val height: Int)

    private data class ViewBox(val width: Double, val height: Double)

    private val SVG_ROOT = Regex("(?is)<svg\\b[^>]*>")
    private val WIDTH_ATTR = Regex("""(?i)(?<![-\w])width\s*=\s*(?:"([^"]*)"|'([^']*)')""")
    private val HEIGHT_ATTR = Regex("""(?i)(?<![-\w])height\s*=\s*(?:"([^"]*)"|'([^']*)')""")
    private val VIEW_BOX_ATTR = Regex("""(?i)(?<![-\w])viewBox\s*=\s*(?:"([^"]*)"|'([^']*)')""")
    private val XML_DECLARATION = Regex("""(?is)^\s*<\?xml[^>]*\?>\s*""")
    private val DOCTYPE = Regex("""(?is)^\s*<!DOCTYPE[^>]*>\s*""")
    private const val MIN_SIZE = 1
    private const val MAX_SIZE = 16_384

    private const val PAGE_STYLE =
        "html,body{margin:0;padding:0;width:100%;height:100%;background:transparent;overflow:hidden}" +
            "svg{display:block}"

    fun parseSize(svg: String): SvgSize {
        val root = SVG_ROOT.find(svg)
            ?: throw IllegalArgumentException("SVG 缺少根 <svg> 标签，无法解析尺寸")

        val viewBox = parseViewBox(attribute(root.value, VIEW_BOX_ATTR))
        val width = dimension(root.value, WIDTH_ATTR, "width", viewBox?.width)
        val height = dimension(root.value, HEIGHT_ATTR, "height", viewBox?.height)
        return SvgSize(viewport(width, "width"), viewport(height, "height"))
    }

    fun toHtml(svg: String, size: SvgSize = parseSize(svg)): String {
        val cleaned = clean(svg)
        val root = SVG_ROOT.find(cleaned)
            ?: throw IllegalArgumentException("SVG 缺少根 <svg> 标签，无法内联")

        val additions = buildString {
            if (!WIDTH_ATTR.containsMatchIn(root.value)) append(" width=\"${size.width}\"")
            if (!HEIGHT_ATTR.containsMatchIn(root.value)) append(" height=\"${size.height}\"")
        }
        val body = if (additions.isEmpty()) {
            cleaned
        } else {
            val tag = root.value
            val replaced = if (tag.endsWith("/>")) {
                tag.dropLast(2) + additions + "/>"
            } else {
                tag.dropLast(1) + additions + ">"
            }
            cleaned.replaceRange(root.range, replaced)
        }
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>$PAGE_STYLE</style></head>" +
            "<body style=\"margin:0;background:transparent\">$body</body></html>"
    }

    private fun clean(svg: String): String {
        var value = svg.removePrefix("\uFEFF")
        value = XML_DECLARATION.replace(value, "")
        value = DOCTYPE.replace(value, "")
        return value
    }

    private fun dimension(rootTag: String, regex: Regex, name: String, viewBoxFallback: Double?): Double {
        val raw = attribute(rootTag, regex)
        if (raw == null) {
            return viewBoxFallback ?: throw IllegalArgumentException(
                "SVG 缺少 $name 属性且 viewBox 无法提供该尺寸",
            )
        }
        return pixels(raw, name)
    }

    private fun viewport(value: Double, name: String): Int {
        val pixels = ceil(value).toInt()
        require(pixels in MIN_SIZE..MAX_SIZE) {
            "SVG $name 超出浏览器可截图范围 $MIN_SIZE..$MAX_SIZE: $value"
        }
        return pixels
    }

    private fun pixels(raw: String, name: String): Double {
        val text = raw.trim().removeSuffix("px").trim()
        val value = text.toDoubleOrNull()
            ?: throw IllegalArgumentException("SVG $name 不是有效数值: \"$raw\"")
        require(value.isFinite() && value > 0) { "SVG $name 必须为正数: \"$raw\"" }
        return value
    }

    private fun parseViewBox(raw: String?): ViewBox? {
        if (raw == null) return null
        val parts = raw.trim().split(Regex("[,\\s]+")).filter { it.isNotEmpty() }
        if (parts.size != 4) {
            throw IllegalArgumentException("SVG viewBox 必须包含 4 个数值: \"$raw\"")
        }
        val width = pixels(parts[2], "viewBox width")
        val height = pixels(parts[3], "viewBox height")
        return ViewBox(width, height)
    }

    private fun attribute(tag: String, regex: Regex): String? {
        val match = regex.find(tag) ?: return null
        return match.groupValues[1].ifEmpty { match.groupValues[2] }
    }
}
