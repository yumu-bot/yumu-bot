package com.now.nowbot.image.view

import com.now.nowbot.image.util.Numbers
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

internal object ViewSupport {
    fun longestBpm(red: List<ViewTiming>, lastTime: Double): Double {
        val bpmMap = LinkedHashMap<Double, Double>()
        for (index in red.indices) {
            val current = red[index]
            val nextTime = if (index < red.lastIndex) red[index + 1].time.toDouble() else lastTime
            val duration = nextTime - current.time
            if (duration <= 0) continue

            val bpm = current.bpm ?: continue
            if (bpm < 0.1 || bpm > 10000) continue

            val currentDuration = bpmMap[bpm] ?: 0.0
            bpmMap[Numbers.jsRound(bpm * 100) / 100] = currentDuration + duration
        }

        var longestBpm = 120.0
        var maxDuration = 0.0
        for ((bpm, duration) in bpmMap) {
            if (duration > maxDuration) {
                maxDuration = duration
                longestBpm = bpm
            }
        }
        return longestBpm
    }

    fun normalizeBpm(rawBpm: Double, minBpm: Double = 120.0, maxBpm: Double = 300.0): Double {
        if (!rawBpm.isFinite()) return maxBpm
        if (rawBpm <= 0) return minBpm

        var result = rawBpm
        if (result > maxBpm) {
            val folds = ceil(log2(result / maxBpm))
            result /= 2.0.pow(folds)
        } else if (result < minBpm) {
            val folds = ceil(log2(minBpm / result))
            result *= 2.0.pow(folds)
        }
        return max(minBpm, min(maxBpm, result))
    }

    fun keyOverlay(totalKey: Int, special: Boolean): List<String> {
        if (special && totalKey % 2 == 0) {
            val result = mutableListOf("s")
            for (index in 0 until totalKey - 1) {
                result += if (index % 2 == 0) "1" else "2"
            }
            return result
        }

        val half = totalKey / 2
        val side = mutableListOf<String>()
        for (index in 0 until half) {
            side += if (index % 2 == 0) "1" else "2"
        }
        return if (totalKey % 2 == 0) {
            side + side.reversed()
        } else {
            side + "s" + side.reversed()
        }
    }

    fun key(x: Int, totalKey: Int): Int {
        return max(min(x * totalKey / 512, totalKey - 1), 0)
    }

    fun svX(sv: Double, center: Double, maxSv: Double, minSv: Double): Double {
        return if (sv >= 1.0) {
            val range = max(maxSv, 1.0) - 1.0
            center + (if (range == 0.0) 0.0 else ((min(sv, 5.0) - 1.0) / range) * center)
        } else {
            val range = 1.0 - min(minSv, 1.0)
            center - (if (range == 0.0) 0.0 else ((1.0 - max(sv, 0.0)) / range) * center)
        }
    }

    fun svY(sv: Double, center: Double, maxSv: Double, minSv: Double): Double {
        return if (sv >= 1.0) {
            val range = max(maxSv, 1.0) - 1.0
            center + (if (range == 0.0) 0.0 else ((min(sv, 5.0) * -1.0 - 1.0) / range) * center)
        } else {
            val range = 1.0 - min(minSv, 1.0)
            center - (if (range == 0.0) 0.0 else ((1.0 - max(sv, 0.0)) * -1.0 / range) * center)
        }
    }

    fun formatSv(sv: Double): String {
        return Numbers.round(sv, 2).replace("0.", ".")
    }

    fun svSummary(minSv: Double, maxSv: Double): String {
        if (!minSv.isFinite() || !maxSv.isFinite()) return ""
        return "sv: min ${Numbers.round(minSv, 2)}x, max ${Numbers.round(maxSv, 2)}x // "
    }
}
