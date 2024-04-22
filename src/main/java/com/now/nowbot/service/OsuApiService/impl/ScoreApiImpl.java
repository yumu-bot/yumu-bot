package com.now.nowbot.service.OsuApiService.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.BeatmapUserScore;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.util.JacksonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
public class ScoreApiImpl implements OsuScoreApiService {
    private static final Logger log = LoggerFactory.getLogger(ScoreApiImpl.class);
    OsuApiBaseService base;

    public ScoreApiImpl(OsuApiBaseService baseService) {
        base = baseService;
    }

    @Override
    public List<Score> getBestPerformance(BinUser user, OsuMode mode, int offset, int limit) {
        if (! user.isAuthorized()) return getBestPerformance(user.getOsuID(), mode, offset, limit);
        return base.osuApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("users/{uid}/scores/best")
                        .queryParam("legacy_only", 0)
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .queryParamIfPresent("mode", OsuMode.getName(mode))
                        .build(user.getOsuID()))
                .headers(base.insertHeader(user))
                .retrieve()
                .bodyToFlux(Score.class)
                .collectList()
                .block();
    }

    @Override
    public List<Score> getBestPerformance(Long id, OsuMode mode, int offset, int limit) {
        return base.osuApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("users/{uid}/scores/best")
                        .queryParam("legacy_only", 0)
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .queryParamIfPresent("mode", OsuMode.getName(mode))
                        .build(id))
                .headers(base::insertHeader)
                .retrieve()
                .bodyToFlux(Score.class)
                .collectList()
                .block();
    }

    @Override
    public List<Score> getRecent(BinUser user, OsuMode mode, int offset, int limit) {
        return getRecent(user, mode, false, offset, limit);
    }

    @Override
    public List<Score> getRecentIncludingFail(BinUser user, OsuMode mode, int offset, int limit) {
        return getRecent(user, mode, true, offset, limit);
    }

    @Override
    public List<Score> getRecent(long uid, OsuMode mode, int offset, int limit) {
        return getRecent(uid, mode, false, offset, limit);
    }

    @Override
    public List<Score> getRecentIncludingFail(long uid, OsuMode mode, int offset, int limit) {
        return getRecent(uid, mode, true, offset, limit);
    }

    @Override
    public BeatmapUserScore getScore(long bid, long uid, OsuMode mode) {
        return do404Retry(uriBuilder -> uriBuilder
                        .path("beatmaps/{bid}/scores/users/{uid}")
                        .queryParam("legacy_only", 0)
                        .queryParamIfPresent("mode", OsuMode.getName(mode))
                        .build(bid, uid), base::insertHeader, BeatmapUserScore.class,
                uriBuilder -> uriBuilder
                        .path("beatmaps/{bid}/scores/users/{uid}")
                        .queryParam("legacy_only", 1)
                        .queryParamIfPresent("mode", OsuMode.getName(mode))
                        .build(bid, uid)
        ).block();
    }

    @Override
    public BeatmapUserScore getScore(long bid, BinUser user, @NonNull OsuMode mode) {
        if (! user.isAuthorized()) return getScore(bid, user.getOsuID(), mode);
        return do404Retry(uriBuilder -> uriBuilder
                        .path("beatmaps/{bid}/scores/users/{uid}")
                        .queryParam("legacy_only", 0)
                        .queryParamIfPresent("mode", OsuMode.getName(mode))
                        .build(bid, user.getOsuID()), base.insertHeader(user), BeatmapUserScore.class,
                uriBuilder -> uriBuilder
                        .path("beatmaps/{bid}/scores/users/{uid}")
                        .queryParam("legacy_only", 1)
                        .queryParamIfPresent("mode", OsuMode.getName(mode))
                        .build(bid, user.getOsuID())
        ).block();
    }

    @Override
    public BeatmapUserScore getScore(long bid, long uid, OsuMode mode, int modsValue) {
        return do404Retry(uriBuilder -> uriBuilder
                        .path("beatmaps/{bid}/scores/users/{uid}")
                        .queryParam("legacy_only", 0)
                        .queryParamIfPresent("mode", OsuMode.getName(mode))
                        .build(bid, uid), base::insertHeader, BeatmapUserScore.class,
                uriBuilder -> uriBuilder
                        .path("beatmaps/{bid}/scores/users/{uid}")
                        .queryParam("legacy_only", 1)
                        .queryParamIfPresent("mode", OsuMode.getName(mode))
                        .build(bid, uid)
        ).block();
    }

    @Override
    public List<Score> getScoreAll(long bid, BinUser user, OsuMode mode) {
        if (! user.isAuthorized()) getScoreAll(bid, user.getOsuID(), mode);
        return base.osuApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("beatmaps/{bid}/scores/users/{uid}/all")
                        .queryParam("legacy_only", 0)
                        .queryParamIfPresent("mode", OsuMode.getName(mode))
                        .build(bid, user.getOsuID()))
                .headers(base.insertHeader(user))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> JacksonUtil.parseObjectList(json.get("scores"), Score.class))
                .block();
    }

    @Override
    public List<Score> getScoreAll(long bid, long uid, OsuMode mode) {
        return base.osuApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("beatmaps/{bid}/scores/users/{uid}/all")
                        .queryParam("legacy_only", 0)
                        .queryParamIfPresent("mode", OsuMode.getName(mode))
                        .build(bid, uid))
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> JacksonUtil.parseObjectList(json.get("scores"), Score.class))
                .block();
    }

    @Override
    public List<Score> getBeatmapScores(long bid, OsuMode mode) {
        return base.osuApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("beatmaps/{bid}/scores")
                        .queryParam("legacy_only", 0)
                        .queryParamIfPresent("mode", OsuMode.getName(mode))
                        .build(bid))
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> JacksonUtil.parseObjectList(json.get("scores"), Score.class))
                .block();
    }

    private List<Score> getRecent(BinUser user, OsuMode mode, boolean includeFails, int offset, int limit) {
        return base.osuApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("users/{uid}/scores/recent")
                        .queryParam("legacy_only", 0)
                        .queryParam("include_fails", includeFails ? 1 : 0)
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .queryParamIfPresent("mode", OsuMode.getName(mode))
                        .build(user.getOsuID()))
                .headers(base.insertHeader(user))
                .retrieve()
                .bodyToMono(String.class)
                .mapNotNull(s -> JacksonUtil.parseObjectList(s, Score.class))
                .block();
    }

    public List<Score> getRecent(long uid, OsuMode mode, boolean includeFails, int offset, int limit) {
        return base.osuApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("users/{uid}/scores/recent")
                        .queryParam("legacy_only", 0)
                        .queryParam("include_fails", includeFails ? 1 : 0)
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .queryParamIfPresent("mode", OsuMode.getName(mode))
                        .build(uid))
                .headers(base::insertHeader)
                .retrieve()
                .bodyToFlux(Score.class)
                .collectList()
                .block();
    }

    private <T> Mono<T> do404Retry(Function<UriBuilder, URI> uri, Consumer<HttpHeaders> headers, Class<T> clazz, Function<UriBuilder, URI> retry) {
        var mono = base.osuApiWebClient.get().uri(uri).headers(headers).retrieve().bodyToMono(clazz);
        if (Objects.nonNull(retry)) {
            return mono.onErrorResume(WebClientResponseException.NotFound.class, e ->
                    base.osuApiWebClient.get().uri(retry).headers(headers).retrieve().bodyToMono(clazz));
        } else return mono;
    }

    @Override
    public byte[] getReplay(long id, OsuMode mode) {
        return new byte[0];
    }
}
