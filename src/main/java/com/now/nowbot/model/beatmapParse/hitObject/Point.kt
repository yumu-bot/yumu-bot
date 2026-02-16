package com.now.nowbot.model.beatmapParse.hitObject;

public class Point {
    int x;
    int y;
    public Point(){
        x = 0;
        y = 0;
    }
    public Point(int x, int y){
        this.x = x;
        this.y = y;
    }
    public double dotMultiply(Point other){
        return Math.fma(x, other.x, y * other.y);
    }
    public double distance(){
        return Math.sqrt(dotMultiply(this));
    }
    public Point normalize(){
        return this.divide(this.distance());
    }

    public Point plus(int x, int y){
        return new Point(this.x + x, this.y + y);
    }

    public Point minus(int x, int y){
        return new Point(this.x - x, this.y - y);
    }

    public Point multiply(double multiplier){
        return new Point((int) Math.round(x * multiplier), (int) Math.round(y * multiplier));
    }

    public Point divide(double divisor){
        if (divisor != 0) {
            return new Point((int) Math.round(x / divisor), (int) Math.round(y / divisor));
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
