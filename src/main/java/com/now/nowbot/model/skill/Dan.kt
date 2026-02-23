package com.now.nowbot.model.skill

import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.util.SkillUtil

abstract class Dan(
    val name: String,
    val boundary: List<Double>,
    // 需要比 boundary 大一个
    val grade: List<String>,
    val max: Double?,
    val offset: Int = 0,
    val use: List<Int>
)

data class DanResult(
    val name: String,
    val level: Double,
    val grade: String,
)

fun DanResult.toDynamicMap(): Map<String, Any>  {
    return mapOf(
        this.name to this
    )
}


fun getDanFromFavor(skills: List<Double>, favor: List<DanType>): Map<String, Any> {
    return favor.map { getDanResult(skills, it).toDynamicMap() }.reduce { a, b -> a + b }
}

/**
 * 会根据成绩 key 占比，返回最高的段位
 */
fun getDanFromBests(
    weightedMap: Map<Long, List<Double>>,
    bests: List<LazerScore>,
): Map<String, Any> {
    val (key4, key7) = bests.partition { (it.beatmap.cs ?: 4.0f) < 5.5f }

    val key4Set = key4.map { it.beatmapID }.toSet()
    val key7Set = key7.map { it.beatmapID }.toSet()

    val key4Skills = SkillUtil.collectScoreSkills(
        weightedMap.mapNotNull { (k, v) ->
            if (k in key4Set) v else null
        }
    )

    val key7Skills = SkillUtil.collectScoreSkills(
        weightedMap.mapNotNull { (k, v) ->
            if (k in key7Set) v else null
        }
    )

    val isPrefer4Key = SkillUtil.getMapSkillRating(key4Skills) >= SkillUtil.getMapSkillRating(key7Skills)

    val totalSkills = SkillUtil.collectScoreSkills(weightedMap.map { it.value })

    return if (isPrefer4Key) {
        getDanFromFavor(totalSkills, listOf(DanType.DDMYTHICAL_REFORM, DanType.UNDERJOY_LN))
    } else {
        getDanFromFavor(totalSkills, listOf(DanType.JINJIN_REGULAR, DanType.JINJIN_LN))
    }
}

fun getDanFromBeatmap(skills: List<Double>, cs: Number? = null): Map<String, Any> {
    return if ((cs?.toDouble() ?: 4.0) < 5.5) {
        getDanFromFavor(skills, listOf(DanType.DDMYTHICAL_REFORM, DanType.UNDERJOY_LN))
    } else {
        getDanFromFavor(skills, listOf(DanType.JINJIN_REGULAR, DanType.JINJIN_LN))
    }
}

fun getDanResult(
    skills: List<Double>,
    danType: DanType = DanType.DDMYTHICAL_REFORM,
): DanResult {
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
        return DanResult(name, (boundary.size + baseGradeOffset).toDouble(), grades.last() + "+")
    } else if (index >= boundary.size - 1) {
        return DanResult(name, baseGrade.toDouble(), grades.last())
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
    return DanResult(name, (baseGrade + fraction), grade)
}

enum class DanType {
    DDMYTHICAL_REFORM, UNDERJOY_LN, JINJIN_REGULAR, JINJIN_LN;

    fun getDan(): Dan = when (this) {
        DDMYTHICAL_REFORM -> DDMythicalReformDan()
        UNDERJOY_LN -> UnderjoyLnDan()
        JINJIN_REGULAR -> JinjinRegularDan()
        JINJIN_LN -> JinjinLnDan()
    }
}

class DDMythicalReformDan : Dan(
    "reform",
    listOf(
        0.0, 1.5, 2.5, 3.5,
        3.9, 4.2, 4.5, 4.8, 5.1,
        5.4, 5.7, 6.0, 6.5, 7.0,
        7.5, 8.0, 8.5, 9.0, 9.5,
        10.0, 10.8, 11.6
    ),
    listOf(
        "-",
        ".1", ".2", ".3",
        "1", "2", "3", "4", "5",
        "6", "7", "8", "9", "10",
        "A", "B", "G", "D", "E",
        "Z", "H", "S"
    ),
    12.0,
    -3,
    listOf(1, 2, 3)
)

class JinjinRegularDan : Dan(
    "regular",
    listOf(
        0.0, 5.0,
        5.5, 6.0, 6.5, 7.0, 7.5,
        8.0, 8.5, 9.0, 9.4, 9.8,
        10.2, 10.6, 11.0, 11.8
    ),
    listOf(
        "-",
        "0",
        "1", "2", "3", "4", "5",
        "6", "7", "8", "9", "10",
        "G", "A", "Z", "S",
    ),
    12.4,
    -1,
    listOf(1, 2, 3)
)

class UnderjoyLnDan : Dan(
    "underjoy",
    listOf(
        0.0,
        4.0, 4.5, 5.0, 5.5, 6.0,
        6.5, 7.0, 7.4, 7.7, 8.0,
        8.3, 8.6, 8.8
    ),
    listOf(
        "-",
        "1", "2", "3", "4", "5",
        "6", "7", "8", "9", "10",
        "11", "12", "13"
    ),
    9.0,
    0,
    listOf(4, 5, 6)
)

class JinjinLnDan : Dan(
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