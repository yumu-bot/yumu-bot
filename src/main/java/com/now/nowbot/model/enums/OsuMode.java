package com.now.nowbot.model.enums;

import org.springframework.lang.Nullable;

import java.util.Optional;

public enum OsuMode {
    OSU("osu",0),
    TAIKO("taiko",1),
    CATCH("fruits",2),
    MANIA("mania",3),
    DEFAULT("",-1);

    final String name;
    final short modeValue;

    OsuMode(String mode, int i) {
        name = mode;
        modeValue = (short) i;
    }

    public static OsuMode getMode(String mode, String defaultMode) {
        var m = getMode(defaultMode);
        if (!DEFAULT.equals(m)) return getMode(mode, m);
        return getMode(mode);
    }
    public static OsuMode getMode(String mode, OsuMode defaultMode) {
        var m = getMode(mode);
        if (DEFAULT.equals(m)) return defaultMode;
        return m;
    }
    public static OsuMode getMode(OsuMode m, OsuMode defaultMode) {
        if (DEFAULT.equals(m)) return defaultMode;
        return m;
    }

    public static OsuMode getMode(@Nullable String desc){
        if (desc == null) return DEFAULT;
        return switch (desc.toLowerCase()) {
            case "osu", "o", "0" -> OSU;
            case "taiko", "t", "1" -> TAIKO;
            case "catch", "c", "fruits", "f", "2" -> CATCH;
            case "mania", "m", "3" -> MANIA;
            default -> DEFAULT;
        };
    }

    public static OsuMode getMode(@Nullable Integer desc){
        return switch (desc) {
            case 0 -> OSU;
            case 1 -> TAIKO;
            case 2 -> CATCH;
            case 3 -> MANIA;
            case null, default -> DEFAULT;
        };
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public short getModeValue() {
        return modeValue;
    }

    public org.spring.osu.OsuMode toRosuMode() {
        return switch (this) {
            case OSU -> org.spring.osu.OsuMode.Osu;
            case TAIKO -> org.spring.osu.OsuMode.Taiko;
            case CATCH -> org.spring.osu.OsuMode.Catch;
            case MANIA -> org.spring.osu.OsuMode.Mania;
            default -> org.spring.osu.OsuMode.Default;
        };
    }

    public static boolean isDefaultOrNull(OsuMode mode) {
        return mode == null || mode == DEFAULT;
    }

    public static boolean isNotDefaultOrNull(OsuMode mode) {
        return ! isDefaultOrNull(mode);
    }


    public static Optional<String> getName(OsuMode mode) {
        if (DEFAULT.equals(mode)) {
            return Optional.empty();
        }
        return Optional.of(mode.getName());
    }
}
