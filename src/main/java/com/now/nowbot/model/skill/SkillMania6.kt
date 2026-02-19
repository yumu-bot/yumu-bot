package com.now.nowbot.model.skill

import com.now.nowbot.model.beatmapParse.HitObject
import com.now.nowbot.model.beatmapParse.hitObject.HitObjectType
import com.now.nowbot.model.beatmapParse.parse.ManiaBeatmapAttributes
import com.now.nowbot.model.skill.SkillMania6.Hand.*
import com.now.nowbot.model.skill.SkillMania6.Finger.*
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class SkillMania6(attr: ManiaBeatmapAttributes, val isIIDXStyle: Boolean = true, spaceStyle: Byte = 2) : Skill6() {

    private val totalKey: Int = attr.cs.toInt()

    private val sortedNotes: MutableList<HitObject> = attr.hitObjects

    init {
        sortedNotes.sortBy { it.column }
        sortedNotes.sortBy { it.startTime }
    }

    private enum class Hand {
        LEFT, RIGHT, BOTH
    }

    private enum class Finger {
        THUMB,
        INDEX,
        MIDDLE,
        RING,
        PINKY;

        companion object {
            // 相同键本来用不到，但是有些多 K 可能存在同键不同指的情况
            private val gestureResults = arrayOf(
                1.0,
                1.1, 1.0,
                1.1, 1.0, 1.0,
                1.2, 1.2, 1.4, 1.0,
                1.4, 1.2, 1.6, 1.8, 1.0
            )

            // 获取不同手指交互时的难度增益
            fun getGestureBonus(f1: Finger, f2: Finger): Double {
                val i = maxOf(f1.ordinal, f2.ordinal)
                val j = minOf(f1.ordinal, f2.ordinal)

                // 三角阵索引公式
                val index = (i * (i + 1)) / 2 + j
                return gestureResults[index]
            }
        }
    }

    private enum class Type {
        RICE, LN
    }

    /**
     * 最小的计量单位：一次操作（List<ManiaAction>）
     */
    private data class ManiaAction(
        val finger: Finger,
        val hand: Hand,
        val column: Int,
        val startTime: Int,
        val endTime: Int,
        val type: Type,
    )

    private fun getSpacePair(spaceStyle: Byte): Pair<Hand, Finger> {
        return when (spaceStyle) {
            2.toByte() -> RIGHT
            1.toByte() -> LEFT
            else -> BOTH
        } to THUMB
    }

    private val playStyle: List<Pair<Hand, Finger>> = when (totalKey) {
        1 -> listOf(BOTH to THUMB)
        2 -> listOf(
            LEFT to INDEX,
            RIGHT to INDEX,
        )

        3 -> listOf(
            LEFT to INDEX,
            getSpacePair(spaceStyle),
            RIGHT to INDEX,
        )

        4 -> listOf(
            LEFT to MIDDLE,
            LEFT to INDEX,
            RIGHT to INDEX,
            RIGHT to MIDDLE,
        )

        5 -> listOf(
            LEFT to MIDDLE,
            LEFT to INDEX,
            getSpacePair(spaceStyle),
            RIGHT to INDEX,
            RIGHT to MIDDLE,
        )

        6 -> listOf(
            LEFT to RING,
            LEFT to MIDDLE,
            LEFT to INDEX,
            RIGHT to INDEX,
            RIGHT to MIDDLE,
            RIGHT to RING,
        )

        7 -> listOf(
            LEFT to RING,
            LEFT to MIDDLE,
            LEFT to INDEX,
            getSpacePair(spaceStyle),
            RIGHT to INDEX,
            RIGHT to MIDDLE,
            RIGHT to RING,
        )

        8 -> if (isIIDXStyle) {
            when (spaceStyle) {
                2.toByte() -> {
                    listOf(
                        RIGHT to PINKY,
                        LEFT to RING,
                        LEFT to MIDDLE,
                        LEFT to INDEX,
                        getSpacePair(spaceStyle),
                        RIGHT to INDEX,
                        RIGHT to MIDDLE,
                        RIGHT to RING,
                    )
                }

                else -> listOf(
                    LEFT to PINKY,
                    LEFT to RING,
                    LEFT to MIDDLE,
                    LEFT to INDEX,
                    getSpacePair(spaceStyle),
                    RIGHT to INDEX,
                    RIGHT to MIDDLE,
                    RIGHT to RING,
                )
            }
        } else {
            listOf(
                LEFT to PINKY,
                LEFT to RING,
                LEFT to MIDDLE,
                LEFT to INDEX,
                RIGHT to INDEX,
                RIGHT to MIDDLE,
                RIGHT to RING,
                RIGHT to PINKY,
            )
        }

        9 -> listOf(
            LEFT to PINKY,
            LEFT to RING,
            LEFT to MIDDLE,
            LEFT to INDEX,
            getSpacePair(spaceStyle),
            RIGHT to INDEX,
            RIGHT to MIDDLE,
            RIGHT to RING,
            RIGHT to PINKY,
        )

        10 -> listOf(
            LEFT to PINKY,
            LEFT to RING,
            LEFT to MIDDLE,
            LEFT to INDEX,
            LEFT to THUMB,
            RIGHT to THUMB,
            RIGHT to INDEX,
            RIGHT to MIDDLE,
            RIGHT to RING,
            RIGHT to PINKY,
        )

        // 10K2S
        12 -> listOf(
            LEFT to PINKY,
            LEFT to PINKY,
            LEFT to RING,
            LEFT to MIDDLE,
            LEFT to INDEX,
            LEFT to THUMB,
            RIGHT to THUMB,
            RIGHT to INDEX,
            RIGHT to MIDDLE,
            RIGHT to RING,
            RIGHT to PINKY,
            RIGHT to PINKY,
        )

        14 -> when (spaceStyle) {

            // DP 14
            1.toByte() -> listOf(
                LEFT to PINKY,
                LEFT to RING,
                LEFT to PINKY,
                LEFT to MIDDLE,
                LEFT to THUMB,
                LEFT to INDEX,
                LEFT to THUMB,

                RIGHT to THUMB,
                RIGHT to INDEX,
                RIGHT to THUMB,
                RIGHT to MIDDLE,
                RIGHT to PINKY,
                RIGHT to RING,
                RIGHT to PINKY,
            )

            // EZ2AC 14
            else -> listOf(
                LEFT to PINKY,
                LEFT to RING,
                LEFT to MIDDLE,
                LEFT to INDEX,
                LEFT to THUMB,

                BOTH to INDEX,
                BOTH to INDEX,
                BOTH to INDEX,
                BOTH to INDEX,

                RIGHT to THUMB,
                RIGHT to INDEX,
                RIGHT to MIDDLE,
                RIGHT to RING,
                RIGHT to PINKY,
            )
        }

        16 -> when (spaceStyle) {

            // DP 16
            1.toByte() -> listOf(
                LEFT to PINKY,
                LEFT to PINKY,
                LEFT to RING,
                LEFT to PINKY,
                LEFT to MIDDLE,
                LEFT to THUMB,
                LEFT to INDEX,
                LEFT to THUMB,

                RIGHT to THUMB,
                RIGHT to INDEX,
                RIGHT to THUMB,
                RIGHT to MIDDLE,
                RIGHT to PINKY,
                RIGHT to RING,
                RIGHT to PINKY,
                RIGHT to PINKY,
            )

            // EZ2AC 16
            else -> listOf(
                LEFT to PINKY,
                LEFT to PINKY,
                LEFT to RING,
                LEFT to MIDDLE,
                LEFT to INDEX,
                LEFT to THUMB,

                BOTH to INDEX,
                BOTH to INDEX,
                BOTH to INDEX,
                BOTH to INDEX,

                RIGHT to THUMB,
                RIGHT to INDEX,
                RIGHT to MIDDLE,
                RIGHT to RING,
                RIGHT to PINKY,
                RIGHT to PINKY,
            )
        }

        18 -> when (spaceStyle) {
            // 10K8K
            1.toByte() -> listOf(
                LEFT to PINKY,
                LEFT to RING,
                LEFT to MIDDLE,
                LEFT to INDEX,

                LEFT to PINKY,
                LEFT to RING,
                LEFT to MIDDLE,
                LEFT to INDEX,
                LEFT to THUMB,

                RIGHT to THUMB,
                RIGHT to INDEX,
                RIGHT to MIDDLE,
                RIGHT to RING,
                RIGHT to PINKY,

                RIGHT to INDEX,
                RIGHT to MIDDLE,
                RIGHT to RING,
                RIGHT to PINKY,
            )

            // 9K9K
            else -> listOf(
                LEFT to PINKY,
                LEFT to PINKY,
                LEFT to RING,
                LEFT to RING,
                LEFT to MIDDLE,
                LEFT to MIDDLE,
                LEFT to INDEX,
                LEFT to INDEX,
                LEFT to THUMB,

                RIGHT to THUMB,
                RIGHT to INDEX,
                RIGHT to INDEX,
                RIGHT to MIDDLE,
                RIGHT to MIDDLE,
                RIGHT to RING,
                RIGHT to RING,
                RIGHT to PINKY,
                RIGHT to PINKY,
            )
        }

        else -> List(totalKey) { BOTH to INDEX }
    }

    /**
     * @param sortedObjects 这里的物件必须按时间以及大小排序
     */
    private fun objectsToActions(
        sortedObjects: List<HitObject>,
        threshold: Double = frac8
    ): List<List<ManiaAction>> {
        if (sortedObjects.isEmpty()) return emptyList()

        val actions = mutableListOf<List<ManiaAction>>()
        val currentBatch = mutableListOf<HitObject>()

        for (obj in sortedObjects) {
            if (currentBatch.isEmpty()) {
                currentBatch.add(obj)
                continue
            }

            // 核心判定逻辑：
            // 1. 轨道是否重复
            val isTrackConflict = currentBatch.any { it.column == obj.column }
            // 2. 时间差是否超过了当前组内最大的时间（即最后一个音符的时间）
            val isTimeOut = (obj.startTime - (currentBatch.last().startTime)) > threshold

            if (isTrackConflict || isTimeOut) {
                // 归纳当前组
                actions.add(batchToAction(currentBatch))
                currentBatch.clear()
            }

            currentBatch.add(obj)
        }

        // 处理收尾
        if (currentBatch.isNotEmpty()) {
            actions.add(batchToAction(currentBatch))
        }

        return actions
    }

    private fun batchToAction(batch: MutableList<HitObject>): List<ManiaAction> {
        return batch.map { hit ->

            val ps = playStyle[hit.column]
            val type = if (hit.type == HitObjectType.CIRCLE ||
                (hit.endTime - hit.startTime) <= frac12
            ) {
                Type.RICE
            } else {
                Type.LN
            }

            // 将太短的 ln 看成 rice
            val endTime = if (type == Type.RICE) {
                hit.startTime
            } else {
                hit.endTime
            }

            ManiaAction(
                ps.second,
                ps.first,
                hit.column,
                hit.startTime,
                endTime,
                type
            )
        }
    }

    enum class NoteType {
        STREAM, BRACKET, JACK, // Rice, S, B, J
        RELEASE, SHIELD, REVERSE_SHIELD, // LN, R, E, V
        HAND_LOCK, OVERLAP, // CO, H, O
        GRACE, DELAYED_TAIL, // PR, G, Y
        CHORD, TRILL, BURST, // SP, C, T, U
        FATIGUE // ST, F
        ;

        val index: Int = ordinal

        val eval: List<List<Double>> = listOf(
            listOf(3.0, 0.5), listOf(3.5, 0.8), listOf(3.0, 0.5),

            listOf(2.55, 0.615), listOf(4.24, 0.457), listOf(4.865, 0.53),

            listOf(3.633, 0.332), listOf(3.5, 0.282),

            listOf(2.0, 0.606), listOf(0.69, 1.15),

            listOf(0.7, 1.2), listOf(0.5, 1.0), listOf(0.05, 0.95),

            listOf(0.002, 0.886)
        )
    }

    private fun noteDataToSubValue(grouped: List<NoteData>): List<Double> {
        return NoteType.entries.map { type ->
            val e = type.eval[type.index]

            if (type != NoteType.BURST) {
                grouped.map { noteData ->
                    noteData.get(type)
                }.aggregate().eval(e.getOrNull(0) ?: 1.0, e.getOrNull(1) ?: 1.0)
            } else {
                (grouped.maxOfOrNull { noteData -> noteData.get(NoteType.BURST) } ?: 0.0).eval(e.getOrNull(0) ?: 1.0, e.getOrNull(1) ?: 1.0)
            }
        }
    }

    private fun groupingNoteData(allNotes: List<NoteData>): List<NoteData> {
        if (allNotes.isEmpty()) return emptyList()

        val windowSize = calculateUnit

        // 1. 找到时间的边界
        val startTime = allNotes.minOf { it.time }
        val endTime = allNotes.maxOf { it.time }

        // 2. 先按 Index 分组，方便后续查找
        val groupedMap = allNotes.groupBy { it.time / windowSize }

        // 3. 按照时间轴从开始到结束，生成完整的窗口列表
        val startWindow = startTime / windowSize
        val endWindow = endTime / windowSize

        val result = mutableListOf<NoteData>()

        for (windowIndex in startWindow..endWindow) {
            val notesInWindow = groupedMap[windowIndex]

            if (notesInWindow == null) {
                // --- 处理空段落 ---
                // 创建一个“零值”或“空值”的 NoteData
                val emptyData = NoteData().apply {
                    time = windowIndex * windowSize
                    // 这里不需要 add 或 divide，保持默认初始状态即可
                }
                result.add(emptyData)
            } else {
                // --- 处理有数据的段落 ---
                val sumData = NoteData().apply {
                    time = windowIndex * windowSize
                }

                for (note in notesInWindow) {
                    sumData.add(note)
                }

                val count = notesInWindow.size.toDouble()
                if (count > 0) {
                    sumData.divide(count)
                }
                result.add(sumData)
            }
        }

        return result
    }

    private fun actionsToNoteData(actions: List<List<ManiaAction>>): List<NoteData> {
        val legacy: MutableList<ManiaAction> = mutableListOf()
        var burstBefore = 0.0
        var fatigueBefore = 0.0

        return actions.zipWithNext { it, after ->
            val minStartTime = it.minOfOrNull { it.startTime } ?: 0

            legacy.removeIf { l -> l.endTime < minStartTime }

            val (data, ly) = calculate(
                it, legacy, after, burstBefore, fatigueBefore
            )

            burstBefore = data.burst
            fatigueBefore = data.fatigue

            legacy.addAll(ly)

            data
        }
    }

    /**
     * @param actionBefore 可能遗漏的前一个操作的 ln 操作，当且仅当这个 ln 的持续时间长于 actionAfter 的触发时间时
     */
    private fun calculate(
        action: List<ManiaAction>,
        actionBefore: List<ManiaAction>,
        actionAfter: List<ManiaAction>,
        burstBefore: Double = 0.0,
        fatigueBefore: Double = 0.0,
    ): Pair<NoteData, List<ManiaAction>> {
        val data = NoteData()
        val legacy = mutableListOf<ManiaAction>()

        val afterEndTimeMax = actionAfter.maxOfOrNull { it.endTime } ?: 0
        val afterStartTimeMax = actionAfter.maxOfOrNull { it.startTime } ?: 0
        val startTimeMax = action.maxOfOrNull { it.startTime } ?: 0
        val startTimeMin = action.minOfOrNull { it.startTime } ?: 0

        val chord = action.size
        val chordBonus = calculateChord(chord, totalKey)

        // 分键操作
        action.forEach {
            val leftBefore: ManiaAction? = actionBefore.find { lb ->
                lb.column == it.column - 1
            }

            val leftAfter: ManiaAction? = actionAfter.find { la ->
                la.column == it.column - 1
            }

            val itAfter: ManiaAction? = actionAfter.find { ia ->
                ia.column == it.column
            }

            val rightBefore: ManiaAction? = actionBefore.find { rb ->
                rb.column == it.column + 1
            }

            val rightAfter: ManiaAction? = actionAfter.find { ra ->
                ra.column == it.column + 1
            }

            // 先收拾遗产

            if (leftBefore != null) {
                data.add(calculateAsideRelease(it, leftBefore))
            }

            if (rightBefore != null) {
                data.add(calculateAsideRelease(it, rightBefore))
            }

            if (itAfter != null) {
                data.add(
                    calculateAfter(it, itAfter)
                )
            } else {
                if (leftAfter != null) {
                    data.add(
                        calculateAsideHit(it, leftAfter, chordBonus)
                    )

                    if (rightAfter != null) {
                        // 双不为 null
                        data.add(
                            calculateBothSide(it, leftAfter, rightAfter)
                        )
                    }
                }

                if (rightAfter != null) {
                    data.add(
                        calculateAsideHit(it, rightAfter, chordBonus)
                    )
                }
            }

            if (leftAfter != null) {
                data.add(calculateAsideRelease(it, leftAfter))
            }

            if (rightAfter != null) {
                data.add(calculateAsideRelease(it, rightAfter))
            }

            if (it.type == Type.LN) {
                // LN
                // 可能有遗产
                if (it.endTime - frac8 > afterEndTimeMax) {
                    legacy.add(it)
                }
            }
        }

        action.zipWithNext { it, next ->
            if (next.type == Type.LN) {
                data.add(calculateAsideRelease(it, next))
            }
        }

        // 集合操作
        val graceDelta = startTimeMax - startTimeMin

        if (graceDelta >= frac8 && chord > 1) {
            action.zipWithNext { a, b ->
                data.grace += (a.startTime - b.startTime).exponent(frac8, frac4)
            }
        }

        // 耐力和爆发操作
        val delta = afterStartTimeMax - startTimeMax

        val burstDecay = 0.5.pow((delta / 1000.0) / BURST_RECOVERY_HALF_LIFE)
        val fatigueDecay = 0.5.pow((delta / 1000.0) / FATIGUE_RECOVERY_HALF_LIFE)

        data.fatigue = fatigueBefore * fatigueDecay + chordBonus
        data.burst = burstBefore * burstDecay + chordBonus

        data.chord = chordBonus / totalKey

        data.time = startTimeMin

        return data to legacy
    }

    private fun calculateAfter(it: ManiaAction, after: ManiaAction): NoteData {
        val data = NoteData()

        if (it.type == Type.RICE) {
            if (after.type == Type.RICE) {
                data.jack += (after.startTime - it.startTime)
                    .inverse(frac2, frac1, frac16)
            } else {
                data.reverseShield += (after.startTime - it.startTime)
                    .inverse(frac2, frac1, frac16)
            }
        } else {
            data.shield += (after.startTime - it.endTime)
                .inverse(frac4, frac1, frac16)
        }

        return data
    }

    /**
     * 要求：后面不能有音符，和 calcAfter 相对
     */
    private fun calculateAsideHit(it: ManiaAction, aside: ManiaAction, chordBonus: Double): NoteData {
        val data = NoteData()

        val bonus = Finger.getGestureBonus(
            it.finger, aside.finger
        )

        if (it.hand != aside.hand) {
            data.trill += chordBonus * (aside.startTime - it.startTime).exponent(frac4, frac1)
        } else {
            data.stream += bonus * (aside.startTime - it.startTime).exponent(frac4, frac1)
        }

        return data
    }

    /**
     * @param aside 必须是 ln
     */
    private fun calculateAsideRelease(it: ManiaAction, aside: ManiaAction): NoteData {
        val data = NoteData()

        val bonus = Finger.getGestureBonus(
            it.finger, aside.finger
        )

        if (it.type == Type.RICE) {
            val isIn = aside.endTime - frac8 > it.startTime && aside.startTime + frac8 < it.startTime

            data.handLock += if (isIn) {
                bonus
            } else {
                0.0
            }
        } else {
            val overlap = min(aside.endTime, it.endTime) - max(aside.startTime, it.startTime)

            if (overlap > 0) {
                data.overlap += overlap.approach(frac2)
            }

            data.grace += (aside.startTime - it.startTime).exponent(frac8, frac4)

            if (aside.type == Type.LN) {
                data.release += (aside.endTime - it.endTime).exponent(frac4, frac1)
                data.delayedTail += (aside.endTime - it.endTime).exponent(frac6, frac3)
            }
        }

        return data
    }

    private fun calculateBothSide(it: ManiaAction, left: ManiaAction, right: ManiaAction): NoteData {
        val data = NoteData()

        val min = min(left.startTime, right.startTime)

        if (min > it.startTime + frac16) {
            data.bracket += (min - it.startTime).exponent(frac4, frac1)
        }

        return data
    }

    private fun calculateChord(chord: Int, totalKey: Int = 1): Double {
        if (chord !in 1..totalKey) return 0.0

        // 满足：f(1) = 1.0, f(6) = 3.0
        val cv = ln(chord + B) * K

        return if (chord == totalKey && totalKey > 4) {
            cv * 0.8
        } else {
            cv
        }
    }

    data class NoteData(
        // 使用数组存储所有值
        private val values: DoubleArray =
            DoubleArray(NoteType.entries.size) { 0.0 },
    ) {
        // 这个是定位用的
        var time: Int = 0

        // 使用属性委托
        var stream: Double by ArrayDelegate(NoteType.STREAM)
        var bracket: Double by ArrayDelegate(NoteType.BRACKET)
        var jack: Double by ArrayDelegate(NoteType.JACK)

        var release: Double by ArrayDelegate(NoteType.RELEASE)
        var shield: Double by ArrayDelegate(NoteType.SHIELD)
        var reverseShield: Double by ArrayDelegate(NoteType.REVERSE_SHIELD)

        var handLock: Double by ArrayDelegate(NoteType.HAND_LOCK)
        var overlap: Double by ArrayDelegate(NoteType.OVERLAP)

        var grace: Double by ArrayDelegate(NoteType.GRACE)
        var delayedTail: Double by ArrayDelegate(NoteType.DELAYED_TAIL)

        var chord: Double by ArrayDelegate(NoteType.CHORD)
        var trill: Double by ArrayDelegate(NoteType.TRILL)
        var burst: Double by ArrayDelegate(NoteType.BURST)

        var fatigue: Double by ArrayDelegate(NoteType.FATIGUE)

        fun add(other: NoteData): NoteData {
            for (i in values.indices) {
                this.values[i] += other.values[i]
            }
            return this
        }

        fun divide(divisor: Double): NoteData {
            for (i in values.indices) {
                this.values[i] /= divisor
            }
            return this
        }


        fun clear(): NoteData {
            this.values.fill(0.0)
            return this
        }

        fun flatten(): List<Double> {
            return values.toList()
        }

        fun get(field: NoteType): Double {
            return values[field.index]
        }

        // 内部类：实现属性委托，让访问 values[index] 像访问成员变量一样快
        private class ArrayDelegate(val field: NoteType) {
            operator fun getValue(thisRef: NoteData, property: Any?): Double = thisRef.values[field.index]
            operator fun setValue(thisRef: NoteData, property: Any?, value: Double) {
                thisRef.values[field.index] = value
            }
        }
    }

    private val acts = objectsToActions(sortedNotes)

    private val data = actionsToNoteData(acts)

    private val group = groupingNoteData(data)

    override val bases: List<Double> = noteDataToSubValue(group)

    override val skills: List<Double>
        get() = listOf(
            listOf(bases[0], bases[1], bases[2]).sortAndSum(),
            listOf(bases[3], bases[4], bases[5]).sortAndSum(),
            listOf(bases[6], bases[7]).sortAndSum(),
            listOf(bases[8], bases[9]).sortAndSum(),
            listOf(bases[10], bases[11], bases[12]).sortAndSum(),
            bases[13]
        )
    override val names: List<String>
        get() = arrayListOf("rice", "long note", "coordination", "precision", "speed", "stamina", "speed variation")
    override val abbreviates: List<String>
        get() = arrayListOf("RC", "LN", "CO", "PR", "SP", "ST", "SV")
    override val rating: Double
        get() {
            val sortedValues = skills.take(6).sortedDescending()
            if (sortedValues[0] <= 0) return 0.0

            val final = sortedValues.take(3).sortAndSum() + 0.1 * sortedValues.drop(3).average()

            return final
        }

    companion object {
        private const val B = 0.176
        private val K = 1.0 / ln(1.0 + B)

        private const val FATIGUE_RECOVERY_HALF_LIFE = 20.0
        private const val BURST_RECOVERY_HALF_LIFE = 2.0
    }
}