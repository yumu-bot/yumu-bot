package com.now.nowbot.service.messageServiceImpl;

import com.now.nowbot.config.Permission;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.json.OsuUser;
import com.now.nowbot.model.json.Score;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.osuApiService.OsuScoreApiService;
import com.now.nowbot.service.osuApiService.OsuUserApiService;
import com.now.nowbot.throwable.GeneralTipsException;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instruction;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

@Service("TEST_FIX")
public class TestFixPPService implements MessageService<Matcher> {
    @Resource
    OsuUserApiService userApiService;
    @Resource
    OsuBeatmapApiService beatmapApiService;
    @Resource
    OsuScoreApiService scoreApiService;
    
    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) throws Throwable {
        var m = Instruction.TEST_FIX.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        if (Permission.isCommonUser(event)) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Permission_Group);
        }

        var names = DataUtil.splitString(matcher.group("data"));
        var mode = OsuMode.getMode(matcher.group("mode"));

        if (CollectionUtils.isEmpty(names)) throw new GeneralTipsException(GeneralTipsException.Type.G_Fetch_List);
        
        StringBuilder sb = new StringBuilder();
        
        for (var name : names) {
            if (! StringUtils.hasText(name)) {
                break;
            }

            OsuUser user;
            List<Score> bps;
            double playerPP;

            try {
                var id = userApiService.getOsuId(name);
                user = userApiService.getPlayerOsuInfo(id);
                playerPP = Objects.requireNonNullElse(user.getPP(), 0d);
                
                if (mode == OsuMode.DEFAULT) {
                    mode = user.getCurrentOsuMode();
                }
                
                bps = scoreApiService.getBestPerformance(id, mode, 0, 100);
            } catch (Exception e) {
                sb.append("name=").append(name).append(" not found").append('\n');
                break;
            }
            
            if (CollectionUtils.isEmpty(bps)) {
                sb.append("name=").append(name).append(" bp is empty").append('\n');
            }

            List<Score> fixed = new ArrayList<>(bps.size());

            float bpPP = 0f;

            for (var bp : bps) {
                beatmapApiService.applyBeatMapExtendFromDataBase(bp);

                int max = bp.getBeatMap().getMaxCombo();
                int combo = bp.getMaxCombo();

                int miss = bp.getStatistics().getCountMiss();
                int all = Objects.requireNonNullElse(bp.getStatistics().getCountAll(bp.getMode()), 1);

                // 断连击，mania 模式不参与此项筛选
                boolean isChoke = (miss == 0) && (combo < Math.round(max * 0.98f)) && (bp.getMode() != OsuMode.MANIA);

                // 含有 <1% 的失误
                boolean has1pMiss = (miss > 0) && ((1f * miss / all) <= 0.01f);

                // 并列关系，miss 不一定 choke（断尾不会计入 choke），choke 不一定 miss（断滑条
                if (isChoke || has1pMiss) {
                    var pp = (float) beatmapApiService.getFcPP(bp).getPp();

                    bp.setPP(pp);
                }

                fixed.add(bp);
                bpPP += bp.getWeightedPP();
            }

            fixed = fixed.stream().sorted(Comparator.comparing(Score::getPP).reversed()).toList();

            float weight = 1f / 0.95f;

            for (var f : fixed) {
                weight *= 0.95f;
                var pp = (float) f.getPP();

                f.setWeight(new Score.Weight(weight, pp * weight));
            }

            var fixedPP = fixed.stream().mapToDouble(Score::getWeightedPP).reduce(Double::sum).orElse(0d);

            var resultPP = playerPP - bpPP + fixedPP;
            sb.append(Math.round(resultPP)).append(',').append(' ');
        }


        from.sendMessage(sb.substring(0, sb.length() - 2));
    }
}
