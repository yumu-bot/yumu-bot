package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.ServiceException.MiniCardException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Objects;

@Service("PR_CARD")
public class ScorePRCardService implements MessageService<ScorePRService.ScorePRParam> {

    @Resource
    BindDao bindDao;
    @Resource
    OsuUserApiService userApiService;
    @Resource
    ImageService imageService;
    @Resource
    OsuScoreApiService scoreApiService;
    @Resource
    OsuBeatmapApiService beatmapApiService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<ScorePRService.ScorePRParam> data) throws Throwable {
        var matcher2 = Instructions.DEPRECATED_YMX.matcher(messageText);
        if (matcher2.find()) throw new MiniCardException(MiniCardException.Type.MINI_Deprecated_X);

        var matcher = Instructions.PR_CARD.matcher(messageText);
        if (! matcher.find()) return false;
        boolean isRecent;

        if (StringUtils.hasText(matcher.group("recent"))) {
            isRecent = true;
        } else if (StringUtils.hasText(matcher.group("pass"))) {
            isRecent = false;
        } else {
            throw new MiniCardException(MiniCardException.Type.MINI_Classification_Error);
        }

        var name = matcher.group("name");
        var hasSpaceAtEnd = StringUtils.hasText(name) && name.endsWith(" ");
        var nStr = matcher.group("n");

        //处理 n
        long n = 1L;

        if (StringUtils.hasText(nStr)) {
            if (hasSpaceAtEnd) {
                //如果输入的有空格，并且后面有数字，则主观认为后面的是天数（比如 !t osu 420），如果找不到再合起来
                try {
                    n = Long.parseLong(nStr);
                } catch (NumberFormatException e) {
                    throw new MiniCardException(MiniCardException.Type.MINI_Score_RankError);
                }
            } else {
                //避免 !b lolol233 这样子被错误匹配
                name += nStr;
            }
        }

        //避免 !b lolol233 这样子被错误匹配
        if (n < 1L || n > 100L) {
            name += nStr;
            n = 1L;
        }

        // 构建参数
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        String qqStr = matcher.group("qq");
        BinUser binUser;
        OsuMode mode = OsuMode.getMode(matcher.group("mode"));

        if (Objects.nonNull(at)) {
            binUser = bindDao.getUserFromQQ(at.getTarget());
        } else if (StringUtils.hasText(name)) {
            binUser = new BinUser();
            Long id;
            try {
                id = userApiService.getOsuId(name.trim());
                binUser.setOsuID(id);
            } catch (WebClientResponseException.NotFound e) {

                // 补救机制 1
                try {
                    id = userApiService.getOsuId(name.concat(nStr));
                    binUser.setOsuID(id);
                } catch (WebClientResponseException.NotFound e1) {
                    throw new MiniCardException(MiniCardException.Type.MINI_Player_NotFound, binUser.getOsuName());
                }
            } catch (Exception e) {
                throw new MiniCardException(MiniCardException.Type.MINI_Player_NotFound, binUser.getOsuName());
            }
        } else if (StringUtils.hasText(qqStr)) {
            try {
                long qq = Long.parseLong(qqStr);
                binUser = bindDao.getUserFromQQ(qq);
            } catch (BindException e) {
                throw new MiniCardException(MiniCardException.Type.MINI_QQ_NotFound, qqStr);
            }
        } else {
            try {
                binUser = bindDao.getUserFromQQ(event.getSender().getId());
            } catch (BindException e) {
                throw new MiniCardException(MiniCardException.Type.MINI_Me_TokenExpired);
            }
        }

        if (Objects.isNull(binUser)) {
            throw new MiniCardException(MiniCardException.Type.MINI_Me_TokenExpired);
        }

        data.setValue(
                new ScorePRService.ScorePRParam(binUser, Math.toIntExact(n - 1), 1, isRecent, false, mode)
        );

        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, ScorePRService.ScorePRParam param) throws Throwable {
        var from = event.getSubject();

        Score score;
        BeatMap beatMap;

        try {
            score = scoreApiService.getRecent(param.user().getOsuID(), param.mode(), param.offset(), param.limit(), ! param.isRecent()).getFirst();
        } catch (Exception e) {
            throw new MiniCardException(MiniCardException.Type.MINI_Recent_NotFound, param.user().getOsuName());
        }

        try {
            beatMap = beatmapApiService.getBeatMapInfo(score.getBeatMap().getId());
            score.setBeatMap(beatMap);
            score.setBeatMapSet(beatMap.getBeatMapSet());
        } catch (Exception e) {
            throw new MiniCardException(MiniCardException.Type.MINI_Map_FetchError);
        }

        byte[] image;

        try {
            image = imageService.getPanelGamma(score);
        } catch (Exception e) {
            throw new MiniCardException(MiniCardException.Type.MINI_Render_Error);
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            throw new MiniCardException(MiniCardException.Type.MINI_Send_Error);
        }
    }
}