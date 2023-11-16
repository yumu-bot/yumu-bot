package com.now.nowbot.service.OsuApiService.impl;

import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.BeatmapUserScore;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScoreApiImpl implements OsuScoreApiService {
    OsuApiBaseService base;

    public ScoreApiImpl(OsuApiBaseService baseService) {
        base = baseService;
    }

    @Override
    public List<Score> getBestPerformance(BinUser user, OsuMode mode, int s, int e) {
        if (!user.isAuthorized()) return getBestPerformance(user.getOsuID(), mode, s, e);
        return base.osuApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("users/{uid}/scores/best")
                        .queryParam("offset", s)
                        .queryParam("limit", e)
                        .queryParamIfPresent("mode", OsuMode.getName(mode))
                        .build(user.getOsuID()))
                .headers(base.insertHeader(user))
                .retrieve()
                .bodyToFlux(Score.class)
                .collectList()
                .block();
    }

    @Override
    public List<Score> getBestPerformance(Long id, OsuMode mode, int s, int e) {
        return base.osuApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("users/{uid}/scores/best")
                        .queryParam("offset", s)
                        .queryParam("limit", e)
                        .queryParam("mode", OsuMode.getName(mode))
                        .build(id))
                .headers(base::insertHeader)
                .retrieve()
                .bodyToFlux(Score.class)
                .collectList()
                .block();
    }

    @Override
    public List<Score> getRecent(BinUser user, OsuMode mode, int s, int e) {
        return getRecent(user, mode, false, s, e);
    }

    @Override
    public List<Score> getRecentIncludingFail(BinUser user, OsuMode mode, int s, int e) {
        return getRecent(user, mode, true, s, e);
    }

    private List<Score> getRecent(BinUser user, OsuMode mode, boolean includeF, int s, int e) {
        return base.osuApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("users/{uid}/scores/recent")
                        .queryParam("include_fails", includeF ? 1 : 0)
                        .queryParam("offset", s)
                        .queryParam("limit", e)
                        .queryParam("mode", OsuMode.getName(mode))
                        .build(user.getOsuID()))
                .headers(base.insertHeader(user))
                .retrieve()
                .bodyToFlux(Score.class)
                .collectList()
                .block();
    }

    @Override
    public BeatmapUserScore getScore(long bid, long uid, OsuMode mode) {
        return base.osuApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("beatmaps/{bid}/scores/users/{uid}")
                        .queryParam("mode", OsuMode.getName(mode))
                        .build(bid, uid))
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(BeatmapUserScore.class)
                .block();
    }

    public BeatmapUserScore getScore(long bid, BinUser user, OsuMode mode) {
        if (!user.isAuthorized()) return getScore(bid, user.getOsuID(), mode);
        return base.osuApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("beatmaps/{bid}/scores/users/{uid}")
                        .queryParam("mode", OsuMode.getName(mode))
                        .build(bid, user.getOsuID()))
                .headers(base.insertHeader(user))
                .retrieve()
                .bodyToMono(BeatmapUserScore.class)
                .block();
    }

    @Override
    public BeatmapUserScore getScore(long bid, long uid, OsuMode mode, int modsValue) {
        return base.osuApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("beatmaps/{bid}/scores/users/{uid}")
                        .queryParam("mode", OsuMode.getName(mode))
                        .build(bid, uid))
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(BeatmapUserScore.class)
                .block();
    }

    @Override
    public List<Score> getScoreAll(long bid, BinUser user, OsuMode mode) {
        if (!user.isAuthorized()) getScoreAll(bid, user.getOsuID(), mode);
        return base.osuApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("beatmaps/{bid}/scores/users/{uid}/all")
                        .queryParam("mode", OsuMode.getName(mode))
                        .build(bid, user.getOsuID()))
                .headers(base.insertHeader(user))
                .retrieve()
                .bodyToFlux(Score.class)
                .collectList()
                .block();
    }

    @Override
    public List<Score> getScoreAll(long bid, long uid, OsuMode mode) {
        return base.osuApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("beatmaps/{bid}/scores/users/{uid}/all")
                        .queryParam("mode", OsuMode.getName(mode))
                        .build(bid, uid))
                .headers(base::insertHeader)
                .retrieve()
                .bodyToFlux(Score.class)
                .collectList()
                .block();
    }

    @Override
    public List<Score> getBeatmapScores(long bid, OsuMode mode) {
        return base.osuApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("beatmaps/{bid}/scores")
                        .queryParam("mode", OsuMode.getName(mode))
                        .build(bid))
                .headers(base::insertHeader)
                .retrieve()
                .bodyToFlux(Score.class)
                .collectList()
                .block();
    }

    @Override
    public byte[] getReplay(long id, OsuMode mode) {
        return new byte[0];
    }
}
