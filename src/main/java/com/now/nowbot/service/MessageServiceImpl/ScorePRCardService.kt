package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.MiniCardException;
import com.now.nowbot.throwable.ServiceException.ScoreException;
import com.now.nowbot.util.CmdUtil;
import com.now.nowbot.util.Instruction;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Service("PR_CARD")
public class ScorePRCardService implements MessageService<ScorePRCardService.PRCardParam> {

    private static final Logger log = LoggerFactory.getLogger(ScorePRCardService.class);
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

    public record PRCardParam(OsuUser user, Score score) {}


    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<PRCardParam> data) throws Throwable {
        var matcher2 = Instruction.DEPRECATED_YMX.matcher(messageText);
        if (matcher2.find()) throw new MiniCardException(MiniCardException.Type.MINI_Deprecated_X);

        var matcher = Instruction.PR_CARD.matcher(messageText);
        if (! matcher.find()) return false;
        Score score = null;

        var mode = CmdUtil.getMode(matcher);
        var range = CmdUtil.getUserWithRange(event, matcher, mode, new AtomicBoolean());

        var offset = Math.max(0, range.getValue(0, true));
        List<Score> scores;
        if (StringUtils.hasText(matcher.group("recent"))) {
            scores = scoreApiService.getRecent(range.getData().getUserID(), mode.getData(),offset, 1);
        } else if (StringUtils.hasText(matcher.group("pass"))) {
            scores = scoreApiService.getRecentIncludingFail(range.getData().getUserID(), mode.getData(),offset, 1);
        } else {
            throw new MiniCardException(MiniCardException.Type.MINI_Classification_Error);
        }
        if (!scores.isEmpty()) score = scores.getFirst();
        data.setValue(new PRCardParam(range.getData(), score));
/*
        if (StringUtils.hasText(matcher.group("recent"))) {
            isPass = false;
        } else if (StringUtils.hasText(matcher.group("pass"))) {
            isPass = true;
        } else {
            throw new MiniCardException(MiniCardException.Type.MINI_Classification_Error);
        }

        var ur = HandleUtil.getUserAndRange(matcher, 1, false);
        OsuUser user;

        if (Objects.isNull(ur.user())) {
            user = HandleUtil.getMyselfUser(event, mode);
        } else {
            user = HandleUtil.getOsuUser(ur.user(), mode);
        }

        mode = HandleUtil.getModeOrElse(mode, user);

        var scores = HandleUtil.getOsuScoreList(user, mode, ur.range(), isPass);

        data.setValue(new ScorePRService.ScorePRParam(user, scores, mode));
*/
        return true;
    }

    /*
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

        var data = matcher.group("data");
        var nStr = matcher.group("n");
        var hasHash = StringUtils.hasText(matcher.group("hash"));

        //处理 n
        long n = 1L;

        var noSpaceAtEnd = StringUtils.hasText(data) && ! data.endsWith(" ") && ! hasHash;

        if (StringUtils.hasText(nStr)) {
            if (noSpaceAtEnd) {
                // 如果名字后面没有空格，并且有 n 匹配，则主观认为后面也是名字的一部分（比如 !t lolol233）
                data += nStr;
                nStr = "";
            } else {
                // 如果输入的有空格，并且有名字，后面有数字，则主观认为后面的是天数（比如 !t osu 420），如果找不到再合起来
                // 没有名字，但有 n 匹配的也走这边 parse
                try {
                    n = Long.parseLong(nStr);
                } catch (NumberFormatException e) {
                    throw new ScoreException(ScoreException.Type.SCORE_Score_RankError);
                }
            }
        }

        //避免 !b 970 这样子被错误匹配
        var isIllegalN = n < 1L || n > 100L;
        if (isIllegalN) {
            if (StringUtils.hasText(data)) {
                data += nStr;
            } else {
                data = nStr;
            }

            nStr = "";
            n = 1L;
        }

        // 构建参数
        AtMessage at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        String qq = matcher.group("qq");
        BinUser binUser;
        OsuMode mode = OsuMode.getMode(matcher.group("mode"));

        if (Objects.nonNull(at)) {
            binUser = bindDao.getUserFromQQ(at.getTarget());
        } else if (StringUtils.hasText(data)) {
            binUser = new BinUser();
            Long id;
            try {
                id = userApiService.getOsuId(data.trim());
                binUser.setOsuID(id);
            } catch (WebClientResponseException.NotFound e) {
                if (StringUtils.hasText(nStr)) {
                    // 补救机制 1
                    try {
                        id = userApiService.getOsuId(data.concat(nStr));
                        binUser.setOsuID(id);
                    } catch (WebClientResponseException.NotFound e1) {
                        throw new MiniCardException(MiniCardException.Type.MINI_Player_NotFound, data.concat(nStr));
                    }
                } else {
                    throw new MiniCardException(MiniCardException.Type.MINI_Player_NotFound, data.trim());
                }
            } catch (Exception e) {
                throw new MiniCardException(MiniCardException.Type.MINI_Player_NotFound, data.trim());
            }
        } else if (StringUtils.hasText(qq)) {
            try {
                binUser = bindDao.getUserFromQQ(Long.parseLong(qq));
            } catch (BindException e) {
                throw new MiniCardException(MiniCardException.Type.MINI_QQ_NotFound, qq);
            }
        } else {
            try {
                binUser = bindDao.getUserFromQQ(event.getSender().getId());
            } catch (BindException e) {
                throw new MiniCardException(MiniCardException.Type.MINI_Me_TokenExpired);
            }
        }

        data.setValue(
                new ScorePRService.ScorePRParam(binUser, Math.toIntExact(n - 1), 1, isRecent, false, mode)
        );

        return true;
    }

     */

    @Override
    public void HandleMessage(MessageEvent event, PRCardParam param) throws Throwable {
        var from = event.getSubject();
        var user = param.user();

        if (Objects.isNull(param.score)) {
            throw new ScoreException(ScoreException.Type.SCORE_Recent_NotFound, user.getUsername());
        }

        Score score = param.score();

        try {
            beatmapApiService.applyBeatMapExtend(score);
        } catch (Exception e) {
            throw new MiniCardException(MiniCardException.Type.MINI_Map_FetchError);
        }

        byte[] image;

        try {
            image = imageService.getPanelGamma(score);
        } catch (Exception e) {
            log.error("迷你成绩面板：渲染失败", e);
            throw new MiniCardException(MiniCardException.Type.MINI_Render_Error);
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("迷你成绩面板：发送失败", e);
            throw new MiniCardException(MiniCardException.Type.MINI_Send_Error);
        }
    }
}