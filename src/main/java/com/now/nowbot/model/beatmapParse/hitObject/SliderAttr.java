package com.now.nowbot.model.beatmapParse.hitObject;

import java.util.List;

enum PointType {
    Catmull,
    Bezier,
    Linear,
    PerfectCircle,
    ;

    static PointType fromStr(String s) {
        if (s.isEmpty()) return fromChar('*');
        return fromChar(s.charAt(0));
    }

    static PointType fromChar(char s) {
        return switch (s) {
            case 'L' -> Linear;
            case 'B' -> Bezier;
            case 'P' -> PerfectCircle;
            default -> Catmull;
        };
    }


}

class SilderPoint extends Point {
    PointType type;
    public SilderPoint(int x, int y) {
        super(x, y);
        type = null;
    }
}

public class SliderAttr {
    // 像素长度
    float length;

    // 重复次数 (折返点)
    int repeats;

    // 控制点
    List<SilderPoint> controlPoints;

    List<Integer> sounds;
}