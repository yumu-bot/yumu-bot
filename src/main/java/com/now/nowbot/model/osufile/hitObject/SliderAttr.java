package com.now.nowbot.model.osufile.hitObject;

import java.util.List;

public class SliderAttr {
    // 像素长度
    float length;
    // 重复次数 (折返点)
    int repeats;
    // 控制点
    List<SilderPoint> controlPoints;
    List<Integer> sounds;
}

class SilderPoint extends Point {
    PointType type;
    public SilderPoint(int x, int y) {
        super(x, y);
        type = null;
    }
}

enum PointType {
    Catmull(0),
    Bezier(1),
    Linear(2),
    PerfectCurve(3),
    ;
    final int type;

    PointType(int i) {
        type = i;
    }

    static PointType fromStr(String s) {
        if (s.length() < 1) return fromChar('*');
        return fromChar(s.charAt(0));
    }

    static PointType fromChar(char s) {
        return switch (s) {
            case 'L' -> Linear;
            case 'B' -> Bezier;
            case 'P' -> PerfectCurve;
            default -> Catmull;
        };
    }
}