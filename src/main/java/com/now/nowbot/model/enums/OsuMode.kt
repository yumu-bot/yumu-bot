package com.now.nowbot.model.enums

import com.fasterxml.jackson.annotation.JsonValue
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

enum class OsuMode(val fullName: String, val shortName: String, val charName: String, val modeValue: Byte) {
    OSU("osu!standard", "osu", "o", 0),
    TAIKO("osu!taiko", "taiko", "t", 1),
    CATCH("osu!catch", "fruits", "c", 2),
    MANIA("osu!mania", "mania", "m", 3),
    DEFAULT("default", "", "", -1),
    OSU_RELAX("osu!standard relax", "osu", "o", 4),
    TAIKO_RELAX("osu!taiko relax", "taiko", "t",  5),
    CATCH_RELAX("osu!catch relax", "fruits", "c",  6),
    MANIA_7K("osu!mania", "mania", "m", 7),
    OSU_AUTOPILOT("osu!standard autopilot", "osu", "o",  8),

    ;

    override fun toString(): String {
        return fullName
    }

    @JsonValue
    fun value() = this.shortName

    val safeModeValue: Byte 
        get() = when (this) {
            OSU, OSU_RELAX, OSU_AUTOPILOT -> 0
            TAIKO, TAIKO_RELAX -> 1
            CATCH, CATCH_RELAX -> 2
            MANIA, MANIA_7K -> 3
            DEFAULT -> 0
        }

    fun isDefault(): Boolean {
        return isDefaultOrNull(this)
    }

    fun isNotDefault(): Boolean {
        return isNotDefaultOrNull(this)
    }

    /**
     * 当且仅当这是转谱组合时（主模式搭配输入的其他模式）
     */
    fun isConvertAble(mode: OsuMode?): Boolean {
        return this.safeModeValue == 0.toByte() && (mode?.safeModeValue != 0.toByte())
    }

    /**
     * 返回相等，或是 mode 等于默认。
     * 它位于 ConvertAble 和 NotConvertAble 之间
     */
    fun isEqualOrDefault(mode: OsuMode?): Boolean {
        return mode == DEFAULT || this.safeModeValue == mode?.safeModeValue
    }

    /**
     * 返回不可以被 mode 转换
     */
    fun isNotConvertAble(mode: OsuMode?): Boolean {
        return (this == TAIKO || this == CATCH || this == MANIA) && (mode != DEFAULT && mode != this)
    }

    companion object {
        /**
         * 当 mode 为 Default 或者 mode == mode2 时，返回 true
         */
        fun equalOrDefault(mode: OsuMode, mode2: Any?) : Boolean {
            return mode2 is OsuMode && (mode == mode2 || mode == DEFAULT)
        }

        fun getMode(str: String?, default: String?): OsuMode {
            val mode = getMode(default)
            if (DEFAULT != mode) return getMode(str, mode)
            return getMode(str)
        }

        fun getMode(str: String?, default: OsuMode?): OsuMode {
            val mode = getMode(str)
            if (DEFAULT == mode) return default ?: DEFAULT
            return mode
        }

        @JvmStatic
        fun getMode(mode: OsuMode?, default: OsuMode?): OsuMode {
            if (isDefaultOrNull(mode)) return default ?: DEFAULT
            return mode
        }

        /**
         * 用于覆盖默认的游戏模式。优先级：mode > groupMode > selfMode
         * @param mode 玩家查询时输入的游戏模式
         * @param selfMode 一般是玩家自己绑定的游戏模式
         * @param groupMode 群聊绑定游戏模式
         */

        @JvmStatic
        fun getMode(mode: OsuMode?, selfMode: OsuMode?, groupMode: OsuMode?): OsuMode {
            if (isNotDefaultOrNull(mode)) return mode
            if (isNotDefaultOrNull(groupMode)) return groupMode
            return selfMode ?: DEFAULT
        }

        @JvmStatic
        fun getMode(name: String?): OsuMode {
            return when (name?.replace(" ", "")?.trim()?.lowercase()) {
                "taiko", "t", "1", "osu!taiko" -> TAIKO
                "catch", "catchthebeat", "ctb", "c", "fruits", "fruit", "f", "2", "osu!catch" -> CATCH
                "mania", "m", "3", "osu!mania" -> MANIA
                "osu", "o", "0", "osu!", "std", "standard" -> OSU
                "osu relax", "osurelax", "std relax", "stdrelax", "osurx", "osu rx", "or", "4", "rx0" -> OSU_RELAX
                "taiko relax", "taikorelax", "taikorx", "tr", "5", "rx1" -> TAIKO_RELAX
                "catch relax", "catchrelax", "catchrx", "cr", "6", "rx2" -> CATCH_RELAX
                "mania7k", "7k", "7", "osu!mania7k" -> MANIA_7K
                "osu autopilot", "osuautopilot", "autopilot", "stdap", "std ap", "osuap", "oa", "ap", "8" -> OSU_AUTOPILOT

                else -> DEFAULT
            }
        }

        private val sortedEntries = OsuMode.entries.sortedBy { it.modeValue }

        // 2. 提取出纯基本类型的 IntArray，用于二分查找（无装箱开销）
        private val keysTable: ByteArray = sortedEntries.map { it.modeValue }.toByteArray()

        // 3. 提取出对应的枚举实例数组
        private val valuesTable: Array<OsuMode> = sortedEntries.toTypedArray()

        fun Byte?.toOsuMode(): OsuMode {
            return getMode(this)
        }

        fun Number?.toOsuMode(): OsuMode {
            return getMode(this?.toByte())
        }

        @JvmStatic
        fun getMode(num: Byte?): OsuMode {
            val index = keysTable.binarySearch(num ?: return DEFAULT)
            return if (index >= 0) valuesTable[index] else DEFAULT
        }

        @JvmStatic
        fun getMode(num: Int?): OsuMode {
            return getMode(num?.toByte())
        }

        @JvmStatic
        @OptIn(ExperimentalContracts::class)
        fun isDefaultOrNull(mode: OsuMode?): Boolean {
            contract {
                returns(false) implies (mode != null)
            }
            return mode == null || mode == DEFAULT
        }

        @JvmStatic
        @OptIn(ExperimentalContracts::class)
        fun isNotDefaultOrNull(mode: OsuMode?): Boolean {
            contract {
                returns(true) implies (mode != null)
            }
            return mode != null && mode != DEFAULT
        }

        /**
         * 修正无法转换的模式：只有可转换的谱面才能赋予模式
         */
        fun getConvertableMode(convert: OsuMode?, map: OsuMode?): OsuMode {
            return if (isDefaultOrNull(convert) || (map != null && map != OSU && map != DEFAULT)) {
                map ?: DEFAULT
            } else {
                convert
            }
        }

        fun getQueryName(mode: OsuMode?): Optional<String> {
            if (mode == null) {
                return Optional.empty()
            }
            if (DEFAULT == mode) {
                return Optional.empty()
            }
            return Optional.of(mode.shortName)
        }
    }
}
