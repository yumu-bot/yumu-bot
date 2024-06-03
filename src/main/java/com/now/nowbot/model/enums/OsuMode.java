package com.now.nowbot.model.enums;

import rosu.osu.Mode;

import java.util.Optional;

public enum OsuMode {
    OSU("osu",0),
    TAIKO("taiko",1),
    CATCH("fruits",2),
    MANIA("mania",3),
    DEFAULT("",-1);

    String name;
    short modeValue;

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

    public static OsuMode getMode(String desc){
        if (desc == null) return DEFAULT;
        return switch (desc.toLowerCase()) {
            case "osu", "o", "0" -> OSU;
            case "taiko", "t", "1" -> TAIKO;
            case "catch", "c", "fruits", "f", "2" -> CATCH;
            case "mania", "m", "3" -> MANIA;
            default -> DEFAULT;
        };
    }
    public static OsuMode getMode(int desc){
        return switch (desc) {
            case 0 -> OSU;
            case 1 -> TAIKO;
            case 2 -> CATCH;
            case 3 -> MANIA;
            default -> DEFAULT;
        };
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public short getModeValue() {
        return modeValue;
    }

    public void setModeValue(short modeValue) {
        this.modeValue = modeValue;
    }

    public Mode toRosuMode() {
        return switch (this) {
            case OSU -> Mode.Osu;
            case TAIKO -> Mode.Taiko;
            case CATCH -> Mode.Catch;
            case MANIA -> Mode.Mania;
            default -> Mode.Default;
        };
    }

    public static boolean isDefaultOrNull(OsuMode mode) {
        return mode == null || mode == DEFAULT;
    }


    public static Optional<String> getName(OsuMode mode) {
        if (DEFAULT.equals(mode)) {
            return Optional.empty();
        }
        return Optional.of(mode.getName());
    }
}
