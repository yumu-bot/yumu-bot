package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.imag.MapAttr;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BPAnalysisException;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("BP_ANALYSIS")
public class BPAnalysisService implements MessageService<BPAnalysisService.BAParam> {
    private static final Logger log = LoggerFactory.getLogger(BPAnalysisService.class);
    private static final String[] RANK_ARRAY = new String[]{"XH", "X", "SSH", "SS", "SH", "S", "A", "B", "C", "D", "F"};
    @Resource
    OsuUserApiService userApiService;
    @Resource
    OsuScoreApiService scoreApiService;
    @Resource
    BindDao bindDao;
    @Resource
    ImageService imageService;
    @Resource
    UUBAService uubaService;

    public record BAParam(Long qq, String name, OsuMode mode, boolean at, boolean isMyself) {
    }


    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<BAParam> data) {
        var matcher = Instructions.BP_ANALYSIS.matcher(messageText);
        if (!matcher.find()) return false;

        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        var mode = OsuMode.getMode(matcher.group("mode"));
        var name = matcher.group("name");
        var qqStr = matcher.group("qq");

        if (Objects.nonNull(at)) {
            data.setValue(new BAParam(at.getTarget(), null, mode, true, false));
            return true;
        } else if (StringUtils.hasText(name)) {
            data.setValue(new BAParam(null, name.trim(), mode, false, false));
            return true;
        } else if (StringUtils.hasText(qqStr)) {
            data.setValue(new BAParam(Long.parseLong(qqStr), null, mode, false, false));
            return true;
        } else {
            data.setValue(new BAParam(event.getSender().getId(), null, mode, false, true));
            return true;
        }
    }

    @Override
    public void HandleMessage(MessageEvent event, BAParam param) throws Throwable {
        var from = event.getSubject();
        var mode = param.mode();

        //bp列表
        List<Score> bps;
        OsuUser osuUser;
        if (Objects.nonNull(param.qq)) {
            BinUser binUser = bindDao.getUserFromQQ(param.qq);
            try {
                if (mode != OsuMode.DEFAULT) {
                    osuUser = userApiService.getPlayerInfo(binUser, mode);
                    osuUser.setPlayMode(mode.getName());
                    bps = scoreApiService.getBestPerformance(binUser, mode, 0, 100);
                } else {
                    bps = scoreApiService.getBestPerformance(binUser, binUser.getMode(), 0, 100);
                    osuUser = userApiService.getPlayerInfo(binUser, binUser.getMode());
                }
            } catch (HttpClientErrorException | WebClientResponseException e) {
                if (param.isMyself) {
                    throw new BPAnalysisException(BPAnalysisException.Type.BA_Me_TokenExpired);
                } else {
                    throw new BPAnalysisException(BPAnalysisException.Type.BA_Player_TokenExpired);
                }
            } catch (Exception e) {
                if (param.isMyself) {
                    throw new BPAnalysisException(BPAnalysisException.Type.BA_Me_FetchFailed);
                } else {
                    throw new BPAnalysisException(BPAnalysisException.Type.BA_Player_FetchFailed);
                }
            }
        } else {
            long id;
            try {
                id = userApiService.getOsuId(param.name);
            } catch (Exception e) {
                throw new BPAnalysisException(BPAnalysisException.Type.BA_Player_NotFound);
            }
            try {
                if (mode != OsuMode.DEFAULT) {
                    bps = scoreApiService.getBestPerformance(id, mode, 0, 100);
                    osuUser = userApiService.getPlayerInfo(id, mode);
                    osuUser.setPlayMode(mode.getName());
                } else {
                    osuUser = userApiService.getPlayerInfo(id);
                    bps = scoreApiService.getBestPerformance(id, osuUser.getOsuMode(), 0, 100);
                }
            } catch (HttpClientErrorException | WebClientResponseException e) {
                throw new BPAnalysisException(BPAnalysisException.Type.BA_Player_TokenExpired);
            } catch (Exception e) {
                throw new BPAnalysisException(BPAnalysisException.Type.BA_Player_FetchFailed);
            }
        }

        if (Objects.isNull(bps) || bps.size() <= 5) {
            if (param.isMyself) {
                throw new BPAnalysisException(BPAnalysisException.Type.BA_Me_NotEnoughBP, osuUser.getPlayMode());
            } else {
                throw new BPAnalysisException(BPAnalysisException.Type.BA_Player_NotEnoughBP, osuUser.getPlayMode());
            }
        }

        byte[] image = new byte[0];

        var data = parseData(osuUser, bps, userApiService);

        try {
            image = imageService.getPanelJ(data);
        } catch (HttpServerErrorException.InternalServerError e) {
            log.error("最好成绩分析：复杂面板生成失败", e);
            try {
                var msg = uubaService.getAllMsg(bps, osuUser.getUsername(), osuUser.getPlayMode());
                var image2 = imageService.getPanelAlpha(msg);
                from.sendImage(image2);

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

    public Map<String, Object> parseData(OsuUser user, List<Score> bps, OsuUserApiService userApiService) throws TipsException {
        var bpSize = bps.size();
        // top
        var t5 = bps.subList(0, Math.min(bpSize, 5));
        var b5 = bps.subList(Math.max(bpSize - 5, 0), bpSize);

        // 提取星级变化的谱面 DT/HT 等
        MapAttr.applyModChangeForScores(bps, user.getOsuMode(), imageService);

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
                    s.getMods().forEach(m -> modsPPMap.add(m, s.getWeight().weightedPP()));
                    modsSum += s.getMods().size();
                } else {
//                    modsPPSum.add("NM", s.getWeight().getPP());
                    modsSum += 1;
                }
                if (s.isPerfect()) {
                    rankMap.add("FC", s.getWeight().weightedPP());
                }
                rankMap.add(s.getRank(), s.getWeight().weightedPP());
            }
        }
        // 0 length; 1 combo; 2 star; 3 bpm
        @SuppressWarnings("unchecked")
        ArrayList<BeatMap4BA>[] mapStatistics = new ArrayList[4];
        var bpListSortedByLength = beatMapList.stream().sorted(Comparator.comparingInt(BeatMap4BA::length).reversed()).toList();
        mapStatistics[0] = new ArrayList<>(3);
        mapStatistics[0].add(bpListSortedByLength.getFirst());
        mapStatistics[0].add(bpListSortedByLength.get(bpSize / 2));
        mapStatistics[0].add(bpListSortedByLength.get(bpSize - 1));

        var bpListSortedByCombo = beatMapList.stream().sorted(Comparator.comparing(BeatMap4BA::combo).reversed()).toList();
        mapStatistics[1] = new ArrayList<>(3);
        mapStatistics[1].add(bpListSortedByCombo.getFirst());
        mapStatistics[1].add(bpListSortedByCombo.get(bpSize / 2));
        mapStatistics[1].add(bpListSortedByCombo.get(bpSize - 1));

        var bpListSortedByStar = beatMapList.stream().sorted(Comparator.comparing(BeatMap4BA::star).reversed()).toList();
        mapStatistics[2] = new ArrayList<>(3);
        mapStatistics[2].add(bpListSortedByStar.getFirst());
        mapStatistics[2].add(bpListSortedByStar.get(bpSize / 2));
        mapStatistics[2].add(bpListSortedByStar.get(bpSize - 1));

        var bpListSortedByBpm = beatMapList.stream().sorted(Comparator.comparing(BeatMap4BA::bpm).reversed()).toList();
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
        record Mapper(String avatar_url, String username, Integer map_count, Float pp_count) {

        }

        var mapperMap = bps.stream()
                .collect(Collectors.groupingBy(s -> s.getBeatMap().getMapperID(), Collectors.counting()));
        int mapperSize = mapperMap.size();
        var mapperCount = mapperMap
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(8)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k, v) -> k, LinkedHashMap::new));
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
                    return new Mapper(avatar, name, mapperCount.get(e.getKey()).intValue(), e.getValue().floatValue());
                })
                .toList();

        var bpPPs = bps.stream().mapToDouble(Score::getPP).toArray();

        var userPP = user.getPP();
        var bonusPP = DataUtil.getBonusPP(userPP, bpPPs);

        //bpPP + remainPP (bp100之后的) = rawPP
        var bpPP = (float) bps.stream().mapToDouble(s -> Optional.ofNullable(s.getWeight().weightedPP()).orElse(0f)).sum();
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

        /*
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

         */
        Map<String, Object> data = new HashMap<>();
        data.put("card_A1", user);
        data.put("bpTop5", t5);
        data.put("bpLast5", b5);
        data.put("bpLength", mapStatistics[0]);
        data.put("bpCombo", mapStatistics[1]);
        data.put("bpSR", mapStatistics[2]);
        data.put("bpBpm", mapStatistics[3]);
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
