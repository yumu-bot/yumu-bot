package com.now.nowbot.model

import com.now.nowbot.model.enums.LazerModType
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.throwable.ModsException
import java.util.*

data class LazerMod(@JvmField val type: LazerModType) {
    var speed: Double = 1.0
        get() {
            field =
                    if ((type == LazerModType.DoubleTime || type == LazerModType.Nightcore) && field == 1.0) {
                        1.5
                    } else if ((type == LazerModType.HalfTime || type == LazerModType.Daycore) && field == 1.0) {
                        0.75
                    } else {
                        field
                    }

            return field
        }

    var finalSpeed: Double = 1.0
        get() {
            field =
                    if (type == LazerModType.WindUp && field == 1.0) {
                        1.5
                    } else if (type == LazerModType.WindDown && field == 1.0) {
                        0.75
                    } else {
                        field
                    }

            return field
        }

    var cs: Double? = null
    var ar: Double? = null
    var od: Double? = null
    var hp: Double? = null

    constructor(type: LazerModType, speed: Double = 1.0, finalSpeed: Double = 1.0) : this(type) {
        this.speed = speed
        this.finalSpeed = finalSpeed
    }

    constructor(type: LazerModType, cs: Double, ar: Double, od: Double, hp: Double) : this(type) {
        this.cs = cs
        this.ar = ar
        this.od = od
        this.hp = hp
    }

    override fun toString(): String {
        return this.type.acronym
    }

    companion object {
        @JvmStatic
        fun contains(mods: List<LazerMod?>?, type: LazerModType?): Boolean {
            if (mods == null || type == null) return false

            val t = mods.stream().map { it?.type }.toList()
            return t.contains(type)
        }

        @JvmStatic
        fun getModFromAcronym(acronym: String?): LazerMod {
            return LazerMod(getTypeFromAcronym(acronym))
        }

        @JvmStatic
        fun getTypeFromAcronym(acronym: String?): LazerModType {
            return when (acronym?.uppercase(Locale.getDefault())) {
                "",
                "NM" -> LazerModType.None
                "NF" -> LazerModType.NoFail
                "EZ" -> LazerModType.Easy
                "HT" -> LazerModType.HalfTime
                "TD" -> LazerModType.TouchDevice
                "HR" -> LazerModType.HardRock
                "HD" -> LazerModType.Hidden
                "FI" -> LazerModType.FadeIn
                "SD" -> LazerModType.SuddenDeath
                "PF" -> LazerModType.Perfect
                "DT" -> LazerModType.DoubleTime
                "NC" -> LazerModType.Nightcore
                "FL" -> LazerModType.Flashlight
                "RX" -> LazerModType.Relax
                "AP" -> LazerModType.Autopilot
                "AT" -> LazerModType.Autoplay
                "CM" -> LazerModType.Cinema
                "SO" -> LazerModType.SpunOut
                "CP" -> LazerModType.KeyCoop
                "MR" -> LazerModType.Mirror
                "RD" -> LazerModType.Random
                // 以下是 Lazer 独占的可以上传成绩的模组
                "DC" -> LazerModType.Daycore
                "BL" -> LazerModType.Blinds
                "CO" -> LazerModType.Cover
                "ST" -> LazerModType.StrictTracking
                "AC" -> LazerModType.AccuracyChallenge
                "DA" -> LazerModType.DifficultyAdjust
                "TC" -> LazerModType.Traceable
                "WU" -> LazerModType.WindUp
                "WD" -> LazerModType.WindDown
                "AS" -> LazerModType.AdaptiveSpeed
                "MU" -> LazerModType.Muted
                "NS" -> LazerModType.NoScope

                else -> LazerModType.None
            }
        }

        @JvmStatic
        fun getModFromScoreMod(mod: LazerScore.ScoreMod?): LazerMod {
            if (mod == null) return LazerMod(LazerModType.None)

            val m = getModFromAcronym(mod.acronym)

            if (mod.settings != null) {

                // DT HT
                if (mod.settings?.speed != null) m.speed = mod.settings!!.speed!!

                // DA
                if (mod.settings?.cs != null) m.cs = mod.settings!!.cs!!
                if (mod.settings?.ar != null) m.ar = mod.settings!!.ar!!
                if (mod.settings?.od != null) m.od = mod.settings!!.od!!
                if (mod.settings?.hp != null) m.hp = mod.settings!!.hp!!

                // AS WU WD，四维受 speed 影响，星数受 finalSpeed 影响
                if (mod.settings?.initialSpeed != null) m.speed = mod.settings!!.initialSpeed!!
                if (mod.settings?.finalSpeed != null) m.finalSpeed = mod.settings!!.finalSpeed!!
            }

            return m
        }

        @JvmStatic
        fun getModsList(acronym: String?): List<LazerMod> {
            if (acronym.isNullOrBlank()) return mutableListOf()

            val acronyms = splitModAcronyms(acronym.uppercase(Locale.getDefault()))
            val modList =
                    acronyms
                            .stream()
                            .map { a: String -> getModFromAcronym(acronym) }
                            .filter { e: LazerMod? -> e?.type != LazerModType.Other }
                            .distinct()
                            .toList()
            checkMods(modList)
            return modList
        }

        @JvmStatic
        fun getModsList(mods: List<String>?): List<LazerMod> {
            if (mods.isNullOrEmpty()) return mutableListOf()

            val modList =
                    mods.stream()
                            .map { s -> getModFromAcronym(s.uppercase()) }
                            .filter { e -> e.type != LazerModType.Other }
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
        fun getModsList(value: Int): List<LazerMod> {
            val modList =
                    Arrays.stream(LazerModType.entries.toTypedArray())
                            .filter { e: LazerModType? -> 0 != (e!!.value and value) }
                            .map { t -> LazerMod(t) }
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
            return modList.stream().map { m -> m.type.acronym }.toList()
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
                    .map { m -> m.type.acronym }
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
        fun getModsValue(acronymArray: Array<String?>?): Int {
            if (acronymArray == null) return 0

            val mList =
                    Arrays.stream(acronymArray)
                            .map { acronym: String? ->
                                getModFromAcronym(acronym?.uppercase(Locale.getDefault()))
                            }
                            .filter { e -> e.type != LazerModType.Other }
                            .distinct()
                            .toList()
            return getModsValue(mList)
        }

        @JvmStatic
        fun getModsValue(mods: List<LazerMod?>?): Int {
            if (mods.isNullOrEmpty()) return 0

            checkMods(mods)

            return mods.stream()
                    .map { m -> m?.type?.value ?: -1 }
                    .filter { it >= 0 }
                    .reduce(0) { a, b -> a + b }
        }

        @JvmStatic
        fun getModsValueFromAcronyms(acronyms: List<String?>?): Int {
            if (acronyms.isNullOrEmpty()) return 0
            checkAcronyms(acronyms)

            return getModsValue(
                    acronyms
                            .stream()
                            .map { acronym: String? ->
                                getModFromAcronym(acronym?.uppercase(Locale.getDefault()))
                            }
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
            val modList = mutableListOf<LazerMod>()

            for (a in acronyms) {
                val mod = getModFromAcronym(a)
                modList.add(mod)
            }

            checkMods(modList)
        }

        private fun checkAcronyms(acronyms: List<String?>?) {
            if (acronyms.isNullOrEmpty()) return

            if (acronyms.contains(LazerModType.None.acronym) && acronyms.size > 1) {
                throw ModsException(ModsException.Type.MOD_Receive_Conflict, LazerModType.None.acronym)
            }
            if (
                    acronyms.contains(LazerModType.DoubleTime.acronym) &&
                            acronyms.contains(LazerModType.HalfTime.acronym)
            ) {
                throw ModsException(
                        ModsException.Type.MOD_Receive_Conflict,
                        "${LazerModType.DoubleTime.acronym} ${LazerModType.HalfTime.acronym}",
                )
            }
            if (acronyms.contains(LazerModType.HardRock.acronym) && acronyms.contains(LazerModType.Easy.acronym)) {
                throw ModsException(
                        ModsException.Type.MOD_Receive_Conflict,
                        "${LazerModType.HardRock.acronym} ${LazerModType.Easy.acronym}",
                )
            }
            if (
                    acronyms.contains(LazerModType.NoFail.acronym) &&
                            (acronyms.contains(LazerModType.SuddenDeath.acronym) ||
                                    acronyms.contains(LazerModType.Perfect.acronym))
            ) {
                throw ModsException(
                        ModsException.Type.MOD_Receive_Conflict,
                        "${LazerModType.NoFail.acronym} ${LazerModType.SuddenDeath.acronym} ${LazerModType.Perfect.acronym}",
                )
            }
            if (
                    acronyms.contains(LazerModType.DoubleTime.acronym) &&
                            acronyms.contains(LazerModType.Nightcore.acronym)
            ) {
                throw ModsException(
                        ModsException.Type.MOD_Receive_Conflict,
                        "${LazerModType.DoubleTime.acronym} ${LazerModType.Nightcore.acronym}",
                )
            }
        }

        private fun checkMods(mods: List<LazerMod?>?) {
            if (mods.isNullOrEmpty()) return

            val type = mods.stream().map{it ?.type}.toList()

            // TODO 暂时弃用这个检查，因为那些 Lazer 的模组是没有 value 的，会直接触发这个
            /*
            if (osuModList.contains(None) && osuModList.size > 1) {
                throw ModsException(ModsException.Type.MOD_Receive_Conflict, None.acronym)
            }

             */
            if (type.contains(LazerModType.DoubleTime) && type.contains(LazerModType.HalfTime)) {
                throw ModsException(
                        ModsException.Type.MOD_Receive_Conflict,
                        "${LazerModType.DoubleTime.acronym} ${LazerModType.HalfTime.acronym}",
                )
            }
            if (type.contains(LazerModType.HardRock) && type.contains(LazerModType.Easy)) {
                throw ModsException(
                        ModsException.Type.MOD_Receive_Conflict,
                        "${LazerModType.HardRock.acronym} ${LazerModType.Easy.acronym}",
                )
            }
            if (
                    type.contains(LazerModType.NoFail) &&
                            (type.contains(LazerModType.SuddenDeath) || type.contains(LazerModType.Perfect))
            ) {
                throw ModsException(
                        ModsException.Type.MOD_Receive_Conflict,
                        "${LazerModType.NoFail.acronym} ${LazerModType.SuddenDeath.acronym} ${LazerModType.Perfect.acronym}",
                )
            }
            if (type.contains(LazerModType.DoubleTime) && type.contains(LazerModType.Nightcore)) {
                throw ModsException(
                        ModsException.Type.MOD_Receive_Conflict,
                        "${LazerModType.DoubleTime.acronym} ${LazerModType.Nightcore.acronym}",
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
                    Arrays.stream(LazerModType.entries.toTypedArray())
                            .filter { e: LazerModType -> 0 != (e.value and value) }
                            .map { e -> LazerMod(e) }
                            .distinct()
                            .toList()
            )
        }

        @JvmStatic
        fun getModSpeed(acronym: String?): Double {
            val modList: List<LazerMod> = getModsList(acronym)
            return getModSpeed(modList)
        }

        @JvmStatic
        /** 如果要计算星数，请使用 getModSpeedForStarCalculate */
        fun getModSpeed(mods: List<LazerMod>?): Double {
            if (mods.isNullOrEmpty()) return 1.0

            for (m in mods) {
                val t = m.type

                if (
                        t == LazerModType.HalfTime ||
                                t == LazerModType.Daycore ||
                                t == LazerModType.DoubleTime ||
                                t == LazerModType.Nightcore ||
                                t == LazerModType.AdaptiveSpeed ||
                                t == LazerModType.WindUp ||
                                t == LazerModType.WindDown
                ) {
                    return m.speed
                }
            }

            return 1.0
        }

        @JvmStatic
        /** WU 和 WD 会改变速率，但是不会作用在谱面上。 */
        fun getModSpeedForStarCalculate(mods: List<LazerMod>?): Double {
            if (mods.isNullOrEmpty()) return 1.0

            for (m in mods) {
                val t = m.type
                if (
                    t == LazerModType.HalfTime ||
                    t == LazerModType.Daycore ||
                    t == LazerModType.DoubleTime ||
                    t == LazerModType.Nightcore ||
                    t == LazerModType.AdaptiveSpeed
                ) {
                    return m.speed
                }

                if (t == LazerModType.WindUp || t == LazerModType.WindDown) {
                    return m.finalSpeed
                }
            }

            return 1.0
        }

        @JvmStatic
        fun hasMod(mods: List<LazerMod?>?, type: LazerModType?): Boolean {
            if (mods == null || type == null) return false

            return mods.stream().map { it?.type }.filter { it != null }.toList().contains(type)
        }

        @JvmStatic
        fun hasMod(modInt: Int?, type: LazerModType?): Boolean {
            if (modInt == null || type == null) return false
            return (type.value and modInt) != 0
        }

        @JvmStatic
        fun hasModFromAcronyms(acronyms: List<String>?, type: LazerModType?): Boolean {
            if (acronyms == null || type == null) return false
            return acronyms.contains(type.acronym)
        }

        @JvmStatic
        fun hasDt(i: Int): Boolean {
            return hasMod(i, LazerModType.DoubleTime) || hasMod(i, LazerModType.Nightcore)
        }

        @JvmStatic
        fun hasHt(i: Int): Boolean {
            return hasMod(i, LazerModType.HalfTime)
        }

        @JvmStatic
        fun hasHr(i: Int): Boolean {
            return hasMod(i, LazerModType.HardRock)
        }

        @JvmStatic
        fun hasEz(i: Int): Boolean {
            return hasMod(i, LazerModType.Easy)
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
        fun hasChangeRating(mods: List<LazerMod?>?): Boolean {
            if (mods.isNullOrEmpty()) return false

            val type = mods.stream().map { it?.type }.filter { it != LazerModType.Other }.toList()

            return type.contains(LazerModType.Easy) ||
                    type.contains(LazerModType.HardRock) ||
                    type.contains(LazerModType.HalfTime) ||
                    type.contains(LazerModType.Daycore) ||
                    type.contains(LazerModType.DoubleTime) ||
                    type.contains(LazerModType.Nightcore) ||
                    type.contains(LazerModType.WindUp) ||
                    type.contains(LazerModType.WindDown) ||
                    type.contains(LazerModType.Flashlight) ||
                    type.contains(LazerModType.TouchDevice) ||
                    type.contains(LazerModType.DifficultyAdjust) ||
                    type.contains(LazerModType.AdaptiveSpeed)
        }

        @JvmStatic
        fun hasChangeRatingFromAcronyms(acronyms: List<String>): Boolean {
            val v = getModsList(acronyms)
            return hasChangeRating(v)
        }

        fun add(old: Int, type: LazerModType): Int {
            return old or type.value
        }

        fun sub(old: Int, type: LazerModType): Int {
            return old and type.value.inv()
        }
    }
}
