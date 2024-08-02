package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.PPMinusException;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instruction;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import static com.now.nowbot.util.DataUtil.getBonusPP;

@Service("TEST_PPM")
public class TestPPMService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(TestPPMService.class);
    @Resource
    OsuUserApiService userApiService;
    @Resource
    OsuScoreApiService scoreApiService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instruction.TEST_PPM.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    @CheckPermission(test = true)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        var names = DataUtil.splitString(matcher.group("data"));
        var mode = OsuMode.getMode(matcher.group("mode"));

        if (CollectionUtils.isEmpty(names)) throw new PPMinusException(PPMinusException.Type.PM_Test_Empty);

        StringBuilder sb = new StringBuilder();

        for (var name : names) {
            if (! StringUtils.hasText(name)) {
                break;
            }

            OsuUser user;
            List<Score> bpList;

            try {
                var id = userApiService.getOsuId(name);
                user = userApiService.getPlayerOsuInfo(id);

                if (mode == OsuMode.DEFAULT) {
                    mode = user.getCurrentOsuMode();
                }
                
                bpList = scoreApiService.getBestPerformance(id, mode, 0, 100);
            } catch (Exception e) {
                sb.append("name=").append(name).append(" not found").append('\n');
                break;
            }

            var ppmData = new TestPPMData();
            ppmData.init(user, bpList);

            sb.append(user.getUsername()).append(',')
                    .append(user.getGlobalRank()).append(',')
                    .append(user.getPP()).append(',')
                    .append(user.getAccuracy()).append(',')
                    .append(user.getLevelCurrent()).append(',')
                    .append(user.getStatistics().getMaxCombo()).append(',')
                    .append(user.getTotalHits()).append(',')
                    .append(user.getPlayCount()).append(',')
                    .append(user.getPlayTime()).append(',')
                    .append(ppmData.notfc).append(',')
                    .append(ppmData.rawpp).append(',')
                    .append(ppmData.xx).append(',')
                    .append(ppmData.xs).append(',')
                    .append(ppmData.xa).append(',')
                    .append(ppmData.xb).append(',')
                    .append(ppmData.xc).append(',')
                    .append(ppmData.xd).append(',')
                    .append(ppmData.ppv0).append(',')
                    .append(ppmData.accv0).append(',')
                    .append(ppmData.lengv0).append(',')
                    .append(ppmData.pgr0).append(',')
                    .append(ppmData.ppv45).append(',')
                    .append(ppmData.accv45).append(',')
                    .append(ppmData.lengv45).append(',')
                    .append(ppmData.pgr45).append(',')
                    .append(ppmData.ppv90).append(',')
                    .append(ppmData.accv45).append(',')
                    .append(ppmData.lengv90).append(',')
                    .append(ppmData.pgr90).append('\n');
        }

        var result = sb.toString();

        //必须群聊
        if (from instanceof Group group) {
            try {
                group.sendFile(result.getBytes(StandardCharsets.UTF_8), STR."\{names.getFirst()}...-testppm.csv");
            } catch (Exception e) {
                log.error("TESTPPM:", e);
                throw new PPMinusException(PPMinusException.Type.PM_Test_SendError);
            }
        } else {
            throw new PPMinusException(PPMinusException.Type.PM_Test_NotGroup);
        }

        //event.getSubject().sendMessage(sb.toString());
    }
    static class TestPPMData {
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

        private void init(OsuUser user, List<Score> bps){
            double[] bpPPs = new double[bps.size()];
            for (int i = 0; i < bps.size(); i++) {
                var bp = bps.get(i);
                var bpiPP = Optional.ofNullable(bp.getWeightedPP()).orElse(0f);
                var bprPP = Optional.ofNullable(bp.getPP()).orElse(0f);
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
            /*
            double pp = user.getStatistics().getPP();
            double acc = user.getStatistics().getAccuracy();
            double pc = user.getStatistics().getPlayCount();
            double pt = user.getStatistics().getPlayTime();
            double tth = user.getStatistics().getTotalHits();

             */
        }
    }
}
