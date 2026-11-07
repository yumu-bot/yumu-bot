package com.now.nowbot.image.component

import com.now.nowbot.model.calculate.CalculatePerformance
import com.now.nowbot.model.calculate.CosuPerformance
import com.now.nowbot.model.calculate.FullCalculatePerformance
import com.now.nowbot.model.calculate.RosuPerformance
import kotlin.math.max

data class PerformanceValues(
    val pp: Double = 0.0,
    val fullPp: Double = 0.0,
    val perfectPp: Double = 0.0,
    val aimPp: Double = 0.0,
    val speedPp: Double = 0.0,
    val accuracyPp: Double = 0.0,
    val readingPp: Double = 0.0,
    val flashlightPp: Double = 0.0,
    val difficultyPp: Double = 0.0,
    val effectiveMissCount: Double? = null,
) {
    companion object {
        fun from(attributes: Any?, rulesetId: Int): PerformanceValues {
            val performance = attributes as? CalculatePerformance
            val pp = performance?.pp ?: 0.0
            val fullPp = (attributes as? FullCalculatePerformance)?.fullPP ?: 0.0
            val perfectPp = (attributes as? FullCalculatePerformance)?.perfectPP ?: 0.0

            return when (attributes) {
                is RosuPerformance -> {
                    val accuracy = attributes.ppAccuracy ?: 0.0
                    PerformanceValues(
                        pp = pp,
                        fullPp = fullPp,
                        perfectPp = perfectPp,
                        aimPp = attributes.ppAim ?: 0.0,
                        speedPp = attributes.ppSpeed ?: 0.0,
                        accuracyPp = accuracy,
                        readingPp = attributes.ppReading ?: 0.0,
                        flashlightPp = attributes.ppFlashlight ?: 0.0,
                        difficultyPp = attributes.ppDifficulty
                            ?: if (rulesetId == 1) max(pp - accuracy, 0.0) else 0.0,
                        effectiveMissCount = attributes.effectiveMissCount,
                    )
                }
                is CosuPerformance -> PerformanceValues(
                    pp = pp,
                    fullPp = fullPp,
                    perfectPp = perfectPp,
                    aimPp = attributes.aim,
                    speedPp = attributes.speed,
                    accuracyPp = attributes.accuracy,
                    readingPp = 0.0,
                    flashlightPp = attributes.flashlight,
                    difficultyPp = if (rulesetId == 1) max(pp - attributes.accuracy, 0.0) else 0.0,
                    effectiveMissCount = attributes.effectiveMissCount,
                )
                else -> PerformanceValues(pp = pp, fullPp = fullPp, perfectPp = perfectPp)
            }
        }
    }
}
