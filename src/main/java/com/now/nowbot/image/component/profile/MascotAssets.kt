package com.now.nowbot.image.component.profile

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.util.Colors
import com.now.nowbot.model.osu.OsuUser
import java.util.concurrent.ThreadLocalRandom

object MascotAssets {
    private val COUNTS = intArrayOf(79, 35, 7, 5, 14, 1, 3, 5, 5, 7)
    private val TRANSPARENT_COUNTS = intArrayOf(2, 1, 0, 0, 1, 0, 0, 1, 0, 1)

    fun name(mode: String): String {
        return when (mode.lowercase()) {
            "osu" -> "pippi"
            "taiko" -> {
                val pick = weighted(1..6, 1..6)
                listOf("Mocha", "Aiko", "Alisa", "Chirou", "Tama", "Taikonator")[pick - 1]
            }
            "fruits", "catch" -> "Yuzu"
            "mania" -> if (ThreadLocalRandom.current().nextBoolean()) "Mani" else "Mari"
            else -> "pippi"
        }
    }

    data class Banner(val name: String, val image: String?, val offsetY: Double)

    fun banner(mode: String, windowHeight: Double, assets: ImageAssets): Banner {
        val name = name(mode)
        val index = indexOf(name)
        val count = COUNTS.getOrElse(index) { 1 }.coerceAtLeast(1)
        val pick = ThreadLocalRandom.current().nextInt(1, count + 1)
        val percentages = OFFSETS[name] ?: emptyList()
        val percent = (percentages.getOrElse(pick - 1) { 50 } / 100.0).coerceIn(0.0, 1.0)
        val y = kotlin.math.max(0.0, percent * 710.0 - windowHeight / 2)
        return Banner(name, assets.local("Mascots/${name}_$pick.webp"), -y)
    }

    fun path(name: String, assets: ImageAssets): String? {
        val index = indexOf(name)
        val count = COUNTS.getOrElse(index) { 1 }.coerceAtLeast(1)
        val pick = ThreadLocalRandom.current().nextInt(1, count + 1)
        return assets.local("Mascots/${name}_$pick.webp")
    }

    fun transparentPath(name: String, assets: ImageAssets): String? {
        val index = indexOf(name)
        val mappedName = when (name) {
            "Aiko", "Alisa" -> "Mocha"
            "Tama", "Taikonator" -> "Chirou"
            "Mani" -> "Mari"
            else -> name
        }
        val mappedIndex = indexOf(mappedName)
        val count = TRANSPARENT_COUNTS.getOrElse(mappedIndex) { 0 }
        if (count <= 0) return path(name, assets)
        val pick = ThreadLocalRandom.current().nextInt(1, count + 1)
        return assets.local("Mascots/Transparent/${mappedName}_$pick.webp")
    }

    fun rankBackground(user: OsuUser?, assets: ImageAssets): String? {
        val rank = user?.globalRank ?: 0
        val percent = user?.statistics?.globalRankPercent ?: (rank / 1000.0)
        val key = when {
            rank in 1..100 -> "PF"
            percent < 0.0005 -> "SSS"
            percent < 0.0025 -> "A"
            percent < 0.005 -> "B"
            percent < 0.025 -> "X"
            percent < 0.05 -> "XH"
            percent < 0.25 -> "SP"
            percent < 0.5 -> "SH"
            else -> "F"
        }
        return assets.local("object-score-backimage-$key.webp")
    }

    fun colorForRank(rank: String): String = Colors.getRankColor(rank)

    private fun indexOf(name: String): Int = when (name) {
        "pippi" -> 0
        "Mocha" -> 1
        "Aiko" -> 2
        "Alisa" -> 3
        "Chirou" -> 4
        "Tama" -> 5
        "Taikonator" -> 6
        "Yuzu" -> 7
        "Mani" -> 8
        "Mari" -> 9
        else -> 0
    }

    private val OFFSETS: Map<String, List<Int>> = mapOf(
        "Aiko" to listOf(45, 40, 30, 50, 45, 25, 25),
        "Alisa" to listOf(30, 35, 45, 20, 45),
        "Chirou" to listOf(50, 45, 55, 60, 25, 55, 35, 65, 60, 35, 30, 40, 65, 35),
        "Mani" to listOf(65, 40, 40, 20, 55),
        "Mocha" to listOf(25, 25, 50, 40, 20, 30, 35, 30, 50, 25, 35, 30, 25, 30, 70, 50, 20, 40, 30, 25, 30, 30, 25, 35, 25, 30, 20, 20, 40, 30, 50, 20, 25, 35, 30),
        "pippi" to listOf(45, 35, 35, 30, 45, 45, 50, 30, 30, 15, 20, 50, 20, 20, 35, 10, 20, 30, 30, 35, 35, 35, 30, 45, 20, 35, 20, 35, 35, 20, 40, 25, 25, 30, 15, 50, 60, 35, 30, 35, 15, 15, 20, 20, 20, 20, 25, 35, 30, 30, 50, 25, 20, 35, 45, 20, 10, 30, 25, 30, 25, 30, 30, 40, 25, 25, 15, 20, 60, 25, 30, 30, 15, 20, 35, 40, 30, 25, 30),
        "Taikonator" to listOf(40, 30, 50),
        "Tama" to listOf(35),
        "Yuzu" to listOf(35, 35, 20, 20, 0),
    )

    private fun weighted(range: IntRange, weights: IntRange): Int {
        val total = range.sumOf { COUNTS[it] }
        var value = ThreadLocalRandom.current().nextInt(1, total + 1)
        for (index in range) {
            value -= COUNTS[index]
            if (value <= 0) return index
        }
        return range.last
    }
}
