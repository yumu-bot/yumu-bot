package com.now.nowbot.model.PPm;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public interface PPmObject {
    static PPmOsu presOsu(JSONObject prd, JSONArray prbp){
        return new PPmOsu(prd, prbp);
    }
    static PPmTaiko presTaiko(JSONObject prd, JSONArray prbp){
        return new PPmTaiko(prd, prbp);
    }
    static PPmCatch presCatch(JSONObject prd, JSONArray prbp){
    return new PPmCatch(prd, prbp);
    }
    double check(double value, double min, double max);

    float getPpv0();

    float getPpv45();

    float getPpv90();

    float getAccv0();

    float getAccv45();

    float getAccv90();

    long getLengv0();

    long getLengv45();

    long getLengv90();

    double getBpp();

    double getRawpp();

    double getBonus();

    int getXd();

    int getXc();

    int getXb();

    int getXa();

    int getXs();

    int getXx();

    int getNotfc();

    String getName();

    float getPp();

    float getAcc();

    int getLevel();

    int getRank();

    int getCombo();

    long getThit();

    long getPcont();

    long getPtime();

    double getFacc();

    double getEng();

    double getSth();

    double getStb();

    double getSta();

    double getPtt();

    double getTtl();

    double getSan();

    String getHeadURL();
}
