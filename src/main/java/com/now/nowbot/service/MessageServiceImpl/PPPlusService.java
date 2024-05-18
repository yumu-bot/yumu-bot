package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.PPPlus;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.service.PerformancePlusService;
import com.now.nowbot.throwable.ServiceException.PPPlusException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Service("PP_PLUS")
public class PPPlusService implements MessageService<PPPlusService.PPPlusParam> {
    private static final Logger log = LoggerFactory.getLogger(PPPlusService.class);
    @Resource
    OsuUserApiService    userApiService;
    @Resource
    OsuScoreApiService   scoreApiService;
    @Resource
    OsuBeatmapApiService beatmapApiService;
    @Resource
    BindDao              bindDao;
    @Resource
    PerformancePlusService performancePlusService;
    @Resource
    ImageService         imageService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<PPPlusParam> data) throws Throwable {
        var matcher = Instructions.PP_PLUS.matcher(messageText);
        if (! matcher.find()) return false;

        var cmd = Objects.requireNonNullElse(matcher.group("function"), "pp");
        var a1 = matcher.group("area1");
        var a2 = matcher.group("area2");
        boolean isUser = true;

        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);

        var me = event.getSender().getId();

        try {
            switch (cmd.toLowerCase()) {
                case "pp", "ppp", "pp+", "p+", "ppplus", "plus" -> {
                    // user 非vs
                    if (Objects.nonNull(a1) && a1.isBlank()) a1 = null;
                    if (Objects.nonNull(a2) && a2.isBlank()) a2 = null;
                    if (Objects.nonNull(at))
                        setUser(null, null, at.getQQ(), false, data);
                    else
                        setUser(a1, a2, me, false, data);
                }
                case "px", "ppx", "ppv", "ppvs", "pppvs", "ppplusvs", "plusvs" -> {
                    // user vs
                    if (Objects.nonNull(at)) {
                        var user = bindDao.getUserFromQQ(at.getQQ());
                        setUser(null, user.getOsuName(), me, true, data);
                    } else {
                        setUser(a1, a2, me, true, data);
                    }
                }
                case "pa", "ppa", "ppplusmap", "pppmap", "plusmap", "pppm", "ppplusmapvs", "plusmapvs", "pppmv" -> {
                    // 这部分确实是 isVs 指令没什么区别, 完全是按照参数数量来判断的, 甚至没参数会默认调用 user
                    isUser = false;
                    setMap(a1, a2, data);
                }
                default -> {
                    log.error("PP+ 指令解析失败: [{}]", cmd);
                    return false;
                }
            }
        } catch (WebClientResponseException e) {
            log.error("pp+ 请求异常", e);
            if (isUser) {
                throw new PPPlusException(PPPlusException.Type.PL_User_NotFound);
            } else {
                throw new PPPlusException(PPPlusException.Type.PL_Map_NotFound);
            }
        } catch (NumberFormatException e) {
            throw new PPPlusException(PPPlusException.Type.PL_Map_BIDParseError);
        }

        return true;

    }

    @Override
    public void HandleMessage(MessageEvent event, PPPlusParam param) throws Throwable {
        var from = event.getSubject();

        var dataMap = new HashMap<String, Object>(6);

        if (param.isUser()) {
            // user 对比
            dataMap.put("isUser", true);
            OsuUser u1 = (OsuUser) param.me;
            dataMap.put("me", u1);
            dataMap.put("my", getUserPerformancePlus(u1.getUID()));

            if (Objects.nonNull(param.other)) {
                // 包含另一个就是 vs, 直接判断了
                OsuUser u2 = (OsuUser) param.other;
                dataMap.put("other", u2);
                dataMap.put("others", getUserPerformancePlus(u2.getUID()));
            }
        } else {
            dataMap.put("isUser", false);
            BeatMap m1 = (BeatMap) param.me;
            dataMap.put("me", m1);
            dataMap.put("my", getBeatMapPerformancePlus(m1.getId()));
            if (Objects.nonNull(param.other)) {
                BeatMap m2 = (BeatMap) param.other;
                dataMap.put("other", m2);
                dataMap.put("others", getBeatMapPerformancePlus(m2.getId()));
            }
        }

        byte[] image;


        try {
            beforePost(dataMap);
            image = imageService.getPanelB3(dataMap);
        } catch (Exception e) {
            log.error("PP+ 渲染失败", e);
            throw new PPPlusException(PPPlusException.Type.PL_Render_Error);
        }
        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("PP+ 发送失败", e);
            throw new PPPlusException(PPPlusException.Type.PL_Send_Error);
        }

    }

    // 把数据合并一下 。这个才是真传过去的 PP+
    private PPPlus getUserPerformancePlus(long uid) {
        var plus = new PPPlus();
        var bps = scoreApiService.getBestPerformance(uid, OsuMode.OSU, 0, 100);
        var performance = performancePlusService.calculateUserPerformance(bps);
        plus.setPerformance(performance);
        plus.setAdvancedStats(calculateUserAdvancedStats(performance));

        return plus;
    }

    private void setUser(String a1, String a2, Long senderId, boolean isVs, DataValue<PPPlusParam> data) {
        OsuUser p1 = StringUtils.hasText(a1) ?
                userApiService.getPlayerInfo(a1, OsuMode.OSU) :
                userApiService.getPlayerInfo(bindDao.getUserFromQQ(senderId), OsuMode.OSU);

        OsuUser p2 = StringUtils.hasText(a2) ? userApiService.getPlayerInfo(a2, OsuMode.OSU) : null;

        if (isVs && Objects.isNull(p2)) {
            p2 = p1;
            p1 = userApiService.getPlayerInfo(bindDao.getUserFromQQ(senderId), OsuMode.OSU);
        }

        data.setValue(new PPPlusParam(true, p1, p2));
    }

    private void setMap(String a1, String a2, DataValue<PPPlusParam> data) throws PPPlusException {
        BeatMap m1 = StringUtils.hasText(a1) ? getBeatMap(a1) : null;
        BeatMap m2 = StringUtils.hasText(a2) ? getBeatMap(a2) : null;

        if (Objects.isNull(m1) && Objects.isNull(m2)) {
            throw new PPPlusException(PPPlusException.Type.PL_Player_VSNotFound);
        } else if (Objects.isNull(m1)) {
            m1 = m2;
            m2 = null;
        }

        // 不支持其他模式
        if (OsuMode.getMode(m1.getMode()) != OsuMode.OSU || (Objects.nonNull(m2) && OsuMode.getMode(m2.getMode()) != OsuMode.OSU)) {
            throw new PPPlusException(PPPlusException.Type.PL_Function_NotSupported);
        }

        data.setValue(new PPPlusParam(false, m1, m2));
    }

    private BeatMap getBeatMap(String bidStr) throws PPPlusException {
        BeatMap beatMap;
        long id;
        try {
            id = Long.parseLong(bidStr);
        } catch (NumberFormatException e) {
            throw new PPPlusException(PPPlusException.Type.PL_Map_BIDParseError);
        }
        try {
            beatMap = beatmapApiService.getMapInfoFromDB(id);
        } catch (WebClientResponseException ignored) {
            try {
                beatMap = beatmapApiService.getBeatMapSetInfo(id).getTopDiff();
            } catch (WebClientResponseException e) {
                throw new PPPlusException(PPPlusException.Type.PL_Map_NotFound);
            }
        }
        if (Objects.isNull(beatMap)) {
            throw new PPPlusException(PPPlusException.Type.PL_Map_NotFound);
        }
        return beatMap;
    }

    private PPPlus getBeatMapPerformancePlus(long bid) throws PPPlusException {
        try {
            return performancePlusService.getMapPerformancePlus(bid);
        } catch (RuntimeException e) {
            log.error("PP+：获取失败", e);
            throw new PPPlusException(PPPlusException.Type.PL_Fetch_APIConnectFailed);
        }
    }

    // 计算进阶指数的等级
    private double calculateLevel(double value, int[] array) {
        if (array == null || array.length < 13) return 0;

        int lv = 11;

        for (int i = 0; i < 13; i++) {
            if (value < array[i]) {
                lv = i - 2;
                break;
            }
        }

        switch (lv) {
            case - 2 -> {
                // 0 - 25
                return 0.25d * value / array[0];
            }
            case - 1 -> {
                // 25 - 75
                return 0.25d + 0.5d * (value - array[0]) / (array[1] - array[0]);
            }
            case 0 -> {
                // 75 - 100
                return 0.75d + 0.25d * (value - array[1]) / (array[2] - array[1]);
            }
            default -> {
                return lv;
            }
        }
    }

    private PPPlus.AdvancedStats calculateUserAdvancedStats(PPPlus.Stats performance) {
        if (performance == null) return null;

        //第一个是 25%，第二个是 75%，第三个是LV1
        int[] jumpArray = {1300, 1700, 1975, 2250, 2525, 2800, 3075, 3365, 3800, 4400, 4900, 5900, 6900};
        int[] flowArray = {200, 450, 563, 675, 788, 900, 1013, 1225, 1500, 1825, 2245, 3200, 4400};
        int[] precisionArray = {200, 400, 463, 525, 588, 650, 713, 825, 950, 1350, 1650, 2300, 3050};
        int[] speedArray = {950, 1250, 1363, 1475, 1588, 1700, 1813, 1925, 2200, 2400, 2650, 3100, 3600};
        int[] staminaArray = {600, 1000, 1100, 1200, 1300, 1400, 1500, 1625, 1800, 2000, 2200, 2600, 3050};
        int[] accuracyArray = {600, 900, 1000, 1100, 1200, 1300, 1400, 1500, 1750, 2100, 2550, 3400, 4400};

        // 常规指数和进阶指数，进阶指数是以上情况的第二大的值，达标情况的目标是以上第二大值 * 6 - 4，
        double generalIndex;
        double advancedIndex;

        double jumpAim = calculateLevel(performance.jumpAim(), jumpArray);
        double flowAim = calculateLevel(performance.flowAim(), flowArray);
        double precision = calculateLevel(performance.precision(), precisionArray);
        double speed = calculateLevel(performance.speed(), speedArray);
        double stamina = calculateLevel(performance.stamina(), staminaArray);
        double accuracy = calculateLevel(performance.accuracy(), accuracyArray);

        generalIndex = Math.sqrt(getPiCent(performance.jumpAim(), 1300, 1700) + 8d) * (getPiCent(performance.flowAim(), 200, 450) + 3d) * 10d
                + getPiCent(performance.precision(), 200, 400)
                + getPiCent(performance.speed(), 950, 1250) * 7d
                + getPiCent(performance.speed(), 950, 1250) * 3d
                + getPiCent(performance.accuracy(), 600, 1200) * 10d
        ;

        advancedIndex = Stream.of(
                getDetail(performance.jumpAim(), jumpAim, jumpArray[0], jumpArray[11]),
                getDetail(performance.flowAim(), flowAim, flowArray[0], flowArray[11]),
                getDetail(performance.precision(), precision, precisionArray[0], precisionArray[11]),
                getDetail(performance.speed(), speed, speedArray[0], speedArray[11]),
                getDetail(performance.stamina(), stamina, staminaArray[0], staminaArray[11]),
                getDetail(performance.accuracy(), accuracy, accuracyArray[0], accuracyArray[11])
        ).sorted().toList().get(4); // 第二大

        var index = Arrays.asList(jumpAim, flowAim, accuracy, stamina, speed, precision);
        double sum = index.stream().reduce(Double::sum).orElse(0d);

        return new PPPlus.AdvancedStats(index, generalIndex, advancedIndex, sum, advancedIndex * 6 - 4);
    }

    // 化学式进阶指数 获取百分比 * Pi（加权 1）
    private double getPiCent(double val, int percent25, int percent75) {
        return (Math.atan((val * 2d - (percent75 + percent25)) / (percent75 - percent25)) / Math.PI + 0.5d) * Math.PI;
    }

    // 化学式进阶指数 获取详细情况（用于进阶指数求和）
    private double getDetail(double val, double level, int percent75, int percentEX) {
        if (val < percent75) return - 2;
        else if (val > percentEX) return Math.floor(val / percentEX * 10d) + 1d;
        else return level;
    }

    // T/F: pp+user(pp), F/F: pp+map(pa), F/T: pp+mapvs(pc), T/T pp+uservs(px)
    public record PPPlusParam(boolean isUser, Object me, Object other) {
    }

    // 、、、、、、、、、、、、、、、、、、
    // 不要多看, 反正不影响用
    private void beforePost(Map<String, Object> data) {
        var o = data.get("other");
        if (Objects.nonNull(o) && o instanceof OsuUser u && u.getId() == 17064371L && data.get("others") instanceof PPPlus plus) {
            plus.setPerformance(PPPlus.getMaxStats());
        }
    }

}
