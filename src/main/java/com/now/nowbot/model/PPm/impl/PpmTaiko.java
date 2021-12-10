package com.now.nowbot.model.PPm.impl;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.JsonData.BpInfo;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.PPm.Ppm;
import com.now.nowbot.util.Panel.PPMPanelBuilder;
import com.now.nowbot.util.Panel.PPPanelBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.Image;
import org.jetbrains.skija.Paint;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.functions.Func4;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

public class PpmTaiko extends Ppm {
    public PpmTaiko(OsuUser user, List<BpInfo> bps){
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
            }else if(i>=45 && i<55){
                ppv45 += bp.getPp();
                accv45 += bp.getAccuracy();
                lengv45 += bp.getBeatmap().getTotalLength();
            }else if(i>=90){
                ppv90 += bp.getPp();
                accv90 += bp.getAccuracy();
                lengv90 += bp.getBeatmap().getTotalLength();
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
            if (bps.size()<90) {
                ppv90 = 0; accv90 = 0; lengv90 = 0;
            }
            if (bps.size()<45) {
                ppv45 = 0; accv45 = 0; lengv45 = 0;
            }
            if (bps.size()<10) {
                ppv0 = 0; accv0 = 0; lengv0 = 0;
            }
            //1.1 准度fACC formulaic accuracy 0-1 facc
            {
                var acc = user.getStatustucs().getAccuracy();
                value1 = (acc < 0.6D ? 0 : Math.pow((acc  - 0.6) * 2.5, 1.432));
                value1 = check(value1, 0, 1);
            }
            //1.2 1.2 潜力PTT potential 0-1 ptt
                double bpmxd = Math.pow(0.9D, this.ppv45 / (this.ppv0 - this.ppv90 + 1));
            {
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
                value2 = Math.pow((BPD*0.2 + bpmxd*0.4 + 0.4),0.8D);
                value2 = check(value2, 0, 1);
                if (bps.size() < 100) value2 = 1;
            }
            //1.3 耐力STA stamina 0-1.2 sta
            {
                double rSP = user.getStatustucs().getPlagCount() == 0?0:(1.0*user.getStatustucs().getPlatTime()/user.getStatustucs().getPlagCount());
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
                value3 = Math.pow((SPT*0.4 + fLEN*0.6),0.8D) + VLB * 0.2;
                value3 = check(value3, 0, 1.2);
            }
            //1.4 稳定STB stability (-0.16)-1.2 stb
            {
                double GRD = (this.xx + this.xs*0.9 + this.xa* 0.8 + this.xb*0.4 + this.xc*0.2 - this.xd*0.2)/bps.size();
                double FCN = (100-this.notfc)/100D;
                double PFN = (this.xs+ this.xx)/100D;
                value4 = GRD*0.8+(FCN+PFN)*0.2;
                value4 = check(value4, 0, 1.2);
            }
            //1.5 肝力ENG energy eng

            {
                double LNTTH = Math.log(user.getStatustucs().getTotalHits() + 1);
                if (LNTTH < 9) value5 = 0;
                else if (LNTTH > 17) value5 = 1;
                else value5 = Math.pow((LNTTH - 9) * 0.125, 0.4);
                value5 = check(value5, 0, 1);
            }
            //3.6 实力STHv2.1 strengthv2.1 sthv2.1
            {
                double HPS = 1D * user.getStatustucs().getTotalHits() / user.getStatustucs().getPlatTime();
                if (HPS > 7.5) HPS = 7.5;
                else if (HPS < 2.5) HPS = 2.5;
                value6 = Math.pow((HPS - 2.5) / 5, 0.2);
                value6 = check(value6, 0, 1);
            }
            //Total
            value7 = value1 * 0.2 + value5 * 0.1 + value2 * 0.15 + value6 * 0.3 + value4 * 0.05 + value3 * 0.2;
            value7 *= 100;
            //san
            double PPdPC = Math.pow((ppv0 + ppv45 + ppv90) * 20 / (user.getStatustucs().getPlagCount() + 100), 0.8);
            double LPPD = Math.log(ppv0 + 1) / 4.605;
            value8 = ppv0<20?1:(PPdPC * LPPD * Math.pow(bpmxd, 0.6) *(value6 * value5 * 0.4 + 0.4)*(value1+0.25));
            if (value8 < 1){
                value8 = 120 - 20*value8;
            }else if (value8 < 5){
                value8 = 110 - 10*value8;
            }else {
                value8 = 300/value8;
            }
        }
    }
    @Override
    public void drawOverImage(Function<Image, PPMPanelBuilder> doAct) {
        try {
            doAct.apply(SkiaUtil.fileToImage(NowbotConfig.BG_PATH + "ExportFileV3/overlay-ppminusv3.2.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void drawValueName(Func4<Integer, String, String, Paint, PPPanelBuilder> doAct) {
        var p = new Paint().setARGB(255,161,161,161);
        doAct.call(0, "STA",null,p);
    }

    @Override
    public void drawValue(Func3<Integer, String, String, PPMPanelBuilder> doAct) {
        doAct.call(0, String.valueOf((int) (value1 * 100)), PanelUtil.cutDecimalPoint(value1 * 100)/*<-小字部分*/);
    }

    @Override
    public void drawRank(Func2<Integer, Double, PPMPanelBuilder> doAct) {
        doAct.call(0, value1);
    }

    @Override
    public void drawTotleName(Function<String, PPPanelBuilder> left, Function<String, PPPanelBuilder> right) {
        left.apply("Overall");
        right.apply("Sanity");
    }

    @Override
    public void drawTotleValue(Func2<String, String, PPPanelBuilder> left, Func2<String, String, PPPanelBuilder> right) {
        left.call(String.valueOf((int) value7), PanelUtil.cutDecimalPoint(value7));
        left.call(String.valueOf((int) value8), PanelUtil.cutDecimalPoint(value8));
    }
}
