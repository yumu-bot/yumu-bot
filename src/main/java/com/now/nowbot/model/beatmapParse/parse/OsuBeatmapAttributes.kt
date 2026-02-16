package com.now.nowbot.model.beatmapParse.parse

import com.now.nowbot.model.beatmapParse.HitObject
import com.now.nowbot.model.beatmapParse.Timing
import com.now.nowbot.model.beatmapParse.hitObject.HitObjectPosition
import com.now.nowbot.model.beatmapParse.hitObject.HitObjectType
import com.now.nowbot.model.beatmapParse.timing.TimingEffect
import com.now.nowbot.model.beatmapParse.timing.TimingSampleSet
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.util.ContextUtil
import com.now.nowbot.util.ContextUtil.setContext
import java.io.BufferedReader
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

open class OsuBeatmapAttributes(read: BufferedReader, general: BeatmapGeneral?) {
    var clockRate: Double = 1.0
        set(clockRate) {
            field = clockRate

            if (clockRate != 1.0 && clockRate > 0.0) {
                this.length = (this.length / clockRate).toInt()

                for (h in hitObjects) {
                    h.startTime = (h.startTime / clockRate).toInt()
                    h.endTime = (h.endTime / clockRate).toInt()
                }

                for (t in timings) {
                    t.beatLength /= clockRate
                    t.bpm *= clockRate
                    t.startTime /= (t.startTime / clockRate).toInt()
                }
            }
        }

    var version: Int = 14

    var circleCount: Int = 0
    var sliderCount: Int = 0
    var spinnerCount: Int = 0

    protected var mode: OsuMode = OsuMode.DEFAULT

    var ar: Double = 0.0
    var cs: Double = 0.0
    var od: Double = 0.0
    var hp: Double = 0.0

    var sliderBaseVelocity: Double = 1.0
    var sliderTickRate: Double = 1.0
    var sliderMultiplier: Double = 1.0
    var stackLeniency: Double = -1.0

    var length: Int = 0

    var hitObjects: MutableList<HitObject> = LinkedList<HitObject>()

    var timings: MutableList<Timing> = LinkedList<Timing>()

    fun parseDifficulty(line: String) {
        val entity: Array<String?> = line.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (entity.size == 2) {
            val key = entity[0]!!.trim { it <= ' ' }
            val value = entity[1]!!.trim { it <= ' ' }

            when (key) {
                "ApproachRate" -> this.ar = value.toDouble()
                "OverallDifficulty" -> this.od = value.toDouble()
                "CircleSize" -> this.cs = value.toDouble()
                "HPDrainRate" -> this.hp = value.toDouble()
                "SliderTickRate" -> sliderTickRate = value.toDouble()
                "SliderMultiplier" -> sliderMultiplier = value.toDouble()
            }
        }
    }

    fun parseTiming(line: String) {
        val entity: Array<String> = line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (entity.size < 8) throw RuntimeException("解析 [TimingPoints] 错误")

        val startTime = floor(entity[0].toDouble()).toInt()
        val beatLength = entity[1].toDouble()
        val meter = entity[2].toInt() //节拍
        val timingSampleSet = TimingSampleSet.getType(entity[3].toInt())
        val sampleParameter = entity[4].toInt()
        val volume = entity[5].toInt()
        val isRedLine = entity[6].toBoolean()
        val effect = TimingEffect.getType(entity[7].toInt())

        val obj = Timing(startTime, beatLength, meter, timingSampleSet, sampleParameter, volume, isRedLine, effect)
        timings.add(obj)
    }

    /**
     * 逐行读取
     * 
     * @param read    osu file
     * @param general 元信息
     */
    init {
        var line: String?
        var section: String? = ""
        // 逐行
        while ((read.readLine().also { line = it }) != null) {
            if (line!!.startsWith("[")) {
                section = line
                line = read.readLine()
            }
            if (line.isNullOrBlank()) { //空谱面会 null
                continue
            }
            when (section) {
                "[Difficulty]" -> {
                    // 读取 Difficulty 块
                    parseDifficulty(line)
                }

                "[TimingPoints]" -> {
                    // 读取 TimingPoints 块
                    parseTiming(line)
                }

                "[HitObjects]" -> {
                    // 读取 HitObjects 块
                    parseHitObject(line)
                }
            }
        }

        if (hitObjects.isNotEmpty()) {
            length = (hitObjects.last().endTime - hitObjects.first().startTime)
        }
    }

    val isConverted: Boolean
        get() = this.javaClass != OsuBeatmapAttributes::class.java && mode == OsuMode.OSU

    fun getArHitWindow(mods: Int, clockRate: Double): Double {
        var arValue: Double = this.ar
        if ((mods and (1 shl 4)) != 0) {
            arValue = min(arValue * 1.4, 10.0)
        } else if ((mods and (1 shl 1)) != 0) {
            arValue *= 0.5
        }

        return difficultyRange(arValue, AR_MS_MIN, AR_MS_MID, AR_MS_MAX) / clockRate
    }

    fun getOdHitWindow(mods: Int, clockRate: Double): Double {
        var odValue: Double = this.od
        if ((mods and (1 shl 4)) != 0) {
            odValue = min(odValue * 1.4, 10.0)
        } else if ((mods and (1 shl 1)) != 0) {
            odValue *= 0.5
        }
        var window = 0.0
        when (mode) {
            OsuMode.OSU, OsuMode.CATCH -> {
                window = difficultyRange(
                    odValue,
                    HIT_WINDOW_OSU_MAX,
                    HIT_WINDOW_OSU_MID,
                    HIT_WINDOW_OSU_MIN
                ) / clockRate
            }

            OsuMode.TAIKO -> {
                window = difficultyRange(
                    odValue,
                    HIT_WINDOW_TAIKO_MAX,
                    HIT_WINDOW_TAIKO_MID,
                    HIT_WINDOW_TAIKO_MIN
                ) / clockRate
            }

            OsuMode.MANIA -> {
                window = if (!this.isConverted) {
                    34.0 + 3.0 * (min(10.0, max(0.0, (10.0 - this.od))))
                } else if (this.od > 4) {
                    34.0
                } else {
                    47.0
                }

                if ((mods and (1 shl 4)) != 0) {
                    window /= 1.4
                } else if ((mods and (1 shl 1)) != 0) {
                    window *= 1.4
                }
            }

            else -> {}
        }
        return ceil((window * floor(clockRate)) / clockRate)
    }

    private fun difficultyRange(difficulty: Double, min: Double, mid: Double, max: Double): Double {
        if (difficulty > 5) {
            return mid + (max - mid) * (difficulty - 5) / 5
        } else if (difficulty < 5) {
            return mid - (mid - min) * (5 - difficulty) / 5
        } else {
            return mid
        }
    }

    fun parseHitObject(line: String) {
        // line 就是 '320,192,153921,1,0,0:0:0:0:' 这种格式的字符串
        val entity: Array<String?> = line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (entity.size < 4) throw RuntimeException("解析 [HitObjects] 错误")
        // 解析类型
        val type = HitObjectType.getType(entity[3]!!.toInt())
        val startTime = entity[2]!!.toInt()
        val hit = HitObject()
        hit.type = type

        when (type) {
            HitObjectType.CIRCLE -> {
                val x = entity[0]!!.toInt()
                val y = entity[1]!!.toInt()
                hit.position = HitObjectPosition(x, y)
                hit.startTime = startTime
                // 普通泡泡没有结束时间
                hit.endTime = startTime
            }

            HitObjectType.SLIDER -> {
                val x = entity[0]!!.toInt()
                val y = entity[1]!!.toInt()
                // 滑条计算 time = length / (SliderMultiplier * 100 * SV) * beatLength
                val length = entity[7]!!.toDouble()
                val sliderMultiplier = this.sliderMultiplier
                val timing = getBeforeTiming(startTime)
                val sliderTime: Int
                if (timing.isRedLine) {
                    sliderTime = (length / (sliderMultiplier * 100 * 1) * timing.beatLength).roundToInt()
                } else {
                    val sv = timing.beatLength / -100
                    sliderTime = (length / (sliderMultiplier * 100 * sv) * timing.beatLength).roundToInt()
                }
                hit.position = HitObjectPosition(x, y)
                hit.startTime = startTime
                hit.endTime = startTime + sliderTime
            }

            HitObjectType.SPINNER -> {
                hit.position = HitObjectPosition(256, 192)
                hit.startTime = startTime
                hit.endTime = entity[5]!!.toInt()
            }

            HitObjectType.LONGNOTE -> {
                val x = entity[0]!!.toInt()
                // 骂娘的长条不看 y (does not affect holds. It defaults to the center of the playfield)
                hit.position = HitObjectPosition(x, 192)
                hit.startTime = startTime
                hit.endTime = entity[5]!!.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].toInt()
            }

            else -> {}
        }
        hitObjects.add(hit)
    }

    private fun getBeforeTiming(time: Int): Timing {
        val n = ContextUtil.getContext(THREAD_KEY, 0, Int::class.java)!!
        val size = timings.size
        if (n >= (size - 1)) return timings[n]
        var i = n
        while (i < (size - 1) && timings[i].startTime < time) {
            i++
        }
        setContext(THREAD_KEY, i)
        return timings[i]
    }

    companion object {
        const val HIT_WINDOW_OSU_MAX: Double = 80.0
        const val HIT_WINDOW_OSU_MID: Double = 50.0
        const val HIT_WINDOW_OSU_MIN: Double = 20.0

        const val HIT_WINDOW_TAIKO_MAX: Double = 50.0
        const val HIT_WINDOW_TAIKO_MID: Double = 35.0
        const val HIT_WINDOW_TAIKO_MIN: Double = 20.0

        const val AR_MS_MAX: Double = 1800.0
        const val AR_MS_MID: Double = 1200.0
        const val AR_MS_MIN: Double = 450.0

        private const val THREAD_KEY = "R6a8s4d/*9"
    }
}
