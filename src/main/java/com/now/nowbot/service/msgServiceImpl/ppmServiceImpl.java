package com.now.nowbot.service.msgServiceImpl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.entity.BinUser;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;

@Service
public class ppmServiceImpl extends MessageService{
    public ppmServiceImpl() {
        super("ppm");
    }
    @Autowired
    OsuGetService osuGetService;

    @Override
    public void handleMsg(MessageEvent event) {
        Contact from;
        if(event instanceof GroupMessageEvent) {
            from = ((GroupMessageEvent) event).getGroup();
        }else {
            from = event.getSender();
        }

        BinUser user = BindingUtil.readUser(event.getSender().getId());
        if (user == null){
            from.sendMessage("您未绑定，请绑定后使用");
        }
        dates userinfo;
        {
            var userdate = osuGetService.getPlayerOsuInfo(user);
            var bpdate = osuGetService.getOsuBestMap(user, 0, 100);
            userinfo = new dates(userdate, bpdate);
        }

        //1.1 准度fACC formulaic accuracy 0-1 fa
        double fa = ((userinfo.acc/100)<0.6D?0:(userinfo.acc/100-0.6)*Math.pow(2.5D,1.776D));
        //1.2 1.2 潜力PTT potential 0-1 ptt
        double ptt;
        {
            double bpmxd = Math.pow(0.9D, userinfo.ppv45 / (userinfo.accv0 - userinfo.ppv90 + 1));
            double rBPD = userinfo.rawpp / userinfo.ppv0;
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
            ptt = Math.pow((BPD*0.4 + bpmxd*0.2 + 0.4),0.8D);
        }
        //1.3 耐力STA stamina 0-1.2 sta
        double sta;
        {
            double rSP = 1.0*userinfo.ptime/userinfo.pcont;
            double SPT;
            if(rSP<30){
                SPT = 0;
            }else if(rSP<=180){
                SPT = 1 - Math.pow((180-rSP)/150, 2.357);
            }else{
                SPT = 1;
            }
            double rLN = userinfo.lengv0*0.7 + userinfo.lengv45*0.2 + userinfo.lengv90*0.1;
            double VLB;
            if(rLN<180){
                VLB = 0;
            }else if(rLN<=240){
                VLB = Math.pow((rLN-180)/60,0.4);
            }else{
                VLB = 1;
            }
            sta = Math.pow(SPT,0.8D) + VLB * 0.2;
        }
        //1.4 稳定STB stability (-0.16)-1.2 stb
        double stb;
        {
            double GRD = (userinfo.xx + userinfo.xs*0.9 + userinfo.xa* 0.8 + userinfo.xb*0.4 + userinfo.xc*0.2 - userinfo.xd*0.2)/100;
            double FCN = (100-userinfo.notfc)/100D;
            double PFN = (userinfo.xs+ userinfo.xx)/100D;
            stb = GRD*0.8+(FCN+PFN)*0.2;
        }
        //1.5 肝力ENG energy eng
        double eng;
        {
            eng = userinfo.bonus /416.6667;
            if (eng>1)eng =1;
        }
        //1.6 实力STH strength sth
        double sth;
        {
            double HPS = 1D*userinfo.thit/userinfo.pcont;
            if(HPS>4.5) HPS =  4.5;
            else if(HPS<2.5) HPS =  2.5;
            sth = Math.pow((HPS-2.5)/2,0.2);
        }
        StringBuffer sb = new StringBuffer();
        DecimalFormat dx = new DecimalFormat("0.00");
        sb.append("计算结果：").append('\n')
                .append("fACC ").append(dx.format(fa*100)).append('\n')
                .append("PTT ").append(dx.format(ptt*100)).append('\n')
                .append("STA ").append(dx.format(sta*100)).append('\n')
                .append("STB ").append(dx.format(stb*100)).append('\n')
                .append("ENG ").append(dx.format(eng*100)).append('\n')
                .append("STH ").append(dx.format(sth*100));
        from.sendMessage(sb.toString());
    }
    static class dates{
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
        dates(JSONObject prd, JSONArray prbp){

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
                bonus += Math.pow(100.0D, (avgY - (Oxy / Ox2) * avgX) + (Oxy / Ox2) * n);
            }
            rawpp = bpp+ bonus;
            bonus = pp - rawpp;

            ppv0 /= 10;
            ppv45 /= 10;
            ppv90 /= 10;
            accv0 /= 10;
            accv45 /= 10;
            accv90 /= 10;
            lengv0 /= 10;
            lengv45 /= 10;
            lengv90 /= 10;
            name = prd.getString("username").replace(' ','_');
            pp = prd.getJSONObject("statistics").getFloatValue("pp");
            acc = prd.getJSONObject("statistics").getFloatValue("hit_accuracy");
            level = prd.getJSONObject("statistics").getJSONObject("level").getIntValue("current");
            rank = prd.getJSONObject("statistics").getIntValue("global_rank");
            combo = prd.getJSONObject("statistics").getIntValue("maximum_combo");
            thit = prd.getJSONObject("statistics").getLongValue("total_hits");
            pcont = prd.getJSONObject("statistics").getLongValue("play_count");
            ptime = prd.getJSONObject("statistics").getLongValue("play_time");
        }
    }
}
