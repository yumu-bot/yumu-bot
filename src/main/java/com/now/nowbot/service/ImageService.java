package com.now.nowbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.json.*;
import com.now.nowbot.model.mapminus.PPMinus3;
import com.now.nowbot.model.multiplayer.MatchCalculate;
import com.now.nowbot.model.multiplayer.MonitoredMatch;
import com.now.nowbot.model.multiplayer.SeriesCalculate;
import com.now.nowbot.model.ppminus.PPMinus;
import com.now.nowbot.service.messageServiceImpl.*;
import com.now.nowbot.util.ContextUtil;
import com.now.nowbot.util.DataUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service("NOWBOT_IMAGE")
public class ImageService {
    private static final Logger log = LoggerFactory.getLogger(ImageService.class);
    @Resource
    RestTemplate restTemplate;
    public static final String IMAGE_PATH = "http://127.0.0.1:1611/";

    // 2024+ 统一获取方法
    public byte[] getPanel(Map<String, Object> body, String name) {
        HttpHeaders headers = getDefaultHeader();
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_" + name, httpEntity);
    }

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

    public byte[] getPanelA2(BeatMapSetSearch search) {
        HttpHeaders headers = getDefaultHeader();
        HttpEntity<BeatMapSetSearch> httpEntity = new HttpEntity<>(search, headers);
        return doPost("panel_A2", httpEntity);
    }

    public byte[] getPanelA3(BeatMap beatMap, List<LazerScore> scores) {
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of(
                "beatmap", beatMap,
                "scores", scores
        );
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_A3", httpEntity);
    }

    public byte[] getPanelA4(OsuUser osuUser, List<LazerScore> todayBPs, List<Integer> BPRanks, String panel) {
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of(
                "user", osuUser,
                "scores", todayBPs,
                "rank", BPRanks,
                "panel", panel
        );
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_A4", httpEntity);
    }

    public byte[] getPanelA5(OsuUser user, List<LazerScore> scores, String panel) {
        HttpHeaders headers = getDefaultHeader();
        if (ContextUtil.getContext("isNewbie", Boolean.FALSE, Boolean.class)) {
            scores = scores.stream().filter(s -> s.getBeatMap().getStarRating() <= 5.7f).toList();
        }
        var body = Map.of(
                "user", user,
                "score", scores,
                "panel", panel
        );

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_A5", httpEntity);
    }
    /**
     * Markdown 页面，用于帮助和维基 MD/H/W，user 默认 Optional.empty，width 默认 1840， data 默认 ""
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
     * Markdown 页面，用于帮助和维基 MD/H/W， width 默认 1840， data 默认 null
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

    public byte[] getPanelA7(OsuUser user, Map<String, Object> fixes) {
        HttpHeaders headers = getDefaultHeader();
        Map<String, Object> body;
        try {
            fixes.put("user", user);
            body = fixes;
        } catch (UnsupportedOperationException ignore) {
            body = Map.of(
                    "user", user,
                    "scores", fixes.get("scores"),
                    "pp", fixes.get("pp")
            );
        }

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_A7", httpEntity);
    }


    public byte[] getPanelB1(OsuUser user, OsuMode mode, PPMinus my) {

        var cardA1 = List.of(user);

        var cardB = Map.of(
                "ACC", my.getValue1(),
                "PTT", my.getValue2(),
                "STA", my.getValue3(),
                (mode == OsuMode.MANIA) ? "PRE" : "STB", my.getValue4(),
                "EFT", my.getValue5(),
                "STH", my.getValue6(),
                "OVA", my.getValue7(),
                "SAN", my.getValue8()
        );

        var statistics = Map.of("isVS", false, "gameMode", mode.getModeValue());

        HttpHeaders headers = getDefaultHeader();

        var body = Map.of(
                "card_A1", cardA1,
                "card_b_1", cardB,
                "statistics", statistics
        );
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_B1", httpEntity);
    }
    public byte[] getPanelB1(OsuUser me, @Nullable OsuUser other, PPMinus my, @Nullable PPMinus others, OsuMode mode) {
        boolean isVs = other != null && others != null;

        //var Card_A = List.of(getPanelBUser(userMe), getPanelBUser(userOther));

        var cardA1s = new ArrayList<OsuUser>(2);
        cardA1s.add(me);

        if (isVs) cardA1s.add(other);

        var cardB1 = Map.of(
                "ACC", my.getValue1(),
                "PTT", my.getValue2(),
                "STA", my.getValue3(),
                (mode == OsuMode.MANIA) ? "PRE" : "STB", my.getValue4(),
                "EFT", my.getValue5(),
                "STH", my.getValue6(),
                "OVA", my.getValue7(),
                "SAN", my.getValue8()
        );
        var cardB2 = isVs ? Map.of(
                "ACC", others.getValue1(),
                "PTT", others.getValue2(),
                "STA", others.getValue3(),
                (mode == OsuMode.MANIA) ? "PRE" : "STB", others.getValue4(),
                "EFT", others.getValue5(),
                "STH", others.getValue6(),
                "OVA", others.getValue7(),
                "SAN", others.getValue8()
        ) : null;

        var statistics = Map.of("isVS", isVs, "gameMode", mode.getModeValue());
        HttpHeaders headers = getDefaultHeader();

        var body = new HashMap<String, Object>(4);

        body.put("card_A1", cardA1s);
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

    public byte[] getPanelB3(Map<String, Object> hashMap) {
        hashMap.put("isVs", hashMap.containsKey("other"));
        HttpHeaders headers = getDefaultHeader();

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(hashMap, headers);
        return doPost("panel_B3", httpEntity);
    }

    public byte[] getPanelC(MatchCalculate mc) {
        HttpHeaders headers = getDefaultHeader();

        HttpEntity<MatchCalculate> httpEntity = new HttpEntity<>(mc, headers);
        return doPost("panel_C", httpEntity);
    }

    public byte[] getPanelC2(SeriesCalculate sc) {
        HttpHeaders headers = getDefaultHeader();

        HttpEntity<SeriesCalculate> httpEntity = new HttpEntity<>(sc, headers);
        return doPost("panel_C2", httpEntity);
    }


    public byte[] getPanelD(OsuUser osuUser, Optional<OsuUser> historyUser, List<LazerScore> BPs, OsuMode mode) {

        double bonus = 0f;

        if (! BPs.isEmpty()) {
            var bpPPs = BPs.stream().mapToDouble(LazerScore::getPP).toArray();
            bonus = DataUtil.getBonusPP(osuUser.getPP(), bpPPs);
        }

        var times = BPs.stream().map(LazerScore::getEndedTimePretty).toList();
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
            if (user.getStatistics() instanceof InfoLogStatistics log) {
                body.put("day", ChronoUnit.DAYS.between(log.getLogTime().toLocalDate(), LocalDate.now()));
            }
            body.put("historyUser", user);
        });

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_D", httpEntity);
    }

    @Deprecated
    // 请使用 E5
    public byte[] getPanelE(OsuUser user, Score score) throws WebClientResponseException {
        HttpHeaders headers = getDefaultHeader();
        var body = Map.of(
                "user", user,
                "score", score
        );
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_E", httpEntity);
    }

    @Deprecated
    // 请使用 E6
    public byte[] getPanelE2(@Nullable OsuUser user, BeatMap beatMap, MapStatisticsService.Expected expected) {
        HttpHeaders headers = getDefaultHeader();
        var body = new HashMap<>(Map.of(
                "beatmap", beatMap,
                "expected", expected
        ));

        if (user != null) {
            body.put("user", user);
        }

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_E2", httpEntity);
    }

    @Deprecated
    // 请使用 E7
    public byte[] getPanelE3(MatchCalculate matchCalculate, BeatMap beatMap, MapStatisticsService.Expected expected) {
        HttpHeaders headers = getDefaultHeader();
        var body = Map.of(
                "beatmap", beatMap,
                "match", matchCalculate,
                "expected", expected
        );

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_E3", httpEntity);
    }

    public byte[] getPanelE7(MatchMapService.PanelE7Param param) {
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of(
                "match", param.match,
                "players", param.players,
                "mode", param.mode,
                "mods", param.mods,
                "beatmap", param.beatmap,
                "density", param.density,
                "original", param.original
        );

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_E7", httpEntity);
    }

    public byte[] getPanelF(MatchCalculate matchData) {
        HttpHeaders headers = getDefaultHeader();

        HttpEntity<MatchCalculate> httpEntity = new HttpEntity<>(matchData, headers);
        return doPost("panel_F", httpEntity);
    }

    public byte[] getPanelF2(Match.MatchStat matchStat, Match.MatchRound matchRound, int index) {
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of(
                "MatchStat", matchStat,
                "MatchRound", matchRound,
                "index", index
        );

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_F2", httpEntity);
    }

    public byte[] getPanelF2(MonitoredMatch.MatchStat stat, MonitoredMatch.MatchGame game, int index) {
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of(
                "MatchStat", stat,
                "MatchRound", game,
                "index", index
        );

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_F2", httpEntity);
    }


    public byte[] getPanelH(Object mapPool, OsuMode mode) {
        // log.debug(JacksonUtil.objectToJsonPretty(mapPool));
        HttpHeaders headers = getDefaultHeader();

        var body = Map.of(
                "pool", mapPool,
                "mode", mode.getName()
        );

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_H", httpEntity);
    }

    public byte[] getPanelAlpha(String... lines) {
        var headers = getDefaultHeader();
        Map<String, Object> body = new HashMap<>();
        body.put("strs", lines);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_Alpha", httpEntity);
    }

    public byte[] getPanelBeta(LazerScore s) {
        var headers = getDefaultHeader();
        HttpEntity<LazerScore> httpEntity = new HttpEntity<>(s, headers);
        return doPost("panel_Beta", httpEntity);
    }

    public byte[] getPanelGamma(LazerScore score) {
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

    public byte[] getPanelGamma(@Nullable OsuUser user, OsuMode mode, @Nullable PPMinus my) {
        var cardA1 = user == null ? null : List.of(user);

        var cardB = my == null ? null : Map.of(
                "ACC", my.getValue1(),
                "PTT", my.getValue2(),
                "STA", my.getValue3(),
                (mode == OsuMode.MANIA) ? "PRE": "STB", my.getValue4(),
                "EFT", my.getValue5(),
                "STH", my.getValue6(),
                "OVA", my.getValue7(),
                "SAN", my.getValue8()
        );

        var statistics = Map.of("isVS", false, "gameMode", mode.getModeValue());

        HttpHeaders headers = getDefaultHeader();

        var body = new HashMap<String, Object>(4);

        body.putIfAbsent("card_A1", cardA1);
        body.putIfAbsent("card_b_1", cardB);
        body.put("statistics", statistics);
        body.put("panel", "sanity");

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

    private byte[] doPost(String path, HttpEntity<?> entity) throws RestClientException {
        ResponseEntity<byte[]> s = restTemplate.exchange(URI.create(IMAGE_PATH + path), HttpMethod.POST, entity, byte[].class);
        return s.getBody();
    }
}
