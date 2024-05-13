package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.PPPlus;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.service.PerformancePlusService;
import com.now.nowbot.throwable.ServiceException.PPPlusException;
import com.now.nowbot.util.AsyncMethodExecutor;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.now.nowbot.service.MessageServiceImpl.PPPlusService.PPPlusStatus.*;

@Service("PP_PLUS")
public class PPPlusService implements MessageService<PPPlusService.PPPlusParam> {
    private static final Logger log = LoggerFactory.getLogger(PPPlusService.class);
    @Resource
    OsuUserApiService userApiService;
    @Resource
    OsuScoreApiService scoreApiService;
    @Resource
    OsuBeatmapApiService beatmapApiService;
    @Resource
    BindDao bindDao;
    @Resource
    PerformancePlusService performancePlusService;
    @Resource
    ImageService imageService;

    // T/F: pp+user(pp), F/F: pp+map(pa), F/T: pp+mapvs(pc), T/T pp+uservs(px)
    public record PPPlusParam <T> (boolean isUser, boolean isVs, T me, T other) {}

    enum PPPlusStatus {
        USER, USER_VS, MAP, MAP_VS
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<PPPlusParam> data) throws Throwable {
        var matcher = Instructions.PP_PLUS.matcher(messageText);
        if (! matcher.find()) return false;

        var status = switch (Optional.ofNullable(
                matcher.group("function")
        ).orElse("pp").trim().toLowerCase()) {
            case "pp", "ppp", "pp+", "p+", "ppplus", "plus" -> USER;
            case "px", "ppx", "ppv", "ppvs", "pppvs", "ppplusvs", "plusvs" -> USER_VS;
            case "pa", "ppa", "ppplusmap", "pppmap", "plusmap", "pppm" -> MAP;
            case "pc", "ppc", "ppplusmapvs", "ppplusmapcompare", "plusmapvs", "plusmapcompare", "pppmv" -> MAP_VS;
            default -> throw new RuntimeException("PP+：未知的类型");
        };

        boolean isUser = switch (status) {
            case USER, USER_VS -> true;
            default -> false;
        };

        boolean isVS = switch (status) {
            case MAP_VS, USER_VS -> true;
            default -> false;
        };

        var area1 = matcher.group("area1");
        var area2 = matcher.group("area2");

        try {

            if (StringUtils.hasText(area1)) {
                if (StringUtils.hasText(area2)) {
                    if (isUser) {
                        // px 1v2
                        data.setValue(new PPPlusParam<>(
                                true, true, userApiService.getPlayerInfo(area1.trim()), userApiService.getPlayerInfo(area2.trim())
                        ));
                    } else {
                        // pc 1v2
                        data.setValue(new PPPlusParam<>(
                                false, true, getBeatMap(area1), getBeatMap(area2)
                        ));
                    }
                } else {
                    if (isUser) {
                        if (isVS) {
                            // px 0v1
                            data.setValue(new PPPlusParam<>(
                                    true, true, userApiService.getPlayerInfo(
                                    bindDao.getUserFromQQ(event.getSender().getId())), userApiService.getPlayerInfo(area1.trim())
                            ));
                        } else {
                            // pp 1
                            data.setValue(new PPPlusParam<>(
                                    true, false, userApiService.getPlayerInfo(area1.trim()), null
                            ));
                        }
                    } else {
                        // pa 1
                        data.setValue(new PPPlusParam<>(
                                false, false, beatmapApiService.getBeatMapInfo(Long.parseLong(area1)), null
                        ));
                    }
                }
            } else {
                if (StringUtils.hasText(area2)) {
                    if (isUser) {
                        if (isVS) {
                            // px 0v2
                            data.setValue(new PPPlusParam<>(
                                    true, true, userApiService.getPlayerInfo(
                                    bindDao.getUserFromQQ(event.getSender().getId())), userApiService.getPlayerInfo(area2.trim())
                            ));
                        } else {
                            // pp 2
                            data.setValue(new PPPlusParam<>(
                                    true, false, userApiService.getPlayerInfo(area2.trim()), null
                            ));
                        }
                    } else {
                        // pa 2
                        data.setValue(new PPPlusParam<>(
                                false, false, getBeatMap(area2), null
                        ));
                    }
                } else {
                    // pp 0
                    data.setValue(new PPPlusParam<>(
                            true, false, userApiService.getPlayerInfo(
                            bindDao.getUserFromQQ(event.getSender().getId())), null
                    ));
                }
            }
        } catch (WebClientResponseException e) {
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

        var hashMap = new HashMap<String, Object>(6);

        hashMap.put("isUser", param.isUser);
        hashMap.put("isVs", param.isVs);

        if (param.isUser) {
            OsuUser u1 = (OsuUser) param.me;

            var mePlus = calculateUserPerformancePlus(u1.getUID());

            hashMap.put("me", u1);
            hashMap.put("my", mePlus);

            if (param.isVs) {
                OsuUser u2 = (OsuUser) param.other;
                var otherPlus = calculateUserPerformancePlus(u2.getUID());

                hashMap.put("other", u2);
                hashMap.put("others", otherPlus);
                hashMap.put("mePlus", C8N16O32(mePlus));
                hashMap.put("otherPlus", C8N16O32(otherPlus));
            } else {
                // 单人 pp+，右边会放化学式的进阶指标
                hashMap.put("mePlus", C8N16O32(mePlus));
            }
        } else {
            BeatMap m1 = (BeatMap) param.me;

            // 不支持其他模式
            if (OsuMode.getMode(m1.getMode()) != OsuMode.OSU) {
                throw new PPPlusException(PPPlusException.Type.PL_Function_NotSupported);
            }

            hashMap.put("me", m1);
            hashMap.put("my", getBeatMapPPPlus(m1.getId()));

            if (param.isVs) {
                BeatMap m2 = (BeatMap) param.other;

                // 不支持其他模式
                if (OsuMode.getMode(m2.getMode()) != OsuMode.OSU) {
                    throw new PPPlusException(PPPlusException.Type.PL_Function_NotSupported);
                }

                hashMap.put("other", m2);
                hashMap.put("others", getBeatMapPPPlus(m2.getId()));

            }
        }

        byte[] image;

        try {
            image = imageService.getPanelB3(hashMap);
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

    // 你妈 不用接口的形式写我还真头大了 按道理这个要放在 pp+ 的实现类下面去

    private PPPlus.Stats calculateUserPerformancePlus(long uid) {
        var bps = scoreApiService.getBestPerformance(uid, OsuMode.OSU, 0, 100);
        var ppPlus = performancePlusService.getScorePerformancePlus(bps);

        double aim = 0;
        double jumpAim = 0;
        double flowAim = 0;
        double precision = 0;
        double speed = 0;
        double stamina = 0;
        double accuracy = 0;
        double total = 0;

        List<AsyncMethodExecutor.Supplier<String>> suppliers = new ArrayList<>(7);
        Map<String, List<Double>> ppPlusMap = new ConcurrentHashMap<>(7);

        // 逐个排序
        suppliers.add(() -> {
            var stream = ppPlus.stream();
            ppPlusMap.put("aim", stream
                    .map(p -> p.getPerformance().aim())
                    .sorted(Comparator.reverseOrder())
                    .toList()
            );
            return "aim";
        });
        suppliers.add(() -> {
            var stream = ppPlus.stream();
            ppPlusMap.put("jumpAim", stream
                    .map(p -> p.getPerformance().jumpAim())
                    .sorted(Comparator.reverseOrder())
                    .toList()
            );
            return "jumpAim";
        });
        suppliers.add(() -> {
            var stream = ppPlus.stream();
            ppPlusMap.put("flowAim", stream
                    .map(p -> p.getPerformance().flowAim())
                    .sorted(Comparator.reverseOrder())
                    .toList()
            );
            return "flowAim";
        });
        suppliers.add(() -> {
            var stream = ppPlus.stream();
            ppPlusMap.put("precision", stream
                    .map(p -> p.getPerformance().precision())
                    .sorted(Comparator.reverseOrder())
                    .toList()
            );
            return "precision";
        });
        suppliers.add(() -> {
            var stream = ppPlus.stream();
            ppPlusMap.put("speed", stream
                    .map(p -> p.getPerformance().speed())
                    .sorted(Comparator.reverseOrder())
                    .toList()
            );
            return "speed";
        });
        suppliers.add(() -> {
            var stream = ppPlus.stream();
            ppPlusMap.put("stamina", stream
                    .map(p -> p.getPerformance().stamina())
                    .sorted(Comparator.reverseOrder())
                    .toList()
            );
            return "stamina";
        });
        suppliers.add(() -> {
            var stream = ppPlus.stream();
            ppPlusMap.put("accuracy", stream
                    .map(p -> p.getPerformance().accuracy())
                    .sorted(Comparator.reverseOrder())
                    .toList()
            );
            return "accuracy";
        });

        suppliers.add(() -> {
            var stream = ppPlus.stream();
            ppPlusMap.put("total", stream
                    .map(p -> p.getPerformance().total())
                    .sorted(Comparator.reverseOrder())
                    .toList()
            );
            return "total";
        });

        AsyncMethodExecutor.AsyncSupplier(suppliers);

        // 计算加权和
        double weight = 1d / 0.95d;

        for (int n = 0; n < ppPlus.size(); n++) {
            weight *= 0.95d;

            aim += ppPlusMap.get("aim").get(n) * weight;
            jumpAim += ppPlusMap.get("jumpAim").get(n) * weight;
            flowAim += ppPlusMap.get("flowAim").get(n) * weight;
            precision += ppPlusMap.get("precision").get(n) * weight;
            speed += ppPlusMap.get("speed").get(n) * weight;
            stamina += ppPlusMap.get("stamina").get(n) * weight;
            accuracy += ppPlusMap.get("accuracy").get(n) * weight;
            total += ppPlusMap.get("total").get(n) * weight;
        }

        return new PPPlus.Stats(aim, jumpAim, flowAim, precision, speed, stamina, accuracy, total);
    }

    private Map<String, Object> C8N16O32(PPPlus.Stats difficulty) {
        if (difficulty == null) return null;

        Map<String, Object> hashMap = new HashMap<>();

        int[] jumpArray = {1700, 1975, 2250, 2525, 2800, 3075, 3365, 3800, 4400, 4900, 5900, 6900};
        int[] flowArray = {450, 563, 675, 788, 900, 1013, 1225, 1500, 1825, 2245, 3200, 4400};
        int[] precisionArray = {400, 463, 525, 588, 650, 713, 825, 950, 1350, 1650, 2300, 3050};
        int[] speedArray = {1250, 1363, 1475, 1588, 1700, 1813, 1925, 2200, 2400, 2650, 3100, 3600};
        int[] staminaArray = {1000, 1100, 1200, 1300, 1400, 1500, 1625, 1800, 2000, 2200, 2600, 3050};
        int[] accuracyArray = {900, 1000, 1100, 1200, 1300, 1400, 1500, 1750, 2100, 2550, 3400, 4400};

        double jumpAim = 0;
        double flowAim = 0;
        double precision = 0;
        double speed = 0;
        double stamina = 0;
        double accuracy = 0;

        // 常规指数和进阶指数，进阶指数是以上情况的第二大的值，达标情况的目标是以上第二大值 * 6 - 4，
        double generalIndex;
        double advancedIndex;

        for (int i = 0; i < 12; i++) {
            if (jumpArray[i] > difficulty.jumpAim()) jumpAim = i;
            if (flowArray[i] > difficulty.flowAim()) flowAim = i;
            if (precisionArray[i] > difficulty.precision()) precision = i;
            if (speedArray[i] > difficulty.speed()) speed = i;
            if (staminaArray[i] > difficulty.stamina()) stamina = i;
            if (accuracyArray[i] > difficulty.accuracy()) accuracy = i;
        }

        if (jumpAim == 0) jumpAim = difficulty.jumpAim() / 1700f;
        if (flowAim == 0) flowAim = difficulty.flowAim() / 450f;
        if (precision == 0) precision = difficulty.precision() / 400f;
        if (speed == 0) speed = difficulty.speed() / 1250f;
        if (stamina == 0) stamina = difficulty.stamina() / 1000f;
        if (accuracy == 0) accuracy = difficulty.accuracy() / 900f;

        generalIndex = Math.sqrt(getPiCent(difficulty.jumpAim(), 1300, 1700) + 8d) * (getPiCent(difficulty.flowAim(), 200, 450) + 3) * 10d
                + getPiCent(difficulty.precision(), 200, 400)
                + getPiCent(difficulty.speed(), 950, 1250) * 7d
                + getPiCent(difficulty.speed(), 950, 1250) * 3d
                + getPiCent(difficulty.accuracy(), 600, 1200) * 10d
        ;

        advancedIndex = Stream.of(
                getDetail(difficulty.jumpAim(), jumpAim, jumpArray[0], jumpArray[11]),
                getDetail(difficulty.flowAim(), flowAim, flowArray[0], flowArray[11]),
                getDetail(difficulty.precision(), precision, precisionArray[0], precisionArray[11]),
                getDetail(difficulty.speed(), speed, speedArray[0], speedArray[11]),
                getDetail(difficulty.stamina(), stamina, staminaArray[0], staminaArray[11]),
                getDetail(difficulty.accuracy(), accuracy, accuracyArray[0], accuracyArray[11])
                ).sorted().toList().get(4); // 第二大

        // jump 放在前面
        var index = Arrays.asList(accuracy, jumpAim, flowAim, precision, speed, stamina);
        double sum = index.stream().reduce(Double::sum).orElse(0d);

        hashMap.put("index", index);
        hashMap.put("general", generalIndex);
        hashMap.put("advanced", advancedIndex);
        hashMap.put("sum", sum);
        hashMap.put("approval", advancedIndex * 6 - 4);

        return hashMap;
    }

    // 化学式进阶指数 获取百分比 * Pi（加权 1）
    private double getPiCent(double val, int percent25, int percent75) {
        return (Math.atan((val * 2d - (percent75 + percent25)) / (percent75 - percent25)) / Math.PI + 0.5d) * Math.PI;
    }
    // 化学式进阶指数 获取详细情况（用于进阶指数求和）
    private double getDetail(double val, double level, int percent75, int percentEX) {
        if (val < percent75) return -2;
        else if (val > percentEX) return Math.floor(val / percentEX * 10d) + 1d;
        else return level;
    }

    private BeatMap getBeatMap(String bidStr) throws PPPlusException {
        BeatMap beatMap;

        try {
            beatMap = beatmapApiService.getBeatMapInfo(Long.parseLong(bidStr));
        } catch (WebClientResponseException ignored) {
            try {
                beatMap = beatmapApiService.getBeatMapSetInfo(Long.parseLong(bidStr)).getTopDiff();
                if (Objects.isNull(beatMap)) {
                    throw new PPPlusException(PPPlusException.Type.PL_Map_NotFound);
                }
            } catch (WebClientResponseException e) {
                throw new PPPlusException(PPPlusException.Type.PL_Map_NotFound);
            }
        } catch (NumberFormatException e) {
            throw new PPPlusException(PPPlusException.Type.PL_Map_BIDParseError);
        }

        return beatMap;
    }

    private PPPlus getBeatMapPPPlus(long bid) throws PPPlusException {
        try {
            return performancePlusService.getMapPerformancePlus(bid);
        } catch (RuntimeException e) {
            log.error("PP+：获取失败", e);
            throw new PPPlusException(PPPlusException.Type.PL_Fetch_APIConnectFailed);
        }
    }
}
