package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.BPException;
import com.now.nowbot.util.QQMsgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
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
    private static final Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(ym)?(bestperformance|bp(?![a-zA-Z_])|b(?![a-zA-Z_]))+\\s*([:：](?<mode>\\w+))?\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]*?)?\\s*(#?(?<n>\\d+)(-(?<m>\\d+))?)?$");

    public static void main(String[] args) {
        var m = pattern.matcher("!b:o #15");
        System.out.println(m.find());
        System.out.println(m.group("name"));
    }
    @Override
    public boolean isHandle(MessageEvent event, DataValue data) {
        var matcher = pattern.matcher(event.getRawMessage());
        if (!matcher.find()) {
            return false;
        }
        var param = new BPParam();
        try {
            var nStr = matcher.group("n");
            var mStr = matcher.group("m");
            param.name = matcher.group("name");


            if (nStr == null || nStr.isEmpty()) {
                // throw new BPException(BPException.Type.BP_Map_NoRank); // 你正则 (\d+)?  就不可能时空字符串,要么为 null 要么就是数字, 这两个抛错都不可能触发,请检查逻辑
                param.n = 0;
                //有没有可能，那个d+是后面优化的，这里没改而已
                //而且这里明显是从0开始，你初始是1，那查的就是bp2
                //而且多加几层try catch难道还会有很大的性能损失？？？？？？？？？
            } else {
//                try {
                param.n = Integer.parseInt(nStr) - 1;
//                } catch (NumberFormatException e) {
//                    throw new BPException(BPException.Type.BP_Map_RankError);
//                }
            }

            if (param.n < 0) param.n = 0;
            else if (param.n > 99) param.n = 99;

            if (mStr != null && !mStr.isEmpty()) {
                param.m = Integer.parseInt(mStr);
                if (param.m < param.n) {
                    int temp = param.m;
                    param.m = param.n;
                    param.n = temp;
                    // throw new BPException(BPException.Type.BP_Map_RankError); //!bp 55-45  直接处理了
                } else if (param.m == param.n) {
                    throw new BPException(BPException.Type.BP_Map_RankError);
                }
                if (param.m > 100) {
                    param.m = 100 - param.n; //!bp 45-101
                } else {
                    param.m = param.m - param.n;//正常
                }
            } else {
                param.m = 1;
            }
        } catch (Exception e) {
            param.err = e;
        }
        param.mode = OsuMode.getMode(matcher.group("mode"));
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
                user = bindDao.getUser(event.getSender().getId());
            } catch (Exception e) {
                throw new BPException(BPException.Type.BP_Me_LoseBind);
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

        try {
            bpList = osuGetService.getBestPerformance(user, mode, n, m);
        } catch (Exception e) {
            log.error("请求出错", e);
            throw new BPException(BPException.Type.BP_Player_FetchFailed);
        }
        if (bpList == null || bpList.isEmpty()) throw new BPException(BPException.Type.BP_Player_NoBP);

        try {
            var ouMe = osuGetService.getPlayerInfo(user, mode);

            if (m > 1) {
                for (int i = n; i <= (m + n); i++) rankList.add(i + 1);
                var data = imageService.getPanelA4(ouMe, bpList, rankList);
                QQMsgUtil.sendImage(from, data);
            } else {
                var score = bpList.get(0);
                var data = imageService.getPanelE(ouMe, score, osuGetService);
                QQMsgUtil.sendImage(from, data);
            }
        } catch (Exception e) {
            NowbotApplication.log.error("BP Error: ", e);
            throw new BPException(BPException.Type.BP_Send_Error);
        }
    }
}
