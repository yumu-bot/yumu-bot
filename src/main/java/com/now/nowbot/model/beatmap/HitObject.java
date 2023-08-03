package com.now.nowbot.model.beatmap;

public class HitObject {
    HitObjectPosition position;
    HitObjectType type;
    double start_time;
    double end_time;

    public HitObjectPosition getPosition() {
        return position;
    }
    public void setPosition(HitObjectPosition position) {
        this.position = position;
    }
    public HitObjectType getType() {
        return type;
    }
    public void setType(HitObjectType type) {
        this.type = type;
    }
}
