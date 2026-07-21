package com.now.nowbot.model.enums

import com.fasterxml.jackson.annotation.JsonValue
import com.now.nowbot.model.osu.Beatmap
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

    val isDefault: Boolean
        get() = this.isDefaultOrNull()

    val isNotDefault: Boolean
        get() = this.isNotDefaultOrNull()

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
        fun String?.toOsuMode(orElse: OsuMode? = null): OsuMode {
            return getMode(this).orElse(orElse)
        }

        fun OsuMode?.takeUnlessDefault(): OsuMode? = this?.takeUnless { it == DEFAULT }

        fun OsuMode?.orElse(mode: OsuMode? = null): OsuMode = this.takeUnlessDefault() ?: mode ?: DEFAULT

        fun OsuMode?.orElse(value: Number?): OsuMode = this.takeUnlessDefault() ?: value.toOsuMode()

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
            val index = keysTable.binarySearch(this ?: return DEFAULT)
            return if (index >= 0) valuesTable[index] else DEFAULT
        }

        fun Number?.toOsuMode(): OsuMode {
            return this?.toByte().toOsuMode()
        }

        @JvmStatic
        fun getMode(num: Number?): OsuMode {
            return num.toOsuMode()
        }

        @OptIn(ExperimentalContracts::class)
        fun OsuMode?.isDefaultOrNull(): Boolean {
            contract {
                returns(false) implies (this@isDefaultOrNull != null)
            }

            return this == null || this == DEFAULT
        }

        @OptIn(ExperimentalContracts::class)
        fun OsuMode?.isNotDefaultOrNull(): Boolean {
            contract {
                returns(true) implies (this@isNotDefaultOrNull != null)
            }

            return this != null && this != DEFAULT
        }
        /**
         * 修正无法转换的模式：只有可转换的谱面才能赋予模式
         */
        fun OsuMode?.takeIfConvertable(beatmap: Beatmap?): OsuMode = this.takeIfConvertable(beatmap?.mode)

        /**
         * 修正无法转换的模式：只有可转换的谱面才能赋予模式
         */
        fun OsuMode?.takeIfConvertable(beatmapMode: OsuMode?): OsuMode =
            if (this.isDefaultOrNull() || (beatmapMode != null && beatmapMode.safeModeValue != 0.toByte())) {
                beatmapMode ?: DEFAULT
            } else {
                this
            }

        /**
         * 修正无法转换的模式：只有可转换的谱面才能赋予模式
         */
        fun getConvertableMode(convert: OsuMode?, map: OsuMode?): OsuMode {
            return if (convert.isDefaultOrNull() || (map != null && map != OSU && map != DEFAULT)) {
                map ?: DEFAULT
            } else {
                convert
            }
        }

        fun OsuMode?.getQuery(): Optional<String> = if (this.isDefaultOrNull()) {
            Optional.empty()
        } else {
            Optional.of(this.shortName)
        }

        fun me.aloic.rosupp.GameMode.toOsuMode(): OsuMode {
            return when (this) {
                me.aloic.rosupp.GameMode.OSU -> OSU
                me.aloic.rosupp.GameMode.TAIKO -> TAIKO
                me.aloic.rosupp.GameMode.CATCH -> CATCH
                me.aloic.rosupp.GameMode.MANIA -> MANIA
            }
        }

        fun OsuMode.toRosuMode(): me.aloic.rosupp.GameMode {
            return when (this.safeModeValue) {
                0.toByte() -> me.aloic.rosupp.GameMode.OSU
                1.toByte() -> me.aloic.rosupp.GameMode.TAIKO
                2.toByte() -> me.aloic.rosupp.GameMode.CATCH
                3.toByte() -> me.aloic.rosupp.GameMode.MANIA

                else -> me.aloic.rosupp.GameMode.OSU
            }
        }
    }
}
