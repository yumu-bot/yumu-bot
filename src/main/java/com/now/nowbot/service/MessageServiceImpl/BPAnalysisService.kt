package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BPAnalysisException;
import com.now.nowbot.util.CmdUtil;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instruction;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("BP_ANALYSIS")
public class BPAnalysisService implements MessageService<BPAnalysisService.BAParam> {
    private static final Logger   log        = LoggerFactory.getLogger(BPAnalysisService.class);
    private static final String[] RANK_ARRAY = new String[]{"XH", "X", "SSH", "SS", "SH", "S", "A", "B", "C", "D", "F"};
    @Resource
    OsuScoreApiService   scoreApiService;
    @Resource
    OsuUserApiService    userApiService;
    @Resource
    ImageService         imageService;
    @Resource
    UUBAService          uubaService;
    @Resource
    OsuBeatmapApiService beatmapApiService;

    public record BAParam(OsuUser user, List<Score> bpList, boolean isMyself) {
    }


    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<BAParam> data) throws Throwable {
        var matcher = Instruction.BP_ANALYSIS.matcher(messageText);
        if (!matcher.find()) return false;

        var isMyself = new AtomicBoolean();

        var mode = CmdUtil.getMode(matcher);
        var user = CmdUtil.getUserWithOutRange(event, matcher, mode, isMyself);

        if (Objects.isNull(user)) {
            return false;
        }

        var bpList = scoreApiService.getBestPerformance(user.getUserID(), mode.getData(), 0, 100);

        data.setValue(new BAParam(user, bpList, isMyself.get()));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, BAParam param) throws Throwable {
        var from = event.getSubject();

        var bpList = param.bpList();
        var user = param.user();

        if (CollectionUtils.isEmpty(bpList) || bpList.size() <= 5) {
            if (param.isMyself) {
                throw new BPAnalysisException(BPAnalysisException.Type.BA_Me_NotEnoughBP, user.getMode());
            } else {
                throw new BPAnalysisException(BPAnalysisException.Type.BA_Player_NotEnoughBP, user.getMode());
            }
        }

        // 提取星级变化的谱面 DT/HT 等
        beatmapApiService.applySRAndPP(bpList);

        byte[] image;

        var data = parseData(user, bpList, userApiService);

        try {
            image = imageService.getPanelJ(data);
        } catch (HttpServerErrorException.InternalServerError e) {
            log.error("最好成绩分析：复杂面板生成失败", e);
            try {
                var msg = uubaService.getAllMsg(bpList, user.getUsername(), user.getMode());
                var image2 = imageService.getPanelAlpha(msg);
                from.sendImage(image2);
                return;

            } catch (ResourceAccessException | HttpServerErrorException.InternalServerError e1) {
                log.error("最好成绩分析：渲染失败", e1);
                throw new BPAnalysisException(BPAnalysisException.Type.BA_Render_Error);
            } catch (Exception e1) {
                log.error("最好成绩分析：UUBA 转换失败", e1);
                throw new BPAnalysisException(BPAnalysisException.Type.BA_Send_UUError);
            }

        } catch (Exception e) {
            log.error("最好成绩分析：渲染失败", e);
            throw new BPAnalysisException(BPAnalysisException.Type.BA_Render_Error);
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("最好成绩分析：发送失败", e);
            throw new BPAnalysisException(BPAnalysisException.Type.BA_Send_Error);
        }
    }

    public static Map<String, Object> parseData(OsuUser user, List<Score> bpList, OsuUserApiService userApiService) {
        if (bpList == null || bpList.size() <= 5) return HashMap.newHashMap(1);

        var bps = new ArrayList<>(bpList);

        var bpSize = bps.size();

        // top
        var t5 = bps.subList(0, 5);
        var b5 = bps.subList(Math.max(bpSize - 5, 0), bpSize);

        record BeatMap4BA(int ranking, int length, int combo, float bpm, float star, String rank, String cover,
                          String[] mods) {
        }

        record Attr(String index, int map_count, float pp_count, float percent) {
        }

        List<BeatMap4BA> beatMapList = new ArrayList<>(bpSize);
        MultiValueMap<String, Float> modsPPMap = new LinkedMultiValueMap<>();
        MultiValueMap<String, Float> rankMap = new LinkedMultiValueMap<>();

        int modsSum = 0;

        for (int i = 0; i < bpSize; i++) {
            var s = bps.get(i);
            var b = s.getBeatMap();

            {
                var m = new BeatMap4BA(
                        i + 1,
                        b.getTotalLength(),
                        s.getMaxCombo(),
                        b.getBPM(),
                        b.getStarRating(),
                        s.getRank(),
                        s.getBeatMapSet().getCovers().getList(),
                        s.getMods().toArray(new String[0])
                );

                beatMapList.add(m);
            }

            { // 统计 mods / rank
                if (!CollectionUtils.isEmpty(s.getMods())) {
                    s.getMods().forEach(m -> modsPPMap.add(m, s.getWeightedPP()));
                    modsSum += s.getMods().size();
                } else {
//                    modsPPSum.add("NM", s.getWeight().getPP());
                    modsSum += 1;
                }
                if (s.isPerfect()) {
                    rankMap.add("FC", s.getWeightedPP());
                }
                rankMap.add(s.getRank(), s.getWeightedPP());
            }
        }

        // 0 length; 1 combo; 2 star; 3 bpm
        var summary = new HashMap<String, List<BeatMap4BA>>(4);

        var lengthSort = beatMapList.stream().sorted(Comparator.comparingInt(BeatMap4BA::length).reversed()).toList();
        var lengthStat = new ArrayList<BeatMap4BA>(3);
        lengthStat.add(lengthSort.getFirst());
        lengthStat.add(lengthSort.get(bpSize / 2));
        lengthStat.add(lengthSort.get(bpSize - 1));
        summary.put("length", lengthStat);

        var comboSort = beatMapList.stream().sorted(Comparator.comparing(BeatMap4BA::combo).reversed()).toList();
        var comboStat = new ArrayList<BeatMap4BA>(3);
        comboStat.add(comboSort.getFirst());
        comboStat.add(comboSort.get(bpSize / 2));
        comboStat.add(comboSort.get(bpSize - 1));
        summary.put("combo", comboStat);

        var starSort = beatMapList.stream().sorted(Comparator.comparing(BeatMap4BA::star).reversed()).toList();
        var starStat = new ArrayList<BeatMap4BA>(3);
        starStat.add(starSort.getFirst());
        starStat.add(starSort.get(bpSize / 2));
        starStat.add(starSort.get(bpSize - 1));
        summary.put("star", starStat);

        var bpmSort = beatMapList.stream().sorted(Comparator.comparing(BeatMap4BA::bpm).reversed()).toList();
        var bpmStat = new ArrayList<BeatMap4BA>(3);
        bpmStat.add(bpmSort.getFirst());
        bpmStat.add(bpmSort.get(bpSize / 2));
        bpmStat.add(bpmSort.get(bpSize - 1));
        summary.put("bpm", bpmStat);

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
        record Mapper(String avatar_url, String username, Integer map_count, Float pp_count) {

        }

        var mapperMap = bps.stream()
                .collect(Collectors.groupingBy(s -> s.getBeatMap().getMapperID(), Collectors.counting()));
        int mapperSize = mapperMap.size();
        var mapperCount = mapperMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(8)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k, v) -> k, LinkedHashMap::new));
        var mapperInfo = userApiService.getUsers(mapperCount.keySet());
        var mapperList = bps.stream()
                .filter(s -> mapperCount.containsKey(s.getBeatMap().getMapperID()))
                .collect(Collectors.groupingBy(s -> s.getBeatMap().getMapperID(), Collectors.summingDouble(Score::getPP)))
                .entrySet().stream()
                .sorted(Comparator.<Map.Entry<Long, Double>, Long>comparing(e -> mapperCount.get(e.getKey())).reversed().thenComparing(Map.Entry::getValue, Comparator.reverseOrder()))
                .map(e -> {
                    String name = "";
                    String avatar = "";
                    for (var node : mapperInfo) {
                        if (e.getKey().equals(node.getUserID())) {
                            name = node.getUserName();
                            avatar = node.getAvatarUrl();
                            break;
                        }
                    }
                    return new Mapper(avatar, name, mapperCount.get(e.getKey()).intValue(), e.getValue().floatValue());
                })
                .toList();

        var bpPPs = bps.stream().mapToDouble(Score::getPP).toArray();

        var userPP = user.getPP();
        var bonusPP = DataUtil.getBonusPP(userPP, bpPPs);

        //bpPP + remainPP (bp100之后的) = rawPP
        var bpPP = (float) bps.stream().mapToDouble(s -> Optional.ofNullable(s.getWeightedPP()).orElse(0f)).sum();
        var rawPP = (float) (userPP - bonusPP);

        List<Attr> modsAttr;
        {
            final int m = modsSum;
            List<Attr> modsAttrTmp = new ArrayList<>(modsPPMap.size());
            modsPPMap.forEach((mod, value) -> {

                Attr attr = new Attr(mod,
                        value.stream().filter(Objects::nonNull).toList().size(),
                        value.stream().filter(Objects::nonNull).reduce(Float::sum).orElse(0F),
                        (1F * value.size() / m));
                modsAttrTmp.add(attr);
            });
            modsAttr = modsAttrTmp.stream().sorted(Comparator.comparingDouble(Attr::pp_count).reversed()).toList();
        }

        List<Attr> rankAttr = new ArrayList<>(rankMap.size());
        {
            var fcList = rankMap.remove("FC");
            Attr fc;
            if (CollectionUtils.isEmpty(fcList)) {
                fc = new Attr("FC", 0, 0, 0);
            } else {
                float ppSum = fcList.stream().reduce(Float::sum).orElse(0F);
                fc = new Attr("FC", fcList.size(), ppSum, (ppSum / bpPP));
            }
            rankAttr.add(fc);
            for (var rank : RANK_ARRAY) {
                if (rankMap.containsKey(rank)) {
                    var value = rankMap.get(rank);
                    float ppSum;
                    Attr attr = null;
                    if (Objects.nonNull(value) && !value.isEmpty()) {
                        ppSum = value.stream().filter(Objects::nonNull).reduce(Float::sum).orElse(0F);
                        attr = new Attr(rank,
                                value.stream().filter(Objects::nonNull).toList().size(),
                                ppSum, (ppSum / bpPP));
                    }
                    rankAttr.add(attr);
                }
            }
        }

        var data = new HashMap<String, Object>(18);

        data.put("card_A1", user);
        data.put("bpTop5", t5);
        data.put("bpLast5", b5);
        data.put("bpLength", summary.get("length"));
        data.put("bpCombo", summary.get("combo"));
        data.put("bpSR", summary.get("star"));
        data.put("bpBpm", summary.get("bpm"));
        data.put("favorite_mappers_count", mapperSize);
        data.put("favorite_mappers", mapperList);
        data.put("pp_raw_arr", ppRawList);
        data.put("rank_arr", rankCount);
        data.put("rank_elect_arr", rankSort);
        data.put("bp_length_arr", beatMapList.stream().map(BeatMap4BA::length).toList());
        data.put("mods_attr", modsAttr);
        data.put("rank_attr", rankAttr);
        data.put("pp_raw", rawPP);
        data.put("pp", userPP);
        data.put("game_mode", bps.getFirst().getMode());

        return data;
    }
}
