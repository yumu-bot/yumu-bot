package com.now.nowbot.image.util

import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod

object GameModes {
    private val numToMode = mapOf(
        0 to "osu", 1 to "taiko", 2 to "fruits", 3 to "mania",
    )

    private val modeToRuleset = mapOf(
        "osu" to 0, "taiko" to 1, "fruits" to 2, "catch" to 2, "mania" to 3,
    )

    private val mappingByLevel = mapOf(
        -2 to mapOf("osu" to "0", "taiko" to "1", "fruits" to "2", "catch" to "2", "mania" to "3", "_default" to "0"),
        -1 to mapOf("osu" to "\uE800", "taiko" to "\uE803", "fruits" to "\uE801", "catch" to "\uE801", "mania" to "\uE802", "_default" to ""),
        1 to mapOf("osu" to "o", "taiko" to "t", "fruits" to "c", "catch" to "c", "mania" to "m", "_default" to "?"),
        2 to mapOf(
            "osu" to "osu!standard", "taiko" to "osu!taiko", "fruits" to "osu!catch",
            "catch" to "osu!catch", "mania" to "osu!mania", "_default" to "default",
        ),
    )

    private val iidx = listOf("Black Another", "Beginner", "Normal", "Hyper", "Another", "Leggendaria")
    private val sdvx = listOf("Basic", "Novice", "Advanced", "Exhaust", "Infinite", "Maximum", "Ultimate", "Gravity", "Heavenly", "Vivid", "Exceed", "Nabla")
    private val sdvxShort = listOf("BSC", "NOV", "ADV", "EXH", "INF", "MXM", "ULT", "GRV", "HVN", "VVD", "XCD", "NBL")
    private val standard = listOf("Easy", "Normal", "Hard", "Insane", "Lunatic", "Extra", "Extreme", "Expert", "Master", "Ultra")
    private val taiko = listOf("Inner Oni", "Ura Oni", "RASHOMON", "Hell Oni", "Kantan", "Futsuu", "Muzukashii", "Oni")
    private val fruits = listOf("Cup", "Salad", "Platter", "Rain", "Overdose", "Deluge", "Cataclysm")
    private val mania = listOf("EZ", "NM", "HD", "MX", "SC", "SHD")

    private val rulesets = mapOf(
        1 to RatingRules(
            listOf(0.1, 1.2, 2.0, 2.8, 4.0, 5.3, 6.5, 8.0),
            listOf("-", "SHOKYUU", "KANTAN", "FUTSUU", "MUZUKASHII", "ONI", "INNER ONI", "HELL ONI", "RASHOMON"),
        ),
        2 to RatingRules(
            listOf(0.1, 1.0, 1.8, 2.5, 3.5, 4.6, 6.0, 8.0),
            listOf("-", "SEED", "CUP", "SALAD", "PLATTER", "RAIN", "OVERDOSE", "DELUGE", "CATACLYSM"),
        ),
        3 to RatingRules(
            listOf(0.1, 1.2, 2.0, 2.8, 4.0, 5.3, 6.5, 8.0),
            listOf("-", "BG", "EZ", "NM", "HD", "MX", "SC", "SHD", "EX"),
        ),
        null to RatingRules(
            listOf(0.1, 1.2, 2.0, 2.8, 4.0, 5.3, 6.5, 8.0),
            listOf("-", "BEGINNER", "EASY", "NORMAL", "HARD", "INSANE", "EXPERT", "EXTREME", "ULTIMATE"),
        ),
    )

    private data class RatingRules(val thresholds: List<Double>, val names: List<String>)

    fun getGameMode(mode: Any?, level: Int = 0, defaultMode: String = "default"): String {
        val modeString = when (mode) {
            is Number -> {
                if (level == -2) return mode.toInt().toString()
                numToMode[mode.toInt()] ?: "osu"
            }
            is String -> {
                val rulesetId = modeToRuleset[mode] ?: -1
                numToMode[rulesetId] ?: mode.lowercase()
            }
            else -> return defaultMode
        }

        if (modeString == "default") return defaultMode
        val levelMap = mappingByLevel[level] ?: return modeString
        return levelMap[modeString] ?: levelMap["_default"].orEmpty()
    }

    fun rulesetId(mode: Any?): Int {
        return when (mode) {
            is Number -> mode.toInt()
            is String -> modeToRuleset[mode.lowercase()] ?: 0
            else -> 0
        }
    }

    fun getDifficultyIndex(difficultyName: String?, starRating: Double, rulesetId: Int, mods: List<LazerMod>): String {
        val ruleset = rulesetId
        val difficulties = (difficultyName ?: "")
            .replace("0", "o")
            .replace("1", "i")
            .replace("3", "e")
            .replace("4", "a")
            .replace("5", "s")
            .replace("6", "b")
            .replace("7", "t")
            .replace("9", "g")
            .uppercase()
            .trim()
            .split(Regex("\\s+"))

        if (!matchAnyMods(mods, listOf("DT", "NC", "HT", "DC"))) {
            findMatch(iidx, difficulties)?.let { return it }
            findMatch(sdvx, difficulties)?.let { matched ->
                val index = sdvx.indexOfFirst { it.equals(matched, ignoreCase = true) }
                return if (index >= 0) sdvxShort[index].uppercase() else matched
            }
            val rulesetMap = listOf(standard, taiko, fruits, mania)
            findMatch(rulesetMap.getOrElse(ruleset) { standard }, difficulties)?.let { return it }
        }

        return ratingName(ruleset, starRating)
    }

    private fun findMatch(difficultyList: List<String>, words: List<String>): String? {
        for (difficulty in difficultyList) {
            val upper = difficulty.uppercase()
            val difficultyWords = upper.split(' ')
            for (index in 0..words.size - difficultyWords.size) {
                val segment = words.subList(index, index + difficultyWords.size).joinToString(" ")
                if (segment == upper) return upper
            }
        }
        return null
    }

    private fun ratingName(rulesetId: Int, starRating: Double): String {
        if (starRating.isNaN()) return "UNKNOWN"
        val config = rulesets[rulesetId] ?: rulesets.getValue(null)
        val index = config.thresholds.indexOfFirst { starRating < it }
        return if (index < 0) config.names.last() else config.names[index]
    }

    fun getKeyDifficulty(beatmap: Beatmap?): String {
        if (beatmap == null) return ""
        var difficultyText = beatmap.difficultyName
        if (beatmap.modeInt == 3) {
            val startPattern = Regex("""^[\s\[/\\_\-]*(\d+\s?k)(ey)?[\s\]/\\_\-]*.*""", RegexOption.IGNORE_CASE)
            if (!startPattern.matches(difficultyText)) {
                val key = Numbers.jsRound((beatmap.cs ?: 0f).legacy()).toLong()
                val removePattern = Regex(
                    """[\s\[/\\_-]*${key}k\s?(ey)?[\s\]/\\_-]*""",
                    RegexOption.IGNORE_CASE,
                )
                difficultyText = "[${key}K] " + removePattern.replace(difficultyText, " ").trim()
            }
        }
        return difficultyText
    }

    fun removeGuest(difficultyName: String?): String {
        return (difficultyName ?: "").replace(
            Regex("""(\[[0-9]+K]\s*)?[^'\]]+('s?)""", RegexOption.IGNORE_CASE),
            "${'$'}1...${'$'}2",
        )
    }

    fun cs2px(cs: Double, rulesetId: Int): String {
        return when (rulesetId) {
            0, 2 -> Numbers.round(54.4 - 4.48 * cs, 2) + "px"
            else -> "-"
        }
    }

    fun ar2ms(ar: Double, rulesetId: Int): String {
        return when (rulesetId) {
            0, 2 -> {
                if (ar > 5) {
                    if (ar > 11) "300ms" else "${Numbers.jsRound(1200 - (150 * (ar - 5))).toLong()}ms"
                } else {
                    "${Numbers.jsRound(1800 - (120 * ar)).toLong()}ms"
                }
            }
            else -> "-"
        }
    }

    fun od2ms(od: Double, rulesetId: Int): String {
        val ms = when (rulesetId) {
            0 -> {
                if (od > 11) return "14ms"
                Numbers.jsRound(80 - (6 * od)).toLong().toString()
            }
            1 -> {
                if (od > 10) return "17ms"
                Numbers.jsRound(50 - (3 * od)).toLong().toString()
            }
            2 -> return "-"
            3 -> {
                if (od > 11) return "31ms"
                if (od < 0) return "64ms"
                Numbers.jsRound(64 - (3 * od)).toLong().toString()
            }
            else -> "0"
        }
        return ms + "ms"
    }

    fun hasLeaderBoard(ranked: Int): Boolean = ranked > 1e-6

    fun mapStatusFile(status: Int): String {
        return when (status) {
            -2, -1, 0 -> "object-beatmap-unranked.png"
            1, 2 -> "object-beatmap-ranked.png"
            3 -> "object-beatmap-qualified.png"
            4 -> "object-beatmap-loved.png"
            else -> "error.png"
        }
    }

    fun mapStatusImage(status: String?): String {
        return when (status?.lowercase()?.trim()) {
            "ranked", "approved" -> "object-beatmap-ranked.png"
            "qualified" -> "object-beatmap-qualified.png"
            "loved" -> "object-beatmap-loved.png"
            "" -> "error.png"
            else -> "object-beatmap-unranked.png"
        }
    }

    fun rankBackgroundFile(rank: String?, passed: Boolean): String {
        var value = rank ?: "F"
        if (!passed) value = "F"
        if (value == "X+" || value == "SS") value = "X"
        if (value == "SSH") value = "XH"
        if (value == "S+") value = "S"
        if (value == "EX") value = "PF"
        return "object-score-backimage-$value.webp"
    }

    fun rankImageFile(rank: String?): String = "object-score-${rank ?: "F"}2.png"

    fun scoreTypeFile(isLazer: Boolean?, version: String, scoreType: String?): String? {
        if (scoreType == "sb_score") return "object-type-ppysb$version.png"
        return when (isLazer) {
            null -> null
            true -> "object-type-lazer$version.png"
            false -> "object-type-stable$version.png"
        }
    }

    private fun matchAnyMods(mods: List<LazerMod>, names: List<String>): Boolean {
        return mods.any { mod -> names.any { it.equals(mod.acronym, ignoreCase = true) } }
    }

    private fun Float.legacy(): Double = toString().toDouble()
}
