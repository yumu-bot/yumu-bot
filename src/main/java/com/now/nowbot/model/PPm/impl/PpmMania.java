package com.now.nowbot.model.PPm.impl;

import com.now.nowbot.model.JsonData.BpInfo;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.PPm.Ppm;

import java.util.List;

public class PpmMania extends Ppm {
    public PpmMania(OsuUser user, List<BpInfo> bps){
        double [] allBpPP = new double[bps.size()];
        for (int i = 0; i < bps.size(); i++) {
            var bp = bps.get(i);
            bpp += bp.getWeight().getPp();
            allBpPP[i] += Math.log10(bp.getWeight().getPp())/2;

            switch (bp.getRank()){
                case "XH", "X" -> xx++;
                case "SH", "S" -> xs++;
                case "A" -> xa++;
                case "B" -> xb++;
                case "C" -> xc++;
                case "D" -> xd++;
            }
            if (!bp.isPerfect()) notfc ++;
            if(i < 10){
                ppv0 += bp.getPp();
                accv0 += bp.getAccuracy();
                lengv0 += bp.getBeatmap().getTotalLength();
                pgr0 += 1.0*bp.getStatistics().getCountGeki()/bp.getStatistics().getCount300();
            }else if(i>=45 && i<55){
                ppv45 += bp.getPp();
                accv45 += bp.getAccuracy();
                lengv45 += bp.getBeatmap().getTotalLength();
                pgr45 += 1.0*bp.getStatistics().getCountGeki()/bp.getStatistics().getCount300();
            }else if(i>=90){
                ppv90 += bp.getPp();
                accv90 += bp.getAccuracy();
                lengv90 += bp.getBeatmap().getTotalLength();
                pgr90 += 1.0*bp.getStatistics().getCountGeki()/bp.getStatistics().getCount300();
            }
            bonus = bonusPP(allBpPP, user.getStatustucs().getPlagCount());
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
            if (bps.size()<90) {
                ppv90 = 0; accv90 = 0; lengv90 = 0; pgr0 = 0;
            }
            if (bps.size()<45) {
                ppv45 = 0; accv45 = 0; lengv45 = 0; pgr45 = 0;
            }
            if (bps.size()<10) {
                ppv0 = 0; accv0 = 0; lengv0 = 0; pgr90 = 0;
            }
            // 4.1 准度fACC formulaic accuracy 0-1
            {
                if (user.getStatustucs().getAccuracy() < 0.6D){
                    value1 = 0;
                }else if(user.getStatustucs().getAccuracy() > 0.97D){
                    value1 = 1;
                }else {
                    value1 = Math.pow((user.getStatustucs().getAccuracy() - 0.6D) * 2.5D, 2.567D);
                }
                value1 = check(value1, 0, 1);
            }
            // 4.2 潜力PTT potential 0-1.2
            {
                double rBPV = ppv0 / (ppv90 + 1);
                double rBPD = ppv0 == 0 ? 0 : (rawpp / ppv0);
                double LPI = user.getPp() > 1000 ? 1 : Math.pow(user.getPp() / 1000D , 0.5D);

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
                value2 = Math.pow(BPD * 0.1D , 0.4D) + BPV * 0.9D * LPI + VWB;
                value2 = check(value2, 0, 1.2);
            }
            // 4.3 耐力STA stamina 0-1.2
            {
                double rSPT = user.getStatustucs().getPlatTime() == 0 ? 0 : (user.getStatustucs().getPlatTime() * 1.0D / user.getStatustucs().getPlagCount());
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
                value3 = Math.pow((SPT * 0.4D + BPT * 0.6D), 0.8D) + VLB;
                value3 = check(value3, 0, 1.2);
            }
            //1.4 稳定STB stability (-0.16)-1.2 stb
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
                value4 = PGR + VPB;
                value4 = check(value4, 0, 1.2);
            }
            // 4.5 肝力ENG energy 0-1.2
            {
                double rLNT = Math.log1p(user.getStatustucs().getTotalHits());
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
                value5 = Math.pow(LNT , 0.6D) + VEB;
                value5 = check(value5, 0, 1.2);
            }
            // 4.6 实力STH strength 0-1.2
            {
                double rHPS = user.getStatustucs().getPlatTime() == 0 ? 0 : user.getStatustucs().getTotalHits() * 1.0D / user.getStatustucs().getPlatTime() ; // raw hit per second 每秒击打初值

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

                value6 = Math.pow(HPS * 0.2 + PTR * 0.8 , 0.4D) + VHB;
                value6 = check(value6, 0, 1.2);
            }
            //  4.7 总计TTL Total / Overall 0-1.16
            value7 = value1 *0.2 + value2*0.1 + value3*0.2 + value4*0.25 + value5*0.05 + value6*0.2;
            value7 *= 100;
            {
                double LPI = user.getPp() > 1000 ? 1 : Math.pow(user.getPp() / 1000D , 0.5D); // low PP index 低pp指数 过低PP会导致rSAN异常偏高，故需补正。

                double rSAN = value1 * value2 * Math.sqrt(Math.pow(ppv0 , 2.0D) / ((ppv45 + 1.0) * (ppv90 + 1.0))) * LPI; // raw sanity 理智初值

                if (rSAN >= 5) {
                    value8 = rSAN / 300D;
                } else if (rSAN >= 1) {
                    value8 = 1.1 - rSAN * 0.1D;
                } else {
                    value8 = 1.2 - rSAN * 0.2D;
                }
                value8 = check(value8, 0, 1.2);
            }
        }
    }
}
