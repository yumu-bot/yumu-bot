package com.now.nowbot.model.osufile.hitObject;

public enum HitObjectType{
    DEFAULT("default",0),
    CIRCLE("circle",1),
    SLIDER("slider",2),
    SPINNER("spinner",8),
    LONGNOTE("longnote",128);
    String type_name;
    int type_value;

    HitObjectType(String name, int value) {
        type_name = name;
        type_value = value;
    }

    public static HitObjectType getType(int value) {
        if ((value & 0b1) == 1) return CIRCLE;
        else if ((value >> 1 & 0b1) == 1) return SLIDER;
        else if ((value >> 3 & 0b1) == 1) return SPINNER;
        else if ((value >> 7 & 0b1) == 1) return LONGNOTE;
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