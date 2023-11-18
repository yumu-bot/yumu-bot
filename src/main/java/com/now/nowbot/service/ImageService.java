package com.now.nowbot.service;

import com.now.nowbot.config.NoProxyRestTemplate;
import com.now.nowbot.model.JsonData.*;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.imag.MapAttr;
import com.now.nowbot.model.imag.MapAttrGet;
import com.now.nowbot.model.match.GameInfo;
import com.now.nowbot.model.match.MPScore;
import com.now.nowbot.model.match.Match;
import com.now.nowbot.model.match.MatchEvent;
import com.now.nowbot.model.multiplayer.MatchData;
import com.now.nowbot.model.multiplayer.MatchRound;
import com.now.nowbot.model.ppminus.PPMinus;
import com.now.nowbot.model.ppminus3.MapMinus;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("NOWBOTIMAGE")
public class ImageService {
    private static final Logger log = LoggerFactory.getLogger(ImageService.class);
    private static final String[] RANK_ARRAY = new String[]{"XH", "X", "SSH", "SS", "SH", "S", "A", "B", "C", "D", "F"};
    RestTemplate restTemplate;
    public static final String IMAGE_PATH = "http://127.0.0.1:1611/";

    @Autowired
    ImageService(NoProxyRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public byte[] getMarkdownImage(String markdown) {
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of("md", markdown, "width", 1500);
        HttpEntity<Map<String, ?>> httpEntity = new HttpEntity<>(body, headers);
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
        HttpEntity<Map<String, ?>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("md", httpEntity);
    }

    public List<MapAttr> getMapAttr(MapAttrGet p) {
        HttpHeaders headers = getDefaultHeader();

        HttpEntity<MapAttrGet> httpEntity = new HttpEntity<>(p, headers);
        ResponseEntity<List<MapAttr>> s = restTemplate.exchange(URI.create(IMAGE_PATH + "attr"), HttpMethod.POST, httpEntity, new ParameterizedTypeReference<>() {
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

    public byte[] getPanelA1(OsuUser userMe, List<MicroUser> friendList) {
        var headers = getDefaultHeader();
        Map<String, Object> body = new HashMap<>();
        body.put("me_card_A1", userMe);
        body.put("friend_card_A1", friendList);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_A1", httpEntity);
    }

    public byte[] getPanelA2(Search search) {
        HttpHeaders headers = getDefaultHeader();
        HttpEntity<Search> httpEntity = new HttpEntity<>(search, headers);
        return doPost("panel_A2", httpEntity);
    }

    public byte[] getPanelA3(BeatMap beatMap, List<Score> scores) {
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of(
                "beatmap", beatMap,
                "scores", scores
        );
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_A3", httpEntity);
    }

    public byte[] getPanelA4(OsuUser osuUser, List<Score> bpList, ArrayList<Integer> bpRank) {
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of(
                "me", osuUser,
                "bps", bpList,
                "rank", bpRank
        );
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_A4", httpEntity);
    }

    public byte[] getPanelB1(OsuUser user, OsuMode mode, PPMinus PPMinusMe) {
        String STBPRE;

        if (mode == OsuMode.MANIA) {
            STBPRE = "PRE";
        } else {
            STBPRE = "STB";
        }

        var Card_A = List.of(user);

        var cardB = Map.of(
                "ACC", PPMinusMe.getValue1(),
                "PTT", PPMinusMe.getValue2(),
                "STA", PPMinusMe.getValue3(),
                STBPRE, PPMinusMe.getValue4(),
                "EFT", PPMinusMe.getValue5(),
                "STH", PPMinusMe.getValue6(),
                "OVA", PPMinusMe.getValue7(),
                "SAN", PPMinusMe.getValue8()
        );

        var statistics = Map.of("isVS", false, "gameMode", mode.getModeValue());

        HttpHeaders headers = getDefaultHeader();

        var body = Map.of(
                "card_A1", Card_A,
                "card_b_1", cardB,
                "statistics", statistics
        );
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_B1", httpEntity);
    }

    public byte[] getPanelB1(OsuUser userMe, OsuUser userOther, PPMinus PPMinusMe, PPMinus PPMinusOther, OsuMode mode) {
        String STBPRE;

        if (mode == OsuMode.MANIA) {
            STBPRE = "PRE";
        } else {
            STBPRE = "STB";
        }
        //var Card_A = List.of(getPanelBUser(userMe), getPanelBUser(userOther));
        var Card_A = List.of(userMe, userOther);

        var cardB1 = Map.of(
                "ACC", PPMinusMe.getValue1(),
                "PTT", PPMinusMe.getValue2(),
                "STA", PPMinusMe.getValue3(),
                STBPRE, PPMinusMe.getValue4(),
                "EFT", PPMinusMe.getValue5(),
                "STH", PPMinusMe.getValue6(),
                "OVA", PPMinusMe.getValue7(),
                "SAN", PPMinusMe.getValue8()
        );
        var cardB2 = Map.of(
                "ACC", PPMinusOther.getValue1(),
                "PTT", PPMinusOther.getValue2(),
                "STA", PPMinusOther.getValue3(),
                STBPRE, PPMinusOther.getValue4(),
                "EFT", PPMinusOther.getValue5(),
                "STH", PPMinusOther.getValue6(),
                "OVA", PPMinusOther.getValue7(),
                "SAN", PPMinusOther.getValue8()
        );

        var statistics = Map.of("isVS", true, "gameMode", mode.getModeValue());
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of(
                "card_A1", Card_A,
                "card_b_1", cardB1,
                "card_b_2", cardB2,
                "statistics", statistics
        );
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_B1", httpEntity);
    }

    public byte[] getPanelB2(BeatMap beatMap, MapMinus mapMinus) {

        HttpHeaders headers = getDefaultHeader();

        var body = Map.of(
                "beatMap", beatMap,
                "mapMinus", mapMinus
        );

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_B2", httpEntity);
    }

    public byte[] getPanelC(MatchData matchData) {
        HttpHeaders headers = getDefaultHeader();

        HttpEntity<MatchData> httpEntity = new HttpEntity<>(matchData, headers);
        return doPost("panel_C", httpEntity);
    }

    public byte[] getPanelF2(MatchRound round, int index) {
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of(
                "MatchRound", round,
                "index", index
        );

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_F2", httpEntity);
    }


    public byte[] getPanelD(OsuUser osuUser, List<Score> BPs, List<Score> Recents, OsuMode mode) {

        double bonus = 0f;
        if (!BPs.isEmpty()) {
            var bpPPs = BPs.stream().mapToDouble(Score::getPP).toArray();
            bonus = SkiaUtil.getBonusPP(osuUser.getPP(), bpPPs);
        }
        var times = BPs.stream().map(Score::getCreateTime).toList();
        var now = LocalDate.now();
        var bpTimes = new int[90];
        times.forEach(time -> {
            var day = (int) (now.toEpochDay() - time.toLocalDate().toEpochDay());
            if (day > 0 && day <= 90) {
                bpTimes[90 - day]++;
            }
        });

        HttpHeaders headers = getDefaultHeader();

        var body = Map.of("user", osuUser,
                "bp-time", bpTimes,
                "bp-list", BPs.subList(0, Math.min(BPs.size(), 8)),
                "re-list", Recents,
                "bonus_pp", bonus,
                "mode", mode.getName(),
                "ranked_map_play_count", SkiaUtil.getPlayedRankedMapCount(bonus)
        );
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_D", httpEntity);
    }

    public byte[] getPanelE(OsuUser user, Score score, OsuBeatmapApiService beatmapApiService) {
        var map = beatmapApiService.getBeatMapInfo(score.getBeatMap().getId());
        score.setBeatMap(map);
        score.setBeatMapSet(map.getBeatMapSet());

        HttpHeaders headers = getDefaultHeader();
        var body = Map.of("user", user,
                "score", score
        );
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_E", httpEntity);
    }

    public byte[] getPanelA5(OsuUser user, List<Score> scores) {
        HttpHeaders headers = getDefaultHeader();
        var body = Map.of(
                "user", user,
                "score", scores
        );

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_A5", httpEntity);
    }

    public byte[] getPanelF(Match match, OsuGetService osuGetService, int skipRounds, int deleteEnd, boolean includingFail, boolean includingRematch) {
        //scores
        List<GameInfo> games = match.getEvents().stream()
                .map(MatchEvent::getGame)
                .filter(Objects::nonNull)
                .filter(m -> m.getScoreInfoList() != null && !m.getScoreInfoList().isEmpty())
                .toList();
        {
            final int rawSize = games.size();
            games = games.stream().limit(rawSize - deleteEnd).skip(skipRounds).collect(Collectors.toList());

            if (!includingRematch) {
                // 保证顺序的情况下,去除重复
                var bsit = new HashSet<Long>();
                Collections.reverse(games);
                games.removeIf((e) -> !bsit.add(e.getBID()));
                Collections.reverse(games);
            }
        }

        var uidMap = new HashMap<Long, MicroUser>(match.getUsers().size());
        for (var u : match.getUsers()) {
            uidMap.put(u.getId(), u);
        }
        String firstBackground = null;
        var scores = new ArrayList<>(games.size());
        int r_win = 0;
        int b_win = 0;
        int n_win = 0;
        for (var g : games) {
            var statistics = new HashMap<String, Object>();

            var g_scores = g.getScoreInfoList().stream().filter(s -> (s.getPassed() || includingFail) && s.getScore() >= 10000).toList();
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

            if (g.getBeatmap() != null) {
                var info = osuGetService.getMapInfoLite(g.getBeatmap().getId());
                if (firstBackground == null) {
                    firstBackground = info.getMapSet().getList();
                }
                statistics.put("delete", false);
                statistics.put("background", info.getMapSet().getList());
                statistics.put("title", info.getMapSet().getTitle());
                statistics.put("artist", info.getMapSet().getArtist());
                statistics.put("mapper", info.getMapSet().getCreator());
                statistics.put("difficulty", info.getVersion());
                statistics.put("status", info.getMapSet().getStatus());
                statistics.put("bid", g.getBID());
                statistics.put("mode", g.getMode());
//                if (gameItem.getModInt() != null) {
//                    statistics.put("mod_int", gameItem.getModInt());
//                } else {
//                    statistics.put("mod_int", 0);
//                }
                statistics.put("mod_int", allUserModInt);
            } else {
                statistics.put("delete", true);
                statistics.put("bid", g.getBID());
            }
            var scoreRankList = g.getScoreInfoList().stream().sorted(Comparator.comparing(MPScore::getScore).reversed()).map(MPScore::getUID).toList();
            if ("team-vs".equals(g.getTeamType())) {
                statistics.put("is_team_vs", true);
                // 成绩分类
                var r_score = g_scores.stream().filter(s -> "red".equals(s.getMatch().get("team").asText())).toList();
                var b_score = g_scores.stream().filter(s -> "blue".equals(s.getMatch().get("team").asText())).toList();
                // 计算胜利(仅分数和
                var b_score_sum = b_score.stream().mapToInt(MPScore::getScore).sum();
                var r_score_sum = r_score.stream().mapToInt(MPScore::getScore).sum();
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

                var r_user_list = r_score.stream().sorted(Comparator.comparing(MPScore::getScore).reversed()).map(s -> {
                    var u = uidMap.get(s.getUID().longValue());
                    return getMatchScoreInfo(u.getUserName(), u.getAvatarUrl(), s.getScore(), s.getMods(), scoreRankList.indexOf(u.getId().intValue()) + 1);
                }).toList();
                var b_user_list = b_score.stream().sorted(Comparator.comparing(MPScore::getScore).reversed()).map(s -> {
                    var u = uidMap.get(s.getUID().longValue());
                    return getMatchScoreInfo(u.getUserName(), u.getAvatarUrl(), s.getScore(), s.getMods(), scoreRankList.indexOf(u.getId().intValue()) + 1);
                }).toList();
                if (r_user_list.isEmpty() || b_user_list.isEmpty()) continue;
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

                statistics.put("score_total", g_scores.stream().mapToInt(MPScore::getScore).sum());

                //如果只有一两个人，则不排序
                List<Map<String, Object>> user_list;

                {
                    var stream = g_scores.stream();

                    if (g_scores.size() > 2) {
                        stream = stream.sorted(Comparator.comparing(MPScore::getScore).reversed());
                    }

                    user_list = stream.map(s -> {
                        var u = uidMap.get(s.getUID().longValue());
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
                    scores.add(Map.of(
                            "statistics", statistics,
                            "none", user_list
                    ));
                }

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


    public byte[] getPanelJ(OsuUser user, List<Score> bps, OsuUserApiService userApiService) {
        var bpSize = bps.size();
        // top
        var t5 = bps.subList(0, Math.min(bpSize, 5));
        var b5 = bps.subList(Math.max(bpSize - 5, 0), bpSize);

        // 提取星级变化的谱面 DT/HT 等
        var mapAttrGet = new MapAttrGet(user.getPlayMode());
        bps.stream()
                .filter(s -> Mod.hasChangeRating(Mod.getModsValueFromStr(s.getMods())))
                .forEach(s -> mapAttrGet.addMap(s.getBeatMap().getId(), Mod.getModsValueFromStr(s.getMods())));
        Map<Long, MapAttr> changedAttrsMap;
        if (CollectionUtils.isEmpty(mapAttrGet.getMaps())) {
            changedAttrsMap = null;
        } else {
            changedAttrsMap = getMapAttr(mapAttrGet).stream().collect(Collectors.toMap(MapAttr::getBid, s -> s));
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
                var minfo = s.getBeatMap();
                if (changedAttrsMap != null && changedAttrsMap.containsKey(s.getBeatMap().getId())) {
                    var attr = changedAttrsMap.get(s.getBeatMap().getId());
                    minfo.setDifficultyRating(attr.getStars());
                    if (s.getMods().contains("DT") || s.getMods().contains("NC")) {
                        minfo.setTotalLength(Math.round(minfo.getTotalLength() / 1.5f));
                        minfo.setBpm(minfo.getBPM() * 1.5f);
                    } else if (s.getMods().stream().anyMatch(r -> r.equals("HT"))) {
                        minfo.setTotalLength(Math.round(minfo.getTotalLength() / 0.75f));
                        minfo.setBpm(minfo.getBPM() * 0.75f);
                    }
                }
                var m = new map(
                        i + 1,
                        minfo.getTotalLength(),
                        s.getMaxCombo(),
                        minfo.getBPM(),
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
                .collect(Collectors.groupingBy(s -> s.getBeatMap().getUserId(), Collectors.counting()));
        int mappers = bpMapperMap.size();
        var mapperCount = bpMapperMap
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(8)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (o, e) -> o, LinkedHashMap::new));
        var mapperInfo = userApiService.getUsers(mapperCount.keySet());
        var mapperList = bps.stream()
                .filter(s -> mapperCount.containsKey(s.getBeatMap().getUserId()))
//                .collect(Collectors.groupingBy(s -> s.getBeatMap().getUserId(), Collectors.summingDouble(s -> s.getWeight().getPP())))
                .collect(Collectors.groupingBy(s -> s.getBeatMap().getUserId(), Collectors.summingDouble(Score::getPP)))
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
        var bonusPP = SkiaUtil.getBonusPP(userPP, bpPPs);

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
            java.util.function.Consumer<Score> f = (s) -> {
                long id = s.getBeatMap().getId();
                if (changedAttrsMap.containsKey(id)) {
                    var attr = changedAttrsMap.get(id);
                    s.getBeatMap().setDifficultyRating(attr.getStars());
                    s.getBeatMap().setBpm(attr.getBpm());
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
        var headers = getDefaultHeader();
        Map<String, Object> body = new HashMap<>();
        body.put("card_A1", user);
        body.put("bpTop5", t5);
        body.put("bpLast5", b5);
        body.put("bpLength", mapStatistics[0]);
        body.put("bpCombo", mapStatistics[1]);
        body.put("bpSR", mapStatistics[2]);
        body.put("bpBpm", mapStatistics[3]);
        body.put("favorite_mappers_count", mappers);
        body.put("favorite_mappers", mapperList);
        body.put("pp_raw_arr", ppRawList);
        body.put("rank_arr", rankCount);
        body.put("rank_elect_arr", rankSort);
        body.put("bp_length_arr", mapList.stream().map(map::length).toList());
        body.put("mods_attr", modsAttr);
        body.put("rank_attr", rankAttr);
        body.put("pp_raw", rawPP);
        body.put("pp", userPP);
        body.put("game_mode", bps.get(0).getMode());
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_J", httpEntity);
    }
    //2023-07-12T12:42:37Z

    public byte[] getPanelM(OsuUser user, OsuGetService osuGetService) {
        var page = 1;
        var query = new HashMap<String, Object>();
        query.put("q", "creator=" + user.getUID());
        query.put("sort","ranked_desc");
        query.put("s", "any");
        query.put("page", page);

        Search search = null;
        //依据QualifiedMapService 的逻辑来多次获取

        {
            int resultCount = 0;
            do {
                if (search == null) {
                    search = osuGetService.searchBeatmap(query);
                    resultCount += search.getBeatmapsets().size();
                    continue;
                }
                page ++;
                query.put("page", page);
                var result = osuGetService.searchBeatmap(query);
                resultCount += result.getResultCount();
                search.getBeatmapsets().addAll(result.getBeatmapsets());
            } while (resultCount < search.getTotal() && page < 10);
        }

        List<ActivityEvent> activity;
        List<ActivityEvent> mappingActivity;
        try {
            activity = osuGetService.getUserRecentActivity(user.getUID(), 0, 100);
            mappingActivity = activity.stream().filter(ActivityEvent::isTypeMapping).toList();
                    /* 原设想是，这里把相近的同名同属性活动删去。但是不知道怎么写
                    .collect(collectingAndThen(
                    toCollection(() -> new TreeSet<>(Comparator.comparing(s -> (s.getBeatmapSet().title())))),
                    ArrayList::new))

                    .stream().sorted(Comparator.comparing(ActivityEvent::getCreatedAt).reversed()).toList();

                     */

        } catch (Exception e) {
            mappingActivity = null;
        }

        var mostPopularBeatmap = search
                .getBeatmapsets()
                .stream()
                .filter(s -> (s.getMapperUID().longValue() == user.getUID()))
                .sorted(Comparator.comparing(BeatMapSet::getPlayCount).reversed())
                .limit(6)
                .toList();

        var mostRecentRankedBeatmap = search
                .getBeatmapsets()
                .stream()
                .filter(s -> (s.isRanked() && user.getUID() == s.getMapperUID().longValue()))
                .findFirst()
                .orElse(null);

        if (mostRecentRankedBeatmap == null && user.getBeatmapSetCountRankedAndApproved() > 0) {
            try {
                var query1 = new HashMap<String, Object>();
                query1.put("q", user.getUID().toString());
                query1.put("sort", "ranked_desc");
                query1.put("s", "any");
                query1.put("page", 1);

                var search1 = osuGetService.searchBeatmap(query1);
                mostRecentRankedBeatmap = search1.getBeatmapsets().stream().filter(BeatMapSet::isRanked).findFirst().orElse(null);

            } catch (Exception ignored) {}
        }

        var mostRecentRankedGuestDiff = search
                .getBeatmapsets()
                .stream()
                .filter(s -> (s.isRanked()) && user.getUID() != s.getMapperUID().longValue())
                .findFirst()
                .orElse(null);
        var allBeatmaps = search.getBeatmapsets().stream().flatMap(s -> s.getBeatmaps().stream()).toList();

        var diffArr = new int[8];
        {
            var diffAll = allBeatmaps.stream().filter(b -> b.getUserId().longValue() == user.getUID()).mapToDouble(BeatMap::getDifficultyRating).toArray();
            var starMaxBoundary = new double[]{2f, 2.8f, 4f, 5.3f, 6.5f, 8f, 10f, Double.MAX_VALUE};
            for (var d : diffAll) {
                for (int i = 0; i < 8; i++) {
                    if (d <= starMaxBoundary[i]) {
                        diffArr[i]++;
                        break;
                    }
                }
            }
        }

        int[] genre;
        {
            String[] keywords = new String[]{"unspecified", "video game", "anime", "rock", "pop", "other", "novelty", "hip hop", "electronic", "metal", "classical", "folk", "jazz"};
            genre = new int[keywords.length];
            AtomicBoolean hasAnyGenre = new AtomicBoolean(false);

            //逻辑应该是先每张图然后再遍历12吧？
            if (!search.getBeatmapsets().isEmpty()) {
                search.getBeatmapsets().forEach(m -> {
                    for (int i = 1; i < keywords.length; i++) {
                        var keyword = keywords[i];

                        if (m.getTags().toLowerCase().contains(keyword)) {
                            genre[i]++;
                            hasAnyGenre.set(true);
                        }
                    }

                    //0是实在找不到 tag 的时候所赋予的默认值
                    if (hasAnyGenre.get()) {
                        hasAnyGenre.set(false);
                    } else {
                        genre[0]++;
                    }
                });
            }
        }

        int favorite = 0;
        int playcount = 0;
        if (!search.getBeatmapsets().isEmpty()) {
            for (int i = 0; i < search.getBeatmapsets().size(); i++) {
                var v = search.getBeatmapsets().get(i);

                if (v.getMapperUID() == user.getUID().intValue()) {
                    favorite += v.getFavourite();
                    playcount += v.getPlayCount();
                }
            }
        }

        var lengthArr = new int[8];
        {
            var lengthAll = allBeatmaps.stream().filter(b -> b.getUserId().longValue() == user.getUID()).mapToDouble(BeatMap::getTotalLength).toArray();
            var lengthMaxBoundary = new double[]{60, 90, 120, 150, 180, 210, 240, Double.MAX_VALUE};
            for (var f : lengthAll) {
                for (int i = 0; i < 8; i++) {
                    if (f <= lengthMaxBoundary[i]) {
                        lengthArr[i]++;
                        break;
                    }
                }
            }
        }


        var headers = getDefaultHeader();
        Map<String, Object> body = new HashMap<>();
        body.put("user", user);
        body.put("most_popular_beatmap", mostPopularBeatmap);
        body.put("most_recent_ranked_beatmap", mostRecentRankedBeatmap);
        body.put("most_recent_ranked_guest_diff", mostRecentRankedGuestDiff);
        body.put("difficulty_arr", diffArr);
        body.put("length_arr", lengthArr);
        body.put("genre", genre);
        body.put("recent_activity", mappingActivity);
        body.put("favorite", favorite);
        body.put("playcount", playcount);
        return doPost("panel_M", new HttpEntity<>(body, headers));
    }

    public byte[] drawLine(String... lines) {
        var headers = getDefaultHeader();
        Map<String, Object> body = new HashMap<>();
        body.put("strs", lines);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_Alpha", httpEntity);
    }

    public byte[] getPanelGamma(Score score) {
        var headers = getDefaultHeader();
        Map<String, Object> body = new HashMap<>();
        body.put("score", score);
        body.put("panel", "score");
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_Gamma", httpEntity);
    }

    public byte[] getPanelGamma(OsuUser osuUser) {
        var headers = getDefaultHeader();
        Map<String, Object> body = new HashMap<>();
        body.put("user", osuUser);
        body.put("panel", "info");
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_Gamma", httpEntity);
    }

    public byte[] getPanelDelta(BeatMap beatMap, String round, String mod, Short position, boolean hasBG) {
        var headers = getDefaultHeader();
        Map<String, Object> body = new HashMap<>();
        body.put("beatMap", beatMap);
        body.put("round", round);
        body.put("mod", mod);
        body.put("position", position);
        body.put("hasBG", hasBG);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_Delta", httpEntity);
    }

    public byte[] getPanelEpsilon(String username, long uid) {
        var headers = getDefaultHeader();
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("uid", uid);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_Epsilon", httpEntity);
    }

    public byte[] spInfo(Score s) {
        var headers = getDefaultHeader();
        HttpEntity<Score> httpEntity = new HttpEntity<>(s, headers);
        return doPost("panel_Beta", httpEntity);
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

    private byte[] doPost(String path, HttpEntity<?> entity) {
        ResponseEntity<byte[]> s = restTemplate.exchange(URI.create(IMAGE_PATH + path), HttpMethod.POST, entity, byte[].class);
        return s.getBody();
    }
}
