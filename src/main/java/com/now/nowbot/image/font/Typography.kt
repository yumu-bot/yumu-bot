package com.now.nowbot.image.font

import com.now.nowbot.image.svg.Svg
import java.awt.geom.PathIterator
import java.util.Locale

data class TextPart(
    val text: String = "",
    val font: FontFace,
    val size: Double = 24.0,
    val color: String = "#fff",
)

data class TextMetrics(
    val width: Double,
    val height: Double,
    val ascender: Double,
    val descender: Double,
    val x: Double,
    val baseline: Double,
)

data class BeatmapTitlePaths(
    val title: String,
    val titleUnicode: String,
)

object Typography {
    fun width(face: FontFace, text: String, size: Double): Double {
        if (text.isEmpty() || size <= 0) return 0.0
        val source = face.source
        val scale = size / source.unitsPerEm
        val glyphs = glyphs(source, text)
        var width = 0.0
        for (index in glyphs.indices) {
            val advance = source.advanceWidth(glyphs[index])
            if (advance != 0) width += advance * scale
            if (index < glyphs.lastIndex) width += source.kerning(glyphs[index], glyphs[index + 1]) * scale
        }
        return width
    }

    fun metrics(face: FontFace, text: String, size: Double): TextMetrics {
        val source = face.source
        val scale = size / source.unitsPerEm
        val ascender = source.ascender * scale
        val descender = source.descender * scale
        return TextMetrics(
            width = width(face, text, size),
            height = (source.ascender - source.descender) * scale,
            ascender = ascender,
            descender = descender,
            x = 0.0,
            baseline = ascender,
        )
    }

    fun textPath(
        face: FontFace,
        text: String,
        x: Double,
        y: Double,
        size: Double,
        anchor: String = "left baseline",
        fill: String = "#fff",
        opacity: Double = 1.0,
        shadow: Boolean = false,
        stroke: String = "none",
        strokeWidth: Double = 0.0,
        strokeOpacity: Double = 1.0,
    ): String {
        if (text.isEmpty() || size <= 0) return ""
        val source = face.source
        val scale = size / source.unitsPerEm
        val width = width(face, text, size)
        val height = (source.ascender - source.descender) * scale
        val ascender = source.ascender * scale

        val horizontal = anchorValue(anchor, HORIZONTAL_ANCHOR, "left")
        val vertical = anchorValue(anchor, VERTICAL_ANCHOR, "baseline")

        val startX = when (horizontal) {
            "center" -> x - width / 2
            "right" -> x - width
            else -> x
        }
        val baseline = when (vertical) {
            "top" -> y + ascender
            "middle" -> y - height / 2 + ascender
            "bottom" -> y - height + ascender
            else -> y
        }

        val builder = StringBuilder()
        if (shadow) {
            builder.append("<path fill=\"#1c1719\" d=\"")
            builder.append(pathData(source, text, startX + 2, baseline + 2, scale))
            builder.append("\"/>")
        }
        builder.append("<path fill=\"").append(Svg.escape(fill))
            .append("\" fill-opacity=\"").append(Svg.number(opacity))
            .append("\" stroke=\"").append(Svg.escape(stroke))
            .append("\" stroke-width=\"").append(Svg.number(strokeWidth))
            .append("\" stroke-opacity=\"").append(Svg.number(strokeOpacity))
            .append("\" stroke-linejoin=\"round\" d=\"")
            .append(pathData(source, text, startX, baseline, scale))
            .append("\"/>")
        return builder.toString()
    }

    fun cutStringTail(face: FontFace, text: String, size: Double, maxWidth: Double, ellipsis: Boolean = true): String {
        if (width(face, text, size) <= maxWidth) return text

        val dot3 = if (ellipsis) "..." else ""
        val targetWidth = maxWidth - width(face, dot3, size)
        if (targetWidth <= 0) return dot3

        var low = 0
        var high = text.length
        var best = 0
        while (low <= high) {
            val middle = (low + high).ushr(1)
            val currentWidth = width(face, text.substring(0, middle), size)
            if (currentWidth <= targetWidth) {
                best = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }

        return text.substring(0, best) + dot3
    }

    fun twoSizeTextPath(
        face: FontFace,
        largerText: Any?,
        smallerText: Any?,
        largeSize: Double,
        smallSize: Double,
        x: Double,
        y: Double,
        anchor: String = "center baseline",
        color: String = "#fff",
    ): String {
        val larger = largerText?.toString().orEmpty()
        val smaller = smallerText?.toString().orEmpty()
        val horizontal = anchorValue(anchor, HORIZONTAL_ANCHOR, "center")
        val vertical = anchorValue(anchor, VERTICAL_ANCHOR, "baseline")

        return when (horizontal.firstOrNull()) {
            'l' -> {
                val largerWidth = width(face, larger, largeSize)
                face.textPath(larger, x, y, largeSize, anchor, color) +
                    face.textPath(smaller, x + largerWidth, y, smallSize, anchor, color)
            }
            'r' -> {
                val smallerWidth = width(face, smaller, smallSize)
                face.textPath(larger, x - smallerWidth, y, largeSize, anchor, color) +
                    face.textPath(smaller, x, y, smallSize, anchor, color)
            }
            else -> {
                val largerWidth = width(face, larger, largeSize)
                val smallerWidth = width(face, smaller, smallSize)
                val half = (largerWidth + smallerWidth) / 2
                face.textPath(larger, x - half, y, largeSize, "left $vertical", color) +
                    face.textPath(smaller, x + half, y, smallSize, "right $vertical", color)
            }
        }
    }

    fun multipleTextPath(
        parts: List<TextPart>,
        x: Double,
        y: Double,
        anchor: String = "center baseline",
        opacity: Double = 1.0,
        shadow: Boolean = false,
    ): String {
        if (parts.isEmpty()) return ""
        var totalWidth = 0.0
        for (part in parts) totalWidth += width(part.font, part.text, part.size)

        val horizontal = anchorValue(anchor, HORIZONTAL_ANCHOR, "center")
        val vertical = anchorValue(anchor, VERTICAL_ANCHOR, "baseline")
        var currentX = when (horizontal) {
            "center" -> x - totalWidth / 2
            "right" -> x - totalWidth
            else -> x
        }

        val builder = StringBuilder()
        for (part in parts) {
            builder.append(part.font.textPath(part.text, currentX, y, part.size, "left $vertical", part.color, opacity, shadow))
            currentX += width(part.font, part.text, part.size)
        }
        return builder.toString()
    }

    fun beatMapTitlePath(
        font: FontFace,
        font2: FontFace,
        title: String,
        titleUnicode: String,
        artist: String?,
        x: Double,
        y: Double,
        y2: Double,
        size: Double,
        size2: Double,
        maximumWidth: Double,
        anchor: String = "center baseline",
        color: String = "#fff",
        color2: String = color,
        x2: Double = x,
    ): BeatmapTitlePaths {
        if (title == titleUnicode) {
            val titleCut = cutStringTail(font, title, size, maximumWidth, false)
            val exceed = if (titleCut.length == title.length) {
                artist.orEmpty()
            } else {
                title.substring(titleCut.length.coerceAtMost(title.length))
            }
            return BeatmapTitlePaths(
                title = font.textPath(titleCut, x, y, size, anchor, color),
                titleUnicode = font.textPath(cutStringTail(font, exceed, size2, maximumWidth, true), x2, y2, size2, anchor, color2),
            )
        }

        return BeatmapTitlePaths(
            title = font.textPath(cutStringTail(font, title, size, maximumWidth, true), x, y, size, anchor, color),
            titleUnicode = font2.textPath(cutStringTail(font2, titleUnicode, size2, maximumWidth, true), x2, y2, size2, anchor, color2),
        )
    }

    private val HORIZONTAL_ANCHOR = Regex("left|center|right", RegexOption.IGNORE_CASE)
    private val VERTICAL_ANCHOR = Regex("baseline|top|bottom|middle", RegexOption.IGNORE_CASE)

    private fun anchorValue(anchor: String, regex: Regex, default: String): String {
        return regex.find(anchor)?.value?.lowercase(Locale.ROOT) ?: default
    }

    private fun glyphs(source: OpenTypeFace, text: String): IntArray {
        val glyphs = IntArray(text.codePointCount(0, text.length))
        var index = 0
        var offset = 0
        while (offset < text.length) {
            val codePoint = text.codePointAt(offset)
            glyphs[index++] = source.glyphId(codePoint)
            offset += Character.charCount(codePoint)
        }
        return glyphs
    }

    private fun pathData(source: OpenTypeFace, text: String, x: Double, baseline: Double, scale: Double): String {
        val glyphs = glyphs(source, text)
        val builder = StringBuilder()
        var pen = x
        for (index in glyphs.indices) {
            val glyph = glyphs[index]
            appendGlyph(builder, runCatching { source.glyphPath(glyph) }.getOrNull(), pen, baseline, scale)
            pen += source.advanceWidth(glyph) * scale
            if (index < glyphs.lastIndex) pen += source.kerning(glyph, glyphs[index + 1]) * scale
        }
        return builder.toString()
    }

    private fun appendGlyph(builder: StringBuilder, path: java.awt.Shape?, x: Double, baseline: Double, scale: Double) {
        if (path == null) return
        val iterator = path.getPathIterator(null)
        val coordinates = DoubleArray(6)
        while (!iterator.isDone) {
            when (iterator.currentSegment(coordinates)) {
                PathIterator.SEG_MOVETO -> {
                    builder.append('M')
                    appendPoint(builder, x + coordinates[0] * scale, baseline - coordinates[1] * scale)
                }
                PathIterator.SEG_LINETO -> {
                    builder.append('L')
                    appendPoint(builder, x + coordinates[0] * scale, baseline - coordinates[1] * scale)
                }
                PathIterator.SEG_QUADTO -> {
                    builder.append('Q')
                    appendValues(
                        builder,
                        x + coordinates[0] * scale, baseline - coordinates[1] * scale,
                        x + coordinates[2] * scale, baseline - coordinates[3] * scale,
                    )
                }
                PathIterator.SEG_CUBICTO -> {
                    builder.append('C')
                    appendValues(
                        builder,
                        x + coordinates[0] * scale, baseline - coordinates[1] * scale,
                        x + coordinates[2] * scale, baseline - coordinates[3] * scale,
                        x + coordinates[4] * scale, baseline - coordinates[5] * scale,
                    )
                }
                PathIterator.SEG_CLOSE -> builder.append('Z')
            }
            iterator.next()
        }
    }

    private fun appendPoint(builder: StringBuilder, px: Double, py: Double) {
        builder.append(Svg.pathNumber(px))
        if (py >= 0) builder.append(' ')
        builder.append(Svg.pathNumber(py))
    }

    private fun appendValues(builder: StringBuilder, vararg values: Double) {
        values.forEachIndexed { index, value ->
            if (index > 0 && value >= 0) builder.append(' ')
            builder.append(Svg.pathNumber(value))
        }
    }
}
