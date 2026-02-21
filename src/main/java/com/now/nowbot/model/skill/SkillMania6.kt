package com.now.nowbot.model.skill

import com.now.nowbot.model.beatmapParse.HitObject
import com.now.nowbot.model.beatmapParse.hitObject.HitObjectType
import com.now.nowbot.model.beatmapParse.parse.ManiaBeatmapAttributes
import com.now.nowbot.model.skill.SkillMania6.Hand.*
import com.now.nowbot.model.skill.SkillMania6.Finger.*
import org.slf4j.LoggerFactory
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class SkillMania6(attr: ManiaBeatmapAttributes, val isIIDXStyle: Boolean = true, spaceStyle: Byte = 2) : Skill6() {

    private val totalKey: Int = attr.cs.toInt()

    private val sortedNotes: MutableList<HitObject> = attr.hitObjects

    init {
        sortedNotes.sortBy { it.column }
        sortedNotes.sortBy { it.startTime }
    }

    private enum class Hand {
        LEFT, RIGHT, BOTH;

        companion object {
            fun getHandPunishment(it: Hand, that: Hand): Double {
                return if (it == that && it != BOTH) {
                    1.0
                } else {
                    0.5
                }
            }
        }
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
        FATIGUE, // ST, F
        TRILL, BURST, // SP, C, T, U
        RELEASE, SHIELD, REVERSE_SHIELD, // LN, R, E, V
        HAND_LOCK, OVERLAP, // CO, H, O
        GRACE, DELAYED_TAIL, // PR, G, Y
        ;

        val index: Int = ordinal

        val eval: List<List<Double>> = listOf(
            // S https://curve.fit/BIdo3Jm7/single/20260221115943
            // B https://curve.fit/BIdo3Jm7/single/20260221120019
            // J https://curve.fit/BIdo3Jm7/single/20260221120103
            listOf(1.461e+00, 5.878e-01), listOf(2.993e+00, 5.712e-01), listOf(2.188e+00, 5.928e-01),

            // F https://curve.fit/BIdo3Jm7/single/20260221113539
            listOf(1.310e-02, 9.376e-01),

            // T https://curve.fit/BIdo3Jm7/single/20260221060743
            // U https://curve.fit/BIdo3Jm7/single/20260221060902
            listOf(1.593e+00, 3.964e-01), listOf(1.307e-01, 8.938e-01),

            // R https://curve.fit/BIdo3Jm7/single/20260221113635
            // E https://curve.fit/BIdo3Jm7/single/20260221113733
            // V https://curve.fit/BIdo3Jm7/single/20260221113928
            listOf(4.384e+00, 4.746e-01), listOf(4.466e+00, 4.170e-01), listOf(3.596e+00, 5.749e-01),

            // H https://curve.fit/BIdo3Jm7/single/20260221112433
            // O https://curve.fit/BIdo3Jm7/single/20260221112543

            listOf(3.264e+00, 3.434e-01), listOf(5.536e+00, 3.438e-01),

            // G https://curve.fit/BIdo3Jm7/single/20260221114128
            // Y https://curve.fit/BIdo3Jm7/single/20260221114305
            listOf(1.423e+00, 6.051e-01), listOf(4.510e+00, 4.033e-01),
        )
    }

    private fun noteDataToSubValue(grouped: List<NoteData>): List<Double> {
        return NoteType.entries.map { type ->
            val e = type.eval[type.index]

            when (type) {
                NoteType.BURST -> {
                    (grouped.maxOfOrNull { noteData -> noteData.get(NoteType.BURST) } ?: 0.0)
                        .square(
                            e.getOrNull(0) ?: 1.0,
                            e.getOrNull(1) ?: 1.0
                        )
                }
                NoteType.FATIGUE -> {
                    (grouped.maxOfOrNull { noteData -> noteData.get(NoteType.FATIGUE) } ?: 0.0)
                        .square(
                        e.getOrNull(0) ?: 1.0,
                        e.getOrNull(1) ?: 1.0
                        )
                }
                else -> {
                    grouped.map { noteData ->
                        noteData.get(type)
                    }.aggregate().square(e.getOrNull(0) ?: 1.0, e.getOrNull(1) ?: 1.0)
                }
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
            val columnSet = it.map { a -> a.column }.toSet()

            legacy.removeIf { l -> l.endTime < (minStartTime + frac16) || l.column in columnSet }

            val data = calculate(
                it, legacy, after, burstBefore, fatigueBefore
            )

            legacy.addAll(it.filter { it.type == Type.LN })

            burstBefore = data.burst
            fatigueBefore = data.fatigue

            data
        }
    }

    /**
     * @param activeHoldings 可能遗漏的前一个操作的 ln 操作，当且仅当这个 ln 的持续时间长于 actionAfter 的触发时间时
     */
    private fun calculate(
        action: List<ManiaAction>,
        activeHoldings: List<ManiaAction>,
        actionAfter: List<ManiaAction>,
        burstBefore: Double = 0.0,
        fatigueBefore: Double = 0.0,
    ):NoteData {
        val data = NoteData()
        val afterStartTimeMax = actionAfter.maxOfOrNull { it.startTime } ?: 0
        val startTimeMax = action.maxOfOrNull { it.startTime } ?: 0
        val startTimeMin = action.minOfOrNull { it.startTime } ?: 0

        val chord = action.size
        val chordBonus = calculateChord(chord, totalKey)

        // 分键操作
        action.forEach {

            val leftAfterList: List<ManiaAction> = actionAfter.filter { la ->
                la.column < it.column
            }

            val leftAfter: ManiaAction? = leftAfterList.maxByOrNull { la ->
                la.column
            }

            val itAfter: ManiaAction? = actionAfter.find { ia ->
                ia.column == it.column
            }

            val rightAfterList: List<ManiaAction> = actionAfter.filter { ra ->
                ra.column > it.column
            }

            val rightAfter: ManiaAction? = rightAfterList.minByOrNull { ra ->
                ra.column
            }

            // 1. 只处理遗产：当前 Hit 与 之前已经在按住的 LN 产生的冲突
            activeHoldings.forEach { holding ->
                // 这里包含了 AsideRelease, Overlap, HandLock 等
                data.add(calculateAsideRelease(it, holding))
            }

            if (itAfter != null) {
                data.add(
                    calculateAfter(it, itAfter)
                )
            } else {
                leftAfterList.forEach { la ->
                    data.add(
                        calculateAsideHit(it, la, action.size, leftAfterList.size, chordBonus, totalKey)
                    )
                }

                rightAfterList.forEach { ra ->
                    data.add(
                        calculateAsideHit(it, ra, action.size, rightAfterList.size, chordBonus, totalKey)
                    )
                }

                if (leftAfter != null && rightAfter != null) {
                    // 双不为 null
                    data.add(
                        calculateBothSide(it, leftAfter, rightAfter)
                    )
                }
            }

        // --- 【删掉这里】 ---
        // leftAfterList.forEach { la -> data.add(calculateAsideRelease(it, la)) }
        // rightAfterList.forEach { ra -> data.add(calculateAsideRelease(it, ra)) }
        // 因为这些 la 和 ra 在下一帧作为 it 时，会通过 activeHoldings 找回现在的 it
        }

        action.zipWithNext { a, b ->
            if (a.type == Type.LN || b.type == Type.LN) {
                data.add(calculateAsideRelease(a, b))
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

        data.time = startTimeMin

        return data
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
    private fun calculateAsideHit(it: ManiaAction, aside: ManiaAction, itChord: Int, asideChord: Int, chordBonus: Double, totalKey: Int): NoteData {
        val data = NoteData()

        val bonus = Finger.getGestureBonus(
            it.finger, aside.finger
        )

        val punishment = Hand.getHandPunishment(it.hand, aside.hand)

        if (it.hand != aside.hand && (itChord + asideChord > 2 || totalKey < 4)) {
            data.trill += chordBonus * (aside.startTime - it.startTime).exponent(frac4, frac1)
        } else {
            data.stream += bonus * punishment * (aside.startTime - it.startTime).exponent(frac4, frac1)
        }

        data.grace += (aside.startTime - it.startTime).exponent(frac8, frac4)

        return data
    }

    /**
     * @param aside 必须是 ln
     */
    private fun calculateAsideRelease(it: ManiaAction, aside: ManiaAction): NoteData {
        val data = NoteData()

        if (aside.type != Type.LN) {
            return data
        }

        val bonus = Finger.getGestureBonus(
            it.finger, aside.finger
        )

        if (it.type == Type.RICE) {
            val endDelta = aside.endTime - it.startTime
            val startDelta = it.startTime - aside.startTime

            val isIn = endDelta > 0 && startDelta > 0

            val delta = sqrt(endDelta.approach(frac2) * startDelta.approach(frac2))

            data.handLock += if (isIn) {
                bonus * delta
            } else {
                0.0
            }
        } else {
            // 只看相邻轨道和同手
            if (abs(aside.column - it.column) == 1 && it.hand == aside.hand) {
                val pressDelta = abs(aside.startTime - it.startTime)
                val releaseDelta = abs(aside.endTime - it.endTime)
                val changeDelta = min(it.endTime, aside.endTime) - max(it.startTime, aside.startTime)

                val delta = if (changeDelta <= 0) {
                    0
                } else {
                    (pressDelta * releaseDelta * changeDelta * 1.0).pow(1.0/3.0).toInt()
                }

                val overlap = delta.exponent(frac2, 3 * frac2)

                data.overlap += overlap
            }

            data.release += (aside.endTime - it.endTime).exponent(frac4, frac1)
            data.delayedTail += (aside.endTime - it.endTime).exponent(frac6, frac3)
        }

        //

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

        // 满足：f(1) = 1.0, f(6) = 1.5
        val cv = ln(chord + B) * K

        return if (chord == totalKey && totalKey >= 4) {
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

        var fatigue: Double by ArrayDelegate(NoteType.FATIGUE)

        var trill: Double by ArrayDelegate(NoteType.TRILL)
        var burst: Double by ArrayDelegate(NoteType.BURST)

        var release: Double by ArrayDelegate(NoteType.RELEASE)
        var shield: Double by ArrayDelegate(NoteType.SHIELD)
        var reverseShield: Double by ArrayDelegate(NoteType.REVERSE_SHIELD)

        var handLock: Double by ArrayDelegate(NoteType.HAND_LOCK)
        var overlap: Double by ArrayDelegate(NoteType.OVERLAP)

        var grace: Double by ArrayDelegate(NoteType.GRACE)
        var delayedTail: Double by ArrayDelegate(NoteType.DELAYED_TAIL)

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

    override val graphs: List<List<Double>>
        get() = group.map { it.flatten() }

    override val skills: List<Double>
        get() = listOf(
            listOf(bases[0], bases[1], bases[2]).sortAndSum(),
            bases[3],
            listOf(bases[4], bases[5]).sortAndSum(),
            listOf(bases[6], bases[7], bases[8]).sortAndSum(),
            listOf(bases[9], bases[10]).sortAndSum(),
            listOf(bases[11], bases[12]).sortAndSum(),
        )

    override val names: List<String>
        get() = arrayListOf("rice", "stamina", "speed", "long note", "coordination", "precision", "speed variation")
    override val abbreviates: List<String>
        get() = arrayListOf("RC", "ST", "SP", "LN", "CO", "PR", "SV")
    override val rating: Double
        get() {
            val sorted = skills.take(6).sortedDescending()
            if (sorted[0] <= 0) return 0.0

            val final = sorted[1] * 0.6 + sorted[2] * 0.4 + sorted[3] * 0.2

            return final
        }

    override val dan: Map<String, Any> = getDan(skills, DanType.REFORM) + getDan(skills, DanType.LN)


    companion object {
        private const val B = 0.176
        private val K = 0.5 / ln(1.0 + B)

        private const val FATIGUE_RECOVERY_HALF_LIFE = 20.0
        private const val BURST_RECOVERY_HALF_LIFE = 2.0

        private val log = LoggerFactory.getLogger(SkillMania6::class.java)
    }
}
