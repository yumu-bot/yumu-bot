package com.now.nowbot.model.PPm;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.model.JsonData.BpInfo;
import com.now.nowbot.model.JsonData.OsuUser;

import java.util.List;

public class PPmMania implements PPmObject {
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
    double pre;
    double sta;
    double ttl;
    double san;
    String headURL;
    String bgURL;

    float pgr0;
    float pgr45;
    float pgr90;
    /***
     * 计算mania ppm
     * @param prd 个人信息 JSON 替换为
     * @param prbp bp列表
     */
    public PPmMania(JSONObject prd, JSONArray prbp) {
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
                pgr0 += jsb.getJSONObject("statistics").getFloatValue("count_geki")/jsb.getJSONObject("statistics").getFloatValue("count_300");
            }else if(j>=45 && j<55){
                ppv45 += jsb.getFloatValue("pp");
                accv45 += jsb.getFloatValue("accuracy");
                lengv45 += jsb.getJSONObject("beatmap").getFloatValue("total_length");
                pgr45 += jsb.getJSONObject("statistics").getFloatValue("count_geki")/jsb.getJSONObject("statistics").getFloatValue("count_300");
            }else if(j>=90){
                ppv90 += jsb.getFloatValue("pp");
                accv90 += jsb.getFloatValue("accuracy");
                lengv90 += jsb.getJSONObject("beatmap").getFloatValue("total_length");
                pgr90 += jsb.getJSONObject("statistics").getFloatValue("count_geki")/jsb.getJSONObject("statistics").getFloatValue("count_300");
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
        rawpp = bpp + bonus;

        ppv0 /= 10;
        ppv45 /= 10;
        ppv90 /= 10;
        accv0 /= 10;
        accv45 /= 10;
        accv90 /= 10;
        lengv0 /= 10;
        lengv45 /= 10;
        lengv90 /= 10;
        pgr0 /= 10;
        pgr45 /= 10;
        pgr90 /= 10;
        if (prbp.size()<90) {
            ppv90 = 0; accv90 = 0; lengv90 = 0; pgr0 = 0;
        }
        if (prbp.size()<45) {
            ppv45 = 0; accv45 = 0; lengv45 = 0; pgr45 = 0;
        }
        if (prbp.size()<10) {
            ppv0 = 0; accv0 = 0; lengv0 = 0; pgr90 = 0;
        }
        name = prd.getString("username");
        pp = prd.getJSONObject("statistics").getFloatValue("pp");
        if (pp > rawpp) {
            bonus = pp - rawpp;
        }else {
            bonus = 0;
        }
        headURL = prd.getString("avatar_url");
        bgURL = prd.getString("cover_url");
        acc = prd.getJSONObject("statistics").getFloatValue("hit_accuracy");
        level = prd.getJSONObject("statistics").getJSONObject("level").getIntValue("current");
        rank = prd.getJSONObject("statistics").getIntValue("global_rank");
        combo = prd.getJSONObject("statistics").getIntValue("maximum_combo");
        thit = prd.getJSONObject("statistics").getLongValue("total_hits");
        pcont = prd.getJSONObject("statistics").getLongValue("play_count");
        ptime = prd.getJSONObject("statistics").getLongValue("play_time");

        //1.1 准度fACC formulaic accuracy 0-1 facc
        {
            fa = ((this.acc / 100) < 0.6D ? 0 : Math.pow((this.acc / 100 - 0.6) * 2.5D, 1.776D));
            fa = check(fa, 0, 1);
        }
        //1.2 1.2 潜力PTT potential 0-1 ptt

        {
            double bpmxd = Math.pow(0.9D, this.ppv45 / (this.ppv0 - this.ppv90 + 1));
            double rBPD = this.ppv0 == 0?0:(this.rawpp / this.ppv0);
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
            ptt = check(ptt, 0, 1);
            if (prbp.size() < 100) ptt = 1;
        }
        //1.3 耐力STA stamina 0-1.2 sta

        {
            double rSP = this.pcont == 0?0:(1.0*this.ptime/this.pcont);
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
            sta = check(sta, 0, 1.2);
        }
        //1.4 稳定STB stability (-0.16)-1.2 stb

        {
            double GRD = (this.xx + this.xs*0.9 + this.xa* 0.8 + this.xb*0.4 + this.xc*0.2 - this.xd*0.2)/100;
            double FCN = (100-this.notfc)/100D;
            double PFN = (this.xs+ this.xx)/100D;
            stb = GRD*0.8+(FCN+PFN)*0.2;
            stb = check(stb, 0, 1.2);
        }
        //1.5 肝力ENG energy eng

        {
            eng = this.bonus /416.6667;
            if (eng>1)eng =1;
            eng = Math.pow(eng, 0.4D);
            eng = check(eng, 0, 1);
        }
        //1.6 实力STH strength sth

        {
            double HPS = 1D*this.thit/this.ptime;
            if(HPS>4.5) HPS =  4.5;
            else if(HPS<2.5) HPS =  2.5;
            sth = Math.pow((HPS-2.5)/2,0.2);
            sth = check(sth, 0, 1);
        }
        ttl = fa*0.2 + eng*0.2 + ptt*0.1 + sth*0.2 + stb*0.15 + sta*0.15;
        san = ppv0<20?0:(ppv0/(ppv45+ppv90*0.2+1)*(ptt+0.25)*(sth+0.25));
        //san = rsan
        if (san < 1){
            san = 120 - 20*san;
        }else if (san < 5){
            san = 110 - 10*san;
        }else {
            san = 300/san;
        }
    }

    PPmMania(OsuUser user, List<BpInfo> bps){
        double[] ys = new double[bps.size()];
        for (int j = 0; j < bps.size(); j++) {
            var jsb = bps.get(j);
            bpp += jsb.getPp() * Math.pow(0.95d, j);
            ys[j] = Math.log10(jsb.getPp() * Math.pow(0.95, j)) / Math.log10(100);

            if (jsb.getRank().startsWith("D")) xd++;
            if (jsb.getRank().startsWith("C")) xc++;
            if (jsb.getRank().startsWith("B")) xb++;
            if (jsb.getRank().startsWith("A")) xa++;
            if (jsb.getRank().startsWith("S")) xs++;
            if (jsb.getRank().startsWith("X")) xx++;
            if (!jsb.isPerfect()) notfc++;
            if (j < 10) {
                ppv0 += jsb.getPp();
                accv0 += jsb.getAccuracy();
                lengv0 += jsb.getBeatmap().getTotalLength();
            } else if (j >= 45 && j < 55) {
                ppv45 += jsb.getPp();
                accv45 += jsb.getAccuracy();
                lengv45 += jsb.getBeatmap().getTotalLength();
            } else if (j >= 90) {
                ppv90 += jsb.getPp();
                accv90 += jsb.getAccuracy();
                lengv90 += jsb.getBeatmap().getTotalLength();
            }
        }
        double sumOxy = 0.0D;
        double sumOx2 = 0.0D;
        double avgX = 0.0D;
        double avgY = 0.0D;
        double sumX = 0.0D;
        for (int n = 1; n <= ys.length; n++) {
            double weight = Math.log1p(n + 1.0D);
            sumX += weight;
            avgX += n * weight;
            avgY += ys[n - 1] * weight;
        }
        avgX /= sumX;
        avgY /= sumX;
        for (int n = 1; n <= ys.length; n++) {
            sumOxy += (n - avgX) * (ys[n - 1] - avgY) * Math.log1p(n + 1.0D);
            sumOx2 += Math.pow(n - avgX, 2.0D) * Math.log1p(n + 1.0D);
        }
        double Oxy = sumOxy / sumX;
        double Ox2 = sumOx2 / sumX;
        for (double n = 100; n <= user.getStatustucs().getPlagCount(); n++) {
            double val = Math.pow(100.0D, (avgY - (Oxy / Ox2) * avgX) + (Oxy / Ox2) * n);
            if (val <= 0.0D) {
                break;
            }
            bonus += val;
        }
        rawpp = bpp + bonus;

        ppv0 /= 10;
        ppv45 /= 10;
        ppv90 /= 10;
        accv0 /= 10;
        accv45 /= 10;
        accv90 /= 10;
        lengv0 /= 10;
        lengv45 /= 10;
        lengv90 /= 10;
        if (bps.size() < 90) {
            ppv90 = 0;
            accv90 = 0;
            lengv90 = 0;
        }
        if (bps.size() < 45) {
            ppv45 = 0;
            accv45 = 0;
            lengv45 = 0;
        }
        if (bps.size() < 10) {
            ppv0 = 0;
            accv0 = 0;
            lengv0 = 0;
        }
        name = user.getUsername();
        pp = user.getPp().floatValue();
        if (pp > rawpp) {
            bonus = pp - rawpp;
        } else {
            bonus = 0;
        }
        headURL = user.getAvatarUrl();
        bgURL = user.getCoverUrl();
        acc = user.getStatustucs().getAccuracy().floatValue();
        level = user.getStatustucs().getLevelCurrent();
        rank = Math.toIntExact(user.getStatustucs().getGlobalRank());
        combo = user.getStatustucs().getMaxCombo();
        thit = user.getStatustucs().getTotalHits();
        pcont = user.getStatustucs().getPlagCount();
        ptime = user.getStatustucs().getPlatTime();

        // 4.1 准度fACC formulaic accuracy 0-1
        {
            if (acc / 100 < 0.6D){
                fa = 0;
            }else if(acc / 100 > 0.97D){
                fa = 1;
            }else {
                fa = Math.pow((acc / 100 - 0.6D) * 2.5D, 2.567D);
            }
            fa = check(fa, 0, 1);
        }

        // 4.2 潜力PTT potential 0-1.2
        {
            double rBPV = ppv0 / (ppv90 + 1);
            double rBPD = ppv0 == 0 ? 0 : (rawpp / ppv0);
            double LPI = pp > 1000 ? 1 : Math.pow(pp / 1000D , 0.5D); // low PP index 低pp指数 过低PP会导致ptt异常偏高，故需补正。

            double BPD; // BP density BP密度
            if (rBPD == 0) {
                BPD = 0;
            } else if (rBPD >= 19) {
                BPD = 0;
            } else if (rBPD >= 18) {
                BPD = (19 - rBPD) * 0.6D;
            } else if (rBPD >= 14) {
                BPD = (18 - rBPD) * 0.1D + 0.6D;
            } else {
                BPD = 1;
            }

            double BPV; // BP vitality BP活力
            if (rBPV >= 1.8) {
                BPV = 1;
            } else if (rBPV >= 1.4) {
                BPV = (rBPV - 1.4) + 0.6D;
            } else if (rBPV >= 1.3) {
                BPV = (rBPV - 1.3) * 6D;
            } else {
                BPV = 0;
            }

            double VWB; // very wide (bp) bonus 超活力奖励
            if (rBPV >= 3.8) {
                VWB = 0.2;
            } else if (rBPV >= 1.8) {
                VWB = (rBPV - 1.8) / 10D;
            } else {
                VWB = 0;
            }

            ptt = Math.pow(BPD * 0.1D , 0.4D) + BPV * 0.9D * LPI + VWB;
            ptt = check(ptt, 0, 1.2);
        }

        // 4.3 耐力STA stamina 0-1.2
        {
            double rSPT = pcont == 0 ? 0 : (ptime * 1.0D / pcont);
            double SPT; // single play count time 单次游玩时长
            if (rSPT >= 120){
                SPT = 1;
            } else if (rSPT >= 100){
                SPT = (rSPT - 100) * 0.005D + 0.9D;
            } else if (rSPT >= 60){
                SPT = (rSPT - 60) * 0.0075D + 0.6D;
            } else if (rSPT >= 40){
                SPT = (rSPT - 40) * 0.06D;
            } else {
                SPT = 0;
            }

            double rBPT = lengv0 * 0.7 + lengv45 * 0.2 + lengv90 * 0.1; // BP playtime BP 游玩时长

            double BPT; // BP playtime BP 游玩时长 等同于旧版fLENT。
            if (rBPT >= 260){
                BPT = 1;
            } else if (rBPT >= 220){
                BPT = (rBPT - 220) * 0.0025D + 0.9D;
            } else if (rBPT >= 140){
                BPT = (rBPT - 140) * 0.00375D + 0.6D;
            } else if (rBPT >= 100){
                BPT = (rBPT - 100) * 0.015D;
            } else {
                BPT = 0;
            }

            double VLB; // very long bonus 超长奖励
            if (rBPT >= 320) {
                VLB = 0.2;
            } else if (rBPT >= 280) {
                VLB = (rBPT - 280) * 0.005D;
            } else {
                VLB = 0;
            }

            sta = Math.pow((SPT * 0.4D + BPT * 0.6D), 0.8D) + VLB;
            sta = check(sta, 0, 1.2);
        }

        // 4.4 彩力PRE precision 0-1.2 模式独占评价系统，STB 因 mania 意义不大而不用。
        {
            double rPGR = pgr0 * 0.7 + pgr45 * 0.2 + pgr90 * 0.1; // raw perfect-great rate 彩黄比初值

            double PGR; // perfect-great rate 彩黄比/彩黄分数
            if (rPGR >= 2.2) {
                PGR = 1;
            } else if (rPGR >= 1.0) {
                PGR = (rPGR - 1.0) / 1.2D;
            } else {
                PGR = 0;
            }

            double VPB; // very precise bonus 超彩率奖励
            if (rPGR >= 3) {
                VPB = 0.2;
            } else if (rPGR >= 2.2) {
                VPB = (rPGR - 2.2) / 4D;
            } else {
                VPB = 0;
            }

            pre = PGR + VPB;
            pre = check(pre, 0, 1.2);
        }

        // 4.5 肝力ENG energy 0-1.2
        {
            double rLNT = Math.log1p(thit);
            double LNT; // LNTTH 总击打自然对数分数
            if (rLNT >= 17) {
                LNT = 1;
            } else if (rLNT >= 12) {
                LNT = (rLNT - 12) * 0.2D;
            } else {
                LNT = 0;
            }

            double VEB; // very energetic bonus 超肝力奖励
            if (rLNT >= 18) {
                VEB = 0.2;
            } else if (rLNT >= 17) {
                VEB = (rLNT - 17) * 0.2D;
            } else {
                VEB = 0;
            }

            eng = Math.pow(LNT , 0.6D) + VEB;
            eng = check(eng, 0, 1.2);
        }


        // 4.6 实力STH strength 0-1.2
        {
            double rHPS = ptime == 0 ? 0 : thit * 1.0D / ptime ; // raw hit per second 每秒击打初值

            double rPTR = Math.sqrt(ppv0 * lengv0) * 0.7D + Math.sqrt(ppv45 * lengv45) * 0.2D + Math.sqrt(ppv90 * lengv90) * 0.1D;
            // raw PP-time root PP-时间均方根初值

            double HPS;
            if (rHPS >= 18) {
                HPS = 1;
            } else if (rHPS >= 0) {
                HPS = rHPS / 18D;
            } else {
                HPS = 0;
            }

            double PTR;
            if (rPTR >= 400) {
                PTR = 1;
            } else if (rPTR >= 50) {
                PTR = (rPTR - 50) / 350D;
            } else {
                PTR = 0;
            }

            double VHB; // very high (pp) bonus 超实力奖励
            if (rPTR >= 500) {
                VHB = 0.2;
            } else if (rPTR >= 400) {
                VHB = (rPTR - 400) / 500D;
            } else {
                VHB = 0;
            }

            sth = Math.pow(HPS * 0.2 + PTR * 0.8 , 0.4D) + VHB;
            sth = check(sth, 0, 1.2);
        }
        // 4.7 总计TTL Total / Overall 0-1.16
        ttl = fa*0.2 + ptt*0.1 + sta*0.2 + pre*0.25 + eng*0.05 + sth*0.2;

        // 4.8 理智SAN sanity 0-1.2
        {
            double LPI = pp > 1000 ? 1 : Math.pow(pp / 1000D , 0.5D); // low PP index 低pp指数 过低PP会导致rSAN异常偏高，故需补正。

            double rSAN = fa * ptt * Math.sqrt(Math.pow(ppv0 , 2.0D) / ((ppv45 + 1.0) * (ppv90 + 1.0))) * LPI; // raw sanity 理智初值

            if (rSAN >= 5) {
                san = rSAN / 300D;
            } else if (rSAN >= 1) {
                san = 1.1 - rSAN * 0.1D;
            } else {
                san = 1.2 - rSAN * 0.2D;
            }
            san = check(san, 0, 1.2);
        }
    }

    @Override
    public double check(double value, double min, double max){
        if (value>max) return max;else return Math.max(value, min);
    }

    @Override
    public float getPpv0() {
        return ppv0;
    }

    @Override
    public float getPpv45() {
        return ppv45;
    }

    @Override
    public float getPpv90() {
        return ppv90;
    }

    @Override
    public float getAccv0() {
        return accv0;
    }

    @Override
    public float getAccv45() {
        return accv45;
    }

    @Override
    public float getAccv90() {
        return accv90;
    }

    @Override
    public long getLengv0() {
        return lengv0;
    }

    @Override
    public long getLengv45() {
        return lengv45;
    }

    @Override
    public long getLengv90() {
        return lengv90;
    }

    @Override
    public double getBpp() {
        return bpp;
    }

    @Override
    public double getRawpp() {
        return rawpp;
    }

    @Override
    public double getBonus() {
        return bonus;
    }

    @Override
    public int getXd() {
        return xd;
    }

    @Override
    public int getXc() {
        return xc;
    }

    @Override
    public int getXb() {
        return xb;
    }

    @Override
    public int getXa() {
        return xa;
    }

    @Override
    public int getXs() {
        return xs;
    }

    @Override
    public int getXx() {
        return xx;
    }

    @Override
    public int getNotfc() {
        return notfc;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float getPp() {
        return pp;
    }

    @Override
    public float getAcc() {
        return acc;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getRank() {
        return rank;
    }

    @Override
    public int getCombo() {
        return combo;
    }

    @Override
    public long getThit() {
        return thit;
    }

    @Override
    public long getPcont() {
        return pcont;
    }

    @Override
    public long getPtime() {
        return ptime;
    }

    @Override
    public double getFacc() {
        return fa;
    }

    @Override
    public double getEng() {
        return eng;
    }

    @Override
    public double getSth() {
        return sth;
    }

    @Override
    public double getStb() {
        return stb;
    }

    @Override
    public double getSta() {
        return sta;
    }

    @Override
    public double getPtt() {
        return ptt;
    }

    @Override
    public double getTtl() {
        return ttl;
    }

    @Override
    public double getSan() {
        return san;
    }

    @Override
    public String getHeadURL() {
        return headURL;
    }

    @Override
    public String getBackgroundURL() {
        return bgURL;
    }

    @Override
    public void dovs(){
        float n = pp*0.25f;
        fa *= n;
        eng *= n;
        sth *= n;
        stb *= n;
        sta *= n;
        ptt *= n;
        ttl *= n;
    }

    /**
     * 黄彩比
     *
     * @return
     */
    @Override
    public float getPGR0() {
        return pgr0;
    }

    @Override
    public float getPGR45() {
        return pgr45;
    }

    @Override
    public float getPGR90() {
        return pgr90;
    }
}
