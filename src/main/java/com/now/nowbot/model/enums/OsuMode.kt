package com.now.nowbot.model.enums

import org.springframework.lang.Nullable
import java.util.*

enum class OsuMode(@JvmField val fullName: String, @JvmField val shortName: String, @JvmField val modeValue: Short) {
    OSU("osu!standard", "osu", 0),
    TAIKO("osu!taiko", "taiko", 1),
    CATCH("osu!catch", "fruits", 2),
    MANIA("osu!mania", "mania", 3),
    DEFAULT("default", "", -1);

    override fun toString(): String {
        return fullName
    }

    fun toRosuMode(): org.spring.osu.OsuMode {
        return when (this) {
            OSU -> org.spring.osu.OsuMode.Osu
            TAIKO -> org.spring.osu.OsuMode.Taiko
            CATCH -> org.spring.osu.OsuMode.Catch
            MANIA -> org.spring.osu.OsuMode.Mania
            else -> org.spring.osu.OsuMode.Default
        }
    }

    companion object {
        /**
         * 当 mode 为 Default 或者 mode == mode2 时，返回 true
         */
        @JvmStatic fun equal(mode: OsuMode, mode2: Any?) : Boolean {
            if (mode2 is OsuMode) {
                return mode == mode2 || mode == DEFAULT
            }

            return false
        }

        @JvmStatic fun getMode(str: String?, default: String?): OsuMode {
            val mode = getMode(default)
            if (DEFAULT != mode) return getMode(str, mode)
            return getMode(str)
        }

        @JvmStatic fun getMode(str: String?, default: OsuMode?): OsuMode {
            val mode = getMode(str)
            if (DEFAULT == mode) return default ?: DEFAULT
            return mode
        }

        @JvmStatic fun getMode(mode: OsuMode?, default: OsuMode?): OsuMode {
            if (isDefaultOrNull(mode)) return default ?: DEFAULT
            return mode!!
        }

        @JvmStatic fun getMode(@Nullable name: String?): OsuMode {
            return when (name?.lowercase()) {
                "osu", "o", "0" -> OSU
                "taiko", "t", "1" -> TAIKO
                "catch", "c", "fruits", "f", "2" -> CATCH
                "mania", "m", "3" -> MANIA
                else -> DEFAULT
            }
        }

        @JvmStatic fun getMode(@Nullable int: Int?): OsuMode {
            return when (int) {
                0 -> OSU
                1 -> TAIKO
                2 -> CATCH
                3 -> MANIA
                else -> DEFAULT
            }
        }

        @JvmStatic fun isDefaultOrNull(@Nullable mode: OsuMode?): Boolean {
            return mode == null || mode == DEFAULT
        }

        @JvmStatic fun isNotDefaultOrNull(@Nullable mode: OsuMode?): Boolean {
            return !isDefaultOrNull(mode)
        }

        // 修正无法转换的模式
        @JvmStatic fun correctConvert(@Nullable convert: OsuMode?, @Nullable map: OsuMode?): OsuMode {
            return if (map != OSU && map != null && map != DEFAULT) {
                map
            } else {
                convert ?: DEFAULT
            }
        }

        @JvmStatic fun getQueryName(@Nullable mode: OsuMode?): Optional<String> {
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
