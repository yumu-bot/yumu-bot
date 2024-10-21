package com.now.nowbot.model.enums

import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.throwable.ModsException
import java.util.*

enum class OsuMod(
        @JvmField val value: Int,
        @JvmField val acronym: String,
        @JvmField var speed: Double = 1.0,
        @JvmField var finalSpeed: Double = 1.0,
        @JvmField var cs: Double? = null,
        @JvmField var ar: Double? = null,
        @JvmField var od: Double? = null,
        @JvmField var hp: Double? = null,
) {
    None(0, ""),
    NoMod(0, "NM"),
    NoFail(1, "NF"),
    Easy(1 shl 1, "EZ"),
    TouchDevice(1 shl 2, "TD"), // 替换未使用的 No Video
    Hidden(1 shl 3, "HD"),
    HardRock(1 shl 4, "HR"),
    SuddenDeath(1 shl 5, "SD"),
    DoubleTime(1 shl 6, "DT", 1.5),
    Relax(1 shl 7, "RX"),
    HalfTime(1 shl 8, "HT", 0.75),
    Nightcore((1 shl 9) + (DoubleTime.value), "NC", 1.5), // 总是和 DT 一起使用 : 512 + 64 = 576
    Flashlight(1 shl 10, "FL"),
    Autoplay(1 shl 11, "AT"),
    SpunOut(1 shl 12, "SO"),
    Autopilot(1 shl 13, "AP"),
    Perfect(1 shl 14, "PF"),
    Key4(1 shl 15, "4K"),
    Key5(1 shl 16, "5K"),
    Key6(1 shl 17, "6K"),
    Key7(1 shl 18, "7K"),
    Key8(1 shl 19, "8K"),
    FadeIn(1 shl 20, "FI"),
    Random(1 shl 21, "RD"),
    Cinema(1 shl 22, "CM"),
    TargetPractice(1 shl 23, "TP"),
    Key9(1 shl 24, "9K"),
    KeyCoop(1 shl 25, "CP"),
    Key1(1 shl 26, "1K"),
    Key3(1 shl 27, "3K"),
    Key2(1 shl 28, "2K"),
    ScoreV2(1 shl 29, "V2"),
    Mirror(1 shl 30, "MR"), // mania 可以上传成绩
    KeyMod(521109504, "NK"),
    FreeMod(522171579, "FM"),
    ScoreIncreaseMods(1049688, "IM"),
    Other(-1, "??"),
    // 以下是 Lazer 独占的可以上传成绩的模组
    Daycore(-1, "DC", 0.75),
    Blinds(-1, "BL"),
    Cover(-1, "CO"),
    StrictTracking(-1, "ST"),
    AccuracyChallenge(-1, "AC"),
    DifficultyAdjust(-1, "DA"),
    Traceable(-1, "TC"),
    WindUp(-1, "WU", 1.0, 1.5),
    WindDown(-1, "WD", 1.0, 0.75),
    AdaptiveSpeed(-1, "AS"),
    Muted(-1, "MU"),
    NoScope(-1, "NS");

    fun add(old: Int): Int {
        return old or this.value
    }

    override fun toString(): String {
        return this.acronym
    }

    companion object {

        @JvmStatic
        fun getModFromLazerMod(mod: LazerScore.LazerMod?): OsuMod {
            if (mod == null) return None

            val e = getModFromAcronym(mod.acronym)

            if (mod.settings != null) {

                // DT HT
                if (mod.settings?.speed != null) e.speed = mod.settings!!.speed!!

                // DA
                if (mod.settings?.cs != null) e.cs = mod.settings!!.cs!!
                if (mod.settings?.ar != null) e.ar = mod.settings!!.ar!!
                if (mod.settings?.od != null) e.od = mod.settings!!.od!!
                if (mod.settings?.hp != null) e.hp = mod.settings!!.hp!!

                // AS WU WD，四维受 speed 影响，星数受 finalSpeed 影响
                if (mod.settings?.initialSpeed != null) e.speed = mod.settings!!.initialSpeed!!
                if (mod.settings?.finalSpeed != null) e.finalSpeed = mod.settings!!.finalSpeed!!
            }

            return e
        }

        @JvmStatic
        fun getModsList(acronym: String?): List<OsuMod> {
            if (acronym.isNullOrBlank()) return mutableListOf()

            val acronyms = splitModAcronyms(acronym.uppercase(Locale.getDefault()))
            val modList =
                    acronyms
                            .stream()
                            .map { a: String -> getModFromAcronym(acronym) }
                            .filter { e: OsuMod? -> e != Other }
                            .distinct()
                            .toList()
            checkMods(modList)
            return modList
        }

        @JvmStatic
        fun getModsList(mods: List<String>?): List<OsuMod> {
            if (mods.isNullOrEmpty()) return mutableListOf()

            val modList =
                    mods.stream()
                            .map { a: String -> getModFromAcronym(a.uppercase()) }
                            .filter { e: OsuMod? -> e != Other }
                            .distinct()
                            .toList()

            checkMods(modList)
            return modList
        }

        /**
         * 不能使用计算过的 Value
         *
         * @param value 值
         * @return 模组类列表
         */
        @JvmStatic
        fun getModsList(value: Int): List<OsuMod> {
            val modList =
                    Arrays.stream(entries.toTypedArray())
                            .filter { e: OsuMod? -> 0 != (e!!.value and value) }
                            .distinct()
                            .toList()
            checkMods(modList)
            return modList
        }

        /**
         * 不能使用计算过的 Value
         *
         * @param value 值
         * @return 缩写列表
         */
        @JvmStatic
        fun splitModAcronyms(value: Int): List<String> {
            val modList = getModsList(value)
            return modList.stream().map { obj: OsuMod? -> obj!!.acronym }.toList()
        }

        /**
         * 不能使用计算过的 Value
         *
         * @param value 值
         * @return 缩写组
         */
        @JvmStatic
        fun getModsAcronym(value: Int): String {
            return getModsList(value)
                    .stream()
                    .map { obj: OsuMod? -> obj!!.acronym }
                    .toList()
                    .joinToString { "" }
        }

        @JvmStatic
        fun getModsValue(acronym: String?): Int {
            if (acronym.isNullOrEmpty()) return 0
            checkMultiAcronym(acronym)
            val acronyms = splitModAcronyms(acronym.uppercase(Locale.getDefault()))
            return getModsValueFromAcronyms(acronyms)
        }

        /**
         * 这个没有检查，暂时专用于成绩的检查
         *
         * @param acronymArray 模组数组
         * @return 值
         */
        @JvmStatic
        fun getModsValue(acronymArray: Array<String>?): Int {
            if (acronymArray == null) return 0

            val mList =
                    Arrays.stream(acronymArray)
                            .map { obj: String -> obj.uppercase(Locale.getDefault()) }
                            .map { acronym: String -> getModFromAcronym(acronym) }
                            .filter { e: OsuMod? -> e != Other }
                            .distinct()
                            .toList()
            return getModsValue(mList)
        }

        @JvmStatic
        fun getModsValue(mods: List<OsuMod>?): Int {
            if (mods.isNullOrEmpty()) return 0

            checkMods(mods)

            return mods.stream()
                    .map { m: OsuMod? -> m!!.value }
                    .reduce(0) { result: Int, element: Int ->
                        if (element > 0) {
                            // 这里如果是 -1，会导致 JNI 无法计算准确的星数和 pp
                            return@reduce element or result
                        } else {
                            return@reduce result
                        }
                    }
        }

        @JvmStatic
        fun getModsValueFromAcronyms(acronyms: List<String>?): Int {
            if (acronyms.isNullOrEmpty()) return 0
            checkAcronyms(acronyms)

            return getModsValue(
                    acronyms
                            .stream()
                            .map { obj: String -> obj.uppercase(Locale.getDefault()) }
                            .map { acronym: String -> getModFromAcronym(acronym) }
                            .distinct()
                            .toList()
            )
        }

        @JvmStatic
        fun splitModAcronyms(acronyms: String?): List<String> {
            if (acronyms.isNullOrBlank()) return ArrayList(0)

            val newStr = acronyms.uppercase(Locale.getDefault()).replace("\\s+".toRegex(), "")
            if (newStr.length % 2 != 0) {
                throw ModsException(ModsException.Type.MOD_Receive_CharNotPaired)
            }
            val list =
                    Arrays.stream(
                                    newStr.split("(?<=\\w)(?=(\\w{2})+$)".toRegex())
                                            .dropLastWhile { it.isEmpty() }
                                            .toTypedArray()
                            )
                            .toList()
            checkAcronyms(list)

            return list
        }

        private fun checkMultiAcronym(acronym: String?) {
            if (acronym.isNullOrEmpty()) return

            val acronyms = splitModAcronyms(acronym.uppercase(Locale.getDefault()))
            val modList = mutableListOf<OsuMod>()

            for (a in acronyms) {
                val mod = getModFromAcronym(a)
                modList.add(mod)
            }

            checkMods(modList)
        }

        private fun checkAcronyms(acronyms: List<String>?) {
            if (acronyms.isNullOrEmpty()) return

            if (acronyms.contains(None.acronym) && acronyms.size > 1) {
                throw ModsException(ModsException.Type.MOD_Receive_Conflict, None.acronym)
            }
            if (acronyms.contains(DoubleTime.acronym) && acronyms.contains(HalfTime.acronym)) {
                throw ModsException(
                        ModsException.Type.MOD_Receive_Conflict,
                        "${DoubleTime.acronym} ${HalfTime.acronym}",
                )
            }
            if (acronyms.contains(HardRock.acronym) && acronyms.contains(Easy.acronym)) {
                throw ModsException(
                        ModsException.Type.MOD_Receive_Conflict,
                        "${HardRock.acronym} ${Easy.acronym}",
                )
            }
            if (
                    acronyms.contains(NoFail.acronym) &&
                            (acronyms.contains(SuddenDeath.acronym) ||
                                    acronyms.contains(Perfect.acronym))
            ) {
                throw ModsException(
                        ModsException.Type.MOD_Receive_Conflict,
                        "${NoFail.acronym} ${SuddenDeath.acronym} ${Perfect.acronym}",
                )
            }
            if (acronyms.contains(DoubleTime.acronym) && acronyms.contains(Nightcore.acronym)) {
                throw ModsException(
                        ModsException.Type.MOD_Receive_Conflict,
                        "${DoubleTime.acronym} ${Nightcore.acronym}",
                )
            }
        }

        private fun checkMods(mods: List<OsuMod>?) {
            if (mods.isNullOrEmpty()) return

            // TODO 暂时弃用这个检查，因为那些 Lazer 的模组是没有 value 的，会直接触发这个
            /*
            if (osuModList.contains(None) && osuModList.size > 1) {
                throw ModsException(ModsException.Type.MOD_Receive_Conflict, None.acronym)
            }

             */
            if (mods.contains(DoubleTime) && mods.contains(HalfTime)) {
                throw ModsException(
                        ModsException.Type.MOD_Receive_Conflict,
                        "${DoubleTime.acronym} ${HalfTime.acronym}",
                )
            }
            if (mods.contains(HardRock) && mods.contains(Easy)) {
                throw ModsException(
                        ModsException.Type.MOD_Receive_Conflict,
                        "${HardRock.acronym} ${Easy.acronym}",
                )
            }
            if (mods.contains(NoFail) && (mods.contains(SuddenDeath) || mods.contains(Perfect))) {
                throw ModsException(
                        ModsException.Type.MOD_Receive_Conflict,
                        "${NoFail.acronym} ${SuddenDeath.acronym} ${Perfect.acronym}",
                )
            }
            if (mods.contains(DoubleTime) && mods.contains(Nightcore)) {
                throw ModsException(
                        ModsException.Type.MOD_Receive_Conflict,
                        "${DoubleTime.acronym} ${Nightcore.acronym}",
                )
            }
        }

        /**
         * 这里不能用 getModsValue，会误报重复
         *
         * @param value 值
         * @return 倍率
         */
        @JvmStatic
        fun getModSpeed(value: Int): Double {
            return getModSpeed(
                    Arrays.stream(entries.toTypedArray())
                            .filter { e: OsuMod -> 0 != (e.value and value) }
                            .distinct()
                            .toList()
            )
        }

        @JvmStatic
        fun getModSpeed(acronym: String?): Double {
            val modList: List<OsuMod> = getModsList(acronym)
            return getModSpeed(modList)
        }

        @JvmStatic
        /** 如果要计算星数，请使用 getModSpeedForStarCalculate */
        fun getModSpeed(mods: List<OsuMod>?): Double {
            if (mods.isNullOrEmpty()) return 1.0

            for (m in mods) {
                if (
                        m == HalfTime ||
                                m == Daycore ||
                                m == DoubleTime ||
                                m == Nightcore ||
                                m == AdaptiveSpeed ||
                                m == WindUp ||
                                m == WindDown
                ) {
                    return m.speed
                }
            }

            return 1.0
        }

        @JvmStatic
        /** WU 和 WD 会改变速率，但是不会作用在谱面上。 */
        fun getModSpeedForStarCalculate(mods: List<OsuMod>?): Double {
            if (mods.isNullOrEmpty()) return 1.0

            for (m in mods) {
                if (
                        m == HalfTime ||
                                m == Daycore ||
                                m == DoubleTime ||
                                m == Nightcore ||
                                m == AdaptiveSpeed
                ) {
                    return m.speed
                }

                if (m == WindUp || m == WindDown) {
                    return m.finalSpeed
                }
            }

            return 1.0
        }

        @JvmStatic
        fun getModFromAcronym(acronym: String): OsuMod {
            return when (acronym.uppercase(Locale.getDefault())) {
                "",
                "NM" -> None
                "NF" -> NoFail
                "EZ" -> Easy
                "HT" -> HalfTime
                "TD" -> TouchDevice
                "HR" -> HardRock
                "HD" -> Hidden
                "FI" -> FadeIn
                "SD" -> SuddenDeath
                "PF" -> Perfect
                "DT" -> DoubleTime
                "NC" -> Nightcore
                "FL" -> Flashlight
                "RX" -> Relax
                "AP" -> Autopilot
                "AT" -> Autoplay
                "CM" -> Cinema
                "SO" -> SpunOut
                "CP" -> KeyCoop
                "MR" -> Mirror
                "RD" -> Random
                // 以下是 Lazer 独占的可以上传成绩的模组
                "DC" -> Daycore
                "BL" -> Blinds
                "CO" -> Cover
                "ST" -> StrictTracking
                "AC" -> AccuracyChallenge
                "DA" -> DifficultyAdjust
                "TC" -> Traceable
                "WU" -> WindUp
                "WD" -> WindDown
                "AS" -> AdaptiveSpeed
                "MU" -> Muted
                "NS" -> NoScope

                else -> None
            }
        }

        @JvmStatic
        fun hasMod(modList: List<OsuMod>?, mod: OsuMod): Boolean {
            return hasMod(getModsValue(modList), mod)
        }

        @JvmStatic
        fun hasMod(modInt: Int?, mod: OsuMod?): Boolean {
            if (modInt == null || mod == null) return false
            return (mod.value and modInt) != 0
        }

        @JvmStatic
        fun hasModFromAcronyms(acronyms: List<String>?, mod: OsuMod?): Boolean {
            return hasMod(getModsValueFromAcronyms(acronyms), mod)
        }

        @JvmStatic
        fun hasDt(i: Int): Boolean {
            return hasMod(i, DoubleTime) || hasMod(i, Nightcore)
        }

        @JvmStatic
        fun hasHt(i: Int): Boolean {
            return hasMod(i, HalfTime)
        }

        @JvmStatic
        fun hasHr(i: Int): Boolean {
            return hasMod(i, HardRock)
        }

        @JvmStatic
        fun hasEz(i: Int): Boolean {
            return hasMod(i, Easy)
        }

        @JvmStatic
        @Deprecated(
                "尽量用重载方法 List<OsuMod>，因为部分 lazer mod 是没有 value 的",
                ReplaceWith(
                        "hasChangeRating(getModsList(value))",
                        "com.now.nowbot.model.enums.OsuMod.Companion.hasChangeRating",
                        "com.now.nowbot.model.enums.OsuMod.Companion.getModsList",
                ),
        )
        fun hasChangeRating(value: Int): Boolean {
            return hasChangeRating(getModsList(value))
        }

        @JvmStatic
        fun hasChangeRating(mods: List<OsuMod?>?): Boolean {
            if (mods.isNullOrEmpty()) return false

            return mods.contains(Easy) ||
                    mods.contains(HardRock) ||
                    mods.contains(HalfTime) ||
                    mods.contains(Daycore) ||
                    mods.contains(DoubleTime) ||
                    mods.contains(Nightcore) ||
                    mods.contains(WindUp) ||
                    mods.contains(WindDown) ||
                    mods.contains(Flashlight) ||
                    mods.contains(TouchDevice) ||
                    mods.contains(DifficultyAdjust)
        }

        @JvmStatic
        fun hasChangeRatingFromAcronyms(acronyms: List<String>): Boolean {
            val v = getModsList(acronyms)
            return hasChangeRating(v)
        }

        fun add(old: Int, osuMod: OsuMod): Int {
            return old or osuMod.value
        }

        fun sub(old: Int, osuMod: OsuMod): Int {
            return old and osuMod.value.inv()
        }
    }
}
