package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BPException;
import com.now.nowbot.util.HandleUtil;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.JacksonUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

@Service("BP")
public class BPService implements MessageService<BPService.BPParam> {
    private static final Logger log = LoggerFactory.getLogger(BPService.class);
    @Resource
    OsuUserApiService userApiService;
    @Resource
    OsuScoreApiService scoreApiService;
    @Resource
    OsuBeatmapApiService beatmapApiService;
    @Resource
    BindDao bindDao;
    @Resource
    ImageService imageService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<BPParam> data) throws BPException {
        var matcher = Instructions.BP1.matcher(messageText);
        if (!matcher.find()) {
            return false;
        }

        var isSelf = false;
        var mode = HandleUtil.getMode(matcher);
        var user = HandleUtil.getOtherUser(event, matcher, mode);
        if (Objects.isNull(user)) {
            isSelf = true;
            user = HandleUtil.getSelfUser(event, mode);
        }

        var scores = HandleUtil.getOsuBPList(user, matcher, mode);

        data.setValue(new BPParam(user, mode, scores, isSelf));
/*
        var name = matcher.group("name");
        var s = matcher.group("s");
        var nStr = matcher.group("n");
        var mStr = matcher.group("m");
        var hasHash = StringUtils.hasText(matcher.group("hash"));

        int offset;
        int limit;
        boolean isMyself = false;
        boolean isMultipleScore;

        {   // !p 45-55 offset/n = 44 limit/m = 11
            //处理 n，m
            long n = 1L;
            long m;

            var noSpaceAtEnd = StringUtils.hasText(name) && ! name.endsWith(" ") && ! hasHash;

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
                        throw new BPException(BPException.Type.BP_Map_RankError);
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

                nStr = "";
                n = 1L;
            }

            if (! StringUtils.hasText(mStr)) {
                m = n;
            } else {
                try {
                    m = Long.parseLong(mStr);
                } catch (NumberFormatException e) {
                    throw new BPException(BPException.Type.BP_Map_RankError);
                }
            }

            offset = DataUtil.parseRange2Offset(Math.toIntExact(n), Math.toIntExact(m));
            limit = DataUtil.parseRange2Limit(Math.toIntExact(n), Math.toIntExact(m));

            //如果匹配多成绩模式，则自动设置 offset 和 limit
            if (StringUtils.hasText(s)) {
                offset = 0;
                if (! StringUtils.hasText(nStr) || isIllegalN) {
                    limit = 20;
                } else if (! StringUtils.hasText(mStr)) {
                    limit = Math.toIntExact(n);
                }
            }
            isMultipleScore = (limit > 1);
        }

        // 构建参数
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        String qqStr = matcher.group("qq");
        BinUser user;
        OsuMode mode = OsuMode.getMode(matcher.group("mode"));

        if (Objects.nonNull(at)) {
            try {
                user = bindDao.getUserFromQQ(at.getTarget());
            } catch (BindException e) {
                throw new BPException(BPException.Type.BP_Player_TokenExpired);
            }
        } else if (StringUtils.hasText(qqStr)) {
            try {
                long qq = Long.parseLong(qqStr);
                user = bindDao.getUserFromQQ(qq);
            } catch (BindException e) {
                throw new BPException(BPException.Type.BP_QQ_NotFound, qqStr);
            }
        } else if (StringUtils.hasText(name)) {
            user = new BinUser();
            long id;
            try {
                id = userApiService.getOsuId(name.trim());
            } catch (WebClientResponseException.NotFound e) {
                if (StringUtils.hasText(nStr)) {
                    // 补救机制 1
                    try {
                        id = userApiService.getOsuId(name.concat(nStr));
                    } catch (WebClientResponseException.NotFound e1) {
                        throw new BPException(BPException.Type.BP_Player_NotFound, name.concat(nStr));
                    }
                } else {
                    throw new BPException(BPException.Type.BP_Player_NotFound, name.trim());
                }
            } catch (Exception e) {
                throw new BPException(BPException.Type.BP_Player_NotFound, name.trim());
            }
            user.setOsuID(id);
            user.setMode(mode);
        } else {
            try {
                user = bindDao.getUserFromQQ(event.getSender().getId());
                isMyself = true;
            } catch (BindException e) {
                //退避 !bp
                if (event.getRawMessage().toLowerCase().contains("bp")) {
                    log.info("bp 退避成功");
                    return false;
                } else {
                    throw new BPException(BPException.Type.BP_Me_TokenExpired);
                }
            }
        }

        if (Objects.isNull(user)) {
            throw new BPException(BPException.Type.BP_Me_TokenExpired);
        }

        if (OsuMode.isDefault(mode)) {
            mode = user.getMode();
        }

        data.setValue(new BPParam(user, offset, limit, mode, isMultipleScore, isMyself));

 */
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, BPParam param) throws Throwable {


        var from = event.getSubject();

        var bpMap = param.scores();

        var mode = param.mode();
        OsuUser osuUser = param.user();


        if (CollectionUtils.isEmpty(bpMap)) throw new BPException(BPException.Type.BP_Player_NoBP, mode);

        byte[] image;

        try {
            if (bpMap.size() > 1) {
                var rankList = new ArrayList<Integer>();
                var scoreList = new ArrayList<Score>();
                for (var e : bpMap.entrySet()) {
                    rankList.add(e.getKey());
                    scoreList.add(e.getValue());
                }
                log.info("{}'s score: {}", osuUser.getUsername(), JacksonUtil.toJson(rankList));
                image = imageService.getPanelA4(osuUser, scoreList, rankList);
            } else {
                Score score = null;
                for (var e : bpMap.entrySet()) {
                    score = e.getValue();
                }
                log.info("{}'s score: {}", osuUser.getUsername(), score.getPP());
                image = imageService.getPanelE(osuUser, score, beatmapApiService);
            }
        } catch (HttpClientErrorException.Unauthorized | WebClientResponseException.Unauthorized e) {
            if (param.isMyself()) {
                throw new BPException(BPException.Type.BP_Me_TokenExpired);
            } else {
                throw new BPException(BPException.Type.BP_Player_TokenExpired);
            }
        } catch (Exception e) {
            log.error("最好成绩：渲染失败", e);
            throw new BPException(BPException.Type.BP_Render_Error);
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("最好成绩：发送失败", e);
            throw new BPException(BPException.Type.BP_Send_Error);
        }
    }

    public record BPParam(OsuUser user, OsuMode mode, Map<Integer, Score> scores, boolean isMyself) {
    }
}
