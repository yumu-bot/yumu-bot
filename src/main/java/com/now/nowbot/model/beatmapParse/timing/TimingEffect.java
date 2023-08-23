package com.now.nowbot.model.beatmapParse.timing;

public enum TimingEffect {
    DEFAULT("default",0),
    KIAI("normal",1),
    OMITFIRSTBAR("soft",8);
    String type_name;
    int type_value;

    TimingEffect(String name, int value) {
        type_name = name;
        type_value = value;
    }

    public static TimingEffect getType(int value) {
        if ((value & 0b1) == 1) return KIAI;
        else if ((value >> 2 & 0b1) == 1) return OMITFIRSTBAR;
        else return DEFAULT;
    }

    @Override
    public String toString() {
        return type_name;
    }

    public String getName() {
        return type_name;
    }

    public void setName(String name) {
        this.type_name = name;
    }

    public int getValue() {
        return type_value;
    }

    public void setValue(int value) {
        this.type_value = value;
    }
}
