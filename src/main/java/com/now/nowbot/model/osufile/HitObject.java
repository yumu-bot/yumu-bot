package com.now.nowbot.model.osufile;

import com.now.nowbot.model.osufile.hitObject.HitObjectPosition;
import com.now.nowbot.model.osufile.hitObject.HitObjectSound;
import com.now.nowbot.model.osufile.hitObject.HitObjectType;
import com.now.nowbot.model.osufile.hitObject.SliderAttr;

public class HitObject {
    HitObjectPosition position;
    HitObjectType type;
    HitObjectSound sound;
    int start_time;
    int end_time;

    int column;

    /**
     * 仅 type == Slider 时存在
     */
    SliderAttr sliderAttr;



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
        return start_time;
    }
    public void setStartTime(int time) {
        this.start_time = time;
    }
    public int getEndTime() {
        return end_time;
    }
    public void setEndTime(int time) {
        this.end_time = time;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }
}
