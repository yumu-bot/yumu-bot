package com.now.nowbot.model.skill

import com.now.nowbot.model.beatmapParse.HitObject
import com.now.nowbot.model.beatmapParse.hitObject.HitObjectType.CIRCLE
import com.now.nowbot.model.beatmapParse.hitObject.HitObjectType.LONGNOTE
import com.now.nowbot.model.beatmapParse.parse.ManiaBeatmapAttributes
import kotlin.math.*

class SkillMania(val file: ManiaBeatmapAttributes): Skill() {
    override val values: List<Float>
        get() = listOf(
            (eval(noteDataList.map { it.stream }, 0.23f, 0.5f)
                    + eval(noteDataList.map { it.jack }, 0.05f, 0.69f)
                    ) / 2f,

            (eval(noteDataList.map { it.release }, 0.15f, 0.58f)
                    + eval(noteDataList.map { it.shield }, 0.25f, 0.51f)
                    + eval(noteDataList.map { it.reverseShield },0.43f, 0.5f)
                    ) / 3f,

            (eval(noteDataList.map { it.bracket }, 0.28f, 0.54f)
                    + eval(noteDataList.map { it.handLock }, 0.19f, 0.78f)
                    + eval(noteDataList.map { it.overlap },0.35f, 0.54f)
                    ) / 3f,

            (eval(noteDataList.map { it.grace }, 0.4f, 0.53f)
                    + eval(noteDataList.map { it.delayedTail }, 0.26f, 0.52f)
                    ) / 2f,

            (eval(noteDataList.map { it.speedJack }, 0.22f, 0.56f)
                    + eval(noteDataList.map { it.trill }, 0.35f, 0.54f)
                    + 0.2f * ((noteDataList.maxOfOrNull { it.burst } ?: 0f) / 2.5f)
                    ) / 3f,

            (eval(noteDataList.map { it.riceDensity }, 0.05f, 0.71f)
                    + eval(noteDataList.map { it.lnDensity }, 0.07f, 0.73f)
                    ) / 2f,
        )
    override val bases: List<Float>
        get() = listOf(
            eval(noteDataList.map { it.stream }),
            eval(noteDataList.map { it.jack }),

            eval(noteDataList.map { it.release }),
            eval(noteDataList.map { it.shield }),
            eval(noteDataList.map { it.reverseShield }),

            eval(noteDataList.map { it.bracket }),
            eval(noteDataList.map { it.handLock }),
            eval(noteDataList.map { it.overlap }),

            eval(noteDataList.map { it.grace }),
            eval(noteDataList.map { it.delayedTail }),

            eval(noteDataList.map { it.speedJack }),
            eval(noteDataList.map { it.trill }),
            (noteDataList.maxOfOrNull { it.burst } ?: 0f) / 2.5f,

            eval(noteDataList.map { it.riceDensity }),
            eval(noteDataList.map { it.lnDensity }),

            decreaseLowBurst((valueDataList.maxOfOrNull { it.burst } ?: 0f) / 2.5f),
            getLengthIndex(file.length),
            /*

            eval(noteDataList.map { it.bump }),
            eval(noteDataList.map { it.stop }),
            eval(noteDataList.map { it.fastJam }),
            eval(noteDataList.map { it.slowJam }),
            eval(noteDataList.map { it.teleport }),
            eval(noteDataList.map { it.negative }),

             */
        )
    override val names: List<String>
        get() = arrayListOf("rice", "long note", "coordination", "stamina", "speed", "precision", "speed variation")
    override val abbreviates: List<String>
        get() = arrayListOf("RC", "LN", "CO", "PR", "SP", "ST", "SV")
    override val star: Float
        get() {
            val stars = values
                .take(6)
                .sortedDescending()

            return stars[0] * 0.5f + stars[1] * 0.3f + stars[2] * 0.2f + stars[3] * 0.1f + stars[4] * 0.05f

            /*
            var star = 0f
            val powers = listOf(0.8f, 0.8f, 0.8f, 0.4f, 0.6f, 1.2f)

            for (i in 0 ..< 6) {
                star += powers[i] * values[i]
            }

            star /= 3.2f

            return 0.6f * star + 0.4f * values.sorted()[4]

             */
        }

    private val valueDataList: MutableList<ValueData> = mutableListOf()

    // 仅供测试
    private val noteDataList: MutableList<NoteData> = mutableListOf()

    data class ValueData(
        var rice: Float = 0f,
        var ln: Float = 0f,
        var coordination: Float = 0f,
        var stamina: Float = 0f,
        var speed: Float = 0f,
        var precision: Float = 0f,
        var sv: Float = 0f,

        var burst: Float = 0f
    ) {
        fun add(data: ValueData): ValueData {
            this.rice += data.rice
            this.ln += data.ln
            this.coordination += data.coordination
            this.stamina += data.stamina
            this.speed += data.speed
            this.precision += data.precision
            this.sv += data.sv

            this.burst += data.burst

            return this
        }
    }

    data class NoteData(
        var stream: Float = 0f,
        var jack: Float = 0f,

        var release: Float = 0f,
        var shield: Float = 0f,
        var reverseShield: Float = 0f,

        var bracket: Float = 0f,
        var handLock: Float = 0f,
        var overlap: Float = 0f,

        var riceDensity: Float = 0f,
        var lnDensity: Float = 0f,

        var speedJack: Float = 0f,
        var trill: Float = 0f,
        var burst: Float = 0f,

        var grace: Float = 0f,
        var delayedTail: Float = 0f,

        var bump: Float = 0f,
        var stop: Float = 0f,
        var fastJam: Float = 0f,
        var slowJam: Float = 0f,
        var teleport: Float = 0f,
        var negative: Float = 0f,
    ) {
        fun add(data: NoteData): NoteData {
            this.stream += data.stream
            this.jack += data.jack

            this.release += data.release
            this.shield += data.shield
            this.reverseShield += data.reverseShield

            this.bracket += data.bracket
            this.handLock += data.handLock
            this.overlap += data.overlap

            this.riceDensity += data.riceDensity
            this.lnDensity += data.lnDensity

            this.speedJack += data.speedJack
            this.trill += data.trill
            this.burst = max(this.burst, data.burst)

            this.grace += data.grace
            this.delayedTail += data.delayedTail

            this.bump += data.bump
            this.stop += data.stop
            this.fastJam += data.fastJam
            this.slowJam += data.slowJam
            this.teleport += data.teleport
            this.negative += data.negative

            return this
        }

        fun clear(): NoteData {
            return NoteData()
        }
    }

    init {
        val hitObjects = file.hitObjects
        if (hitObjects.isNotEmpty()) {
            calculate(hitObjects)
        }
    }

    fun calculate(hitObjects: List<HitObject>) {
        // 初始化
        var nowTime = hitObjects.first().startTime
        var nextTime = hitObjects.first().startTime + calculateUnit
        var chord = 0

        val key = file.cs.toInt()

        val noteCategory = List(key) { mutableListOf<HitObject>() }

        // 遍历数据，并存储在 noteCategory 中
        for (h in hitObjects) {
            val column = h.column
            if (column > key) return

            noteCategory[column].add(h)
        }

        // 缓存
        var note = NoteData()

        // 遍历数据，开始计算
        for (h in hitObjects) {
            if (h.startTime <= nowTime + frac16 && chord < key) {
                chord++
            }

            // 重置现在时间
            nowTime = h.startTime

            val after = getAfterNote(h, noteCategory[h.column])

            // 根据不同轨道来计算邻轨
            if (key == 1) {
                note.add(calcNote(h, after = after))
            } else {
                when(h.column) {
                    0 ->
                        note.add(calcNote(h, right = getNearestNote(h, noteCategory[1]), after = after))
                    (key - 1) ->
                        note.add(calcNote(h, left = getNearestNote(h, noteCategory[h.column - 1]), after = after))
                    in 1..< key ->
                        note.add(calcNote(h, left = getNearestNote(h, noteCategory[h.column - 1]), right = getNearestNote(h, noteCategory[h.column + 1]), after = after))
                    else ->
                        note.add(NoteData())
                }
            }

            note.burst ++

            // 计算元已满足要求，收集数据输出
            if (nowTime >= nextTime || h == hitObjects.last()) {
                // 重置计算单元
                do {
                    nextTime += calculateUnit
                } while (nowTime >= nextTime && h != hitObjects.last())

                val value = ValueData()
                value.burst = max(note.burst, value.burst)

                value.rice +=
                    sqrt(chord.toFloat()) * note.stream + note.jack
                value.ln +=
                    sqrt(chord.toFloat()) * note.release + note.shield + note.reverseShield
                value.coordination +=
                    note.bracket + 10f * note.handLock + 10f * note.overlap
                value.stamina +=
                    note.riceDensity + note.lnDensity
                value.speed +=
                    note.speedJack + note.trill
                value.precision +=
                    note.grace + affectedByOD(note.delayedTail, file.od.toFloat())
                value.sv +=
                    note.bump + note.fastJam + note.slowJam + note.stop + note.teleport + note.negative

                valueDataList.add(value)
                noteDataList.add(note)

                chord = 0
                note = NoteData()
            }

        }


    }

    // 计算物件
    private fun calcNote(it: HitObject, left: HitObject? = null, right: HitObject? = null, after: HitObject? = null): NoteData {
        val data = NoteData()

        data.add(calcThis(it))

        if (after != null) data.add(calcAfter(it, after))

        if (left != null) data.add(calcAside(it, left))
        if (right != null) data.add(calcAside(it, right))

        if (left != null && right != null) data.add(calcBetween(it, left, right))

        return data
    }

    // 返回此物件的一些特征值
    private fun calcThis(it: HitObject): NoteData {
        val data = NoteData()

        when(it.type) {
            CIRCLE -> data.riceDensity ++
            LONGNOTE -> data.lnDensity += calcLnDensity(it.startTime, it.endTime)
            else -> {}
        }

        return data
    }

    // 比较该物件和同轨道的下一个物件
    private fun calcAfter(it: HitObject, after: HitObject): NoteData {
        val data = NoteData()

        when(it.type) {
            CIRCLE -> {
                when(after.type) {
                    CIRCLE -> {
                        data.jack += calcJack(it.startTime, after.startTime)
                    }

                    LONGNOTE -> {
                        data.reverseShield += calcReverseShield(it.startTime, after.startTime)
                    }
                    else -> {}
                }

                data.speedJack += calcSpeedJack(it.startTime, after.startTime)

            }

            LONGNOTE -> {
                data.shield += calcShield(it.endTime, after.startTime)
            }

            else -> {}
        }

        return data
    }


    // 比较该物件和周围轨道的下一个物件
    private fun calcBetween(it: HitObject, left: HitObject, right: HitObject): NoteData {
        val data = NoteData()

        data.trill += calcTrill(it.startTime, left.startTime, right.startTime)

        data.bracket += calcBracket(it.startTime, left.startTime, right.startTime)

        return data
    }


    // 比较该物件和附近轨道的下一个物件
    private fun calcAside(it: HitObject, aside: HitObject): NoteData {
        val data = NoteData()

        when(it.type) {
            CIRCLE -> {
                when(aside.type) {
                    CIRCLE -> {
                        data.stream += calcStream(it.startTime, aside.startTime)

                        data.grace += calcGrace(it.startTime, aside.startTime)
                    }

                    LONGNOTE -> {
                        data.handLock += calcHandLock(it.startTime, aside.startTime, aside.endTime)

                        data.release += calcStream(it.startTime, aside.endTime)
                    }

                    else -> {}

                }
            }

            LONGNOTE -> {
                when(aside.type) {
                    CIRCLE -> {} // 避免重复计算

                    LONGNOTE -> {
                        data.release += calcStream(it.endTime, aside.endTime)

                        data.delayedTail += calcDelayedTail(it.endTime, aside.endTime)

                        data.overlap += calcOverlap(it.startTime, it.endTime, aside.startTime, aside.endTime)
                    }

                    else -> {}
                }
            }

            else -> {}
        }

        return data
    }



    // 返回这个物件与周围这一组物件对比，最靠上的物件，或是被 LN 包围的这个 LN，使用二分法查询
    private fun getNearestNote(now: HitObject, asideColumn: List<HitObject>?): HitObject? {
        if (asideColumn.isNullOrEmpty()) return null

        val n = now.startTime

        var min = 0
        var max = asideColumn.size - 1

        // 筛除超过区域的物件
        if (asideColumn.first().startTime > n) return asideColumn.first()
        if (asideColumn.last().endTime < n) return null

        while (max - min > 1) {
            val mid = (min + max) / 2
            val m = asideColumn[mid].startTime

            if (n < m) {
                max = mid
            } else {
                //n >= m
                min = mid
            }
        }

        val xs = asideColumn[max].startTime
        val xe = asideColumn[max].endTime
        val ms = asideColumn[min].startTime
        val me = asideColumn[min].endTime

        if (min == asideColumn.size - 1) return null

        if (n >= ms) {
            if (n <= me) return asideColumn[min]
            if (n <= xs || n <= xe) return asideColumn[max]
        }

        return null
    }

    // 获取同轨道之后的物件，使用二分法查询
    /*
    private fun getAfterNote(now: HitObject, thisColumn: List<HitObject>): HitObject? {
        val n = now.startTime

        var min = 0
        var max = thisColumn.size - 1

        // 筛除超过区域的物件（最后一个）
        if (thisColumn.last().startTime == n) return null

        while (max - min > 1) {
            val mid = (min + max) / 2
            val m = thisColumn[mid].startTime

            if (n < m) {
                max = mid
            } else {
                //n >= m
                min = mid
            }
        }

        return thisColumn[max]
    }

     */

    // 获取同轨道之后的物件，使用二分法查询
    private fun getAfterNote(now: HitObject, thisColumn: List<HitObject>): HitObject? {
        val n = now.startTime

        // 使用 binarySearchBy 简化代码
        val index = thisColumn.binarySearchBy(n) { it.startTime }

        val insertionPoint = if (index >= 0) index + 1 else -index - 1

        return thisColumn.getOrNull(insertionPoint)
    }


    // 计算

    private fun calcStream(it: Int, aside: Int): Float {
        return exponent(
            abs(aside - it).toFloat(), frac4, frac1
        )
    }

    private fun calcBracket(it: Int, left: Int, right: Int): Float {
        val lx = abs(left - it)
        val rx = abs(right - it)

        return if (lx <= frac2 && rx <= frac2) {
            (exponent(lx.toFloat(), frac4, frac1) + exponent(rx.toFloat(), frac4, frac1)) / 2f
        } else {
            0f
        }
    }

    private fun calcGrace(it: Int, aside: Int): Float {
        return exponent(abs(aside - it).toFloat(), frac12, frac6)
    }

    private fun calcDelayedTail(it: Int, aside: Int): Float {
        return exponent(abs(aside - it).toFloat(), frac6, frac3)
    }

    private fun calcJack(it: Int, after: Int): Float {
        return inverse(abs(after - it).toFloat(), frac2, frac1, frac16)
    }

    private fun calcShield(it: Int, after: Int): Float {
        return inverse(abs(after - it).toFloat(), frac4, frac1, frac16)
    }

    private fun calcReverseShield(it: Int, after: Int): Float {
        return inverse(abs(after - it).toFloat(), frac2, frac1, frac16)
    }

    private fun calcSpeedJack(it: Int, after: Int): Float {
        return inverse(abs(after - it).toFloat(), frac4, frac2, frac16)
    }

    private fun calcHandLock(it: Int, aside: Int, asideRelease: Int): Float {
        return if (asideRelease - frac8 > it && aside < it - frac8 && aside > 0f && asideRelease > 0f) {
            1f
        } else 0f
    }

    private fun calcOverlap(it: Int, itRelease: Int, aside: Int, asideRelease: Int): Float {
        // 只有类似S或Z类型的才可算有效重叠
        return if ((asideRelease > itRelease && aside > it) || (asideRelease < itRelease && aside < it)) {
            val overlap = min(asideRelease, itRelease) - max(it, aside)

            if (overlap > 0) {
                approach(overlap.toFloat(), frac4)
            } else {
                0f
            }
        } else {
            0f
        }
    }

    private fun calcLnDensity(it: Int, release: Int): Float {
        val delta = abs(release - it)

        return approach(delta.toFloat(), frac2)
    }

    private fun calcTrill(it: Int, left: Int, right: Int): Float {
        val lx = abs(left - it)
        val rx = abs(right - it)

        return if (lx < frac8 && rx >= frac8) {
            exponent(rx.toFloat(), frac4, frac1)
        } else if (lx >= frac8 && rx < frac8) {
            exponent(lx.toFloat(), frac4, frac1)
        } else {
            0f
        }
    }

    // 由于爆发是记录的最大值，在低难度内，爆发需要缩减至 0。
    private fun decreaseLowBurst(burst: Float): Float {
        if (burst >= 15f) return burst

        return burst * (Math.E * (burst / 15f) / exp(burst / 15f)).toFloat()
    }


    // 获取长度因数。一般认为长度 = 300s 的时候，大概是 0.95x
    private fun getLengthIndex(millis: Int): Float {
        return 1f - (1f / exp(millis / 100000f))
    }

    // 增强 OD 带来的影响。OD7: 1.0x, OD10: 4.48x
    private fun affectedByOD(value: Float, od: Float): Float {
        return value * exp(max(od - 7f, 0f) / 2f)
    }

    override fun toString(): String {
        return values.joinToString(separator = ",") { String.format("%.2f", it) } +
                "," +
                String.format("%.2f", star) +
                "," +
                bases.joinToString(separator = ",") { String.format("%.2f", it) }
    }
}