package com.now.nowbot.service;

import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.match.Match;
import com.now.nowbot.model.match.MatchEvent;
import com.now.nowbot.model.score.MpScoreInfo;
import com.now.nowbot.util.SkiaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
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

@Service("nowbot-image")
public class ImageService {
    private static final Logger log = LoggerFactory.getLogger(ImageService.class);
    RestTemplate restTemplate;
    public static final String IMAGE_PATH = "http://127.0.0.1:1611/";

    @Autowired
    ImageService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
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

    public byte[] getPanelD(BinUser user, OsuMode mode, OsuGetService osuGetService) {
        var userInfo = osuGetService.getPlayerInfo(user, mode);
        var bps = osuGetService.getBestPerformance(user, mode, 0, 100);
        var res = osuGetService.getRecentN(user, mode, 0, 3);

        float bonus = 0f;
        if (bps.size() > 0) {
            var bppps = bps.stream().map((bpInfo) -> bpInfo.getWeight().getPP()).mapToDouble(Float::doubleValue).toArray();
            bonus = SkiaUtil.getBonusPP(bppps, userInfo.getPlayCount());
        }
        var times = bps.stream().map(Score::getCreateTime).toList();
        var now = LocalDate.now();
        var bpNum = new int[90];
        times.forEach(time -> {
            var day = (int)(now.toEpochDay() - time.toLocalDate().toEpochDay());
            if (day > 0 && day <= 90){
                bpNum[90-day] ++;
            }
        });

        HttpHeaders headers = getDefaultHeader();

        var body = Map.of("user",userInfo,
                "bp-time",bpNum,
                "bp-list", bps.subList(0,Math.min(bps.size(), 8)),
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
                .filter(m->m.getScoreInfos() != null && m.getScoreInfos().size()!=0)
                .toList();
        final int rSise = games.size();
        games = games.stream().limit(rSise - deleteEnd).skip(skipRounds).toList();
        var uidMap = new HashMap<Long, MicroUser>(match.getUsers().size());
        for (var u : match.getUsers()){
            uidMap.put(u.getId(), u);
        }
        String firstBackground = null;
        var scores = new ArrayList<Object>(games.size());
        int r_win = 0;
        int b_win = 0;
        int n_win = 0;
        float averageStar = 0;
        for (var gameItem: games){
            var statistics = new HashMap<String, Object>();
            var mapInfo = osuGetService.getMapInfo(gameItem.getBeatmap().getId());
            if (firstBackground == null){
                firstBackground = mapInfo.getBeatMapSet().getCovers().getList2x();
            }
            averageStar += mapInfo.getDifficultyRating() / games.size();
            statistics.put("background", mapInfo.getBeatMapSet().getCovers().getList2x());
            statistics.put("title", mapInfo.getBeatMapSet().getTitle());
            statistics.put("artist", mapInfo.getBeatMapSet().getArtist());
            statistics.put("mapper", mapInfo.getBeatMapSet().getCreator());
            statistics.put("difficulty", mapInfo.getVersion());
            statistics.put("status", mapInfo.getBeatMapSet().getStatus());
            statistics.put("bid", gameItem.getBeatmap().getId());
            var scoreRankList = gameItem.getScoreInfos().stream().sorted(Comparator.comparing(MpScoreInfo::getScore).reversed()).map(MpScoreInfo::getUserId).toList();
            if ("team-vs".equals(gameItem.getTeamType())){
                statistics.put("is_team_vs", true);
                var g_scores = gameItem.getScoreInfos().stream().filter(s -> s.getPassed() && s.getScore() >= 10000).toList();
                // 成绩分类
                var r_score = g_scores.stream().filter(s -> "red".equals(s.getMatch().get("team").asText())).toList();
                var b_score = g_scores.stream().filter(s -> "blue".equals(s.getMatch().get("team").asText())).toList();
                // 计算胜利(仅分数和
                var b_score_sum = b_score.stream().mapToInt(MpScoreInfo::getScore).sum();
                var r_score_sum = r_score.stream().mapToInt(MpScoreInfo::getScore).sum();
                statistics.put("score_team_red", r_score_sum );
                statistics.put("score_team_blue", b_score_sum );
                statistics.put("score_total", r_score_sum + b_score_sum );

                if (r_score_sum > b_score_sum) {
                    statistics.put("is_team_red_win", true);
                    statistics.put("is_team_blue_win", false);
                    r_win ++;
                } else if (r_score_sum < b_score_sum) {
                    statistics.put("is_team_red_win", false);
                    statistics.put("is_team_blue_win", true);
                    b_win ++;
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
                scores.add(Map.of(
                        "statistics",statistics,
                        "red", r_user_list,
                        "blue", b_user_list
                ));
            } else {
                var g_scores = gameItem.getScoreInfos().stream().filter(s -> s.getPassed() && s.getScore() >= 10000).toList();
                statistics.put("is_team_vs", false);
                statistics.put("is_team_red_win", false);
                statistics.put("is_team_blue_win", false);
                statistics.put("score_team_red", 0 );
                statistics.put("score_team_blue", 0 );
                statistics.put("wins_team_red_before", 0);
                statistics.put("wins_team_blue_before", 0);
                statistics.put("score_total", g_scores.stream().mapToInt(MpScoreInfo::getScore).sum());
                var user_list = g_scores.stream().map(s -> {
                    var u = uidMap.get(s.getUserId().longValue());
                    return getMatchScoreInfo(u.getUserName(), u.getAvatarUrl(), s.getScore(), s.getMods(), scoreRankList.indexOf(u.getId().intValue()) + 1);
                }).toList();
                scores.add(Map.of(
                        "statistics",statistics,
                        "none", user_list
                ));
                n_win ++;
            }
        }

        // match
        var matchInfo = match.getMatchInfo();
        var format = DateTimeFormatter.ofPattern("HH:mm");
        var info = new HashMap<String, Object>();
        info.put("background", firstBackground);
        info.put("match_title", matchInfo.getName());
        info.put("match_round", games.size());
        info.put("match_time", matchInfo.getStartTime().format(format) + '-' + matchInfo.getEndTime().format(format));
        info.put("match_time_start", matchInfo.getStartTime());
        info.put("match_time_end", matchInfo.getEndTime());
        info.put("average_star_rating", averageStar);
        info.put("mpid", matchInfo.getId());
        info.put("wins_team_red", r_win);
        info.put("wins_team_blue", b_win);
        info.put("wins_team_none", n_win);
        info.put("is_team_vs", n_win != 0);


        HttpHeaders headers = getDefaultHeader();

        var body = new HashMap<String, Object>();
        body.put("match", info);
        body.put("scores", scores);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("panel_F", httpEntity);
    }

    public byte[] drawLine(String ...lines){
        var headers = getDefaultHeader();
        Map<String, Object> body = new HashMap<>();
        body.put("strs", lines);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        return doPost("drawLine", httpEntity);
    }

    private Map<String,Object> getMatchScoreInfo(String name, String avatar, int score, String[] mods,int rank){
        return Map.of(
                "player_name",name,
                "player_avatar",avatar,
                "player_score",score,
                "player_mods",mods,
                "player_rank",rank
        );
    }
    private HttpHeaders getDefaultHeader(){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
    private byte[] doPost(String path, HttpEntity entity ){
        ResponseEntity<byte[]> s = restTemplate.exchange(URI.create(IMAGE_PATH + path), HttpMethod.POST, entity, byte[].class);
        return s.getBody();
    }
}
