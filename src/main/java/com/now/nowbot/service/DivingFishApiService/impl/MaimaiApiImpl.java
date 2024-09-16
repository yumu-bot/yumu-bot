package com.now.nowbot.service.DivingFishApiService.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.now.nowbot.model.JsonData.MaiBestPerformance;
import com.now.nowbot.model.JsonData.MaiRanking;
import com.now.nowbot.model.JsonData.MaiSong;
import com.now.nowbot.model.enums.MaiVersion;
import com.now.nowbot.service.DivingFishApiService.MaimaiApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class MaimaiApiImpl implements MaimaiApiService {
    private static final Logger log = LoggerFactory.getLogger(MaimaiApiService.class);
    DivingFishBaseService base;

    public MaimaiApiImpl(DivingFishBaseService baseService) {
        base = baseService;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MaimaiBestScoreBody(@Nullable Long qq, @Nullable String username, Boolean b50) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MaimaiByVersionBody(@Nullable Long qq, @Nullable String username, @NonNull List<String> version) {}

    @Override
    public MaiBestPerformance getMaimaiBest50(Long qq) {
        return getMaimaiBest(qq, null);
    }

    @Override
    public MaiBestPerformance getMaimaiBest50(String probername) {
        return getMaimaiBest(null, probername);
    }

    @Override
    public MaiBestPerformance getMaimaiScoreByVersion(String probername, List<MaiVersion> versions) {
        return getMaimaiScoreByVersion(null, probername, versions);
    }

    @Override
    public MaiBestPerformance getMaimaiScoreByVersion(Long qq, List<MaiVersion> versions) {
        return getMaimaiScoreByVersion(qq, null, versions);
    }

    private MaiBestPerformance getMaimaiBest(Long qq, String probername) {
        var b = new MaimaiBestScoreBody(qq, probername, true);

        return base.divingFishApiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("api/maimaidxprober/query/player")
                        .build())
                .body(Mono.just(b), MaimaiBestScoreBody.class)
                .headers(v -> v.set("Content-Type", "application/json"))
                .retrieve()
                .bodyToMono(MaiBestPerformance.class)
                .block();
    }

    private MaiBestPerformance getMaimaiScoreByVersion(Long qq, String probername, List<MaiVersion> versions) {
        var b = new MaimaiByVersionBody(qq, probername, MaiVersion.getNameList(versions));

        return base.divingFishApiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("api/maimaidxprober/query/plate")
                        .build())
                .body(Mono.just(b), MaimaiByVersionBody.class)
                .headers(v -> v.set("Content-Type", "application/json"))
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
    public List<MaiSong> getMaimaiSongLibrary() {
        return base.divingFishApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("api/maimaidxprober/music_data")
                        .build())
                .retrieve()
                .bodyToFlux(MaiSong.class)
                .collectList()
                .block();
    }

    @Override
    public List<MaiRanking> getMaimaiRankLibrary() {
        return base.divingFishApiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("api/maimaidxprober/rating_ranking")
                        .build())
                .retrieve()
                .bodyToFlux(MaiRanking.class)
                .collectList()
                .block();
    }

    @Override
    public MaiSong getMaimaiSong(Integer songID) {
        return null;
    }

    @Override
    public MaiSong getMaimaiSong(String title) {
        return null;
    }
}
