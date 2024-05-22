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
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.HandleUtil;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;

@Service("BP_FIX")
public class BPFixService implements MessageService<BPFixService.BPFixParam> {
    private static final Logger log = LoggerFactory.getLogger(BPFixService.class);

    @Resource
    OsuUserApiService userApiService;
    @Resource
    BindDao bindDao;
    @Resource
    OsuScoreApiService scoreApiService;
    @Resource
    ImageService imageService;

    public record BPFixParam(OsuUser user, String mode, int offset, int limit) {}

    public record BPFix(Long id, Float fixPP) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<BPFixParam> data) throws Throwable {
        var matcher = Instructions.BP_FIX.matcher(messageText);
        if (! matcher.find()) return false;

        var name = matcher.group("name");
        var nStr = matcher.group("n");
        var mStr = matcher.group("m");
        var hasHash = StringUtils.hasText(matcher.group("hash"));

        int offset;
        int limit;

        {   // !p 45-55 offset/n = 44 limit/m = 11
            //处理 n，m
            long n = 100L;
            long m;

            var noSpaceAtEnd = StringUtils.hasText(name) && !name.endsWith(" ") && !hasHash;

            if (StringUtils.hasText(nStr)) {
                if (noSpaceAtEnd) {
                    // 如果名字后面没有空格，并且有 n 匹配，则主观认为后面也是名字的一部分（比如 !t lolol233）
                    name += nStr;
                    nStr = "";
                } else {
                    // 如果输入的有空格，并且有名字，后面有数字，则主观认为后面的是天数（比如 !t osu 420），如果找不到再合起来
                    // 没有名字，但有 n 匹配的也走这边 parse
                    try {
                        n = Long.parseLong(nStr);
                    } catch (NumberFormatException e) {
                        throw new BPFixException(BPFixException.Type.BF_Map_RankError);
                    }
                }
            }

            //避免 !b 970 这样子被错误匹配
            var isIllegalN = n < 1L || n > 100L;
            if (isIllegalN) {
                if (StringUtils.hasText(name)) {
                    name += nStr;
                } else {
                    name = nStr;
                }

                n = 100L;
            }

            if (!StringUtils.hasText(mStr)) {
                m = n;
                n = 1L;
            } else {
                try {
                    m = Long.parseLong(mStr);
                } catch (NumberFormatException e) {
                    m = 100L - n + 1L;
                }
            }

            offset = DataUtil.parseRange2Offset(Math.toIntExact(n), Math.toIntExact(m));
            limit = DataUtil.parseRange2Limit(Math.toIntExact(n), Math.toIntExact(m));
        }


        data.setValue(new BPFixParam(
                HandleUtil.getOsuUserFromMessageText(event, name, matcher.group("qq"), matcher.group("mode"), bindDao, userApiService),
                matcher.group("mode"), offset, limit)
        );

        return true;


    }

    @Override
    public void HandleMessage(MessageEvent event, BPFixParam param) throws Throwable {
        var from = event.getSubject();

        var BPList = HandleUtil.getOsuBPFromMessageText(param.user, param.mode, param.offset, param.limit, scoreApiService);

        if (CollectionUtils.isEmpty(BPList)) throw new BPFixException(BPFixException.Type.BF_BP_Empty);
        
        var fixes = getBPFixList(BPList);

        if (CollectionUtils.isEmpty(fixes)) throw new BPFixException(BPFixException.Type.BF_Fix_Empty);

        byte[] image;

        try {
            image = imageService.getPanelA7(param.user, fixes);
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
    public Map<String, Object> getBPFixList(@Nullable List<Score> BPList) throws BPFixException {
        if (CollectionUtils.isEmpty(BPList)) return null;

        // 筛选需要 fix 的图，带 miss 的
        var rankList = new ArrayList<Integer>();


        var scores = new ArrayList<Score>();

        for (int i = 0; i < BPList.size(); i++) {
            var s = BPList.get(i);

            if (Objects.requireNonNullElse(s.getStatistics().getCountMiss(), 0) > 0) {
                rankList.add(i + 1);
                scores.add(s);
            }

        }

        if (CollectionUtils.isEmpty(scores)) return null;

        Map<Long, Float> fixMap;

        try {
            fixMap = imageService.getBPFix(scores);
        } catch (ResourceAccessException | HttpServerErrorException.InternalServerError e) {
            throw new BPFixException(BPFixException.Type.BF_Exchange_TooMany);
        } catch (WebClientResponseException e) {
            throw new BPFixException(BPFixException.Type.BF_Render_ConnectFailed);
        }


        for (var s : scores) {
            var f = fixMap.get(s.getBeatMap().getId());
            s.setFcPP(f);
        }

        var result = new HashMap<String, Object>(2);
        result.put("scores", scores);
        result.put("ranks", rankList);

        return result;
    }
}
