package com.now.nowbot.entity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class PPmObject {
    float ppv0=0;
    float ppv45=0;
    float ppv90=0;
    float accv0=0;
    float accv45=0;
    float accv90=0;
    long lengv0=0;
    long lengv45=0;
    long lengv90=0;
    double bpp=0;
    double rawpp = 0;
    double bonus = 0;
    int xd=0;
    int xc=0;
    int xb=0;
    int xa=0;
    int xs=0;
    int xx=0;
    int notfc=0;
    String name;
    float pp ;
    float acc;
    int level;
    int rank ;
    int combo;
    long thit;
    long pcont;
    long ptime;

    double fa;
    double eng;
    double ptt;
    double sth;
    double stb;
    double sta;
    private PPmObject(){}
    private PPmObject creatOsu(JSONObject prd, JSONArray prbp){
        double[] ys = new double[prbp.size()];
        for (int j = 0; j < prbp.size(); j++) {
            var jsb = prbp.getJSONObject(j);
            bpp += jsb.getDoubleValue("pp")*Math.pow(0.95d,j);
            ys[j] = Math.log10(jsb.getDoubleValue("pp") * Math.pow(0.95, j)) / Math.log10(100);

            if (jsb.getString("rank").startsWith("D")) xd++;
            if (jsb.getString("rank").startsWith("C")) xc++;
            if (jsb.getString("rank").startsWith("B")) xb++;
            if (jsb.getString("rank").startsWith("A")) xa++;
            if (jsb.getString("rank").startsWith("S")) xs++;
            if (jsb.getString("rank").startsWith("X")) xx++;
            if(!jsb.getBoolean("perfect")) notfc++;
            if(j < 10){
                ppv0 += jsb.getFloatValue("pp");
                accv0 += jsb.getFloatValue("accuracy");
                lengv0 += jsb.getJSONObject("beatmap").getFloatValue("total_length");
            }else if(j>=45 && j<55){
                ppv45 += jsb.getFloatValue("pp");
                accv45 += jsb.getFloatValue("accuracy");
                lengv45 += jsb.getJSONObject("beatmap").getFloatValue("total_length");
            }else if(j>=90){
                ppv90 += jsb.getFloatValue("pp");
                accv90 += jsb.getFloatValue("accuracy");
                lengv90 += jsb.getJSONObject("beatmap").getFloatValue("total_length");
            }
        }
        double sumOxy = 0.0D;
        double sumOx2 = 0.0D;
        double avgX = 0.0D;
        double avgY = 0.0D;
        double sumX = 0.0D;
        for(int n = 1; n <= ys.length; n++){
            double weight = Math.log1p(n + 1.0D);
            sumX += weight;
            avgX += n * weight;
            avgY += ys[n - 1] * weight;
        }
        avgX /= sumX;
        avgY /= sumX;
        for(int n = 1; n <= ys.length; n++){
            sumOxy += (n - avgX) * (ys[n - 1] - avgY) * Math.log1p(n + 1.0D);
            sumOx2 += Math.pow(n - avgX, 2.0D) * Math.log1p(n + 1.0D);
        }
        double Oxy = sumOxy / sumX;
        double Ox2 = sumOx2 / sumX;
        for(double n = 100; n <= prd.getJSONObject("statistics").getIntValue("play_count"); n++){
            double val = Math.pow(100.0D, (avgY - (Oxy / Ox2) * avgX) + (Oxy / Ox2) * n);
            if(val <= 0.0D){
                break;
            }
            bonus += val;
        }
        rawpp = bpp+ bonus;

        ppv0 /= 10;
        ppv45 /= 10;
        ppv90 /= 10;
        accv0 /= 10;
        accv45 /= 10;
        accv90 /= 10;
        lengv0 /= 10;
        lengv45 /= 10;
        lengv90 /= 10;
        if (prbp.size()<90) {
            ppv90 = 0; accv90 = 0; lengv90 = 0;
        }
        if (prbp.size()<45) {
            ppv45 = 0; accv45 = 0; lengv45 = 0;
        }
        if (prbp.size()<10) {
            ppv0 = 0; accv0 = 0; lengv0 = 0;
        }
        name = prd.getString("username");
        pp = prd.getJSONObject("statistics").getFloatValue("pp");
        if (pp > rawpp) {
            bonus = pp - rawpp;
        }else {
            bonus = 0;
        }
        acc = prd.getJSONObject("statistics").getFloatValue("hit_accuracy");
        level = prd.getJSONObject("statistics").getJSONObject("level").getIntValue("current");
        rank = prd.getJSONObject("statistics").getIntValue("global_rank");
        combo = prd.getJSONObject("statistics").getIntValue("maximum_combo");
        thit = prd.getJSONObject("statistics").getLongValue("total_hits");
        pcont = prd.getJSONObject("statistics").getLongValue("play_count");
        ptime = prd.getJSONObject("statistics").getLongValue("play_time");

        //1.1 准度fACC formulaic accuracy 0-1 fa
        fa = ((this.acc/100)<0.6D?0:Math.pow((this.acc/100-0.6)*2.5D,1.776D));
        fa = setm(fa, 0, 1);
        //1.2 1.2 潜力PTT potential 0-1 ptt

        {
            double bpmxd = Math.pow(0.9D, this.ppv45 / (this.ppv0 - this.ppv90 + 1));
            double rBPD = this.rawpp / this.ppv0;
            double BPD;
            if (rBPD <= 14) {
                BPD = 1;
            } else if (rBPD <= 18) {
                BPD = (18 - rBPD) * 0.1D + 0.6D;
            } else if (rBPD <= 19) {
                BPD = (19 - rBPD) * 0.6D;
            } else {
                BPD = 0;
            }
            ptt = Math.pow((BPD*0.2 + bpmxd*0.4 + 0.4),0.8D);
            ptt = setm(ptt, 0, 1);
            if (prbp.size() < 100) ptt = 1;
        }
        //1.3 耐力STA stamina 0-1.2 sta

        {
            double rSP = 1.0*this.ptime/this.pcont;
            double SPT;
            if(rSP<30){
                SPT = 0;
            }else if(rSP<=180){
                SPT = 1 - Math.pow((180-rSP)/150, 2.357);
            }else{
                SPT = 1;
            }
            double rLN = this.lengv0*0.7 + this.lengv45*0.2 + this.lengv90*0.1;
            double fLEN;
            if(rLN<30){
                fLEN = 0;
            }else if(rLN<=180){
                fLEN = 1 - Math.pow((180-rLN)/150, 2.357);
            }else{
                fLEN = 1;
            }
            double VLB;
            if(rLN<180){
                VLB = 0;
            }else if(rLN<=240){
                VLB = Math.pow((rLN-180)/60,0.4);
            }else{
                VLB = 1;
            }
            sta = Math.pow((SPT*0.4 + fLEN*0.6),0.8D) + VLB * 0.2;
            sta = setm(sta, 0, 1.2);
        }
        //1.4 稳定STB stability (-0.16)-1.2 stb

        {
            double GRD = (this.xx + this.xs*0.9 + this.xa* 0.8 + this.xb*0.4 + this.xc*0.2 - this.xd*0.2)/100;
            double FCN = (100-this.notfc)/100D;
            double PFN = (this.xs+ this.xx)/100D;
            stb = GRD*0.8+(FCN+PFN)*0.2;
            stb = setm(stb, 0, 1.2);
        }
        //1.5 肝力ENG energy eng

        {
            eng = this.bonus /416.6667;
            if (eng>1)eng =1;
            eng = Math.pow(eng, 0.4D);
            eng = setm(eng, 0, 1);
        }
        //1.6 实力STH strength sth

        {
            double HPS = 1D*this.thit/this.ptime;
            if(HPS>4.5) HPS =  4.5;
            else if(HPS<2.5) HPS =  2.5;
            sth = Math.pow((HPS-2.5)/2,0.2);
            sth = setm(sth, 0, 1);
        }
        return this;
    }
    double setm(double value, double min, double max){
        if (value>max) return max;else return Math.max(value, min);
    }
    public static PPmObject presOsu(JSONObject prd, JSONArray prbp){
        return new PPmObject().creatOsu(prd, prbp);
    }

    public float getPpv0() {
        return ppv0;
    }

    public float getPpv45() {
        return ppv45;
    }

    public float getPpv90() {
        return ppv90;
    }

    public float getAccv0() {
        return accv0;
    }

    public float getAccv45() {
        return accv45;
    }

    public float getAccv90() {
        return accv90;
    }

    public long getLengv0() {
        return lengv0;
    }

    public long getLengv45() {
        return lengv45;
    }

    public long getLengv90() {
        return lengv90;
    }

    public double getBpp() {
        return bpp;
    }

    public double getRawpp() {
        return rawpp;
    }

    public double getBonus() {
        return bonus;
    }

    public int getXd() {
        return xd;
    }

    public int getXc() {
        return xc;
    }

    public int getXb() {
        return xb;
    }

    public int getXa() {
        return xa;
    }

    public int getXs() {
        return xs;
    }

    public int getXx() {
        return xx;
    }

    public int getNotfc() {
        return notfc;
    }

    public String getName() {
        return name;
    }

    public float getPp() {
        return pp;
    }

    public float getAcc() {
        return acc;
    }

    public int getLevel() {
        return level;
    }

    public int getRank() {
        return rank;
    }

    public int getCombo() {
        return combo;
    }

    public long getThit() {
        return thit;
    }

    public long getPcont() {
        return pcont;
    }

    public long getPtime() {
        return ptime;
    }

    public double getFa() {
        return fa;
    }

    public double getEng() {
        return eng;
    }

    public double getSth() {
        return sth;
    }

    public double getStb() {
        return stb;
    }

    public double getSta() {
        return sta;
    }

    public double getPtt() {
        return ptt;
    }
}
