package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.Service.UserParam;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.imag.MapAttr;
import com.now.nowbot.model.imag.MapAttrGet;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BPAnalysisException;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("BP_ANALYSIS")
public class BPAnalysisService implements MessageService<UserParam> {
    private static final Logger log = LoggerFactory.getLogger(BPAnalysisService.class);
    private static final String[] RANK_ARRAY = new String[]{"XH", "X", "SSH", "SS", "SH", "S", "A", "B", "C", "D", "F"};
    OsuUserApiService userApiService;
    OsuScoreApiService scoreApiService;
    BindDao bindDao;
    ImageService imageService;
    @Resource
    UUBAService uubaService;

    @Autowired
    public BPAnalysisService(OsuUserApiService userApiService, OsuScoreApiService scoreApiService, BindDao bindDao, ImageService imageService) {
        this.userApiService = userApiService;
        this.scoreApiService = scoreApiService;
        this.bindDao = bindDao;
        this.imageService = imageService;
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<UserParam> data) {
        var matcher = Instructions.BP_ANALYSIS.matcher(messageText);
        if (!matcher.find()) return false;
        var mode = OsuMode.getMode(matcher.group("mode"));
        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        if (Objects.nonNull(at)) {
            data.setValue(new UserParam(at.getTarget(), null, mode, true));
            return true;
        }
        String name = matcher.group("name");
        if (Objects.nonNull(name) && Strings.isNotBlank(name)) {
            data.setValue(new UserParam(null, name, mode, false));
            return true;
        }
        data.setValue(new UserParam(event.getSender().getId(), null, mode, false));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, UserParam param) throws Throwable {
        var from = event.getSubject();
        var mode = param.mode();

        //bp列表
        List<Score> bps;
        OsuUser u;
        if (Objects.nonNull(param.qq())) {
            BinUser bu = bindDao.getUserFromQQ(param.qq());
            try {
                if (mode != OsuMode.DEFAULT) {
                    u = userApiService.getPlayerInfo(bu, mode);
                    u.setPlayMode(mode.getName());
                    bps = scoreApiService.getBestPerformance(bu, mode, 0, 100);
                } else {
                    bps = scoreApiService.getBestPerformance(bu, bu.getMode(), 0, 100);
                    u = userApiService.getPlayerInfo(bu, bu.getMode());
                }
            } catch (Exception e) {
                if (!param.at()) {
                    throw new BPAnalysisException(BPAnalysisException.Type.BPA_Me_FetchFailed);
                } else {
                    throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_FetchFailed);
                }
            }
        } else {
            String name = param.name().trim();
            long id;
            try {
                id = userApiService.getOsuId(name);
            } catch (Exception e) {
                throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_NotFound);
            }
            try {
                if (mode != OsuMode.DEFAULT) {
                    bps = scoreApiService.getBestPerformance(id, mode, 0, 100);
                    u = userApiService.getPlayerInfo(id, mode);
                    u.setPlayMode(mode.getName());
                } else {
                    u = userApiService.getPlayerInfo(id);
                    bps = scoreApiService.getBestPerformance(id, u.getOsuMode(), 0, 100);
                }
            } catch (Exception e) {
                throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_FetchFailed);
            }
        }

        if (Objects.isNull(bps) || bps.size() <= 5) {
            if (Objects.nonNull(param.qq()) && param.qq() == event.getSender().getId()) {
                throw new BPAnalysisException(BPAnalysisException.Type.BPA_Me_NotEnoughBP, u.getPlayMode());
            } else {
                throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_NotEnoughBP, u.getPlayMode());
            }
        }

        byte[] image = new byte[0];

        try {
            var data = parseData(u, bps, userApiService);
            image = imageService.getPanelJ(data);
        } catch (HttpServerErrorException.InternalServerError e) {
            try {
                var msg = uubaService.getAllMsg(bps, u.getUsername(), u.getPlayMode());
                var image2 = imageService.getPanelAlpha(msg);

                from.sendImage(image2);
            } catch (Exception e1) {
                log.error("BPA Error (to UUBA): ", e1);
                throw new BPAnalysisException(BPAnalysisException.Type.BPA_Send_Error);
            }

        } catch (Exception e) {
            log.error("BPA Error (other than HTTP 500): ", e);
            throw new BPAnalysisException(BPAnalysisException.Type.BPA_Send_Error);
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            throw new BPAnalysisException(BPAnalysisException.Type.BPA_Send_Error);
        }
    }

    public Map<String, Object> parseData(OsuUser user, List<Score> bps, OsuUserApiService userApiService) {
        var bpSize = bps.size();
        // top
        var t5 = bps.subList(0, Math.min(bpSize, 5));
        var b5 = bps.subList(Math.max(bpSize - 5, 0), bpSize);

        // 提取星级变化的谱面 DT/HT 等
        var mapAttrGet = new MapAttrGet(user.getOsuMode());
        bps.stream()
                .filter(s -> Mod.hasChangeRating(Mod.getModsValueFromStr(s.getMods())))
                .forEach(s -> mapAttrGet.addMap(s.getScoreId(), s.getBeatMap().getId(), Mod.getModsValueFromStr(s.getMods())));
        Map<Long, MapAttr> changedAttrsMap;
        if (CollectionUtils.isEmpty(mapAttrGet.getMaps())) {
            changedAttrsMap = null;
        } else {
            changedAttrsMap = imageService.getMapAttr(mapAttrGet);
        }

        record map(int ranking, int length, int combo, float bpm, float star, String rank, String cover,
                   String[] mods) {
        }

        record attr(String index, int map_count, float pp_count, float percent) {
        }

        List<map> mapList = new ArrayList<>(bpSize);
        MultiValueMap<String, Float> modsPPSum = new LinkedMultiValueMap<>();
        MultiValueMap<String, Float> rankSum = new LinkedMultiValueMap<>();
        int modsSum = 0;
        for (int i = 0; i < bpSize; i++) {
            var s = bps.get(i);
            {// 处理 mapList
                var b = s.getBeatMap();
                if (!CollectionUtils.isEmpty(changedAttrsMap) && changedAttrsMap.containsKey(s.getScoreId())) {
                    var attr = changedAttrsMap.get(s.getScoreId());
                    b.setStarRating(attr.getStars());
                    b.setBPM(attr.getBpm());
                    if (s.getMods().contains("DT") || s.getMods().contains("NC")) {
                        b.setTotalLength(Math.round(b.getTotalLength() / 1.5f));
                    } else if (s.getMods().stream().anyMatch(r -> r.equals("HT"))) {
                        b.setTotalLength(Math.round(b.getTotalLength() / 0.75f));
                    }
                }
                var m = new map(
                        i + 1,
                        b.getTotalLength(),
                        s.getMaxCombo(),
                        b.getBPM(),
                        b.getStarRating(),
                        s.getRank(),
                        s.getBeatMapSet().getCovers().getList2x(),
                        s.getMods().toArray(new String[0])
                );
                mapList.add(m);
            }

            { // 统计 mods / rank
                if (!CollectionUtils.isEmpty(s.getMods())) {
                    s.getMods().forEach(m -> modsPPSum.add(m, s.getWeight().getPP()));
                    modsSum += s.getMods().size();
                } else {
//                    modsPPSum.add("NM", s.getWeight().getPP());
                    modsSum += 1;
                }
                if (s.isPerfect()) {
                    rankSum.add("FC", s.getWeight().getPP());
                }
                rankSum.add(s.getRank(), s.getWeight().getPP());
            }
        }
        // 0 length; 1 combo; 2 star; 3 bpm
        ArrayList<map>[] mapStatistics = new ArrayList[4];
        var bpListSortedByLength = mapList.stream().sorted(Comparator.comparingInt(map::length).reversed()).toList();
        mapStatistics[0] = new ArrayList<>(3);
        mapStatistics[0].add(bpListSortedByLength.getFirst());
        mapStatistics[0].add(bpListSortedByLength.get(bpSize / 2));
        mapStatistics[0].add(bpListSortedByLength.get(bpSize - 1));

        var bpListSortedByCombo = mapList.stream().sorted(Comparator.comparing(map::combo).reversed()).toList();
        mapStatistics[1] = new ArrayList<>(3);
        mapStatistics[1].add(bpListSortedByCombo.getFirst());
        mapStatistics[1].add(bpListSortedByCombo.get(bpSize / 2));
        mapStatistics[1].add(bpListSortedByCombo.get(bpSize - 1));

        var bpListSortedByStar = mapList.stream().sorted(Comparator.comparing(map::star).reversed()).toList();
        mapStatistics[2] = new ArrayList<>(3);
        mapStatistics[2].add(bpListSortedByStar.getFirst());
        mapStatistics[2].add(bpListSortedByStar.get(bpSize / 2));
        mapStatistics[2].add(bpListSortedByStar.get(bpSize - 1));

        var bpListSortedByBpm = mapList.stream().sorted(Comparator.comparing(map::bpm).reversed()).toList();
        mapStatistics[3] = new ArrayList<>(3);
        mapStatistics[3].add(bpListSortedByBpm.getFirst());
        mapStatistics[3].add(bpListSortedByBpm.get(bpSize / 2));
        mapStatistics[3].add(bpListSortedByBpm.get(bpSize - 1));

        //        var ppList = bps.stream().map(s -> s.getWeight().getPP());
        var ppRawList = bps.stream().map(Score::getPP).toList();
        var rankCount = bps.stream()
                .map(Score::getRank)
                .toList();
        var rankSort = rankCount.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted((v1, v2) -> v2.getValue().compareTo(v1.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        record mapper(String avatar_url, String username, Integer map_count, Float pp_count) {
        }
        var bpMapperMap = bps.stream()
                .collect(Collectors.groupingBy(s -> s.getBeatMap().getMapperID(), Collectors.counting()));
        int mappers = bpMapperMap.size();
        var mapperCount = bpMapperMap
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(8)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o, e) -> o, LinkedHashMap::new));
        var mapperInfo = userApiService.getUsers(mapperCount.keySet());
        var mapperList = bps.stream()
                .filter(s -> mapperCount.containsKey(s.getBeatMap().getMapperID()))
//                .collect(Collectors.groupingBy(s -> s.getBeatMap().getUserId(), Collectors.summingDouble(s -> s.getWeight().getPP())))
                .collect(Collectors.groupingBy(s -> s.getBeatMap().getMapperID(), Collectors.summingDouble(Score::getPP)))
                .entrySet()
                .stream()
                .sorted(Comparator.<Map.Entry<Long, Double>, Long>comparing(e -> mapperCount.get(e.getKey())).reversed().thenComparing(Map.Entry::getValue, Comparator.reverseOrder()))
                .map(e -> {
                    String name = "";
                    String avatar = "";
                    for (var node : mapperInfo) {
                        if (e.getKey().equals(node.getId())) {
                            name = node.getUserName();
                            avatar = node.getAvatarUrl();
                            break;
                        }
                    }
                    return new mapper(avatar, name, mapperCount.get(e.getKey()).intValue(), e.getValue().floatValue());
                })
                .toList();

        var bpPPs = bps.stream().mapToDouble(Score::getPP).toArray();

        var userPP = user.getPP();
        var bonusPP = DataUtil.getBonusPP(userPP, bpPPs);

        //bpPP + remainPP (bp100之后的) = rawPP
        var bpPP = (float) bps.stream().mapToDouble(s -> s.getWeight().getPP()).sum();
        var rawPP = (float) (userPP - bonusPP);

        List<attr> modsAttr;
        {
            final int m = modsSum;
            List<attr> modsAttrTmp = new ArrayList<>(modsPPSum.size());
            modsPPSum.forEach((mod, value) -> {
                attr attr = new attr(mod, value.size(), value.stream().reduce(Float::sum).orElse(0F), (1F * value.size() / m));
                modsAttrTmp.add(attr);
            });
            modsAttr = modsAttrTmp.stream().sorted(Comparator.comparingDouble(attr::pp_count).reversed()).toList();
        }

        List<attr> rankAttr = new ArrayList<>(rankSum.size());
        {
            var fcList = rankSum.remove("FC");
            attr fc;
            if (CollectionUtils.isEmpty(fcList)) {
                fc = new attr("FC", 0, 0, 0);
            } else {
                float ppSum = fcList.stream().reduce(Float::sum).orElse(0F);
                fc = new attr("FC", fcList.size(), ppSum, (ppSum / bpPP));
            }
            rankAttr.add(fc);
            for (var rank : RANK_ARRAY) {
                if (rankSum.containsKey(rank)) {
                    var value = rankSum.get(rank);
                    float ppSum = 0f;
                    if (value != null) {
                        ppSum = value.stream().reduce(Float::sum).orElse(0F);
                    }
                    attr attr = null;
                    if (value != null) {
                        attr = new attr(rank, value.size(), ppSum, (ppSum / bpPP));
                    }
                    rankAttr.add(attr);
                }
            }
        }
        if (changedAttrsMap != null) {
            Consumer<Score> f = (s) -> {
                long id = s.getBeatMap().getId();
                if (changedAttrsMap.containsKey(id)) {
                    var attr = changedAttrsMap.get(id);
                    s.getBeatMap().setStarRating(attr.getStars());
                    s.getBeatMap().setBPM(attr.getBpm());
                    if (Mod.hasDt(attr.getMods())) {
                        s.getBeatMap().setTotalLength(Math.round(s.getBeatMap().getTotalLength() / 1.5f));
                    } else if (Mod.hasHt(attr.getMods())) {
                        s.getBeatMap().setTotalLength(Math.round(s.getBeatMap().getTotalLength() / 0.75f));
                    }
                }
            };
            b5.forEach(f);
            t5.forEach(f);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("card_A1", user);
        data.put("bpTop5", t5);
        data.put("bpLast5", b5);
        data.put("bpLength", mapStatistics[0]);
        data.put("bpCombo", mapStatistics[1]);
        data.put("bpSR", mapStatistics[2]);
        data.put("bpBpm", mapStatistics[3]);
        data.put("favorite_mappers_count", mappers);
        data.put("favorite_mappers", mapperList);
        data.put("pp_raw_arr", ppRawList);
        data.put("rank_arr", rankCount);
        data.put("rank_elect_arr", rankSort);
        data.put("bp_length_arr", mapList.stream().map(map::length).toList());
        data.put("mods_attr", modsAttr);
        data.put("rank_attr", rankAttr);
        data.put("pp_raw", rawPP);
        data.put("pp", userPP);
        data.put("game_mode", bps.getFirst().getMode());

        return data;
    }

}
