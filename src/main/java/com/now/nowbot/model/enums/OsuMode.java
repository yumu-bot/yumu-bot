package com.now.nowbot.model.enums;

public enum OsuMode {
    OSU("osu"),
    TAIKO("taiko"),
    CATCH("fruits"),
    MANIA("mania"),
    DEFAULT("");

    String value;

    OsuMode(String mode) {
        value = mode;
    }

    public static OsuMode getMode(String desc){
        if (desc == null) return DEFAULT;
        switch (desc.toLowerCase()){
            case "osu":;
            case "o":;
            case "0":return OSU;

            case "taiko":;
            case "t":;
            case "1":return TAIKO;

            case "catch":;
            case "c":;
            case "fruits":;
            case "f":;
            case "2":return CATCH;

            case "mania":;
            case "m":;
            case "3":return MANIA;

            default:return DEFAULT;
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
