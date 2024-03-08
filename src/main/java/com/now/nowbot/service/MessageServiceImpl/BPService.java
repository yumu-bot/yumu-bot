package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.LogException;
import com.now.nowbot.throwable.ServiceException.BPException;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instructions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;

@Service("BP")
public class BPService implements MessageService<BPService.BPParam> {
    private static final Logger log = LoggerFactory.getLogger(BPService.class);
    OsuUserApiService userApiService;
    OsuScoreApiService scoreApiService;
    OsuBeatmapApiService beatmapApiService;
    BindDao bindDao;
    ImageService imageService;

    @Autowired
    public BPService(OsuUserApiService userApiService,
                     OsuScoreApiService scoreApiService,
                     OsuBeatmapApiService beatmapApiService,
                     BindDao bindDao,
                     ImageService image) {
        this.userApiService = userApiService;
        this.scoreApiService = scoreApiService;
        this.beatmapApiService = beatmapApiService;
        this.bindDao = bindDao;
        imageService = image;
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<BPParam> data) throws BPException {
        var matcher = Instructions.BP.matcher(messageText);
        if (!matcher.find()) {
            return false;
        }
        int offset;
        int limit;
        boolean isMultipleBP;
        boolean hasName;
        BinUser user;
        OsuMode mode;

        var nStr = matcher.group("n");
        var mStr = matcher.group("m");
        var name = matcher.group("name");
        hasName = StringUtils.hasText(name);
        var s = matcher.group("s");

        //处理 n，m
        {
            long n;
            long m;
            if (! StringUtils.hasText(nStr)) {
                n = 1L;
            } else {
                try {
                    n = Long.parseLong(nStr);
                } catch (NumberFormatException e) {
                    throw new BPException(BPException.Type.BP_Map_RankError);
                }
            }

            //避免 !b lolol233 这样子被错误匹配
            boolean nNotFit = (n < 1L || n > 100L);
            if (nNotFit) {
                name += nStr;
                n = 1L;
            }

            if (mStr == null || mStr.isBlank()) {
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
                if (! StringUtils.hasText(nStr) || nNotFit) {
                    offset = 0;
                    limit = 20;
                } else {
                    limit = offset + 1;
                    offset = 0;
                }
            }

            isMultipleBP = (limit > 1);
        }

        if (! hasName) {
            try {
                user = bindDao.getUserFromQQ(event.getSender().getId());
            } catch (BindException e) {
                //退避 !bp
                if (event.getRawMessage().toLowerCase().contains("bp")) {
                    throw new LogException("bp 退避成功");
                } else {
                    throw new BPException(BPException.Type.BP_Me_NoBind);
                }
            }
        } else {
            try {
                long uid = userApiService.getOsuId(name);
                user = new BinUser();
                user.setOsuID(uid);
                user.setMode(OsuMode.DEFAULT);
            } catch (Exception e) {
                throw new BPException(BPException.Type.BP_Player_NotFound);
            }
        }

        mode = OsuMode.getMode(matcher.group("mode"));
        if (OsuMode.isDefault(mode)) {
            mode = user.getMode();
        }
        var param = new BPParam(user, offset, limit, mode, isMultipleBP, hasName);
        data.setValue(param);
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, BPParam param) throws Throwable {
        int offset = param.offset();
        int limit = param.limit();

        var from = event.getSubject();

        List<Score> bpList;
        ArrayList<Integer> rankList = new ArrayList<>();

        var mode = param.mode();
        OsuUser osuUser;
        try {
            osuUser = userApiService.getPlayerInfo(param.user(), mode);
            if (OsuMode.isDefault(mode)) {
                mode = osuUser.getOsuMode();
            }
        } catch (HttpClientErrorException.Unauthorized | WebClientResponseException.Unauthorized e) {
            throw new BPException(BPException.Type.BP_Me_TokenExpired);
        } catch (HttpClientErrorException.NotFound | WebClientResponseException.NotFound e) {
            if (param.hasName()) {
                throw new BPException(BPException.Type.BP_Me_Banned);
            } else {
                throw new BPException(BPException.Type.BP_Player_NotFound);
            }
        } catch (Exception e) {
            log.error("BP 获取出错", e);
            throw new BPException(BPException.Type.BP_Player_FetchFailed);
        }

        try {
            bpList = scoreApiService.getBestPerformance(param.user(), mode, offset, limit);
        } catch (Exception e) {
            log.error("BP 请求出错", e);
            throw new BPException(BPException.Type.BP_Player_FetchFailed);
        }

        if (bpList == null || bpList.isEmpty()) throw new BPException(BPException.Type.BP_Player_NoBP, mode);

        try {
            if (param.isMultipleBP()) {
                for (int i = offset; i <= (offset + limit); i++) rankList.add(i + 1);
                var image = imageService.getPanelA4(osuUser, bpList, rankList);
                from.sendImage(image);
            } else {
                var score = bpList.getFirst();
                var image = imageService.getPanelE(osuUser, score, beatmapApiService);
                from.sendImage(image);
            }
        } catch (HttpClientErrorException.Unauthorized | WebClientResponseException.Unauthorized e) {
            throw new BPException(BPException.Type.BP_Me_TokenExpired);
        } catch (Exception e) {
            log.error("BP 发送出错", e);
            throw new BPException(BPException.Type.BP_Send_Error);
        }
    }

    public record BPParam(BinUser user, int offset, int limit, OsuMode mode, boolean isMultipleBP, boolean hasName) {
    }
}
