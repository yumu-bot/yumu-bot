package com.now.nowbot.model.beatmap;

public class HitObjectPosition extends HitObject{
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
    public double dotMultiply(HitObjectPosition other){
        return Math.fma(x, other.x, y * other.y);
    }
    public double distance(){
        return Math.sqrt(dotMultiply(this));
    }
    public HitObjectPosition normalize(){
        return this.divide(this.distance());
    }

    public HitObjectPosition plus(double x, double y){
        return new HitObjectPosition(this.x + x, this.y + y);
    }

    public HitObjectPosition minus(double x, double y){
        return new HitObjectPosition(this.x - x, this.y - y);
    }

    public HitObjectPosition multiply(double multiplier){
        return new HitObjectPosition(x * multiplier, y * multiplier);
    }

    public HitObjectPosition divide(double divisor){
        if (divisor != 0) {
            return new HitObjectPosition(x / divisor, y / divisor);
        }
        else {
            throw new RuntimeException("除数为0");
        }
    }
    public int getKeyColumn (double x, int key) {
        int column = (int) Math.floor(x * key / 512f);
        column = Math.max(Math.min(0, column), key);
        return column;
    }
}
