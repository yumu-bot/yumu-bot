package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.config.Permission;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.GeneralTipsException;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Matcher;

@Service("TEST_HD")
public class TestHiddenPPService implements MessageService<Matcher> {
    @Resource
    OsuUserApiService userApiService;
    @Resource
    OsuScoreApiService scoreApiService;
    
    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) throws Throwable {
        var m = Instructions.TEST_HD.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        if (! Permission.isGroupAdmin(event)) {
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
            double hiddenPP = 0d;

            try {
                var id = userApiService.getOsuId(name);
                user = userApiService.getPlayerOsuInfo(id);
                
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
            
            for (var bp : bps) {
                if (OsuMod.hasMod(bp.getMods(), OsuMod.Hidden)) {
                    hiddenPP += bp.getWeightedPP();
                }
            }

            sb.append(Math.round(hiddenPP)).append(',').append(' ');
        }


        from.sendMessage(sb.substring(0, sb.length() - 2));
    }
}
