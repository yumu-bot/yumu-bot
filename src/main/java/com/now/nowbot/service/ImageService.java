package com.now.nowbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.model.JsonData.*;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.imag.MapAttr;
import com.now.nowbot.model.imag.MapAttrGet;
import com.now.nowbot.model.multiplayer.MatchData;
import com.now.nowbot.model.multiplayer.MatchRound;
import com.now.nowbot.model.multiplayer.MatchStat;
import com.now.nowbot.model.multiplayer.SeriesData;
import com.now.nowbot.model.ppminus.PPMinus;
import com.now.nowbot.model.ppminus3.PPMinus3;
import com.now.nowbot.service.MessageServiceImpl.BPFixService;
import com.now.nowbot.service.MessageServiceImpl.MapStatisticsService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.util.ContextUtil;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.JacksonUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service("NOWBOT_IMAGE")
public class ImageService {
    private static final Logger log = LoggerFactory.getLogger(ImageService.class);
    @Resource
    RestTemplate restTemplate;
    public static final String IMAGE_PATH = "http://127.0.0.1:1611/";

    /**
     * 获取 md 图片，现已经弃用，被 panel A6 代替
     * @param markdown md 字符串
     * @return 图片流
     */
    @Deprecated
    public byte[] getMarkdownImage(String markdown) {
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of("md", markdown, "width", 1500);
        HttpEntity<Map<String, ?>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("md", httpEntity);
    }

    /***
     * 获取 md 图片，现已经弃用，被 panel A6 代替
     * 宽度是px,最好600以上
     * @param markdown md 字符串
     * @param width 宽度
     * @return 图片流
     */
    @Deprecated
    public byte[] getMarkdownImage(String markdown, int width) {
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of("md", markdown, "width", width);
        HttpEntity<Map<String, ?>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("md", httpEntity);
    }

    public Map<Long, MapAttr> getMapAttr(MapAttrGet p) {
        HttpHeaders headers = getDefaultHeader();

        HttpEntity<MapAttrGet> httpEntity = new HttpEntity<>(p, headers);
        ResponseEntity<List<MapAttr>> s = restTemplate.exchange(
                URI.create(STR."\{IMAGE_PATH}attr")
                , HttpMethod.POST, httpEntity
                , new ParameterizedTypeReference<>() {
                });
        List<MapAttr> result = s.getBody();
        if (CollectionUtils.isEmpty(result)) {
            return new HashMap<>();
        }

        return result.stream().collect(Collectors.toMap(MapAttr::getId, attr -> attr));
    }

    public void deleteLocalFile(long bid) {
        HttpHeaders headers = getDefaultHeader();
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(Map.of("bid", bid), headers);
        var result = restTemplate.exchange(
                URI.create(STR."\{IMAGE_PATH}del"),
                HttpMethod.POST,
                httpEntity,
                JsonNode.class
        );

        if (! result.getStatusCode().is2xxSuccessful()) {
            var body = result.getBody();
            if (body != null)
                throw new RuntimeException(body.get("status").asText());
        }
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

    public byte[] getPanelA4(OsuUser osuUser, List<Score> todayBPs, ArrayList<Integer> BPRanks) {
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of(
                "me", osuUser,
                "bps", todayBPs,
                "rank", BPRanks
        );
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_A4", httpEntity);
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

    public byte[] getPanelA6(OsuUser user, List<BPFixService.BPFix> fixes) {
        HttpHeaders headers = getDefaultHeader();
        var body = Map.of(
                "user", user,
                "score", fixes
        );

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_A6", httpEntity);
    }

    /**
     * Markdown 页面，用于帮助和维基 MD/H/W，user 默认 Optional.empty，width 默认 1840， name 默认 ""
     */
    public byte[] getPanelA6(String markdown) {
        return getPanelA6(Optional.empty(), markdown, "", 1840);
    }

    /**
     * Markdown 页面，用于帮助和维基 MD/H/W，user 默认 Optional.empty，width 默认 1840
     */
    public byte[] getPanelA6(String markdown, String name) {
        return getPanelA6(Optional.empty(), markdown, name, 1840);
    }

    /**
     * Markdown 页面，用于帮助和维基 MD/H/W， width 默认 1840， name 默认 null
     */
    public byte[] getPanelA6(Optional<OsuUser> user, String markdown) {
        return getPanelA6(user, markdown, "", 1840);
    }

    /**
     * Markdown 页面，用于帮助和维基 MD/H/W， width 默认 1840
     */
    public byte[] getPanelA6(Optional<OsuUser> user, String markdown, String name) {
        return getPanelA6(user, markdown, name, 1840);
    }
    /**
     * Markdown 页面，用于帮助和维基 MD/H/W
     * @param user 左上角的玩家，可以为 Optional.empty
     * @param markdown md 字符串
     * @param name 名字，仅支持 null、wiki、help
     * @param width 默认 1840
     * @return 图片流
     */

    public byte[] getPanelA6(Optional<OsuUser> user, String markdown, String name, Integer width) {
        HttpHeaders headers = getDefaultHeader();

        if (Objects.isNull(width)) width = 1840;

        var body = new HashMap<String, Object>(Map.of(
                "markdown", markdown,
                "name", name,
                "width", width
        ));

        if (user.isPresent()) {
            body.put("user", user);
        }

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_A6", httpEntity);
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
    public byte[] getPanelB1(OsuUser me, @Nullable OsuUser other, PPMinus my, @Nullable PPMinus others, OsuMode mode) {
        String value4;
        boolean isVs = other != null && others != null;

        if (mode == OsuMode.MANIA) {
            value4 = "PRE";
        } else {
            value4 = "STB";
        }
        //var Card_A = List.of(getPanelBUser(userMe), getPanelBUser(userOther));

        var CardA1 = new ArrayList<OsuUser>(2);
        CardA1.add(me);

        if (isVs) CardA1.add(other);

        var cardB1 = Map.of(
                "ACC", my.getValue1(),
                "PTT", my.getValue2(),
                "STA", my.getValue3(),
                value4, my.getValue4(),
                "EFT", my.getValue5(),
                "STH", my.getValue6(),
                "OVA", my.getValue7(),
                "SAN", my.getValue8()
        );
        var cardB2 = isVs ? Map.of(
                "ACC", others.getValue1(),
                "PTT", others.getValue2(),
                "STA", others.getValue3(),
                value4, others.getValue4(),
                "EFT", others.getValue5(),
                "STH", others.getValue6(),
                "OVA", others.getValue7(),
                "SAN", others.getValue8()
        ) : null;

        var statistics = Map.of("isVS", isVs, "gameMode", mode.getModeValue());
        HttpHeaders headers = getDefaultHeader();

        var body = new HashMap<String, Object>(4);

        body.put("card_A1", CardA1);
        body.put("card_b_1", cardB1);
        body.putIfAbsent("card_b_2", cardB2);
        body.put("statistics", statistics);

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_B1", httpEntity);
    }

    public byte[] getPanelB2(BeatMap beatMap, PPMinus3 mapMinus) {
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of(
                "beatMap", beatMap,
                "mapMinus", mapMinus
        );

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_B2", httpEntity);
    }

    public byte[] getPanelB3(OsuUser user, PPPlus plus) {
        var hashMap = new HashMap<String, Object>(6);

        hashMap.put("isUser", true);
        hashMap.put("isVs", false);
        hashMap.put("me", user);
        hashMap.put("my", plus);
        hashMap.put("other", null);
        hashMap.put("others", null);

        return getPanelB3(hashMap);
    }

    public byte[] getPanelB3(BeatMap beatMap, PPPlus plus) {
        var hashMap = new HashMap<String, Object>(6);

        hashMap.put("isUser", false);
        hashMap.put("isVs", false);
        hashMap.put("me", beatMap);
        hashMap.put("my", plus);
        hashMap.put("other", null);
        hashMap.put("others", null);

        return getPanelB3(hashMap);
    }

    public byte[] getPanelB3(HashMap<String, Object> hashMap) {
        hashMap.put("isVs", hashMap.containsKey("other"));
        HttpHeaders headers = getDefaultHeader();

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(hashMap, headers);
        return doPost("panel_B3", httpEntity);
    }

    public byte[] getPanelC(MatchData matchData) {
        HttpHeaders headers = getDefaultHeader();

        HttpEntity<MatchData> httpEntity = new HttpEntity<>(matchData, headers);
        return doPost("panel_C", httpEntity);
    }

    public byte[] getPanelC2(SeriesData seriesData) {
        HttpHeaders headers = getDefaultHeader();

        HttpEntity<SeriesData> httpEntity = new HttpEntity<>(seriesData, headers);
        return doPost("panel_C2", httpEntity);
    }


    public byte[] getPanelD(OsuUser osuUser, Optional<OsuUser> historyUser, Integer day, List<Score> BPs, OsuMode mode) {

        double bonus = 0f;

        if (! BPs.isEmpty()) {
            var bpPPs = BPs.stream().mapToDouble(Score::getPP).toArray();
            bonus = DataUtil.getBonusPP(osuUser.getPP(), bpPPs);
        }

        var times = BPs.stream().map(Score::getCreateTime).toList();
        var now = LocalDate.now();
        var bpTimes = new int[90];
        times.forEach(time -> {
            var d = (int) (now.toEpochDay() - time.toLocalDate().toEpochDay());
            if (d > 0 && d <= 90) {
                bpTimes[90 - d]++;
            }
        });

        HttpHeaders headers = getDefaultHeader();

        var body = new HashMap<>(Map.of("user", osuUser,
                "bp-time", bpTimes,
                "bonus_pp", bonus,
                "mode", mode.getName()
        ));

        historyUser.ifPresent(user -> {
            if (day != null) {
                body.put("day", day);
            }
            body.put("historyUser", user);
        });

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_D", httpEntity);
    }

    public byte[] getPanelE(OsuUser user, Score score, OsuBeatmapApiService beatmapApiService) throws WebClientResponseException {
        var map = beatmapApiService.getBeatMapInfo(score.getBeatMap().getId());
        score.setBeatMap(map);
        score.setBeatMapSet(map.getBeatMapSet());

        if (ContextUtil.getContext("isTest", Boolean.FALSE, Boolean.class)) {
            log.info("score.created_at_str: {}", score.getCreateTimeStr());
        }
        HttpHeaders headers = getDefaultHeader();
        var body = Map.of("user", user,
                "score", score
        );
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_E", httpEntity);
    }

    public byte[] getPanelE2(Optional<OsuUser> user, BeatMap beatMap, MapStatisticsService.Expected expected) {
        HttpHeaders headers = getDefaultHeader();
        var body = new HashMap<>(Map.of(
                "beatmap", beatMap,
                "expected", expected
        ));

        if (user.isPresent()) {
            body.put("user", user);
        }

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_E2", httpEntity);
    }

    public byte[] getPanelE3(MatchData matchData, BeatMap beatMap, MapStatisticsService.Expected expected) {
        HttpHeaders headers = getDefaultHeader();
        var body = Map.of(
                "beatmap", beatMap,
                "matchData", matchData,
                "expected", expected
        );

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_E3", httpEntity);
    }

    public byte[] getPanelF(MatchData matchData) {
        HttpHeaders headers = getDefaultHeader();

        HttpEntity<MatchData> httpEntity = new HttpEntity<>(matchData, headers);
        return doPost("panel_F", httpEntity);
    }

    public byte[] getPanelF2(MatchStat matchStat, MatchRound matchRound, int index) {
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of(
                "MatchStat", matchStat,
                "MatchRound", matchRound,
                "index", index
        );

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_F2", httpEntity);
    }


    public byte[] getPanelH(Object mapPool) {
        log.debug(JacksonUtil.objectToJsonPretty(mapPool));
        HttpHeaders headers = getDefaultHeader();
        HttpEntity<Object> httpEntity = new HttpEntity<>(mapPool, headers);
        return doPost("panel_H", httpEntity);
    }


    public byte[] getPanelJ(Map<String, Object> data) {
        var headers = getDefaultHeader();
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(data, headers);
        return doPost("panel_J", httpEntity);
    }
    //2023-07-12T12:42:37Z

    public byte[] getPanelM(Map<String, Object> data) {
        var headers = getDefaultHeader();
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(data, headers);
        return doPost("panel_M", httpEntity);
    }

    public byte[] getPanelN(Map<String, Object> data) {
        var headers = getDefaultHeader();
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(data, headers);
        return doPost("panel_N", httpEntity);
    }

    public byte[] getPanelAlpha(String... lines) {
        var headers = getDefaultHeader();
        Map<String, Object> body = new HashMap<>();
        body.put("strs", lines);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_Alpha", httpEntity);
    }

    public byte[] getPanelBeta(Score s) {
        var headers = getDefaultHeader();
        HttpEntity<Score> httpEntity = new HttpEntity<>(s, headers);
        return doPost("panel_Beta", httpEntity);
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
        body.put("beatmap", beatMap);
        body.put("round", round);
        body.put("mod", mod);
        body.put("position", position);
        body.put("hasBG", hasBG);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_Delta", httpEntity);
    }

    public byte[] getPanelEpsilon(OsuUser user) {
        var headers = getDefaultHeader();
        HttpEntity<OsuUser> httpEntity = new HttpEntity<>(user, headers);
        return doPost("panel_Epsilon", httpEntity);
    }

    public byte[] getPanelAlpha(StringBuilder sb) {
        return getPanelAlpha(sb.toString().split("\n"));
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
