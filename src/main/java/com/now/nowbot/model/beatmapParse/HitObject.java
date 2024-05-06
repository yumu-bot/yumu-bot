package com.now.nowbot.model.beatmapParse;

import com.now.nowbot.model.beatmapParse.hitObject.HitObjectPosition;
import com.now.nowbot.model.beatmapParse.hitObject.HitObjectSound;
import com.now.nowbot.model.beatmapParse.hitObject.HitObjectType;
import com.now.nowbot.model.beatmapParse.hitObject.SliderAttr;

public class HitObject {
    HitObjectPosition position;
    HitObjectType  type;
    HitObjectSound sound;
    Integer        startTime;
    Integer        endTime;

    Integer column;

    /**
     * 仅 type == Slider 时存在
     */
    SliderAttr sliderAttr;

    public HitObject() {
    }

    public HitObject(int x, int y, HitObjectType type, int time, int end) {
        position = new HitObjectPosition(x, y);
        startTime = time;
        endTime = end;
    }

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

    public void setSound(HitObjectSound sound) {
        this.sound = sound;
    }

    public HitObjectSound getSound() {
        return sound;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int time) {
        this.startTime = time;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int time) {
        this.endTime = time;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }
}
