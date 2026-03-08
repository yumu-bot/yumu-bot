package com.now.nowbot.model.filter

data class Condition(
    private val string: String,
) {
    val condition = string.replace('_', ' ').trim()
    val long: Long = condition.toLongOrNull() ?: -1L
    val int: Int = condition.toIntOrNull() ?: -1
    val double: Double = condition.toDoubleOrNull() ?: -1.0
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
