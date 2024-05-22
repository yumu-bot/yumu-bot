package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BPFixException;
import com.now.nowbot.util.HandleUtil;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Service("BP_FIX")
public class BPFixService implements MessageService<OsuUser> {
    private static final Logger log = LoggerFactory.getLogger(BPFixService.class);

    @Resource
    OsuUserApiService userApiService;
    @Resource
    BindDao bindDao;
    @Resource
    OsuScoreApiService scoreApiService;
    @Resource
    ImageService imageService;

    public record BPFix(Score score, Double fixPP) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<OsuUser> data) throws Throwable {
        var matcher = Instructions.BP_FIX.matcher(messageText);
        if (! matcher.find()) return false;


        data.setValue(
                HandleUtil.getOsuUserFromMessageText(
                        event, matcher.group("name"), matcher.group("qq"), bindDao, userApiService)
        );

        return true;


    }

    @Override
    public void HandleMessage(MessageEvent event, OsuUser user) throws Throwable {
        var from = event.getSubject();

        var BPList = HandleUtil.getOsuBPFromMessageText(user, scoreApiService);

        if (CollectionUtils.isEmpty(BPList)) throw new BPFixException(BPFixException.Type.BF_BP_Empty);
        
        var fixes = getBPFixList(BPList);

        if (CollectionUtils.isEmpty(fixes)) throw new BPFixException(BPFixException.Type.BF_Fix_Empty);

        byte[] image;

        try {
            image = imageService.getPanelA6(user, fixes);
        } catch (Exception e) {
            log.error("理论最好成绩：渲染失败", e);
            throw new BPFixException(BPFixException.Type.BF_Render_Error);
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("理论最好成绩：发送失败", e);
            throw new BPFixException(BPFixException.Type.BF_Send_Error);
        }
    }

    // 主计算
    @Nullable
    public List<BPFix> getBPFixList(@Nullable List<Score> BPList) {
        if (CollectionUtils.isEmpty(BPList)) return null;


        return new ArrayList<>();
    }
}
