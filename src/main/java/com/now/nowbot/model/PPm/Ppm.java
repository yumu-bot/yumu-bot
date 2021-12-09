package com.now.nowbot.model.PPm;

import com.now.nowbot.model.JsonData.BpInfo;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.PPm.impl.PpmCatch;
import com.now.nowbot.model.PPm.impl.PpmMania;
import com.now.nowbot.model.PPm.impl.PpmOsu;
import com.now.nowbot.model.PPm.impl.PpmTaiko;
import com.now.nowbot.model.enums.OsuMode;

import java.util.List;
import java.util.function.Function;

public abstract class Ppm {
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
    protected double bpp = 0;
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

    public static Ppm getInstance(OsuMode mode, OsuUser user, List<BpInfo> bps){
        return switch (mode){
            case OSU -> new PpmOsu(user, bps);
            case TAIKO -> new PpmTaiko(user, bps);
            case CATCH -> new PpmCatch(user, bps);
            case MANIA -> new PpmMania(user, bps);
            case default -> null;
        };
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
        getValues((v)-> (float)Math.pow((v < 0.6 ? 0 : v - 0.6) * 2.5f, 0.8));
        return value8;
    }
    protected double check(double value, double min, double max){
        if (value>max) return max;else return Math.max(value, min);
    }

    public float[] getValues(Function<Double, Float> doAct){
        float[] out = new float[6];
        out[0] = doAct.apply(value1);
        out[1] = doAct.apply(value2);
        out[2] = doAct.apply(value3);
        out[3] = doAct.apply(value4);
        out[4] = doAct.apply(value5);
        out[5] = doAct.apply(value6);
        return out;
    }
    /**
     * 计算bonusPP
     * 算法类似于通过 正态分布 "估算"超过bp100的pp数 此方法不严谨
     */
    public float bonusPP(double[] pp, Long pc){
        double  bonus = 0;
        double sumOxy = 0;
        double sumOx2 = 0;
        double avgX = 0;
        double avgY = 0;
        double sumX = 0;
        for (int i = 1; i <= pp.length; i++) {
            double weight = Math.log1p(i + 1);
            sumX += weight;
            avgX += i * weight;
            avgY += pp[i - 1] * weight;
        }
        avgX /= sumX;
        avgY /= sumX;
        for(int n = 1; n <= pp.length; n++){
            sumOxy += (n - avgX) * (pp[n - 1] - avgY) * Math.log1p(n + 1.0D);
            sumOx2 += Math.pow(n - avgX, 2.0D) * Math.log1p(n + 1.0D);
        }
        double Oxy = sumOxy / sumX;
        double Ox2 = sumOx2 / sumX;
        for(int n = 100; n <= pc; n++){
            double val = Math.pow(100.0D, (avgY - (Oxy / Ox2) * avgX) + (Oxy / Ox2) * n);
            if(val <= 0.0D){
                break;
            }
            bonus += val;
        }
        return (float) bonus;
    }
}
