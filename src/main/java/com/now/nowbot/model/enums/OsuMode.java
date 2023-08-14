package com.now.nowbot.model.enums;

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
        switch (desc.toLowerCase()){
            case "osu":
            case "o":
            case "0":return OSU;

            case "taiko":
            case "t":
            case "1":return TAIKO;

            case "catch":
            case "c":
            case "fruits":
            case "f":
            case "2":return CATCH;

            case "mania":
            case "m":
            case "3":return MANIA;

            default:return DEFAULT;
        }
    }
    public static OsuMode getMode(int desc){
        switch (desc) {
            case 0 : return OSU;
            case 1 : return TAIKO;
            case 2 : return CATCH;
            case 3 : return MANIA;
            default : return DEFAULT;
        }
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
    public static boolean isDefault (OsuMode mode) {
        return mode == null || mode == DEFAULT;
    }
}
