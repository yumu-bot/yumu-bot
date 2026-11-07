package com.now.nowbot.image.util

import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.roundToInt

object ColorArray {
    val iron = listOf("#BAB3AB", "#DDD")
    val bronze = listOf("#855C47", "#B88F7A")
    val silver = listOf("#A3A3C2", "#E0E0EB")
    val gold = listOf("#E0C952", "#F0E4A8")
    val platinum = listOf("#52E0DF", "#A8F0EF")
    val rhodium = listOf("#A0CF96", "#D9F8D3")
    val radiant = listOf("#ED82FF", "#97DCFF")
    val lustrous = listOf("#ED82FF", "#FFE600")
    val orange = listOf("#FF4E6F", "#FAD126")
    val amber = listOf("#EC6841", "#FF9800")
    val yellow = listOf("#FCAC46", "#FEDC45")
    val lightYellow = listOf("#FFC86B", "#FFFF00")
    val whiteGreen = listOf("#42EB85", "#38F8D3")
    val lightGreen = listOf("#5EDC5B", "#CAF881")
    val green = listOf("#31B16C", "#12B4B1")
    val indigo = listOf("#12B4B1", "#00B7EE")
    val pink = listOf("#F54869", "#FBA37E")
    val peach = listOf("#F990AB", "#F9BDBE")
    val sakura = listOf("#FED8EC", "#F7D8FE")
    val red = listOf("#D32F2F", "#FD5392")
    val deepRed = listOf("#8A1538", "#D32F2F")
    val blue = listOf("#7776FF", "#4FACFE")
    val deepBlue = listOf("#1D2088", "#0068B7")
    val cyan = listOf("#4FACFE", "#00F2FE")
    val magenta = listOf("#C12E70", "#FF4DA6")
    val purple = listOf("#9922EE", "#F772D1")
    val deepGray = listOf("#666666", "#999999")
    val gray = listOf("#999999", "#CCCCCC")
    val white = listOf("#CCCCCC", "#FAFAFA")
    val rainbow = listOf("#C645B8", "#FF4E6F", "#FF6868", "#F6F05C", "#7CFF4F", "#4FFFD5", "#00F2FE")
    val iridescent = listOf("#FF4E6F", "#F6F05C", "#7CFF4F", "#00F2FE")
    val rating = listOf("#4FFFFD", "#7CFF4F", "#F6F05C", "#FF6868", "#FF4E6F", "#C645B8", "#6563DE", "#18158E")
    val darkRed = listOf("#4A0B1E", "#6E1818")
    val darkBlue = listOf("#0E1045", "#004275")
    val darkGray = listOf("#4D4D4D", "#737373")
}

object Colors {
    private data class RankRule(val limit: Double, val color: List<String>)

    private val rankColorMap = mapOf(
        "PF" to "#FAFAFA", "XH" to "#FAFAFA", "SSH" to "#FAFAFA", "EX" to "#FAFAFA",
        "X+" to "#FAFAFA", "X" to "#FFFF00", "SS" to "#FFFF00", "SH" to "#BDBDBD",
        "SP" to "#E86100", "S+" to "#E86100", "S" to "#FF9800", "A" to "#22AC38",
        "B" to "#00A0E9", "C" to "#9922EE", "D" to "#D32F2F", "F" to "#616161",
        "FC" to "#7ECEF4",
    )

    private val rankColorsMap = mapOf(
        "PF" to ColorArray.white, "XH" to ColorArray.white, "SSH" to ColorArray.white,
        "EX" to ColorArray.white, "X+" to ColorArray.white, "X" to ColorArray.lightYellow,
        "SS" to ColorArray.lightYellow, "SH" to ColorArray.gray, "SP" to ColorArray.orange,
        "S+" to ColorArray.orange, "S" to ColorArray.amber, "A" to ColorArray.green,
        "B" to ColorArray.blue, "C" to ColorArray.purple, "D" to ColorArray.red,
        "F" to ColorArray.deepGray, "FC" to ColorArray.cyan,
    )

    private val globalRules = listOf(
        RankRule(0.0005, ColorArray.radiant),
        RankRule(0.0025, ColorArray.rhodium),
        RankRule(0.005, ColorArray.platinum),
        RankRule(0.025, ColorArray.gold),
        RankRule(0.05, ColorArray.silver),
        RankRule(0.25, ColorArray.bronze),
        RankRule(0.5, ColorArray.iron),
    )

    private val rankedPlayRules = listOf(
        RankRule(0.01, ColorArray.radiant),
        RankRule(0.025, ColorArray.radiant),
        RankRule(0.05, ColorArray.radiant),
        RankRule(0.1, ColorArray.rhodium),
        RankRule(0.15, ColorArray.rhodium),
        RankRule(0.2, ColorArray.rhodium),
        RankRule(0.3, ColorArray.platinum),
        RankRule(0.4, ColorArray.platinum),
        RankRule(0.5, ColorArray.platinum),
        RankRule(0.55, ColorArray.gold),
        RankRule(0.65, ColorArray.gold),
        RankRule(0.75, ColorArray.gold),
        RankRule(0.8, ColorArray.silver),
        RankRule(0.875, ColorArray.silver),
        RankRule(0.95, ColorArray.silver),
        RankRule(0.96, ColorArray.bronze),
        RankRule(0.98, ColorArray.bronze),
        RankRule(1.0, ColorArray.bronze),
    )

    private val starRatingColor = ColorInterpolator(
        values = listOf(0.1, 1.25, 2.0, 2.5, 3.3, 4.2, 4.9, 5.8, 6.7, 7.7, 9.0),
        colors = listOf(
            "#4290FB", "#4FC0FF", "#4FFFFD", "#7CFF4F", "#F6F05C", "#FF6868",
            "#FF4E6F", "#C645B8", "#6563DE", "#18158E", "#000000",
        ),
        minBound = 0.1,
        minColor = "#AAAAAA",
        maxBound = 9.0,
        maxColor = "#000000",
    )

    fun getRankColor(rank: String?): String {
        return rank?.uppercase()?.let { rankColorMap[it] } ?: "none"
    }

    fun getRankColors(rank: String?, darken: Boolean = false): List<String> {
        val colors = rank?.uppercase()?.let { rankColorsMap[it] } ?: return listOf("none", "none")
        return if (darken) listOf(colors.last(), ColorArray.deepGray.first()) else colors.asReversed()
    }

    fun getStarRatingColor(star: Double): String = starRatingColor(star)

    fun getStarRatingColors(star: Double): List<String> {
        if (star - 0.15 < 0.15 || star - 0.15 > 9) {
            val mid = getStarRatingColor(star)
            return listOf(mid, mid)
        }
        return listOf(getStarRatingColor(star - 0.15), getStarRatingColor(star + 0.15))
    }

    fun getGlobalRankPercentColor(globalRank: Long, globalRankPercent: Double, rankedPlay: Boolean = false): List<String> {
        if (globalRank <= 0 && globalRankPercent <= 0) return ColorArray.deepGray
        if (globalRank in 1..100) return ColorArray.lustrous
        val rules = if (rankedPlay) rankedPlayRules else globalRules
        return rules.firstOrNull { globalRankPercent <= it.limit }?.color ?: ColorArray.deepGray
    }

    fun getModColor(acronym: String?, explicit: String? = null): String {
        if (!explicit.isNullOrBlank()) return explicit
        return when (acronym?.uppercase()) {
            "NF" -> "#0068B7"
            "EZ" -> "#22AC38"
            "HD", "FI" -> "#F8B551"
            "HR" -> "#D32F2F"
            "SD" -> "#FF9800"
            "DT" -> "#00A0E9"
            "RX" -> "#BFC31F"
            "HT" -> "#BDBDBD"
            "NC" -> "#9922EE"
            "FL" -> "#000000"
            "AT", "CN" -> "#00B7EE"
            "SO" -> "#B28850"
            "AP" -> "#B3D465"
            "PF" -> "#FFF100"
            "4K", "5K", "6K", "7K", "8K", "9K" -> "#616161"
            "NR", "SR" -> "#68BE8D"
            "RD" -> "#009944"
            "MR" -> "#007130"
            "TD" -> "#7ECEF4"
            "DC" -> "#DADADA"
            "CO" -> "#F8B551"
            "BL" -> "#EB6100"
            "ST" -> "#D32F2F"
            "AC" -> "#9E040D"
            "DA" -> "#601986"
            "CL", "TP" -> "#920783"
            "AL" -> "#F16DAA"
            "SG" -> "#F59AC3"
            "SW" -> "#7B0046"
            "DS", "BM" -> "#9E005E"
            "IN" -> "#5F5BA8"
            "CS" -> "#A086BF"
            "HO" -> "#8781BE"
            "TR", "WG", "SI", "GR", "DF", "WU", "WD", "TC", "BR", "AD", "MU",
            "NS", "MG", "RP", "AS", "FR", "FF", "BU", "SY", "DP" -> "#EA68A2"
            "NM" -> "#22AC38"
            "FM" -> "#9922EE"
            "EX" -> "#FF9800"
            "DI" -> "#FF6100"
            "TB", "SV2", "V2" -> "#000000"
            "RC" -> "#22AC38"
            "LN" -> "#00A0E9"
            "HB" -> "#FF9800"
            "SV" -> "#9922EE"
            "SP" -> "#EA68A2"
            else -> "none"
        }
    }

    fun hex2rgbArray(hex: String): List<Int> {
        val value = hex.removePrefix("#")
        val normalized = when (value.length) {
            3 -> value.map { "$it$it" }.joinToString("")
            6, 8 -> value.take(6)
            else -> "000000"
        }
        return listOf(
            normalized.substring(0, 2).toIntOrNull(16) ?: 0,
            normalized.substring(2, 4).toIntOrNull(16) ?: 0,
            normalized.substring(4, 6).toIntOrNull(16) ?: 0,
        )
    }

    fun hex2rgbColor(hex: String): String = hex2rgbArray(hex).joinToString(", ")

    fun hex2OKLabBrightness(hex: String): Double {
        val (red, green, blue) = hex2rgbArray(hex).map { it / 255.0 }
        val r = linear(red)
        val g = linear(green)
        val b = linear(blue)
        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        return 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    }

    fun changeHexOKLabLightnessProgressive(hex: String, difference: Double): String {
        val (red, green, blue) = hex2rgbArray(hex).map { it / 255.0 }
        val r = linear(red)
        val g = linear(green)
        val b = linear(blue)

        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)

        val originalL = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
        val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
        val bValue = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s

        var adjusted = difference
        if (difference > 0) {
            if (originalL < 0.15) {
                adjusted *= 1.0 + ((0.15 - originalL) / 0.15).pow(2) * 0.8
            } else if (originalL > 0.6) {
                adjusted *= (1.0 - originalL) / 0.4
            }
        } else if (difference < 0) {
            if (originalL > 0.85) {
                adjusted *= 1.0 + ((originalL - 0.85) / 0.15).pow(2) * 0.8
            } else if (originalL < 0.4) {
                adjusted *= originalL / 0.4
            }
        }

        val lightness = (originalL + adjusted).coerceIn(0.0, 1.0)
        val lInv = lightness + 0.3963377774 * a + 0.2158037573 * bValue
        val mInv = lightness - 0.1055613458 * a - 0.0638541728 * bValue
        val sInv = lightness - 0.0894841775 * a - 1.2914855480 * bValue

        val rLinear = 4.0767416621 * lInv.pow(3) - 3.3077115913 * mInv.pow(3) + 0.2309699292 * sInv.pow(3)
        val gLinear = -1.2684380046 * lInv.pow(3) + 2.6093323202 * mInv.pow(3) - 0.3413342856 * sInv.pow(3)
        val bLinear = -0.0041960863 * lInv.pow(3) - 0.7034186147 * mInv.pow(3) + 1.7076147010 * sInv.pow(3)

        return rgbHex(
            (srgb(rLinear) * 255).roundToInt(),
            (srgb(gLinear) * 255).roundToInt(),
            (srgb(bLinear) * 255).roundToInt(),
        )
    }

    private fun rgbHex(red: Int, green: Int, blue: Int): String {
        fun channel(value: Int): String = value.coerceIn(0, 255).toString(16).padStart(2, '0')
        return "#${channel(red)}${channel(green)}${channel(blue)}".uppercase()
    }

    private fun linear(value: Double): Double = if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)

    private fun srgb(value: Double): Double = if (value <= 0.0031308) 12.92 * value else 1.055 * value.pow(1.0 / 2.4) - 0.055
}

class ColorInterpolator(
    private val values: List<Double>,
    private val colors: List<String>,
    private val minBound: Double? = null,
    private val maxBound: Double? = null,
    private val minColor: String = "#000000",
    private val maxColor: String = "#000000",
    private val gamma: Double = 2.2,
) {
    private val stops = values.mapIndexed { index, value -> value to Colors.hex2rgbArray(colors[index]) }
        .sortedBy { it.first }

    operator fun invoke(value: Double): String {
        if (minBound != null && value < minBound) return minColor
        if (maxBound != null && value >= maxBound) return maxColor
        if (stops.isEmpty()) return minColor
        if (value <= stops.first().first) return toHex(stops.first().second)
        if (value >= stops.last().first) return toHex(stops.last().second)

        val topIndex = stops.indexOfFirst { value < it.first }.let { if (it < 0) stops.lastIndex else it }
        val bottom = stops[topIndex - 1]
        val top = stops[topIndex]
        val ratio = (value - bottom.first) / (top.first - bottom.first)

        fun interpolate(first: Int, second: Int): String {
            val channel = ((1 - ratio) * first.toDouble().pow(gamma) + ratio * second.toDouble().pow(gamma)).pow(1 / gamma)
            return channel.roundToInt().toString(16).padStart(2, '0')
        }

        return "#${interpolate(bottom.second[0], top.second[0])}" +
            "${interpolate(bottom.second[1], top.second[1])}" +
            interpolate(bottom.second[2], top.second[2])
    }

    private fun toHex(rgb: List<Int>): String {
        return "#" + rgb.joinToString("") { it.toString(16).padStart(2, '0') }
    }
}
