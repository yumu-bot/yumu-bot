package com.now.nowbot.model.osufile;

public class HitObjectPosition extends HitObject{
    int x;
    int y;
    public HitObjectPosition(){
        x = 0;
        y = 0;
    }
    public HitObjectPosition(int x, int y){
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

    public HitObjectPosition plus(int x, int y){
        return new HitObjectPosition(this.x + x, this.y + y);
    }

    public HitObjectPosition minus(int x, int y){
        return new HitObjectPosition(this.x - x, this.y - y);
    }

    public HitObjectPosition multiply(double multiplier){
        return new HitObjectPosition((int) Math.round(x * multiplier), (int) Math.round(y * multiplier));
    }

    public HitObjectPosition divide(double divisor){
        if (divisor != 0) {
            return new HitObjectPosition((int) Math.round(x / divisor), (int) Math.round(y / divisor));
        }
        else {
            throw new RuntimeException("除数为0");
        }
    }


    public double getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }


}
