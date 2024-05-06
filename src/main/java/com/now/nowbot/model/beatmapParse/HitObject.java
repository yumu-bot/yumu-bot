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

    public HitObject(String line) {
        // line 就是 '320,192,153921,1,0,0:0:0:0:' 这种格式的字符串
        var entity = line.split(",");
        if (entity.length < 3) throw new RuntimeException("解析 [HitObjects] 错误");
        int x = Integer.parseInt(entity[0]);
        int y = Integer.parseInt(entity[1]);
        int time = Integer.parseInt(entity[2]);

        position = new HitObjectPosition(x, y);
        startTime = time;
    }

    public HitObject(int x, int y, int time) {
        position = new HitObjectPosition(x, y);
        startTime = time;
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
