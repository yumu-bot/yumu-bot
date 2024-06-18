package com.now.nowbot.model.ppminus;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.ppminus.impl.PPMinusCatch;
import com.now.nowbot.model.ppminus.impl.PPMinusMania;
import com.now.nowbot.model.ppminus.impl.PPMinusOsu;
import com.now.nowbot.model.ppminus.impl.PPMinusTaiko;
import com.now.nowbot.model.enums.OsuMode;

import java.util.List;
import java.util.function.Function;

public abstract class PPMinus {
    protected float ppv0 = 0;
    protected float ppv45 = 0;
    protected float ppv90 = 0;
    protected float accv0 = 0;
    protected float pgr0 = 0;
    protected float pgr45 = 0;
    protected float pgr90 = 0;
    protected float accv45 = 0;
    protected float accv90 = 0;
    protected long lengv0 = 0;
    protected long lengv45 = 0;
    protected long lengv90 = 0;
    protected double bpPP = 0;
    protected double rawpp = 0;
    protected double bonus = 0;
    protected int xd = 0;
    protected int xc = 0;
    protected int xb = 0;
    protected int xa = 0;
    protected int xs = 0;
    protected int xx = 0;
    protected int notfc = 0;

    protected double value1;
    protected double value2;
    protected double value3;
    protected double value4;
    protected double value5;
    protected double value6;
    protected double value7;
    protected double value8;

    public static PPMinus getInstance(OsuMode mode, OsuUser user, List<Score> bps){
        PPMinus PPMinus = null;
        if (OsuMode.isDefaultOrNull(mode)) mode = user.getCurrentOsuMode();
        switch (mode) {
            case OSU -> PPMinus = new PPMinusOsu(user, bps);
            case TAIKO -> PPMinus = new PPMinusTaiko(user, bps);
            case CATCH -> PPMinus = new PPMinusCatch(user, bps);
            case MANIA -> PPMinus = new PPMinusMania(user, bps);
        }
        return PPMinus;
    }

    public double getValue1(){
        return value1;
    }
    public double getValue2(){
        return value2;
    }
    public double getValue3(){
        return value3;
    }
    public double getValue4(){
        return value4;
    }
    public double getValue5(){
        return value5;
    }
    public double getValue6(){
        return value6;
    }
    public double getValue7(){
        return value7;
    }
    public double getValue8(){
        return value8;
    }
    public double getValue1(Function<Double, Float> doAct){
        return doAct.apply(value1);
    }
    public double getValue2(Function<Double, Float> doAct){
        return doAct.apply(value2);
    }
    public double getValue3(Function<Double, Float> doAct){
        return doAct.apply(value3);
    }
    public double getValue4(Function<Double, Float> doAct){
        return doAct.apply(value4);
    }
    public double getValue5(Function<Double, Float> doAct){
        return doAct.apply(value5);
    }
    public double getValue6(Function<Double, Float> doAct){
        return doAct.apply(value6);
    }
    public double getValue7(Function<Double, Float> doAct){
        return doAct.apply(value7);
    }
    public double getValue8(Function<Double, Float> doAct){
        return doAct.apply(value8);
    }
    protected double check(double value, double min, double max){
        if (Double.isNaN(value)) return 0;
        if (value>max) return max;else return Math.max(value, min);
    }

    public float[] getValues(Function<Double, Float> doAct){
        float[] out = new float[6];
        out[0] = doAct.apply(value2);
        out[1] = doAct.apply(value3);
        out[2] = doAct.apply(value4);
        out[3] = doAct.apply(value5);
        out[4] = doAct.apply(value6);
        out[5] = doAct.apply(value1);
        return out;
    }
}
