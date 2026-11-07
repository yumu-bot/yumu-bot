package com.now.nowbot.image.svg

import java.math.BigDecimal

object Svg {
    private val XML_HEADER = Regex("""^\s*<\?xml[^>]*\?>\s*""")
    private val SVG_BODY = Regex("""(?is)<svg[^>]*>(.*)</svg>""")

    fun escape(value: String?): String {
        val text = value.orEmpty()
        return buildString(text.length) {
            for (c in text) {
                when (c) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")
                    else -> append(c)
                }
            }
        }
    }

    fun number(value: Double): String {
        if (!value.isFinite()) return "0"
        if (value == 0.0) return "0"
        val plain = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
        return if (plain == "-0") "0" else plain
    }

    fun pathNumber(value: Double): String {
        if (!value.isFinite() || value == 0.0) return "0"
        if (value == value.toLong().toDouble()) return value.toLong().toString()
        return BigDecimal(value).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
    }

    fun insertAfter(base: String, marker: String, insert: String): String {
        val index = base.indexOf(marker)
        require(index >= 0) { "SVG marker not found: $marker" }
        val position = index + marker.length
        return base.substring(0, position) + insert + base.substring(position)
    }

    fun setText(base: String, insert: String?, marker: String): String {
        if (insert.isNullOrEmpty()) return base
        return insertAfter(base, marker, insert)
    }

    fun setTexts(base: String, inserts: List<String?>, marker: String): String {
        val body = inserts.asReversed().filterNot { it.isNullOrEmpty() }.joinToString("\n")
        if (body.isEmpty()) return base
        return insertAfter(base, marker, body)
    }

    fun group(x: Double, y: Double, body: String): String {
        return "<g transform=\"translate(${number(x)} ${number(y)})\">$body</g>"
    }

    fun image(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        href: String,
        opacity: Double = 1.0,
        ratio: String = "xMidYMid slice",
    ): String {
        if (href.isEmpty()) return ""
        val transform = if (x != 0.0 || y != 0.0) {
            " transform=\"translate(${number(x)} ${number(y)})\""
        } else {
            ""
        }
        return "<image width=\"${number(width)}\" height=\"${number(height)}\"$transform " +
            "href=\"${escape(href)}\" style=\"opacity: ${number(opacity)}\" " +
            "preserveAspectRatio=\"${escape(ratio)}\" vector-effect=\"non-scaling-stroke\"/>"
    }

    fun rect(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        radius: Double = 0.0,
        color: String = "#FFFFFF",
        opacity: Double = 1.0,
    ): String {
        return "<rect x=\"${number(x)}\" y=\"${number(y)}\" width=\"${number(width)}\" " +
            "height=\"${number(height)}\" rx=\"${number(radius)}\" ry=\"${number(radius)}\" " +
            "style=\"fill: ${escape(color)}; fill-opacity: ${number(opacity)}\"/>"
    }

    fun circle(cx: Double, cy: Double, radius: Double, color: String, opacity: Double = 1.0): String {
        return "<circle cx=\"${number(cx)}\" cy=\"${number(cy)}\" r=\"${number(radius)}\" " +
            "style=\"fill: ${escape(color)}; fill-opacity: ${number(opacity)}\"/>"
    }

    fun polygon(points: List<Double>, color: String, opacity: Double = 1.0): String {
        require(points.size % 2 == 0) { "多边形坐标数量必须为偶数: ${points.size}" }
        val coordinates = points.chunked(2).joinToString(" ") { (x, y) -> "${number(x)},${number(y)}" }
        return "<polygon points=\"$coordinates\" " +
            "style=\"fill: ${escape(color)}; fill-opacity: ${number(opacity)}\"/>"
    }

    fun line(x1: Double, y1: Double, x2: Double, y2: Double, color: String, width: Double = 1.0, opacity: Double = 1.0): String {
        return "<line x1=\"${number(x1)}\" y1=\"${number(y1)}\" x2=\"${number(x2)}\" y2=\"${number(y2)}\" " +
            "style=\"stroke: ${escape(color)}; stroke-width: ${number(width)}; stroke-opacity: ${number(opacity)}\"/>"
    }

    fun removeXmlDeclaration(value: String): String = XML_HEADER.replace(value, "")

    fun bodyOf(value: String): String {
        val match = SVG_BODY.find(value) ?: return value
        return match.groupValues[1]
    }
}
