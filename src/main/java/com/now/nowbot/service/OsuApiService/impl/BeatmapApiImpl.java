package com.now.nowbot.service.OsuApiService.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.config.FileConfig;
import com.now.nowbot.dao.BeatMapDao;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.BeatMapSet;
import com.now.nowbot.model.JsonData.BeatmapDifficultyAttributes;
import com.now.nowbot.model.JsonData.Search;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.util.AsyncMethodExecutor;
import com.now.nowbot.util.JacksonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class BeatmapApiImpl implements OsuBeatmapApiService {
    private static final Logger log = LoggerFactory.getLogger(BeatmapApiImpl.class);
    OsuApiBaseService base;
    BeatMapDao beatMapDao;
    private final Path osuDir;

    public BeatmapApiImpl(
            OsuApiBaseService baseService,
            FileConfig config,
            BeatMapDao mapDao
    ) {
        base = baseService;
        osuDir = Path.of(config.getOsuFilePath());
        beatMapDao = mapDao;
    }

    @Override
    public String getBeatMapFile(long bid) throws Exception {
        if (Files.isRegularFile(osuDir.resolve(bid + ".osu"))) {
            return Files.readString(osuDir.resolve(bid + ".osu"));
        } else {
            return AsyncMethodExecutor.execute(() -> $downloadOsuFile(bid), "_getBetamapFile" + bid, (String) null);
        }
    }

    @Override
    public boolean downloadBeatMapFile(long bid) {
        Path f = osuDir.resolve(bid + ".osu");
        if (Files.isRegularFile(f)) {
            return false;
        }
        String osuStr;
        try {
            $downloadOsuFile(bid);
        } catch (WebClientResponseException e) {
            log.error("请求失败: ", e);
            return false;
        } catch (IOException e) {
            log.error("文件写入失败: ", e);
            return false;
        }

        return true;
    }

    @Override
    public boolean checkBeatMap(long bid, String checkStr) throws IOException {
        var path = osuDir.resolve(bid + ".osu");
        if (Files.isRegularFile(path)) {
            return DigestUtils.md5DigestAsHex(Files.readAllBytes(path)).equals(checkStr);
        }
        return false;
    }

    @Override
    public BeatmapDifficultyAttributes getAttributes(Long id, OsuMode mode) {
        Map<String, Object> body = new HashMap<>();
        if (! OsuMode.isDefault(mode)) {
            body.put("ruleset_id", mode.getModeValue());
        }
        return base.osuApiWebClient.post()
                .uri("beatmaps/{id}/attributes", id)
                .headers(base::insertHeader)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .mapNotNull(j -> JacksonUtil.parseObject(j.get("attributes"), BeatmapDifficultyAttributes.class))
                .block();
    }

    @Override
    public BeatMap getBeatMapInfo(long bid) {
        return base.osuApiWebClient.get()
                .uri("beatmaps/{bid}", bid)
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(BeatMap.class)
                .map(b -> {
                    beatMapDao.saveMap(b);
                    return b;
                })
                .block();
    }

    @Override
    public BeatMapSet getBeatMapSetInfo(long sid) {
        return base.osuApiWebClient.get()
                .uri("beatmapsets/{sid}", sid)
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(BeatMapSet.class)
                .map(s -> {
                    beatMapDao.saveMapSet(s);
                    return s;
                })
                .block();
    }

    @Override
    public BeatMap getMapInfoFromDB(long bid) {
        try {
            var lite = beatMapDao.getBeatMapLite(bid);
            return BeatMapDao.fromBeatmapLite(lite);
        } catch (Exception e) {
            return getBeatMapInfo(bid);
        }
    }

    @Override
    public BeatmapDifficultyAttributes getAttributes(Long id, OsuMode mode, int modsValue) {
        Map<String, Object> body = new HashMap<>();
        if (! OsuMode.isDefault(mode)) {
            body.put("ruleset_id", mode.getModeValue());
        }
        if (modsValue != 0) {
            body.put("mods", modsValue);
        }
        return base.osuApiWebClient.post()
                .uri("beatmaps/{id}/attributes", id)
                .headers(base::insertHeader)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .mapNotNull(j -> JacksonUtil.parseObject(j.get("attributes"), BeatmapDifficultyAttributes.class))
                .block();
    }

    private String $downloadOsuFile(long bid) throws IOException {
        String osuStr = base.osuApiWebClient.get()
                .uri("https://osu.ppy.sh/osu/{bid}", bid)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        if (Objects.isNull(osuStr)) return null;
        Files.writeString(osuDir.resolve(bid + ".osu"), osuStr);
        return osuStr;
    }

    @Override
    public JsonNode lookupBeatmap(String checksum, String filename, Long id) {
        return base.osuApiWebClient.get()
                .uri(u -> u.path("beatmapsets/lookup")
                        .queryParamIfPresent("checksum", Optional.ofNullable(checksum))
                        .queryParamIfPresent("filename", Optional.ofNullable(filename))
                        .queryParamIfPresent("id", Optional.ofNullable(id))
                        .build())
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    @Override
    public Search searchBeatmap(Map<String, Object> query) {
        return base.osuApiWebClient.get()
                .uri(u -> {
                    u.path("beatmapsets/search");
                    query.forEach((k, v) -> {
                        if (Objects.nonNull(v)) u.queryParam(k, v);
                        else u.queryParam(k);
                    });
                    return u.build();
                })
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(Search.class)
                .block();
    }
}
