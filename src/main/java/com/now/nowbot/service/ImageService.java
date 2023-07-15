package com.now.nowbot.service;

import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.PPm.Ppm;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.imag.MapAttr;
import com.now.nowbot.model.imag.MapAttrGet;
import com.now.nowbot.model.match.Match;
import com.now.nowbot.model.match.MatchEvent;
import com.now.nowbot.model.score.MpScoreInfo;
import com.now.nowbot.util.SkiaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service("nowbot-image")
public class ImageService {
    private static final Logger log = LoggerFactory.getLogger(ImageService.class);
    RestTemplate restTemplate;
    public static final String IMAGE_PATH = "http://127.0.0.1:1611/";

    @Autowired
    ImageService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public byte[] getMarkdownImage(String markdown) {
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of("md", markdown, "width", 1500);
        HttpEntity<Map> httpEntity = new HttpEntity<>(body, headers);
        return doPost("md", httpEntity);
    }

    /***
     * 宽度是px,最好600以上
     * @param width 宽度
     * @return 图片
     */
    public byte[] getMarkdownImage(String markdown, int width) {
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of("md", markdown, "width", width);
        HttpEntity<Map> httpEntity = new HttpEntity<>(body, headers);
        return doPost("md", httpEntity);
    }

    public List<MapAttr> getMapAttr(MapAttrGet p) {
        HttpHeaders headers = getDefaultHeader();

        HttpEntity<MapAttrGet> httpEntity = new HttpEntity<>(p, headers);
        ResponseEntity<List<MapAttr>> s = restTemplate.exchange(URI.create(IMAGE_PATH + "attr"), HttpMethod.POST, httpEntity, new ParameterizedTypeReference<List<MapAttr>>() {
        });
        return s.getBody();
    }

    public byte[] getCardH() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            InputStream in = new FileInputStream("/home/spring/Downloads/background.jpg");
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            ByteArrayResource fs = new ByteArrayResource(in.readAllBytes(), "") {
                @Override
                public String getFilename() {
                    return "bg.png";
                }
            };
            form.add("background", fs);
            form.add("title", "title");
            form.add("artist", "artist");
            form.add("info", "info");
            form.add("mod", "mod");
            form.add("star_b", "3");
            form.add("star_m", ".33");
            HttpEntity<MultiValueMap<String, Object>> datas = new HttpEntity<>(form, headers);
            var t = restTemplate.postForEntity("http://localhost:8555/card-d", datas, byte[].class);
            if (t.getStatusCode().is2xxSuccessful()) {
                return t.getBody();
            }
        } catch (IOException e) {
            log.error("File error", e);
        }
        return new byte[0];
    }

    public byte[] getPanelJ(OsuUser user, List<Score> bp) {
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of(
                "card_A1", user,
                "bp", bp
        );
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity(body, headers);
        return doPost("panel_J", httpEntity);
    }

    public byte[] getPanelB(OsuUser user, OsuMode mode, Ppm ppmMe) {
        String STBPRE;

        if (mode == OsuMode.MANIA) {
            STBPRE = "PRE";
        } else {
            STBPRE = "STB";
        }
        var Card_A = List.of(getPanelBUser(user));

        var cardB = Map.of(
                "ACC", ppmMe.getValue1(),
                "PTT", ppmMe.getValue2(),
                "STA", ppmMe.getValue3(),
                STBPRE, ppmMe.getValue4(),
                "EFT", ppmMe.getValue5(),
                "STH", ppmMe.getValue6(),
                "OVA", ppmMe.getValue7(),
                "SAN", ppmMe.getValue8()
        );

        var statistics = Map.of("isVS", false, "gameMode", mode.getModeValue());

        HttpHeaders headers = getDefaultHeader();

        var body = Map.of(
                "card_A1", Card_A,
                "card_b_1", cardB,
                "statistics", statistics
        );
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity(body, headers);
        return doPost("panel_B", httpEntity);
    }

    public byte[] getPanelB(OsuUser userMe, OsuUser userOther, Ppm ppmMe, Ppm ppmOther, OsuMode mode) {
        String STBPRE;

        if (mode == OsuMode.MANIA) {
            STBPRE = "PRE";
        } else {
            STBPRE = "STB";
        }
        var Card_A = List.of(getPanelBUser(userMe), getPanelBUser(userOther));

        var cardB1 = Map.of(
                "ACC", ppmMe.getValue1(),
                "PTT", ppmMe.getValue2(),
                "STA", ppmMe.getValue3(),
                STBPRE, ppmMe.getValue4(),
                "EFT", ppmMe.getValue5(),
                "STH", ppmMe.getValue6(),
                "OVA", ppmMe.getValue7(),
                "SAN", ppmMe.getValue8()
        );
        var cardB2 = Map.of(
                "ACC", ppmOther.getValue1(),
                "PTT", ppmOther.getValue2(),
                "STA", ppmOther.getValue3(),
                STBPRE, ppmOther.getValue4(),
                "EFT", ppmOther.getValue5(),
                "STH", ppmOther.getValue6(),
                "OVA", ppmOther.getValue7(),
                "SAN", ppmOther.getValue8()
        );

        var statistics = Map.of("isVS", true, "gameMode", mode.getModeValue());
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of(
                "card_A1", Card_A,
                "card_b_1", cardB1,
                "card_b_2", cardB2,
                "statistics", statistics
        );
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity(body, headers);
        return doPost("panel_B", httpEntity);
    }

    public byte[] getPanelD(BinUser user, OsuMode mode, OsuGetService osuGetService) {
        var userInfo = osuGetService.getPlayerInfo(user, mode);
        var bps = osuGetService.getBestPerformance(user, mode, 0, 100);
        var res = osuGetService.getRecentN(user, mode, 0, 3);

        double bpp = 0;

        for (int i = 0; i < bps.size(); i++) {
            var bp = bps.get(i);
            bpp += bp.getWeight().getPP();
        }

        double bonus = 0f;
        if (bps.size() > 0) {
            var bppps = bps.stream().map((bpInfo) -> bpInfo.getWeight().getPP()).mapToDouble(Float::doubleValue).toArray();
            bonus = Math.max(userInfo.getPP() - bpp - SkiaUtil.getOverBP100PP(bppps, userInfo.getPlayCount()), 0f);
        }
        var times = bps.stream().map(Score::getCreateTime).toList();
        var now = LocalDate.now();
        var bpNum = new int[90];
        times.forEach(time -> {
            var day = (int) (now.toEpochDay() - time.toLocalDate().toEpochDay());
            if (day > 0 && day <= 90) {
                bpNum[90 - day]++;
            }
        });

        HttpHeaders headers = getDefaultHeader();

        var body = Map.of("user", userInfo,
                "bp-time", bpNum,
                "bp-list", bps.subList(0, Math.min(bps.size(), 8)),
                "re-list", res,
                "bonus_pp", bonus,
                "mode", mode.getName()
        );
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_D", httpEntity);
    }

    public byte[] getPanelF(Match match, OsuGetService osuGetService, int skipRounds, int deleteEnd, boolean includingFail) {
        //scores
        var games = match.getEvents().stream()
                .map(MatchEvent::getGame)
                .filter(Objects::nonNull)
                .filter(m -> m.getScoreInfos() != null && m.getScoreInfos().size() != 0)
                .toList();
        final int rSise = games.size();
        games = games.stream().limit(rSise - deleteEnd).skip(skipRounds).toList();
        var uidMap = new HashMap<Long, MicroUser>(match.getUsers().size());
        for (var u : match.getUsers()) {
            uidMap.put(u.getId(), u);
        }
        String firstBackground = null;
        var scores = new ArrayList<>(games.size());
        int r_win = 0;
        int b_win = 0;
        int n_win = 0;
        for (var gameItem : games) {
            var statistics = new HashMap<String, Object>();

            var g_scores = gameItem.getScoreInfos().stream().filter(s -> (s.getPassed() || includingFail) && s.getScore() >= 10000).toList();
            final int allScoreSize = g_scores.size();
            var allUserModInt = g_scores.stream()
                    .flatMap(m -> Arrays.stream(m.getMods()))
                    .collect(Collectors.groupingBy(m -> m, Collectors.counting()))
                    .entrySet()
                    .stream().filter(a -> a.getValue() >= allScoreSize)
                    .map(Map.Entry::getKey)
                    .map(Mod::fromStr)
                    .mapToInt(m -> m.value)
                    .reduce(0, (a, i) -> a | i);

            if (gameItem.getBeatmap() != null) {
                var mapInfo = osuGetService.getMapInfoLite(gameItem.getBeatmap().getId());
                if (firstBackground == null) {
                    firstBackground = mapInfo.getMapSet().getList();
                }
                statistics.put("delete", false);
                statistics.put("background", mapInfo.getMapSet().getList());
                statistics.put("title", mapInfo.getMapSet().getTitle());
                statistics.put("artist", mapInfo.getMapSet().getArtist());
                statistics.put("mapper", mapInfo.getMapSet().getCreator());
                statistics.put("difficulty", mapInfo.getVersion());
                statistics.put("status", mapInfo.getMapSet().getStatus());
                statistics.put("bid", gameItem.getBeatmap().getId());
                statistics.put("mode", gameItem.getMode());
//                if (gameItem.getModInt() != null) {
//                    statistics.put("mod_int", gameItem.getModInt());
//                } else {
//                    statistics.put("mod_int", 0);
//                }
                statistics.put("mod_int", allUserModInt);
            } else {
                statistics.put("delete", true);
            }
            var scoreRankList = gameItem.getScoreInfos().stream().sorted(Comparator.comparing(MpScoreInfo::getScore).reversed()).map(MpScoreInfo::getUserId).toList();
            if ("team-vs".equals(gameItem.getTeamType())) {
                statistics.put("is_team_vs", true);
                // 成绩分类
                var r_score = g_scores.stream().filter(s -> "red".equals(s.getMatch().get("team").asText())).toList();
                var b_score = g_scores.stream().filter(s -> "blue".equals(s.getMatch().get("team").asText())).toList();
                // 计算胜利(仅分数和
                var b_score_sum = b_score.stream().mapToInt(MpScoreInfo::getScore).sum();
                var r_score_sum = r_score.stream().mapToInt(MpScoreInfo::getScore).sum();
                statistics.put("score_team_red", r_score_sum);
                statistics.put("score_team_blue", b_score_sum);
                statistics.put("score_total", r_score_sum + b_score_sum);

                if (r_score_sum > b_score_sum) {
                    statistics.put("is_team_red_win", true);
                    statistics.put("is_team_blue_win", false);
                    r_win++;
                } else if (r_score_sum < b_score_sum) {
                    statistics.put("is_team_red_win", false);
                    statistics.put("is_team_blue_win", true);
                    b_win++;
                } else {
                    statistics.put("is_team_red_win", false);
                    statistics.put("is_team_blue_win", false);
                }

                statistics.put("wins_team_red_before", r_win);
                statistics.put("wins_team_blue_before", b_win);

                var r_user_list = r_score.stream().sorted(Comparator.comparing(MpScoreInfo::getScore).reversed()).map(s -> {
                    var u = uidMap.get(s.getUserId().longValue());
                    return getMatchScoreInfo(u.getUserName(), u.getAvatarUrl(), s.getScore(), s.getMods(), scoreRankList.indexOf(u.getId().intValue()) + 1);
                }).toList();
                var b_user_list = b_score.stream().sorted(Comparator.comparing(MpScoreInfo::getScore).reversed()).map(s -> {
                    var u = uidMap.get(s.getUserId().longValue());
                    return getMatchScoreInfo(u.getUserName(), u.getAvatarUrl(), s.getScore(), s.getMods(), scoreRankList.indexOf(u.getId().intValue()) + 1);
                }).toList();
                if (r_user_list.size() == 0 || b_user_list.size() == 0) continue;
                scores.add(Map.of(
                        "statistics", statistics,
                        "red", r_user_list,
                        "blue", b_user_list
                ));
            } else {
                statistics.put("is_team_vs", false);
                statistics.put("is_team_red_win", false);
                statistics.put("is_team_blue_win", false);
                statistics.put("score_team_red", 0);
                statistics.put("score_team_blue", 0);
                statistics.put("wins_team_red_before", 0);
                statistics.put("wins_team_blue_before", 0);

                statistics.put("score_total", g_scores.stream().mapToInt(MpScoreInfo::getScore).sum());

                //如果只有一两个人，则不排序
                List user_list;

                if (g_scores.size() > 2) {
                    user_list = g_scores.stream().sorted(Comparator.comparing(MpScoreInfo::getScore).reversed()).map(s -> {
                        var u = uidMap.get(s.getUserId().longValue());
                        if (u == null) {
                            return getMatchScoreInfo("Unknown",
                                    "https://osu.ppy.sh/images/layout/avatar-guest.png",
                                    0,
                                    new String[0],
                                    -1
                            );
                        }
                        return getMatchScoreInfo(u.getUserName(), u.getAvatarUrl(), s.getScore(), s.getMods(), scoreRankList.indexOf(u.getId().intValue()) + 1);
                    }).toList();

                } else {
                    user_list = g_scores.stream().map(s -> {
                        var u = uidMap.get(s.getUserId().longValue());
                        if (u == null) {
                            return getMatchScoreInfo("Unknown",
                                    "https://osu.ppy.sh/images/layout/avatar-guest.png",
                                    0,
                                    new String[0],
                                    -1
                            );
                        }
                        return getMatchScoreInfo(u.getUserName(), u.getAvatarUrl(), s.getScore(), s.getMods(), scoreRankList.indexOf(u.getId().intValue()) + 1);
                    }).toList();
                }

                scores.add(Map.of(
                        "statistics", statistics,
                        "none", user_list
                ));

                n_win++;
            }
        }

        // match
        var matchInfo = match.getMatchInfo();
        var format = DateTimeFormatter.ofPattern("HH:mm");
        var info = new HashMap<String, Object>();
        info.put("background", firstBackground);
        info.put("match_title", matchInfo.getName());
        info.put("match_round", games.size());
        if (matchInfo.getEndTime() != null) {
            info.put("match_time", matchInfo.getStartTime().format(format) + '-' + matchInfo.getEndTime().format(format));
        } else {
            info.put("match_time", matchInfo.getStartTime().format(format) + '-' + "now");
        }
        info.put("match_time_start", matchInfo.getStartTime());
        info.put("match_time_end", matchInfo.getEndTime());
        info.put("mpid", matchInfo.getId());
        info.put("wins_team_red", r_win);
        info.put("wins_team_blue", b_win);
        info.put("wins_team_none", n_win);
        info.put("is_team_vs", n_win == 0);


        HttpHeaders headers = getDefaultHeader();

        var body = new HashMap<String, Object>();
        body.put("match", info);
        body.put("scores", scores);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_F", httpEntity);
    }


    public byte[] getPanelJ(OsuUser user, List<Score> bps, OsuGetService osuGetService) {
        var bpSize = bps.size();
        // top
        var t5 = bps.subList(0, 5);
        // bottom
        var b5 = bps.subList(bpSize - 6, bpSize - 1);

        var ppList = bps.stream().map(s -> s.getWeight().getPP());
        var ppRawList = bps.stream().map(Score::getPP);
        var rankCount = bps.stream()
                .map(Score::getRank)
                .toList();
        record mapper(String avatar_url, String username, Integer map_count, Float pp_count) {
        }
        List<mapper> mappers = new ArrayList<>(Math.min(8, bpSize));
        var mapperCount = bps.stream()
                .collect(Collectors.groupingBy(s -> s.getBeatMap().getUserId(), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(8)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o, n) -> o, LinkedHashMap::new));
        var mapperInfo = osuGetService.getUsers(mapperCount.keySet()).get("users");
        var mapperList = bps.stream()
                .filter(s -> mapperCount.containsKey(s.getBeatMap().getUserId()))
//                .collect(Collectors.groupingBy(s -> s.getBeatMap().getUserId(), Collectors.summingDouble(s -> s.getWeight().getPP())))
                .collect(Collectors.groupingBy(s -> s.getBeatMap().getUserId(), Collectors.summingDouble(Score::getPP)))
                .entrySet()
                .stream()
                .sorted(Comparator.<Map.Entry<Integer, Double>, Long>comparing(e -> mapperCount.get(e.getKey())).reversed().thenComparing(e -> e.getValue(), Comparator.reverseOrder()))
                .map(e -> {
                    String name = "";
                    String avatar = "";
                    for (var node : mapperInfo) {
                        if (e.getKey().equals(node.get("id").asInt(0))) {
                            name = node.get("username").asText("unknown");
                            avatar = node.get("avatar_url").asText("unknown");
                            break;
                        }
                    }
                    return new mapper(avatar, name, mapperCount.get(e.getKey()).intValue(), e.getValue().floatValue());
                })
                .toList();

        // 提取星级变化的谱面 DT/HT 等
        var mapAttrGet = new MapAttrGet(user.getPlayMode());
        bps.stream()
                .filter(s -> Mod.hasChangeRating(Mod.getModsValueFromStr(s.getMods())))
                .forEach(s -> {
                    mapAttrGet.addMap(s.getBeatMap().getId(), Mod.getModsValueFromStr(s.getMods()));
                });
        var changedAttrsMap = getMapAttr(mapAttrGet).stream().collect(Collectors.toMap(MapAttr::getBid, s -> s));

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
                var minfo = s.getBeatMap();
                if (changedAttrsMap.containsKey(s.getBeatMap().getId())) {
                    var attr = changedAttrsMap.get(s.getBeatMap().getId());
                    minfo.setDifficultyRating(attr.getStars());
                    if (s.getMods().contains("DT") || s.getMods().contains("NC")) {
                        minfo.setTotalLength(Math.round(minfo.getTotalLength() / 1.5f));
                        minfo.setBpm(minfo.getBpm() * 1.5f);
                    } else if (s.getMods().stream().anyMatch(r -> r.equals("HT"))) {
                        minfo.setTotalLength(Math.round(minfo.getTotalLength() / 0.75f));
                        minfo.setBpm(minfo.getBpm() * 0.75f);
                    }
                }
                var m = new map(
                        i,
                        minfo.getTotalLength(),
                        s.getMaxCombo(),
                        minfo.getBpm(),
                        minfo.getDifficultyRating(),
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
//                modsPPSum.add("NM", s.getWeight().getPP());
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
        mapStatistics[0].add(bpListSortedByLength.get(0));
        mapStatistics[0].add(bpListSortedByLength.get(bpSize / 2));
        mapStatistics[0].add(bpListSortedByLength.get(bpSize - 1));

        var bpListSortedByCombo = mapList.stream().sorted(Comparator.comparing(map::combo).reversed()).toList();
        mapStatistics[1] = new ArrayList<>(3);
        mapStatistics[1].add(bpListSortedByCombo.get(0));
        mapStatistics[1].add(bpListSortedByCombo.get(bpSize / 2));
        mapStatistics[1].add(bpListSortedByCombo.get(bpSize - 1));

        var bpListSortedByStar = mapList.stream().sorted(Comparator.comparing(map::star).reversed()).toList();
        mapStatistics[2] = new ArrayList<>(3);
        mapStatistics[2].add(bpListSortedByStar.get(0));
        mapStatistics[2].add(bpListSortedByStar.get(bpSize / 2));
        mapStatistics[2].add(bpListSortedByStar.get(bpSize - 1));

        var bpListSortedByBpm = mapList.stream().sorted(Comparator.comparing(map::bpm).reversed()).toList();
        mapStatistics[3] = new ArrayList<>(3);
        mapStatistics[3].add(bpListSortedByBpm.get(0));
        mapStatistics[3].add(bpListSortedByBpm.get(bpSize / 2));
        mapStatistics[3].add(bpListSortedByBpm.get(bpSize - 1));

        float rawPP = bps.stream().map(s -> s.getWeight().getPP()).reduce(Float::sum).orElse(0F);

        List<attr> modsAttr = new ArrayList<>(modsPPSum.size());
        {
            final int m = modsSum;
            modsPPSum.forEach((mod, value) -> {
                attr attr = new attr(mod, value.size(), value.stream().reduce(Float::sum).orElse(0F), (1F * value.size() / m));
                modsAttr.add(attr);
            });
        }

        List<attr> rankAttr = new ArrayList<>(rankSum.size());
        {
            final float rpp = rawPP;
            var fcList = rankSum.remove("FC");
            attr fc;
            if (CollectionUtils.isEmpty(fcList)) {
                fc = new attr("FC", 0, 0, 0);
            } else {
                float ppSum = fcList.stream().reduce(Float::sum).orElse(0F);
                fc = new attr("FC", fcList.size(), ppSum, (ppSum / rpp));
            }
            rankAttr.add(fc);
            rankSum.forEach((rank, value) -> {
                float ppSum = value.stream().reduce(Float::sum).orElse(0F);
                attr attr = new attr(rank, value.size(), ppSum, (ppSum / rpp));
                rankAttr.add(attr);
            });
        }
        var headers = getDefaultHeader();
        Map<String, Object> body = new HashMap<>();
        body.put("card_A1", user);
        body.put("bpTop5", t5);
        body.put("bpLast5", b5);
        body.put("bpLength", mapStatistics[0]);
        body.put("bpCombo", mapStatistics[1]);
        body.put("bpSR", mapStatistics[2]);
        body.put("bpBpm", mapStatistics[3]);
        body.put("favorite_mappers", mapperList);
        body.put("pp_raw_arr", ppRawList);
        body.put("rank_arr", rankCount);
        body.put("pp_length_arr", mapList.stream().map(map::length).toList());
        body.put("mods_attr", modsAttr);
        body.put("rank_attr", rankAttr);
        body.put("pp_raw", rawPP);
        body.put("pp", user.getPP());
        body.put("game_mode", user.getPlayMode().getName());
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_J", httpEntity);
    }

    public byte[] getPanelA1(OsuUser userMe, List<MicroUser> friendList) {
        var headers = getDefaultHeader();
        Map<String, Object> body = new HashMap<>();
        body.put("me_card_A1", userMe);
        body.put("friend_card_A1", friendList);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_A1", httpEntity);
    }


    public byte[] getPanelE(OsuUser user, Score score, OsuGetService osuGetService) {
        var map = osuGetService.getMapInfo(score.getBeatMap().getId());
        score.setBeatMap(map);
        score.setBeatMapSet(map.getBeatMapSet());

        HttpHeaders headers = getDefaultHeader();
        var body = Map.of("user", user,
                "score", score
        );
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_E", httpEntity);
    }

    public byte[] drawLine(String... lines) {
        var headers = getDefaultHeader();
        Map<String, Object> body = new HashMap<>();
        body.put("strs", lines);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("drawLine", httpEntity);
    }

    public byte[] drawLine(StringBuilder sb) {
        return drawLine(sb.toString().split("\n"));
    }

    private Map<String, Object> getMatchScoreInfo(String name, String avatar, int score, String[] mods, int rank) {
        return Map.of(
                "player_name", name,
                "player_avatar", avatar,
                "player_score", score,
                "player_mods", mods,
                "player_rank", rank
        );
    }

    private HttpHeaders getDefaultHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    private byte[] doPost(String path, HttpEntity entity) {
        ResponseEntity<byte[]> s = restTemplate.exchange(URI.create(IMAGE_PATH + path), HttpMethod.POST, entity, byte[].class);
        return s.getBody();
    }

    private Map<String, Object> getPanelBUser(OsuUser user) {
        var map = new HashMap<String, Object>(12);
        map.put("background", user.getCoverUrl());
        map.put("avatar", user.getAvatarUrl());
        map.put("sub_icon1", user.getCountry().countryCode());
        map.put("sub_icon2", user.getSupportLeve());
        map.put("name", user.getUsername());
        map.put("rank_global", user.getGlobalRank());
        map.put("rank_country", user.getCountryRank());
        map.put("country", user.getCountry().countryCode());
        map.put("acc", user.getAccuracy());
        map.put("level", user.getLevelCurrent());
        map.put("progress", user.getLevelProgress());
        map.put("pp", user.getPP());
        return map;
    }
}
