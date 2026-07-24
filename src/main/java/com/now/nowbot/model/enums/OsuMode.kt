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
        return (this.safeModeValue in 1..3) && (mode != DEFAULT && mode?.safeModeValue != this.safeModeValue)
    }

    companion object {
        fun OsuMode?.takeUnlessDefault(): OsuMode? = this?.takeUnless { it == DEFAULT }

        fun OsuMode?.orElse(mode: OsuMode? = null): OsuMode = this.takeUnlessDefault() ?: mode ?: DEFAULT

        fun OsuMode?.orElse(value: Number?): OsuMode = this.takeUnlessDefault() ?: value.toOsuMode()

        private val MODE_LOOKUP_MAP: Map<String, OsuMode> by lazy {
            buildMap {
                fun register(mode: OsuMode, vararg aliases: String) {
                    aliases.forEach { put(it, mode) }
                }

                register(TAIKO, "taiko", "t", "1", "osu!taiko")
                register(CATCH, "catch", "catchthebeat", "ctb", "c", "fruits", "fruit", "f", "2", "osu!catch")
                register(MANIA, "mania", "m", "3", "osu!mania")
                register(OSU, "osu", "o", "0", "osu!", "std", "standard")
                register(OSU_RELAX, "osurelax", "stdrelax", "osurx", "or", "4", "rx0")
                register(TAIKO_RELAX, "taikorelax", "taikorx", "tr", "5", "rx1")
                register(CATCH_RELAX, "catchrelax", "catchrx", "cr", "6", "rx2")
                register(MANIA_7K, "mania7k", "7k", "7", "osu!mania7k")
                register(OSU_AUTOPILOT, "osuautopilot", "autopilot", "stdap", "osuap", "oa", "ap", "8")
            }
        }

        private val sortedEntries = OsuMode.entries.sortedBy { it.modeValue }

        private val keysTable: ByteArray = sortedEntries.map { it.modeValue }.toByteArray()

        private val valuesTable: Array<OsuMode> = sortedEntries.toTypedArray()

        fun Byte?.toOsuMode(): OsuMode {
            val index = keysTable.binarySearch(this ?: return DEFAULT)
            return if (index >= 0) valuesTable[index] else DEFAULT
        }

        fun Number?.toOsuMode(): OsuMode {
            return this?.toByte().toOsuMode()
        }

        fun String?.toOsuMode(orElse: OsuMode? = null): OsuMode {
            fun getMode(name: String?): OsuMode {
                if (name.isNullOrBlank()) return DEFAULT

                val key = name.replace(" ", "").lowercase()
                return MODE_LOOKUP_MAP[key] ?: DEFAULT
            }

            return getMode(this).orElse(orElse)
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
