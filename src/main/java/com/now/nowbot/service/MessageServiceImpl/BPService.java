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
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
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
    RestTemplate template;
    ImageService imageService;

    @Autowired
    public BPService(OsuUserApiService userApiService,
                     OsuScoreApiService scoreApiService,
                     OsuBeatmapApiService beatmapApiService,
                     BindDao bindDao,
                     RestTemplate template,
                     ImageService image) {
        this.userApiService = userApiService;
        this.scoreApiService = scoreApiService;
        this.beatmapApiService = beatmapApiService;
        this.bindDao = bindDao;
        this.template = template;
        imageService = image;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<BPParam> data) throws BPException {
        var matcher = Instructions.BP.matcher(event.getRawMessage());
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
            int n;
            int m;
            if (! StringUtils.hasText(nStr)) {
                n = 1;
            } else {
                try {
                    n = Integer.parseInt(nStr);
                } catch (NumberFormatException e) {
                    throw new BPException(BPException.Type.BP_Map_RankError);
                }
            }

            //避免 !b lolol233 这样子被错误匹配
            boolean nNotFit = (n < 1 || n > 100);
            if (nNotFit) {
                name += nStr;
                n = 1;
            }

            if (mStr == null || mStr.isBlank()) {
                m = n;
            } else {
                try {
                    m = Integer.parseInt(mStr);
                } catch (NumberFormatException e) {
                    throw new BPException(BPException.Type.BP_Map_RankError);
                }
            }
            //分流：正常，相等，相反
            if (m > n) {
                offset = n - 1;
                limit = m - n + 1;
            } else if (m == n) {
                offset = n - 1;
                limit = 1;
            } else {
                offset = m - 1;
                limit = n - m + 1;
            }

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
                mode = osuUser.getPlayMode();
            }
        } catch (WebClientResponseException.Unauthorized e) {
            throw new BPException(BPException.Type.BP_Me_TokenExpired);
        } catch (WebClientResponseException.NotFound e) {
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
                var data = imageService.getPanelA4(osuUser, bpList, rankList);
                QQMsgUtil.sendImage(from, data);
            } else {
                var score = bpList.getFirst();
                var data = imageService.getPanelE(osuUser, score, beatmapApiService);
                QQMsgUtil.sendImage(from, data);
            }
        } catch (Exception e) {
            log.error("BP 发送出错", e);
            throw new BPException(BPException.Type.BP_Send_Error);
        }
    }

    public static class BPParam1 {
        String n;
        String m;
        String name;
        OsuMode mode;

        boolean s;
    }

    public record BPParam(BinUser user, int offset, int limit, OsuMode mode, boolean isMultipleBP, boolean hasName) {
    }
}
