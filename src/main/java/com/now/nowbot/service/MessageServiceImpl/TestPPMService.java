package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.now.nowbot.util.SkiaUtil.getBonusPP;

@Service("TESTPPM")
public class TestPPMService implements MessageService<Matcher> {
    @Autowired
    public TestPPMService(OsuGetService osuGetService,BindDao bindDao) {
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
    }

    BindDao bindDao;
    private final OsuGetService osuGetService;

    Pattern pattern = Pattern.compile("[!！]\\s*(?i)testppm(\\s*[:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*))");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = pattern.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    @CheckPermission(test = true)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {

        OsuUser user;
        List<Score> bpList;
        var mode = OsuMode.getMode(matcher.group("mode"));
        if (matcher.group("name") != null && !matcher.group("name").trim().isEmpty()) {
            var id = osuGetService.getOsuId(matcher.group("name").trim());
            user = osuGetService.getPlayerOsuInfo(id);
            bpList = osuGetService.getBestPerformance(id, mode, 0, 100);
        } else {
            var userBin = bindDao.getUser(event.getSender().getId());
            user = osuGetService.getPlayerOsuInfo(userBin);
            bpList = osuGetService.getBestPerformance(userBin, mode, 0, 100);
        }

        var date = new ppmtest();
        date.act(user, bpList);
        StringBuilder sb = new StringBuilder();
        sb.append(user.getUsername()).append(',')
                .append(user.getGlobalRank()).append(',')
                .append(user.getPP()).append(',')
                .append(user.getAccuracy()).append(',')
                .append(user.getLevelCurrent()).append(',')
                .append(user.getStatistics().getMaxCombo()).append(',')
                .append(user.getTotalHits()).append(',')
                .append(user.getPlayCount()).append(',')
                .append(user.getPlayTime()).append(',')
                .append(date.notfc).append(',')
                .append(date.rawpp).append(',')
                .append(date.xx).append(',')
                .append(date.xs).append(',')
                .append(date.xa).append(',')
                .append(date.xb).append(',')
                .append(date.xc).append(',')
                .append(date.xd).append(',')
                .append(date.ppv0).append(',')
                .append(date.accv0).append(',')
                .append(date.lengv0).append(',')
                .append(date.pgr0).append(',')
                .append(date.ppv45).append(',')
                .append(date.accv45).append(',')
                .append(date.lengv45).append(',')
                .append(date.pgr45).append(',')
                .append(date.ppv90).append(',')
                .append(date.accv45).append(',')
                .append(date.lengv90).append(',')
                .append(date.pgr90);

        event.getSubject().sendMessage(sb.toString());
    }
    static class ppmtest{
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
        protected double bpPP = 0;
        protected double rawpp = 0;
        protected double bonus = 0;
        protected int xd = 0;
        protected int xc = 0;
        protected int xb = 0;
        protected int xa = 0;
        protected int xs = 0;
        protected int xx = 0;
        protected int notfc = 0;
        private void act(OsuUser user, List<Score> bps){
            double[] bpPPs = new double[bps.size()];
            for (int i = 0; i < bps.size(); i++) {
                var bp = bps.get(i);
                var bpiPP = bp.getWeight().getPP();
                var bprPP = bp.getPP();
                bpPP += bpiPP;
                bpPPs[i] = bprPP;

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
            double pp = user.getStatistics().getPP();
            double acc = user.getStatistics().getAccuracy();
            double pc = user.getStatistics().getPlayCount();
            double pt = user.getStatistics().getPlayTime();
            double tth = user.getStatistics().getTotalHits();
        }
    }

}
