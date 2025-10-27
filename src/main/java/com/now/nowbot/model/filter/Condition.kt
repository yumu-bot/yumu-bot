package com.now.nowbot.model.filter

import com.now.nowbot.util.DataUtil
import java.time.Period
import kotlin.time.Duration

/**
 * @param mode
 * - null: 自动判断 HH:mm 还是 mm:ss，不推荐
 * - false: 默认 HH:mm
 * - true: 默认 mm:ss
 */
data class Condition(
    private val string: String,
    private val mode: Boolean? = true,
) {
    val condition = string.replace('_', ' ').trim()
    val long: Long = condition.toLongOrNull() ?: -1L
    val int: Int = condition.toIntOrNull() ?: -1
    val double: Double = condition.toDoubleOrNull() ?: -1.0
    val time: Pair<Period, Duration> = DataUtil.parseTime(condition, mode = mode)
    val hasDecimal = condition.contains(".")
    val boolean = when(condition) {
        "真", "是", "正确", "对", "t", "true", "y", "yes", "" -> true
        else -> false
    }

    override fun hashCode(): Int {
        return condition.lowercase().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Condition

        return condition == other.condition
    }
}
