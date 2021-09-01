package com.now.nowbot.entity.bitmap;

public class HitObject {
    public static class HitObjectPosition{
        double x;
        double y;
        public HitObjectPosition(double x,double y){
            this.x = x;
            this.y = y;
        }
        public double length(){
            return Math.sqrt(Math.fma(x,x,y*y));
        }
    }
    HitObjectPosition pos;
    double startTime;

    int sound;
}
