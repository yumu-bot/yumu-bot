package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.BPException;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.QQMsgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service("BP")
public class BPService implements MessageService<BPService.BPParam> {
    private static final Logger log = LoggerFactory.getLogger(BPService.class);
    OsuGetService osuGetService;
    BindDao bindDao;
    RestTemplate template;
    ImageService imageService;

    @Autowired
    public BPService(OsuGetService osuGetService, BindDao bindDao, RestTemplate template, ImageService image) {
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
        this.template = template;
        imageService = image;
    }

    static class BPParam {
        int n;
        int m;
        String name;
        OsuMode mode;
        Exception err;
    }
    private static final Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(ym)?(bestperformance|best|bp(?![a-zA-Z_])|b(?![a-zA-Z_]))+\\s*([:：](?<mode>\\w+))?\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*?)?\\s*(#?(?<n>\\d+)([-－](?<m>\\d+))?)?$");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<BPParam> data) {
        var matcher = pattern.matcher(event.getRawMessage());
        if (!matcher.find()) {
            return false;
        }
        var param = new BPParam();
        param.mode = OsuMode.getMode(matcher.group("mode"));
        param.name = matcher.group("name");

        //处理 n，m
        {
            var nStr = matcher.group("n");
            var mStr = matcher.group("m");

            if (nStr == null || nStr.isBlank()) {
                param.n = 0;
            } else {
                try {
                    param.n = Integer.parseInt(nStr) - 1;
                } catch (NumberFormatException e) {
                    param.err = new BPException(BPException.Type.BP_Map_RankError);
                }
            }

            //避免 !b lolol233 这样子被错误匹配
            if (param.n < 0 || param.n > 99) {
                param.name += nStr;
                param.n = 0;
            }

            if (mStr == null || mStr.isBlank()) {
                param.m = 1;
                data.setValue(param);
                return true;
            }

            param.m = Integer.parseInt(mStr);
            if (param.m < param.n) {
                int temp = param.m;
                param.m = param.n;
                param.n = temp;
            } else if (param.m == param.n) {
                param.err = new BPException(BPException.Type.BP_Map_RankError);
            }
            if (param.m > 100) {
                param.m = 100 - param.n; //!bp 45-101
            } else {
                param.m = param.m - param.n;//正常
            }
        }
        data.setValue(param);
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, BPParam param) throws Throwable {

        int n = param.n;
        int m = param.m;

        if (param.err != null) {
            throw param.err;
        }

        var from = event.getSubject();

        BinUser user;
        List<Score> bpList;
        ArrayList<Integer> rankList = new ArrayList<>();

        if (param.name == null || param.name.isEmpty() || param.name.isBlank()) {
            try {
                user = bindDao.getUserFromQQ(event.getSender().getId());
            } catch (BindException e) {
                throw new BPException(BPException.Type.BP_Me_NoBind);
            }
        } else {
            try {
                long uid = osuGetService.getOsuId(param.name);
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
            osuUser = osuGetService.getPlayerInfo(user, mode);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new BPException(BPException.Type.BP_Me_TokenExpired);
        } catch (HttpClientErrorException.NotFound e) {
            if (param.name == null || param.name.isEmpty() || param.name.isBlank()) {
                throw new BPException(BPException.Type.BP_Me_Banned);
            } else {
                throw new BPException(BPException.Type.BP_Player_NotFound);
            }
        } catch (Exception e) {
            log.error("获取出错", e);
            throw new BPException(BPException.Type.BP_Player_FetchFailed);
        }

        try {
            bpList = osuGetService.getBestPerformance(user, mode, n, m);
        } catch (Exception e) {
            log.error("请求出错", e);
            throw new BPException(BPException.Type.BP_Player_FetchFailed);
        }

        if (bpList == null || bpList.isEmpty()) throw new BPException(BPException.Type.BP_Player_NoBP);

        try {
            if (m > 1) {
                for (int i = n; i <= (m + n); i++) rankList.add(i + 1);
                var data = imageService.getPanelA4(osuUser, bpList, rankList);
                QQMsgUtil.sendImage(from, data);
            } else {
                var score = bpList.get(0);
                var data = imageService.getPanelE(osuUser, score, osuGetService);
                QQMsgUtil.sendImage(from, data);
            }
        } catch (Exception e) {
            log.error("BP 发送出错", e);
            throw new BPException(BPException.Type.BP_Send_Error);
        }
    }
}
