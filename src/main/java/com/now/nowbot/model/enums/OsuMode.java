package com.now.nowbot.model.enums;

public enum OsuMode {
    OSU("osu"),
    TAIKO("taiko"),
    CATCH("fruits"),
    MANIA("mania"),
    DEFAULT("");

    final String value;

    OsuMode(String mode) {
        value = mode;
    }
    
    public static OsuMode getMode(Number number) {
        if (number == null) {
            return DEFAULT;
        }

        var index = number.intValue();

        return switch (index) {
            case 0 -> OsuMode.OSU;
            case 1 -> OsuMode.TAIKO;
            case 2 -> OsuMode.CATCH;
            case 3 -> OsuMode.MANIA;
            default -> OsuMode.DEFAULT;
        };
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

    @Override
    public String toString() {
        return value;
    }
}
