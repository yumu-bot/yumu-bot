package com.now.nowbot.image.font

import org.apache.fontbox.cff.CFFCIDFont
import org.apache.fontbox.ttf.KerningSubtable
import org.apache.fontbox.ttf.OTFParser
import org.apache.fontbox.ttf.OpenTypeFont
import org.apache.fontbox.ttf.TrueTypeFont
import java.awt.geom.GeneralPath
import java.io.InputStream

internal class OpenTypeFace(resource: String) {
    private val font: TrueTypeFont
    private val gpos: GposKerning?
    private val legacyKerning: KerningSubtable?

    val unitsPerEm: Int
    val ascender: Int
    val descender: Int

    init {
        val stream: InputStream = OpenTypeFace::class.java.getResourceAsStream(resource)
            ?: throw IllegalStateException("font resource not found: $resource")
        font = stream.use { OTFParser().parseEmbedded(it) }
        unitsPerEm = font.unitsPerEm
        ascender = font.horizontalHeader.ascender.toInt()
        descender = font.horizontalHeader.descender.toInt()

        val gposTable = font.tableMap["GPOS"]
        gpos = if (gposTable != null) {
            runCatching { GposKerning.parse(font.getTableBytes(gposTable)) }.getOrNull()
        } else {
            null
        }
        legacyKerning = if (gposTable == null) {
            runCatching { font.kerning?.getHorizontalKerningSubtable() }.getOrNull()
        } else {
            null
        }
    }

    fun glyphId(codePoint: Int): Int = font.unicodeCmapLookup.getGlyphId(codePoint)

    fun advanceWidth(glyphId: Int): Int = font.horizontalMetrics.getAdvanceWidth(glyphId)

    fun kerning(left: Int, right: Int): Int {
        gpos?.let { return it.kerning(left, right) }
        return legacyKerning?.getKerning(left, right) ?: 0
    }

    fun glyphPath(glyphId: Int): GeneralPath {
        val openType = font as? OpenTypeFont
        if (openType != null && openType.isPostScript) {
            val cff = openType.getCFF().font
            val selector = if (cff is CFFCIDFont) cff.charset.getCIDForGID(glyphId) else glyphId
            return cff.getType2CharString(selector).path
        }
        return font.getGlyph().getGlyph(glyphId).path
    }
}
