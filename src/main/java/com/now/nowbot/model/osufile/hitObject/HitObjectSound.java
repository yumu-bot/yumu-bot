package com.now.nowbot.model.osufile.hitObject;

import javax.annotation.Nullable;

public enum HitObjectSound {
    NORMAL("normal",0),
    WHISTLE("whistle",2),
    FINISH("finish",4),
    CLAP("clap",8),
    DEFAULT("normal",null);

    String sound_name;
    Integer sound_value;
    HitObjectSound(String name, @Nullable Integer value) {
        sound_name = name;
        sound_value = value;
    }

    public static HitObjectSound getSound(int value) {
        if (value == 0) return NORMAL;
        if ((value & 0b1) == 1) return WHISTLE;
        else if ((value >> 1 & 0b1) == 1) return FINISH;
        else if ((value >> 2 & 0b1) == 1) return CLAP;
        else return DEFAULT;
    }

    @Override
    public String toString() {
        return sound_name;
    }

    public String getName() {
        return sound_name;
    }

    public void setName(String name) {
        this.sound_name = name;
    }

    public int getValue() {
        return sound_value;
    }

    public void setValue(int value) {
        this.sound_value = value;
    }
}
