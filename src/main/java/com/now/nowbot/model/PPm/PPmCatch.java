package com.now.nowbot.model.PPm;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.model.JsonData.BpInfo;
import com.now.nowbot.model.JsonData.OsuUser;

import java.util.List;

public class PPmCatch implements PPmObject {
    float ppv0 = 0;
    float ppv45 = 0;
    float ppv90 = 0;
    float accv0 = 0;
    float accv45 = 0;
    float accv90 = 0;
    long lengv0 = 0;
    long lengv45 = 0;
    long lengv90 = 0;
    double bpp = 0;
    double rawpp = 0;
    double bonus = 0;
    int xd = 0;
    int xc = 0;
    int xb = 0;
    int xa = 0;
    int xs = 0;
    int xx = 0;
    int notfc = 0;
    String name;
    Double pp;
    Double acc;
    int level;
    int rank;
    int combo;
    long thit;
    long pcont;
    long ptime;

    double facc;
    double eng;
    double ptt;
    double sth;
    double stb;
    double sta;
    double ttl;
    double san;
    String headURL;
    String bgURL;

    /***
     * 计算cathc ppm
     * @param prd 个人信息 JSON 替换为
     * @param prbp bp列表
     */
    public PPmCatch(JSONObject prd, JSONArray prbp) {
        double[] ys = new double[prbp.size()];
        for (int j = 0; j < prbp.size(); j++) {
            var jsb = prbp.getJSONObject(j);
            bpp += jsb.getDoubleValue("pp") * Math.pow(0.95d, j);
            ys[j] = Math.log10(jsb.getDoubleValue("pp") * Math.pow(0.95, j)) / Math.log10(100);

            if (jsb.getString("rank").startsWith("D")) xd++;
            if (jsb.getString("rank").startsWith("C")) xc++;
            if (jsb.getString("rank").startsWith("B")) xb++;
            if (jsb.getString("rank").startsWith("A")) xa++;
            if (jsb.getString("rank").startsWith("S")) xs++;
            if (jsb.getString("rank").startsWith("X")) xx++;
            if (!jsb.getBoolean("perfect")) notfc++;
            if (j < 10) {
                ppv0 += jsb.getFloatValue("pp");
                accv0 += jsb.getFloatValue("accuracy");
                lengv0 += jsb.getJSONObject("beatmap").getFloatValue("total_length");
            } else if (j >= 45 && j < 55) {
                ppv45 += jsb.getFloatValue("pp");
                accv45 += jsb.getFloatValue("accuracy");
                lengv45 += jsb.getJSONObject("beatmap").getFloatValue("total_length");
            } else if (j >= 90) {
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
        for (double n = 100; n <= prd.getJSONObject("statistics").getIntValue("play_count"); n++) {
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
        if (prbp.size() < 90) {
            ppv90 = 0;
            accv90 = 0;
            lengv90 = 0;
        }
        if (prbp.size() < 45) {
            ppv45 = 0;
            accv45 = 0;
            lengv45 = 0;
        }
        if (prbp.size() < 10) {
            ppv0 = 0;
            accv0 = 0;
            lengv0 = 0;
        }
        name = prd.getString("username");
        pp = Double.valueOf(prd.getJSONObject("statistics").getFloatValue("pp"));
        if (pp > rawpp) {
            bonus = pp - rawpp;
        } else {
            bonus = 0;
        }
        headURL = prd.getString("avatar_url");
        bgURL = prd.getString("cover_url");
        acc = Double.valueOf(prd.getJSONObject("statistics").getFloatValue("hit_accuracy"));
        level = prd.getJSONObject("statistics").getJSONObject("level").getIntValue("current");
        rank = prd.getJSONObject("statistics").getIntValue("global_rank");
        combo = prd.getJSONObject("statistics").getIntValue("maximum_combo");
        thit = prd.getJSONObject("statistics").getLongValue("total_hits");
        pcont = prd.getJSONObject("statistics").getLongValue("play_count");
        ptime = prd.getJSONObject("statistics").getLongValue("play_time");

        //3.1 准度fACC formulaic accuracy 0-1 facc
        {
            facc = ((this.acc / 100) < 0.6 ? 0 : Math.pow((this.acc / 100 - 0.6) * 2.5, 5));
            facc = check(facc, 0, 1);
        }
        //3.2 潜力PTT potential 0-1 value2

        double bpmxd = Math.pow(0.9D, this.ppv45 / (this.ppv0 - this.ppv90 + 1));
        {
            double rBPD = this.ppv0 == 0 ? 0 : (this.rawpp / this.ppv0);
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
            ptt = Math.pow((BPD * 0.2 + bpmxd * 0.4 + 0.4), 0.8D);
            ptt = check(ptt, 0, 1);
            if (prbp.size() < 100) ptt = 1;
        }
        //3.3 耐力STA stamina 0-1.2 sta

        {
            double rSP = this.pcont == 0 ? 0 : (1.0 * this.ptime / this.pcont);
            double SPT;
            if (rSP < 30) {
                SPT = 0;
            } else if (rSP <= 180) {
                SPT = 1 - Math.pow((180 - rSP) / 150, 2.357);
            } else {
                SPT = 1;
            }
            double rLN = this.lengv0 * 0.7 + this.lengv45 * 0.2 + this.lengv90 * 0.1;
            double fLEN;
            if (rLN < 30) {
                fLEN = 0;
            } else if (rLN <= 180) {
                fLEN = 1 - Math.pow((180 - rLN) / 150, 2.357);
            } else {
                fLEN = 1;
            }
            double VLB;
            if (rLN < 180) {
                VLB = 0;
            } else if (rLN <= 240) {
                VLB = Math.pow((rLN - 180) / 60, 0.4);
            } else {
                VLB = 1;
            }
            sta = Math.pow((SPT * 0.4 + fLEN * 0.6), 0.8D) + VLB * 0.2;
            sta = check(sta, 0, 1.2);
        }

        //3.4 稳定STB stability (-0.16)-1.2 stb

        {
            double GRD = (this.xx + this.xs * 0.9 + this.xa * 0.8 + this.xb * 0.4 - this.xc * 0.2 - this.xd * 0.2) / prbp.size();
            double FCN = (this.xx + this.xs) / 100D;
            double PFN = Math.pow((1 - 1.0 * this.notfc / prbp.size()), 2);
            stb = GRD * 0.8 + FCN * 0.2 + PFN * 0.2;
            stb = check(stb, 0, 1.2);
        }

        //3.5 肝力ENG energy eng

        {
            eng = this.bonus /416.6667;
            if (eng>1)eng =1;
            eng = Math.pow(eng, 0.4D);
            eng = check(eng, 0, 1);
        }

        //3.6 实力STHv2 strengthv2 sthv2

        {
            double PPTTH = 0.6D * Math.log(ppv0 * lengv0 + 1) + 0.3D * Math.log(ppv45 * lengv45 + 1) + 0.1D * Math.log(ppv90 * lengv90 + 1);
            if(PPTTH > 12) PPTTH = 12;
            else if(PPTTH < 8) PPTTH = 8;
            sth = Math.pow((PPTTH - 8)/4,0.2);
            sth = check(sth, 0, 1);
        }
        ttl = facc*0.2 + ptt*0.1 + eng*0.1 + sth*0.2 + stb*0.2 + sta*0.2;

        // ppmcatch san没写好uwu
        double PPdPC = Math.pow((ppv0 + ppv45 + ppv90) * 20 / (pcont + 100), 0.8);
        double LPPD = Math.log(ppv0 + 1) / 4.605;
        san = ppv0<20?1:(PPdPC * LPPD * Math.pow(bpmxd, 0.6) * (sth - eng * 0.4 + 0.4) * (facc + 0.25));
        //san = rsan
        if (san < 1){
            san = 120 - 20*san;
        }else if (san < 5){
            san = 110 - 10*san;
        }else {
            san = 300/san;
        }
    }

    PPmCatch(OsuUser user, List<BpInfo> bps){
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
        for (double n = 100; n <= user.getStatustucs().getplaycount(); n++) {
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
        pp = user.getPp();
        if (pp > rawpp) {
            bonus = pp - rawpp;
        } else {
            bonus = 0;
        }
        headURL = user.getAvatarUrl();
        bgURL = user.getCoverUrl();
        acc = user.getStatustucs().getAccuracy();
        level = user.getStatustucs().getLevelCurrent();
        rank = Math.toIntExact(user.getStatustucs().getGlobalRank());
        combo = user.getStatustucs().getMaxCombo();
        thit = user.getStatustucs().getTotalHits();
        pcont = user.getStatustucs().getplaycount();
        ptime = user.getStatustucs().getplaytime();

        // 3.1 准度fACC formulaic accuracy 0-1.2
        {
            double rFA;
            if (acc == 100){
                rFA = 1;
            }else if(acc >= 60){
                rFA = Math.pow((acc / 100 - 0.6) / 0.4D , 5D);
            }else {
                rFA = 0;
            }

            double PFB = ((xx + xs + xa + xb + xc + xd - notfc) * 1.0D) / 500; // 完美奖励

            facc = rFA + PFB;
            facc = check(facc, 0, 1.2);
        }

        // 3.2 潜力PTT potential 0-1.2
        {
            double rBPV = ppv0 / (ppv90 + 1);
            double rBPD = ppv0 == 0 ? 0 : (rawpp / ppv0);
            double LPI = pp > 1000 ? 1 : Math.pow(pp / 1000D , 0.5D); // low PP index 低pp指数 过低PP会导致ptt异常偏高，故需补正。

            double BPD; // BP density BP密度
            if (rBPD == 0) {
                BPD = 0;
            } else if (rBPD >= 20) {
                BPD = 0;
            } else if (rBPD >= 19) {
                BPD = (20 - rBPD) * 0.6D;
            } else if (rBPD >= 15) {
                BPD = (19 - rBPD) * 0.1D + 0.6D;
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
            if (rBPV >= 6.8) {
                VWB = 0.2;
            } else if (rBPV >= 1.8) {
                VWB = (rBPV - 1.8) / 25D;
            } else {
                VWB = 0;
            }

            ptt = Math.pow(BPD , 0.4D) * 0.2D + BPV * 0.8D * LPI + VWB;
            ptt = check(ptt, 0, 1.2);
        }

        // 3.3 耐力STA stamina 0-1.2
        {
            double rSPT = pcont == 0 ? 0 : (ptime * 1.0D / pcont);
            double SPT; // single play count time 单次游玩时长
            if (rSPT >= 90){
                SPT = 1;
            } else if (rSPT >= 80){
                SPT = (rSPT - 80) * 0.01D + 0.9D;
            } else if (rSPT >= 40){
                SPT = (rSPT - 40) * 0.0075D + 0.6D;
            } else if (rSPT >= 30){
                SPT = (rSPT - 30) * 0.06D;
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

        // 3.4 稳定STB stability 0-1.2
        {
            double FCN = (xx + xs) / 100D; // full-combo number 全连数量

            double GRD = (xx * 1.0 + xs * 0.95 + xa * 0.4 + xb * 0.2 - xc * 0.2 - xd * 0.4) / (xx + xs + xa + xb + xc + xd); // grade 评级分数
            GRD = check(GRD, 0, 1);

            double PFB = ((xx + xs + xa + xb + xc + xd - notfc) * 1.0D) / 500; // 完美奖励

            stb = Math.pow(FCN * 0.2D + GRD * 0.8D , 0.6D) + PFB;
            stb = check(stb, 0, 1.2);
        }

        // 3.5 肝力ENG energy 0-1.2
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


        // 3.6 实力STH strength 0-1.2
        {
            // rHPS raw hit per second 每秒击打初值在ctb内无意义，故禁用

            double rLNB = Math.log1p(ppv0 * lengv0);
            // raw ln (the) best (performance multiplayer) 最好表现因子自然对数初值

            double LNB;
            if (rLNB >= 11.5) {
                LNB = 1;
            } else if (rLNB >= 0) {
                LNB = Math.pow(rLNB / 11.5D , 3.0D);
            } else {
                LNB = 0;
            }

            double VHB; // very high (pp) bonus 超实力奖励
            if (rLNB >= 12.5) {
                VHB = 0.2;
            } else if (rLNB >= 11.5) {
                VHB = Math.pow(rLNB - 11.5 , 0.5D) * 0.2D;
            } else {
                VHB = 0;
            }

            sth = Math.pow(LNB , 0.4D) + VHB;
            sth = check(sth, 0, 1.2);
        }
        // 3.7 总计TTL Total / Overall 0-1.2
        ttl = facc * 0.2 + ptt * 0.1 + sta * 0.2 + stb * 0.25 + eng * 0.05 + sth * 0.2;

        // 3.8 理智SAN sanity 0-1.2
        {
            double LPI = pp > 1000 ? 1 : Math.pow(pp / 1000D, 0.5D); // low PP index 低pp指数 过低PP会导致rSAN异常偏高，故需补正。

            double PCI = Math.pow(pcont * 30 / (pp + 100), 0.8D); // play count index PC因子

            double rSAN = facc * ptt * Math.sqrt(Math.pow(ppv0, 2.0D) / ((ppv45 + 1.0) * (ppv90 + 1.0))) * LPI * PCI; // raw sanity 理智初值

            if (rSAN >= 5) {
                san = 3D / rSAN;
            } else if (rSAN >= 1) {
                san = 1.1 - rSAN * 0.1D;
            } else {
                san = 1.2 - rSAN * 0.2D;
            }
            san = check(san, 0, 1.2);
        }
    }

    @Override
    public double check(double value, double min, double max) {
        if (value > max) return max;
        else return Math.max(value, min);
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
        return pp.floatValue();
    }

    @Override
    public float getAcc() {
        return acc.floatValue();
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
        return facc;
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
        float n = (float) (pp*0.25f);
        facc *= n;
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
        return 0;
    }

    @Override
    public float getPGR45() {
        return 0;
    }

    @Override
    public float getPGR90() {
        return 0;
    }
}
