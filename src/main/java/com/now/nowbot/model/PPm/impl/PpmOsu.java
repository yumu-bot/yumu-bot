package com.now.nowbot.model.PPm.impl;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.PPm.Ppm;

import java.util.List;

import static com.now.nowbot.util.SkiaUtil.getBonusPP;

public class PpmOsu extends Ppm {
    public PpmOsu(OsuUser user, List<Score> bps) {
        double[] bpPPs = new double[bps.size()];
        for (int i = 0; i < bps.size(); i++) {
            var bp = bps.get(i);
            var bpiPP = bp.getWeight().getPP();
            var bprPP = bp.getPP();
            bpPP += bpiPP;
            bpPPs[i] = bprPP; // Math.log10(bp.getWeight().getPP()) / 2; //这个lg /2 是从哪来的？

            switch (bp.getRank()) {
                case "XH", "X" -> xx++;
                case "SH", "S" -> xs++;
                case "A" -> xa++;
                case "B" -> xb++;
                case "C" -> xc++;
                case "D" -> xd++;
            }
            if (!bp.isPerfect()) notfc++;
            if (i < 10) {
                ppv0 += bp.getPP();
                accv0 += bp.getAccuracy();
                lengv0 += bp.getBeatMap().getTotalLength();
            } else if (i >= 45 && i < 55) {
                ppv45 += bp.getPP();
                accv45 += bp.getAccuracy();
                lengv45 += bp.getBeatMap().getTotalLength();
            } else if (i >= 90) {
                ppv90 += bp.getPP();
                accv90 += bp.getAccuracy();
                lengv90 += bp.getBeatMap().getTotalLength();
            }
        }
        //bonus = bonusPP(allBpPP, user.getStatistics().getPlayCount());
        bonus = getBonusPP(user.getPP(), bpPPs);
        rawpp = user.getPP() - bonus;

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
        double pp = user.getPP();
        double acc = user.getAccuracy();
        double pc = user.getPlayCount();
        double pt = user.getPlayTime();
        double tth = user.getTotalHits();


        // 1.1 准度fACC formulaic accuracy 0-1.2
        {
            double rFA;
            if (acc == 100) {
                rFA = 1;
            } else if (acc >= 60) {
                rFA = Math.pow((acc / 100 - 0.6) / 0.4D, 1.776D);
            } else {
                rFA = 0;
            }

            double PFB = ((xx + xs + xa + xb + xc + xd - notfc) * 1.0D) / 500; // 完美奖励

            value1 = rFA + PFB;
            value1 = check(value1, 0, 1.2);
        }

        // 1.2 潜力PTT potential 0-1.2
        {
            double rBPV = ppv0 / (ppv90 + 5);
            double rBPD = ppv0 == 0 ? 0 : (rawpp / ppv0);
            double LPI = pp > 1000 ? 1 : Math.pow(pp / 1000D, 0.5D); // low PP index 低pp指数 过低PP会导致ptt异常偏高，故需补正。

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
                BPV = (rBPV - 1.4) * 0.25D + 0.9D;
            } else if (rBPV >= 1.2) {
                BPV = (rBPV - 1.2) * 1.5D + 0.6D;
            } else if (rBPV >= 1.0) {
                BPV = (rBPV - 1) * 3D;
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

            value2 = Math.pow(BPD, 0.4D) * 0.2D + BPV * 0.8D * LPI + VWB;
            value2 = check(value2, 0, 1.2);
        }

        // 1.3 耐力STA stamina 0-1.2
        {
            double rSPT = pc == 0 ? 0 : (pt / pc);
            double SPT; // single play count time 单次游玩时长
            if (rSPT >= 90) {
                SPT = 1;
            } else if (rSPT >= 80) {
                SPT = (rSPT - 80) * 0.01D + 0.9D;
            } else if (rSPT >= 40) {
                SPT = (rSPT - 40) * 0.0075D + 0.6D;
            } else if (rSPT >= 30) {
                SPT = (rSPT - 30) * 0.06D;
            } else {
                SPT = 0;
            }

            double rBPT = lengv0 * 0.7 + lengv45 * 0.2 + lengv90 * 0.1; // BP playtime BP 游玩时长

            double BPT; // BP playtime BP 游玩时长 等同于旧版fLENT。
            if (rBPT >= 240) {
                BPT = 1;
            } else if (rBPT >= 200) {
                BPT = (rBPT - 200) * 0.0025D + 0.9D;
            } else if (rBPT >= 50) {
                BPT = (rBPT - 50) * 0.002D + 0.6D;
            } else if (rBPT >= 30) {
                BPT = (rBPT - 30) * 0.03D;
            } else {
                BPT = 0;
            }

            double VLB; // very long bonus 超长奖励
            if (rBPT >= 320) {
                VLB = 0.2;
            } else if (rBPT >= 240) {
                VLB = (rBPT - 240) * 0.0025D;
            } else {
                VLB = 0;
            }

            value3 = Math.pow((SPT * 0.4D + BPT * 0.6D), 0.8D) + VLB;
            value3 = check(value3, 0, 1.2);
        }
        //1.4 稳定STB stability (-0.16)-1.2 stb
        {
            double FCN = (xx + xs) / 100D; // full-combo number 全连数量

            double GRD = (xx * 1.0 + xs * 0.95 + xa * 0.9 + xb * 0.4 - xc * 0.2 - xd * 0.4) / bps.size(); // grade 评级分数
            GRD = check(GRD, 0, 1);

            double PFB = ((bps.size() - notfc) * 1.0D) / 500; // 完美奖励

            value4 = Math.pow(FCN * 0.2D + GRD * 0.8D, 0.6D) + PFB;
            value4 = check(value4, 0, 1.2);
        }

        // 1.5 肝力ENG energy 0-1.2

        {
            double rLNT = Math.log1p(tth);
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

            value5 = Math.pow(LNT, 0.6D) + VEB;
            value5 = check(value5, 0, 1.2);
        }

        // 1.6 实力STH strength 0-1.2
        {
            double rHPS = pt == 0 ? 0 : tth / pt; // raw hit per second 每秒击打初值

            double rLNB = Math.log1p(ppv0 * lengv0);
            // raw ln (the) best (performance multiplayer) 最好表现因子自然对数初值

            double HPS;
            if (rHPS >= 5) {
                HPS = 1;
            } else if (rHPS >= 0) {
                HPS = rHPS / 5D;
            } else {
                HPS = 0;
            }

            double LNB;
            if (rLNB >= 11.5) {
                LNB = 1;
            } else if (rLNB >= 0) {
                LNB = Math.pow(rLNB / 11.5D, 3.0D);
            } else {
                LNB = 0;
            }

            double VHB; // very high (pp) bonus 超实力奖励
            if (rLNB >= 12.5) {
                VHB = 0.2;
            } else if (rLNB >= 11.5) {
                VHB = Math.pow(rLNB - 11.5, 0.5D) * 0.2D;
            } else {
                VHB = 0;
            }

            value6 = Math.pow(HPS * 0.2 + LNB * 0.8, 0.4D) + VHB;
            value6 = check(value6, 0, 1.2);

        }
        // 1.7 总计TTL Total / Overall 0-1.2
        value7 = value1 * 0.2 + value2 * 0.1 + value3 * 0.2 + value4 * 0.2 + value5 * 0.1 + value6 * 0.2;
        value7 *= 100;

        // 1.8 理智SAN sanity 0-1.2
        {
            double LPI = pp > 1000 ? 1 : Math.pow(pp / 1000D, 0.5D); // low PP index 低pp指数 过低PP会导致rSAN异常偏高，故需补正。

            double PCI = Math.pow(ppv0 * 30 / (pc + 100), 0.8D); // play count index PC因子

            double rSAN = value1 * value2 * Math.sqrt(Math.pow(ppv0, 2.0D) / ((ppv45 + 1.0) * (ppv90 + 1.0))) * LPI * PCI; // raw sanity 理智初值

            if (rSAN >= 5) {
                value8 = 3D / rSAN;
            } else if (rSAN >= 1) {
                value8 = 1.1 - rSAN * 0.1D;
            } else {
                value8 = 1.2 - rSAN * 0.2D;
            }
            value8 = check(value8, 0, 1.2);
            value8 *= 100;
        }
    }
}
