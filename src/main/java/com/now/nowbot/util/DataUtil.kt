package com.now.nowbot.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.model.JsonData.BeatMap
import com.now.nowbot.model.JsonData.OsuUser
import com.now.nowbot.model.JsonData.Score
import com.now.nowbot.model.JsonData.Statistics
import com.now.nowbot.model.enums.OsuMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.*
import io.github.humbleui.skija.Typeface
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.lang.NonNull
import org.springframework.lang.Nullable
import org.springframework.util.CollectionUtils
import org.springframework.util.StringUtils
import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.math.*

object DataUtil {
    private val log: Logger = LoggerFactory.getLogger(DataUtil::class.java)

    private val mapper: ObjectMapper = JsonMapper.builder().build()

    var TORUS_REGULAR: Typeface? = null
        @JvmStatic get() {
            if (field == null || field!!.isClosed) {
                try {
//                InputStream in = class.getClassLoader().getResourceAsStream("static/font/Torus-Regular.ttf");
//                TORUS_REGULAR = Typeface.makeFromData(Data.makeFromBytes(in.readAllBytes()));
                    field = Typeface.makeFromFile("${NowbotConfig.FONT_PATH}Torus-Regular.ttf")
                } catch (e: Exception) {
                    log.error("未读取到目标字体:Torus-Regular.ttf", e)
                    field = Typeface.makeDefault()
                }
            }
            return field
        }

    var TORUS_SEMIBOLD: Typeface? = null
        @JvmStatic get() {
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
        @JvmStatic get() {
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
        @JvmStatic get() {
            if (field == null || field!!.isClosed) {
                try {
                    field = Typeface.makeFromFile("${NowbotConfig.FONT_PATH}Alibaba-PuHuiTi-Medium.ttf")
                } catch (e: Exception) {
                    log.error("Alibaba-PuHuiTi-Medium.ttf", e)
                    field = Typeface.makeDefault()
                }
            }
            return field
        }

    var EXTRA: Typeface? = null
        @JvmStatic get() {
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

    /**
     * 将按逗号或者 |、:：分隔的字符串分割
     * 如果未含有分隔的字符，返回 null
     * @param str 需要分析的字符串
     * @return 玩家名列表
     */
    @JvmStatic
    @Nullable
    fun splitString(@Nullable str: String): List<String>? {
        if (!StringUtils.hasText(str)) return null
        val strings = str.trim { it <= ' ' }.split("[,，|:：`、]+".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray() //空格和-_不能匹配
        if (strings.size == 0) return null

        return Arrays.stream(strings).map { obj: String -> obj.trim { it <= ' ' } }
            .toList()
    }

    /**
     * 判定优秀成绩。用于临时区分 panel E 和 panel E5
     * @param score 成绩，需要先算好 pp，并使用完全体
     * @return 是否为优秀成绩
     */
    @NonNull
    fun isExcellentScore(@NonNull score: Score, user: OsuUser): Boolean {
        // 指标分别是：星数 >= 8，星数 >= 6.5，准确率 > 90%，连击 > 98%，PP > 300，PP > 玩家总 PP 减去 400 之后的 1/25 （上 BP，并且计 2 点），失误数 < 1%。
        var r = 0
        val p = getPP(score, user)

        val ultra = score.beatMap.starRating >= 8f
        val extreme = score.beatMap.starRating >= 6.5f
        val acc = score.accuracy >= 0.9f
        val combo = 1f * score.maxCombo / Objects.requireNonNullElse(score.beatMap.maxCombo, Int.MAX_VALUE) >= 0.98f
        val pp = score.pp >= 300f
        val bp = p >= 400f && score.pp >= (p - 400f) / 25f
        val miss =
            score.statistics.getCountAll(score.mode) > 0 && score.statistics.countMiss <= score.statistics.getCountAll(
                score.mode
            ) * 0.01f

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

    // 获取优秀成绩的私有方法
    private fun getPP(score: Score, user: OsuUser): Double {
        var pp = Objects.requireNonNullElse(user.pp, 0.0)

        val is4K = score.beatMap.osuMode == MANIA && score.beatMap.cs == 4f
        val is7K = score.beatMap.osuMode == MANIA && score.beatMap.cs == 7f

        if (is4K) {
            pp = Objects.requireNonNullElse(user.statistics.pP4K, pp)
        }

        if (is7K) {
            pp = Objects.requireNonNullElse(user.statistics.pP7K, pp)
        }

        return pp
    }

    /**
     * 将 !bp 45-55 转成 score API 能看懂的 offset-limit 对
     * @param start 开始
     * @param end 结束
     * @return offset
     */
    @JvmStatic
    @NonNull
    fun parseRange2Offset(@Nullable start: Int, @Nullable end: Int): Int {
        return parseRange(start, end).offset
    }

    /**
     * 将 !bp 45-55 转成 score API 能看懂的 offset-limit 对
     * @param start 开始
     * @param end 结束
     * @return limit
     */
    @JvmStatic
    @NonNull
    fun parseRange2Limit(@Nullable start: Int, @Nullable end: Int): Int {
        return parseRange(start, end).limit
    }

    /**
     * 将 !bp 45-55 转成 score API 能看懂的 offset-limit 对
     * @param start 开始
     * @param end 结束
     * @return offset-limit 对
     */
    @NonNull
    private fun parseRange(@Nullable start: Int, @Nullable end: Int): Range {
        var start = start
        val offset: Int
        val limit: Int

        if (Objects.isNull(start) || start < 1 || start > 100) start = 1

        if (Objects.isNull(end) || end < 1 || end > 100) {
            offset = start - 1
            limit = 1
        } else {
            //分流：正常，相等，相反
            if (end > start) {
                offset = start - 1
                limit = end - start + 1
            } else if (start == end) {
                offset = start - 1
                limit = 1
            } else {
                offset = end - 1
                limit = start - end + 1
            }
        }

        return Range(offset, limit)
    }

    /**
     * 根据准确率，通过获取原成绩的判定结果的彩率，来构建一个达到目标准确率的判定结果
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
            Arrays.asList(stat.countGeki, stat.count300, stat.countKatu, stat.count100, stat.count50, stat.countMiss)

        //一个物件所占的 Acc 权重
        if (total <= 0) return stat
        val weight = 1.0 / total

        //彩黄比
        val ratio =
            if ((stat.count300 + stat.countGeki > 0)) stat.countGeki * 1.0 / (stat.count300 + stat.countGeki) else 0.0

        var current = stat.getAccuracy(MANIA)

        if (current >= aiming) return stat

        //交换评级
        if (current < aiming && stat.countMiss > 0) {
            val ex = exchangeJudge(list.first(), list.last(), 1.0, 0.0, current, aiming, weight)
            list[0] = ex.great
            list[5] = ex.bad
            current = ex.accuracy
        }

        if (current < aiming && stat.count50 > 0) {
            val ex = exchangeJudge(list.first(), list[4], 1.0, 1.0 / 6.0, current, aiming, weight)
            list[0] = ex.great
            list[4] = ex.bad
            current = ex.accuracy
        }

        if (current < aiming && stat.count100 > 0) {
            val ex = exchangeJudge(list.first(), list[3], 1.0, 1.0 / 3.0, current, aiming, weight)
            list[0] = ex.great
            list[3] = ex.bad
            current = ex.accuracy
        }

        if (current < aiming && stat.countKatu > 0) {
            val ex = exchangeJudge(list.first(), list[2], 1.0, 2.0 / 3.0, current, aiming, weight)
            list[0] = ex.great
            list[2] = ex.bad
            // current = ex.accuracy;
        }

        val nGreat = list.first() + list[1]

        list[0] = floor(nGreat * ratio).toInt()
        list[1] = max((nGreat - list.first()).toDouble(), 0.0).toInt()

        stat.setCountGeki(list.first())
        stat.setCount300(list[1])
        stat.setCountKatu(list[2])
        stat.setCount100(list[3])
        stat.setCount50(list[4])
        stat.setCountMiss(list.last())

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
        if (str == null) return false

        return str.trim { it <= ' ' }.equals("help", ignoreCase = true) || str.trim { it <= ' ' }
            .equals("帮助", ignoreCase = true)
    }

    /**
     * 根据分隔符，分割玩家名
     * @param str 需要分割的，含分割符和玩家名的长文本
     * @return 分割好的玩家名
     */
    @JvmStatic
    @NonNull
    fun parseUsername(@Nullable str: String): List<String> {
        if (Objects.isNull(str)) return listOf("")
        val split = str.trim { it <= ' ' }.split("[,，、|:：]+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (split.size == 0) return listOf(str)

        return Arrays.stream(split).map { obj: String -> obj.trim { it <= ' ' } }
            .filter { str: String? -> StringUtils.hasText(str) }.toList()
    }

    fun String2Markdown(str: String): String {
        return str.replace("\n", "\n\n")
    }

    fun JsonString2Markdown(str: String): String? {
        if (Objects.isNull(str)) return null
        return str.replace("},", "},\n\n")
    }

    fun getRoundedNumberUnit(number: Double, level: Int): Char {
        var number = number
        var unit = '-'
        number = abs(number)
        if (level < 1 || level > 2) return '-'
        var m = 1 + level

        if (number < 10.0.pow(m)) {  //level==1->100 level==2->1000
            unit = 0.toChar()
        } else if (number < 10.0.pow(3.let { m += it; m })) {
            unit = 'K'
        } else if (number < 10.0.pow(3.let { m += it; m })) {
            unit = 'M'
        } else if (number < 10.0.pow(3.let { m += it; m })) {
            unit = 'G'
        } else if (number < 10.0.pow(3.let { m += it; m })) {
            unit = 'T'
        } else if (number < 10.0.pow((m + 3))) {
            unit = 'P'
        }

        return unit
    }


    fun getRoundedNumber(number: Double, level: Int): Double {
        // lv1.保留1位小数，结果不超4位字符宽(包含单位)
        //1-99-0.1K-9.9K-10K-99K-0.1M-9.9M-10M-99M-0.1G-9.9G-10G-99G-0.1T-9.9T-10T-99T-Inf.

        // lv2.保留2位小数，结果不超7位字符宽(包含单位)
        //1-999-1.00K-999.99K-1.00M-999.99M-1.00G-999.99G-...-999.9T-Inf.

        //将负值纳入计算

        var number = number
        while (number >= 1000 || number <= -1000) {
            number /= 1000.0
        }

        if (level == 1) {
            if (number >= 100) {
                number /= 1000.0
            }
            number = Math.round(number * 10).toDouble() / 10.0
        } else if (level == 2) {
            number = Math.round(number * 1000).toDouble() / 1000.0
        }
        if (number - Math.round(number) <= 0.0001) number = Math.round(number).toDouble()

        return number
    }

    fun getRoundedNumberStr(number: Double, level: Int): String {
        var number = number
        val c = getRoundedNumberUnit(number, level)
        val isInt: Boolean
        val intValue: Int
        if (c.code == 0) {
            intValue = number.toInt()
            isInt = if (level == 1) {
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
                number /= 1000.0
            }
            number = Math.round(number * 10).toDouble() / 10.0
        } else if (level == 2) {
            number = Math.round(number * 1000).toDouble() / 1000.0
        }
        intValue = number.toInt()
        isInt = if (level == 1) {
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

    fun Time2HourAndMinient(time: Long): String {
        if (time < 3600000) {
            return String.format("%dM", time / 60000)
        }
        val h = time / 3600000
        val m = (time % 3600000) / 60000
        return String.format("%dH%dM", h, m)
    }

    @JvmStatic
    fun AR2MS(ar: Float): Int = when {
        ar > 10 -> 450
        ar > 5 -> 1200 - (150 * (ar - 5)).toInt()
        ar > 0 -> 1800 - (120 * ar).toInt()
        else -> 1800
    }

    fun MS2AR(ms: Float): Float = when {
        ms < 300 -> 11f
        ms < 1200 -> 5 +  (1200 - ms) / 150f
        ms < 2400 -> (1800 - ms) / 120f
        else -> -5f
    }

    @JvmStatic
    fun AR(ar: Float, mod: Int): Float {
        var ar = ar
        //      1800  -  1200  -  450  -  300
        if (OsuMod.hasHr(mod)) {
            ar *= 1.4f
        } else if (OsuMod.hasEz(mod)) {
            ar /= 2f
        }
        var ms = AR2MS(ar).toFloat()
        if (OsuMod.hasDt(mod)) {
            ms /= (3f / 2f)
        } else if (OsuMod.hasHt(mod)) {
            ms /= (3f / 4f)
        }
        ar = MS2AR(ms)
        return roundTwoDecimals(ar)
    }

    @JvmStatic
    fun OD2MS(od: Float): Float = when {
        od > 10 -> 20f
        od > 0 -> 80 - 6 * od
        else -> 80f
    }

    fun MS2OD(ms: Float): Float {
        return (80 - ms) / 6f
    }

    @JvmStatic
    fun OD(od: Float, mod: Int): Float {
        var od = od
        if (OsuMod.hasHr(mod)) {
            od *= 1.4f
        } else if (OsuMod.hasEz(mod)) {
            od /= 2f
        }
        var ms = OD2MS(od)
        if (OsuMod.hasDt(mod)) {
            ms /= (3f / 2)
        } else if (OsuMod.hasHt(mod)) {
            ms /= (3f / 4)
        }
        od = MS2OD(ms)
        return roundTwoDecimals(od)
    }


    @JvmStatic
    fun CS(cs: Float, mod: Int): Float {
        var cs = cs
        if (OsuMod.hasHr(mod)) {
            cs *= 1.3f
        } else if (OsuMod.hasEz(mod)) {
            cs /= 2f
        }
        if (cs > 10) cs = 10f
        else if (cs < 0) cs = 0f
        return roundTwoDecimals(cs)
    }

    @JvmStatic
    fun BPM(bpm: Float, mod: Int): Float {
        var bpm = bpm
        if (OsuMod.hasDt(mod)) {
            bpm *= 1.5f
        } else if (OsuMod.hasHt(mod)) {
            bpm *= 0.75f
        }
        return roundTwoDecimals(bpm)
    }

    @JvmStatic
    fun Length(length: Float, mod: Int): Int {
        var length = length
        if (OsuMod.hasDt(mod)) {
            length /= 1.5f
        } else if (OsuMod.hasHt(mod)) {
            length /= 0.75f
        }
        return Math.round(length)
    }

    @JvmStatic
    fun HP(hp: Float, mod: Int): Float {
        var hp = hp
        if (OsuMod.hasHr(mod)) {
            hp *= 1.3f
        } else if (OsuMod.hasEz(mod)) {
            hp /= 1.3f
        }
        if (hp > 10) hp = 10f
        else if (hp < 0) hp = 0f
        return roundTwoDecimals(hp)
    }

    private fun roundTwoDecimals(value: Float): Float {
        return BigDecimal(value.toDouble()).setScale(2, RoundingMode.HALF_EVEN).toFloat()
    }

    // 应用四维的变化 4 dimensions
    @JvmStatic
    fun applyBeatMapChanges(beatMap: BeatMap, mods: Int) {
        if (OsuMod.hasChangeRating(mods)) {
            beatMap.bpm =
                BPM(Optional.ofNullable(beatMap.bpm).orElse(0f), mods)
            beatMap.ar = AR(Optional.ofNullable(beatMap.ar).orElse(0f), mods)
            beatMap.cs = CS(Optional.ofNullable(beatMap.cs).orElse(0f), mods)
            beatMap.od = OD(Optional.ofNullable(beatMap.od).orElse(0f), mods)
            beatMap.hp = HP(Optional.ofNullable(beatMap.hp).orElse(0f), mods)
            beatMap.totalLength = Length(beatMap.totalLength.toFloat(), mods)
        }
    }

    fun getPlayedRankedMapCount(bonusPP: Double): Int {
        val v = -(bonusPP / (1000f / 2.4f)) + 1
        return if (v < 0) {
            0
        } else {
            Math.round(ln(v) / ln(0.9994)).toInt()
        }
    }

    fun getBonusPP(playerPP: Double, fullPP: List<Double>): Float {
        if (CollectionUtils.isEmpty(fullPP)) return 0f

        val array = DoubleArray(fullPP.size)

        for (i in fullPP.indices) {
            array[i] = Objects.requireNonNullElse(fullPP[i], 0.0)
        }

        return getBonusPP(playerPP, array)
    }

    /**
     * 计算bonusPP
     * 算法是最小二乘 y = kx + b
     * 输入的PP数组应该是加权之前的数组。
     */
    @JvmStatic
    fun getBonusPP(playerPP: Double, fullPP: DoubleArray?): Float {
        val bonusPP: Double
        var remainPP = 0.0
        val k: Double
        val b: Double
        var bpPP = 0.0
        var x = 0.0
        var x2 = 0.0
        var xy = 0.0
        var y = 0.0

        if (fullPP == null || fullPP.size.toDouble() == 0.0) return 0f

        val length = fullPP.size

        for (i in 0 until length) {
            val weight: Double = 0.95.pow(i)
            val PP = fullPP[i]

            //只拿最后50个bp来算，这样精准
            if (i >= 50) {
                x += i.toDouble()
                y += PP
                x2 += i.toDouble().pow(2)
                xy += i * PP
            }
            bpPP += PP * weight //前 100 的bp上的 pp
        }

        val N = (length - 50).toDouble()
        // Exiyi - Nxy__ / Ex2i - Nx_2
        k = (xy - (x * y / N)) / (x2 - (x.pow(2.0) / N))
        b = (y / N) - k * (x / N)

        //找零点
        val expectedX = if ((k == 0.0)) -1 else floor(-b / k).toInt()

        //这个预估的零点应该在很后面，不应该小于 100
        //如果bp没满100，那么bns直接可算得，remainPP = 0
        if (length < 100 || expectedX <= 100) {
            bonusPP = playerPP - bpPP
        } else {
            //对离散数据求和
            for (i in length..expectedX) {
                val weight: Double = 0.95.pow(i)
                remainPP += (k * i + b) * weight
            }

            bonusPP = playerPP - bpPP - remainPP
        }

        return max(min(bonusPP, 413.894179759), 0.0).toFloat()
    }

    fun getV3ScoreProgress(score: Score): Double { //下下策
        val mode = score.mode

        val progress = if (!score.passed) {
            1.0 * score.statistics.getCountAll(mode) / score.beatMap.maxCombo
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
        val p = getV3ScoreProgress(score) //下下策
        val c = score.maxCombo
        val m = score.beatMap.maxCombo
        val ap8: Double = score.accuracy.pow(8.0)
        val v3 = when (score.mode) {
            OSU, CATCH, DEFAULT -> fc * i * (0.7f * c / m + 0.3f * ap8) * p
            TAIKO -> fc * i * (0.75f * c / m + 0.25f * ap8) * p
            MANIA -> fc * i * (0.01f * c / m + 0.99f * ap8) * p
        }

        return String.format("%07d", Math.round(v3)) //补 7 位达到 v3 分数的要求
    }

    // 这东西是啥?
    fun getV3ModsMultiplier(mod: List<String?>, mode: OsuMode?): Double {
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

            null, DEFAULT -> { }
        }
        return index
    }

    /***
     * 缩短字符 220924
     * @param Str 需要被缩短的字符
     * @param MaxWidth 最大宽度
     * @return 返回已缩短的字符
     */
    fun getShortenStr(Str: String, MaxWidth: Int): String {
        val sb = StringBuilder()
        val Char = Str.toCharArray()

        var allWidth = 0f
        var backL = 0

        for (thisChar in Char) {
            if (allWidth > MaxWidth) {
                break
            }
            sb.append(thisChar)
            if ((allWidth) < MaxWidth) {
                backL++
            }
        }
        if (allWidth > MaxWidth) {
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
     * @param path 文件名，和相对路径
     * @return 文件内容
     */
    @JvmStatic
    fun getMarkdownFile(path: String?): String? {
        val sb = StringBuilder()

        try {
            val bufferedReader = Files.newBufferedReader(
                Path.of(NowbotConfig.EXPORT_FILE_PATH).resolve(path)
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
            return null
        }
    }

    /**
     * 获取该文件名的图片文件并转成字符串
     * @param path 图片名，和相对路径
     * @return 图片流
     */
    @JvmStatic
    fun getPicture(path: String): ByteArray? {
        if (path.isEmpty()) return null

        return try {
            Files.readAllBytes(
                Path.of(NowbotConfig.EXPORT_FILE_PATH).resolve(path)
            )
        } catch (e: IOException) {
            null
        }
    }

    @JvmRecord
    private data class Range(val offset: Int, val limit: Int)

    @JvmRecord
    data class Exchange(val great: Int, val bad: Int, val accuracy: Double)
}
