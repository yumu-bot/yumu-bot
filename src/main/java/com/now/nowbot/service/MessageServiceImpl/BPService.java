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
import com.now.nowbot.throwable.ServiceException.BPException;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.QQMsgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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

    public static class BPParam {
        String n;
        String m;
        String name;
        OsuMode mode;

        boolean s;
    }
    private static final Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(?<bp>(ym)?(bestperformance|best|bp(?![a-rt-zA-RT-Z_])|b(?![a-rt-zA-RT-Z_])))(?<s>s)?\\s*([:：](?<mode>\\w+))?\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*?)?\\s*(#?(?<n>\\d+)([-－](?<m>\\d+))?)?$");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<BPParam> data) {
        var matcher = pattern.matcher(event.getRawMessage());
        if (!matcher.find()) {
            return false;
        }
        var param = new BPParam();

        param.n = matcher.group("n");
        param.m = matcher.group("m");
        param.mode = OsuMode.getMode(matcher.group("mode"));
        param.name = matcher.group("name");
        param.s = (matcher.group("s") != null && !matcher.group("s").isBlank());

        data.setValue(param);
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, BPParam param) throws Throwable {

        int n;
        int m;
        int offset;
        int limit;
        String name = param.name;
        boolean isMultipleBP;

        //处理 n，m
        {
            var nStr = param.n;
            var mStr = param.m;

            if (nStr == null || nStr.isBlank()) {
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
            if (param.s) {
                if (nStr == null || nStr.isBlank() || nNotFit) {
                    offset = 0;
                    limit = 20;
                } else {
                    limit = offset + 1;
                    offset = 0;
                }
            }

            isMultipleBP = (limit > 1);
        }

        var from = event.getSubject();

        BinUser user;
        List<Score> bpList;
        ArrayList<Integer> rankList = new ArrayList<>();

        boolean hasName = (name == null || name.isEmpty() || name.isBlank());
        if (hasName) {
            try {
                user = bindDao.getUserFromQQ(event.getSender().getId());
            } catch (BindException e) {
                //退避 !bp
                if (event.getRawMessage().toLowerCase().contains("bp")) {
                    log.info("bp 退避成功");
                    return;
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

        var mode = param.mode == OsuMode.DEFAULT ? user.getMode() : param.mode;
        OsuUser osuUser;

        try {
            osuUser = userApiService.getPlayerInfo(user, mode);
        } catch (WebClientResponseException.Unauthorized e) {
            throw new BPException(BPException.Type.BP_Me_TokenExpired);
        } catch (WebClientResponseException.NotFound e) {
            if (hasName) {
                throw new BPException(BPException.Type.BP_Me_Banned);
            } else {
                throw new BPException(BPException.Type.BP_Player_NotFound);
            }
        } catch (Exception e) {
            log.error("BP 获取出错", e);
            throw new BPException(BPException.Type.BP_Player_FetchFailed);
        }

        try {
            bpList = scoreApiService.getBestPerformance(user, mode, offset, limit);
        } catch (Exception e) {
            log.error("BP 请求出错", e);
            throw new BPException(BPException.Type.BP_Player_FetchFailed);
        }

        if (bpList == null || bpList.isEmpty()) throw new BPException(BPException.Type.BP_Player_NoBP);

        try {
            if (isMultipleBP) {
                for (int i = offset; i <= (offset + limit); i++) rankList.add(i + 1);
                var data = imageService.getPanelA4(osuUser, bpList, rankList);
                QQMsgUtil.sendImage(from, data);
            } else {
                var score = bpList.get(0);
                var data = imageService.getPanelE(osuUser, score, beatmapApiService);
                QQMsgUtil.sendImage(from, data);
            }
        } catch (Exception e) {
            log.error("BP 发送出错", e);
            throw new BPException(BPException.Type.BP_Send_Error);
        }
    }
}
