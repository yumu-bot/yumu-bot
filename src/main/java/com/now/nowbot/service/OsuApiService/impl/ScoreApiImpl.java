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
                        .queryParam(OsuMode.isDefault(mode) ? "" : "mode", mode.getName())
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
                        .queryParam(OsuMode.isDefault(mode) ? "" : "mode", mode.getName())
                        .build(id))
                .headers(base::insertHeader)
                .retrieve()
                .bodyToFlux(Score.class)
                .collectList()
                .block();
    }

    @Override
    public List<Score> getRecent(BinUser user, OsuMode mode, int s, int e) {
        return null;
    }

    @Override
    public List<Score> getRecentIncludingFail(BinUser user, OsuMode mode, int s, int e) {
        return null;
    }

    @Override
    public BeatmapUserScore getScore(long bid, long uid, OsuMode mode) {
        return null;
    }

    @Override
    public BeatmapUserScore getScore(long bid, long uid, OsuMode mode, int modsValue) {
        return null;
    }

    @Override
    public List<Score> getScoreAll(long bid, BinUser user, OsuMode mode) {
        return null;
    }

    @Override
    public List<Score> getScoreAll(long bid, long uid, OsuMode mode) {
        return null;
    }

    @Override
    public List<Score> getBeatmapScores(long bid, OsuMode mode) {
        return null;
    }

    @Override
    public byte[] getReplay(long id) {
        return new byte[0];
    }
}
