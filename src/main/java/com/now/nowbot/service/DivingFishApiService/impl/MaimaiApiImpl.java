package com.now.nowbot.service.DivingFishApiService.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.model.JsonData.*;
import com.now.nowbot.model.enums.MaiVersion;
import com.now.nowbot.service.DivingFishApiService.MaimaiApiService;
import com.now.nowbot.util.JacksonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@Service
public class MaimaiApiImpl implements MaimaiApiService {
    private static final Logger log = LoggerFactory.getLogger(MaimaiApiService.class);
    DivingFishBaseService base;

    public MaimaiApiImpl(DivingFishBaseService baseService) {
        base = baseService;
    }

    private record MaimaiBestScoreQQBody(Long qq, Boolean b50) {}

    private record MaimaiBestScoreNameBody(String username, Boolean b50) {}

    private record MaimaiByVersionQQBody(Long qq, @NonNull List<String> version) {}

    private record MaimaiByVersionNameBody(String username, @NonNull List<String> version) {}

    @Override
    public MaiBestPerformance getMaimaiBest50(Long qq) {
        var b = new MaimaiBestScoreQQBody(qq, true);

        return base.divingFishApiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("api/maimaidxprober/query/player")
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(b), MaimaiBestScoreQQBody.class)
                .headers(base::insertJSONHeader)
                .retrieve()
                .bodyToMono(MaiBestPerformance.class)
                .block();
    }

    @Override
    public MaiBestPerformance getMaimaiBest50(String username) {
        var b = new MaimaiBestScoreNameBody(username, true);

        return base.divingFishApiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("api/maimaidxprober/query/player")
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(b), MaimaiBestScoreNameBody.class)

                .retrieve()
                .bodyToMono(MaiBestPerformance.class)
                .block();
    }

    @Override
    public MaiBestPerformance getMaimaiScoreByVersion(String username, List<MaiVersion> versions) {
        var b = new MaimaiByVersionNameBody(username, MaiVersion.getNameList(versions));

        return base.divingFishApiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("api/maimaidxprober/query/plate")
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(b), MaimaiByVersionNameBody.class)
                .headers(base::insertJSONHeader)
                .retrieve()
                .bodyToMono(MaiBestPerformance.class)
                .block();
    }

    @Override
    public MaiBestPerformance getMaimaiScoreByVersion(Long qq, List<MaiVersion> versions) {
        var b = new MaimaiByVersionQQBody(qq, MaiVersion.getNameList(versions));

        return base.divingFishApiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("api/maimaidxprober/query/plate")
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(b), MaimaiByVersionQQBody.class)
                .headers(base::insertJSONHeader)
                .retrieve()
                .bodyToMono(MaiBestPerformance.class)
                .block();
    }

    @Override
    public byte[] getMaimaiCover(Long songID) {
        String song;
        long id;

        if (songID == null) {
            id = 1L;
        } else if (songID.equals(1235L)) {
            id = songID + 10000L; // 这是水鱼的 bug，不关我们的事
        } else if (songID > 10000L && songID < 11000L){
            id = songID - 10000L;
        } else {
            id = songID;
        }

        song = String.format("%05d", id);

        byte[] cover;
        try {
            cover = base.divingFishApiWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("covers/" + song + ".png")
                            .build())
                    .retrieve()
                    .bodyToMono(byte[].class).block();
        } catch (WebClientResponseException.NotFound e) {
            cover = base.divingFishApiWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("covers/00000.png")
                            .build())
                    .retrieve()
                    .bodyToMono(byte[].class).block();
        }

        return cover;
    }

    @Override
    // TODO 临时方案
    public List<MaiSong> getMaimaiSongLibrary() throws IOException {
        var out = base.divingFishApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("api/maimaidxprober/music_data")
                        .build())
                .retrieve()
                .bodyToFlux(MaiSong.class)
                .collectList()
                .block();

        var path = Path.of("/home/spring/cache/nowbot/bg/ExportFileV3/maimai/data-songs.json");
        Files.write(path, Objects.requireNonNull(JacksonUtil.objectToJson(out)).getBytes());

        return out;
    }

    @Override
    public List<MaiRanking> getMaimaiRankLibrary() throws IOException {
        var out = base.divingFishApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("api/maimaidxprober/rating_ranking")
                        .build())
                .retrieve()
                .bodyToFlux(MaiRanking.class)
                .collectList()
                .block();

        var path = Path.of("/home/spring/cache/nowbot/bg/ExportFileV3/maimai/data-ranking.json");
        Files.write(path, Objects.requireNonNull(JacksonUtil.objectToJson(out)).getBytes());

        return out;
    }

    @Override
    public MaiSong getMaimaiSong(Integer songID) {
        return null;
    }

    @Override
    public MaiSong getMaimaiSong(String title) {
        return null;
    }

    @Override
    public MaiScore getMaimaiScore(Long qq, Integer songID) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway {
        return null;
    }

    @Override
    public List<MaiScore> getMaimaiScores(Long qq, List<Integer> songIDs) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway {
        return List.of();
    }

    @Override
    public MaiScore getMaimaiScore(String username, Integer songID) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway {
        return null;
    }

    @Override
    public List<MaiScore> getMaimaiScores(String username, List<Integer> songIDs) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway {
        return List.of();
    }

    @Override
    public MaiBestPerformance getMaimaiBest50P(Long qq) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway {
        var b = new MaimaiBestScoreQQBody(qq, true);

        return base.divingFishApiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("api/maimaidxprober/query/player")
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(b), MaimaiBestScoreQQBody.class)
                .headers(base::insertDeveloperHeader)
                .retrieve()
                .bodyToMono(MaiBestPerformance.class)
                .block();
    }

    @Override
    public MaiBestPerformance getMaimaiBest50P(String probername) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway {
        var b = new MaimaiBestScoreNameBody(probername, true);

        return base.divingFishApiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("api/maimaidxprober/query/player")
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(b), MaimaiBestScoreNameBody.class)
                .headers(base::insertDeveloperHeader)
                .retrieve()
                .bodyToMono(MaiBestPerformance.class)
                .block();
    }


    @Override
    public MaiFit getMaimaiFit() throws IOException {
        var out = base.divingFishApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("api/maimaidxprober/chart_stats")
                        .build())
                .retrieve()
                .bodyToMono(MaiFit.class)
                .block();

        var path = Path.of("/home/spring/cache/nowbot/bg/ExportFileV3/maimai/data-fit.json");
        Files.write(path, Objects.requireNonNull(JacksonUtil.objectToJson(out)).getBytes());

        return out;
    }
}
