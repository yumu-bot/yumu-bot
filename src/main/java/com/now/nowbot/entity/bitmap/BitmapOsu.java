package com.now.nowbot.entity.bitmap;

import java.util.List;

public class BitmapOsu {
    /***
     *
     */
    int version;
    /***
     * 物件数
     */
    int nCircles;
    int nSliders;
    int nSpinners;

    /***
     * 四维
     */
    double approachRate;
    double overallDifficulty;
    double circleSize;
    double hpDrainRate;
    /***
     * 滑条速度
     * 滑条点
     */
    double sliderMultiplier;
    double sliderTickRate;
    /***
     * 堆叠
     */
    Double StackLeniency = -1D;
    /***
     * 数组统计
     */
    List<HitObject> hitObjects;
}
