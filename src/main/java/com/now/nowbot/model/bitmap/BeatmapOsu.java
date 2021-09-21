package com.now.nowbot.model.bitmap;

import java.util.List;

public class BeatmapOsu {
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


    static class HitObject{
        HitObjectPosition pos;
        double startTime;
        HitObjectType type;
        int sound;

        class HitObjectPosition{
            double x;
            double y;
            public HitObjectPosition(){
                x = 0;
                y = 0;
            }
            public HitObjectPosition(double x,double y){
                this.x = x;
                this.y = y;
            }
            public double lengthSquared(){
                return dotMultiply(this);
            }
            public double dotMultiply(HitObjectPosition other){
                return Math.fma(x, other.x, y * other.y);
            }
            public double length(){
                return Math.sqrt(lengthSquared());
            }
            public HitObjectPosition normalize(){
                return this.div(this.length());
            }

            public HitObjectPosition plus(HitObjectPosition other){
                return new HitObjectPosition(x+other.x, y+other.y);
            }

            public HitObjectPosition minus(HitObjectPosition other){
                return new HitObjectPosition(x-other.x, y-other.y);
            }

            public HitObjectPosition times(double div){
                return new HitObjectPosition(x*div, y*div);
            }

            public HitObjectPosition div(double div){
                if (div != 0) {
                    return new HitObjectPosition(x / div, y / div);
                }
                else {
                    throw new RuntimeException("除数为0");
                }
            }
        }

        class HitObjectType{

        }
    }
}
