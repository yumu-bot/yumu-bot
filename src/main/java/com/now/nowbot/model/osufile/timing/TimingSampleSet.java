package com.now.nowbot.model.osufile.timing;

public enum TimingSampleSet {
    DEFAULT("default",0),
    NORMAL("normal",1),
    SOFT("soft",2),
    DRUM("drum",3);
    String type_name;
    int type_value;

    TimingSampleSet(String name, int value) {
        type_name = name;
        type_value = value;
    }

    public static TimingSampleSet getType(int value) {
        if ((value & 0b1) == 1) return NORMAL;
        else if ((value >> 1 & 0b1) == 1) return SOFT;
        else if ((value >> 2 & 0b1) == 1) return DRUM;
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
