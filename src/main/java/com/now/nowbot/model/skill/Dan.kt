package com.now.nowbot.model.skill

abstract class Dan(
    val name: String,
    val boundary: List<Double>,
    // 需要比 boundary 大一个
    val grade: List<String>,
    val max: Double?,
    val offset: Int = 0,
    val use: List<Int>
)

enum class DanType {
    REFORM, REGULAR, LN;

    fun getDan(): Dan = when (this) {
        REFORM -> ReformDan()
        LN -> LnDan()
        REGULAR -> ReformDan()
    }
}

class LnDan : Dan(
    "ln",
    listOf(
        0.0, 5.0,
        5.5, 6.0, 6.5, 7.0, 7.5,
        8.0, 8.5, 9.0, 9.5, 10.0,
        10.8, 11.6, 12.4, 13.0

        ),
    listOf(
        "-",
        "0",
        "1", "2", "3", "4", "5",
        "6", "7", "8", "9", "10",
        "G", "A", "Z", "S",
    ),
    14.5,
    -1,
    listOf(4, 5, 6)
)

class ReformDan : Dan(
    "reform",
    listOf(
        0.0, 1.5, 2.5, 3.5,
        3.9, 4.2, 4.5, 4.8, 5.1,
        5.4, 5.7, 6.0, 6.5, 7.0,
        7.5, 8.0, 8.5, 9.0, 9.5,
        10.0, 10.5,
    ),
    listOf(
        "-",
        ".1", ".2", ".3",
        "1", "2", "3", "4", "5",
        "6", "7", "8", "9", "10",
        "A", "B", "G", "D", "E",
        "Z", "H"
    ),
    12.0,
    -3,
    listOf(1, 2, 3)
)

fun getDan(
    skills: List<Double>,
    danType: DanType = DanType.REFORM,
): Map<String, Any> {
    val dan = danType.getDan()
    val sorted = dan.use.mapNotNull { skills.getOrNull(it - 1) }.sortedDescending()
    val sum = 0.5 * sorted[0] + 0.3 * sorted[1] + 0.2 * sorted[2]

    val boundary = dan.boundary
    val grades = dan.grade
    val name = dan.name
    val baseGradeOffset = dan.offset

    // 1. 找到 sum 应该落入的区间索引
    // indexOfLast 会找到最后一个小于等于 sum 的边界。
    // coerceAtLeast(0) 确保哪怕传入了极端的负数，也会 fallback 到最低区间
    val index = boundary.indexOfLast { sum >= it }.coerceAtLeast(0)

    // 2. 根据索引计算 baseGrade
    val baseGrade = index + baseGradeOffset

    // 3. 处理达到或超过最高级的情况
    if (sum >= (dan.max ?: Double.MAX_VALUE)) {
        return mapOf(
            "${name}_level" to boundary.size + baseGradeOffset,
            "${name}_grade" to grades.last() + "+",
        )
    } else if (index >= boundary.size - 1) {
        return mapOf(
            "${name}_level" to baseGrade,
            "${name}_grade" to grades.last(),
        )
    }

    // 4. 确定当前区间的左右边界
    val lower = boundary[index]
    val upper = boundary[index + 1]

    // 5. 计算线性偏移 (0.0 到 1.0 之间)
    val fraction = (sum - lower) / (upper - lower)

    val plus = if (fraction in 0.5 ..< 1.0) "+" else ""

    // 6. 组合名称 (消除了硬编码的 "-")
    val grade = if (index == 0) {
        grades[index] // 最低等级通常不加 "+"
    } else {
        grades[index] + plus
    }

    // 7. 返回结果
    return mapOf(
        "${name}_level" to (baseGrade + fraction),
        "${name}_grade" to grade,
    )
}