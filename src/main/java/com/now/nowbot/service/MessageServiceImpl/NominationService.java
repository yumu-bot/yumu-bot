package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.BeatMapSet;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("NOMINATE")
public class NominationService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(NominationService.class);

    @Resource
    OsuBeatmapApiService osuBeatmapApiService;
    @Resource
    OsuUserApiService osuUserApiService;
    @Resource
    ImageService imageService;

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) throws Throwable {

        var matcher = Instructions.NOMINATION.matcher(event.getRawMessage().trim());
        if (! matcher.find()) return false;

        data.setValue(matcher);
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var sid = Long.parseLong(matcher.group("sid"));

        BeatMapSet m = new BeatMapSet();

        try {
            m = osuBeatmapApiService.getBeatMapSetInfo(sid);
        } catch (Exception e) {
            log.error("NOM", e);
        }

        {
            m.getCreatorData().parseFull(osuUserApiService);
        }

        QQMsgUtil.sendImage(event.getSubject(), imageService.getMarkdownImage(m.toString()));
    }
}
