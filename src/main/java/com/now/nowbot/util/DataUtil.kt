package com.now.nowbot.util

import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.model.enums.GreekChar
import com.now.nowbot.model.enums.JaChar
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.*
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.Statistics
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.command.*
import io.github.humbleui.skija.Typeface
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.lang.NonNull
import org.springframework.lang.Nullable
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.*

object DataUtil {
    private val log: Logger = LoggerFactory.getLogger(DataUtil::class.java)

    var TORUS_REGULAR: Typeface? = null
        @JvmStatic
        get() {
            if (field == null || field!!.isClosed) {
                try {
                    field = Typeface.makeFromFile("${NowbotConfig.FONT_PATH}Torus-Regular.ttf")
                } catch (e: Exception) {
                    log.error("未读取到目标字体:Torus-Regular.ttf", e)
                    field = Typeface.makeDefault()
                }
            }
            return field
        }

    var TORUS_SEMIBOLD: Typeface? = null
        @JvmStatic
        get() {
            if (field == null || field!!.isClosed) {
                try {
                    field = Typeface.makeFromFile("${NowbotConfig.FONT_PATH}Torus-SemiBold.ttf")
                } catch (e: Exception) {
                    log.error("未读取到目标字体:Torus-SemiBold.ttf", e)
                    field = Typeface.makeDefault()
                }
            }
            return field
        }

    var PUHUITI: Typeface? = null
        @JvmStatic
        get() {
            if (field == null || field!!.isClosed) {
                try {
                    field = Typeface.makeFromFile("${NowbotConfig.FONT_PATH}Puhuiti.ttf")
                } catch (e: Exception) {
                    log.error("Alibaba-PuHuiTi-Medium.ttf", e)
                    field = Typeface.makeDefault()
                }
            }
            return field
        }

    var PUHUITI_MEDIUM: Typeface? = null
        @JvmStatic
        get() {
            if (field == null || field!!.isClosed) {
                try {
                    field =
                            Typeface.makeFromFile(
                                    "${NowbotConfig.FONT_PATH}Alibaba-PuHuiTi-Medium.ttf")
                } catch (e: Exception) {
                    log.error("Alibaba-PuHuiTi-Medium.ttf", e)
                    field = Typeface.makeDefault()
                }
            }
            return field
        }

    var EXTRA: Typeface? = null
        @JvmStatic
        get() {
            if (field == null || field!!.isClosed) {
                try {
                    field = Typeface.makeFromFile(NowbotConfig.FONT_PATH + "extra.ttf")
                } catch (e: Exception) {
                    log.error("未读取到目标字体:extra.ttf", e)
                    throw e
                }
            }
            return field
        }

    fun int2hex(color: Int): String {
        return "#" + color.toString(16).padStart(6, '0')
    }

    /**
     * @param h 0..360
     * @param s 0..100
     * @param l 0..100
     */
    fun hsl2hex(h: Int, s: Int, l: Int): String {
        fun hue2rgb(p:Double, q:Double, h:Double) :Double{
            var t = h
            if (t < 0) t += 1.0
            if (t > 1) t -= 1.0
            if (t < 1.0 / 6) return p + (q - p) * 6.0 * t
            if (t < 1.0 / 2) return q
            if (t < 2.0 / 3) return p + (q - p) * (2.0 / 3 - t) * 6.0
            return p
        }
        val sf = s / 100.0
        val lf = l / 100.0
        val hf = h / 360.0
        if (s == 0) return "#0"
        val q = if (l < 0.5) {
            lf * (1 + sf)
        } else {
            lf + sf - lf * sf
        }
        val p = 2 * lf - q
        val r = (hue2rgb(p, q, hf + 1.0 / 3) * 255).toInt()
        val g = (hue2rgb(p, q, hf) * 255).toInt()
        val b = (hue2rgb(p, q, hf - 1.0 / 3) * 255).toInt()

        return String.format("#%02x%02x%02x", r, g, b)
    }

    /**
     * 将按逗号或者 |、:：分隔的字符串分割 如果未含有分割的字符，返回 null
     * 根据分隔符，分割玩家名
     *
     * @param str 需要分割的字符串
     * @param splitSpace 如果需要分割玩家名之类可能含有空格的字段，则为 false，默认 false
     * @return 玩家名列表
     */
    fun splitString(str: String?, splitSpace: Boolean = false): List<String>? {
        if (str.isNullOrBlank()) return null
        val regex = if (splitSpace) REG_SEPERATOR.toRegex() else REG_SEPERATOR_NO_SPACE.toRegex()

        val strings = str.split(regex).map { it.trim() }.dropLastWhile { it.isBlank() }
        if (strings.isEmpty()) return null
        return strings
    }

    /*
    /**
     * 判定优秀成绩。用于临时区分 panel E 和 panel E5
     *
     * @param score 成绩，需要先算好 pp，并使用完全体
     * @return 是否为优秀成绩
     */
    @Deprecated("弃用")
    fun isExcellentScore(@NonNull score: Score, user: OsuUser): Boolean {
        // 指标分别是：星数 >= 8，星数 >= 6.5，准确率 > 90%，连击 > 98%，PP > 300，PP > 玩家总 PP 减去 400 之后的 1/25 （上 BP，并且计
        // 2 点），失误数 < 1%。
        var r = 0
        val p = getPP(score, user)

        val ultra = score.beatmap.starRating >= 8f
        val extreme = score.beatmap.starRating >= 6.5f
        val acc = score.accuracy >= 0.9f
        val combo =
            1f * score.maxCombo / (score.beatmap.maxCombo ?: Int.MAX_VALUE) >= 0.98f
        val pp = score.pp >= 300f
        val bp = p >= 400f && score.pp >= (p - 400f) / 25f
        val miss =
            score.statistics.getCountAll(score.mode) > 0 &&
                    score.statistics.countMiss!! <=
                    score.statistics.getCountAll(score.mode) * 0.01f

        val fail = score.rank == null || score.rank == "F"

        if (ultra) r++
        if (extreme) r++
        if (acc) r++
        if (combo) r++
        if (pp) r++
        if (bp) r += 2
        if (miss) r++

        return r >= 3 && !fail
    }

     */

    // 获取优秀成绩的私有方法
    /*
    private fun getPP(score: Score, user: OsuUser): Double {
        var pp = user.pp

        val is4K = score.beatmap.mode == MANIA && score.beatmap.CS == 4f
        val is7K = score.beatmap.mode == MANIA && score.beatmap.CS == 7f

        if (is4K) {
            pp = user.statistics!!.pp4K!!
        }

        if (is7K) {
            pp = user.statistics!!.pp7K!!
        }

        return pp
    }

     */

    /**
     * 将 !bp 45-55 转成 score API 能看懂的 offset-limit 对
     *
     * @param start 开始
     * @param end 结束
     * @return offset
     */
    @JvmStatic
    @NonNull
    fun parseRange2Offset(start: Int?, end: Int?): Int {
        return parseRange(start, end).offset
    }

    /**
     * 将 !bp 45-55 转成 score API 能看懂的 offset-limit 对
     *
     * @param start 开始
     * @param end 结束
     * @return limit
     */
    @JvmStatic
    @NonNull
    fun parseRange2Limit(start: Int?, end: Int?): Int {
        return parseRange(start, end).limit
    }

    /**
     * 将 !bp 45-55 转成 score API 能看懂的 offset-limit 对
     *
     * @param start 开始
     * @param end 结束
     * @return offset-limit 对
     */
    @NonNull
    private fun parseRange(start: Int?, end: Int?): Range {
        val st =
            start.let {
                if (it == null || it < 1 || it > 100) {
                    1
                } else {
                    it
                }
            }

        val offset: Int
        val limit: Int

        if (end == null || end < 1 || end > 100) {
            offset = st - 1
            limit = 1
        } else {
            // 分流：正常，相等，相反
            if (end > st) {
                offset = st - 1
                limit = end - st + 1
            } else if (st == end) {
                offset = st - 1
                limit = 1
            } else {
                offset = end - 1
                limit = st - end + 1
            }
        }

        return Range(offset, limit)
    }



    // 获取谱面的原信息，方便成绩面板使用。请在 applyBeatMapExtend 和 applySRAndPP 之前用。
    @JvmStatic
    fun getOriginal(beatmap: Beatmap): Map<String, Any> {
        if (beatmap.CS == null) return mapOf()

        return mapOf(
            "cs" to beatmap.CS!!,
            "ar" to beatmap.AR!!,
            "od" to beatmap.OD!!,
            "hp" to beatmap.HP!!,
            "bpm" to beatmap.BPM!!,
            "drain" to beatmap.hitLength!!,
            "total" to beatmap.totalLength,
        )
    }

    /** 根据准确率，通过获取准确率，来构建一个 Statistic。 */
    @NonNull
    @JvmStatic
    fun accuracy2Statistics(accuracy: Double, total: Int, osuMode: OsuMode): Statistics {
        val stat = Statistics()

        var acc = accuracy

        // 一个物件所占的 Acc 权重
        if (total <= 0) return stat
        val weight = 1.0 / total

        fun getTheoreticalCount(value: Double): Int {
            val count = floor(acc / value).roundToInt()
            acc -= (value * count)
            return count
        }

        when (osuMode) {
            OSU,
            DEFAULT -> {
                val n300 = min(getTheoreticalCount(weight), max(total, 0))
                val n100 = min(getTheoreticalCount(weight / 3), max(total - n300, 0))
                val n50 = min(getTheoreticalCount(weight / 6), max(total - n300 - n100, 0))
                val n0 = max(total - n300 - n100 - n50, 0)

                stat.count300 = n300
                stat.count100 = n100
                stat.count50 = n50
                stat.countMiss = n0
            }

            TAIKO -> {
                val n300 = min(getTheoreticalCount(weight), max(total, 0))
                val n100 = min(getTheoreticalCount(weight / 3), max(total - n300, 0))
                val n0 = max(total - n300 - n100, 0)

                stat.count300 = n300
                stat.count100 = n100
                stat.countMiss = n0
            }

            CATCH -> {
                val n300 = min(getTheoreticalCount(weight), max(total, 0))
                val n0 = max(total - n300, 0)

                stat.count300 = n300
                stat.countMiss = n0
            }

            MANIA -> {
                val n320 = min(getTheoreticalCount(weight), max(total, 0))
                val n200 = min(getTheoreticalCount(weight / 1.5), max(total - n320, 0))
                val n100 = min(getTheoreticalCount(weight / 3), max(total - n320 - n200, 0))
                val n50 = min(getTheoreticalCount(weight / 6), max(total - n320 - n200 - n100, 0))
                val n0 = max(total - n320 - n200 - n100 - n50, 0)

                stat.countGeki = n320
                stat.countKatu = n200
                stat.count100 = n100
                stat.count50 = n50
                stat.countMiss = n0
            }
        }

        return stat
    }

    /**
     * 根据准确率，通过获取原成绩的判定结果的彩率，来构建一个达到目标准确率的判定结果
     *
     * @param aiming 准确率，0-10000
     * @param stat 当前的判定结果
     * @return 达到目标准确率时的判定结果
     */
    @NonNull
    fun maniaAimingAccuracy2Statistics(aiming: Double?, @NonNull stat: Statistics): Statistics {
        if (stat.isNull) {
            return Statistics()
        }

        if (aiming == null) {
            return stat
        }

        val total = stat.getCountAll(MANIA)

        // geki, 300, katu, 100, 50, 0
        val list =
            mutableListOf(
                stat.countGeki ?: 0,
                stat.count300 ?: 0,
                stat.countKatu ?: 0,
                stat.count100 ?: 0,
                stat.count50 ?: 0,
                stat.countMiss ?: 0
            )

        // 一个物件所占的 Acc 权重
        if (total <= 0) return stat
        val weight = 1.0 / total

        // 彩黄比
        val ratio =
            if ((stat.count300!! + stat.countGeki!! > 0))
                stat.countGeki!! * 1.0 / (stat.count300!! + stat.countGeki!!)
            else 0.0

        var current = stat.getAccuracy(MANIA)

        if (current >= aiming) return stat

        // 交换评级
        if (current < aiming && stat.countMiss!! > 0) {
            val ex = exchangeJudge(list.first(), list.last(), 1.0, 0.0, current, aiming, weight)
            list[0] = ex.great
            list[5] = ex.bad
            current = ex.accuracy
        }

        if (current < aiming && stat.count50!! > 0) {
            val ex = exchangeJudge(list.first(), list[4], 1.0, 1.0 / 6.0, current, aiming, weight)
            list[0] = ex.great
            list[4] = ex.bad
            current = ex.accuracy
        }

        if (current < aiming && stat.count100!! > 0) {
            val ex = exchangeJudge(list.first(), list[3], 1.0, 1.0 / 3.0, current, aiming, weight)
            list[0] = ex.great
            list[3] = ex.bad
            current = ex.accuracy
        }

        if (current < aiming && stat.countKatu!! > 0) {
            val ex = exchangeJudge(list.first(), list[2], 1.0, 2.0 / 3.0, current, aiming, weight)
            list[0] = ex.great
            list[2] = ex.bad
            // current = ex.accuracy;
        }

        val nGreat = list.first() + list[1]

        list[0] = floor(nGreat * ratio).toInt()
        list[1] = max((nGreat - list.first()).toDouble(), 0.0).toInt()

        stat.countGeki = list.first()
        stat.count300 = list[1]
        stat.countKatu = list[2]
        stat.count100 = list[3]
        stat.count50 = list[4]
        stat.countMiss = list.last()

        return stat
    }

    // 交换评级
    @NonNull
    fun exchangeJudge(
        nGreat: Int,
        nBad: Int,
        wGreat: Double,
        wBad: Double,
        currentAcc: Double,
        aimingAcc: Double,
        weight: Double
    ): Exchange {
        var g = nGreat
        var b = nBad
        var c = currentAcc

        val gainAcc = weight * (wGreat - wBad)

        for (i in 0 until nBad) {
            g++
            b--
            c += gainAcc

            if (c >= aimingAcc) break
        }

        return Exchange(g, b, currentAcc)
    }

    @JvmStatic
    @NonNull
    fun isHelp(@Nullable str: String?): Boolean {
        val s = str?.trim() ?: return false
        return s.equals("help", ignoreCase = true) ||
                s.equals("帮助", ignoreCase = true)
    }

    fun string2Markdown(str: String): String {
        return str.replace("\n", "\n\n")
    }

    fun jsonString2Markdown(str: String?): String? {
        if (str.isNullOrBlank()) return null
        return str.replace("},", "},\n\n")
    }

    private fun getRoundedNumberUnit(num: Double, level: Int): Char {
        var number = num
        var unit = '-'
        number = abs(number)
        if (level < 1 || level > 2) return '-'
        var m = 1 + level

        if (number < 10.0.pow(m)) { // level==1->100 level==2->1000
            unit = 0.toChar()
        } else if (number <
            10.0.pow(
                3.let {
                    m += it
                    m
                })
        ) {
            unit = 'K'
        } else if (number <
            10.0.pow(
                3.let {
                    m += it
                    m
                })
        ) {
            unit = 'M'
        } else if (number <
            10.0.pow(
                3.let {
                    m += it
                    m
                })
        ) {
            unit = 'G'
        } else if (number <
            10.0.pow(
                3.let {
                    m += it
                    m
                })
        ) {
            unit = 'T'
        } else if (number < 10.0.pow((m + 3))) {
            unit = 'P'
        }

        return unit
    }

    fun getRoundedNumber(num: Double, level: Int): Double {
        // lv1.保留1位小数，结果不超4位字符宽(包含单位)
        // 1-99-0.1K-9.9K-10K-99K-0.1M-9.9M-10M-99M-0.1G-9.9G-10G-99G-0.1T-9.9T-10T-99T-Inf.

        // lv2.保留2位小数，结果不超7位字符宽(包含单位)
        // 1-999-1.00K-999.99K-1.00M-999.99M-1.00G-999.99G-...-999.9T-Inf.

        // 将负值纳入计算

        var number = num
        while (number >= 1000 || number <= -1000) {
            number /= 1000.0
        }

        if (level == 1) {
            if (number >= 100) {
                number /= 1000.0
            }
            number = (number * 10).roundToInt().toDouble() / 10.0
        } else if (level == 2) {
            number = (number * 1000).roundToInt().toDouble() / 1000.0
        }
        if (number - number.roundToInt() <= 0.0001) number = number.roundToInt().toDouble()

        return number
    }

    fun getRoundedNumberStr(num: Double, level: Int): String {
        var number = num
        val c = getRoundedNumberUnit(number, level)
        val isInt: Boolean
        val intValue: Int
        if (c.code == 0) {
            intValue = number.toInt()
            isInt =
                if (level == 1) {
                    number - intValue <= 0.1
                } else {
                    number - intValue <= 0.001
                }
            if (isInt) return intValue.toString()
            return number.toString()
        }

        while (number >= 1000 || number <= -1000) {
            number /= 1000.0
        }

        if (level == 1) {
            if (number >= 100) {
                number /= 1000
            }
            number = (number * 10).roundToInt().toDouble() / 10
        } else if (level == 2) {
            number = (number * 1000).roundToInt().toDouble() / 1000
        }
        intValue = number.toInt()
        isInt =
            if (level == 1) {
                number - intValue <= 0.1
            } else {
                number - intValue <= 0.001
            }

        if (isInt) {
            return String.format("%d%c", intValue, c)
        }
        var out = String.format(if (level == 1) "%.1f%c" else "%.2f%c", number, c)
        if (out[out.length - 2] == '0') {
            out = out.substring(0, out.length - 2) + c
        }
        return out
    }

    fun time2HourAndMinute(time: Long): String {
        if (time < 3600000) {
            return String.format("%dM", time / 60000)
        }
        val h = time / 3600000
        val m = (time % 3600000) / 60000
        return String.format("%dH%dM", h, m)
    }

    fun getPlayedRankedMapCount(bonusPP: Double): Int {
        val v = -(bonusPP / (1000 / 2.4)) + 1.0
        return if (v < 0) {
            0
        } else {
            round(ln(v) / ln(0.9994)).toInt()
        }
    }

    fun getBonusPP(playerPP: Double, fullPP: List<Double>): Double {
        return getBonusPP(playerPP, fullPP.toDoubleArray())
    }

    /** 计算bonusPP 算法是最小二乘 y = kx + b 输入的PP数组应该是加权之前的数组。 */
    @JvmStatic
    fun getBonusPP(playerPP: Double, fullPP: DoubleArray?): Double {
        val bonusPP: Double
        var remainPP = 0.0
        val k: Double
        val b: Double
        var bpPP = 0.0
        var x = 0.0
        var x2 = 0.0
        var xy = 0.0
        var y = 0.0

        if (fullPP == null || fullPP.size.toDouble() == 0.0) return 0.0

        val length = fullPP.size

        for (i in 0 until length) {
            val weight: Double = 0.95.pow(i)
            val pp = fullPP[i]

            // 只拿最后50个bp来算，这样精准
            if (i >= 50) {
                x += i.toDouble()
                y += pp
                x2 += i.toDouble().pow(2)
                xy += i * pp
            }
            bpPP += pp * weight // 前 100 的bp上的 pp
        }

        val N = (length - 50).toDouble()
        // Exiyi - Nxy__ / Ex2i - Nx_2
        k = (xy - (x * y / N)) / (x2 - (x.pow(2.0) / N))
        b = (y / N) - k * (x / N)

        // 找零点
        val expectedX = if ((k == 0.0)) -1 else floor(-b / k).toInt()

        // 这个预估的零点应该在很后面，不应该小于 100
        // 如果bp没满100，那么bns直接可算得，remainPP = 0
        if (length < 100 || expectedX <= 100) {
            bonusPP = playerPP - bpPP
        } else {
            // 对离散数据求和
            for (i in length..expectedX) {
                val weight: Double = 0.95.pow(i)
                remainPP += (k * i + b) * weight
            }

            bonusPP = playerPP - bpPP - remainPP
        }

        return max(min(bonusPP, 413.894179759), 0.0)
    }

    // 下下策
    /*
    private fun getV3ScoreProgress(score: Score): Double {
        val mode = score.mode

        val progress =
            if (!score.passed) {
                1.0 * score.statistics.getCountAll(mode) / (score.beatmap.maxCombo ?: Int.MAX_VALUE)
            } else {
                1.0
            }
        return progress
    }

    fun getV3Score(score: Score): String {
        // 算 v3 分（lazer的计分方式
        // 有个版本指出，目前 stable 的 v2 是这个算法的复杂版本，acc是10次方，转盘分数纳入mod倍率

        val mode = score.mode
        val mods = score.mods

        val fc = 1000000
        val i = getV3ModsMultiplier(mods, mode)
        val p = getV3ScoreProgress(score) // 下下策
        val c = score.maxCombo
        val m = score.beatmap.maxCombo!!
        val ap8: Double = score.accuracy.pow(8.0)
        val v3 =
            when (score.mode) {
                OSU,
                CATCH,
                DEFAULT,
                null -> fc * i * (0.7f * c / m + 0.3f * ap8) * p

                TAIKO -> fc * i * (0.75f * c / m + 0.25f * ap8) * p
                MANIA -> fc * i * (0.01f * c / m + 0.99f * ap8) * p
            }

        return String.format("%07d", round(v3)) // 补 7 位达到 v3 分数的要求
    }

    private fun getV3ModsMultiplier(mod: List<String?>, mode: OsuMode?): Double {
        var index = 1.00

        if (mod.contains("EZ")) index *= 0.50

        when (mode) {
            OSU -> {
                if (mod.contains("HT")) index *= 0.30
                if (mod.contains("HR")) index *= 1.10
                if (mod.contains("DT")) index *= 1.20
                if (mod.contains("NC")) index *= 1.20
                if (mod.contains("HD")) index *= 1.06
                if (mod.contains("FL")) index *= 1.12
                if (mod.contains("SO")) index *= 0.90
            }

            TAIKO -> {
                if (mod.contains("HT")) index *= 0.30
                if (mod.contains("HR")) index *= 1.06
                if (mod.contains("DT")) index *= 1.12
                if (mod.contains("NC")) index *= 1.12
                if (mod.contains("HD")) index *= 1.06
                if (mod.contains("FL")) index *= 1.12
            }

            CATCH -> {
                if (mod.contains("HT")) index *= 0.30
                if (mod.contains("HR")) index *= 1.12
                if (mod.contains("DT")) index *= 1.12
                if (mod.contains("NC")) index *= 1.12
                if (mod.contains("FL")) index *= 1.12
            }

            MANIA -> {
                if (mod.contains("HT")) index *= 0.50
                if (mod.contains("CO")) index *= 0.90
            }

            null,
            DEFAULT -> {
            }
        }
        return index
    }

     */

    /**
     * 缩短字符 220924
     *
     * @param str 需要被缩短的字符
     * @param maxWidth 最大宽度
     * @return 返回已缩短的字符
     */
    fun getShortenStr(str: String, maxWidth: Int): String {
        val sb = StringBuilder()
        val char = str.toCharArray()

        val allWidth = 0f
        var backL = 0

        for (thisChar in char) {
            if (allWidth > maxWidth) {
                break
            }
            sb.append(thisChar)
            if ((allWidth) < maxWidth) {
                backL++
            }
        }
        if (allWidth > maxWidth) {
            sb.delete(backL, sb.length)
            sb.append("...")
        }

        sb.delete(0, sb.length)

        return sb.toString()
    }

    /*
    public static float getBonusPP (double playerPP, double[] rawPP){
        double bonusPP, remainPP = 0, a, b, c, bpPP = 0, x = 0, x2 = 0, x3 = 0, x4 = 0, xy = 0, x2y = 0, y = 0;

        if (rawPP == null || rawPP.length == 0d) return 0f;

        int length = rawPP.length;

        for (int i = 0; i < length; i++) {
            double weight = Math.pow(0.95f, i);
            double PP = rawPP[i];

            x += i;
            x2 += Math.pow(i, 2f);
            x3 += Math.pow(i, 3f);
            x4 += Math.pow(i, 4f);
            xy += i * PP;
            x2y += Math.pow(i, 2f) * PP;
            y += PP;
            bpPP += PP * weight;//前 100 的bp上的 pp
        }

        if (length < 100) { //如果bp没满100，那么bns直接可算得，remaining = 0
            return (float) Math.min((playerPP - bpPP), 416.6667f);
        } else {

            x /= length;
            x2 /= length;
            x3 /= length;
            x4 /= length;
            xy /= length;
            x2y /= length;
            y /= length;

            double a1 = xy - x * y;
            double a2 = x3 - x * x2;
            double a3 = x2y - x2 * y;
            double a4 = x2 - Math.pow(x, 2f);
            double a5 = x4 - Math.pow(x2, 2f) * x2;

            //得到 y = ax2 + bx + c
            a = ((a1 * a2) - (a3 * a4)) / (Math.pow(a2, 2f) - (a5 * a4));
            b = (xy - (x * y) - (a * a2)) / (x2 - Math.pow(x, 2f));
            c = y - a * x2 - b * x;

            //好像不需要求导，直接找零点
            double delta = Math.pow(b, 2f) - (4 * a * c);
            if (delta < 0) {
                return 0f; //不相交
            }
            int expectedX = (int) Math.floor(( - b - Math.sqrt(delta)) / (2 * a)); //找左边的零点，而且要向下取整
            if (expectedX <= 100) {
                return (float) Math.min((playerPP - bpPP), 416.6667f); //这个预估的零点应该在很后面
            }

            //对离散数据求和
            for (int i = length; i <= expectedX; i++) {
                double weight = Math.pow(0.95f, i);
                remainPP += (a * Math.pow(i, 2f) + b * i + c) * weight;
            }

            bonusPP = playerPP - bpPP - remainPP;

            return (float) Math.min(bonusPP, 416.6667f);
        }
    }

     */
    /**
     * 获取该文件名的 Markdown 文件并转成字符串
     *
     * @param path 文件名，和相对路径
     * @return 文件内容
     */
    @JvmStatic
    fun getMarkdownFile(path: String?): String {
        val sb = StringBuilder()

        try {
            val bufferedReader =
                Files.newBufferedReader(
                    Path.of(NowbotConfig.EXPORT_FILE_PATH).resolve(path ?: return "")
                )

            // 逐行读取文本内容
            var line: String?
            while ((bufferedReader.readLine().also { line = it }) != null) {
                sb.append(line).append('\n')
            }

            // 关闭流
            bufferedReader.close()

            return sb.toString()
        } catch (ignored: Exception) {
            return ""
        }
    }

    /**
     * 获取该文件名的图片文件并转成字符串
     *
     * @param path 图片名，和相对路径
     * @return 图片流
     */
    @JvmStatic
    fun getPicture(path: String): ByteArray? {
        if (path.isEmpty()) return null

        return try {
            Files.readAllBytes(Path.of(NowbotConfig.EXPORT_FILE_PATH).resolve(path))
        } catch (e: IOException) {
            null
        }
    }

    @JvmStatic
    // 获取两个字符串的相似度。
    fun getStringSimilarity(compare: String?, to: String?): Double {
        if (compare.isNullOrEmpty() || to.isNullOrEmpty()) return 0.0

        val cs = getStandardisedString(compare)
        val ts = getStandardisedString(to)

        return getSimilarity(cs, ts) * getSimilarity(cs.reversed(), ts.reversed()).pow(0.5)
    }

    @JvmStatic
    // 获取字符串与已经格式化后的字符串的相似度。
    fun getStringSimilarityStandardised(compare: String?, toStandard: String?): Double {
        if (compare.isNullOrEmpty() || toStandard.isNullOrEmpty()) return 0.0

        val cs = getStandardisedString(compare)

        return getSimilarity(cs, toStandard) * getSimilarity(cs.reversed(), toStandard.reversed()).pow(0.5)
    }

    private fun getSimilarity(compare: String, too: String): Double {
        var to = too
        val cLength = compare.length
        val tLength = to.length

        var count = 0.0
        var sequentMax = 0.0
        var sequent = 0.0
        if (compare.isNotBlank() && to.isNotBlank()) {

            outer@ for (v in compare) {
                for (w in to) {
                    if (v == w) {
                        to = to.substring(to.indexOf(w) + 1)
                        count++
                        sequent++
                        continue@outer
                    }
                }
                sequentMax = max(sequent, sequentMax)
                sequent = 0.0
            }

            sequentMax = max(sequent, sequentMax)

            // to 1 matched length
            val m1 = max(tLength - to.length, 1)

            if (cLength > 0 && tLength > 0) {
                return count * sequentMax / (cLength * m1)
            }
        }

        return 0.0
    }

    fun numberTo62(n: Long): String {
        val sb = StringBuilder()
        var number = n
        var mod: Int
        var c: Char
        while (number > 0) {
            mod = (number % 62).toInt()
            c = when {
                mod < 10 -> '0' + mod
                mod < 36 -> 'a' + mod - 10
                else -> 'A' + mod - 36
            }
            sb.append(c)
            number /= 62
        }
        return sb.reverse().toString()
    }

    fun stringTo62(s: String): Long {
        var result = 0L
        for (c in s) {
            result = result * 62 + when(c){
                in '0'..'9' -> c - '0'
                in 'a'..'z' -> c - 'a' + 10
                in 'A'..'Z' -> c - 'A' + 36
                else -> 0
            }
        }
        return result
    }

    @JvmStatic
    fun getStandardisedString(str: String?): String {
        if (str.isNullOrEmpty()) return ""

        return str
            .toRomanizedJaChar()
            .toRomanizedGreekChar()
            .toHalfWidthChar()
            .lowercase()
            .replace(Regex(REG_HYPHEN), "-")
            .replace(Regex(REG_PLUS), "+")
            .replace(Regex(REG_COLON), ":")
            .replace(Regex(REG_HASH), "#")
            .replace(Regex(REG_STAR), "*")
            .replace(Regex(REG_EXCLAMATION), "!")
            .replace(Regex(REG_QUESTION), "?")
            .replace(Regex(REG_QUOTATION), "\"")
            .replace(Regex(REG_FULL_STOP), ".")
            .replace(Regex(REG_LEFT_BRACKET), "[")
            .replace(Regex(REG_RIGHT_BRACKET), "]")
            .replace(Regex("[\\s　]+"), "") // 这里有个全宽还是零宽空格？
    }

    // 标准化字段
    @JvmStatic
    fun getSort(sort: String?): String {
        return when (sort?.lowercase()) {
            "t", "t+", "ta", "title", "title asc", "title_asc" -> "title_asc"
            "t-", "td", "title desc", "title_desc" -> "title_desc"
            "a", "a+", "aa", "artist", "artist asc", "artist_asc" -> "artist_asc"
            "a-", "ad", "artist desc", "artist_desc" -> "artist_desc"
            "d", "d+", "da", "difficulty", "difficulty asc", "difficulty_asc", "s", "s+", "sa", "star", "star asc", "star_asc" -> "difficulty_asc"
            "d-", "dd", "difficulty desc", "difficulty_desc", "s-", "sd", "star desc", "star_desc" -> "difficulty_desc"
            "m", "m+", "ma", "map", "rating", "rating asc", "rating_asc" -> "rating_asc"
            "m-", "md", "map desc", "rating desc", "rating_desc" -> "rating_desc"
            "p", "p+", "pa", "plays", "pc asc", "plays asc", "plays_asc" -> "plays_asc"
            "p-", "pd", "pc desc", "plays desc", "plays_desc" -> "plays_desc"
            "r", "r+", "ra", "ranked", "time asc", "ranked asc", "ranked_asc" -> "ranked_asc"
            "r-", "rd", "time desc", "ranked desc", "ranked_desc" -> "ranked_desc"
            else -> "relevance_desc"
        }
    }

    @JvmStatic
    fun getStatus(status: String?): String? {
        return when (status?.lowercase()) {
            "0", "p", "pend", "pending" -> "pending"
            "1", "r", "rnk", "rank", "ranked" -> "ranked"
            "2", "a", "app", "approve", "approved" -> "approved"
            "3", "q", "qua", "qualify", "qualified" -> "qualified"
            "4", "l", "lvd", "loved" -> "loved"
            "-1", "5", "w", "wip", "inprogress", "in progress", "workinprogress", "work in progress" -> "wip"
            "-2", "6", "g", "gra", "grave", "graveyard", "graveyarded" -> "graveyard"
            "f", "fav", "favor", "favorite", "favorites" -> "favorites"
            "h", "has", "leader", "leaderboard", "has leaderboard" -> null
            else -> "any"
        }
    }

    @JvmStatic
    fun getGenre(genre: String?): Byte? {
        return when (genre?.lowercase()) {
            "u", "un", "uns", "unspecified" -> 1
            "v", "vg", "vgm", "videogame", "video game" -> 2
            "a", "an", "ani", "manga", "anime" -> 3
            "r", "rk", "rock" -> 4
            "p", "pp", "pop" -> 5
            "o", "ot", "oth", "other" -> 6
            "n", "nv", "nvt", "novel", "novelty" -> 7
            "h", "hh", "hip", "hop", "hiphop", "hip hop" -> 9
            "e", "el", "ele", "elect", "electro", "electric", "electronic" -> 10
            "m", "mt", "mtl", "metal" -> 11
            "c", "cl", "cls", "classic", "classical" -> 12
            "f", "fk", "folk" -> 13
            "j", "jz", "jazz" -> 14
            else -> null
        }
    }

    @JvmStatic
    fun getLanguage(language: String?): Byte? {
        return when (language?.lowercase()) {
            "c", "cn", "chn", "china", "chinese" -> 4
            "e", "en", "gb", "eng", "gbr", "england", "english" -> 2
            "f", "fr", "fra", "france", "french" -> 7
            "g", "ge", "ger", "germany", "german" -> 8
            "i", "it", "ita", "italy", "italian" -> 11
            "j", "ja", "jpn", "japan", "japanese" -> 3
            "k", "kr", "kor", "korea", "korean" -> 6
            "s", "sp", "esp", "spa", "spain", "spanish" -> 10
            "w", "sw", "swe", "sweden", "swedish" -> 9
            "r", "ru", "rus", "russia", "russian" -> 12
            "p", "po", "pol", "poland", "polish" -> 13
            "n", "in", "ins", "instrument", "instrumental" -> 5
            "u", "un", "uns", "unspecified" -> 1
            "o", "ot", "oth", "any", "other", "others" -> 14
            else -> null
        }
    }

    /**
     * 自己写的空格或逗号分隔的匹配器，这样子就可以无所谓匹配顺序了
     * @param regexes 正则表达式。注意，这里的正则需要写得越简洁越好，不然会有大量重复匹配。推荐写成 xxx=yyy 的形式
     * @param noContains 如果不填，默认遵循一匹配到就输出的逻辑。如果填写，则只有在靠近末尾的字符串含有这样的字符时，切断并输出当前匹配到的
     */
    fun paramMatcher(str: String?, regexes: List<Regex>, noContains: Regex? = null) : List<List<String>> {
        if (str == null) return emptyList()

        val result = List(regexes.size) { emptyList<String>().toMutableList() }
        var matcher = ""

        val strs = str.split(REG_SEPERATOR.toRegex())

        strs.forEachIndexed { j, s ->
            matcher += s

            for (i in regexes.indices) {
                val reg = regexes[i]

                if (reg.matches(matcher)) {

                    if (noContains == null || (j <= strs.size - 2 && strs[j + 1].contains(noContains)) || (j == strs.size - 1)) {
                        // 正常输出、不包含输出、已经结尾，可以输出

                        result[i].add(matcher)
                        matcher = ""
                    }
                }
            }
        }

        /*

        for (s in strs) {
            matcher += s

            for (i in regexes.indices) {
                val reg = regexes[i]

                if (reg.matches(matcher)) {

                    result[i].add(matcher)
                    matcher = ""
                }
            }
        }

         */

        return result
    }

    /**
     * 一个将列表转换成 markdown 语法的表的方法。
     * 这在列表很大的时候很实用。
     * @param list 含有某个类的列表
     * @param page 当前页面，默认为 1
     * @param supplier 将这个类（行）拆分成不同列的闭包
     * @param heading 表头，格式是
     * | 标题1 | 标题2 |
     * | :-- | :-: |
     * @param maxPerPage 一页中最多存在的行数。
     */
    fun <T, U> getMarkDownChartFromList(list: List<T>, page: Int = 1, supplier: (T) -> List<U>, heading: String = """
                | 标题1 | 标题2 |
                | :-- | :-: |
                """.trimIndent(), maxPerPage: Int = 50): String {

        if (list.isEmpty()) throw GeneralTipsException(GeneralTipsException.Type.G_Empty_Result)

        val sb = StringBuilder()
        sb.append(heading).append('\n') // 导入表头

        val maxPage = ceil(list.size * 1.0 / maxPerPage).roundToInt()

        val start = max(min(page, maxPage) * maxPerPage - maxPerPage, 0)
        val end = min(min(page, maxPage) * maxPerPage, list.size)

        for (i in start..< end) {
            val row = list[i]
            val columns = supplier(row)

            for (c in columns) {
                sb.append(c).append(" | ")
            }

            sb.append('\n')
        }


        sb.append("\n\n第 ${min(page, maxPage)} 页，共 $maxPage 页")

        return sb.toString()
    }

    /**
     * 离散变量估计正态分布的均值和方差
     */
    fun <T: Number> getNormalDistribution(numbers: List<T?>): Pair<Double, Double> {
        val u = if (numbers.isNotEmpty()) numbers.map { it?.toDouble() ?: 0.0 }.average() else 0.0
        val o2 = if (numbers.isNotEmpty()) numbers.map { ((it?.toDouble() ?: 0.0) - u).pow(2.0) }.average() else 0.0

        return u to o2
    }

    @JvmRecord
    private data class Range(val offset: Int, val limit: Int)

    @JvmRecord
    data class Exchange(val great: Int, val bad: Int, val accuracy: Double)

    private fun String.toRomanizedJaChar() = JaChar.getRomanized(this)
    private fun String.toRomanizedGreekChar() = GreekChar.getRomanized(this)
    private fun String.toHalfWidthChar() = run {
        val sb = StringBuilder(this.length)

        for (c in this) {
            if (c.code in 0xFF01..0xFF5E) {
                sb.append((c.code - 0xFEE0).toChar())
            } else {
                sb.append(c)
            }
        }

        return@run sb.toString()
    }
}
