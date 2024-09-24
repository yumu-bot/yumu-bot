package com.now.nowbot.service.divingFishApiService.impl;

import com.now.nowbot.model.enums.MaiVersion;
import com.now.nowbot.model.json.*;
import com.now.nowbot.service.divingFishApiService.MaimaiApiService;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MaimaiApiImpl implements MaimaiApiService {
    private static final Logger log = LoggerFactory.getLogger(MaimaiApiService.class);

    // D:/App2/[Projects]/yumu-bot-run/img/ExportFileV3/Maimai
    // /home/spring/work/img/ExportFileV3/Maimai
    private static final Path path = Path.of("/home/spring/work/img/ExportFileV3/Maimai");

    private static final long updatePeriodMillis = 86400000L; // 1 天： 86400000L，30 秒：30000L

    DivingFishBaseService base;


    public MaimaiApiImpl(DivingFishBaseService baseService) {
        base = baseService;
    }

    private record MaimaiBestScoreQQBody(Long qq, Boolean b50) {
    }

    private record MaimaiBestScoreNameBody(String username, Boolean b50) {
    }

    private record MaimaiByVersionQQBody(Long qq, @NonNull List<String> version) {
    }

    private record MaimaiByVersionNameBody(String username, @NonNull List<String> version) {
    }

    @Override
    public MaiBestPerformance getMaimaiBest50(Long qq) {
        var b = new MaimaiBestScoreQQBody(qq, true);

        return base.divingFishApiWebClient.post().uri(uriBuilder -> uriBuilder.path("api/maimaidxprober/query/player").build()).contentType(MediaType.APPLICATION_JSON).body(Mono.just(b), MaimaiBestScoreQQBody.class).headers(base::insertJSONHeader).retrieve().bodyToMono(MaiBestPerformance.class).block();
    }

    @Override
    public MaiBestPerformance getMaimaiBest50(String username) {
        var b = new MaimaiBestScoreNameBody(username, true);

        return base.divingFishApiWebClient.post().uri(uriBuilder -> uriBuilder.path("api/maimaidxprober/query/player").build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(b), MaimaiBestScoreNameBody.class)
                .retrieve()
                .bodyToMono(MaiBestPerformance.class)
                .block();
    }

    @Override
    public MaiBestPerformance getMaimaiScoreByVersion(String username, List<MaiVersion> versions) {
        var b = new MaimaiByVersionNameBody(username, MaiVersion.getNameList(versions));

        return base.divingFishApiWebClient.post().uri(uriBuilder -> uriBuilder.path("api/maimaidxprober/query/plate").build())
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

        return base.divingFishApiWebClient.post().uri(uriBuilder -> uriBuilder.path("api/maimaidxprober/query/plate")
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(b), MaimaiByVersionQQBody.class)
                .headers(base::insertJSONHeader)
                .retrieve()
                .bodyToMono(MaiBestPerformance.class).block();
    }

    @Override
    public byte[] getMaimaiCover(Long songID) {
        String song;
        long id;

        if (songID == null) {
            id = 1L;
        } else if (songID.equals(1235L)) {
            id = songID + 10000L; // 这是水鱼的 bug，不关我们的事
        } else if (songID > 10000L && songID < 11000L) {
            id = songID - 10000L;
        } else {
            id = songID;
        }

        song = String.format("%05d", id);

        byte[] cover;
        try {
            cover = base.divingFishApiWebClient.get().uri(uriBuilder -> uriBuilder.path("covers/" + song + ".png").build()).retrieve().bodyToMono(byte[].class).block();
        } catch (WebClientResponseException.NotFound e) {
            cover = base.divingFishApiWebClient.get().uri(uriBuilder -> uriBuilder.path("covers/00000.png").build()).retrieve().bodyToMono(byte[].class).block();
        }

        return cover;
    }

    @Override
    public Map<Integer, MaiSong> getMaimaiSongLibrary() {
        if (isFileOutdated("data-songs.json")) {
            log.info("maimai: 歌曲库不存在或者已过期，更新中");
            updateMaimaiSongLibrary();
        }

        return Objects.requireNonNull(file2Objects("data-songs.json", MaiSong.class))
                .stream().collect(Collectors.toMap(MaiSong::getSongID, s -> s));
    }

    @Override
    public Map<String, Integer> getMaimaiRankLibrary() {
        if (isFileOutdated("data-ranking.json")) {
            log.info("maimai: 排名库不存在或者已过期，更新中");
            updateMaimaiRankLibrary();
        }

        return Objects.requireNonNull(file2Objects("data-ranking.json", MaiRanking.class))
                .stream().collect(Collectors.toMap(MaiRanking::getName, MaiRanking::getRating));

    }

    @Override
    public MaiFit getMaimaiFit() {
        if (isFileOutdated("data-fit.json")) {
            log.info("maimai: 统计库不存在或者已过期，更新中");
            updateMaimaiFit();
        }

        return file2Object("data-fit.json", MaiFit.class);
    }

    @Override
    public void updateMaimaiSongLibrary() {
        var data = base.divingFishApiWebClient.get().uri(uriBuilder -> uriBuilder.path("api/maimaidxprober/music_data").build()).retrieve().bodyToMono(String.class).block();

        saveFile(data, "data-songs.json", "歌曲");
    }

    @Override
    public void updateMaimaiRankLibrary() {
        var data = base.divingFishApiWebClient.get().uri(uriBuilder -> uriBuilder.path("api/maimaidxprober/rating_ranking").build()).retrieve().bodyToMono(String.class).block();

        saveFile(data, "data-ranking.json", "排名");
    }

    @Override
    public void updateMaimaiFit() {
        var data = base.divingFishApiWebClient.get().uri(uriBuilder -> uriBuilder.path("api/maimaidxprober/chart_stats").build()).retrieve().bodyToMono(String.class).block();

        saveFile(data, "data-fit.json", "统计");
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
    public MaiScore getMaimaiSongScore(Long qq, Integer songID) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway {
        return null;
    }

    @Override
    public List<MaiScore> getMaimaiSongsScore(Long qq, List<Integer> songIDs) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway {
        return List.of();
    }

    @Override
    public MaiScore getMaimaiSongScore(String username, Integer songID) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway {
        return null;
    }

    @Override
    public List<MaiScore> getMaimaiSongsScore(String username, List<Integer> songIDs) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway {
        return List.of();
    }

    @Override
    public MaiBestPerformance getMaimaiBest50P(Long qq) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway {
        var b = new MaimaiBestScoreQQBody(qq, true);

        return base.divingFishApiWebClient.post().uri(uriBuilder -> uriBuilder.path("api/maimaidxprober/query/player").build()).contentType(MediaType.APPLICATION_JSON).body(Mono.just(b), MaimaiBestScoreQQBody.class).headers(base::insertDeveloperHeader).retrieve().bodyToMono(MaiBestPerformance.class).block();
    }

    @Override
    public MaiBestPerformance getMaimaiBest50P(String probername) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway {
        var b = new MaimaiBestScoreNameBody(probername, true);

        return base.divingFishApiWebClient.post().uri(uriBuilder -> uriBuilder.path("api/maimaidxprober/query/player").build()).contentType(MediaType.APPLICATION_JSON).body(Mono.just(b), MaimaiBestScoreNameBody.class).headers(base::insertDeveloperHeader).retrieve().bodyToMono(MaiBestPerformance.class).block();
    }

    private <T> T file2Object(String fileName, Class<T> clazz) {
        var file = path.resolve(fileName);
        try {
            var s = Files.readString(file);
            return JacksonUtil.parseObject(s, clazz);
        } catch (IOException e) {
            log.error("maimai: 获取文件失败", e);
            return null;
        }
    }

    private <T> List<T> file2Objects(String fileName, Class<T> clazz) {
        var file = path.resolve(fileName);
        try {
            var s = Files.readString(file);
            return JacksonUtil.parseObjectList(s, clazz);
        } catch (IOException e) {
            log.error("maimai: 获取文件失败", e);
            return null;
        }
    }

    private void saveFile(String data, String fileName, String dictionaryName) {
        var file = path.resolve(fileName);

        try {
            if (isFileOutdated(fileName)) {
                Files.write(file, Objects.requireNonNull(data).getBytes());
                log.info(String.format("maimai: 已更新%s库", dictionaryName));
            } else if (Files.isDirectory(path)) {
                Files.write(file, Objects.requireNonNull(data).getBytes());
                log.info(String.format("maimai: 已保存%s库", dictionaryName));
            } else {
                log.info(String.format("maimai: 未保存%s库", dictionaryName));
            }
        } catch (IOException e) {
            log.error(String.format("maimai: %s库保存失败", dictionaryName), e);
        }
    }

    private boolean isFileOutdated(String fileName) {
        var file = path.resolve(fileName);

        try {
            return (!Files.isWritable(path) || !Files.isRegularFile(file) || System.currentTimeMillis() - Files.getLastModifiedTime(file).toMillis() > updatePeriodMillis);
        } catch (IOException e) {
            return true;
        }
    }
}
