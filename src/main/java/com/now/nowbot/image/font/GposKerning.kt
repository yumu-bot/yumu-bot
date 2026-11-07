package com.now.nowbot.image.font

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class GposKerning private constructor(
    private val data: ByteBuffer,
    private val lookups: List<Lookup>,
) {
    fun kerning(left: Int, right: Int): Int {
        for (lookup in lookups) {
            for (subtable in lookup.subtables) {
                val coverage = subtable.coverage(left)
                if (coverage < 0) continue
                when (subtable.format) {
                    1 -> {
                        val pairSet = subtable.pairSets.getOrNull(coverage) ?: continue
                        for (pair in pairSet) {
                            if (pair.second == right) return pair.xAdvance
                        }
                    }
                    2 -> {
                        val class1 = subtable.classDef1(left)
                        val class2 = subtable.classDef2(right)
                        return subtable.classRecords
                            .getOrNull(class1)
                            ?.getOrNull(class2)
                            ?: 0
                    }
                }
            }
        }
        return 0
    }

    private class Lookup(val subtables: List<Subtable>)

    private class Subtable(
        val format: Int,
        private val coverageFormat: Int,
        private val coverageGlyphs: IntArray,
        private val coverageRanges: List<CoverageRange>,
        val pairSets: List<List<Pair>>,
        private val classDef1: ClassDef,
        private val classDef2: ClassDef,
        val classRecords: List<List<Int>>,
    ) {
        fun coverage(glyph: Int): Int {
            return if (coverageFormat == 1) {
                coverageGlyphs.indexOf(glyph)
            } else {
                val range = coverageRanges.firstOrNull { glyph in it.start..it.end } ?: return -1
                range.index + glyph - range.start
            }
        }

        fun classDef1(glyph: Int): Int = classDef1.value(glyph)

        fun classDef2(glyph: Int): Int = classDef2.value(glyph)
    }

    private data class Pair(val second: Int, val xAdvance: Int)

    private data class CoverageRange(val start: Int, val end: Int, val index: Int)

    private class ClassDef(private val format: Int, private val start: Int, private val classes: IntArray, private val ranges: List<ClassRange>) {
        fun value(glyph: Int): Int {
            return if (format == 1) {
                val index = glyph - start
                if (index in classes.indices) classes[index] else 0
            } else {
                ranges.firstOrNull { glyph in it.start..it.end }?.value ?: 0
            }
        }
    }

    private data class ClassRange(val start: Int, val end: Int, val value: Int)

    companion object {
        private const val X_ADVANCE = 0x0004

        fun parse(bytes: ByteArray): GposKerning {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            if (buffer.remaining() < 10) return GposKerning(buffer, emptyList())

            val major = u16(buffer, 0)
            if (major != 1) return GposKerning(buffer, emptyList())
            val scriptListOffset = u16(buffer, 4)
            val featureListOffset = u16(buffer, 6)
            val lookupListOffset = u16(buffer, 8)

            val script = findScript(buffer, scriptListOffset)
            if (script < 0) return GposKerning(buffer, emptyList())
            val langSys = findDefaultLangSys(buffer, script)
            if (langSys < 0) return GposKerning(buffer, emptyList())

            val featureIndices = readIndices(buffer, langSys + 4)
            val lookupIndices = mutableListOf<Int>()
            val featureCount = u16(buffer, featureListOffset)
            for (featureIndex in featureIndices) {
                if (featureIndex >= featureCount) continue
                val recordOffset = featureListOffset + 2 + featureIndex * 6
                if (readTag(buffer, recordOffset) != "kern") continue
                val featureOffset = featureListOffset + u16(buffer, recordOffset + 4)
                lookupIndices += readIndices(buffer, featureOffset + 2)
            }
            if (lookupIndices.isEmpty()) return GposKerning(buffer, emptyList())

            val lookupCount = u16(buffer, lookupListOffset)
            val lookups = mutableListOf<Lookup>()
            for (lookupIndex in lookupIndices.distinct()) {
                if (lookupIndex >= lookupCount) continue
                val lookupOffset = lookupListOffset + u16(buffer, lookupListOffset + 2 + lookupIndex * 2)
                val lookupType = u16(buffer, lookupOffset)
                if (lookupType != 2) continue
                val subtableCount = u16(buffer, lookupOffset + 4)
                val subtables = mutableListOf<Subtable>()
                for (subtableIndex in 0 until subtableCount) {
                    val offset = lookupOffset + u16(buffer, lookupOffset + 6 + subtableIndex * 2)
                    parseSubtable(buffer, offset)?.let { subtables += it }
                }
                if (subtables.isNotEmpty()) lookups += Lookup(subtables)
            }
            return GposKerning(buffer, lookups)
        }

        private fun parseSubtable(buffer: ByteBuffer, offset: Int): Subtable? {
            if (offset < 0 || offset + 8 > buffer.limit()) return null
            val format = u16(buffer, offset)
            val coverageOffset = u16(buffer, offset + 2)
            val valueFormat1 = u16(buffer, offset + 4)
            val valueFormat2 = u16(buffer, offset + 6)
            val valueSize1 = valueRecordSize(valueFormat1)
            val valueSize2 = valueRecordSize(valueFormat2)
            val xAdvanceOffset = Integer.bitCount(valueFormat1 and (X_ADVANCE - 1)) * 2

            val coverage = parseCoverage(buffer, offset + coverageOffset) ?: return null
            return when (format) {
                1 -> {
                    val pairSetCount = u16(buffer, offset + 8)
                    val pairSets = mutableListOf<List<Pair>>()
                    for (pairSetIndex in 0 until pairSetCount) {
                        val pairSetOffset = offset + u16(buffer, offset + 10 + pairSetIndex * 2)
                        val pairValueCount = u16(buffer, pairSetOffset)
                        val pairs = mutableListOf<Pair>()
                        var record = pairSetOffset + 2
                        for (pairIndex in 0 until pairValueCount) {
                            val second = u16(buffer, record)
                            record += 2
                            val xAdvance = if (valueFormat1 and X_ADVANCE != 0) s16(buffer, record + xAdvanceOffset) else 0
                            record += valueSize1 + valueSize2
                            pairs += Pair(second, xAdvance)
                        }
                        pairSets += pairs
                    }
                    Subtable(
                        1, coverage.first, coverage.second, coverage.third, pairSets,
                        ClassDef(1, 0, IntArray(0), emptyList()), ClassDef(1, 0, IntArray(0), emptyList()),
                        emptyList(),
                    )
                }
                2 -> {
                    val classDef1Offset = u16(buffer, offset + 8)
                    val classDef2Offset = u16(buffer, offset + 10)
                    val class1Count = u16(buffer, offset + 12)
                    val class2Count = u16(buffer, offset + 14)
                    val classDef1 = parseClassDef(buffer, offset + classDef1Offset)
                    val classDef2 = parseClassDef(buffer, offset + classDef2Offset)
                    val records = mutableListOf<List<Int>>()
                    var record = offset + 16
                    for (class1 in 0 until class1Count) {
                        val row = mutableListOf<Int>()
                        for (class2 in 0 until class2Count) {
                            val xAdvance = if (valueFormat1 and X_ADVANCE != 0) s16(buffer, record + xAdvanceOffset) else 0
                            row += xAdvance
                            record += valueSize1 + valueSize2
                        }
                        records += row
                    }
                    Subtable(
                        2, coverage.first, coverage.second, coverage.third, emptyList(),
                        classDef1, classDef2, records,
                    )
                }
                else -> null
            }
        }

        private fun parseCoverage(buffer: ByteBuffer, offset: Int): Triple<Int, IntArray, List<CoverageRange>>? {
            if (offset < 0 || offset + 4 > buffer.limit()) return null
            return when (val format = u16(buffer, offset)) {
                1 -> {
                    val count = u16(buffer, offset + 2)
                    val glyphs = IntArray(count) { u16(buffer, offset + 4 + it * 2) }
                    Triple(format, glyphs, emptyList())
                }
                2 -> {
                    val count = u16(buffer, offset + 2)
                    val ranges = (0 until count).map {
                        val record = offset + 4 + it * 6
                        CoverageRange(u16(buffer, record), u16(buffer, record + 2), u16(buffer, record + 4))
                    }
                    Triple(format, IntArray(0), ranges)
                }
                else -> null
            }
        }

        private fun parseClassDef(buffer: ByteBuffer, offset: Int): ClassDef {
            if (offset < 0 || offset + 4 > buffer.limit()) return ClassDef(1, 0, IntArray(0), emptyList())
            return when (val format = u16(buffer, offset)) {
                1 -> {
                    val start = u16(buffer, offset + 2)
                    val count = u16(buffer, offset + 4)
                    ClassDef(format, start, IntArray(count) { u16(buffer, offset + 6 + it * 2) }, emptyList())
                }
                2 -> {
                    val count = u16(buffer, offset + 2)
                    val ranges = (0 until count).map {
                        val record = offset + 4 + it * 6
                        ClassRange(u16(buffer, record), u16(buffer, record + 2), u16(buffer, record + 4))
                    }
                    ClassDef(format, 0, IntArray(0), ranges)
                }
                else -> ClassDef(1, 0, IntArray(0), emptyList())
            }
        }

        private fun findScript(buffer: ByteBuffer, scriptListOffset: Int): Int {
            if (scriptListOffset <= 0 || scriptListOffset + 2 > buffer.limit()) return -1
            val count = u16(buffer, scriptListOffset)
            var fallback = -1
            for (index in 0 until count) {
                val record = scriptListOffset + 2 + index * 6
                val tag = readTag(buffer, record)
                val offset = scriptListOffset + u16(buffer, record + 4)
                if (tag == "DFLT") return offset
                if (tag == "latn") fallback = offset
            }
            return fallback
        }

        private fun findDefaultLangSys(buffer: ByteBuffer, scriptOffset: Int): Int {
            if (scriptOffset + 4 > buffer.limit()) return -1
            val defaultOffset = u16(buffer, scriptOffset)
            if (defaultOffset != 0) return scriptOffset + defaultOffset
            val languageCount = u16(buffer, scriptOffset + 2)
            if (languageCount == 0) return -1
            val record = scriptOffset + 4
            return scriptOffset + u16(buffer, record + 4)
        }

        private fun readIndices(buffer: ByteBuffer, offset: Int): List<Int> {
            if (offset < 0 || offset + 2 > buffer.limit()) return emptyList()
            val count = u16(buffer, offset)
            return (0 until count).map { u16(buffer, offset + 2 + it * 2) }
        }

        private fun readTag(buffer: ByteBuffer, offset: Int): String {
            val bytes = ByteArray(4)
            for (index in 0 until 4) bytes[index] = buffer.get(offset + index)
            return String(bytes, Charsets.US_ASCII)
        }

        private fun valueRecordSize(format: Int): Int {
            var size = 0
            for (bit in 0 until 8) {
                if (format and (1 shl bit) != 0) size += 2
            }
            return size
        }

        private fun u16(buffer: ByteBuffer, offset: Int): Int {
            return buffer.getShort(offset).toInt() and 0xFFFF
        }

        private fun s16(buffer: ByteBuffer, offset: Int): Int = buffer.getShort(offset).toInt()
    }
}
