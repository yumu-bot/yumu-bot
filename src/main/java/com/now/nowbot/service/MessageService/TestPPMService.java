package com.now.nowbot.service.MessageService;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.model.JsonData.BpInfo;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;

@Service("t-ppm")
public class TestPPMService implements MessageService {
    @Autowired
    public TestPPMService(OsuGetService osuGetService) {
        this.osuGetService = osuGetService;
    }

    private OsuGetService osuGetService;

    @Override
    @CheckPermission(supperOnly = true)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {

        OsuUser user;
        List<BpInfo> bpList;
        var mode = OsuMode.getMode(matcher.group("mode"));
        if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
            var id = osuGetService.getOsuId(matcher.group("name").trim());
            user = osuGetService.getPlayerOsuInfoN(id);
            bpList = osuGetService.getBestPerformance(id, mode, 0, 100);
        } else {
            var userBin = bindDao.getUser(event.getSender().getId());
            user = osuGetService.getPlayerOsuInfoN(userBin);
            bpList = osuGetService.getBestPerformance(userBin, mode, 0, 100);
        }

        var date = new ppmtest();
        date.act(user, bpList);
        StringBuilder sb = new StringBuilder();
        sb.append(user.getUsername()).append(' ')
                .append(user.getGlobalRank()).append(' ')
                .append(user.getPp()).append(' ')
                .append(user.getAccuracy()).append(' ')
                .append(user.getLevelProgress()).append(' ')
                .append(user.getStatustucs().getMaxCombo()).append(' ')
                .append(user.getTotalHits()).append(' ')
                .append(user.getPlagCount()).append(' ')
                .append(user.getPlatTime()).append(' ')
                .append(date.notfc).append(' ')
                .append(date.rawpp).append(' ')
                .append(date.xx).append(' ')
                .append(date.xs).append(' ')
                .append(date.xa).append(' ')
                .append(date.xb).append(' ')
                .append(date.xc).append(' ')
                .append(date.xd).append(' ')
                .append(date.ppv0).append(' ')
                .append(date.accv0).append(' ')
                .append(date.lengv0).append(' ')
                .append(date.pgr0).append(' ')
                .append(date.ppv45).append(' ')
                .append(date.accv45).append(' ')
                .append(date.lengv45).append(' ')
                .append(date.pgr45).append(' ')
                .append(date.ppv90).append(' ')
                .append(date.accv45).append(' ')
                .append(date.lengv90).append(' ')
                .append(date.pgr90).append(' ');

        event.getSubject().sendMessage(sb.toString());
    }
    class ppmtest{
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
        private void act(OsuUser user, List<BpInfo> bps){
            double[] allBpPP = new double[bps.size()];
            for (int i = 0; i < bps.size(); i++) {
                var bp = bps.get(i);
                bpp += bp.getWeight().getPp();
                allBpPP[i] += Math.log10(bp.getWeight().getPp()) / 2;

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
                    ppv0 += bp.getPp();
                    accv0 += bp.getAccuracy();
                    lengv0 += bp.getBeatmap().getTotalLength();
                } else if (i >= 45 && i < 55) {
                    ppv45 += bp.getPp();
                    accv45 += bp.getAccuracy();
                    lengv45 += bp.getBeatmap().getTotalLength();
                } else if (i >= 90) {
                    ppv90 += bp.getPp();
                    accv90 += bp.getAccuracy();
                    lengv90 += bp.getBeatmap().getTotalLength();
                }
            }
            bonus = bonusPP(allBpPP, user.getStatustucs().getPlayCount());
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
            double pp = user.getStatustucs().getPp();
            double acc = user.getStatustucs().getAccuracy();
            double pc = user.getStatustucs().getPlayCount();
            double pt = user.getStatustucs().getPlayTime();
            double tth = user.getStatustucs().getTotalHits();
        }
    }
    static float bonusPP(double[] pp, Long pc){
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
