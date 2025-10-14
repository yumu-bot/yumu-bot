package com.now.nowbot.model.enums

import org.springframework.lang.Nullable
import java.util.*

enum class OsuMode(@JvmField val fullName: String, @JvmField val shortName: String, @JvmField val modeValue: Byte) {
    OSU("osu!standard", "osu", 0),
    TAIKO("osu!taiko", "taiko", 1),
    CATCH("osu!catch", "fruits", 2),
    MANIA("osu!mania", "mania", 3),
    DEFAULT("default", "", -1),
    OSU_RELAX("osu!standard relax", "osu", 4),
    TAIKO_RELAX("osu!taiko relax", "taiko", 5),
    CATCH_RELAX("osu!catch relax", "fruits", 6),
    OSU_AUTOPILOT("osu!standard autopilot", "osu", 8),

    ;

    override fun toString(): String {
        return fullName
    }

    fun toRosuMode(): org.spring.osu.OsuMode {
        return when (this) {
            OSU, OSU_RELAX, OSU_AUTOPILOT -> org.spring.osu.OsuMode.Osu
            TAIKO, TAIKO_RELAX -> org.spring.osu.OsuMode.Taiko
            CATCH, CATCH_RELAX -> org.spring.osu.OsuMode.Catch
            MANIA -> org.spring.osu.OsuMode.Mania
            else -> org.spring.osu.OsuMode.Default
        }
    }

    fun isDefault(): Boolean {
        return isDefaultOrNull(this)
    }

    fun isNotDefault(): Boolean {
        return isNotDefaultOrNull(this)
    }

    /**
     * 返回可以被 mode 转换
     */
    fun isConvertAble(mode: OsuMode?): Boolean {
        return this == OSU && (mode == TAIKO || mode == CATCH || mode == MANIA)
    }

    /**
     * 返回相等，或是 mode 等于默认。
     * 它位于 ConvertAble 和 NotConvertAble 之间
     */
    fun isEqualOrDefault(mode: OsuMode?): Boolean {
        return mode == DEFAULT || this == mode
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
            if (mode2 is OsuMode) {
                return mode == mode2 || mode == DEFAULT
            }

            return false
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

        @JvmStatic fun getMode(mode: OsuMode?, default: OsuMode?): OsuMode {
            if (isDefaultOrNull(mode)) return default ?: DEFAULT
            return mode!!
        }

        /**
         * 用于覆盖默认的游戏模式。优先级：mode > groupMode > selfMode
         * @param mode 玩家查询时输入的游戏模式
         * @param selfMode 一般是玩家自己绑定的游戏模式
         * @param groupMode 群聊绑定游戏模式
         */
        @JvmStatic fun getMode(mode: OsuMode?, selfMode: OsuMode?, groupMode: OsuMode?): OsuMode {
            if (isNotDefaultOrNull(mode)) return mode!!
            if (isNotDefaultOrNull(groupMode)) return groupMode!!
            return selfMode ?: DEFAULT
        }

        @JvmStatic fun getMode(@Nullable name: String?): OsuMode {
            return when (name?.replace(" ", "")?.trim()?.lowercase()) {
                "taiko", "t", "1", "osu!taiko" -> TAIKO
                "catch", "ctb", "c", "fruits", "f", "2", "osu!catch" -> CATCH
                "mania", "m", "3", "osu!mania" -> MANIA
                "osu", "o", "0", "osu!" -> OSU
                "osu relax", "osurelax", "std relax", "stdrelax", "osurx", "osu rx", "4", "rx0" -> OSU_RELAX
                "taiko relax", "taikorelax", "taikorx", "tr", "5", "rx1" -> TAIKO_RELAX
                "catch relax", "catchrelax", "catchrx", "cr", "6", "rx2" -> CATCH_RELAX
                "osu autopilot", "autopilot", "stdap", "std ap", "osuap", "oa", "8" -> OSU_AUTOPILOT

                else -> DEFAULT
            }
        }

        @JvmStatic fun getMode(@Nullable num: Number?): OsuMode {
            return when (num?.toInt()) {
                0 -> OSU
                1 -> TAIKO
                2 -> CATCH
                3 -> MANIA
                4 -> OSU_RELAX
                5 -> TAIKO_RELAX
                6 -> CATCH_RELAX
                8 -> OSU_AUTOPILOT
                else -> DEFAULT
            }
        }

        @JvmStatic fun isDefaultOrNull(@Nullable mode: OsuMode?): Boolean {
            return mode == null || mode == DEFAULT
        }

        @JvmStatic fun isNotDefaultOrNull(@Nullable mode: OsuMode?): Boolean {
            return isDefaultOrNull(mode).not()
        }

        /**
         * 修正无法转换的模式：只有可转换的谱面才能赋予模式
         */
        fun getConvertableMode(convert: OsuMode?, map: OsuMode?): OsuMode {
            return if (isDefaultOrNull(convert) || (map != null && map != OSU && map != DEFAULT)) {
                map ?: DEFAULT
            } else {
                convert ?: DEFAULT
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
